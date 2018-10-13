# MiThings
MiLight / LimitlessLED / EasyBulb integration with SmartThings

Originally, this was based upon this work:
http://jjensn.github.io/MiThings/


In order to get around problems with UDP not working easily, this has been written to be used in conjunction with this project:
https://github.com/sidoh/esp8266_milight_hub

It is now so completely different as to be unrecognizable from the original project.

Notable features:

- Includes the ability to pair, unpair, and switch to white mode from within the app as well as setting color and mode.

This repo contains four parts to use from within Smartthings.

Publish "MiLight Manager" under "My SmartApps".
Publish "Milight Controller" under "My Device Handlers".

There are two other Smart Apps; both are needed, so include them.
