Log
===

The settings for logging can be found in:

    .../etc/org.ops4j.pax.logging.cfg

Each Log4J appender can be configured in a similar fashion to the graylog example down below. The following
requirements have to be met:
* It needs to be a Log4J appender
* The used bundle needs to be a fragment-bundle

Graylog
-------

To have all log data available and accessible in one central location one can use graylog. A guide to install
graylog can be found [here](http://docs.graylog.org/en/stable/).


Add gelfj-X.X.X.jar (works up to version 1.1.14) to the appropriate folder in the karaf system folder (e.g. `/system/org/graylog2/gelfj/X.X.X/gelfj-X.X.X.jar`)
The directory has the same structure as a maven repository!

It is important that the appender jar is a valid fragment-bundle of `org.ops4j.pax.logging.pax-logging-service`.

That means the jar MANIFEST.MF must contain this section `Fragment-Host: org.ops4j.pax.logging.pax-logging-service`.

Add the following line at the beginning of the startup.properties file:

```
mvn\:org.graylog2/gelfj/X.X.X = 7
```
We use startlevel `7` here, because it's need to be loaded before the `pax-logging`.

Add this custom logging configuration example to the org.ops4j.pax.logging.cfg file

```
# Async wrapper for send queue in case of GELF destination is unavailable
log4j.appender.gelfasync=org.apache.log4j.AsyncAppender
log4j.appender.gelfasync.blocking=false
log4j.appender.gelfasync.bufferSize=20000
log4j.appender.gelfasync.appenders=gelf

# Define the GELF destination
log4j.appender.gelf=org.graylog2.log.GelfAppender
log4j.appender.gelf.graylogHost=<HOSTNAME OF GRAYLOG INPUT>
log4j.appender.gelf.graylogPort=<PORT OF GRAYLOG INPUT>
log4j.appender.gelf.originHost=<NAME OF SERVICE>
log4j.appender.gelf.facility=karaf
log4j.appender.gelf.layout=org.apache.log4j.PatternLayout
log4j.appender.gelf.extractStacktrace=true
log4j.appender.gelf.addExtendedInformation=true
log4j.appender.gelf.includeLocation=true
log4j.appender.gelf.additionalFields={'environment': 'EXAMPLE-ENV', 'application': 'EXAMPLE-APP'}
```
*Note:* The default protocol is UDP to use TCP instead, prefix hostname with `tcp:`.

Add the new appender to the rootLogger

```
log4j.rootLogger=WARN, stdout, osgi:*, gelfasync
```

Example Configuration
---------------------

```
# Define the GELF destination
log4j.appender.gelf=org.graylog2.log.GelfAppender
log4j.appender.gelf.graylogHost=tcp:graylog.opencast.org
log4j.appender.gelf.graylogPort=12290
log4j.appender.gelf.originHost=test.opencast.org
log4j.appender.gelf.facility=karaf
log4j.appender.gelf.layout=org.apache.log4j.PatternLayout
log4j.appender.gelf.extractStacktrace=true
log4j.appender.gelf.addExtendedInformation=true
log4j.appender.gelf.includeLocation=true
log4j.appender.gelf.additionalFields={'environment': 'OPENCAST-TEST-ENV', 'application': 'OC-ADMIN'}
```

You can find further gelf appender documentation [here](https://github.com/t0xa/gelfj).

