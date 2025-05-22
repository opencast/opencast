OpenSearch Configuration
========================

> Opencast still works with older versions of Elasticsearch.
> If you use Elasticsearch, please make sure to get the correct version.

OpenSearch powers the external API as well as the administrative user interface of Opencast.


Version
-------

Opencast requires specific versions of OpenSearch/Elasticsearch.
Please make sure to install the correct version:

- OpenSearch 1.x
- Elasticsearch 7.10.2

Additional Plug-ins
-------------------

Opencast needs the `analysis-icu` plugin for OpenSearch/Elasticsearch.
**You might have already installed it if you installed Opencast from the package repositories.**

To manually install the ICU plugin, run the following:

```no-highlight
bin/opensearch-plugin install analysis-icu
```

All-in-One Configuration
------------------------

If you run Opencast as a single node and also run OpenSearch on that node, it might be okay to allow Opencast access
with no further authentication. In that case, just edit `/etc/opensearch/opensearch.yml` and set:

```yaml
plugins.security.disabled: true
```

Please be aware, that this allows anyone who can access that machine to write to OpenSearch!


Configuring External Search Node
--------------------------------

If you run Opencast in a cluster, or you just want to run OpenSearch on a different node, the configuration is a bit
more complicated since you want to require authentication and TLS.

<!-- While this links to the RPM guide, the steps are identical for other installation types: -->
To set-up HTTP Basic authentication and TLS within OpenSearch, follow the [Set up OpenSearch in your environment
](https://opensearch.org/docs/1.3/install-and-configure/install-opensearch/rpm/#step-3-set-up-opensearch-in-your-environment)
guide from OpenSearch.

Alternatively, use a reverse proxy like Nginx or Caddy to terminate TLS and require HTTP Basic authentication.

Next, configure the authentication in Opencast.
Opencast's OpenSearch settings can be found in the `etc/custom.properties` configuration file.
The keys are stilli called `org.opencastproject.elasticsearch.*`, but they will work for OpencSearch!

Relevant configuration keys are:

* `org.opencastproject.elasticsearch.server.hostname`
* `org.opencastproject.elasticsearch.server.scheme`
* `org.opencastproject.elasticsearch.server.port`
* `org.opencastproject.elasticsearch.username`
* `org.opencastproject.elasticsearch.password`


Optional Configuration
----------------------

Additional settings can be configured in `org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg`:

* `index.indentifier`
* `index.name`
* `max.retry.attempts.get`
* `max.retry.attempts.update`
* `retry.waiting.period.get`
* `retry.waiting.period.update`
* `retry.delay.on.startup`

The identifier defines which index opencast is looking for.
This might be interesting if you run an OpenSearch cluster and want to follow a naming scheme.
But you should be aware that the index actually consists of multiple sub-indices whose identifiers will be appended to
the base name with an _ (e.g. `opencast_event`).
If an index doesn't exist, Opencast will create it.
The name is used for logging purposes only.

The max retry attempts and the waiting periods will be used in case of an exception, e.g. if there are too many
concurrent requests or OpenSearch is temporarily unavailable.
The retry behavior can be configured differently for get and update/delete requests.
This way you can set more retry attempts for updates because of the more serious consequences if those requests fail.
The waiting period is used to not overwhelm OpenSearch with retry requests, making the problem worse.
By default, no retry will be attempted.

The `retry.delay.on.startup` defines how long Opencast will wait between retry attempts
when the connection to the index fails on startup. The default is 10 seconds.
