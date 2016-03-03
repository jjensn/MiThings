/**
 *  MiLight / EasyBulb / LimitlessLED Light Controller
 *
 *  Copyright 2015 Jared Jensen / jared /at/ cloudsy /dot/ com
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
metadata {
	definition (name: "MiLight Controller", namespace: "cloudsyjared", author: "Jared Jensen", singleInstance: false) {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
        capability "Color Control"
        capability "Polling"
        capability "Sensor"
        capability "Refresh" 
        
        command "reload"        
        command "unknown"
		//command "setAdjustedColor"
	}
    
    preferences {       
       input "mac", "string", title: "MAC Address",
       		  description: "The MAC address of this MiLight bridge", defaultValue: "The MAC address here",
              required: true, displayDuringSetup: false 
       
       input "group", "number", title: "Group Number",
       		  description: "The group you wish to control (0-4), 0 = all", defaultValue: "0",
              required: false, displayDuringSetup: false       
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Lighting.light20", backgroundColor:"#79b821", nextState:"off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Lighting.light20", backgroundColor:"#ffffff", nextState:"on"
				attributeState "unknown", label:'unknwn', action:"switch.on", icon:"st.unknown.unknown.unknown", backgroundColor:"#d3d3d3", nextState:"on"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
            tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}
        
        standardTile("refresh", "device.testing", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"testing", icon:"st.secondary.refresh"
        }
       
		main(["switch"])
		details(["switch","levelSliderControl", "rgbSelector", "refresh"])
	} 
}

def poll() {
	//if(isDebug) { log.debug "MiLight device: ${mac}, entered poll method." }
    /*state.hasPoll = false*/
    return refresh()
}

def parse(String description) {
    //if(isDebug) { log.debug "MiLight device: ${mac}, parse description ${description}" }
    parseResponse(description)
}

private parseResponse(String resp) {
	
     log.debug "Received response: ${resp}"
    
    /*if(state.hasPoll == false || state.hasPoll == null) {
    	if(isDebug) { log.debug "MiLight device: ${mac}, will run poll method in 60 seconds." }
    	runIn(60, poll)
    	state.hasPoll = true
    }*/
}

private parseResponse(resp) {
	
    log.debug "Received response: ${resp.data}" 
    
    /*if(state.hasPoll == false || state.hasPoll == null) {
    	if(isDebug) { log.debug "MiLight device: ${mac}, will run poll method in 60 seconds." }
    	runIn(60, poll)
    	state.hasPoll = true
    }
    
    if(resp.data.state != null) {
    	if(device.currentValue("switch") != resp.data.state){
    		if(isDebug) { log.debug "MiLight device: ${mac}, differences detected between power, updating SmartThings" }
    		sendEvent(name: "switch", value: resp.data.state)
    	}
        
        if(device.currentValue("level") == null) {
            if(isDebug) { log.debug "MiLight device: ${mac}, setLevel() for first time" }
			sendEvent(name: "level", value: resp.data.brightness.toInteger())
		} else {
        	if(resp.data.brightness.toInteger() != device.currentValue("level").toInteger()){
                if(isDebug) { log.debug "MiLight device: ${mac}, differences detected between brightness, updating. CLOUD: ${resp.data.brightness} / DEVICE: ${device.currentValue("level")}" }
                sendEvent(name: "level", value: resp.data.brightness.toInteger())
    		}
        }
        
        if(resp.data.hex != device.currentValue("color")){
    		if(isDebug) { log.debug "MiLight device: ${mac}, differences detected between color, updating. CLOUD: ${resp.data.hex} / DEVICE: ${device.currentValue("color")}" }
    		sendEvent(name: "color", value: "${resp.data.hex}")
    	}
    }*/
}

def setLevel(percentage, boolean sendHttp = true) {
	/*if(isDebug) { log.debug "MiLight device: ${mac}, setLevel: ${percentage}" }
	*/
    
    if (percentage < 1 && percentage > 0) {
		percentage = 1
	}
    
	//def path = buildPath("rgbw/brightness", percentage, group);
    
    sendEvent(name: 'level', value: percentage, data: [sendReq: sendHttp])
    
    
    return sendEvent(name: 'switch', value: "on", data: [sendReq: sendHttp])
}

def setColor(value, boolean sendHttp = true) { 
    setAdjustedColor(value, sendHttp)
}

def unknown() {
    sendEvent(name: "switch", value: "unknown")
}

def setAdjustedColor(value, boolean sendHttp = true) {
    //if(isDebug) { log.debug "MiLight device: ${mac}, setAdjustedColor: ${value}" }

    //int r = value.red
    //int g = value.green
    //int b = value.blue
    
    log.debug "${value}"
    String h = value.hex
    
    
    log.debug "in device code, hex value : ${h}"
    
    sendEvent(name: 'color', value: h, data: [sendReq: sendHttp])
    
    
    //def path = buildColorPath(r, g, b, h, group);

	return sendEvent(name: 'switch', value: "on", data: [sendReq: sendHttp])
}

def on(boolean sendHttp = true) {
	//if(isDebug) { log.debug "MiLight device: ${mac}, setOn" }

   // def path = buildPath("rgbw", "on", group);
    
    //if(!skipHttp) { httpCall(path); }
        
    return sendEvent(name: "switch", value: "on", data: [sendReq: sendHttp])
}

def off(boolean sendHttp = true) {
	//if(isDebug) { log.debug "MiLight device: ${mac}, setOff" }
    
    //def path = buildPath("rgbw", "off", group);
    
    //if(!skipHttp) { httpCall(path); }
    
    log.debug "in device code, ${sendHttp}"
   
    return sendEvent(name: "switch", value: "off", data: [sendReq: sendHttp]);
}
