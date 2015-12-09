/**
 *  MiLight Installer
 *
 *  Copyright 2015 Jared Jensen
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
    name: "MiLight Manager",
    namespace: "cloudsyjared/milight-master",
    author: "Jared Jensen",
    description: "Adds better SmartThings support for MiLight / Easybulb / LimitlessLED bulbs",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)


preferences {
   page(name: "mainPage", title: "MiLight Manager", uninstall: true, submitOnChange: true) {
        section {
            app(name: "childApps", appName: "MiLight Zone", namespace: "cloudsyjared/milight-zone", title: "Add MiLight group...", multiple: true)
            }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
    	// this is where we will create teh child devices
        log.debug "child app: ${child.label}"
        log.debug "zone: ${child.zoneNumber} / name: ${child.zoneName} / hub: ${child.wifiHub}"
 		// stopped here, come up with a standard device name..
        def d = getChildDevice(child.zoneName)
        if(!d) {
            def newDevice = devices.find { (it.value.ip + ":" + it.value.port) == dni               }
            d = addChildDevice("smartthings", "Device Name", dni, newDevice?.value.hub, ["label":newDevice?.value.name])
            subscribeAll() //helper method to update devices
        }
    }
}