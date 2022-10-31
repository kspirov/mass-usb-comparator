
# About the Mass USB Comparator 

This is a lightweight self-contained local tool for various semi-automatic tasks performed on big group of similar USB  media, (hundreds or even thousands of devices). The tool supports backing up, wiping, comparing media peers.

This is applicable for results from election voting machines, telemetry from industrial devices, data and
metadata from smart cams.

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
If you are building on different platform, have in mind the jar file is self-contained - the entire deployment is just to copy this single jar file
```
./gradlew clean build && sshpass -p user123 scp build/libs/mass-usb-comparator-1.0.jar user@192.168.1.24:/home/user/Downloads/delta
```

## Most common operations

### Rapid archiver interactive mode
```
java -jar mass-usb-comparator-1.0.jar  archive  --folder=base.dir
```
### Rapid hash list extractor
```
java -jar mass-usb-comparator-1.0.jar  hasher --folder=archive --file=hasher2.csv
```
This will generate a common file with the hashes of all master files. The list will be lexicographically sorted by the name of the master file. As result, comaring of two groups of results (the primary and secondary) will be extremely easy. One examople application of this process is the independent audit of the election software.

### Rapid wiper interactive mode
```
sudo java -jar mass-usb-comparator-1.0.jar  hasher --folder=delete --file=hasher2.csv
```
Sudo might be necessary as otherwise the program may not have write access to the mounted media. This is not a secure deletion, just a stanard delet ob all filew from the mounted systems.

### Move data in interactive mode
```
sudo java -jar mass-usb-comparator-1.0.jar  hasher --folder=delete --file=hasher2.csv
```
This is just combination of "archive" and "wipe" mode
 
# Project customization and properties (TBD)
