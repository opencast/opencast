Elasticsearch Configuration
===========================

Elasticsearch is powering the external API as well as the administrative user interface of Opencast.

Running Elasticsearch
---------------------

When running Elasticsearch, it is mandatory to deploy the same major version as the client library used by Opencast has,
since the client commands will otherwise not match the server. To check the version, take a look at
[the maven dependency declaration for the Elasticsearch bundle in the search module](https://github.com/opencast/opencast/blob/develop/modules/search/pom.xml).

For example, to quickly spin up an external Elasticsearch matching the current version using Docker, run

```sh
% docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.9.2
```

Configuring External Nodes
--------------------------

To configure an external node, set the server's address in `etc/custom.properties`:

```properties
org.opencastproject.elasticsearch.server.hostname=localhost
org.opencastproject.elasticsearch.server.scheme=http
org.opencastproject.elasticsearch.server.port=9200
```

Opencast will now use your Elasticsearch instance.
