Load Configuration
===================

This guide will help you to set up the load configuration settings which are strongly recommended for each Opencast 
installation. These settings control how many jobs are running on your various hardware nodes. These settings can be
left at their defaults initially, but as your installation grows you will likely wish to fine-tune these to get the best
performance you can out of your hardware.

Background: What is a load value
--------------------------------

Every job obviously imposes a certain amount of load on its processing system, the question is how can we quantify this?
The settings this document will walk you through are estimates of the load placed on your system(s) by each job type. 
This means that every individual instance of that job type will count for a certain amount of load, and Opencast will
refuse to process more than a certain configurable amount of load at any given time on a given node. These loads are 
tracked on a per-node basis, so a job running on one node imposes no load on another.

As an example, say we have a worker with 8 cores. With Opencast 1.x all jobs, even expensive jobs like encoding, had an
effective load value of 1.0. This meant that Opencast would schedule up to 8 encodes on worker 1! Obviously this is not 
ideal, since most encoding jobs consume multiple cores. With Opencast 2.x you can now specify on an encoding profile 
level how much load is imposed on a node. Likewise, all other jobs (video segmentation, publishing, etc) also now have
configurable loads.

Step 1: Determine your load values
----------------------------------

This is a very subjective process, but is arguably the most imporant: How much load does each job and encoding profile
add to your system? We have tried our best to set useful loads for each job, however these are only estimates. If your
installation has, for example, hardware assisted encoding then your encoding jobs may be very inexpensive. In general, 
it is safe to assume that the first load value from the output of `uptime` is a good estimate of the load imposed by a 
job.

Step 2: Setting the load values for system jobs
-----------------------------------------------

Each Opencast instance has its own maximum load. By default this is set to the number of CPU cores present in the 
system. If you wish to change this, set the `org.opencastproject.server.maxload` key in config.properties to the 
maximum load you want this node to accept. Keep in mind that exceeding the number of CPU cores present in the system is
not recommended.

The load values for the non-encoding jobs are set in the etc/services files. Look for files containing the prefix 
`job.load`. These configuration keys control the load for each job type. For example, the `job.load.download.distribute`
configuration key controls the load placed on the system when a download distribution job is running. The current files
with relevant configuration keys are:

|File|Controls|
|org.opencastproject.caption.impl.CaptionServiceImpl.properties|Caption convertion services|
|org.opencastproject.composer.impl.ComposerServiceImpl.properties|Caption embedding services|
|org.opencastproject.distribution.acl.AclDistributionService.properties|ACL file distribution|
|org.opencastproject.distribution.distribution.streaming.StreamingDistributionService.properties|Streaming distribution|
|org.opencastproject.distribution.download.DownloadDistributionServiceImpl.properties|Download distribution|
|org.opencastproject.ingest.impl.IngestServiceImpl.properties|Ingest services|
|org.opencastproject.inspection.ffmpeg.MediaInspectionServiceImpl.properties|Media inspection using ffmpeg|
|org.opencastproject.inspection.impl.MediaInspectionServiceImpl.properties|Media inspection using mediainfo|
|org.opencastproject.publication.youtube.YouTubePublicationServiceImpl.properties|Youtube distribution|
|org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.properties|Youtube distribution|
|org.opencastproject.search.impl.SearchServiceImpl.properties|Matterhorn engage index jobs|
|org.opencastproject.silencedetection.impl.SilenceDetectionServiceImpl.properties|Silence detection|
|org.opencastproject.textanalyzer.impl.TextAnalyzerServiceImpl.properties|Text analysis, including slide OCR|
|org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.properties|Video editor|
|org.opencastproject.videosegmenter.ffmpeg.VideoSegmenterServiceImpl.properties|Video segmentation|

*Note*: Ingest jobs are a special case in Opencast. Because of their immediate nature there is no way to limit the 
number of running jobs. However, these jobs will block other jobs from running on the ingest/admin nodes if enough 
ingests running concurrently.

Step 3: Setting the load values for encoding profiles
-----------------------------------------------------

Each encoding profile can have a load value associated with it. By default, we have not set any, which means that the 
default value of 1.0 is used. To set the load associated with a profile, you simply add a .jobload key to the profile.
For example, the composite encoding profile is prefixed with `profile.composite.http`. If we want to set a different job
load than 1.0, we would create the `profile.composite.http.jobload` key, and set it to an appropriate job value.

Step 4: Restart Opencast
--------------------------

Many of these configuration files are only read on startup, so restarting Opencast is strongly recommended.

Troubleshooting
---------------
* Help, my system has deadlocked, or there are jobs which are always queued even if the system is otherwise idle
This can be caused by setting a job weight that exceeds the maximum load for *all* services of a given type. For 
example, if you have a single worker with 8 cores and set an encoding job to have a jobload of 9. Fortunately, there is
a simple resolution to this issue. Jobs which have already been created do *not* update their load values, even after 
restarting Opencast. To resolve a deadlock caused by job loads you simply stop Opencast, update the offending job's load
to zero, fix the configuration file which caused the issue, then restart Opencast.
TODO: steps to actually do this
