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

There are two major methods to access S3 archiving features: manually, and via a workflow.  Amazon S3 archiving is not
part of the default workflows and manual S3 offload is disabled by default.  To enable manual S3 offload you must edit
the `offload.xml` workflow configuration file and change `var s3Enabled = false;` to `var s3Enabled = true;`.  To
manually offload a media package follow the directions in the user documentation.

To automatically offload a media package to S3 you must add the `move-storage` workflow operation to your workflow.
The operation documentation can be found [here](../workflowoperationhandlers/move-storage-woh.md).

Migrating to S3 Archiving with Pre-Existing Data
---------------------------------------------------

Archiving to S3 is a non-destructive operation in that it is safe to move archive files back and forth between local
storage and S3.  To offload your local archive, select the workflow(s) and follow the manual offload steps described in
the user documentation.
