/**
 *  MiLight / EasyBulb / LimitlessLED Light Controller
 *
 *  Copyright 2017  Rusty Phillips rusty dot phillips at gmail dot com
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
	def lightCount = parent.parent.state.lightCount
	dynamicPage(name: "nameMiLights", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
        section("Light") {
            input "miLightName", "text", title: "Light name", description: "i.e. Living Room", required: true, submitOnChange: false
            input "code", "number", title: "Code", required: true, description: "Optional: will be autoassigned"
            input "lightType", "enum", title: "Bulb Type", required: true, options: ['rgbw', 'cct', 'rgb_cct'], defaultValue: 'rgbw'
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
    String hex = String.format( '0x%04x', port)
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
    if(settings.code == null) {
    	settings.code = parent.parent.incLights();
    }
    myDevice.setPreferences(["code": settings.code, "group":1, "lightType": settings.lightType ])
    
    subscribe(myDevice, "switch.on", switchOnHandler)
    subscribe(myDevice, "switch.off", switchOffHandler)
    subscribe(myDevice, "poll", switchPollHandler)
    subscribe(myDevice, "level", switchLevelHandler)
    subscribe(myDevice, "color", switchColorHandler)
    subscribe(myDevice, "pair", pairHandler)
    subscribe(myDevice, "unpair", unpairHandler)
    subscribe(myDevice, "whiten", whitenHandler)
    
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
	log.debug("Pairing")
    def body = ["command": "pair"]

	//if(parent.parent.settings.isDebug) { log.debug "paired! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}
def unpairHandler(evt) {
    def body = ["command":"unpair"]

	//if(parent.parent.settings.isDebug) { log.debug "unpaired! ${settings.code} / ${evt.device.name}" }
    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}

def whitenHandler(evt) {
	def body = ["command": "set_white"]
     httpCall(body, parent.settings.ipAddress, settings.code, evt.device)
}

def switchOnHandler(evt) {
    def body = ["status": "on"]
	//if(parent.parent.settings.isDebug) { log.debug "master switch on! ${settings.code} / ${evt.device.name}" }
    
     httpCall(body, parent.settings.ipAddress, settings.code, evt.device)
}

def switchOffHandler(evt) {
    def body = ["status": "off"]

	if(parent.parent.settings.isDebug) { log.debug "switch off! ${settings.code} / ${evt.device.name}" }
    /* getPrimaryDevice().httpCall(["status": "off"], settings.ipAddress, settings.code,
        evt.device.getPreferences()["group"]) */

    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}

def switchLevelHandler(evt) {
    def body = ["level": evt.value ]

	if(parent.parent.settings.isDebug) { log.debug "switch set level! ${settings.code} / ${evt.device.name} / ${evt.value}" }
     
    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)
}

def switchColorHandler(evt) {
	// Adapted from HA Bridge.
    log.debug("Hue: " + evt.value)
    def val = (int)((256 + 26 - Math.floor((Float.parseFloat(evt.value) / 100)* 255)) % 256);
    def body = ["hue": val.toString() ]

	if(parent.parent.settings.isDebug) { log.debug "color set! ${settings.code} / ${evt.device.name} / ${evt.value}" }
         
    httpCall(body, parent.settings.ipAddress, settings.code, evt.device)

}

def httpCall(body, ipAddress, code, device) {
    def bulbType = device.getPreferences()['lightType']
    def group = device.getPreferences()['group']
    def codeHex = convertToHex(code);
    def path =  "/gateways/$codeHex/$lightType/$group"
    def bodyString = groovy.json.JsonOutput.toJson(body)
    log.debug("Sending $bodyString to $ipAddress${path}.")

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

        sendHubCommand(hubaction);
        return hubAction;
    } catch (e) {
        log.error "Error sending: $e"
    }
}