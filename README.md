# MiThings
MiLight / LimitlessLED / EasyBulb integration with SmartThings

https://jjensn.github.io/MiThings/

### Latest news:

#####**02/16/2017:**
I am open sourcing everything as I have moved away from SmartThings in favor of [Home-Assistant](https://github.com/home-assistant/home-assistant). No support with any of it! Please don't ask. It is likely that you will be unable to replicate the same design I have in Azure, however you may use my code as a starting point for whatever you like.

#####**11/16/2016:**
Reached my 150$ hard cap for the month in the cloud. Services will resume next billing period (next week).

#####**10/20/2016:**

~~Moving infrastructure away from DigialOcean/OVH to Azure. Expect some downtime!~~

The move didn't go as smooth as I had planned, and I was forced to roll out version 2.0 early. The old cloud code wasn't handling the 100s of connections from my monitoring suite.

Everyone's lights probably went a little crazy -- sorry about that. I'll remember for next time.

Users: I've made endpoint changes to the SmartThings app. You'll need to edit the line, or do a Git pull from the UI to update.

File: smartapps/cloudsyjared/milight-manager.src/milight-manager.groovy

Line 84: ```uri:  'https://lights.cloudsy.com/v1/',```

Needs to be changed to:

```uri:  'https://api.mithings.pw/v1/'```

There is a new cloud server endpoint, please update your hubs:

**TCP,38899,cloud.mithings.pw**

Issues? Post them on Github.

#####**05/13/2016:**

Introducing breaking changes to the cloud API, all users must update. Infrastructure has been fully changed and is now in a self healing docker host.

***

### Release notes

#####**05/13/2016:**

Formalized API calls, better status checking, updated cloud endpoint (adding support for port 80)

***

### Installation instructions & footnotes:

#####**Installation:**

https://github.com/jjensn/MiThings/blob/master/INSTALL.md

#####**Supports:**

RGBW bulbs ONLY. Features supported are On / Off / Dim (1-100) / Colors

#####**Bug reports:**

Please submit on Github, the logs, and what version of the device code you are using.

***

### Thanks & Contributors!

A big thank you to all the gurus on the SmartThings forums who laid the groundwork before me.
