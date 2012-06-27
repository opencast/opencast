/**
 * Scheduler service implementation classes
 * 
 * The scheduler creates events that should be recorded on a Matterhorn Capture Agent at the specified time and date.
 * It is intended that the user adds the events with the scheduler web interface that is provided in the admin tools 
 * (currently http://localhost:8080/admin/scheduler.html).
 * 
 * The service communicates with the UI over some methods that allow to add an new event, update an event, 
 * delete an event and to list the events in the database in various ways. 
 * The Capture Agent get the events belonging to him as an iCalendar, in which the Dublin Core Metadata and 
 * Capture agent specific technical metadata is included as Base64 encoded files.
 * 
 * This implementation stores the events in a database that is provided to the scheduler service as an javax.sql.DataSource 
 * object that should be available over the OSGI server and index them Solr index. Each event consist of DublinCoreDocument
 * describing event and optionally Properties which contains configurations for capture agent.
 * 
 * There is the possibility to define other ingest endpoints that the capture agent might use. This is defined in the 
 * felix confing with the key "capture.ingest.enpoint.url". If this is missing, the value will be constructed from the 
 * "org.opencastproject.server.url" that is in the felix-config too.  
 */
@javax.xml.bind.annotation.XmlSchema(elementFormDefault = XmlNsForm.UNSET, attributeFormDefault = XmlNsForm.UNSET)
package org.opencastproject.scheduler.impl;

import javax.xml.bind.annotation.XmlNsForm;

