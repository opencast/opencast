AWS S3 Distribution Configuration
=================================
This page documents the configuration for Opencast module **matterhorn-distribution-service-aws-s3**.  This
configuration is only required on the presentation node, and only if you are using Amazon S3 and/or Cloudfront for
distributing media to end users.

Amazon User Configuration
-------------------------

Configuration of Amazon users is beyond the scope of this documentation, instead we suggest referring to
[Amazon's documentation](http://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html).
You will, however, require to set up proper credentials by either:

* Creating an [Access Key ID and a Secret Access Key](https://aws.amazon.com/developers/access-keys/) or
* Using [Instance Profile Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-roles.html)
  (recommended when running Opencast on EC2 instances)

*AmazonS3FullAccess* permission is *required*, which can be granted using
[these instructions](http://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_inline-using.html).

**A [free Amazon account](https://aws.amazon.com/free/) will work for small scale testing, but be aware that S3
distribution can cost you a lot of money very quickly.  Be aware of how much data and how many requests you are making,
and be sure to [set alarms](http://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-alarms.html) to
notify you of cost overruns.**

Amazon Service Configuration
----------------------------

The development and testing it is generally safe to allow the Opencast AWS S3 Distribution service to create the S3
bucket for you.  It will create the bucket per its configuration, with public read-only access to the files, and no
versioning.  For production use we suggest using Amazon CloudFront, which requires additional configuration.

Amazon CloudFront
-----------------

Amazon CloudFront provides an *optional* way to better handle distributing your media to end users.  While fully
configuring CloudFront is outside the scope of this documentation, we wish to note that this does affect one of the keys
described below.  Please ensure you use the correct distribution base format depending on which service you are using!

Opencast Service Configuration
------------------------------

The Opencast AWS S3 Distribution service has five configuration keys, which can be found in the
`org.opencastproject.distribution.aws.s3.AwsS3DistributionServiceImpl.cfg` configuration file.

|Key name|Value|Example|
|--------|-----|-------|
|org.opencastproject.distribution.aws.s3.distribution.enable|True to enable S3 distribution, false otherwise|true|
|org.opencastproject.distribution.aws.s3.region|The AWS region to set|us-west-2|
|org.opencastproject.distribution.aws.s3.bucket|The S3 bucket name|example-org-dist|
|org.opencastproject.distribution.aws.s3.distribution.base|Where the S3 files are available from.  This value can be derived from the bucket and region values, or is set by CloudFront.|http://s3-us-west-2.amazonaws.com/example-org-dist, or DOMAIN_NAME.cloudfront.net|
|org.opencastproject.distribution.aws.s3.access.id|Your access ID|20 alphanumeric characters|
|org.opencastproject.distribution.aws.s3.secret.key|Your secret key|40 characters|

If *org.opencastproject.distribution.aws.s3.access.id* and *org.opencastproject.distribution.aws.s3.secret.key* are
 not *explicitly* provided, search for credentials will be performed in the order specified by the
 [Default Credentials Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).

Using S3 Distribution
---------------------

Amazon S3 distribution is already included in the default Opencast workflows, however it must first be enabled.  The
`ng-schedule-and-upload.xml` and `ng-publish.xml` workflow configuration files both contain lines containing the string
"Remove this line if you wish to publish to AWS S3".  Both of these lines must be removed before publishing to AWS S3
will function correctly.

If you wish to use AWS S3 publishing with your own custom workflow, you must add the `publish-aws` workflow operation to
your workflow.  The operation documentation can be found [here](../workflowoperationhandlers/publishaws-woh.md).

Publishing to multiple distribution services
--------------------------------------------

Currently we do not support publication to multiple distribution services simultaneously.  This means that whichever
workflow operation is *last* in the workflow will be the final publication.

Using this handler in custom workflows
--------------------------------------

If your workflow contains both `publish-engage` and `publish-aws`, in that order, and without a 
[conditional](../configuration/workflow.md) you would have publication files stored both locally *and* in AWS.  This is
likely not what you want, so protect your workflow operations appropriately.  If you really do need these files stored
in both places (for example, in cases where you need to make the files available immediately, and only push to AWS in 
some cases) then remember to add a [retract-engage](../workflowoperationhandlers/retract-engage-woh.md) in between the
publication operations.  Note that if this step is omitted the files will remain available locally, but will not be 
used.  Of further note, if you retract after publication to AWS then your workflow *will not be available* to users.
To summarize, this table presents a subset of the various situations that are possible

|Workflow Operations|Files present in the Media Module|Files present in AWS|Files served from|
|publish-engage | Yes | No | Opencast Media Module |
|publish-aws| No | Yes | AWS |
|publish-engage, publish-aws| Yes | Yes | AWS |
|publish-aws, publish-engage| Yes | Yes | Opencast Media Module|
|publish-engage, retract-engage, publish-aws | Temporary | Yes | AWS |
|publish-engage, publish-aws, retract-engage | No | Yes | Not available |

Migrating to S3 Distribution with Pre-Existing Data
---------------------------------------------------

If you already have data published to your local Opencast install, you should be able to republish the media selecting
AWS S3 as the distribution service to use.
