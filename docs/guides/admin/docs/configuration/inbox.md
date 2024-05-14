Inbox Scanner
=============

<div class=warn>
For most purposes, we recommend using the Ingest REST API instead of the inbox.
It is more reliable and provides instant feedback if a problem occurs.
</div>

<div class=warn>
Do not use the inbox on a CIFS mount.
The mount may report inaccurate metadata, which may break ingests or cause files to not be recognized.
</div>


Overview
--------

Besides ingesting media packages using the REST API,
dedicated inbox directories located in the file system can be scanned by Opencast.
This, for example, allows adding media packages to Opencast by copying it to a specific location using scripting/SFTP
without the need for any HTTP traffic.
Opencast periodically scans the specified location for new files.

Each configured inbox directory may result in digest for a separate organization or with a different default workflow.

The inbox can process media files directly, or can work on zipped media packages containing multiple files and
additional metadata.


Step 1: Configure an Inbox Scanner
----------------------------------

Adjust `etc/org.opencastproject.ingest.scanner.InboxScannerService-inbox.cfg`.
The `-inbox` suffix of the file name is variable and multiple files can
be created with different settings for different directories to be watched.


Step 2: Testing the inbox
-----------------------------------------

In order to test the inbox, either put a valid media package zip or a single media file into the scanned directory.
For the first test, using a single video file is easier.

The file will be ingested and removed from the inbox.
After that, you will see a new event appearing in the admin interface
and you can follow the processing in the Opencast logs.

Note that even if the poll interval is small, it may take a little longer until
the media package is visible in the admin interface because extracting and/or
copying the media files will take some time.

The logs will look something like this:

```no-highlight
INFO  | (Ingestor:114) - Install [53e6bda0 thread=db] package.zip
INFO  | (IngestServiceImpl:433) - Ingesting zipped mediapackage
INFO  | (IngestServiceImpl:469) - Storing zip entry 17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360.mkv in working file repository collection '17701'
INFO  | (IngestServiceImpl:482) - Zip entry 17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360.mkv stored at https://opencast.example.com/files/collection/17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360_1.mkv
INFO  | (IngestServiceImpl:469) - Storing zip entry 17701/episode.xml in working file repository collection '17701'
INFO  | (IngestServiceImpl:482) - Zip entry 17701/episode.xml stored at https://opencast.example.com/files/collection/17701/episode_2.xml
INFO  | (IngestServiceImpl:516) - Ingesting mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 is named 'A media package courtesy by the inbox scanner.'
INFO  | (IngestServiceImpl:530) - Ingested mediapackage element 77bf879f-817e-403c-b35e-fd97dee31261/track-1 is located at http://opencast.example.com/files/collection/17701/presenter_c20e7623_81e3_4a78_8738_f7a619141360_1.mkv
INFO  | (IngestServiceImpl:530) - Ingested mediapackage element 77bf879f-817e-403c-b35e-fd97dee31261/catalog-1 is located at http://opencast.example.com/files/collection/17701/episode_2.xml
INFO  | (IngestServiceImpl:544) - Initiating processing of ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261
INFO  | (IngestServiceImpl:1068) - Starting a new workflow with ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 based on workflow definition 'schedule-and-upload'
INFO  | (IngestServiceImpl:1359) - Ingested mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 is processed using workflow template 'schedule-and-upload', specified during ingest
INFO  | (IngestServiceImpl:1120) - Starting new workflow with ingested mediapackage '77bf879f-817e-403c-b35e-fd97dee31261' using the specified template 'schedule-and-upload'
INFO  | (IngestServiceImpl:546) - Ingest of mediapackage 77bf879f-817e-403c-b35e-fd97dee31261 done
INFO  | (Ingestor$1$1:130) - Ingested package.zip as a mediapackage from inbox
INFO  | (WorkflowServiceImpl:843) - [>a508b423] Scheduling workflow 17702 for execution
INFO  | (DefaultsWorkflowOperationHandler:120) - Configuration key 'flagForCutting' of ...
... and the workflow continues
```


Addition: How to Prepare a Media Package
----------------------------------------

Media packages contain media files and metadata files describing them.
Opencast is able to generate zipped media packages using the
[ZipWorkflowOperation](../workflowoperationhandlers/zip-woh.md).

To generate a media package on your own, first, create a `manifest.xml` or manifest file.
You can also let Opencast create a valid empty media package XML using the `/ingest/createMediaPackage` REST endpoint.

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

Next, create a Dublin Core metadata XML.
This is the `episode.xml` file referenced in the media package.

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

Based on the media package example above, you also need the referenced two video files.
These are `presenter.mkv` and `presentation.mkv`.

Finally, create a ZIP file with no compression containing the files:

```
zip -j --compression-method store my-media-package.zip manifest.xml episode.xml presenter.mkv presentation.mkv
```

You can now move the zip media package file to your inbox directory.
