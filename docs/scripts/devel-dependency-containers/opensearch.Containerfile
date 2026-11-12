FROM docker.io/opensearchproject/opensearch:3
RUN bin/opensearch-plugin install analysis-icu
