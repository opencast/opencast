Migrating old content to HTTPS
==============================

This problem of old content is that Opencast has stored and published the old location using HTTP and these published
links will not be changed automatically when changing the base URL configuration. Re-processing or republishing will
help but this is no option for larger migrations. For that, the following steps might help.

Note that you modify stored data directly without any safety nets usually provided by Opencast. You should understand
what you are doing!

1. Backup your database, and the Solr and Elasticsearch indices.
2. Tell your other Opencast systems to use HTTPS for each other,
   or at least for the system delivering the videos to the visitors
   and creating the search indices.
3. Put all your nodes into maintenance mode, or, at least do
   not process any videos.
4. Update the media packages:
   `find . -type f -name "*.xml" -exec \
    sed -i 's/http\:\/\/presentation\.opencast\.example\.com\:80/https:\/\/presentation.opencast.example.com/g' {} +`
5. Update 2 database tables:

        UPDATE opencast.mh_archive_episode
        SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://presentation.opencast.example.com:80',
                    'https://presentation.opencast.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://presentation.opencast.example.com:80') > 0;
        UPDATE opencast.mh_search
        SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://presentation.opencast.example.com:80',
                    'https://presentation.opencast.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://presentation.opencast.example.com:80') > 0;

6. Rebuild the Elasticsearch indices.
   Visit your REST API and push the button:
   https://admin.opencast.example.com/docs.html?path=/admin-ng/index
7. Move the search service's old Solr index away. There might be a directory
   named `solr-indexes/search` but its configuration really depends on
   `org.opencastproject.solr.dir`, or if set in `custom.properties`,
   `org.opencastproject.search.solr.dir`
8. Rebuild the Solr indices by re-starting your Opencast node running the
   search service (usually presentation).
