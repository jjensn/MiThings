# SmartThings-MiLight
MiLight / LimitlessLED / EasyBulb integration with SmartThings

*Latest news:*

05/13/2016: Introducing breaking changes to the cloud API, all users must update.

*Release notes*

05/13/2016: Dockerized the cloud server components, formalized API calls, better status checking, updated cloud endpoint

*Previous releases*

03/03/2016: A major release is being pushed out. All users must upgrade to keep functionality within the coming months.

01/03/2016: The latest changes have been made to the API code which now support routines. I found that, for dimming to work, you need to set the bulbs on AND set the dim level for SmartThings to execute the dim command. This is a SmartThings related problem.

===================

Supports: RGBW bulbs ONLY. Features supported are On / Off / Dim (1-100) / Colors

*There is no support for (the OLD) MiLight RGB-ONLY bulbs or warm/cool lights at this time.*

#### Installation instructions are here: 
#####https://github.com/cloudsyjared/SmartThings-MiLight/blob/master/INSTALL.md


**IMPORTANT**: The cloud API I built is in ALPHA phase. It is not production ready yet. I will be upgrading the code as time permits to add things like authentication, state saving, and other features. It also may crash, since I've only tested it with one MiLight bridge (mine). Don't expect much. If you want to contribute to this project, I need help with the SmartThings code. The source is available on the github repo. Please fork it and submit a pull request when ready.

Bug reports: Please submit on Github, the logs, and what version of the device code you are using.

A big thank you to all the gurus on the SmartThings forums who laid the groundwork before me.

It will be updated as the project matures. You should be able to add it with SmartThings github integration, and update as I update the master branch.
