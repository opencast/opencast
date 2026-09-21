Load Configuration
===================

This guide will help you to set up the load configuration, which is strongly recommended for each Opencast
installation. These settings control how many jobs run on each of your nodes. You can leave them at their defaults
initially, but as your installation grows you will likely want to fine-tune them to get the best performance out of your
hardware.

Background: What is a load value
--------------------------------

Every job puts a certain load on the system that processes it. The settings in this guide are estimates of that load
for each job type. Every instance of a job type counts for its configured load, and Opencast refuses to process more
than a configurable amount of load on a node at any given time. Loads are tracked per node, so a job running on one node
imposes no load on another.

Jobs differ greatly in cost. On a worker with 8 cores, for example, you do not want 8 encodes at once, since most
encoding jobs use multiple cores. That is why each job type and each encoding profile has its own configurable load.

A job load can be any floating point value between 0.0 and Java's MAXFLOAT. Fractional loads are supported since many
jobs are very small. The only check is that loads are not negative, so improperly set load values can cause deadlocks.
This is easy to fix, see [Troubleshooting](#troubleshooting).

Step 1: Determine your load values
----------------------------------
This is a subjective process, but arguably the most important one: how much load does each job and encoding profile add
to your system? We have set useful defaults, but they are only estimates. With hardware-assisted encoding, for example,
your encoding jobs may be very cheap. In general, the first load value from the output of `uptime` is a good estimate of
the load a job imposes.

Note: Job loads are specific to each *node*. If worker A sets no job load for its encoding profiles and worker B does,
encoding jobs created by A get the default load (1.5) and jobs created by B a different, presumably higher one. This can
be useful in rare cases but mostly just causes confusion. We therefore highly recommend putting these settings into your
configuration management system and applying them cluster-wide to keep all nodes consistent.

Step 2: Setting the load values for system jobs
-----------------------------------------------

Each Opencast instance has its own maximum load, which defaults to the number of CPU cores in the system. To change it,
set the `org.opencastproject.server.maxload` key in `custom.properties` to the maximum load this node should accept.
Exceeding the number of CPU cores is not recommended.

The load values for non-encoding jobs are set in the configuration files in the `etc` directory. Search it for files
containing the string `job.load` to find the relevant keys, one for each job type. For example, the
`job.load.download.distribute` key controls the load placed on the system by a download distribution job.

Note: Ingest jobs are a special case. Because they start immediately, the number of running ingest jobs cannot be
limited. If enough ingests run concurrently, though, they block other jobs from running on the ingest/admin nodes.

Step 3: Setting the load values for encoding profiles
-----------------------------------------------------

Each encoding profile can have its own load. By default none is set, so the default of 1.5 is used. To set one, add a
`.jobload` key to the profile. For the composite profile, which is prefixed with `profile.composite.http`, this is the
key `profile.composite.http.jobload`, set to the load you want.

Step 4: Restart Opencast
--------------------------

Many of these configuration files are only read on startup, so restarting Opencast is strongly recommended.

Troubleshooting
---------------

### Help, my system has deadlocked, or there are jobs which are always queued even if the system is otherwise idle

This can be caused by a job load that exceeds the maximum load of *all* services of a given type, for example a job load
of 9 for an encoding job when you have a single worker with 8 cores. Jobs that already exist do *not* update their load
values, even after restarting Opencast. To resolve the deadlock, first determine the queued job's ID (an integer greater
than zero) from the admin UI. We will call it `$jobid`. Then follow these steps:

* Stop Opencast
* Log into your database
* Make sure you are using the right schema. Currently the default is called `opencast`
* Update the job's load
    * This will look something like `UPDATE oc_job SET job\_load=0.0 WHERE id=$jobid`
* Log out of your database
* Change the load specified in the configuration file to an appropriate value
    * This may need to happen across all nodes!
* Restart Opencast
