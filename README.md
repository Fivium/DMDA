Database Mail Delivery Agent
==================================================
DMDA is a Java application used to store email in a database. It is designed to sit behind a Mail Transfer Agent such as postfix and will store emails in the configured databases based on the recipient's email address's domain. 

This application was developed as an alternative for Apache James. James is a large POP3/IMAP server that does a lot of things including storing email in the database however it is very heavy weight.

The main goals of this tool were:
- Reliability: The server uses HikariCP to manage the database connection pool and will reconnect should the connection drop
- Data integrity: The tool will either reject an email or store it. There will be no dropped emails.
- Light weight: The tool is small, fast and easy to configure.


Use
--------------------------------------
Copy config.xml.sample to config.xml and modify it to point it at your database, configure your virus scanner etc.
Run jar


Requirements
--------------------------------------

- Java : >=1.6

Maven needs to sync out the dependencies but builds currently you need to do a regular jar build to include the OJDBC driver in /lib
