Elasticsearch Configuration
===========================

Elasticsearch powers the external API as well as the administrative user interface of Opencast.

Configuring External Nodes
--------------------------

Opencast's Elasticsearch settings can be found in the 'etc/custom.properties' configuration file.

Relevant configuration keys are:

* `org.opencastproject.elasticsearch.server.hostname`
* `org.opencastproject.elasticsearch.server.scheme`
* `org.opencastproject.elasticsearch.server.port`
* `org.opencastproject.elasticsearch.username`
* `org.opencastproject.elasticsearch.password`

Threfore only `admin`, `adminpresentation`, and `allinone` need to connect to Elasticsearch.

Version
-------

Please confer to the [Linux installation guide](../installation/source-linux.md#install-dependencies)
for version information.
