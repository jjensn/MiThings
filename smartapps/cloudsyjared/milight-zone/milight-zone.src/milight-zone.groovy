definition(
    name: "MiLight Zone",
    namespace: "cloudsyjared/milight-zone",
    author: "Jared Jensen",
    description: "Control individual MiLight zones within SmartThings",
    category: "My Apps",

    // the parent option allows you to specify the parent app in the form <namespace>/<app name>
    parent: "cloudsyjared/milight-master:MiLight Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Add MiLight group to SmartThings", install: true
    //page name: "namePage", title: "Automate Lights & Switches", install: true, uninstall: true
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unschedule()
    initialize()
}

def initialize() {
    app.updateLabel(settings.zoneName)
    parent.initialize()
}

def mainPage () {
	dynamicPage(name: "mainPage") {
        section {
        	
            input(name: "zoneName", type: "text", title: "Zone Name", description: "Name this group", required: true)

            input(name: "wifiHub", type: "capability.colorControl", title: "MiLight hub",
                description: "Select the corresponding WiFi hub", multiple: false, required: true, submitOnChange: true)

            input(name: "zoneNumber", type: "enum", title: "Zone Number", description: "Enter the zone number (1-4) that represents this group", required: true, options: ["1", "2", "3", "4"])

        }
    }
}