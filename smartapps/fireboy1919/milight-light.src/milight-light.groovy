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
            input "lightType", "enume", title: "Bulb Type", required: true, options: ['rgbw', 'cct', 'rgb_cct'], default: 'rgbw'
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
    def myDevice = getChildDevices(true)[0];
    if(myDevice == null) {
        log.debug("Adding device.")
 	    myDevice = addChildDevice("fireboy1919", "MiLight Controller", java.util.UUID.randomUUID().toString(), hub.id, [label: "${settings.miLightName}", completedSetup: true])
    }

	myDevice.name = settings.miLightName
    myDevice.label = settings.miLightName
    myDevice.setPreferences(["code": "${settings.code}", "group":1, "lightType":settings.lightType ])
    
    subscribe(myDevice, "switch.on", switchOnHandler)
    subscribe(myDevice, "switch.off", switchOffHandler)
    subscribe(myDevice, "poll", switchPollHandler)
    subscribe(myDevice, "level", switchLevelHandler)
    subscribe(myDevice, "color", switchColorHandler)
    subscribe(myDevice, "pair", pairHandler)
    subscribe(myDevice, "unpair", unpairHandler)
    log.debug("Subscribed")
    //subscribeToCommand(myDevice, "refresh", switchRefreshHandler)
    
}

def uninstalled() {
}

private removeChildDevices(delete) {
    delete.each {
    }
}

def pairHandler(evt) {
    def body = ["pair": "on"]

	if(parent.settings.isDebug) { log.debug "paired! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}
def unpairHandler(evt) {
    def body = ["unpair": "on"]

	if(parent.settings.isDebug) { log.debug "unpaired! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}



def switchOnHandler(evt) {
    def body = ["status": "on"]
	if(parent.settings.isDebug) { log.debug "master switch on! ${settings.code} / ${evt.device.name}" }
    
     httpCall(body, parent.settings.ipAddress, settings.code, evt.device)
}

def switchOffHandler(evt) {
    def body = ["status": "off"]

	if(parent.settings.isDebug) { log.debug "switch off! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}

def switchLevelHandler(evt) {
    def body = ["level": evt.value.toInteger ]

	if(parent.settings.isDebug) { log.debug "switch set level! ${settings.code} / ${evt.device.name} / ${evt.value}" }
     
    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)
}

def switchColorHandler(evt) {
    def body = ["hue": evt.value ]

	if(parent.settings.isDebug) { log.debug "color set! ${settings.code} / ${evt.device.name} / ${evt.value}" }
         
    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}

private String convertToHex(port) { 
    String hex = String.format( '0x%04x', port.toInteger())
    return hex
}

def httpCall(body, ipAddress, code, device) {
    def bulbType = device.getPreferences()['lightType']
    def group = device.getPreferences()['group']
    def codeHex = convertToHex(code);
    def path =  "/gateways/$codeHex/$lightType/$group"
    def bodyString = groovy.json.JsonOutput.toJson(body)

    def deviceNetworkId = "$ipAddressHex:$port"
    try {
        def hubaction = new physicalgraph.device.HubAction([
            method: "POST",
            path: path,
            body: bodyString,
			headers: [ HOST: "$ipAddress:80", "Content-Type": "application/json" ]]
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