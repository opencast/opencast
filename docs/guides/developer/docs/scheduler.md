Scheduler
=========

Modules
-------

The scheduler service consists of the following modules:

- `matterhorn-scheduler-api`
An API module defining the core scheduler functions and properties.
- `matterhorn-scheduler-impl`
The default implementation of the scheduler service as an OSGi service using the AssetManager as it's backend storage.
- `matterhorn-scheduler-remote`
The remote implementation of the scheduler service as an OSGi service.
- `matterhorn-scheduler-conflict-handler`
The default implementation of a conflict handler strategy.
- `matterhorn-scheduler-conflict-notifier-comment`
A conflict notifier implementation adding comments on the conflicting events.
- `matterhorn-scheduler-conflict-notifier-email`
A conflict notifier implementation sending an email including all conflicting events.


Database
--------

The scheduler service uses three tables:

- `mh_scheduled_extended_event`
  Manages extended scheduled event metadata. Not used yet.
- `mh_scheduled_last_modified`
  Manages the last recording modification date of a status change on an event sent by the capture agent.
- `mh_scheduled_transaction`
  Manages the active transactions.

Conflict Handler
----------------

The conflict handler implements a strategy to resolve conflicts by either using the new or the old schedule according the
configuration.


Conflict Notifier
-----------------

There are two implementations of a conflict notifier available and activated by default.

- The *Email conflict notifier* will send an email including all conflicting events to a configured recipient.
- The *Comment conflict notifier* will add a comment on the conflicting event describing the conflict.


Default API
-----------

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
                              event.getOptOut(),
                              event.getSource(),
                              "organization-xyz-script";
  }
```


Transactional API
-----------------

Here is a simplified code example of how to use the Scheduler Java API to synchronize timetable events to scheduled
events. It is mandatory that the media package identifier is stable for the created events of the timetable system.

```java
  public void syncTimeTable(List<Event> timeTableEvents) {
    SchedulerTransaction tx = null;
    try {
      logger.info("Sync timetable to scheduler | start");
      tx = schedulerService.createTransaction("timetable");
      for (Event event : timeTableEvents) {
        tx.addEvent(
                event.getStart(),
                event.getEnd(),
                event.getAgentId(),
                event.getUsers(),
                event.getMediaPackage(),
                event.getWfProperties(),
                event.getCaMetadata(),
                event.getOptOut());
      }
      tx.commit();
      logger.info("Sync timetable to scheduler | end");
    } catch (Exception e) {
      logger.error("Sync timetable to scheduler | error\n{}", e);
      if (tx != null) {
        logger.error("Sync timetable to scheduler | rollback transaction");
        try {
          tx.rollback();
        } catch (Exception e2) {
          logger.error("Sync timetable to scheduler | error doing rollback\n{}", e2);
        }
      }
    }
  }
```
