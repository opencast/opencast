# Domain change/HTTPS migration

If you move your Opencast server to a new domain or if you switch from HTTP to HTTPS, you will change
`org.opencastproject.server.url` in `etc/custom.properties`.
But if your Opencast already processed events, these events still contain the old URL in various places.
So in order for old events to work correctly, you have to migrate them.
Re-processing or re-publishing will update the links but this may not be an option if you have many events.
In that case, the following steps might help.

> Note that you modify stored data directly without any safety nets usually provided by Opencast. You should understand
> what you are doing!

1. Backup your database, and the Solr and OpenSearch/Elasticsearch indexes!
    - OpenSearch/Elasticsearch: `{data}/index`
    - Solr: `{data}/solr-indexes`
2. Change `org.opencastproject.server.url` and test your set-up with a new publication (i.e. uploading a video).
3. Put all your nodes into maintenance mode or, at least, do not process any videos.
4. Update the media packages on disk.
   Run the following in the folder configured in `org.opencastproject.storage.dir` (usually your NFS).
   Note: only the subfolders `archive/` and `downloads/` will be affected by this, so you can also run it in those two
   individually.
   This is especially useful to reduce the execution time of the command if your NFS contains other, Opencast-unrelated
   files (e.g. `.snapshots/`).

         # This changes the domain AND from http to https. Carefully adjust the command as needed!
         # Your old URL may or may not have the port explicitly listed.  Check the previous value of org.opencastproject.server.url and match that.
         find . -type f -name "*.xml" -exec \
            sed -i 's#http://old-domain.example.com:80#https://new-domain.example.com#g' {} +

5. Update database tables.
   Note: there more than the following two tables containing the old domain name, but only these two are relevant.

         -- Reminder: Your old domain name may or may not have its port listed.  Use old value for org.opencastproject.server.url here.
         UPDATE oc_assets_snapshot SET mediapackage_xml = REPLACE(
         
            mediapackage_xml, 'http://old-domain.example.com:80', 'https://new-domain.example.com');
         UPDATE oc_search SET mediapackage_xml = REPLACE(
            mediapackage_xml, 'http://old-domain.example.com:80', 'https://new-domain.example.com');

6. Remove the search service's Solr index. It usually is located at `solr-indexes/search` but its location really
   depends on `org.opencastproject.solr.dir` and `org.opencastproject.search.solr.dir`
7. Rebuild the Solr indices by re-starting your Opencast node running the search service (usually presentation).
8. Rebuild the OpenSearch indices using the REST endpoint listed in the docs:
   https://admin.opencast.example.com/docs.html?path=/index

Note: If the solr index does not repopulate by itself please check if your nodes are still in maintenance mode and
reactivate them.
