LifeCycle Management
====================

The goal of LifeCycle Management is to allows users to automate administrative tasks that are specific to their use
case. For example:

" We want to automatically retract videos of lecture recordings that have been uploaded during the winter semester 2024
at the end of the semester. Lecture recordings are always organized in a series. Conferences should be excluded from
this, as well as all lectures by the VIP Albert Einstein."

To that end, user can create LifeCycle Policies. They allow users to define
- what should happen (action),
- to what it should happen to (target/object) and
- when it should happen (time).

The different components of a policy are described below in greater detail.

## Policy Action

For now there is only one action that can be specified, called "START_WORKFLOW". “START_WORKFLOW” takes a workflow
definition ID (and workflow configuration) to define which workflow should be run. For example, in order to retract
events we would want to run the “retract” workflow. What exactly the “retract” workflow does is then up to the admins
of each institution, i.e. can be customized by each institution as they see fit.

## Policy Target Type

Determines which entity the policy should be applied to. The two entities we have in Opencast are events
or series. Currently only EVENTS are supported.

## Policy Filters

Criteria that further narrow down which entities the policy should be applied to. Filters make use of an entities'
metadata. A policy can have none, one or multiple filters. For example, if the policy should only run on a certain
series it may have the filter “Series identifier is equal to ID-av-portal”. To have the policy apply to all videos of a
semester it may have the two filters “Created Date > 2023-12-20” and “Created Date < 2024-02-30” to delete all videos
from December 20 2023 to February 30 2024.

## Policy dates

The point in time when an action should be performed. There are three types of action dates:
- SPECIFIC_DATE: A fixed date, for example 2024-02-31 at 11:30. This means the policy will be applied exactly once at
  that point in time.
- REPEATING: A periodic date, for example “Every day at “02:00 AM”. This means the policy will be applied to all events
  that is has not been applied to yet every time.
- ALWAYS: No date. This means the policy will applied to all events that is has not been applied to yet as soon as
  possible. Using this is discouraged, as it can become quite resource intensive.

LifeCycle policies are currently only available through their rest endpoint and don't show up anywhere else.


