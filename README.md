
# About the Mass USB Comparator 

This is a lightweight self-contained local tool for various semi-automatic tasks performed on big group of similar USB media (hundreds or even thousands of devices). The tool supports backing up, wiping, comparing media pairs.

This is applicable for results from election voting machines, telemetry from industrial devices, data and
metadata from smart cams.

![Parliamentary elections 2022 in Bulgaria, this program in action.](https://i.imgur.com/oe5qTvW.jpg)
# Getting Started 

## Required environment

* Ubuntu 22.4 or higher
* OpenJDK 11 (sudo apt install openjdk-11-jdk)
* Automatic usb mounting (in order to test, add the USB media, wait 5 seconds a and see the mount point with lsblk -l | grep media)

## Build and deployment

Build command:

```
./gradlew clean build
```
If you are building on different platform, have in mind the jar file is self-contained - the entire deployment is just to copy this single jar file:
```
./gradlew clean build && sshpass -p user123 scp build/libs/mass-usb-comparator-1.0.jar user@192.168.1.24:/home/user/Downloads/delta
```

## Most common operations

### Rapid archiver interactive mode
```
sudo java -jar mass-usb-comparator-1.0.jar archive  --folder=base.dir
```
### Rapid hash list extractor
```
java -jar mass-usb-comparator-1.0.jar  hasher --folder=archive --file=hasher2.csv
```
This will generate a common file with the hashes of all master files. The list will be lexicographically sorted by the name of the master file. As result, comaring of two groups of results (the primary and secondary) will be extremely easy. One example application of this process is the independent audit of  the results from the election software.

### Rapid wiper interactive mode
```
sudo java -jar mass-usb-comparator-1.0.jar  delete --folder=dest 
```
Sudo might be necessary as otherwise the program may not have write access to the mounted media. This is not a secure deletion, just a stanard delete of all files from the mounted systems (only the partitions that are recognized and defined in the properties).

### Move data in interactive mode
```
sudo java -jar mass-usb-comparator-1.0.jar  move --folder=dest  
```
This is just combination of "archive" and "wipe" mode.
 
## Multi folder chain replication
You can use multiple folders as destination folder, in order to define the chain, separate the destination parameter by comma, for example the USB must be wiped only after its content is replicated to nasfolder and localfolder, use:
```
sudo java -jar mass-usb-comparator-1.0.jar  move --folder=nasfolder,localfolder
```
