package main

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"regexp"
	"strings"
	"sync"
	"time"

	kingpin "gopkg.in/alecthomas/kingpin.v2"

	log "github.com/Sirupsen/logrus"
	"github.com/caarlos0/env"
	"github.com/garyburd/redigo/redis"
	_ "github.com/lib/pq"
	// your local imports
)

var (
	//listenPort = kingpin.Flag("port", "Listening port.").Default("38899").Int()
	redisPort = kingpin.Flag("redis-port", "Listening port of the redis server.").Default("6379").Int()
	logLevel  = kingpin.Flag("log-level", "debug / info / error").String()
)

type envConfig struct {
	RedisServer string `env:"REDIS_SERVER,required"`
	RedisPass   string `env:"REDIS_PASS"`
	EnvName     string `env:"ENV_NAME,required"`
	ListenPort  int    `env:"LISTEN_PORT,required"`
	PgSQLDbName string `env:"PGSQL_DB_NAME,required"`
	PgSQLDbUser string `env:"PGSQL_DB_USER,required"`
	PgSQLDbPass string `env:"PGSQL_DB_PASS,required"`
	PgSQLDbHost string `env:"PGSQL_DB_HOST,required"`
}

// CommandItem JSON representation of the commands
type CommandItem struct {
	Instructions []string `json:"cmds"`
	Mac          string   `json:"mac"`
	ActionID     int      `json:"action_log_id"`
}

var pool *redis.Pool
var macMap map[string]chan CommandItem
var cmdMutex sync.Mutex

func init() {
	kingpin.Parse()
}

func newPool(server, password string, dbNum int) *redis.Pool {
	return &redis.Pool{
		MaxIdle:     3,
		IdleTimeout: 240 * time.Second,
		Dial: func() (redis.Conn, error) {
			c, err := redis.Dial("tcp", server)
			if err != nil {
				return nil, err
			}

			if _, err := c.Do("AUTH", password); err != nil {
				c.Close()
				return nil, err
			}

			if _, err := c.Do("SELECT", dbNum); err != nil {
				c.Close()
				return nil, err
			}
			return c, err
		},
		TestOnBorrow: func(c redis.Conn, t time.Time) error {
			if time.Since(t) < time.Minute {
				return nil
			}
			_, err := c.Do("PING")
			return err
		},
	}
}

func newPgSQLPool(cfg envConfig) *sql.DB {
	db, err := sql.Open("postgres", fmt.Sprintf("user=%s dbname=%s password=%s host=%s sslmode=disable", cfg.PgSQLDbUser, cfg.PgSQLDbName, cfg.PgSQLDbPass, cfg.PgSQLDbHost))
	if err != nil {
		log.Fatal(err)
	}
	return db
}

func main() {

	cfg := envConfig{}
	err := env.Parse(&cfg)

	switch *logLevel {
	case "debug":
		log.SetLevel(log.DebugLevel)
	case "info":
		log.SetLevel(log.InfoLevel)
	case "error":
		log.SetLevel(log.ErrorLevel)
	default:
		log.SetLevel(log.InfoLevel)
	}

	customFormatter := new(log.TextFormatter)
	log.SetFormatter(customFormatter)
	customFormatter.FullTimestamp = true

	if err != nil {
		log.WithFields(log.Fields{"error": err}).Error("Error occurred with env.Parse")
		os.Exit(1)
	}

	log.WithFields(log.Fields{"listenPort": fmt.Sprintf("%d", cfg.ListenPort), "redisServer": cfg.RedisServer, "redisPort": fmt.Sprintf("%d", *redisPort), "logLevel": *logLevel}).Debug("Application launched")

	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", cfg.ListenPort))
	if err != nil {
		log.WithFields(log.Fields{"error": err}).Error("Error occurred in net.Listen")
		os.Exit(1)
	}

	if cfg.RedisPass != "" {
		log.WithFields(log.Fields{"password": cfg.RedisPass}).Debug("Password set")
	}

	dbNum := 0

	if cfg.EnvName == "production" {
		dbNum = 1
	}

	log.Debugf("cfg.EnvName: %s", cfg.EnvName)

	pool = newPool(fmt.Sprintf("%s:%d", cfg.RedisServer, *redisPort), cfg.RedisPass, dbNum)

	redisConn := pool.Get()
	defer redisConn.Close()

	macMap = map[string]chan CommandItem{}

	// some basic tests to see if we can read and write to redis (these check to ensure authentication worked)

	writeResult, err := redis.Bytes(redisConn.Do("SET", "init", "init"))

	if err != nil || writeResult == nil {
		log.WithFields(log.Fields{"error": err, "writeResult": writeResult}).Error("Error writing to redis!")
		os.Exit(1)
	}

	readResult, err := redis.Bytes(redisConn.Do("GET", "init"))

	if err != nil || readResult == nil {
		log.WithFields(log.Fields{"error": err, "readResult": readResult}).Error("Error reading from redis!")
		os.Exit(1)
	}

	redisConn.Close()

	db := newPgSQLPool(cfg)

	defer db.Close()

	log.Info("Backend server ready for connections")

	// launch the worker who will read from redis and pass the messages to each connected light/hub
	go redisPubSub(cfg)

	for {
		conn, err := ln.Accept()
		if err != nil {
			log.WithFields(log.Fields{"error": err}).Error("Error occurred in net.Accept")
			continue
		}
		log.WithFields(log.Fields{"remote_addr": conn.RemoteAddr()}).Debug("Client connected")

		// launch each connection in a go routine and keep waiting for more connections
		go handleConnection(conn, db)
	}
}

func redisPubSub(cfg envConfig) {

	conn := pool.Get()
	psc := redis.PubSubConn{Conn: conn}
	defer conn.Close()

	psc.Subscribe(fmt.Sprintf("%s_cmdQueue", cfg.EnvName))

	for {
		switch n := psc.Receive().(type) {
		case redis.Message:
			var Command CommandItem
			log.WithFields(log.Fields{"message": fmt.Sprintf("%s", n.Data)}).Debug("Subscribe sees a message")

			// Unmarshal the JSON string to grab the MAC and actual work bits
			if err := json.Unmarshal(n.Data, &Command); err != nil {
				log.WithFields(log.Fields{"mac": Command.Mac, "error": err}).Error("Error parsing JSON")
				break
			}
			log.WithFields(log.Fields{"mac": Command.Mac, "action_log_id": Command.ActionID}).Debug("Marshaled OK")
			log.Debugf("Locking %s in pubSub", Command.Mac)
			cmdMutex.Lock()
			select {
			// non-blocking channel writes. we don't want to block if nobody is listening (and if nobody is listening, it means no lights are connected)
			// essentially blackholing all api calls when no lights are connected, which is ideal
			case macMap[Command.Mac] <- Command:
				log.Debugf("Dispatching messages to %s in pubSub", Command.Mac)
			default:
				log.Debug("Tried to send, but nobody was listening.")
			}
			log.Debugf("Unlocking %s in pubSub", Command.Mac)
			cmdMutex.Unlock()
			log.WithFields(log.Fields{"mac": Command.Mac}).Debug("Send complete.")

		case redis.Subscription:
			log.WithFields(log.Fields{"subscription": fmt.Sprintf("%s/%s", n.Kind, n.Channel)}).Debug("Found a subscription")
			if n.Count == 0 {
				return
			}
		case error:
			fmt.Printf("error: %v\n", n)
			return
		}
	}

}

func handleConnection(c net.Conn, db *sql.DB) {
	buf := make([]byte, 4096)
	var mac string

	for {
		n, err := c.Read(buf)
		if err != nil || n == 0 {
			c.Close()
			break
		}

		if mac == "" {
			//r, _ := regexp.Compile("[0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}")

			//bMac := r.Find(buf[0:n])
			match, _ := regexp.MatchString("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})", string(buf[0:n]))

			if match {
				tMac := strings.TrimSpace(string(buf[0:n]))
				// seen some odd cases where the mac address has a \n attached to it
				garbageSpot := strings.Index(tMac, "\n")
				if garbageSpot > -1 {
					mac = tMac[0:garbageSpot]
				} else {
					mac = tMac
				}

				log.WithFields(log.Fields{"mac": mac, "remote_addr": c.RemoteAddr()}).Info("Client sent MAC address")
				redisConn := pool.Get()
				// let's keep a running list of active connections, so we can display it on the admin page later
				redisConn.Do("SADD", "currentlyConnected", mac)
				redisConn.Close()

				t := time.Now()

				rows, err := db.Query(`UPDATE hubs SET heartbeat = $1 WHERE adjusted_mac = $2`, t, mac)
				rows.Close()
				if err != nil {
					log.WithFields(log.Fields{"error": err}).Error("Error updating database with heartbeat timestamp!")
				}

				// we've got a new connection here, but if the map already has a member with the same mac, it's just a reconnect.
				// let's close the channel, which will cause the go routine that was launched prior to exit cleanly.
				// this ensures 1 connection = 1 running go routine

				// macMap[mac] will be nil for each unique connection (each unique bulb)
				if macMap[mac] != nil {
					cmdMutex.Lock()
					close(macMap[mac])
					cmdMutex.Unlock()
				}
				log.Debugf("Locking in %s handleConnection", mac)
				cmdMutex.Lock()
				// part of the cleanup process in waitForCommands is to delete the member, so make it each time we get a connect/reconnect
				macMap[mac] = make(chan CommandItem)
				cmdMutex.Unlock()
				log.Debugf("Unlocking in %s handleConnection", mac)
				go waitForCommands(c, mac, db)
			} else {
				log.WithFields(log.Fields{"remote_addr": c.RemoteAddr()}).Debugf("Unexpected string received before MAC, ignoring. (%s)", buf[0:n])
			}
		}
	}
	// the only way we get down here is if the read caused a break, so let's clean things up.
	if mac != "" {
		log.Debugf("Locking in %s handleConnection", mac)
		cmdMutex.Lock()
		close(macMap[mac])
		log.Debugf("Unlocking in %s handleConnection", mac)
		cmdMutex.Unlock()
	}

	log.WithFields(log.Fields{"remote_addr": c.RemoteAddr()}).Debug("Connection closed in handleConnection")
}

func waitForCommands(c net.Conn, s string, db *sql.DB) {
	var zero []byte

	conn := pool.Get()

	defer conn.Close()

	for {
		// blocking
		cmdItem, more := <-macMap[s]

		if !more {
			log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr()}).Debug("Channel closed (new connection from same MAC), exiting go routine")
			break
		}

		// check if we can write to the lightbulb tcp socket since we blocked and don't know what happened during that time.
		w, err := c.Write(zero)

		if err != nil {
			log.Debugf("w %s", w)
			break
		}

		for _, cmd := range cmdItem.Instructions {
			log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr(), "base64_cmd": strings.TrimSpace(cmd)}).Info("JSON marshaled, base64 command")
			data, err := base64.StdEncoding.DecodeString(cmd)
			if err != nil {
				log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr(), "error": err}).Error("Error decoding base64")
				break
			}
			log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr(), "cmd": data}).Debug("Data prepared to write to socket")
			c.Write([]byte(fmt.Sprintf("%s", data)))
			log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr()}).Info("Data written to socket")

			time.Sleep(250 * time.Millisecond)
		}
		rows, err := db.Query(`UPDATE action_logs SET processed = $1 FROM hubs WHERE action_logs.hub_id=hubs.id AND hubs.adjusted_mac = $2 AND action_logs.id = $3`, 1, cmdItem.Mac, cmdItem.ActionID)

		rows.Close()

		if err != nil {
			log.WithFields(log.Fields{"error": err}).Error("Error updating database with processed status!")
		}

		log.WithFields(log.Fields{"updated_rows": rows}).Debug("Updated action_logs successfully.")
		time.Sleep(250 * time.Millisecond)
	}
	// the only way we can get down here is if our channel gets closed (a new connection from the same bulb), or if our tcp connection dies (the bulb says goodbye)
	log.Debug("Locking in waitForCommands")
	cmdMutex.Lock()
	delete(macMap, s)
	log.Debug("Unlocking in waitForCommands")
	cmdMutex.Unlock()
	// remove ourselves from the currently connected list, since our connection is dead
	conn.Do("SREM", "currentlyConnected", s)
	log.WithFields(log.Fields{"mac": s, "remote_addr": c.RemoteAddr()}).Debug("waitForCommands exited")
}
