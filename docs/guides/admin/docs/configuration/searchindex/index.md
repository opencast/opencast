Search Indexes
==============

Opencast comes with multiple search indexes which act both as a cache and as a fast way to perform full text searches on
metadata. By default, the Solr search indexes are created automatically and no additional external software is required.

For OpenSearch/Elasticsearch, a separate installation is required since Opencast version 9.0.  Opencast 12 added support
both OpenSearch and Elasticsearch, with Opencast 14 now preferring OpenSearch by default.  We are planning on
deprecating Elasticsearch support with Opencast 15, and removing it with Opencast 16.  New installs should use OpenSearch.

While this works well, all indexes can be deployed separately. This comes with the obvious drawback of a harder
deployment but has also a few advantages like a smaller core system or being able to have some service redundancies
which would not be possible otherwise.

---

- Solr is mostly powering older services and replacing this index type is planned for the future. But for now it is
  still the back-end for the search service (LTI and engage tools), the workflow service and the series service.

    [Solr Configuration Guide](solr.md)

- OpenSearch/Elasticsearch powers the external API as well as the administrative user interface of Opencast.  Pick one of the following

    [OpenSearch Configuration Guide](../opensearch.md)
    [Elasticsearch Configuration Guide](../elasticsearch.md)

---
