Database Mail Delivery Agent Daemon
==================================================
DMDAd is the binary distribution of DMDA to run as a daemon on linux systems.

Installation
--------------------------------------
Redhat/CentOS:

- Download dmda-x.x-x.x86_64.rpm from the releases page
- yum localinstall dmda-x.x-x.x86_64.rpm

Other sysv (initd) distributions:

- Build or download the jar
- Download dmdad and config.xml.example
- Put these files in /opt/dmda
- ln -s /opt/dmda/dmdad /etc/init.d

Note: The JVM heap has been limited to 256mb. To increase this value, edit the JVMFLAGS variable in dmdad (once installed, /etc/dmdad). 

Use
--------------------------------------
DMDAd implements the basic service commands which can be invoked "service dmdad <command>":

- start - Starts the service. It will report if the server started successfully but might not wait until the server 
  connects to the database. It's always worth checking the logs to see if all the connection pools for your databases 
  have started up correctly.
- stop - Stops the service if it is started.
- restart - stops and restarts the service. There will be a few seconds of downtime.

Building
--------------------------------------
- Open a shell and `cd` to the DMDA folder (not this bin folder)
- Make the project with maven: `mvn install`
- Copy the jar to the bin folder: `cp ./target/dmda-x.x-SNAPSHOT-jar-with-dependencies.jar ./bin/dmda.jar`
- Copy the sample config file to the bin folder: `cp ./config.xml.sample ./bin/config.xml.sample`
- Create a zipped copy of the bin folder: `tar czf dmda.tar.gz -C ./bin . --exclude='README.md' --exclude='dmda.spec'`
- Follow general RPM build process, putting dmda.tar.gz in your SOURCES folder and putting 
  [dmda.spec](dmda.spec) in SPECS before running `rpmbuild -ba dmda.spec`

**Note:** Be wary of line endings. When getting files from git make sure the spec file and dmdad shell script are using
`\n` line endings otherwise the rpm will not be buildable or installable.
