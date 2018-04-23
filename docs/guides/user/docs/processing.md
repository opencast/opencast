# Overview
Processing Tasks can be started on Events that have successfully been added to the Opencast asset management (i.e. they
are not in processing, scheduled or failed state).

A Task is essentially applying a workflow on an Event. A Task can be started on one or multiple events by **selecting
the Event(s) > Actions > Schedule Task**. Select the workflow and press Submit to start the Task.

>It is currently not possible to stop a running Task.


## Taks Statuses

* **INSTANTIATED**: The task on the event is being prepared
* **RUNNING**: The task is currently running
* **STOPPED**: The task has stopped. Note: Stopping a task can only be achieved using the REST endpoint
* **PAUSED**: The task is paused and requires manual action (for example cutting) to proceed
* **SUCCEEDED**: The task has been run successfully
* **FAILED**: The task has failed. See the Event’s details for more information
* **FAILING**: The current task is about to fail and processing artifacts are being cleaned up
