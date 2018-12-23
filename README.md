# SolarPowerExtender

SolarPower java based utility is used to collect and display statistical data from Solar Panels and Inverters. 
This project modifies the utility to also send the collected data to external entity (ex: website).

The original SolarPower I used is 1.14  from http://www.mppsolar.com/manual/SolarPower%20(hybrid)/SolarPower1.14/

Current status:
---------------

 1) The changes only work with the LV version of MPPSolar (U.S. 110v inverters). The relevant class is P18ComUSBProcessor. 
   This can be easily modified to any inverter type however since I don't have access to any of those I can't make the changes.
   
 2) The changes only send specific fields to external source, the future plan is to read a config file with ther required fields.

How do I use this:
------------------

You will need to
1) Modify the P18ComUSBProcessor -> SendPost2WebSsite - > "YOUR WEBSITE HERE" to your actual website that will receive every few seconds 
    all the stats. 
2) Complile the file and inject it to current version of SolarPower.jar (make sure you compile it to the current java version)
3) Modify your lib folder to include "added-libs" from this repo
4) Replace launch.ini with the one from "config" from this repo


When I will have some more time I will make it more generic and configurable without actually compiling files.

What can I do if I don't have this exact inverter but I use SolarPower:
-----------------------------------------------------------------------

You can modify cn.com.voltronic.solar.processor.WorkMonitor.java 

 public void run()
  {
    System.out.println("Process in: "  + this.processer.getClass().getName() + " hash: " + this.processer.getClass().hashCode() + " Serial:" + this.processer.getProtocol().getSerialNo());
    .
    .
    .
        
 This will print to the console which class is used to your own inverter. You can than make similar changes to this class based on the changes to P18ComUSBProcessor.
 

