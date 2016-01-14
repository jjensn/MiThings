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
    namespace: "cloudsyjared",
    author: "Jared Jensen",
    description: "Adds SmartThings support for MiLight / Easybulb / LimitlessLED bulbs",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png",
    singleInstance: false)


preferences {
	page(name: "selectMiLight")
}


def getName(myName, n) {
	def result = input myName, "text", title: "Zone #$n Name", required: true
}

def selectMiLight() {
	dynamicPage(name: "selectMiLight", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
		section("") {
			input "wifiHub", "device.milightController", title: "Wifi Hub Name", required: true, multiple: false
			input "howMany", "enum", title: "How many zones?", options: ["1" , "2" , "3" ,"4"], required: true, submitOnChange: true
		}
		section("Zones") {
        	int x = 0;
        	if(howMany) { x = howMany.toInteger() }
			for (int i = 0; i < x; i++) {
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
	app.updateLabel("MiLight/${settings.wifiHub.label}")
    state.myDevices = [:]
	for (int i = 0 ; i < howMany.toInteger(); i++) {
        def thisName = settings.find {it.key == "dName$i"}
    	def deviceId = "${settings.wifiHub.deviceNetworkId}/${i}"
        def myDevice = getChildDevice(deviceId)
 		if(!myDevice) def childDevice = addChildDevice("cloudsyjared", "MiLight Controller", deviceId, settings.wifiHub.hub.id, [label: thisName.value, completedSetup: true])
		myDevice = getChildDevice(deviceId)
        myDevice.name = thisName.value
        myDevice.label = thisName.value
        def pref = settings.wifiHub.getPreferences()
        myDevice.setPreferences(["mac": "${pref.mac}", "group":i + 1, "isDebug": false])
    }
}