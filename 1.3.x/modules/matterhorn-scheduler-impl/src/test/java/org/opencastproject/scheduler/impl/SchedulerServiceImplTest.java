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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.persistence.SchedulerServiceDatabaseImpl;
import org.opencastproject.scheduler.impl.solr.SchedulerServiceSolrIndex;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
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
import net.fortuna.ical4j.model.ComponentList;
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

  private String persistenceStorage;
  private SchedulerServiceImpl schedulerService;
  private DublinCoreCatalogService dcService;

  // persistent properties
  private ComboPooledDataSource pooledDataSource;
  private SchedulerServiceDatabaseImpl schedulerDatabase;

  // index
  private String indexStorage;
  private SchedulerServiceSolrIndex index;

  private String seriesIdentifier;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {

    long startTime = System.currentTimeMillis();
    indexStorage = PathSupport.concat("target", Long.toString(startTime));
    index = new SchedulerServiceSolrIndex(indexStorage);
    dcService = new DublinCoreCatalogService();
    index.setDublinCoreService(dcService);
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
    dcService = new DublinCoreCatalogService();
    schedulerDatabase.setDublinCoreService(dcService);
    schedulerDatabase.activate(null);

    WorkflowInstance workflowInstance = getSampleWorkflowInstance();

    WorkflowService workflowService = EasyMock.createMock(WorkflowService.class);
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
    workflowService.update((WorkflowInstance) EasyMock.anyObject());

    seriesIdentifier = Long.toString(System.currentTimeMillis());
    DublinCoreCatalog seriesCatalog = getSampleSeriesDublinCoreCatalog(seriesIdentifier);

    SeriesService seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries(EasyMock.eq(seriesIdentifier))).andReturn(seriesCatalog).anyTimes();

    EasyMock.replay(workflowService, seriesService);

    schedulerService = new SchedulerServiceImpl();
    schedulerService.setWorkflowService(workflowService);
    schedulerService.setSeriesService(seriesService);
    schedulerService.setIndex(index);
    schedulerService.setPersistence(schedulerDatabase);

    schedulerService.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    schedulerService = null;
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
    DublinCoreCatalog dc = dcService.newInstance();
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

  protected DublinCoreCatalog generateEvent(String captureDeviceID, Date startTime, Date endTime) {
    DublinCoreCatalog dc = dcService.newInstance();
    dc.set(PROPERTY_TITLE, "Demo event");
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

  protected Properties generateCaptureAgentMetadata(String captureDeviceID) {
    Properties properties = new Properties();
    properties.put("event.title", "Demo event");
    properties.put("capture.device.id", captureDeviceID);
    return properties;
  }

  @Test
  public void testPersistence() throws Exception {

    DublinCoreCatalog event = generateEvent("demo", new Date(), new Date(System.currentTimeMillis() + 60000));

    Long id = schedulerService.addEvent(event);
    Assert.assertNotNull(id);
    DublinCoreCatalog eventLoaded = schedulerService.getEventDublinCore(id);
    Assert.assertEquals(event.getFirst(PROPERTY_TITLE), eventLoaded.getFirst(PROPERTY_TITLE));

    eventLoaded.set(PROPERTY_TITLE, "Something more");
    schedulerService.updateEvent(eventLoaded);

    DublinCoreCatalog eventReloaded = schedulerService.getEventDublinCore(id);
    Assert.assertEquals("Something more", eventReloaded.getFirst(PROPERTY_TITLE));

    Properties caProperties = generateCaptureAgentMetadata("demo");
    schedulerService.updateCaptureAgentMetadata(caProperties, id);
    Assert.assertNotNull(schedulerService.getEventCaptureAgentConfiguration(id));
  }

  @Test
  public void testEventManagement() throws Exception {

    DublinCoreCatalog event = generateEvent("testdevice", new Date(System.currentTimeMillis() - 2000),
            new Date(System.currentTimeMillis() + 60000));
    event.set(PROPERTY_TITLE, "Demotitle");
    Properties caProperties = generateCaptureAgentMetadata("testdevice");
    Long id = schedulerService.addEvent(event);
    schedulerService.updateCaptureAgentMetadata(caProperties, id);

    // test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    SchedulerQuery filter = new SchedulerQuery().setSpatial("testdevice");
    try {
      String icalString = schedulerService.getCalendar(filter);
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
    List<DublinCoreCatalog> upcoming = schedulerService.search(new SchedulerQuery().setStartsFrom(new Date()))
            .getCatalogList();
    Assert.assertTrue(upcoming.isEmpty());

    List<DublinCoreCatalog> all = schedulerService.search(null).getCatalogList();
    Assert.assertEquals(1, all.size());

    all = schedulerService.search(new SchedulerQuery().setSpatial("somedevice")).getCatalogList();
    Assert.assertTrue(upcoming.isEmpty());

    // update event
    event.set(PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(new Date(
            System.currentTimeMillis() + 180000), new Date(System.currentTimeMillis() + 600000)), Precision.Second));

    schedulerService.updateEvent(event);

    // test for upcoming events (now it should be there)
    upcoming = schedulerService.search(new SchedulerQuery().setStartsFrom(new Date())).getCatalogList();
    Assert.assertEquals(1, upcoming.size());

    // delete event
    schedulerService.removeEvent(id);
    try {
      schedulerService.getEventDublinCore(id);
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

    schedulerService.addEvent(eventA);
    schedulerService.addEvent(eventB);
    schedulerService.addEvent(eventC);
    schedulerService.addEvent(eventD);

    List<DublinCoreCatalog> allEvents = schedulerService.search(null).getCatalogList();
    Assert.assertEquals(4, allEvents.size());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<DublinCoreCatalog> events = schedulerService.findConflictingEvents("Some Other Device", start, end)
            .getCatalogList();
    Assert.assertEquals(0, events.size());

    events = schedulerService.findConflictingEvents("Device A", start, end).getCatalogList();
    Assert.assertEquals(1, events.size());

    events = schedulerService.findConflictingEvents("Device A", "FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA", start,
            new Date(start.getTime() + (48 * 60 * 60 * 1000)), new Long(36000)).getCatalogList();
    Assert.assertEquals(2, events.size());
  }

  @Test
  public void testCalendarNotModified() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.replay(request);

    SchedulerRestService restService = new SchedulerRestService();
    restService.setService(schedulerService);
    restService.setDublinCoreService(dcService);

    String device = "Test Device";

    // Store an event
    DublinCoreCatalog event = generateEvent(device, new Date(), new Date(System.currentTimeMillis() + 60000));
    schedulerService.addEvent(event);

    // Request the calendar without specifying an etag. We should get a 200 with the icalendar in the response body
    Response response = restService.getCalendar(device, null, request);
    Assert.assertNotNull(response.getEntity());
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
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
    response = restService.getCalendar(device, null, request);
    Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    Assert.assertNull(response.getEntity());

    // Update the event
    schedulerService.updateEvent(event);

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, request);
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
    String secondEtag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    Assert.assertNotNull(secondEtag);
    Assert.assertFalse(etag.equals(secondEtag));
  }
}
