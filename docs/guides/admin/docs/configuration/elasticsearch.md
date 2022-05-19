Elasticsearch Configuration
===========================

Elasticsearch powers the external API as well as the administrative user interface of Opencast.

Configuring External Nodes
--------------------------

Opencast's Elasticsearch settings can be found in the `etc/custom.properties` configuration file.

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

Additionally, the following settings can be configured in
`org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg`:
* `index.indentifier`
* `index.name`
* `max.retry.attempts.get`
* `max.retry.attempts.update`
* `retry.waiting.period.get`
* `retry.waiting.period.update`

The identifier defines which index opencast is looking for. This might be interesting if you run an
Elasticsearch cluster and want to follow a naming scheme. But you should be aware that the index actually consists of
multiple subindices whose identifiers will be appended to the base name with an _ (e.g. "opencast_event").
If an index doesn't exist, Opencast will create it. The name is used for logging purposes only.

The max retry attempts and the waiting periods will be used in case of an ElasticsearchStatusException, e.g. if there
are too many concurrent requests. The retry behavior can be configured differently for get and update/delete requests.
This way you could set more retry attempts for index updates because of the more serious consequences if those requests
fail. The waiting period is used to not overwhelm the Elasticsearch with retry requests, making the problem worse. By
default, no retry will be attempted.

Version
-------

Please confer to the [Linux installation guide](../installation/source-linux.md#install-dependencies)
for version information.
