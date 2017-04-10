import groovy.json.JsonOutput

definition(
    name: "MiLight Manager",
    singleInstance: true,
    namespace: "fireboy1919",
    author: "Rusty Phillips",
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
            app(name: "childHubs", appName: "MiThings", namespace: "fireboy1919", title: "Add New Hub...", multiple: true)
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

def httpCall(body, uri, mac, evt) {
	def group =  evt.device.getPreferences()["group"]
    def path =  "$uri/gateways/$mac/rgbw/$group"

    log.debug("Sending to ${params['uri']}${path}.")
    try {
        httpPut(path, JsonOutput.toJson(body)) {resp ->
            if(settings.isDebug) { log.debug "Successfully updated settings." }
            //parseResponse(resp, mac, evt)
        }
    } catch (e) {
        log.error "Error sending: $e"
    }
}
