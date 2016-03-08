/**
 *  MiLight Installer
 *
 *  Copyright 2016 Jared Jensen / jared at cloudsy com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
 
definition(
    name: "MiThings",
    namespace: "cloudsyjared",
    parent: "cloudsyjared:MiLight Manager",
    author: "Jared Jensen",
    description: "Child application for MiLight Manager -- do not install directly",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png")


preferences {
	page(name: "selectMiLight", nextPage: "nameMiLights")
    page(name: "nameMiLights")
}


def getName(myName, n) {
	def result = input myName, "text", title: "Zone #$n Name", required: true
}

def selectMiLight() {
	dynamicPage(name: "selectMiLight", title: "MiLight Wifi Hub Setup", uninstall: true) {
		section("") {
            input "miLightName", "text", title: "MiLight hub name", description: "ie: Living Room Master Switch", required: true, submitOnChange: false
            input "macAddress", "text", title: "Hub MAC address", description: "Use format AA:BB:CC:DD:EE:FF", required: true, submitOnChange: false
			input "howMany", "number", title: "How many zones?", required: true, submitOnChange: true, range: "1..4"
		}
		
	}
}

def nameMiLights() {
	dynamicPage(name: "nameMiLights", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
        section("Zones") {
                for (int i = 0; i < howMany; i++) {
                    def thisName = "dName$i"
                    getName(thisName, i + 1)
                    paragraph(" ")
                }
            }
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {

    app.updateLabel("${settings.miLightName}")
    
    def deviceId = "${settings.macAddress}/0"
    def myDevice = getChildDevice(deviceId)
 	if(!myDevice) def childDevice = addChildDevice("cloudsyjared", "MiLight Controller", deviceId, null, [label: "${settings.miLightName}", completedSetup: true])
	myDevice = getChildDevice(deviceId)

	myDevice.name = settings.miLightName
    myDevice.label = settings.miLightName
    myDevice.setPreferences(["mac": "${settings.macAddress}", "group":0])
    
    subscribe(myDevice, "switch.on", masterSwitchOnHandler)
    subscribe(myDevice, "switch.off", masterSwitchOffHandler)
    subscribe(myDevice, "poll", masterSwitchPollHandler)
    subscribe(myDevice, "level", masterSwitchLevelHandler)
    subscribe(myDevice, "color", masterSwitchColorHandler)
    subscribeToCommand(myDevice, "reload", masterSwitchReloadHandler)
    
	for (int i = 0 ; i < howMany; i++) {
        def thisName = settings.find {it.key == "dName$i"}
    	deviceId = "${settings.macAddress}/${i+1}"
        myDevice = getChildDevice(deviceId)
 		if(!myDevice) def childDevice = addChildDevice("cloudsyjared", "MiLight Controller", deviceId, null, [label: thisName.value, completedSetup: true])
		myDevice = getChildDevice(deviceId)
        
        subscribe(myDevice, "switch", zoneSwitchHandler)
        /*subscribe(myDevice, "refresh", zoneRefreshHandler)*/
        subscribe(myDevice, "level", zoneSwitchLevelHandler)
        //subscribe(myDevice, "poll", zoneSwitchPollHandler))
        subscribe(myDevice, "color", zoneSwitchColorHandler)
        
        myDevice.name = thisName.value
        myDevice.label = thisName.value
        myDevice.setPreferences(["mac": "${settings.macAddress}", "group":i + 1])
    }
}

def uninstalled() {
//    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        //deleteChildDevice(it.deviceNetworkId)
    }
}

def masterSwitchOnHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch on! ${settings.macAddress} / ${evt.device.name}" }
    
    def path = parent.buildPath("rgbw", "on", evt);
    parent.httpCall(path, settings.macAddress, evt);
    
    getChildDevices().each {
    	it.on(false)
    }
}

def masterSwitchOffHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch off! ${settings.macAddress} / ${evt.device.name}" }
    
    def path = parent.buildPath("rgbw", "off", evt);
    parent.httpCall(path, settings.macAddress, evt);
   
    getChildDevices().each {
    	it.off(false)
    }
}

def masterSwitchLevelHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch set level! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
    
    def path = parent.buildPath("rgbw/brightness", evt.value.toInteger(), evt);
    parent.httpCall(path, settings.macAddress, evt);
   
    getChildDevices().each {
    	it.setLevel(evt.value.toInteger(), false)
    }
}

def masterSwitchColorHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master color set! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
     
    def path = parent.buildPath("rgbw/hex", evt.value, evt);

    parent.httpCall(path, settings.macAddress, evt);
   
    getChildDevices().each {
    	if(it.getPreferences()["group"] != "0" && it.getPreferences()["group"] != null) {
        	//log.debug "${it.name} / ${it.getPreferences()["group"]} / ${evt}"
    		it.setColor(evt.value, false)
        }
    }
}

def masterSwitchReloadHandler(evt) {
	log.debug "in master reload"
}

def zoneSwitchHandler(evt) {
	if(parent.settings.isDebug) { log.debug "Zone switch changed state! ${evt.value}!" }
    
    def jsonObj = new JsonSlurper().parseText( evt.data )
    
    if(jsonObj.sendReq == true) {
	    def deviceId = "${settings.macAddress}/0"
    	def myDevice = getChildDevice(deviceId)
    
        if(myDevice) {
            myDevice.unknown()
        }
        
        def path = parent.buildPath("rgbw", evt.value, evt);
    	parent.httpCall(path, settings.macAddress, evt);
    }
}

def zoneSwitchLevelHandler(evt) {
	if(parent.settings.isDebug) { log.debug "Zone switch changed level! ${evt.value}!" }
    
    def jsonObj = new JsonSlurper().parseText( evt.data )
    
    if(jsonObj.sendReq == true) {
	    def deviceId = "${settings.macAddress}/0"
    	def myDevice = getChildDevice(deviceId)
    
        if(myDevice) {
            myDevice.unknown()
        }
        
        def path = parent.buildPath("rgbw/brightness", evt.value.toInteger(), evt);
    	parent.httpCall(path, settings.macAddress, evt);
	}
}

def zoneSwitchColorHandler(evt) {
	if(parent.settings.isDebug) { log.debug "Zone switch color change! ${evt.value}!" }
    
    def jsonObj = new JsonSlurper().parseText( evt.data )
    
    if(jsonObj.sendReq == true) {
	    def deviceId = "${settings.macAddress}/0"
    	def myDevice = getChildDevice(deviceId)
    
        if(myDevice) {
            myDevice.unknown()
        }
        
		def path = parent.buildPath("rgbw/hex", evt.value, evt);
		parent.httpCall(path, settings.macAddress, evt);
	}
}