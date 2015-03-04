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

import static net.fortuna.ical4j.model.Component.VEVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.persistence.SchedulerServiceDatabaseImpl;
import org.opencastproject.scheduler.impl.solr.SchedulerServiceSolrIndex;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.mchange.v2.c3p0.ComboPooledDataSource;

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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class SchedulerServiceImplTest {

  private WorkflowService workflowService;
  private SeriesService seriesService;
  private Workspace workspace;
  private MessageSender messageSender;
  private MessageReceiver messageReceiver;

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

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(new URI("http://localhost:8080/test")).anyTimes();

    messageSender = EasyMock.createNiceMock(MessageSender.class);

    final BaseMessage baseMessageMock = EasyMock.createNiceMock(BaseMessage.class);

    messageReceiver = EasyMock.createNiceMock(MessageReceiver.class);
    EasyMock.expect(
            messageReceiver.receiveSerializable(EasyMock.anyString(),
                    EasyMock.anyObject(MessageSender.DestinationType.class))).andStubReturn(
                            new FutureTask<Serializable>(new Callable<Serializable>() {
                              @Override
                              public Serializable call() throws Exception {
                                return baseMessageMock;
                              }
                            }));

    EasyMock.replay(workflowService, seriesService, workspace, messageSender, baseMessageMock, messageReceiver);

    schedSvc = new SchedulerServiceImpl();

    // Set the mocked interfaces
    schedSvc.setWorkflowService(workflowService);
    schedSvc.setSeriesService(seriesService);
    schedSvc.setIndex(index);
    schedSvc.setPersistence(schedulerDatabase);
    schedSvc.setWorkspace(workspace);
    schedSvc.setMessageSender(messageSender);
    schedSvc.setMessageReceiver(messageReceiver);
    schedSvc.setDublinCoreCatalogService(new DublinCoreCatalogService());

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

    // Update the event and clear to cache to make sure it's reloaded
    schedSvc.updateEvent(eventId, event, wfPropertiesUpdated);
    schedSvc.lastModifiedCache.invalidateAll();

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
  public void testEventStatus() throws Exception {
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

    String mediaPackageId = schedSvc.getMediaPackageId(eventId);
    Assert.assertFalse(schedSvc.isOptOut(mediaPackageId));
    Assert.assertFalse(schedSvc.isBlacklisted(mediaPackageId));

    // do opt out update
    schedSvc.updateOptOutStatus(mediaPackageId, true);
    Assert.assertTrue(schedSvc.isOptOut(mediaPackageId));

    // do blacklist update
    schedSvc.updateBlacklistStatus(mediaPackageId, true);
    Assert.assertTrue(schedSvc.isBlacklisted(mediaPackageId));
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
    schedSvc2.setWorkspace(workspace);
    schedSvc2.setMessageSender(messageSender);
    schedSvc2.setMessageReceiver(messageReceiver);

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

  @Test
  /**
   * Test that opted out and blacklisted events don't end up in the calendar but regular events do.
   * @throws Exception
   */
  public void testGetCalendarInputRegularOptedOutBlacklistedExpectsOnlyRegularEvents() throws Exception {
    SchedulerServiceImpl schedulerServiceImpl = EasyMock.createMockBuilder(SchedulerServiceImpl.class)
            .addMockedMethod("getCurrentDate").createMock();

    // Mock the getCurrentDate method to skip to the future
    long currentTime = System.currentTimeMillis();
    Date futureSystemDate = new Date(currentTime + 6610000);
    EasyMock.expect(schedulerServiceImpl.getCurrentDate()).andReturn(futureSystemDate).anyTimes();
    EasyMock.replay(schedulerServiceImpl);

    // Set the mocked interfaces
    schedulerServiceImpl.setWorkflowService(workflowService);
    schedulerServiceImpl.setSeriesService(seriesService);
    schedulerServiceImpl.setIndex(index);
    schedulerServiceImpl.setPersistence(schedulerDatabase);
    schedulerServiceImpl.setWorkspace(workspace);
    schedulerServiceImpl.setMessageSender(messageSender);
    schedulerServiceImpl.setMessageReceiver(messageReceiver);
    schedulerServiceImpl.setDublinCoreCatalogService(new DublinCoreCatalogService());

    int optedOutCount = 3;
    int blacklistedCount = 5;
    int bothCount = 7;
    int regularCount = 9;
    String optedOutPrefix = "OptedOut";
    String blacklistedPrefix = "Blacklisted";
    String bothPrefix = "Both";
    String regularPrefix = "Regular";

    List<Long> optedOutEvents = createEvents(optedOutPrefix, optedOutCount, schedulerServiceImpl, true, false);
    assertEquals(optedOutCount, optedOutEvents.size());
    List<Long> blacklistedEvents = createEvents(blacklistedPrefix, blacklistedCount, schedulerServiceImpl, false, true);
    assertEquals(blacklistedCount, blacklistedEvents.size());
    List<Long> bothOptedOutEventsAndBlacklisted = createEvents(bothPrefix, bothCount, schedulerServiceImpl, true, true);
    assertEquals(bothCount, bothOptedOutEventsAndBlacklisted.size());
    List<Long> regularEvents = createEvents(regularPrefix, regularCount, schedulerServiceImpl, false, false);
    assertEquals(regularCount, regularEvents.size());

    checkEventStatus(schedulerDatabase, optedOutEvents, true, false);
    checkEventStatus(schedulerDatabase, blacklistedEvents, false, true);
    checkEventStatus(schedulerDatabase, bothOptedOutEventsAndBlacklisted, true, true);
    checkEventStatus(schedulerDatabase, regularEvents, false, false);

    SchedulerQuery query = new SchedulerQuery();
    String calendar = schedulerServiceImpl.getCalendar(query);

    assertEquals("There shouldn't be any events that are opted out.", -1, calendar.indexOf(optedOutPrefix));
    assertEquals("There shouldn't be any events that are blacklisted.", -1, calendar.indexOf(blacklistedPrefix));
    assertEquals("There shouldn't be any events that are both blacklisted and opted out.", -1,
            calendar.indexOf(bothPrefix));
    assertEquals("All of the regular events should be in the calendar.", regularCount,
            getCountFromString(regularPrefix, calendar));
  }

  private int getCountFromString(String searchTerm, String value) {
    int count = 0;
    int index = 0;
    while (value.indexOf(searchTerm, index) != -1) {
      count++;
      index = value.indexOf(searchTerm, index) + searchTerm.length();
    }
    return count;
  }

  private void checkEventStatus(SchedulerServiceDatabase schedulerServiceDatabase, List<Long> events, boolean optedOut,
          boolean blacklisted) throws NotFoundException, SchedulerServiceDatabaseException {
    for (Long eventId : events) {
      assertEquals(optedOut, schedulerServiceDatabase.isOptOut(eventId));
      assertEquals(blacklisted, schedulerServiceDatabase.isBlacklisted(eventId));
    }
  }

  private List<Long> createEvents(String titlePrefix, int number, SchedulerServiceImpl schedulerServiceImpl,
          boolean optedout, boolean blacklisted) {
    List<Long> optedOutEvents = new ArrayList<Long>();
    final long currentTime = System.currentTimeMillis();
    for (int i = 0; i < number; i++) {

      final DublinCoreCatalog event = generateEvent("Device A", none(0L), some(titlePrefix + "-" + i), new Date(
              currentTime + 10 * 1000), new Date(currentTime + 3610000));
      try {
        long eventId = schedulerServiceImpl.addEvent(event, wfProperties);
        String mediaPackageId = schedulerServiceImpl.getMediaPackageId(eventId);
        schedulerServiceImpl.updateOptOutStatus(mediaPackageId, optedout);
        schedulerServiceImpl.updateBlacklistStatus(mediaPackageId, blacklisted);
        optedOutEvents.add(eventId);
      } catch (Exception e) {
        System.out.println("Exception " + e.getClass().getCanonicalName() + " message " + e.getMessage());
      }
    }
    return optedOutEvents;
  }

  @Test
  public void getCutOffDateWorksForDaylightSavingsTime() {
    DateTimeZone zurichDateTimeZone = DateTimeZone.forID("Europe/Zurich");
    /**
     * Is a date and time that isn't around a daylight savings time Jan 1st, 2013 @ 3:00am. After the buffer is
     * subtracted it should be Jan 1st, 2013 @ 2:00am local time, Jan 1st, 2013 1:00am GMT.
     */
    DateTime normalTime = new DateTime(2013, 1, 2, 3, 7, zurichDateTimeZone);
    DateTime normalCutoff = SchedulerServiceImpl.getCutoffDate(3600, normalTime);
    assertEquals(2013, normalCutoff.getYear());
    assertEquals(01, normalCutoff.getMonthOfYear());
    assertEquals(02, normalCutoff.getDayOfMonth());
    assertEquals(01, normalCutoff.getHourOfDay());
    assertEquals(07, normalCutoff.getMinuteOfHour());

    /**
     * Sunday, March 31, 2013, 2:00:00 AM clocks were turned forward 1 hour to Sunday, March 31, 2013, 3:00:00 AM local
     * daylight time instead. Therefore, March 31, 2013 @ 3:01am back 1 hour will be March 31, 2013 @ 1:01am Zurich time
     * and 0:01am GMT due to time zone difference of 1 hour.
     */
    DateTime forwardTime = new DateTime(2013, 03, 31, 3, 1, zurichDateTimeZone);
    DateTime forwardCutOff = SchedulerServiceImpl.getCutoffDate(3600, forwardTime);
    assertEquals(2013, forwardCutOff.getYear());
    assertEquals(03, forwardCutOff.getMonthOfYear());
    assertEquals(31, forwardCutOff.getDayOfMonth());
    assertEquals(00, forwardCutOff.getHourOfDay());
    assertEquals(01, forwardCutOff.getMinuteOfHour());

    /**
     * Sunday, October 27, 2013, 3:00:00 AM clocks were turned backward 1 hour to Sunday, October 27, 2013, 2:00:00 AM
     * local standard time instead. Therefore, Oct. 27th, 2013 @ 2:01am back 1 hour is 1:01 am local time and 23:01 GMT.
     */
    DateTime backwardTime = new DateTime(2013, 10, 27, 2, 1, zurichDateTimeZone);
    DateTime backwardCutoff = SchedulerServiceImpl.getCutoffDate(3600, backwardTime);
    assertEquals(2013, backwardCutoff.getYear());
    assertEquals(10, backwardCutoff.getMonthOfYear());
    assertEquals(26, backwardCutoff.getDayOfMonth());
    assertEquals(23, backwardCutoff.getHourOfDay());
    assertEquals(01, backwardCutoff.getMinuteOfHour());
  }

  @Test(expected = SchedulerException.class)
  public void removeScheduledRecordingsBeforeBufferInputSchedulerExceptionExpectsIllegalStateException()
          throws SchedulerException, SchedulerServiceDatabaseException {
    SchedulerServiceIndex index = EasyMock.createMock(SchedulerServiceIndex.class);
    EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andThrow(
            new SchedulerServiceDatabaseException("Mock exception"));
    EasyMock.replay(index);

    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setIndex(index);
    service.setMessageSender(messageSender);
    service.setMessageReceiver(messageReceiver);
    service.removeScheduledRecordingsBeforeBuffer(0);
  }

  @Test
  public void removeScheduledRecordingsBeforeBufferInputEmptyFinishedSchedulesExpectsNoException()
          throws SchedulerException, SchedulerServiceDatabaseException {
    // Setup data
    LinkedList<DublinCoreCatalog> catalogs = new LinkedList<DublinCoreCatalog>();
    DublinCoreCatalogList list = new DublinCoreCatalogList(catalogs, catalogs.size());
    // Setup index
    SchedulerServiceIndex index = EasyMock.createMock(SchedulerServiceIndex.class);
    EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andReturn(list);
    EasyMock.replay(index);
    // Run test
    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setIndex(index);
    service.removeScheduledRecordingsBeforeBuffer(0);
  }

  @Test
  public void removeScheduledRecordingsBeforeBufferInputOneEventEmptyExpectsNoEventDeleted() throws SchedulerException,
          SchedulerServiceDatabaseException {
    // Setup data
    DublinCoreCatalog catalog = EasyMock.createMock(DublinCoreCatalog.class);
    EasyMock.expect(catalog.getFirst(EasyMock.anyObject(EName.class))).andReturn(null);
    EasyMock.replay(catalog);
    LinkedList<DublinCoreCatalog> catalogs = new LinkedList<DublinCoreCatalog>();
    catalogs.add(catalog);
    DublinCoreCatalogList list = new DublinCoreCatalogList(catalogs, catalogs.size());
    // Setup index
    SchedulerServiceIndex index = EasyMock.createMock(SchedulerServiceIndex.class);
    EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andReturn(list);
    EasyMock.replay(index);
    // Run test
    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setMessageSender(messageSender);
    service.setMessageReceiver(messageReceiver);
    service.setIndex(index);
    service.removeScheduledRecordingsBeforeBuffer(0);
  }

  private URI[] createMediapackageURIs(long[] ids) throws URISyntaxException {
    URI[] uris = new URI[ids.length];
    for (int i = 0; i < ids.length; i++) {
      long id = ids[i];
      URI uri = new URI("location" + id);
      uris[i] = uri;
    }
    return uris;
  }

  private Workspace createWorkspace(URI[] uris, boolean throwException) throws NotFoundException, IOException {
    Workspace workspace = EasyMock.createMock(Workspace.class);
    for (int i = 0; i < uris.length; i++) {
      workspace.delete(uris[i]);
      if (throwException && i == 0) {
        EasyMock.expectLastCall().andThrow(new NotFoundException("Mock Exception"));
      } else {
        EasyMock.expectLastCall();
      }
    }
    EasyMock.replay(workspace);
    return workspace;
  }

  private WorkflowService createWorkflowService(long[] ids, URI[] uris) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    WorkflowService workflowService = EasyMock.createMock(WorkflowService.class);
    for (int i = 0; i < ids.length; i++) {
      long id = ids[i];
      URI uri = uris[i];
      // Setup elements
      MediaPackageElement element = EasyMock.createMock(MediaPackageElement.class);
      EasyMock.expect(element.getURI()).andReturn(uri);
      EasyMock.replay(element);
      MediaPackageElement[] elements = { element };
      MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
      EasyMock.expect(mediaPackage.getElements()).andReturn(elements);
      EasyMock.replay(mediaPackage);
      // Setup WorkflowInstance
      WorkflowInstance workflowServiceInstance = EasyMock.createMock(WorkflowInstance.class);
      EasyMock.expect(workflowServiceInstance.getMediaPackage()).andReturn(mediaPackage);
      EasyMock.replay(workflowServiceInstance);
      EasyMock.expect(workflowService.stop(id)).andReturn(workflowServiceInstance);
    }
    EasyMock.replay(workflowService);
    return workflowService;
  }

  private SchedulerServiceDatabase setupPersistence(long[] ids) throws NotFoundException,
          SchedulerServiceDatabaseException {
    SchedulerServiceDatabase persistence = EasyMock.createMock(SchedulerServiceDatabase.class);
    EasyMock.expect(persistence.getMediaPackageId(EasyMock.anyLong())).andReturn("uuid").anyTimes();
    for (long id : ids) {
      persistence.deleteEvent(id);
      EasyMock.expectLastCall();
    }
    EasyMock.replay(persistence);
    return persistence;
  }

  private SchedulerServiceIndex createIndex(long[] ids, boolean throwSchedulerException,
          boolean throwNullPointerException) throws SchedulerServiceDatabaseException {
    LinkedList<DublinCoreCatalog> catalogs = new LinkedList<DublinCoreCatalog>();
    for (long id : ids) {
      DublinCoreCatalog catalog = EasyMock.createMock(DublinCoreCatalog.class);
      EasyMock.expect(catalog.getFirst(EasyMock.anyObject(EName.class))).andReturn(Long.toString(id));
      EasyMock.replay(catalog);
      catalogs.add(catalog);
    }
    DublinCoreCatalogList list = new DublinCoreCatalogList(catalogs, catalogs.size());
    SchedulerServiceIndex index = EasyMock.createMock(SchedulerServiceIndex.class);
    if (throwSchedulerException) {
      EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andThrow(
              new SchedulerException("Mock scheduler exception"));
    } else if (throwNullPointerException) {
      EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andThrow(
              new NullPointerException("Mock null exception"));
    } else {
      EasyMock.expect(index.search(EasyMock.anyObject(SchedulerQuery.class))).andReturn(list);
      for (long id : ids) {
        index.delete(id);
        EasyMock.expectLastCall();
      }
    }
    EasyMock.replay(index);
    return index;
  }

  @Test
  public void removeScheduledRecordingsBeforeBufferInputOneEventOneIDExpectsEventDeleted() throws SchedulerException,
          NotFoundException, UnauthorizedException, SchedulerServiceDatabaseException, URISyntaxException, IOException,
          WorkflowException {
    // Setup data
    long[] ids = { 1L };
    URI[] uris = createMediapackageURIs(ids);
    // Run test
    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setIndex(createIndex(ids, false, false));
    service.setWorkspace(createWorkspace(uris, false));
    service.setWorkflowService(createWorkflowService(ids, uris));
    service.setPersistence(setupPersistence(ids));
    service.setMessageSender(messageSender);
    service.setMessageReceiver(messageReceiver);
    service.removeScheduledRecordingsBeforeBuffer(0);
  }

  @Test
  public void scanInputMultipleEventOneIDExpectsEventsDeleted() throws SchedulerException, NotFoundException,
          UnauthorizedException, SchedulerServiceDatabaseException, URISyntaxException, IOException, WorkflowException {
    // Setup data
    long[] ids = { 4L, 5L, 6L };
    URI[] uris = createMediapackageURIs(ids);
    // Run test
    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setIndex(createIndex(ids, false, false));
    service.setWorkspace(createWorkspace(uris, false));
    service.setWorkflowService(createWorkflowService(ids, uris));
    service.setPersistence(setupPersistence(ids));
    service.setMessageSender(messageSender);
    service.setMessageReceiver(messageReceiver);
    service.removeScheduledRecordingsBeforeBuffer(0);
  }

  @Test
  public void scanInputWorkSpaceExceptionExpectsProperEventDeleted() throws SchedulerException, NotFoundException,
          UnauthorizedException, SchedulerServiceDatabaseException, URISyntaxException, IOException, WorkflowException {
    // Setup data
    long[] ids = { 7L, 8L, 9L };
    // Setup workspace
    URI[] uris = createMediapackageURIs(ids);
    // Run test
    SchedulerServiceImpl service = new SchedulerServiceImpl();
    service.setIndex(createIndex(ids, false, false));
    service.setWorkspace(createWorkspace(uris, true));
    service.setWorkflowService(createWorkflowService(ids, uris));
    service.setPersistence(setupPersistence(ids));
    service.setMessageSender(messageSender);
    service.setMessageReceiver(messageReceiver);
    service.removeScheduledRecordingsBeforeBuffer(0);
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
      return DublinCores.read(IOUtils.toInputStream(s));
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
