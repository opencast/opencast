/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.scheduler.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static net.fortuna.ical4j.model.Component.VEVENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AVAILABLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_REPLACED_BY;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_REPLACES;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SUBJECT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE;
import static org.opencastproject.util.EqualsUtil.eqMap;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.properties;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.persistence.SchedulerServiceDatabaseImpl;
import org.opencastproject.scheduler.impl.solr.SchedulerServiceSolrIndex;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class SchedulerServiceImplTest {

  private WorkflowService workflowService;
  private SeriesService seriesService;
  private IngestService ingestService;

  private String persistenceStorage;
  private SchedulerServiceImpl schedSvc;
  private DublinCoreCatalogService dcSvc;

  // persistent properties
  private ComboPooledDataSource pooledDataSource;
  private SchedulerServiceDatabaseImpl schedulerDatabase;

  // index
  private String indexStorage;
  private SchedulerServiceSolrIndex index;

  private String seriesIdentifier;

  private Map<String, String> wfProperties = new HashMap<String, String>();
  private Map<String, String> wfPropertiesUpdated = new HashMap<String, String>();

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    wfProperties.put("test", "true");
    wfProperties.put("clear", "all");

    wfPropertiesUpdated.put("test", "false");
    wfPropertiesUpdated.put("skip", "true");

    long startTime = System.currentTimeMillis();
    indexStorage = PathSupport.concat("target", Long.toString(startTime));
    index = new SchedulerServiceSolrIndex(indexStorage);
    dcSvc = new DublinCoreCatalogService();
    index.setDublinCoreService(dcSvc);
    index.activate(null);

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    persistenceStorage = PathSupport.concat("target", "db" + startTime + ".h2.db");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + startTime);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase.setPersistenceProvider(new PersistenceProvider());
    schedulerDatabase.setPersistenceProperties(props);
    dcSvc = new DublinCoreCatalogService();
    schedulerDatabase.setDublinCoreService(dcSvc);
    schedulerDatabase.activate(null);

    WorkflowInstance workflowInstance = getSampleWorkflowInstance();
    // workflow service
    workflowService = EasyMock.createMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map<String, String>) EasyMock.anyObject())).andAnswer(new IAnswer<WorkflowInstance>() {
      @Override
      public WorkflowInstance answer() throws Throwable {
        return getSampleWorkflowInstance();
      }
    }).anyTimes();
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();
    EasyMock.expect(workflowService.stop(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();
    // update may be called multiple times
    workflowService.update((WorkflowInstance) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();

    seriesIdentifier = Long.toString(System.currentTimeMillis());
    DublinCoreCatalog seriesCatalog = getSampleSeriesDublinCoreCatalog(seriesIdentifier);

    seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries(EasyMock.eq(seriesIdentifier))).andReturn(seriesCatalog).anyTimes();

    ingestService = EasyMock.createNiceMock(IngestService.class);

    EasyMock.replay(workflowService, seriesService, ingestService);

    schedSvc = new SchedulerServiceImpl();

    // Set the mocked interfaces
    schedSvc.setWorkflowService(workflowService);
    schedSvc.setSeriesService(seriesService);
    schedSvc.setIndex(index);
    schedSvc.setPersistence(schedulerDatabase);
    schedSvc.setIngestService(ingestService);

    schedSvc.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    schedSvc = null;
    index.deactivate();
    index = null;
    FileUtils.deleteQuietly(new File(indexStorage));
    schedulerDatabase.deactivate(null);
    pooledDataSource.close();
    schedulerDatabase = null;
    pooledDataSource = null;
    FileUtils.deleteQuietly(new File(persistenceStorage));
  }

  protected WorkflowInstance getSampleWorkflowInstance() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    Random gen = new Random(System.currentTimeMillis());
    instance.setId(gen.nextInt());
    instance.setMediaPackage(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew());
    instance.setState(WorkflowState.PAUSED);

    WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl(SchedulerServiceImpl.SCHEDULE_OPERATION_ID,
            OperationState.PAUSED);
    List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
    operations.add(op);
    instance.setOperations(operations);
    return instance;
  }

  protected DublinCoreCatalog getSampleSeriesDublinCoreCatalog(String seriesID) {
    DublinCoreCatalog dc = dcSvc.newInstance();
    dc.set(PROPERTY_IDENTIFIER, seriesID);
    dc.set(PROPERTY_TITLE, "Demo series");
    dc.set(PROPERTY_LICENSE, "demo");
    dc.set(PROPERTY_PUBLISHER, "demo");
    dc.set(PROPERTY_CREATOR, "demo");
    dc.set(PROPERTY_SUBJECT, "demo");
    dc.set(PROPERTY_SPATIAL, "demo");
    dc.set(PROPERTY_RIGHTS_HOLDER, "demo");
    dc.set(PROPERTY_EXTENT, "3600000");
    dc.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_LANGUAGE, "demo");
    dc.set(PROPERTY_IS_REPLACED_BY, "demo");
    dc.set(PROPERTY_TYPE, "demo");
    dc.set(PROPERTY_AVAILABLE, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_REPLACES, "demo");
    dc.set(PROPERTY_CONTRIBUTOR, "demo");
    dc.set(PROPERTY_DESCRIPTION, "demo");
    return dc;
  }

  protected DublinCoreCatalog generateEvent(String captureDeviceID, Option<Long> eventId, Option<String> title,
          Date startTime, Date endTime) {
    DublinCoreCatalog dc = dcSvc.newInstance();
    dc.set(PROPERTY_IDENTIFIER, Long.toString(eventId.getOrElse(1L)));
    dc.set(PROPERTY_TITLE, title.getOrElse("Demo event"));
    dc.set(PROPERTY_CREATOR, "demo");
    dc.set(PROPERTY_SUBJECT, "demo");
    dc.set(PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startTime, endTime), Precision.Second));
    dc.set(PROPERTY_SPATIAL, captureDeviceID);
    dc.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_LANGUAGE, "demo");
    dc.set(PROPERTY_CONTRIBUTOR, "demo");
    dc.set(PROPERTY_DESCRIPTION, "demo");
    return dc;
  }

  protected DublinCoreCatalog generateEvent(String captureDeviceID, Date startTime, Date endTime) {
    return generateEvent(captureDeviceID, none(0L), none(""), startTime, endTime);
  }

  protected Properties generateCaptureAgentMetadata(String captureDeviceID) {
    Properties properties = new Properties();
    properties.put("event.title", "Demo event");
    properties.put("capture.device.id", captureDeviceID);
    return properties;
  }

  @Test
  public void testPersistence() throws Exception {

    DublinCoreCatalog event = generateEvent("demo", new Date(), new Date(System.currentTimeMillis() + 60000));

    Long id = schedSvc.addEvent(event, wfProperties);
    Assert.assertNotNull(id);
    DublinCoreCatalog eventLoaded = schedSvc.getEventDublinCore(id);
    assertEquals(event.getFirst(PROPERTY_TITLE), eventLoaded.getFirst(PROPERTY_TITLE));

    eventLoaded.set(PROPERTY_TITLE, "Something more");
    schedSvc.updateEvent(id, eventLoaded, wfPropertiesUpdated);

    DublinCoreCatalog eventReloaded = schedSvc.getEventDublinCore(id);
    assertEquals("Something more", eventReloaded.getFirst(PROPERTY_TITLE));

    Properties caProperties = generateCaptureAgentMetadata("demo");
    schedSvc.updateCaptureAgentMetadata(caProperties, tuple(id, eventLoaded));
    Assert.assertNotNull(schedSvc.getEventCaptureAgentConfiguration(id));
  }

  @Test
  public void testEventManagement() throws Exception {

    DublinCoreCatalog event = generateEvent("testdevice", new Date(System.currentTimeMillis() - 2000),
            new Date(System.currentTimeMillis() + 60000));
    event.set(PROPERTY_TITLE, "Demotitle");
    Properties caProperties = generateCaptureAgentMetadata("testdevice");
    Long id = schedSvc.addEvent(event, wfProperties);
    schedSvc.updateCaptureAgentMetadata(caProperties, tuple(id, schedSvc.getEventDublinCore(id)));

    // test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    SchedulerQuery filter = new SchedulerQuery().setSpatial("testdevice");
    try {
      String icalString = schedSvc.getCalendar(filter);
      cal = calBuilder.build(IOUtils.toInputStream(icalString, "UTF-8"));
      ComponentList vevents = cal.getComponents(VEVENT);
      for (int i = 0; i < vevents.size(); i++) {
        PropertyList attachments = ((VEvent) vevents.get(i)).getProperties(Property.ATTACH);
        for (int j = 0; j < attachments.size(); j++) {
          String attached = ((Property) attachments.get(j)).getValue();
          String filename = ((Property) attachments.get(j)).getParameter("X-APPLE-FILENAME").getValue();
          attached = new String(Base64.decodeBase64(attached));
          if ("org.opencastproject.capture.agent.properties".equals(filename)) {
            Assert.assertTrue(attached.contains("capture.device.id=testdevice"));
          }
          if ("episode.xml".equals(filename)) {
            Assert.assertTrue(attached.contains("Demotitle"));
          }
        }
      }
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } catch (ParserException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    // test for upcoming events (it should not be in there).
    List<DublinCoreCatalog> upcoming = schedSvc.search(new SchedulerQuery().setStartsFrom(new Date())).getCatalogList();
    Assert.assertTrue(upcoming.isEmpty());

    List<DublinCoreCatalog> all = schedSvc.search(null).getCatalogList();
    assertEquals(1, all.size());

    all = schedSvc.search(new SchedulerQuery().setSpatial("somedevice")).getCatalogList();
    Assert.assertTrue(upcoming.isEmpty());

    // update event
    event.set(PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(new Date(
            System.currentTimeMillis() + 180000), new Date(System.currentTimeMillis() + 600000)), Precision.Second));

    schedSvc.updateEvent(id, event, wfPropertiesUpdated);

    // test for upcoming events (now it should be there)
    upcoming = schedSvc.search(new SchedulerQuery().setStartsFrom(new Date())).getCatalogList();
    assertEquals(1, upcoming.size());

    // delete event
    schedSvc.removeEvent(id);
    try {
      schedSvc.getEventDublinCore(id);
      Assert.fail();
    } catch (NotFoundException e) {
      // this is an expected exception
    }
  }

  @Test
  public void testFindConflictingEvents() throws Exception {

    long currentTime = System.currentTimeMillis();
    DublinCoreCatalog eventA = generateEvent("Device A", new Date(currentTime + 10 * 1000), new Date(
            currentTime + 3610000));
    DublinCoreCatalog eventB = generateEvent("Device A", new Date(currentTime + 24 * 60 * 60 * 1000), new Date(
            currentTime + 25 * 60 * 60 * 1000));
    DublinCoreCatalog eventC = generateEvent("Device C", new Date(currentTime - 60 * 60 * 1000), new Date(
            currentTime - 10 * 60 * 1000));
    DublinCoreCatalog eventD = generateEvent("Device D", new Date(currentTime + 10 * 1000), new Date(
            currentTime + 3610000));

    schedSvc.addEvent(eventA, wfProperties);
    schedSvc.addEvent(eventB, wfProperties);
    schedSvc.addEvent(eventC, wfProperties);
    schedSvc.addEvent(eventD, wfProperties);

    List<DublinCoreCatalog> allEvents = schedSvc.search(null).getCatalogList();
    assertEquals(4, allEvents.size());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<DublinCoreCatalog> events = schedSvc.findConflictingEvents("Some Other Device", start, end).getCatalogList();
    assertEquals(0, events.size());

    events = schedSvc.findConflictingEvents("Device A", start, end).getCatalogList();
    assertEquals(1, events.size());

    events = schedSvc.findConflictingEvents("Device A", "FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA", start,
            new Date(start.getTime() + (48 * 60 * 60 * 1000)), new Long(36000), "America/Chicago").getCatalogList();
    assertEquals(2, events.size());
  }

  @Test
  public void testCalendarCutoff() throws Exception {
    long currentTime = System.currentTimeMillis();
    DublinCoreCatalog eventA = generateEvent("Device A", new Date(currentTime + 10 * 1000), new Date(currentTime
            + (60 * 60 * 1000)));
    DublinCoreCatalog eventB = generateEvent("Device A", new Date(currentTime + (20 * 24 * 60 * 60 * 1000)), new Date(
            currentTime + (20 * 25 * 60 * 60 * 1000)));

    schedSvc.addEvent(eventA, wfProperties);
    schedSvc.addEvent(eventB, wfProperties);

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    SchedulerQuery filter = new SchedulerQuery().setSpatial("Device A").setEndsFrom(start).setStartsTo(end);
    List<DublinCoreCatalog> events = schedSvc.search(filter).getCatalogList();
    assertEquals(1, events.size());
  }

  /**
   * Create an event with a start date 1 minute in the past and an end date 60 minutes in to the future. Make sure the
   * event is listed when asking for the schedule of the capture agent.
   */
  @Test
  public void testCalendarCutoffWithStartedEvent() throws Exception {
    long currentTime = System.currentTimeMillis();
    Date startDate = new Date(currentTime - 10 * 1000);
    Date endDate = new Date(currentTime + (60 * 60 * 1000));
    DublinCoreCatalog eventA = generateEvent("Device A", startDate, endDate);
    schedSvc.addEvent(eventA, wfProperties);

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    SchedulerQuery filter = new SchedulerQuery().setSpatial("Device A").setEndsFrom(start).setStartsTo(end);
    List<DublinCoreCatalog> events = schedSvc.search(filter).getCatalogList();
    assertEquals(1, events.size());
  }

  @Test
  public void testSpatial() throws Exception {
    long currentTime = System.currentTimeMillis();
    DublinCoreCatalog eventA = generateEvent("Device A", new Date(currentTime + 10 * 1000), new Date(currentTime
            + (60 * 60 * 1000)));
    DublinCoreCatalog eventB = generateEvent("Device B", new Date(currentTime + 10 * 1000), new Date(currentTime
            + (60 * 60 * 1000)));

    schedSvc.addEvent(eventA, wfProperties);
    schedSvc.addEvent(eventB, wfProperties);

    SchedulerQuery filter = new SchedulerQuery().setSpatial("Device");
    List<DublinCoreCatalog> events = schedSvc.search(filter).getCatalogList();
    assertEquals(0, events.size());

    filter = new SchedulerQuery().setSpatial("Device A");
    events = schedSvc.search(filter).getCatalogList();
    assertEquals(1, events.size());

    filter = new SchedulerQuery().setSpatial("Device B");
    events = schedSvc.search(filter).getCatalogList();
    assertEquals(1, events.size());

    filter = new SchedulerQuery().setText("Device");
    events = schedSvc.search(filter).getCatalogList();
    assertEquals(2, events.size());
  }

  @Test
  public void testCalendarNotModified() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.replay(request);

    SchedulerRestService restService = new SchedulerRestService();
    restService.setService(schedSvc);
    restService.setDublinCoreService(dcSvc);

    String device = "Test Device";

    // Store an event
    final DublinCoreCatalog event = generateEvent(device, new Date(), new Date(System.currentTimeMillis() + 60000));
    final long eventId = schedSvc.addEvent(event, wfProperties);

    // Request the calendar without specifying an etag. We should get a 200 with the icalendar in the response body
    Response response = restService.getCalendar(device, null, null, request);
    Assert.assertNotNull(response.getEntity());
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    final String etag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("If-None-Match")).andAnswer(new IAnswer<String>() {
      @Override
      public String answer() throws Throwable {
        return etag;
      }
    }).anyTimes();
    EasyMock.replay(request);

    // Request using the etag from the first response. We should get a 304 (not modified)
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    Assert.assertNull(response.getEntity());

    // Update the event
    schedSvc.updateEvent(eventId, event, wfPropertiesUpdated);

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
    String secondEtag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    Assert.assertNotNull(secondEtag);
    Assert.assertFalse(etag.equals(secondEtag));
  }

  @Test
  public void testUpdateEvent() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final String initialTitle = "Recording 1";
    final DublinCoreCatalog initalEvent = generateEvent("Device A", none(0L), some(initialTitle), new Date(
            currentTime + 10 * 1000), new Date(currentTime + 3610000));
    final Long eventId = schedSvc.addEvent(initalEvent, wfProperties);
    schedSvc.updateCaptureAgentMetadata(
            properties(tuple("org.opencastproject.workflow.config.archiveOp", "true"),
                    tuple("org.opencastproject.workflow.definition", "full")), tuple(eventId, initalEvent));
    final Properties initalCaProps = schedSvc.getEventCaptureAgentConfiguration(eventId);
    System.out.println("Added event " + eventId);
    checkEvent(eventId, initalCaProps, initialTitle);
    // do single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", some(eventId), some(updatedTitle1), new Date(
            currentTime + 10 * 1000), new Date(currentTime + 3610000));
    schedSvc.updateEvent(eventId, updatedEvent1, wfPropertiesUpdated);
    checkEvent(eventId, initalCaProps, updatedTitle1);
    // do bulk update
    final String updatedTitle2 = "Recording 3";
    final String expectedTitle2 = "Recording 3 1";
    final DublinCoreCatalog updatedEvent2 = generateEvent("Device A", none(0L), some(updatedTitle2), new Date(
            currentTime + 10 * 1000), new Date(currentTime + 3610000));
    schedSvc.updateEvents(list(eventId), updatedEvent2);
    checkEvent(eventId, initalCaProps, expectedTitle2);
  }

  @Test
  /**
   * Test for failure updating past events
   *  This test construct new SchedulerService to mock the getCurrentDate method
   * @throws Exception
   */
  public void testUpdateExpiredEvent() throws Exception {

    SchedulerServiceImpl schedSvc2 = EasyMock.createMockBuilder(SchedulerServiceImpl.class)
            .addMockedMethod("getCurrentDate").createMock();

    // Mock the getCurrentDate method to skip to the future
    long currentTime = System.currentTimeMillis();
    Date futureSystemDate = new Date(currentTime + 6610000);
    EasyMock.expect(schedSvc2.getCurrentDate()).andReturn(futureSystemDate).anyTimes();
    EasyMock.replay(schedSvc2);

    // Set the mocked interfaces
    schedSvc2.setWorkflowService(workflowService);
    schedSvc2.setSeriesService(seriesService);
    schedSvc2.setIndex(index);
    schedSvc2.setPersistence(schedulerDatabase);
    schedSvc2.setIngestService(ingestService);

    schedSvc2.activate(null);

    final String initialTitle = "Recording 1";
    final DublinCoreCatalog initalEvent = generateEvent("Device A", none(0L), some(initialTitle), new Date(
            currentTime + 10 * 1000), new Date(currentTime + 3610000));
    Long eventId = null;
    try {
      eventId = schedSvc2.addEvent(initalEvent, wfProperties);
      schedSvc2.updateCaptureAgentMetadata(
              properties(tuple("org.opencastproject.workflow.config.archiveOp", "true"),
                      tuple("org.opencastproject.workflow.definition", "full")), tuple(eventId, initalEvent));

    } catch (Exception e) {
      System.out.println("Exception " + e.getClass().getCanonicalName() + " message " + e.getMessage());
    }

    final Properties initalCaProps = schedSvc.getEventCaptureAgentConfiguration(eventId);
    System.out.println("Added event " + eventId);
    checkEvent(eventId, initalCaProps, initialTitle);

    // test single update
    try {
      final String updatedTitle1 = "Recording 2";
      final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", some(eventId), some(updatedTitle1), new Date(
              currentTime + 10 * 1000), new Date(currentTime + 3610000));
      schedSvc2.updateEvent(eventId, updatedEvent1, wfPropertiesUpdated);
      checkEvent(eventId, initalCaProps, updatedTitle1);

      Assert.fail("Schedule should not update a recording that has ended (single)");
    } catch (SchedulerException e) {
      System.out.println("Expected exception: " + e.getMessage());
    }

    try { // test bulk update
      final String updatedTitle2 = "Recording 3";
      final String expectedTitle2 = "Recording 3 1";
      final DublinCoreCatalog updatedEvent2 = generateEvent("Device A", none(0L), some(updatedTitle2), new Date(
              currentTime + 10 * 1000), new Date(currentTime + 3610000));
      schedSvc2.updateEvents(list(eventId), updatedEvent2);
      checkEvent(eventId, initalCaProps, expectedTitle2);

      Assert.fail("Schedule should not update a recording that has ended (multi)");
    } catch (SchedulerException e) {
      System.out.println("Expected exception: " + e.getMessage());
    } finally {
      schedSvc2 = null;
    }
  }

  private void checkEvent(long eventId, Properties initialCaProps, String title) throws Exception {
    final Properties updatedCaProps = (Properties) initialCaProps.clone();
    updatedCaProps.setProperty("event.title", title);
    assertTrue("CA properties", eqMap(updatedCaProps, schedSvc.getEventCaptureAgentConfiguration(eventId)));
    assertEquals(Long.toString(eventId), schedSvc.getEventDublinCore(eventId).getFirst(PROPERTY_IDENTIFIER));
    assertEquals("DublinCore title", title, schedSvc.getEventDublinCore(eventId).getFirst(PROPERTY_TITLE));
    checkIcalFeed(updatedCaProps, title);
  }

  private void checkIcalFeed(Properties caProps, String title) throws Exception {
    final String cs = schedSvc.getCalendar(new SchedulerQuery());
    final Calendar cal = new CalendarBuilder().build(new StringReader(cs));
    assertEquals("number of entries", 1, cal.getComponents().size());
    for (Object co : cal.getComponents()) {
      final Component c = (Component) co;
      assertEquals("SUMMARY property should contain the DC title", title, c.getProperty(Property.SUMMARY).getValue());
      final Monadics.ListMonadic<Property> attachments = mlist(c.getProperties(Property.ATTACH)).map(
              Misc.<Object, Property> cast());
      // episode dublin core
      final List<DublinCoreCatalog> dcsIcal = attachments
              .filter(byParamNameAndValue("X-APPLE-FILENAME", "episode.xml")).map(parseDc.o(decodeBase64).o(getValue))
              .value();
      assertEquals("number of episode DCs", 1, dcsIcal.size());
      assertEquals("dcterms:title", title, dcsIcal.get(0).getFirst(PROPERTY_TITLE));
      // capture agent properties
      final List<Properties> caPropsIcal = attachments
              .filter(byParamNameAndValue("X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"))
              .map(parseProperties.o(decodeBase64).o(getValue)).value();
      assertEquals("number of CA property sets", 1, caPropsIcal.size());
      assertTrue("CA properties", eqMap(caProps, caPropsIcal.get(0)));
    }
  }

  private Function<Property, Boolean> byParamNameAndValue(final String name, final String value) {
    return new Function<Property, Boolean>() {
      @Override
      public Boolean apply(Property p) {
        final Parameter param = p.getParameter(name);
        return param != null && param.getValue().equals(value);
      }
    };
  }

  private static Function<Property, String> getValue = new Function<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getValue();
    }
  };

  private static Function<String, String> decodeBase64 = new Function<String, String>() {
    @Override
    public String apply(String base64) {
      return new String(Base64.decodeBase64(base64));
    }
  };

  private static Function<String, DublinCoreCatalog> parseDc = new Function<String, DublinCoreCatalog>() {
    @Override
    public DublinCoreCatalog apply(String s) {
      return new DublinCoreCatalogImpl(IOUtils.toInputStream(s));
    }
  };

  private static Function<String, Properties> parseProperties = new Function.X<String, Properties>() {
    @Override
    public Properties xapply(String s) throws Exception {
      final Properties p = new Properties();
      p.load(new StringReader(s));
      return p;
    }
  };
}
