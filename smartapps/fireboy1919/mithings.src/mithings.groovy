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
    page(name: "nameMiLights")
}

def nameMiLights() {
	  
	dynamicPage(name: "nameMiLights", title: "MiLight Wifi Hub Setup", uninstall: true, install: true) {
        section("Lights") {
            app(name: "childHubs", appName: "MilightLight", namespace: "fireboy1919", title: "Add New Light...", multiple: true)
        }
        section("") {
            input "hubName", "text", title: "Name", description: "Hub Name", required: true, submitOnChange: false
            input "ipAddress", "text", title: "IP Address", description: "IP Address of Hub", required: true, submitOnChange: false
        
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
    app.updateLabel(settings.hubName);
    childApps.each {child ->
		if(settings.isDebug) { log.debug "Installed Hubs: ${child.label}" }
    }
}

def uninstalled() {
}

private removeChildDevices(delete) {
    delete.each {
    }
}
