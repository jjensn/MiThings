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
 
definition(
    name: "MiThings",
    namespace: "fireboy1919",
    parent: "fireboy1919:MiLight Manager",
    author: "Rusty Phillips",
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
            input "macAddress", "text", title: "Hub Device ID", description: "Use format 0xFFFF", required: true, submitOnChange: false
            input "ipAddress", "text", title: "Hub Base URL", description: "Base URL", required: true, submitOnChange: false
            input "zoneCount", "enum", title: "How many zones?", required: true, options: ["1", "2", "3", "4"], submitOnChange: true
		}
	}
}

def nameMiLights() {
	def howMany = 1
	if(zoneCount == "1") {
    	state.howMany = 1
    } else if(zoneCount == "2") {
    	state.howMany = 2
    } else if(zoneCount == "3") {
    	state.howMany = 3
    } else if(zoneCount == "4") {
    	state.howMany = 4
    }
    
	dynamicPage(name: "nameMiLights", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
        section("Zones") {
                for (int i = 0; i < state.howMany; i++) {
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

def isMaster() {
    return settings.group == 0;
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertToHex(port) { 
    String hex = String.format( '%04x', port)
    return hex
}
def getPrimaryDeviceId() {
	return "${convertIPtoHex(settings.ipAddress)}:${convertToHex(80)}"
}
def getPrimaryDevice() {
	log.debug(getChildDevices(true));
    return getChildDevices(true).find { it.deviceNetworkId.contains(":") };
}

def initialize() {

    app.updateLabel("${settings.miLightName}")
    
    def hub = location.getHubs().find() { it.type.toString() != "VIRTUAL" }
	
    def myDevice = getChildDevice("${settings.macAddress}/0")
 	if(!myDevice) def childDevice = addChildDevice("fireboy1919", "MiLight Controller", deviceId, hub.id, [label: "${settings.miLightName}", completedSetup: true])
	myDevice = getChildDevice(deviceId)

	myDevice.name = settings.miLightName
    myDevice.label = settings.miLightName
    myDevice.setPreferences(["mac": "${settings.macAddress}", "group":0, "ipAddress": settings.ipAddress ])
    
    subscribe(myDevice, "switch.on", switchOnHandler)
    subscribe(myDevice, "switch.off", switchOffHandler)
    subscribe(myDevice, "poll", switchPollHandler)
    subscribe(myDevice, "level", switchLevelHandler)
    subscribe(myDevice, "color", switchColorHandler)

    log.debug("Subscribed")
    //subscribeToCommand(myDevice, "refresh", switchRefreshHandler)
    
	for (int i = 0 ; i < state.howMany; i++) {
        def thisName = settings.find {it.key == "dName$i"}
    	deviceId = "${settings.macAddress}/${i+1}"
        myDevice = getChildDevice(deviceId)
 		if(!myDevice) def childDevice = addChildDevice("fireboy1919", "MiLight Controller", deviceId, hub.id, [label: thisName.value, completedSetup: true])
       	myDevice = getChildDevice(deviceId)
        
        subscribe(myDevice, "switch.on", switchOnHandler)
        subscribe(myDevice, "switch.off", switchOffHandler)
        subscribeToCommand(myDevice, "refresh", switchRefreshHandler)
        subscribe(myDevice, "level", switchLevelHandler)
        subscribe(myDevice, "color", switchColorHandler)
        
        myDevice.name = thisName.value
        myDevice.label = thisName.value
        myDevice.setPreferences(["group":i + 1])
    }
}

def uninstalled() {
}

private removeChildDevices(delete) {
    delete.each {
    }
}

def httpCall(body, evt) {
}

def switchOnHandler(evt) {
    def body = ["status": "on"]
	if(parent.settings.isDebug) { log.debug "master switch on! ${settings.macAddress} / ${evt.device.name}" }
    
     httpCall(body, settings.ipAddress, settings.macAddress,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.on(false)
        }
    }
}

def switchOffHandler(evt) {
    def body = ["status": "off"]

	if(parent.settings.isDebug) { log.debug "switch off! ${settings.macAddress} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.macAddress,
        evt.device.getPreferences()["group"]) */

    httpCall(body, settings.ipAddress, settings.macAddress,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.off(false)
        }
    }
}

def switchLevelHandler(evt) {
    def body = ["level": evt.value.toInteger ]

	if(parent.settings.isDebug) { log.debug "switch set level! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
     
    httpCall(body, settings.ipAddress, settings.macAddress,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.setLevel(evt.value.toInteger(), false)
        }
    }
}

def switchColorHandler(evt) {
    def body = ["hue": evt.value ]

	if(parent.settings.isDebug) { log.debug "color set! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
         
    httpCall(body, settings.ipAddress, settings.macAddress,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
    getChildDevices().each {
    		it.setColor(evt.value, false)
        }
    }
}

def switchRefreshHandler(evt) {
	if(parent.settings.isDebug) { log.debug "switch command : refresh !" }
    /* Does nothing. 
    def path = parent.buildPath("rgbw", "status", evt);
	parent.httpCall(path, settings.macAddress, evt);
    */
}

def httpCall(body, ipAddress, mac, group) {

    def path =  "/gateways/$mac/rgbw/$group"
    def bodyString = groovy.json.JsonOutput.toJson(body)
    def ipAddressHex = convertIPtoHex(ipAddress)
    def port = convertToHex(80);

    def deviceNetworkId = "$ipAddressHex:$port"
    try {
        def hubaction = new physicalgraph.device.HubAction([
            method: "POST",
            path: path,
            body: bodyString,
			headers: [ HOST: "$ipAddress:80", "Content-Type": "application/x-www-form-urlencoded" ]]
        )
        /*
        httpPut(path, JsonOutput.toJson(body)) {resp ->
            if(settings.isDebug) { log.debug "Successfully updated settings." }
            //parseResponse(resp, mac, evt)
        }
        */
        log.debug("Sending $bodyString to ${path}.")
        sendHubCommand(hubaction);
        return hubAction;
    } catch (e) {
        log.error "Error sending: $e"
    }
}
