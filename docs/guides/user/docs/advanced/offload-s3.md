[img_select_events]: media/img/select_events.png
[img_confirm_events]: media/img/confirm_events.png
[img_offload_task]: media/img/offload_task.png
[img_offload_s3_1]: media/img/offload_s3_1.png
[img_offload_s3_2]: media/img/offload_s3_2.png

# Offloading to S3

To manually offload a mediapackage to S3 you select the events you wish to offload, and start a task

![img_select_events][]

## Step 1: Confirm the events

![img_confirm_events][]

This page confirms which event(s) you wish to offload

## Step 2: Select the task

![img_offload_task][]

Select the Offload task from the dropdown

## Step 3: Select the offload type

![img_offload_s3_1][]

Select AWS S3 in the list of offload targets

## Step 4: Confirm

![img_offload_s3_2][]

Confirm that the correct values have been set

At this point a workflow will launch which will archive the event to AWS S3.  Accessing this mediapackage will be
transparent to you if it is required for further tasks.
