Logging
=======

The settings for logging can be found in `etc/org.ops4j.pax.logging.cfg`.

Each Log4J appender can be configured in a similar fashion to the Graylog example down below.
The following requirements have to be met:

- It needs to be a Log4J appender
- The used bundle needs to be a fragment-bundle

Log Rotation
------------

By default, `opencast.log` is written by a plain `File` appender that never rotates or truncates it on its own, so
it grows without bound for as long as Opencast keeps running. How to handle that is deliberately left to the
administrator, since the right approach — rotate by size, by time, how many old logs to keep, whether to compress
them — depends on the deployment, and Opencast does not assume one by default.

Installing from certain packages pre-configures this for you: the [RPM
packaging](https://github.com/opencast/opencast-rpmbuild) ships an `/etc/logrotate.d` entry that rotates Opencast's
logs weekly, keeping 52 compressed rotations. Other installation methods have no such rotation configured and need
one of the approaches below.

To enable size-based rotation, replace the `File` appender with a `RollingRandomAccessFile` one in
`etc/org.ops4j.pax.logging.cfg`:

```
# Rolling file appender. Rotates once opencast.log exceeds the configured size.
log4j2.appender.out.type = RollingRandomAccessFile
log4j2.appender.out.name = File
log4j2.appender.out.fileName = ${karaf.data}/log/opencast.log
log4j2.appender.out.filePattern = ${karaf.data}/log/opencast.log.%i
log4j2.appender.out.append = true
log4j2.appender.out.layout.type = PatternLayout
log4j2.appender.out.layout.pattern = ${log4j2.pattern}
log4j2.appender.out.policies.type = Policies
log4j2.appender.out.policies.size.type = SizeBasedTriggeringPolicy
log4j2.appender.out.policies.size.size = 50MB
```

`policies.size.size` is the threshold at which `opencast.log` is rotated to `opencast.log.1` (and so on); adjust it
to fit the deployment. Log4j2 also supports capping how many rotated files are kept (via a `DefaultRolloverStrategy`
and its `max` setting), time-based rotation (`TimeBasedTriggeringPolicy`), and combining multiple policies — see
[Log4j2's own `RollingFile` appender
documentation](https://logging.apache.org/log4j/2.x/manual/appenders.html#RollingFileAppender) for the full set of
options.

Graylog
-------

To have all log data available and accessible in one central location one can use Graylog.
A guide to install Graylog can be found [in Graylog's documentation](https://docs.graylog.org/docs/installing).


Add `gelfj-X.X.X.jar` (works up to version 1.1.14) to the appropriate folder in the Karaf system folder
(e.g. `/system/org/graylog2/gelfj/X.X.X/gelfj-X.X.X.jar`)
The directory has the same structure as a maven repository!

It is important that the appender jar is a valid fragment-bundle of `org.ops4j.pax.logging.pax-logging-service`.

That means the jar's `MANIFEST.MF` must contain this section `Fragment-Host: org.ops4j.pax.logging.pax-logging-service`.

Add the following line at the beginning of the `startup.properties` file:

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

### Example Configuration

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

You can find further GELF appender documentation [in the gelfj GitHub repository](https://github.com/t0xa/gelfj).
