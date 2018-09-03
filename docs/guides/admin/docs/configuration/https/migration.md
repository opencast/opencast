Migrating old content to HTTPS
==============================

Opencast will not modify already published events. This means that old publications might still use HTTP as protocol if
it was used before.  Re-processing or re-publishing will update the links but this may not be an option for larger
migrations. For that, the following steps might help.

> Note that you modify stored data directly without any safety nets usually provided by Opencast. You should understand
> what you are doing!

1. Backup your database, and the Solr and Elasticsearch indexes.
    - Elasticsearch: `{data}/index`
    - Solr: `{data}/solr-indexes`
2. Configure Opencast to use HTTPS and test your set-up with a new publication.
3. Put all your nodes into maintenance mode or, at least, do not process any videos.
4. Update the media packages:

        find . -type f -name "*.xml" -exec \
          sed -i 's/http\:\/\/oc-presentation\.example\.com\:80/https:\/\/oc-presentation.example.com/g' {} +`

5. Update database tables. Note that Opencast 5 did change the database table name prefix from `mh` to `oc`:

        UPDATE opencast.oc_assets_snapshot
           SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://oc-presentation.example.com:80',
                    'https://oc-presentation.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://oc-presentation.example.com:80') > 0;
        UPDATE opencast.oc_search
           SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://oc-presentation.example.com:80',
                    'https://oc-presentation.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://oc-presentation.example.com:80') > 0;

6. Rebuild the Elasticsearch indices using the REST endpoint listed in the docs:
   https://admin.opencast.example.com/docs.html?path=/admin-ng/index
7. Remove the search service's Solr index. It usually is located at `solr-indexes/search` but its location really
   depends on `org.opencastproject.solr.dir` and `org.opencastproject.search.solr.dir`
8. Rebuild the Solr indices by re-starting your Opencast node running the search service (usually presentation).
