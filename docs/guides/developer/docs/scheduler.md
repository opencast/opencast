Scheduler
=========

Modules
-------

The scheduler service consists of the following modules:

- `scheduler-api`
An API module defining the core scheduler functions and properties.
- `scheduler-impl`
The default implementation of the scheduler service as an OSGi service.
- `scheduler-remote`
The remote implementation of the scheduler service as an OSGi service.
- `scheduler-conflict-handler`
The default implementation of a conflict handler strategy.
- `scheduler-conflict-notifier-comment`
A conflict notifier implementation adding comments on the conflicting events.
- `scheduler-conflict-notifier-email`
A conflict notifier implementation sending an email including all conflicting events.


Database
--------

The scheduler service stores snapshots using the AssetManager and additionally uses two tables:

- `oc_scheduled_extended_event`
  Manages scheduled event meta data such as start date, end date, capture agent ID, and so on.
- `oc_scheduled_last_modified`
  Manages the last recording modification date of a status change on an event sent by the capture agent.

Conflict Handler
----------------

The conflict handler implements a strategy to resolve conflicts by either using the new or the old schedule according the
configuration.


Conflict Notifier
-----------------

There are two implementations of a conflict notifier available and activated by default.

- The *Email conflict notifier* will send an email including all conflicting events to a configured recipient.
- The *Comment conflict notifier* will add a comment on the conflicting event describing the conflict.


API
---

Here is a sample to create a single event with the scheduler Java API.

```java
  public void createEvent(Event event) {
    schedulerService.addEvent(event.getStart(),
                              event.getEnd(),
                              event.getAgentId(),
                              event.getUsers(),
                              event.getMediaPackage(),
                              event.getWfProperties(),
                              event.getCaMetadata(),
                              event.getSource(),
                              "organization-xyz-script";
  }
```
