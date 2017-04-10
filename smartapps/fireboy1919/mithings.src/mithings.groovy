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

def initialize() {

    app.updateLabel("${settings.miLightName}")
    
    def deviceId = "${settings.macAddress}/rgbw/0"
    def myDevice = getChildDevice(deviceId)
 	if(!myDevice) def childDevice = addChildDevice("fireboy1919", "MiLight Controller", deviceId, null, [label: "${settings.miLightName}", completedSetup: true])
	myDevice = getChildDevice(deviceId)

	myDevice.name = settings.miLightName
    myDevice.label = settings.miLightName
    myDevice.setPreferences(["mac": "${settings.macAddress}", "group":0])
    
    subscribe(myDevice, "switch.on", switchOnHandler)
    subscribe(myDevice, "switch.off", switchOffHandler)
    subscribe(myDevice, "poll", switchPollHandler)
    subscribe(myDevice, "level", switchLevelHandler)
    subscribe(myDevice, "color", switchColorHandler)
    subscribeToCommand(myDevice, "refresh", switchRefreshHandler)
    
	for (int i = 0 ; i < state.howMany; i++) {
        def thisName = settings.find {it.key == "dName$i"}
    	deviceId = "${settings.macAddress}/rgbw/${i+1}"
        myDevice = getChildDevice(deviceId)
 		if(!myDevice) def childDevice = addChildDevice("fireboy1919", "MiLight Controller", deviceId, null, [label: thisName.value, completedSetup: true])
		myDevice = getChildDevice(deviceId)
        
        subscribe(myDevice, "switch", switchHandler)
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

def switchOnHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch on! ${settings.macAddress} / ${evt.device.name}" }
    
    //def path = parent.buildPath("rgbw/power", "on", evt);
    parent.httpCall(["status": "on" ], settings.ipAddress, evt)
    
    getChildDevices().each {
    	it.on(false)
    }
}

def switchOffHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch off! ${settings.macAddress} / ${evt.device.name}" }
    
    parent.httpCall(["status": "off" ], settings.ipAddress, settings.macAddress, evt);
   
    getChildDevices().each {
    	it.off(false)
    }
}

def switchLevelHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master switch set level! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
    
    parent.httpCall(["level": evt.value.toInteger ], settings.ipAddress, settings.macAddress, evt);
    getChildDevices().each {
    	it.setLevel(evt.value.toInteger(), false)
    }
}

def switchColorHandler(evt) {
	if(parent.settings.isDebug) { log.debug "master color set! ${settings.macAddress} / ${evt.device.name} / ${evt.value}" }
     
    parent.httpCall(["hue": evt.value ], settings.ipAddress, settings.macAddress, evt);
    getChildDevices().each {
    		it.setColor(evt.value, false)
    }
}

def switchRefreshHandler(evt) {
	if(parent.settings.isDebug) { log.debug "Master switch command : refresh !" }
    /* Does nothing. 
    def path = parent.buildPath("rgbw", "status", evt);
	parent.httpCall(path, settings.macAddress, evt);
    */
}