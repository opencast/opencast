AWS Auto-Scaling Termination State Service
==========================================

This page documents the configuration for Opencast module **terminationstate-aws**.  This
configuration is only required on nodes that are part of an [AWS Auto Scaling](https://docs.aws.amazon.com/autoscaling/ec2/userguide/what-is-amazon-ec2-auto-scaling.html)
group.

The purpose of this module to manage the termination of an AWS EC2 instance, triggered
when an Auto Scaling Group "scales in", as the Opencast process may still be processing jobs
which we want to complete. This is special implementation of the basic [Termination State Service](terminationstate.md)

**It does not terminate the Opencast process or the instance itself**.

Auto Scaling Groups can trigger a [Lifecycle Hook](https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html)
when an instance is created or terminated which allow events to occur before the creation or termination
is completed. The service can poll if the termination hook has been triggered, at which point it will:

* put the node in maintenance mode, to stop accepting new jobs
* periodically check for running jobs and if so emit a _heartbeat_
* when no jobs are running it will tell the Auto Scaling group to complete the Terminate life-cycle action

Alternatively you can disable the Lifecycle state polling and call the REST endpoint
(termination/aws/autoscaling) to signal that the instance is now terminating. The details of how to achieve
this are beyond the scope of this document, but using a CloudWatch Alarm to trigger a Lambda function
is a suggested route.

Amazon User Configuration
-------------------------

Configuration of Amazon users is beyond the scope of this documentation, instead we suggest referring to
[Amazon's documentation](http://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html).
You will, however, require to set up proper credentials by either:

* Creating an [Access Key ID and a Secret Access Key](https://aws.amazon.com/developers/access-keys/) or
* Using [Instance Profile Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-roles.html)
  (recommended when running Opencast on EC2 instances)

The termination state service requires a number of permissions to query and respond to changes in the
instance's lifecycle. You should follow [these instructions](http://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_inline-using.html)
to create a new policy that is assigned to the IAM profile or user account. The following policy contains
all the necessary permissions. You will need to change the _region_  and _account_ number in the Resource
ARN with your own.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "ReadInstanceLifcycle",
            "Effect": "Allow",
            "Action": [
                "autoscaling:DescribeAutoScalingInstances",
                "autoscaling:DescribeAutoScalingGroups",
                "autoscaling:DescribeLifecycleHooks"
            ],
            "Resource": "*"
        },
        {
            "Sid": "UpdateLifcycle",
            "Effect": "Allow",
            "Action": [
                "autoscaling:CompleteLifecycleAction",
                "autoscaling:RecordLifecycleActionHeartbeat"
            ],
            "Resource": "arn:aws:autoscaling:<region>:<account>:autoScalingGroup:*:autoScalingGroupName/*"
        }
    ]
}
```

**A [free Amazon account](https://aws.amazon.com/free/) will work for small scale testing, but be aware
that AutoScaling can incur costs if not correctly setup.**

Amazon AutoScaling Configuration
--------------------------------

Please consult the AWS documentation to create an [AutoScaling Group](https://docs.aws.amazon.com/autoscaling/ec2/userguide/GettingStartedTutorial.html).
 You will need to explicitly add a [Lifecycle Hook](https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html)
for the "autoscaling:EC2_INSTANCE_TERMINATING" Lifecycle transition. The 'Heartbeat Timeout' should be set to something
appropriate. The default 3600 seconds is fine for production but quite long when developing your
deployment. NOTE: the service will tell the AutoScaling Group to complete termination even if the timeout
hasn't expired once there are no jobs running.

Opencast Service Configuration
------------------------------

The Opencast AWS Autoscaling Termination State Service configuration can be found in the file
`org.opencastproject.terminationstate.aws.AutoScalingTerminationStateService.cfg`.

|Key name|Value|Example|
|--------|-----|-------|
|enable|true to enable the service, false (default) otherwise|true|
|lifecycle.polling.enabled|true to poll the Lifecycle for the termination state| true|
|lifecycle.polling.period|frequency which to poll the Lifecycle in seconds|300|
|lifecycle.heartbeat.period|frequency which to check if if jobs are running and emit Lifecycle heartbeat|300|
|access.id|AWS user's access ID|20 alphanumeric characters|
|access.secret|AWS user's secret key|40 characters|

If *access.id* and *access.secret* are not *explicitly* provided, search for credentials will be performed in the order specified by the
[Default Credentials Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).

*NOTE*: both the _lifecycle.polling.period_ and _lifecycle.heartbeat.period_ should
be less than the 'Heartbeat Timeout' of the Lifecycle Hook or else the instance
could be terminated before the service can respond.
