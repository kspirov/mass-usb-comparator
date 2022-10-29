Mass USB Comparator (readme WIP)

# Purpose

A lightweight self-contained local tool for various semi-automatic tasks performed on big group of similar USB  media,
(hundreds or even thousands of devices). Includes backing up, wiping, comparing media peers.

This is applicable for results from election voting machines, telemetry from industrial devices, data and
metadata from smart cams.

# Getting Started (WIP)

## Required environment

* Ubuntu 22.4 or higher
* OpenJDK 11 (sudo apt install openjdk-11-jdk)
* Automatic usb mounting (in order to test, add the USB media, wait 5 seconds a and see the mount point with lsblk -l | grep media)

## Build and deployment

Build command:

```
./gradlew clean build
```

If you are building on different platform, have in mind the jar file is self-contained - the entire deployment is
just to copy this single jar file
```
./gradlew clean build && sshpass -p user123 scp build/libs/mass-usb-comparator-1.0.jar user@192.168.1.24:/home/user/Downloads/delta
```

## Most common operations

### Interactive media archiving (TBD, more details needed)

Rapid archiver interactive mode
```
java -jar mass-usb-comparator-1.0.jar  archive  --folder=base.dir
```
Rapid hash list extractor
```
java -jar mass-usb-comparator-1.0.jar  hasher --folder=archive --file=hasher2.csv

```

Rapid wiper interactive mode
```
TBD
```


# Project customization and properties (TBD)
