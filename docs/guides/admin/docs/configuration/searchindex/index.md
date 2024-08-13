Search Indexes
==============

Opencast uses OpenSearch/Elasticsearch both as a cache and as a fast way to perform full text searches on metadata.

For OpenSearch/Elasticsearch, a separate installation is required since Opencast version 9.0.  Opencast 12 added support
both OpenSearch and Elasticsearch, with Opencast 14 and newer preferring OpenSearch by default, however Elasticsearch
will still function.  OpenSearch 1.x is required for now, which preserves compatibility with existing Elasticsearch
indexes.  We are planning on deprecating Elasticsearch support with Opencast 15, and removing it with Opencast 18.
New installs should use OpenSearch 1.x.

[OpenSearch/Elasticsearch Configuration Guide](elasticsearch.md)
