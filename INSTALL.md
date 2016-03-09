# SmartThings / MiLight Installation Instructions

Pre-setup & Requirements:

1. You will need to know your MiLight wifi bridge MAC address. Check your router to see if it lists the MAC. MAC address is formatted as **DE:AD:BE:EF:AA:AA**
2. Download v5 of the admin tool on this page: http://www.limitlessled.com/dev/, and set the device into "TCP" mode. 
3. Set the cloud server setting, "TCP,38899,lights.cloudsy.com"
4. Set the IP to a static IP so that it doesn't change. Commit the changes by clicking the TCP button, and rebooting the wifi controller. 
5. And finally, you must have paired the lights (zones) with the wifi controller already using the native app. With the delay in SmartThings execution, this code is not able to pair bulbs with zones. Take note of zones 1-4 for each controller as you will need to label them in the final steps.

Validating:

1. You can verify that the controller is listening on TCP by opening a telnet prompt, and connecting to the IP PORT (ie: Start > run > "cmd", "telnet 10.0.1.5 8899". If you get a blank black screen then that is enough to verify the setup. 

Device Code Installation:

1. Visit your SmartThings developer site, https://graph.api.smartthings.com/ and login.
2. Select "My Device Handlers"
3. Click "Settings"
4. Add my repo information (owner: cloudsyjared, name: SmartThings-MiLight, branch: master)
5. Save
6. Select "Update from Repo" > "SmartThings-Mighlight (master)"  and check the "publish" box.
7. Verify there is a new device listed, named "cloudsyjared : MiLight Controller"

SmartApp Installation:

1. Select "My SmartApps"
3. Click "Settings"
4. Add my repo information (owner: cloudsyjared, name: SmartThings-MiLight, branch: master)
5. Save
6. Select "Update from Repo" > "SmartThings-Mighlight (master)" and check the "publish" box.
7. Verify there are two new SmartApps listed, named "cloudsyjared : MiLight Manager" and "cloudsyjared : MiThings"

SmartThings Setup:

1. Load SmartThings app on phone or device
2. Select "Marketplace"
3. "SmartApps"
4. "My Apps"
5. Select "MiLight Manager" **DO NOT SELECT "MITHINGS"**
6. "Add New Hub.."
7. Select a name for your master switch (this is the switch that will control all 4 zones). For example, I use "Living Room Master" as all 4 zones are in the same room.
8. Enter the HUB mac address (from Pre-req step #1, formatting is IMPORTANT "AA:BB:CC:DD:EE:FF")
9. Select the number of zones you want to control (MiLight supports up to 4 zones).
10. Select "Next" **NOTE: There are usability issues here. Don't hit back. If you make a mistake, uninstall the hub from the main app and retry.**
10. Depending on the integer entered, additional fields will appear. 
11. Enter the name of each zone (these will be your switch names). I suggest names like "Kitchen", "Dining Room", "Hallway"
12. "Done"
13. "Done"

You should soon see (up to) 5 new devices listed under your "Things" section of the app. The 4 zones you entered previously + the master switch.

SmartThings can now see these as lights/switches, and you should be able to use other SmartApps to automate your lights.

###ARE YOU EXPERIENCING PROBLEMS?

That is understandable. Do not post an issue asking for help without posting any logs, you will not get a response from me. Logs can be found under your dev dashboard, by selecting "Live Logging" and enabling "Debug logging" in the MiLight Manager app.
