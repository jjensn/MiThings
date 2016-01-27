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
    page(name: "mainPage", title: "Manage multiple MiLight hubs", install: true, uninstall: true,submitOnChange: true) {
            section {
                    app(name: "childHubs", appName: "MiThings", namespace: "cloudsyjared", title: "Add New Hub...", multiple: true)
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
            log.info "Installed Hubs: ${child.label}"
    }
}