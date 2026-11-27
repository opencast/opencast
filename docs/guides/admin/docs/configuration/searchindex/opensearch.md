OpenSearch Configuration
========================

OpenSearch powers the external API as well as the administrative user interface of Opencast.


Version
-------

Opencast requires a specific version of OpenSearch.
Please make sure to install the correct version:

- OpenSearch 3.x

Additional Plug-ins
-------------------

Opencast needs the `analysis-icu` plugin for OpenSearch.
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
](https://docs.opensearch.org/latest/install-and-configure/install-opensearch/rpm/#step-3-set-up-opensearch-in-your-environment)
guide from OpenSearch.

Alternatively, use a reverse proxy like Nginx or Caddy to terminate TLS and require HTTP Basic authentication.

Next, configure the authentication in Opencast.
Opencast's OpenSearch settings can be found in the `etc/custom.properties` configuration file.

Relevant configuration keys are:

* `org.opencastproject.opensearch.server.url`
* `org.opencastproject.opensearch.username`
* `org.opencastproject.opensearch.password`

If your OpenSearch cluster consists of multiple nodes, you can list the URLs of several nodes, separated by commas:

```properties
org.opencastproject.opensearch.server.url=https://opensearch1.example.com:9200,https://opensearch2.example.com:9200
```

Each URL consists of scheme, host and port only, paths are not supported.
All nodes have to belong to the same cluster. Opencast sends each request to one of the nodes and distributes the
requests over all of them. If a node cannot be reached, Opencast sends the request to the next node and skips the
unreachable one until it is back. The cluster itself takes care of distributing the data, so make sure to configure
replicas in OpenSearch if you want to be able to lose a node without losing data.


Optional Configuration
----------------------

Additional settings can be configured in `org.opencastproject.opensearch.index.OpenSearchIndex.cfg`:

* `index.indentifier`
* `index.name`
* `max.retry.attempts`
* `retry.waiting.period`
* `retry.delay.on.startup`

The identifier defines which index opencast is looking for.
This might be interesting if you run an OpenSearch cluster and want to follow a naming scheme.
But you should be aware that the index actually consists of multiple sub-indices whose identifiers will be appended to
the base name with an _ (e.g. `opencast_event`).
If an index doesn't exist, Opencast will create it.
The name is used for logging purposes only.

The max retry attempts and the waiting period will be used if a request fails temporarily, i.e. if OpenSearch cannot be
reached, the connection breaks or times out, or OpenSearch reports a timeout, too many concurrent requests or a server
error like being temporarily unavailable.
They apply to all requests to OpenSearch, including the ones of the search service.
Other errors, e.g. requests OpenSearch rejects as malformed, are never retried.
The waiting period is used to not overwhelm OpenSearch with retry requests, making the problem worse.
By default, no retry will be attempted.

The `retry.delay.on.startup` defines how long Opencast will wait between retry attempts
when the connection to the index fails on startup. The default is 10 seconds.


Query Insights Diagnostics
---------------------------

OpenSearch ships a built-in Query Insights feature that records the most expensive search requests it handles (by
latency, CPU, and memory) into rolling daily indices named `top_queries-<date>-<random>`. It is enabled by default
on a stock OpenSearch installation and is unrelated to Opencast, which neither reads from nor writes to these
indices. Left enabled, a new index is created every day and, by default, retained for 7 days
(`search.insights.top_queries.exporter.delete_after_days`) before being cleaned up automatically.

If you'd rather not have OpenSearch collect this diagnostic data at all, you can disable it with:

```sh
curl -X PUT "$OPENSEARCH_HOST/_cluster/settings" \
  -H "Content-Type: application/json" \
  -d '{
    "persistent": {
      "search.insights.top_queries.latency.enabled": false,
      "search.insights.top_queries.cpu.enabled": false,
      "search.insights.top_queries.memory.enabled": false
    }
  }'
```

This is a persistent cluster setting, so it only needs to be applied once and survives OpenSearch restarts. To
re-enable it later (for example, while investigating a performance issue), set those same keys back to `true`.
