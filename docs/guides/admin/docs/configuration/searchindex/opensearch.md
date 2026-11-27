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

* `org.opencastproject.opensearch.server.hostname`
* `org.opencastproject.opensearch.server.scheme`
* `org.opencastproject.opensearch.server.port`
* `org.opencastproject.opensearch.username`
* `org.opencastproject.opensearch.password`


Optional Configuration
----------------------

Additional settings can be configured in `org.opencastproject.opensearch.index.OpenSearchIndex.cfg`:

* `index.indentifier`
* `index.name`
* `max.retry.attempts.get`
* `max.retry.attempts.update`
* `retry.waiting.period.get`
* `retry.waiting.period.update`
* `retry.delay.on.startup`
* `search.boost.event`
* `search.boost.series`

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

Text searches for events and series, e.g. in the search field of the admin interface, rank the results by relevance.
By default, a match in the title counts three times as much as a match in any other field, like the description or the
series of an event. Without this, a word which is the entire value of a short field would rank higher than the same word
in a longer title.

The `search.boost.event` and `search.boost.series` settings let you tune this separately for events and series.
They are a comma separated list of `field^boost` entries:

```properties
search.boost.event=title^3.0,series_name^1.5,description^0.5
search.boost.series=title^3.0
```

Fields are named as in the index, for example `title`, `description`, `creator`, `subject`, `contributor`, `uid`,
`presenter`, `location` and `series_name` for events, or the fields of extended metadata
(`extended_metadata_<schema>_<field>`).
Fields which are not listed count normally, that is with a boost of 1, and the boost has to be greater than 0.
The default is `title^3.0`. An empty value does not weight any field higher than the others.
The settings only change the order of the results, not which results are found, and they can be changed without
restarting Opencast. Invalid entries and fields which the index does not have are reported in the log.


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
