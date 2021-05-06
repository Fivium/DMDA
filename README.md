![DMDA](https://raw.githubusercontent.com/Fivium/DMDA/master/dmda-logo.png)

DMDA (Database Mail Delivery Agent) is a Java application used to store email in a database. It is designed to sit behind a Mail Transfer Agent such 
as postfix and will store emails in the configured databases based on the recipient's email address's domain. 

This application was developed as an alternative for Apache James. James is a large POP3/IMAP server that does a lot of 
things including storing email in the database however it is very heavy weight.

The main goals of this tool were:

- Reliability: The server uses HikariCP to manage the database connection pool and will reconnect should the connection 
  drop
- Data integrity: The tool will either reject an email or store it. There will be no dropped emails.
- Light weight: The tool is small, fast and easy to configure.

Use
--------------------------------------
See the bin folder for creating a linux daemon for DMDA or see the releases page for an RPM distribution.
Instructions on daemon installation and running can be found inside the bin folders [README](bin/README.md).

For local running run `mvn install` first and have a config.xml (see the config.xml.sample for information) in the 
folder you run the following command:

`java -jar ./target/dmda-x.x-SNAPSHOT-jar-with-dependencies.jar`


Configuration
--------------------------------------
All application configuration is defined in the `config.xml` file, with the exception of anti-virus connection information which can also be configured via the following environment variables.

|Name|Description|
|---|---|
|`DMDA_AV_MODE`|The anti-virus mode. One of `clamd` or `none`.|
|`DMDA_AV_SERVER`|The anti-virus server hostname.|
|`DMDA_AV_PORT`|The anti-virus server port.|
|`DMDA_AV_TIMEOUT_MS`|The anti-virus scan timeout in milliseconds.|

If anti-virus configuration exists in both environment variables and the XML config file, the configuration options set in the environment variables will take precedence.

See the [sample config file](https://github.com/Fivium/DMDA/blob/master/config.xml.sample) for all other options.

Health Checks
--------------------------------------
DMDA provides the following basic application health checks, which can be queried over HTTP.
These are disabled by default, but when enabled the following endpoints become available:

|Endpoint|Description|Returns|
|---|---|---|
|`/smtp-status`|Checks that the SMTP server is running.|An HTTP 200 OK if the SMTP server is running, otherwise a 503 Service Unavailable.|
|`/db-status?security-token=foobar`|Checks that a valid connection can be made to each database in the database_list. Note that a security token needs to be provided to access this endpoint, and it must match the one defined in the config.|A list of databases (identified by DB name and JDBC connection URL) and a boolean value indicating if the application can establish a valid connection to the database.|
|`/av-status`|Checks that a valid connection can be made to the virus scanner.|An HTTP 200 OK if a connection to the AV service can be established, or a 503 Service Unavailable otherwise.|

Note that these endpoints should only be exposed locally to monitoring tools such as Monitis, Zabbix, Nagios etc. or used within Docker containers as HEALTHCHECK directives. They are not designed to be accessible from the internet.

Requirements
--------------------------------------
- Java : >=1.8