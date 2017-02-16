definition(
    name: "MiLight Manager",
    singleInstance: true,
    namespace: "cloudsyjared",
    author: "Jared Jensen",
    description: "Adds SmartThings support for MiLight / Easybulb / LimitlessLED bulbs",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light20-icn@2x.png")

preferences {
    page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Manage multiple MiLight hubs", install: true, uninstall: true, submitOnChange: true) {
    	if(state.notification && state.notification.hasMessage) {
            section("") {
                href(name: "hrefWithImage", title: "${state.notification.title}",
                     description: "${state.notification.message}",
                     required: false,
                     image: "${state.notification.image}",
                     url: "${state.notification.url}" )
            }
        }
        section("") {
            app(name: "childHubs", appName: "MiThings", namespace: "cloudsyjared", title: "Add New Hub...", multiple: true)
            input "isDebug", "bool", title: "Enable debug logging", defaultValue: false, required: false, displayDuringSetup: true
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
    childApps.each {child ->
		if(settings.isDebug) { log.debug "Installed Hubs: ${child.label}" }
    }
}

def buildPath(option, value, evt) {
	def path = ""
    
	def group = evt.device.getPreferences()["group"]
       
	if(group == 0 || group == null) {
    	path = "$option/$value"
    } else {
    	path = "$option/$value/$group"
    }
    
    if(settings.isDebug) { log.debug "MiLight device: ${evt.device.getPreferences()["mac"]}, built path: $path" }
    
    return path;
}

def buildColorPath(hex, evt) {
	def path = ""    
    def value = "rgbw/color/$hex"
    def group = evt.device.getPreferences()["group"]
    
	if(group == 0 || group == null) {
    	path = "$value"
    } else {
    	path = "$value/$group"
    }
    
    if(settings.isDebug) { log.debug "MiLight device: ${mac}, color path: $path" }
    
    return path;
}

def httpCall(path, mac, evt) {
    def params = [
        uri:  'http://dev-api.mithings.pw/v1/',
        path: "$path",
        contentType: 'application/json',
        headers: [MAC:"$mac"]
    ]
    try {
        httpGet(params) {resp ->
            if(settings.isDebug) { log.debug "MiLight device: ${mac}, raw data from cloud: ${resp.data}" }
            parseResponse(resp, mac, evt)
        }
    } catch (e) {
        log.error "error: $e"
    }
}

private parseResponse(resp, mac, evt) {

	if(settings.isDebug) { log.debug "Received response: ${resp} ${evt.value}" }    
    if(resp.data.brightness != null) {
        if(evt.device.currentValue("level") == null) {
            if(settings.isDebug) { log.debug "MiLight device: ${mac}, setLevel() for first time" }
			evt.device.setLevel(resp.data.brightness.toInteger())
		} else {
        	if(resp.data.brightness.toInteger() != evt.device.currentValue("level").toInteger()){
                if(settings.isDebug) { log.debug "MiLight device: ${mac}, differences detected between brightness, updating. CLOUD: ${resp.data.brightness} / DEVICE: ${evt.device.currentValue("level")}" }
                evt.device.setLevel(resp.data.brightness.toInteger())
    		}
        }
    }
    
    if(resp.data.hex != null) {
        if(resp.data.hex != evt.device.currentValue("color")){
    		if(settings.isDebug) { log.debug "MiLight device: ${mac}, differences detected between color, updating. CLOUD: ${resp.data.hex} / DEVICE: ${evt.device.currentValue("color")}" }
            evt.device.setColor(resp.data)
    	}
    }
    
    if(resp.data.power != null) {
    	if(evt.device.currentValue("switch") != resp.data.power){
    		if(settings.isDebug) { log.debug "MiLight device: ${mac}, differences detected between power, updating SmartThings. [ device: ${evt.device.currentValue("switch")}, cloud: ${resp.data.power} ]" }
            if(resp.data.power == "on") { evt.device.on() } else if(resp.data.power == "off") { evt.device.off() }
    	}
    }
        
    if(resp.data.notification != null) {
        // hasMessage should be set to 0 or 1
        if(state.notification == null) { state.notification = [:] }
        if(state.notification.hasMessage != resp.data.notification.hasMessage) {
        	if(state.notification.hasMessage == 0 && resp.data.notification.hasMessage == 1) {
            	// New message alert, send notify
                sendPush("MiLight Manager - ${resp.data.notification.title}. Check SmartApp for details.")
            }
        }
        state.notification.hasMessage = resp.data.notification.hasMessage
        if(resp.data.notification.hasMessage == 1) {
            state.notification.message = new String(resp.data.notification.message.decodeBase64())
            state.notification.url = new String(resp.data.notification.url.decodeBase64())
            state.notification.title = new String(resp.data.notification.title.decodeBase64())
            state.notification.image = new String(resp.data.notification.image.decodeBase64())
        } else {
            state.notification.message = ""
            state.notification.url = ""
            state.notification.title = ""
            state.notification.image = ""
        }
    }
}