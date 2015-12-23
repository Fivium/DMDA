Database Mail Delivery Agent
==================================================
DMDA is a Java application used to store email in a database. It is designed to sit behind a Mail Transfer Agent such as postfix and will store emails in the configured databases based on the recipient's email address's domain. 

This application was developed as an alternative for Apache James. James is a large POP3/IMAP server that does a lot of things including storing email in the database however it is very heavy weight.

The main goals of this tool were:
- Reliability: The server uses HikariCP to manage the database connection pool and will reconnect should the connection drop
- Data integrity: The tool will either reject an email or store it. There will be no dropped emails.
- Light weight: The tool is small, fast and easy to configure.


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

Use
--------------------------------------
dmdad implements the basic service commands which can be invoked "service dmdad command":
- start - Starts the service. It will report if the server started successfully but might not wait until the server connects to the database. It's always worth checking the logs to see if all the connection pools for your databases have started up correctly.
- stop - Stops the service if it is started.
- restart - stops and restarts the service. There will be a few seconds of downtime.

Requirements
--------------------------------------

- Java : >=1.8

Building
--------------------------------------

Import the maven project by importing pom.xml into your IDE of choice. Maven will download all dependencies apart from the oracle drivers which will have to be added manually. The jar can then be built in the normal way.
