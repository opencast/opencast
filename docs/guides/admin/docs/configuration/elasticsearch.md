Elasticsearch Configuration
===========================

Elasticsearch powers the external API as well as the administrative user interface of Opencast.

Installing Elasticsearch
------------------------

The [installation](../installation/index.md) documentation contains instructions for installing Elasticsearch along with your Opencast packaging of choice.


Configuring External Nodes
--------------------------

Opencast's Elasticsearch configuration settings are in the core configuration file, which can usually be found at '/etc/opencast/custom.properties'

* `org.opencastproject.elasticsearch.server.hostname`
    * This should be the hostname where Elasticsearch is running.
* `org.opencastproject.elasticsearch.server.scheme`
    * This should match the connection scheme for Elasticsearch.  Leave this commented out unless you have configured https in Elasticsearch.
* `org.opencastproject.elasticsearch.server.port`
    * This is the port that Elasticsearch is listening on.  Leave this commented out unless you have changed the default in Elasticsearch.

