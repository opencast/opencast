Load Configuration
===================

This guide will help you to set up the load configuration settings which are strongly recommended for each Opencast 
installation.  These settings control how many jobs are running on your various hardware nodes.  These settings can be
left at their defaults initially, but as your installation grows you will likely wish to fine-tune these to get the best
performance you can out of your hardware.

Background: What is a load value
--------------------------------

Every job obviously imposes a certain amount of load on its processing system, the question is how can we quantify this?
The settings this document will walk you through are estimates of the load placed on your system(s) by each job type.  
This means that every individual instance of that job type will count for a certain amount of load, and Opencast will
refuse to process more than a certain configurable amount of load at any given time on a given node.  These loads are 
tracked on a per-node basis, so a job running on one node imposes no load on another.

As an example, say we have a worker with 8 cores.  With Opencast 1.x all jobs, even expensive jobs like encoding, had an
effective load value of 1.0.  This meant that Opencast would schedule up to 8 encodes on worker 1! Obviously this is not 
ideal, since most encoding jobs consume multiple cores.  Since Opencast 2.1 you can now specify on an encoding profile 
level how much load is imposed on a node.  Likewise, all other jobs (video segmentation, publishing, etc) also now have
configurable loads.

Job loads can be any floating point value between 0.0, and Java's MAXFLOAT.  Fractional loads are supported, since many
of the jobs that Opencast spawns as a regular part of its workflows are very small.  There is no sanity checking for the
configured loads, aside from assuring they are not negative.  This means that improperly set load values can cause
deadlocks!  Fortunately, this is easy to fix.  See Troubleshooting for more details.

Step 1: Determine your load values
----------------------------------

This is a very subjective process, but is arguably the most imporant: How much load does each job and encoding profile
add to your system? We have tried our best to set useful loads for each job, however these are only estimates.  If your
installation has, for example, hardware assisted encoding then your encoding jobs may be very inexpensive.  In general, 
it is safe to assume that the first load value from the output of `uptime` is a good estimate of the load imposed by a 
job.

Note: These job loads are specific for each *node* in the cluster.  This means that for any given job, each node can 
have a different load value associated.  For instance, if worker A has no job load specified for its encoding profiles, 
and worker B has job loads specified then any encoding jobs dispatched to A will have a load of 1.0, and jobs dispatched
to B will have a different, presumably higher load.  There are edge cases where this may be useful, but is most cases 
this will only cause confusion.  It is therefore highly recommended that these settings be put into your configuration 
management system, and be applied on a cluster level to ensure consistency across all nodes.

Step 2: Setting the load values for system jobs
-----------------------------------------------

Each Opencast instance has its own maximum load.  By default this is set to the number of CPU cores present in the 
system.  If you wish to change this, set the `org.opencastproject.server.maxload` key in config.properties to the 
maximum load you want this node to accept.  Keep in mind that exceeding the number of CPU cores present in the system is
not recommended.

The load values for the non-encoding jobs are set in the etc/services files.  Look for files containing the prefix 
`job.load`.  These configuration keys control the load for each job type.  For example, the 
`job.load.download.distribute` configuration key controls the load placed on the system when a download distribution job
is running.  The current files with relevant configuration keys are:

| File                                                                                     | Controls                           |
|------------------------------------------------------------------------------------------|------------------------------------|
| org.opencastproject.caption.impl.CaptionServiceImpl.cfg                                  | Caption convertion services        |
| org.opencastproject.composer.impl.ComposerServiceImpl.cfg                                | Caption embedding services         |
| org.opencastproject.distribution.acl.AclDistributionService.cfg                          | ACL file distribution              |
| org.opencastproject.distribution.distribution.streaming.StreamingDistributionService.cfg | Streaming distribution             |
| org.opencastproject.distribution.download.DownloadDistributionServiceImpl.cfg            | Download distribution              |
| org.opencastproject.execute.impl.ExecuteServiceImpl.cfg                                  | Execute service                    |
| org.opencastproject.ingest.impl.IngestServiceImpl.cfg                                    | Ingest services                    |
| org.opencastproject.inspection.ffmpeg.MediaInspectionServiceImpl.cfg                     | Media inspection using ffmpeg      |
| org.opencastproject.inspection.impl.MediaInspectionServiceImpl.cfg                       | Media inspection using mediainfo   |
| org.opencastproject.publication.youtube.YouTubePublicationServiceImpl.cfg                | Youtube distribution               |
| org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.cfg              | Youtube distribution               |
| org.opencastproject.search.impl.SearchServiceImpl.cfg                                    | Matterhorn engage index jobs       |
| org.opencastproject.silencedetection.impl.SilenceDetectionServiceImpl.cfg                | Silence detection                  |
| org.opencastproject.textanalyzer.impl.TextAnalyzerServiceImpl.cfg                        | Text analysis, including slide OCR |
| org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.cfg                          | Video editor                       |
| org.opencastproject.videosegmenter.ffmpeg.VideoSegmenterServiceImpl.cfg                  | Video segmentation                 |

Note: Ingest jobs are a special case in Opencast.  Because of their immediate nature there is no way to limit the 
number of running jobs.  However, these jobs will block other jobs from running on the ingest/admin nodes if enough 
ingests running concurrently.

Step 3: Setting the load values for encoding profiles
-----------------------------------------------------

Each encoding profile can have a load value associated with it.  By default, we have not set any, which means that the 
default value of 1.0 is used.  To set the load associated with a profile, you simply add a .jobload key to the profile.
For example, the composite encoding profile is prefixed with `profile.composite.http`.  If we want to set a different 
job load than 1.0, we would create the `profile.composite.http.jobload` key, and set it to an appropriate job value.

Step 4: Restart Opencast
--------------------------

Many of these configuration files are only read on startup, so restarting Opencast is strongly recommended.

Troubleshooting
===============

Help, my system has deadlocked, or there are jobs which are always queued even if the system is otherwise idle
--------------------------------------------------------------------------------------------------------------

This can be caused by setting a job weight that exceeds the maximum load for *all* services of a given type.  For 
example, if you have a single worker with 8 cores and set an encoding job to have a jobload of 9.  Fortunately, there is
a simple resolution to this issue.  Jobs which have already been created do *not* update their load values, even after 
restarting Opencast.  To resolve a deadlock caused by job loads follow these instructions.  First determine the
queued job's ID from the admin UI.  This will be an integer greater than zero.  We will call this $jobid.  Once you have
the job ID, follow these steps:

 * Stop Opencast
 * Log into your database
 * Make sure you are using the right schema.  Currently the default is called `matterhorn`
 * Update the job's load
  * This will look something like `UPDATE mh_job SET job\_load=0.0 WHERE id=$jobid`
 * Log out of your database
 * Change the load specified in the configuration file to an appropriate value
  * This may need to happen across all nodes!
 * Restart matterhorn
