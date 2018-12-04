Elasticsearch Configuration
===========================

Elasticsearch is powering the external API as well as the administrative user interface of Opencast. By default,
Opencast will start its own, internal Elasticsearch node as part of the admin distribution and no special configuration
or deployment is required.

Nevertheless, it is possible to connect Opencast to an external Elasticsearch instead. Reasons for this may be:

- Ability for redundant services
- Lightweight admin distributions
- Cluster set-ups


Running Elasticsearch
---------------------

When running Elasticsearch, it is recommended to deploy the same version Opencast includes as the client commands may
otherwise not match the server. To check the version, take a look at [the maven dependency declaration for the
elasticsearch bundle in the search module](https://github.com/opencast/opencast/blob/develop/modules/search/pom.xml).

For example, to quickly spin up an external Elasticsearch matching the current version using Docker, run:

```sh
% docker run -p 9200:9200 -p 9300:9300 -e discovery.type=single-node  elasticsearch:1.7.6
```

This will already give you a running cluster with the name `elasticsearch`. Note that the cluster name is important and
you will need it later for the configuration.


Configuring External Nodes
--------------------------

To configure an external node, set the server's address in `etc/custom.properties`:

```properties
org.opencastproject.elasticsearch.server.address=127.0.0.1
```

Once this is set, Opencast will not launch its own internal Elasticsearch anymore. If necessary, you can also specify a
custom port in this configuration file.

Next, configure the correct cluster name for all indexes in `etc/index/*/settings.yml`. Make sure that the correct
cluster name is set in the configuration file of each index:

```yml
cluster.name: opencast
```

Opencast will now use the external Elasticsearch.
