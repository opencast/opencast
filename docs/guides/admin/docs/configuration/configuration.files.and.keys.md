# Configuration Files and Keys

This document will help you get an overview of the existing Opencast configuration files and watch folders.

## Watch Folders
Watch folders allow you to quickly augment Opencast's existing behavior, simply by adding new configuration files. Below is a list of default watch folders.

### Encoding Profiles
Default and custom encoding profiles should be placed in <opencast_home>/etc/encoding. The file names should follow the pattern *.properties.

### Atom and RSS Feeds
Default and custom feed definitions should be placed in <opencast_home>/etc/feeds. The file names should follow the pattern *.xml.

### Workflow Definitions
Custom workflow definitions should be placed in <opencast_home>/etc/workflows. The file names should follow the pattern *.xml.

### Inbox
Media files placed in <opencast_home>/inbox will be copied to the Opencast working file repository and made available in the Admin Tools > Uploading Recording user interface, via the File Location menu.

*Queued media files are write-protected, copied to the working file repository and then deleted from the inbox.*

## Configuration Files
Configuration files use key-value pairs to determine Opencast's runtime behavior.

### <opencast_home>/etc/custom.properties
*custom.properties* is the primary Opencast configuration file.

#### Bundle Configuration Properties
|Name|Description|Default|
|----|-----------|-------|
|org.osgi.service.http.port|Port number of http services|8080| 
|org.osgi.service.http.port.secure|Port number of https services|8443|
|org.osgi.service.http.secure.enabled|Toggle https services|false|

Alternatively, edit Apache's httpd.conf to reroute Opencast to port 80:

    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/

#### Opencast Configuration

#####General

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.server.url|URL of Opencast http services|http://localhost:8080 e.g. org.opencastproject.server.url=http://opencast1.telavivuniv.org|
|org.opencastproject.serviceregistry.url|URL of the service registry, if not running in the local JVM|This value is needed only if the host does not have access to the relational database (see JDBC settings below).  This is common for capture agents, but not for servers.|
|org.opencastproject.server.maxload|The maximum number of concurrent jobs this server should attempt to run|Defaults to the number of processing cores available to the JVM, as reported by Runtime.availableProcessors()|
|org.opencastproject.storage.dir|Base path of Opencast data file structure. Note: This must be changed on a production server so that media will not disappear on reboot.|${java.io.tmpdir}/opencast|
|org.opencastproject.security.config|Path to Opencast security configuration file|conf/security.xml|
|org.opencastproject.security.digest.user|Digest authentication privileged user name|opencast_system_account|
|org.opencastproject.security.digest.pass|Digest authentication privileged user password|CHANGE_ME|
|org.opencastproject.anonymous.feedback.url|The project-wide feedback service, used to help the community understand how Opencast is deployed "in the wild.  For more information on this feature, see the Project Feedback Service documentation| No Feedback |


##### Streaming

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.streaming.url|URL of Red5 streaming server|rtmp://localhost/opencast-engage|
|org.opencastproject.streaming.directory|Path to published streaming media|${org.opencastproject.storage.dir}/streams|
|org.opencastproject.streaming.flvcompatibility|Some newer streaming server versions expect an "flv:" tag within the rtmp URL. Not every RTMP-streaming server is compatible with this (i.e. nginx), so this is the compatibility mode to the old syntax. true = without "flv:" tag - old syntax, false = with "flv:" tag - new syntax|false|

##### Database

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.db.ddl.generation|Toggle generation of database description language|true|
|org.opencastproject.db.vendor|Database vendor type|HSQL|
|org.opencastproject.db.jdbc.driver|Fully-qualified name of database adapter|org.h2.Driver|
|org.opencastproject.db.jdbc.url|URL of database, using the jdbc:<type>:// protocol|jdbc:h2:${org.opencastproject.storage.dir}/db;LOCK_MODE=1;MVCC=TRUE|
|org.opencastproject.db.jdbc.user|Privileged database user name|sa|
|org.opencastproject.db.jdbc.pass|Privileged database user password|sa|

##### Distribution

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.download.directory|Path to published progressive media|${org.opencastproject.storage.dir}/downloads|

##### Search

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.search.solr.dir|Path to search index files|${org.opencastproject.storage.dir}/searchindex|

##### Working File Repository

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.file.repo.path|Path to Opencast working file repository|${org.opencastproject.storage.dir}/files|

##### Workspace

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.workspace.rootdir|Path to the workspace root directory|${org.opencastproject.storage.dir}/workspace|

##### Workflow

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.workflow.default.definition|ID of default workflow definition|full|
|org.opencastproject.workflow.solr.dir|If using a local workflow search index, this is the directory to use for storage of the embedded solr server's index files|${org.opencastproject.storage.dir}/workflow|
|org.opencastproject.workflow.solr.url|If using a remote workflow search index, this is the URL to the remote solr server.  Configure either this value or org.opencastproject.workflow.solr.dir but not both.| |
 
##### Inbox

|Name|Description|Default|
|----|-----------|-------|
|org.opencastproject.inbox.threads|The number of mediapackages to ingest concurrently from the watch folder|1|


### <opencast_home>/etc/load/org.opencastproject.organization-mh_default_org.cfg

**org.opencastproject.organization-mh_default_org.cfg** defines the default tennant in a multi tennant setup. If you don't have a multi-tennant setup this is the file for you to edit.

#### Servers and URLs

|Name|Description|Default|
|----|-----------|-------|
|prop.org.opencastproject.admin.ui.url|The URL to the administrative tools.  This sets the URL for the "Adminstrative Tools" link on the Opencast welcome page.|The local server URL, or ${org.opencastproject.server.url}|
|prop.org.opencastproject.engage.ui.url|The URL to the engage tools.  This sets the URL for the "Engage Tools" link on the Opencast welcome page.|The local server URL, or ${org.opencastproject.server.url}|


## Security

### <opencast_home>/etc/security/mh_default_org.xml

*mh_default_org.xml* defines the Opencast access policy using the [Spring Security](http://static.springsource.org/spring-security/site/) framework XML schema.
