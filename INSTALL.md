# SmartThings / MiLight Installation Instructions

Requirements: You need to download v5 of the admin tool on this page: http://www.limitlessled.com/dev/, and set the device into "TCP" mode. Also, I highly recommend setting the IP to a static IP so that it doesn't change. Commit the changes by clicking the TCP button, and rebooting the wifi controller. You can verify that the controller is listening on TCP by opening a telnet prompt, and connecting to the IP PORT (ie: Start > run > "cmd", "telnet 10.0.1.5 8899". If you get a blank black screen then that is enough to verify the connection. 

**IMPORTANT REQUIREMENT**: You must set the cloud server setting, "TCP,38899,lights.cloudsy.com"

Make sure once you add this as a device type, you also add a new device with this device type. NetworkID can be anything, I used "livingroom", "kitchen", etc. I wrote the app so that each MiLight group can now be it's own device. You will edit the group as a setting (0 = all bulbs). In total I added 5 devices, 1 for everything, and 4 for the individual groups. This is so I can tell Alexa, "Alexa, turn off the kitchen".

You will need to know your MiLight wifi bridge MAC address. Check your router to see if it lists the MAC. MAC address is formatted as DE:AD:BE:EF:AA:AA
