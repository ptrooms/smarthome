# Eclipse SmartHome Build Instructions

Thanks for your interest in the Eclipse SmartHome project!

This project has been recreated to upgrade openHAB-2.2.0 verison to openHAB-2.4.0 
on my QNAP running QTS 4.2.6 which (still) uses Java-SE 1.8.0_144-b0.

We found a suitable source at https://github.com/openhab/smarthome/releases/tag/0.10.0.oh240
as was forked form https://github.com/eclipse-archived/smarthome.

We downloaded the source https://codeload.github.com/openhab/smarthome/zip/refs/tags/0.10.0.oh240
and (re)fixed these so it compiles (except for serial-rxtx which has been
taken out_. No problem as my single qnap serial port is attached to an UPS system.

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
$ git clone https://github.com/eclipse/smarthome.git

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


4\. openHAB 2.4.0 generate

Summary how to (re)generate object without discontinued repo's.
using a localised maven / .m2 repository loaded with all required resources.

We are in the proces to eliminate the discontinued repo's below.
- http://www.eclipse.org/SmartHome
- https://api.bintray.com/maven/openhab
- https://repo.eclipse.org/content/repositories/smarthome-snapshots/
- ttps://jcenter.bintray.com
- http://eclipse.github.io/smarthome/third-party/m2-repo/
- https://openhab.jfrog.io/openhab/..
- https://dl.bintray.com/openhab/...
This task is complex as we have many online references.

18mar24: based on out .m2  repository, we follow the procedure below:
- esh-smarthome: $ mvn -o clean install -DskipTests=true -Dcheckstyle.skip
- openhab1-addons-1.13.0: $ mvn -o clean install -DskipTests=true -Dcheckstyle.skip
- addons_R240: $ mvn -o clean install -DskipTests=true -Dcheckstyle.skip
- habpanel_R240: $ mvn -o clean install -DskipTests=true -Dcheckstyle.skip
- openhab-distro:
- - $ mvn -o clean
- - $ mvn -o compile -rf features (install will fail)
- - $ mvn -o compile -pl distributions/openhab (install will fail)
- - $ mvn -o install -pl distributions/openhab-addons
- - $ mvn -o install -pl distributions/openhab-addons-legacy
- - $ mvn -o compile -pl distributions/openhab-verify (install will fail)

result is
a) karaf runtime : ... openhab-distro/distributions/openhab/target/assembly
b) openhab distro: ... openhab-distro/distributions/openhab/target/classes
c) legacy kar file: ... openhab-distro/distributions/openhab-addons-legacy/target/openhab-addons-legacy-2.4.0.kar
d) addon2 kar file: ... openhab-distro/distributions/openhab-addons/target/openhab-addons-2.4.0.kar

The above must be reconstructed to specific platform f this is different then standard windows an/od linux.

In case of a missing jar of wrong version, these can be retrieved from https://mvnrepository.com/artifact/
and, via the sequence below, imported to your localised .m2 repository.

For example to resolved a missing artifact/version like: 'org.openhab.util:pax-web-patch:jar:1.0.0'
we download this from https://mvnrepository.com/artifact/org.openhab.util/pax-web-patch/1.0.0
to a local download file: pax-web-patch-1.0.0.jar
and install this into the .m2 repo via 
	$ mvn install:install-file \
	   -Dfile=pax-web-patch-1.0.0.jar \
	   -DgroupId=org.openhab.util \
	   -DartifactId=pax-web-patch \
	   -Dversion=1.0.0 \
	   -Dpackaging=jar \
	   -DgeneratePom=true \
	   -DcreateChecksum=true 

5\. Modifications
=================

18mar24: we revised a bit to make (-o-ffline compilation working, as lots of artifacts are renewed)
the nett- result is successfull (and useful)

16jan16-18mar24: we revised a couple of bundles, summarized

smarthome:
- JsonStorage.java - fixed logger on 
- FolderObserver.java - see below for reload only when size of file is changed FolderObserver.java
- org.eclipse.smarthome.binding.mqtt.generic: add isPostOnly & allow nullable's
- org.eclipse.smarthome.binding.mqtt.generic: fix regex chaining n (ChannelState & ChannelStateTransformation)

addon version1:
- org.openhab.binding.caldav-command: fix logger for debug  (CalDavActivator's)
- org.openhab.io.caldav: Fix BETWEEN (EventUtils)
- MochadX10CommandParser.java: fix/enhance X10 PL & RF

addon version1:
- org.openhab.binding.network : display dhcp'ed connection to ease visual detection
- org.openhab.binding.avmfritz2: added to solved singleton failures that else would block other jetty httpclients
- org.openhab.binding.tibber:  @NonNullByDefault  fix null-hell (TibberHandler.java)

18jan22: In addtion we found that org.eclipse.smarthome.automation.module.timer does not compile
due to in accessible or disfunctional org.eclipse.smarthome.core.scheduler. This means that
we cannot use schedules inside rules. It appears that during the transition to 2.5.0 @openhab, this
issue was flunked.

jan2016: In this oh240 version, we modified the FolderObserver code to aleviate from overloaded, simultaneously change refreshes.
When  the QNAP is configured via SMB, a single filesave may result in two(2) file(system) changes; (one for setting attributes and one for contents).
Each detected change will normally result by openHAB into a refresh of its applicable internal configuration.
As refreshes are also executed using multiple threads in parallelel, this may conflict and overload the QNAP. 

With the modication, the second subsequent observed filechange will be ignored if the number of bytes int he file are the same.
If an expected change does not result in a refresh, consider adding a byte.
The exact behavior whether a file change is followed by a refresh, can be observed in the log file.

