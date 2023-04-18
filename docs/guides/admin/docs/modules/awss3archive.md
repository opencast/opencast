AWS S3 Archive Configuration
=================================
This page documents the configuration for the AWS S3 components in the Opencast module **asset-manager-storage-aws**.

This configuration is only required on the admin node, and only if you are using Amazon S3 as an archive repository.

Amazon User Configuration
-------------------------

Configuration of Amazon users is beyond the scope of this documentation, instead we suggest referring to
[Amazon's documentation](http://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html).  You will, however,
require an [Access Key ID and Secret Access Key](https://aws.amazon.com/developers/access-keys/).  The user to which
this key belongs *requires* the *AmazonS3FullAccess* permission, which can be granted using
[these instructions](http://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_inline-using.html).

**A [free Amazon account](https://aws.amazon.com/free/) will work for small scale testing, but be aware that S3
archiving can cost you a lot of money very quickly.  Be aware of how much data and how many requests you are making,
and be sure to [set alarms](http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-alarms.html) to
notify you of cost overruns.**

Amazon Service Configuration
----------------------------

For development and testing it is generally safe to allow the Opencast AWS S3 Archive service to create the S3 bucket
for you.  It will create the bucket per its configuration, with private-only access to the files, and no versioning.

Opencast Service Configuration
------------------------------

The Opencast AWS S3 Archive service configuration can be found in the
`org.opencastproject.assetmanager.aws.s3.AwsS3AssetStore.cfg` configuration file.

| Key                                                        | Description                    | Default                      | Example                    |
|:-----------------------------------------------------------|:-------------------------------|:----------------------------:|:--------------------------:|
| org.opencastproject.assetmanager.aws.s3.enabled            | Whether to enable this service | false                        |                            |
| org.opencastproject.assetmanager.aws.s3.region             | The AWS region to set          | us-east-1                    |                            |
| org.opencastproject.assetmanager.aws.s3.bucket             | The S3 bucket name             |                              | example-org-archive        |
| org.opencastproject.assetmanager.aws.s3.access.id          | Your access ID                 |                              | 20 alphanumeric characters |
| org.opencastproject.assetmanager.aws.s3.secret.key         | Your secret key                |                              | 40 characters              |
| org.opencastproject.assetmanager.aws.s3.endpoint           | The endpoint to use            | Default AWS S3 endpoint      | https://s3.service.com     |
| org.opencastproject.assetmanager.aws.s3.path.style         | Whether to use path style      | false / Default AWS S3 style |                            |
| org.opencastproject.assetmanager.aws.s3.max.connections    | Number of max connections      | 50                           |                            |
| org.opencastproject.assetmanager.aws.s3.connection.timeout | Connection timeout in ms       | 10000                        |                            |
| org.opencastproject.assetmanager.aws.s3.max.retries        | Number of max retries          | 100                          |                            |

Using S3 Archiving
------------------

S3 archiving is done on a Snapshot level, that is a mediapackage ID + version.  Because of the way that the Asset
Manager handles snapshots, all newly created snapshots are *always* local.  Creating a snapshot of a mediapackage with
non local data will download *all* related snapshots for that mediapackage which can incur significant costs.  S3
archiving is meant to be a cost reduction, and storage expansion tool, rather than then primary storage for your
asset manager.  Therefore, most adopters do not want to immediately (ie, at the end of your default workflow) offload
your recordings to S3!  Instead, we suggest using the TimedMediaArchiver
(org.opencastproject.assetmanager.impl.TimedMediaArchiver.cfg) to offload your recordings after sufficient time that
further modification of the recording is unlikely.

If you do need to create an additional workflow, a substantially better approach than restoring snapshots involves
using the [ingest-download](../workflowoperationhandlers/ingestdownload-woh.md) workflow operation handler to download
the relevant file(s) to the local workspace.  This dramatically speeds up snapshotting, and allows the operations which
require local files to work properly without having to restore everything, and then re-archive to S3.

Manual S3 Archiving
-------------------

Manually moving assets to and from S3 is done via a workflow operation handler added as part of a workflow.
The workflow operation handler definition looks like this
```
    <operation
      id="move-storage"
      description="Offloading to AWS S3">
      <configurations>
        <configuration key="target-storage">aws-s3</configuration>
      </configurations>
    </operation>
```

Assets in S3 continue to be accessible to Opencast, however there may be cases where you wish to restore your content
back to your local storage.  This can be accomplished using the same workflow operation definition as above, and
changing the `target-storage` configuration value from `aws-s3` to `local-filesystem` like so
```
    <operation
      id="move-storage"
      description="Restoring from AWS S3">
      <configurations>
        <configuration key="target-storage">local-filesystem</configuration>
      </configurations>
    </operation>
```


S3 Storage Tiers
================

S3 supports [storage tiering](https://aws.amazon.com/s3/storage-classes/), which can offer significant cost savings in
return for substantially increased access times.  Opencast does not directly expose this functionality in the UI, but
support is present in the back end.  Both manual, and Lifecycle based storage tiering are supported, as are all tiers.
Attempting to retrieve an asset will trigger a restore to the standard S3 tier, if appropriate, and then return the
file contents.  For more on cold storage, see below.


S3 Glacier Flexible Retrieval and Deep Archive
----------------------------------------------

The Glacier FR and Deep Archive (known as Cold Storage going forward) storage classes are supported, however they have
a significant drawback at this point: Opencast does not understand that these files are not immediately accessible.
Attempts to process a workflow containing assets will trigger a restore of the file, but then likely fail the workflow
after some time.  This because some Opencast configurations use HTTP(S) downloading to transfer the files between
processing nodes, and those transfers *will* time out when access times for the media files are measured in hours.
Note that these failures will not harm your Opencast system, but they will cost you money because of the potentially
wasted restores.

A better approach is to use the REST endpoints at `/assets/aws/s3`, eg `http://stable.opencast.org/assets/aws/s3`.
Specifically, you probably want to use `PUT glacier/{mediaPackageId}/assets`, which enables temporary restoration of
files from the Cold Storage tiers to standard S3.  AWS will automatically remove the temporary copy after the specified
duration, and that should be long enough for your workflow to complete.


Permanently Restoring Content
-----------------------------

Rarely you will need your content restored to S3 on a more permanent basis.  In this case you need to temporarily
restore as above, and the once the restore is complete use the `POST {mediaPackageId}/assets` endpoint to permanently
move the asset.  Note that the permanently restored asset may still be re-Glaciered by any active AWS Object Lifecycle
rules.


Manually Changing Content Storage Tiers
---------------------------------------

While AWS Lifecycle rules are a much more scalable solution, there may be times when you wish to manually alter an
asset's storage tier.  In the same way that you can permanently restore an asset, you can also manually move assets
betwen storage classes using the `POST {mediaPackageId}/assets`.
