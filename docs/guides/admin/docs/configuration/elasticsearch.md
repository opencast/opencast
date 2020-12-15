Elasticsearch Configuration
===========================

Elasticsearch powers the external API as well as the administrative user interface of Opencast.

Installing Elasticsearch
------------------------

The [installation](../installation/index.md) documentation contains instructions for installing Elasticsearch along with your Opencast packaging of choice.


Configuring External Nodes
--------------------------

Opencast's Elasticsearch settings can be found in the 'etc/custom.properties' configuration file.

Relevant configuration keys are:
* `org.opencastproject.elasticsearch.server.hostname`
* `org.opencastproject.elasticsearch.server.scheme`
* `org.opencastproject.elasticsearch.server.port`
