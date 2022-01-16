# Eclipse SmartHome Build Instructions

Thanks for your interest in the Eclipse SmartHome project!

This project has been recreated to upgrade openHAB-2.2.0 verison to openHAB-2.4.0 
on my QNAP running QTS 4.2.6 which (still) uses Java-SE 1.8.0_144-b0.

We found a suitable source at https://github.com/openhab/smarthome/releases/tag/0.10.0.oh240
as was forked form https://github.com/eclipse-archived/smarthome.
We downloaded the source https://codeload.github.com/openhab/smarthome/zip/refs/tags/0.10.0.oh240
and (re)fixed these so it compiles (except for serial-rxtx which has been
taken out_. No problem as my serial port is attached to an UPS system.

Building and running the project is fairly easy if you follow the steps
detailed below.

Please note that Eclipse SmartHome is not a product itself, but a framework to build solutions on top.
This means that what you build is primarily an artifact repository of OSGi bundles that can be used
within smart home products. Besides this repository, a tool called "Designer" is available. The
Designer can be used for editing configuration files with full IDE support.

1\. Prerequisites
=================

The build infrastructure is based on Maven in order to make it
as easy as possible to get up to speed. If you know Maven already then
there won't be any surprises for you. If you have not worked with Maven
yet, just follow the instructions and everything will miraculously work ;-)

What you need before you start:
- Maven3 from http://maven.apache.org/download.html

Make sure that the "mvn" command is available on your path

2\. Checkout
============

Checkout the source code from GitHub, e.g. by running

git clone https://github.com/eclipse/smarthome.git

3\. Building with Maven
=======================

To build Eclipse SmartHome from the sources, Maven takes care of everything:
- set MAVEN_OPTS to "-Xms512m -Xmx1024m"
- change into the smarthome directory ("cd smarthome“)
- run "mvn clean install" to compile and package all sources

If there are tests that are failing occasionally on your local build, 
run `mvn -DskipTests=true -DskipChecks clean install` instead to skip them.

The p2 repository that contains all bundles as a build result will be available in the folder 
`products/org.eclipse.smarthome.repo/target`.

# How to contribute

If you want to become a contributor to the project, please read about [contributing](https://www.eclipse.org/smarthome/documentation/community/contributing.html) and check our [guidelines](https://www.eclipse.org/smarthome/documentation/development/guidelines.html) first. If you can't wait to get your hands dirty have a look at the open issues where we [need your help](https://github.com/eclipse/smarthome/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22) or one of the [open bugs](https://github.com/eclipse/smarthome/issues?q=is%3Aissue+is%3Aopen+label%3Abug).


4\. Modifications
=================

In this version, we modified the FolderObserver code to aleviate from overloaded, simultaneously change refreshes.
When  the QNAP is configured via SMB, a single filesave may result in two(2) file(system) changes; (one for setting attributes and one for contents).
Each detected change will normally result by openHAB into a refresh of its applicable internal configuration.
As refreshes are also executed using multiple threads in parallelel, this may conflict and overload the QNAP. 

With the modication, the second subsequent observed filechamnge will be ignored.
Note: in case of directly modifying a configuration-fie  directly on the QNAP itself, a sconday file(save) may not
result in a file-item. The exact behavior whether a file change is followed by a refresh, can be observed in the log file.

