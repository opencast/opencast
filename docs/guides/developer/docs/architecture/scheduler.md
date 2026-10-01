Scheduler
=========

The scheduler service manages recordings that have been booked for the future: when, on which capture agent, and
with what workflow configuration. For the protocol capture agents use to pick up their schedule and report back
recordings, see the [capture agent guide](capture-agent/capture-agent.md); this page covers the scheduler service's
own internals instead.

Modules
-------

The scheduler service consists of the following modules:

- `scheduler-api`
An API module defining the core scheduler functions and properties.
- `scheduler-impl`
The default implementation of the scheduler service as an OSGi service.
- `scheduler-remote`
The remote implementation of the scheduler service as an OSGi service.

Database
--------

A scheduled event is, underneath, an AssetManager snapshot: the same storage the rest of Opencast uses for actual
recordings, just taken before any media exists. Fields that don't fit a media package — start and end time, which
capture agent, which users — are kept separately in two scheduler-specific tables:

- `oc_scheduled_extended_event`
  The actual scheduling data per event: start date, end date, capture agent ID, and so on.
- `oc_scheduled_last_modified`
  One row per capture agent, touched on every create, update, or delete of any of that agent's events. This is
  what lets the calendar endpoint a capture agent polls respond with an HTTP ETag, letting an agent skip
  reprocessing its schedule when nothing has actually changed for it.

Conflict Detection and Recurring Events
------------------------------------------

Creating or updating an event checks for other events already scheduled on the *same capture agent* with an
overlapping time window; if any are found, the call fails with a `SchedulerConflictException` rather than silently
double-booking the agent.

A series of recurring events can be scheduled in one call by passing an iCal `RRule` instead of a single start and
end time. The service expands the rule into a list of individual time periods and creates one independent event
per period, each from the same template media package but with its own generated ID. There is no ongoing link
between them afterward — each is a regular scheduled event like any other, subject to the same per-agent conflict
check as the rest.

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
                            Optional.of("organization-xyz-script"));
}
```
