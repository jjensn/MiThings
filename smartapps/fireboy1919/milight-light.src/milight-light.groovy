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
    name: "MilightLight",
    namespace: "fireboy1919",
    parent: "fireboy1919:MiThings",
    author: "Rusty Phillips",
    description: "Child application for MiThings -- do not install directly",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png")


preferences {
    page(name: "nameMiLights")
}

def nameMiLights() {
	  
	dynamicPage(name: "nameMiLights", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
        section("Light") {
            input "miLightName", "text", title: "Light name", description: "i.e. Living Room", required: true, submitOnChange: false
            input "code", "number", required: true, description: "Code that is stored in the light to represent the device" 
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

def initialize() {

    app.updateLabel("${settings.miLightName}")
    
    def hub = location.getHubs().find() { it.type.toString() != "VIRTUAL" }
	
    def myDevice = getChildDevice("${settings.code}/0")
 	if(!myDevice) def childDevice = addChildDevice("fireboy1919", "MiLight Controller", deviceId, hub.id, [label: "${settings.miLightName}", completedSetup: true])
	myDevice = getChildDevice(deviceId)

	myDevice.name = settings.miLightName
    myDevice.label = settings.miLightName
    myDevice.setPreferences(["code": "${settings.code}", "group":1, "ipAddress": settings.ipAddress ])
    
    subscribe(myDevice, "switch.on", switchOnHandler)
    subscribe(myDevice, "switch.off", switchOffHandler)
    subscribe(myDevice, "poll", switchPollHandler)
    subscribe(myDevice, "level", switchLevelHandler)
    subscribe(myDevice, "color", switchColorHandler)

    log.debug("Subscribed")
    //subscribeToCommand(myDevice, "refresh", switchRefreshHandler)
    
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
	if(parent.settings.isDebug) { log.debug "master switch on! ${settings.code} / ${evt.device.name}" }
    
     httpCall(body, settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.on(false)
        }
    }
}

def switchOffHandler(evt) {
    def body = ["status": "off"]

	if(parent.settings.isDebug) { log.debug "switch off! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.off(false)
        }
    }
}

def switchLevelHandler(evt) {
    def body = ["level": evt.value.toInteger ]

	if(parent.settings.isDebug) { log.debug "switch set level! ${settings.code} / ${evt.device.name} / ${evt.value}" }
     
    httpCall(body, settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"])

    if(getPrimaryDevice().deviceNetworkId == evt.device.deviceNetworkId) {
        getChildDevices().each {
    	    it.setLevel(evt.value.toInteger(), false)
        }
    }
}

def switchColorHandler(evt) {
    def body = ["hue": evt.value ]

	if(parent.settings.isDebug) { log.debug "color set! ${settings.code} / ${evt.device.name} / ${evt.value}" }
         
    httpCall(body, settings.ipAddress, settings.code,
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
	parent.httpCall(path, settings.code, evt);
    */
}
