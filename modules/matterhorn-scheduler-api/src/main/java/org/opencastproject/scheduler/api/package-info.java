/**
 * Scheduler service.
 * The scheduler creates events that should be recorded on a Matterhorn Capture Agent at the specified time and date.
 * It is intended that the user adds the events with the scheduler web interface that is provided in the admin tools
 * (currently http://localhost:8080/admin/scheduler.html).
 *
 * The service communicates with the UI over some methods that allow to add an new event, update an event,
 * delete an event and to list the events in the database in various ways.
 * The Capture Agent get the events belonging to him as an iCalendar, in which the Dublin Core Metadata and
 * Capture agent specific technical metadata is included as Base64 encoded files.
 *
 * This is only the API see the current implementation for more details.
 */
package org.opencastproject.scheduler.api;

