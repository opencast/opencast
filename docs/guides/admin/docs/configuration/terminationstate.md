Termination State Service
=========================

This page documents the configuration for Opencast module **terminationstate-impl**.

This module is provided as a convenience when you wish to terminate the Opencast process
or the machine it is running on, when Opencast may still be processing jobs you want to complete.
 It also forms the basis for more sophisticated services that assist the dynamic
scaling of nodes in cloud environments, see [AWS AutoScaling: Termination State Service]( terminationstate.aws.autoscaling.md)

**It does not terminate the Opencast process or the instance itself**.

The default termination state is "none". If the termination state is set to "wait" it will:

* put the node in maintenance mode, to stop accepting new jobs
* periodically check for running jobs
* when no jobs are running it will set the termination state to "ready"

The termination state can then be monitored and the action completed when the state
is changes from "wait" to "ready".

Opencast Service Configuration
------------------------------

The Opencast Termination State Service configuration can be found in the file
`org.opencastproject.terminationstate.impl.TerminationStateServiceImpl.cfg`.

|Key name|Value|Example|
|--------|-----|-------|
|job.polling.period|frequency which to check if jobs are running in seconds|300|
