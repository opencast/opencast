Elasticsearch Configuration
===========================

Elasticsearch powers the external API as well as the administrative user interface of Opencast.

Running Elasticsearch
---------------------

When running Elasticsearch, it is mandatory to deploy the same major version as the client library used by Opencast has,
since the client commands will otherwise not match the server. To check the version, take a look at
[the maven dependency declaration for the elasticsearch bundle in the search module](https://github.com/opencast/opencast/blob/develop/modules/search/pom.xml).

For example, to quickly spin up an external Elasticsearch matching the current version using Docker, run

```sh
% docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.9.2
```

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

