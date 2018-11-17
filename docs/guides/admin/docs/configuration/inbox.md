InboxScannerService
=====================

Overview
--------

Besides ingesting media packages using the REST service of the IngestService,
dedicated inbox directories located in the file system can be scanned by
Opencast. This, for example, allows adding media packages to Opencast by copying
it to a specific location using scripting/SFTP without the need for any HTTP
traffic. Opencast periodically scans the specified location for new files.

Each directory may result in digest for a separate organization or with a
different default workflow.

Step 1: Configure an InboxScannerService
-----------------------------------------------

Adjust
[etc/org.opencastproject.ingest.scanner.InboxScannerService-inbox.cfg](https://github.com/opencast/opencast/blob/develop/etc/org.opencastproject.ingest.scanner.InboxScannerService-inbox.cfg).

The `-inbox` suffix of the file name is variable and multiple files can
be created with different settings for different directories to be watched.


Step 2: Testing the inbox
-----------------------------------------

In order to test the inbox scanner service, either put valid media package
zip or a single media file into the scanned directory.

Note that even if the poll interval is small, it may take a little longer until
the media package is visible in the admin interface because extracting and/or
copying the media files will take some time.

Example media package
-----------------------------------------

Media packages contain media files and metadata files describing them.
Opencast is able to generate media packages using the
[ZipWorkflowOperation](../workflowoperationhandlers/zip-woh/).

Create the follwing files:

_manifest.xml_
```xml
<?xml version="1.0" encoding="utf-8"?>
<mediapackage xmlns:oc="http://mediapackage.opencastporject.org">
    <title>A media package courtesy by the inbox scanner.</title>
    <media>
        <track id="track-1" type="presenter/source">
        <url>presenter.mkv</url>
        </track>
        <track id="track-2" type="presentation/source">
        <url>presentation.mkv</url>
        </track>
    </media>
    <metadata>
        <catalog id="catalog-1" type="dublincore/episode">
            <mimetype>text/xml</mimetype>
            <url>episode.xml</url>
        </catalog>
    </metadata>
</mediapackage>
```

Note: You can create a valid empty media package using the
`/ingest/createMediaPackage` REST endpoint.

_episode.xml_
```xml
<?xml version="1.0" encoding="utf-8"?>
<dublincore
   xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/"
   xmlns:dc="http://purl.org/dc/elements/1.1/"
   xmlns:dcterms="http://purl.org/dc/terms/"
   xmlns:oc="http://www.opencastproject.org/matterhorn"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance/"
   xsi:schemaLocation="http://www.opencastproject.org http://www.opencastproject.org/schema.xsd">
<dcterms:title>A media package courtesy by the inbox scanner.</dcterms:title>
</dublincore>
```

_presentation.mkv_
```
binary video file of your choice
```

_presenter.mkv_
```
binary video file of your choice
```

Then run:
`zip -j --compression-method store oc-package.zip /path/to/files/*`

And move the zip media package file to your inbox directory:
`mv oc-package.zip /path/to/inbox/`

You will now see Opencast working on your file:
```
admin_1         | 2016-11-22 15:04:54,631 | INFO  | (Ingestor:114) - Install [53e6bda0 thread=db] package.zip
admin_1         | 2016-11-22 15:04:54,634 | INFO  | (IngestServiceImpl:433) - Ingesting zipped mediapackage
admin_1         | 2016-11-22 15:04:55,296 | INFO  | (IngestServiceImpl:469) - Storing zip entry 17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360.mkv in working file repository collection '17701'
admin_1         | 2016-11-22 15:06:30,059 | INFO  | (IngestServiceImpl:482) - Zip entry 17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360.mkv stored at https://opencast.example.com/files/collection/17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360_1.mkv
admin_1         | 2016-11-22 15:06:30,272 | INFO  | (IngestServiceImpl:469) - Storing zip entry 17701/episode.xml in working file repository collection '17701'
admin_1         | 2016-11-22 15:06:30,287 | INFO  | (IngestServiceImpl:482) - Zip entry 17701/episode.xml stored at https://opencast.example.com/files/collection/17701/episode_2.xml
admin_1         | 2016-11-22 15:06:30,314 | INFO  | (IngestServiceImpl:516) - Ingesting mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 is named 'A media package courtesy by the inbox scanner.'
admin_1         | 2016-11-22 15:06:30,315 | INFO  | (IngestServiceImpl:530) - Ingested mediapackage element 77bf879f-817e-403c-b35e-fd97dee31261/track-1 is located at http://opencast.example.com/files/collection/17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360_1.mkv
admin_1         | 2016-11-22 15:06:30,338 | INFO  | (IngestServiceImpl:530) - Ingested mediapackage element 77bf879f-817e-403c-b35e-fd97dee31261/catalog-1 is located at http://opencast.example.com/files/collection/17701/episode_2.xml
admin_1         | 2016-11-22 15:06:30,339 | INFO  | (IngestServiceImpl:544) - Initiating processing of ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261
admin_1         | 2016-11-22 15:06:30,340 | INFO  | (IngestServiceImpl:1068) - Starting a new workflow with ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 based on workflow definition 'schedule-and-upload'
admin_1         | 2016-11-22 15:06:30,340 | INFO  | (IngestServiceImpl:1359) - Ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 is processed using workflow template 'schedule-and-upload', specified during ingest
admin_1         | 2016-11-22 15:06:30,354 | INFO  | (IngestServiceImpl:1120) - Starting new workflow with ingested mediapackage '77bf879f-817e-403c-b35e-fd97dee31261' using the specified template 'schedule-and-upload'
admin_1         | 2016-11-22 15:06:32,229 | INFO  | (IngestServiceImpl:546) - Ingest of mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 done
admin_1         | 2016-11-22 15:06:32,303 | INFO  | (Ingestor$1$1:130) - Ingested package.zip as a mediapackage from inbox
admin_1         | 2016-11-22 15:06:33,627 | INFO  | (WorkflowServiceImpl:843) - [>a508b423] Scheduling workflow 17702 for execution
admin_1         | 2016-11-22 15:06:38,720 | INFO  | (DefaultsWorkflowOperationHandler:120) - Configuration key 'flagForCutting' of ...
... and the workflow continues
```
_Logs produced by Opencast 2.2.2 Docker_
