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

Therefore only `admin`, `adminpresentation`, and `allinone` need to connect to Elasticsearch.

`username` and `password` are optional. If configured, requests to Elasticsearch are secured by
HTTP basic authentication (which is unsecure without TLS encryption). Refer to [the Elasticsearch
documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/configuring-stack-security.html)
to properly secure Elasticsearch.

Additionally, the base name of the index opencast is looking for can be configured in
'org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg' under `name`. This might be interesting if you run an
Elasticsearch cluster and want to follow a naming scheme. But you should be aware that this index actually consists of
multiple subindices whose names will be appended to the base name with an _ (e.g. "index_event").
If an index doesn't exist, Opencast will create it.

Version
-------

Please confer to the [Linux installation guide](../installation/source-linux.md#install-dependencies)
for version information.
