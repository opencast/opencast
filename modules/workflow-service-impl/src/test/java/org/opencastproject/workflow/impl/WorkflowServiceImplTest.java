/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.impl;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.CONTINUE;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.handler.workflow.ErrorResolutionWorkflowOperationHandler;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowServiceImplTest {

  private static final String REMOTE_SERVICE = "workflowService";
  private static final String REMOTE_HOST = "http://entwinemedia:8080";
  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowDefinition workingDefinition = null;
  private WorkflowDefinition failingDefinitionWithoutErrorHandler = null;
  private WorkflowDefinition failingDefinitionWithErrorHandler = null;
  private WorkflowDefinition pausingWorkflowDefinition = null;
  private MediaPackage mediapackage1 = null;
  private MediaPackage mediapackage2 = null;
  private SucceedingWorkflowOperationHandler succeedingOperationHandler = null;
  private WorkflowOperationHandler failingOperationHandler = null;
  private WorkflowServiceSolrIndex dao = null;
  protected Set<HandlerRegistration> handlerRegistrations = null;
  private Workspace workspace = null;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;
  private SecurityService securityService = null;
  private DefaultOrganization organization = null;

  private File sRoot = null;

  private AccessControlList acl = new AccessControlList();

  protected static final String getStorageRoot() {
    return "." + File.separator + "target" + File.separator + System.currentTimeMillis();
  }

  @Before
  public void setUp() throws Exception {
    // always start with a fresh solr root directory
    sRoot = new File(getStorageRoot());
    try {
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // create operation handlers for our workflows
    succeedingOperationHandler = new SucceedingWorkflowOperationHandler();
    failingOperationHandler = new FailingWorkflowOperationHandler();
    handlerRegistrations = new HashSet<HandlerRegistration>();
    handlerRegistrations.add(new HandlerRegistration("op1", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op3", failingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID,
            new ErrorResolutionWorkflowOperationHandler()));
    handlerRegistrations.add(new HandlerRegistration("opPause", new ResumableTestWorkflowOperationHandler()));
    handlerRegistrations.add(new HandlerRegistration("failOnHost", new FailOnHostWorkflowOperationHandler()));
    handlerRegistrations.add(new HandlerRegistration("failOneTime", new FailOnceWorkflowOperationHandler()));
    handlerRegistrations.add(new HandlerRegistration("failTwice", new FailTwiceWorkflowOperationHandler()));

    scanner = new WorkflowDefinitionScanner();

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    // Add scanner to activate workflow service and store definitions
    service.addWorkflowDefinitionScanner(scanner);

    // security service
    organization = new DefaultOrganization();
    securityService = createNiceMock(SecurityService.class);
    expect(securityService.getUser()).andReturn(SecurityServiceStub.DEFAULT_ORG_ADMIN).anyTimes();
    expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    replay(securityService);

    service.setSecurityService(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(DEFAULT_ORG_ADMIN)
            .anyTimes();
    replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    {
      // This is the asset manager the workflow service itself uses. Further below is the asset manager for the solr
      // index.
      final AssetManager assetManager = createNiceMock(AssetManager.class);
      EasyMock.expect(assetManager.selectProperties(EasyMock.anyString(), EasyMock.anyString()))
              .andReturn(Collections.emptyList())
              .anyTimes();
      EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString())).andReturn(Opt.none()).anyTimes();
      EasyMock.expect(assetManager.snapshotExists(EasyMock.anyString())).andReturn(true).anyTimes();
      EasyMock.replay(assetManager);
      service.setAssetManager(assetManager);
    }

    AuthorizationService authzService = createNiceMock(AuthorizationService.class);
    expect(authzService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    replay(authzService);
    service.setAuthorizationService(authzService);

    List<Organization> organizationList = new ArrayList<Organization>();
    organizationList.add(organization);
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(securityService.getOrganization()).anyTimes();
    expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    MediaPackageMetadataService mds = createNiceMock(MediaPackageMetadataService.class);
    replay(mds);
    service.addMetadataService(mds);

    workspace = createNiceMock(Workspace.class);
    expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    replay(workspace);

    IncidentService incidentService = createNiceMock(IncidentService.class);
    replay(incidentService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, incidentService);
    serviceRegistry.registerHost(REMOTE_HOST, REMOTE_HOST, "remote", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().
            availableProcessors(), Runtime.getRuntime().availableProcessors());
    serviceRegistry.registerService(REMOTE_SERVICE, REMOTE_HOST, "/path", true);
    service.setWorkspace(workspace);

    MessageSender messageSender = createNiceMock(MessageSender.class);
    replay(messageSender);

    dao = new WorkflowServiceSolrIndex();
    dao.setServiceRegistry(serviceRegistry);
    dao.setSecurityService(securityService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.setAuthorizationService(authzService);
    dao.solrRoot = sRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.activate("System Admin");
    service.setDao(dao);
    service.setServiceRegistry(serviceRegistry);
    service.setMessageSender(messageSender);
    service.activate(null);

    InputStream is = null;
    try {
      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml");
      workingDefinition = WorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-2.xml");
      failingDefinitionWithoutErrorHandler = WorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-3.xml");
      failingDefinitionWithErrorHandler = WorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-4.xml");
      pausingWorkflowDefinition = WorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      service.registerWorkflowDefinition(workingDefinition);
      service.registerWorkflowDefinition(failingDefinitionWithoutErrorHandler);
      service.registerWorkflowDefinition(failingDefinitionWithErrorHandler);
      service.registerWorkflowDefinition(pausingWorkflowDefinition);

      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));

      is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml");
      mediapackage1 = mediaPackageBuilder.loadFromXml(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-2.xml");
      mediapackage2 = mediaPackageBuilder.loadFromXml(is);

      Assert.assertNotNull(mediapackage1.getIdentifier());
      Assert.assertNotNull(mediapackage2.getIdentifier());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    AssetManager assetManager = EasyMock.createNiceMock(AssetManager.class);
    Version version = EasyMock.createNiceMock(Version.class);
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    // Just needs to return a mp, not checking which one
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(mediapackage1).anyTimes();
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(organization.getId()).anyTimes();
    EasyMock.expect(snapshot.getVersion()).andReturn(version).anyTimes();
    ARecord aRec = EasyMock.createNiceMock(ARecord.class);
    EasyMock.expect(aRec.getSnapshot()).andReturn(Opt.some(snapshot)).anyTimes();
    Stream<ARecord> recStream = Stream.mk(aRec);
    Predicate p = EasyMock.createNiceMock(Predicate.class);
    EasyMock.expect(p.and(p)).andReturn(p).anyTimes();
    AResult r = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(r.getRecords()).andReturn(recStream).anyTimes();
    Target t = EasyMock.createNiceMock(Target.class);
    ASelectQuery selectQuery = EasyMock.createNiceMock(ASelectQuery.class);
    EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate.class))).andReturn(selectQuery).anyTimes();
    EasyMock.expect(selectQuery.run()).andReturn(r).anyTimes();
    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);
    EasyMock.expect(query.snapshot()).andReturn(t).anyTimes();
    EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String.class))).andReturn(p).anyTimes();
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(selectQuery).anyTimes();
    VersionField v = EasyMock.createNiceMock(VersionField.class);
    EasyMock.expect(v.isLatest()).andReturn(p).anyTimes();
    EasyMock.expect(query.version()).andReturn(v).anyTimes();
    EasyMock.expect(assetManager.createQuery()).andReturn(query).anyTimes();
    EasyMock.replay(assetManager, version, snapshot, aRec, p, r, t, selectQuery, query, v);
    dao.setAssetManager(assetManager);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.deactivate();
    serviceRegistry.unRegisterService(REMOTE_SERVICE, REMOTE_HOST);
    dao.deactivate();
    service.deactivate();
  }

  @SuppressWarnings("unused")
  @Test
  public void testGetWorkflowInstanceById() throws Exception {
    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    // verify that we can retrieve the workflow instance from the service by its ID
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    Assert.assertNotNull(instanceFromDb);
    MediaPackage mediapackageFromDb = instanceFromDb.getMediaPackage();
    Assert.assertNotNull(mediapackageFromDb);
    Assert.assertEquals(mediapackage1.getIdentifier().toString(), mediapackageFromDb.getIdentifier().toString());
    Assert.assertEquals(2, service.countWorkflowInstances());
  }

  @Test
  public void testGetWorkflowByMediaPackageId() throws Exception {
    // Ensure that the database doesn't have a workflow instance with this media package
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(
            0,
            service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage1.getIdentifier().toString()))
                    .size());
    Assert.assertEquals(
            0,
            service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage2.getIdentifier().toString()))
                    .size());

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);
    WorkflowInstance instance3 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance2.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance3.getId()).getState());

    Assert.assertEquals(mediapackage1.getIdentifier().toString(), service.getWorkflowById(instance.getId())
            .getMediaPackage().getIdentifier().toString());
    Assert.assertEquals(mediapackage2.getIdentifier().toString(), service.getWorkflowById(instance2.getId())
            .getMediaPackage().getIdentifier().toString());
    Assert.assertEquals(mediapackage2.getIdentifier().toString(), service.getWorkflowById(instance3.getId())
            .getMediaPackage().getIdentifier().toString());

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediapackage1
            .getIdentifier().toString()));
    Assert.assertEquals(1, workflowsInDb.getItems().length);
  }

  @Test
  public void testGetWorkflowByCreator() throws Exception {
    // Set different creators in the mediapackages
    String manfred = "Dr. Manfred Frisch";
    mediapackage1.addCreator(manfred);
    mediapackage2.addCreator("Somebody else");

    // Ensure that the database doesn't have any workflow instances with media packages with this creator
    Assert.assertEquals(0, service.countWorkflowInstances());

    WorkflowInstance instance1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance instance2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance1.getId()).getState());
    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(instance2.getId()).getState());

    // Build the workflow query
    WorkflowQuery queryForManfred = new WorkflowQuery().withCreator(manfred);

    Assert.assertEquals(1, service.getWorkflowInstances(queryForManfred).getTotalCount());
    Assert.assertEquals(instance1.getMediaPackage().getIdentifier().toString(),
            service.getWorkflowInstances(queryForManfred).getItems()[0].getMediaPackage().getIdentifier().toString());
  }

  @Test
  public void testParentWorkflow() throws Exception {
    WorkflowInstance originalInstance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance childInstance = startAndWait(workingDefinition, mediapackage1, originalInstance.getId(),
            WorkflowState.SUCCEEDED);
    Assert.assertNotNull(service.getWorkflowById(childInstance.getId()).getParentId());
    Assert.assertEquals(originalInstance.getId(), (long) service.getWorkflowById(childInstance.getId()).getParentId());

    // Create a child workflow with a wrong parent id
    try {
      service.start(workingDefinition, mediapackage1, new Long(1876234678), Collections.emptyMap());
      Assert.fail("Workflows should not be started with bad parent IDs");
    } catch (NotFoundException e) {
    } // the exception is expected
  }

  @Test
  public void testGetWorkflowByEpisodeId() throws Exception {
    String mediaPackageId = mediapackage1.getIdentifier().toString();

    // Ensure that the database doesn't have a workflow instance with this episode
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediaPackageId)).size());

    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mediaPackageId));
    Assert.assertEquals(1, workflowsInDb.getItems().length);
  }

  @Test
  public void testGetWorkflowByCurrentOperation() throws Exception {
    // Ensure that the database doesn't have a workflow instance in the "opPause" operation
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery().withCurrentOperation("opPause")).size());

    startAndWait(pausingWorkflowDefinition, mediapackage1, WorkflowState.PAUSED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withCurrentOperation("opPause"));
    Assert.assertEquals(1, workflowsInDb.getItems().length);
  }

  @Test
  public void testGetWorkflowByText() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100)
            .withStartPage(0));
    Assert.assertEquals(1, workflowsInDb.getItems().length);
    Assert.assertEquals(1, service.getWorkflowInstances(new WorkflowQuery().withText("limate")).size());
    Assert.assertEquals(1, service.getWorkflowInstances(new WorkflowQuery().withText("mate")).size());
    Assert.assertEquals(1, service.getWorkflowInstances(new WorkflowQuery().withText("lima")).size());
  }

  @Test
  public void testGetWorkflowSort() throws Exception {
    String contributor1 = "foo";
    String contributor2 = "bar";
    String contributor3 = "baz";

    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());

    // set contributors (a multivalued field)
    mediapackage1.addContributor(contributor1);
    mediapackage1.addContributor(contributor2);
    mediapackage2.addContributor(contributor2);
    mediapackage2.addContributor(contributor3);

    // run the workflows
    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsWithContributor1 = service.getWorkflowInstances(new WorkflowQuery()
            .withContributor(contributor1));
    WorkflowSet workflowsWithContributor2 = service.getWorkflowInstances(new WorkflowQuery()
            .withContributor(contributor2));
    WorkflowSet workflowsWithContributor3 = service.getWorkflowInstances(new WorkflowQuery()
            .withContributor(contributor3));

    Assert.assertEquals(1, workflowsWithContributor1.getTotalCount());
    Assert.assertEquals(2, workflowsWithContributor2.getTotalCount());
    Assert.assertEquals(1, workflowsWithContributor3.getTotalCount());
  }

  @Test
  public void testGetWorkflowByWildcardMatching() throws Exception {
    String searchTerm = "another";
    String searchTermWithoutQuotes = "yet another";
    String searchTermInQuotes = "\"" + searchTermWithoutQuotes + "\"";
    String title = "just" + searchTerm + " " + searchTermInQuotes + " rev129";

    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    mediapackage1.setTitle(title);
    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsWithTitle = service.getWorkflowInstances(new WorkflowQuery().withTitle(searchTerm));
    Assert.assertEquals(1, workflowsWithTitle.getTotalCount());

    WorkflowSet workflowsWithQuotedTitle = service.getWorkflowInstances(new WorkflowQuery()
            .withTitle(searchTermInQuotes));
    Assert.assertEquals(1, workflowsWithQuotedTitle.getTotalCount());

    WorkflowSet workflowsWithUnQuotedTitle = service.getWorkflowInstances(new WorkflowQuery()
            .withTitle(searchTermWithoutQuotes));
    Assert.assertEquals(1, workflowsWithUnQuotedTitle.getTotalCount());
  }

  @Test
  public void testNegativeWorkflowQuery() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);
    startAndWait(failingDefinitionWithoutErrorHandler, mediapackage1, WorkflowState.FAILED);

    WorkflowSet succeededWorkflows = service.getWorkflowInstances(new WorkflowQuery()
            .withState(WorkflowState.SUCCEEDED));
    Assert.assertEquals(2, succeededWorkflows.getItems().length);

    WorkflowSet failedWorkflows = service.getWorkflowInstances(new WorkflowQuery().withState(WorkflowState.FAILED));
    Assert.assertEquals(1, failedWorkflows.getItems().length);

    // Ensure that the "without" queries works
    WorkflowSet notFailedWorkflows = service.getWorkflowInstances(new WorkflowQuery()
            .withoutState(WorkflowState.FAILED));
    Assert.assertEquals(2, notFailedWorkflows.getItems().length);
  }

  protected WorkflowInstance startAndWait(WorkflowDefinition definition, MediaPackage mp, WorkflowState stateToWaitFor)
          throws Exception {
    return startAndWait(definition, mp, null, stateToWaitFor);
  }

  protected WorkflowInstance startAndWait(WorkflowDefinition definition, MediaPackage mp, Long parentId,
          WorkflowState stateToWaitFor) throws Exception {
    WorkflowStateListener stateListener = new WorkflowStateListener(stateToWaitFor);
    service.addWorkflowListener(stateListener);
    WorkflowInstance instance = null;
    synchronized (stateListener) {
      if (parentId == null) {
        instance = service.start(definition, mp);
      } else {
        instance = service.start(definition, mp, parentId, Collections.emptyMap());
      }
      stateListener.wait();
    }
    service.removeWorkflowListener(stateListener);

    return instance;
  }

  protected WorkflowInstance retryAndWait(WorkflowInstance instance, String retryStrategy, WorkflowState stateToWaitFor)
          throws Exception {
    WorkflowStateListener stateListener = new WorkflowStateListener(stateToWaitFor);
    service.addWorkflowListener(stateListener);
    Map<String, String> props = new HashMap<String, String>();
    props.put("retryStrategy", retryStrategy);
    WorkflowInstance wfInstance = null;
    synchronized (stateListener) {
      wfInstance = service.resume(instance.getId(), props);
      stateListener.wait();
    }
    service.removeWorkflowListener(stateListener);

    return wfInstance;
  }

  @Test
  public void testPagedGetWorkflowByText() throws Exception {
    // Ensure that the database doesn't have any workflow instances
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0,
            service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(100).withStartPage(0))
                    .size());

    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED));
    instances.add(startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED));

    Assert.assertEquals(5, service.countWorkflowInstances());
    Assert.assertEquals(5, service.getWorkflowInstances(new WorkflowQuery()).getItems().length);

    // We should get the first two workflows
    WorkflowSet firstTwoWorkflows = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(2)
            .withStartPage(0));
    Assert.assertEquals(2, firstTwoWorkflows.getItems().length);
    Assert.assertEquals(3, firstTwoWorkflows.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the last workflow
    WorkflowSet lastWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Climate").withCount(1)
            .withStartPage(2));
    Assert.assertEquals(1, lastWorkflow.getItems().length);
    Assert.assertEquals(3, lastWorkflow.getTotalCount()); // The total, non-paged number of results should be three

    // We should get the first linguistics (mediapackage2) workflow
    WorkflowSet firstLinguisticsWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Linguistics")
            .withCount(1).withStartPage(0));
    Assert.assertEquals(1, firstLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, firstLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should
                                                                      // be two

    // We should get the second linguistics (mediapackage2) workflow
    WorkflowSet secondLinguisticsWorkflow = service.getWorkflowInstances(new WorkflowQuery().withText("Linguistics")
            .withCount(1).withStartPage(1));
    Assert.assertEquals(1, secondLinguisticsWorkflow.getItems().length);
    Assert.assertEquals(2, secondLinguisticsWorkflow.getTotalCount()); // The total, non-paged number of results should
                                                                       // be two
  }

  @Test
  public void testGetAllWorkflowInstances() throws Exception {
    Assert.assertEquals(0, service.countWorkflowInstances());
    Assert.assertEquals(0, service.getWorkflowInstances(new WorkflowQuery()).size());

    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    WorkflowSet workflowsInDb = service.getWorkflowInstances(new WorkflowQuery());
    Assert.assertEquals(2, workflowsInDb.getItems().length);
  }

  @Test
  public void testFailingOperationWithErrorHandler() throws Exception {
    WorkflowInstance instance = startAndWait(failingDefinitionWithErrorHandler, mediapackage1, WorkflowState.FAILED);

    Assert.assertEquals(WorkflowState.FAILED, service.getWorkflowById(instance.getId()).getState());

    // The second operation should have failed
    Assert.assertEquals(OperationState.FAILED, service.getWorkflowById(instance.getId()).getOperations().get(1)
            .getState());

    // Load a fresh copy
    WorkflowInstance storedInstance = service.getWorkflowById(instance.getId());

    // Make sure the error handler has been added
    Assert.assertEquals(4, storedInstance.getOperations().size());
    Assert.assertEquals("op1", storedInstance.getOperations().get(0).getTemplate());
    Assert.assertEquals("op3", storedInstance.getOperations().get(1).getTemplate());
    Assert.assertEquals("op1", storedInstance.getOperations().get(2).getTemplate());
    Assert.assertEquals("op2", storedInstance.getOperations().get(3).getTemplate());
  }

  @Test
  public void testFailingOperationWithoutErrorHandler() throws Exception {
    WorkflowInstance instance = startAndWait(failingDefinitionWithoutErrorHandler, mediapackage1, WorkflowState.FAILED);
    Assert.assertEquals(WorkflowState.FAILED, service.getWorkflowById(instance.getId()).getState());
  }

  @Test
  public void testRetryStrategyNone() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failOneTime", "fails once", null, true);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.FAILED);

    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getState() == OperationState.FAILED);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getMaxAttempts() == 1);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getFailedAttempts() == 1);
  }

  @Test
  public void testRetryStrategyRetry() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failOneTime", "fails once", null, true);
    opDef.setRetryStrategy(RetryStrategy.RETRY);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.SUCCEEDED);

    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getState() == OperationState.SUCCEEDED);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getMaxAttempts() == 2);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getFailedAttempts() == 1);
  }

  @Test
  public void testRetryStrategyHold() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failOneTime", "fails once", null, true);
    opDef.setRetryStrategy(RetryStrategy.HOLD);
    opDef.setMaxAttempts(2);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.PAUSED);

    WorkflowOperationInstance errorResolutionOperation = service.getWorkflowById(workflow.getId()).getOperations()
            .get(0);
    WorkflowOperationInstance failOneTimeOperation = service.getWorkflowById(workflow.getId()).getOperations().get(1);

    Assert.assertTrue(errorResolutionOperation.getTemplate().equals(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID));
    Assert.assertTrue(errorResolutionOperation.getState() == OperationState.PAUSED);
    Assert.assertTrue(errorResolutionOperation.getFailedAttempts() == 0);

    Assert.assertTrue(failOneTimeOperation.getState() == OperationState.RETRY);
    Assert.assertTrue(failOneTimeOperation.getMaxAttempts() == 2);
    Assert.assertTrue(failOneTimeOperation.getFailedAttempts() == 1);
  }

  @Test
  public void testRetryStrategyHoldMaxAttemptsThree() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failTwice", "fails twice", null, true);
    opDef.setRetryStrategy(RetryStrategy.HOLD);
    opDef.setMaxAttempts(3);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.PAUSED);

    WorkflowOperationInstance errorResolutionOperation = service.getWorkflowById(workflow.getId()).getOperations()
            .get(0);
    WorkflowOperationInstance failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(1);

    Assert.assertTrue(errorResolutionOperation.getTemplate().equals(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID));
    Assert.assertTrue(errorResolutionOperation.getState() == OperationState.PAUSED);
    Assert.assertTrue(errorResolutionOperation.getFailedAttempts() == 0);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.RETRY);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 1);

    // Try operation a second time, simulate user selecting RETRY
    retryAndWait(service.getWorkflowById(workflow.getId()), "RETRY", WorkflowState.PAUSED);

    errorResolutionOperation = service.getWorkflowById(workflow.getId()).getOperations().get(1);
    failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(2);

    Assert.assertTrue(errorResolutionOperation.getTemplate().equals(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID));
    Assert.assertTrue(errorResolutionOperation.getState() == OperationState.PAUSED);
    Assert.assertTrue(errorResolutionOperation.getFailedAttempts() == 0);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.RETRY);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 2);

    // Try operation a third time, it should succeed, simulate user selecting RETRY
    retryAndWait(service.getWorkflowById(workflow.getId()), "RETRY", WorkflowState.SUCCEEDED);

    failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(2);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.SUCCEEDED);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 2);
  }

  @Test
  public void testRetryStrategyHoldFailureByUser() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failTwice", "fails twice", null, true);
    opDef.setRetryStrategy(RetryStrategy.HOLD);
    opDef.setMaxAttempts(3);
    opDef.setFailWorkflowOnException(true);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.PAUSED);

    WorkflowOperationInstance errorResolutionOperation = service.getWorkflowById(workflow.getId()).getOperations()
            .get(0);
    WorkflowOperationInstance failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(1);

    Assert.assertTrue(errorResolutionOperation.getTemplate().equals(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID));
    Assert.assertTrue(errorResolutionOperation.getState() == OperationState.PAUSED);
    Assert.assertTrue(errorResolutionOperation.getFailedAttempts() == 0);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.RETRY);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 1);

    // Try operation a second time, simulate user selecting RETRY
    retryAndWait(service.getWorkflowById(workflow.getId()), "RETRY", WorkflowState.PAUSED);

    errorResolutionOperation = service.getWorkflowById(workflow.getId()).getOperations().get(1);
    failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(2);

    Assert.assertTrue(errorResolutionOperation.getTemplate().equals(WorkflowServiceImpl.ERROR_RESOLUTION_HANDLER_ID));
    Assert.assertTrue(errorResolutionOperation.getState() == OperationState.PAUSED);
    Assert.assertTrue(errorResolutionOperation.getFailedAttempts() == 0);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.RETRY);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 2);

    // Simulate user selecting 'fail'
    retryAndWait(service.getWorkflowById(workflow.getId()), "NONE", WorkflowState.FAILED);

    failTwiceOperation = service.getWorkflowById(workflow.getId()).getOperations().get(2);

    Assert.assertTrue("failTwice".equals(failTwiceOperation.getTemplate()));
    Assert.assertTrue(failTwiceOperation.getState() == OperationState.FAILED);
    Assert.assertTrue(failTwiceOperation.getMaxAttempts() == 3);
    Assert.assertTrue(failTwiceOperation.getFailedAttempts() == 2);
  }

  @Test
  @Ignore
  public void testRetryStrategyFailover() throws Exception {
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("workflow-definition-1");
    def.setTitle("workflow-definition-1");
    def.setDescription("workflow-definition-1");
    service.registerWorkflowDefinition(def);

    WorkflowOperationDefinitionImpl opDef = new WorkflowOperationDefinitionImpl("failOnHost", "fails on host", null,
            true);
    opDef.setRetryStrategy(RetryStrategy.RETRY);
    def.add(opDef);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    WorkflowInstance workflow = startAndWait(def, mp, WorkflowState.SUCCEEDED);

    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getState() == OperationState.SUCCEEDED);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getMaxAttempts() == 2);
    Assert.assertTrue(service.getWorkflowById(workflow.getId()).getOperations().get(0).getFailedAttempts() == 1);
  }

  /**
   * Starts many concurrent workflows to test DB deadlock.
   *
   * @throws Exception
   */
  @Test
  public void testManyConcurrentWorkflows() throws Exception {
    int count = 10;
    Assert.assertEquals(0, service.countWorkflowInstances());
    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();

    WorkflowStateListener stateListener = new WorkflowStateListener(WorkflowState.SUCCEEDED, WorkflowState.FAILED);
    service.addWorkflowListener(stateListener);

    for (int i = 0; i < count; i++) {
      MediaPackage mp = i % 2 == 0 ? mediapackage1 : mediapackage2;
      mp.setIdentifier(new UUIDIdBuilderImpl().createNew());
      instances.add(service.start(workingDefinition, mp, Collections.emptyMap()));
    }

    while (stateListener.countStateChanges() < count) {
      synchronized (stateListener) {
        stateListener.wait();
      }
    }

    Assert.assertEquals(count, service.countWorkflowInstances());
    Assert.assertEquals(count, stateListener.countStateChanges(WorkflowState.SUCCEEDED));
  }

  private WorkflowInstanceImpl setupWorkflowInstanceImpl(long id, String operation, WorkflowState state, Date startDate)
          throws ConfigurationException, MediaPackageException, NotFoundException, ServiceRegistryException {

    Job job = new JobImpl(id);
    job = serviceRegistry.updateJob(job);

    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mediapackage.setDate(startDate);
    mediapackage.setDuration(7200L);

    WorkflowOperationInstanceImpl workflowOperation = new WorkflowOperationInstanceImpl(operation,
            OperationState.PAUSED);
    workflowOperation.setId(id);

    List<WorkflowOperationInstance> workflowOperationInstanceList = new LinkedList<WorkflowOperationInstance>();
    workflowOperationInstanceList.add(workflowOperation);

    WorkflowInstanceImpl workflowInstanceImpl = new WorkflowInstanceImpl();
    workflowInstanceImpl.setMediaPackage(mediapackage);
    workflowInstanceImpl.setState(state);
    workflowInstanceImpl.setId(id);
    workflowInstanceImpl.setOperations(workflowOperationInstanceList);
    return workflowInstanceImpl;
  }

  private void setupJob(long id, String operation, ServiceRegistry mockServiceRegistry)
          throws ServiceRegistryException, NotFoundException {
    ServiceRegistrationJpaImpl serviceRegistrationJpaImpl = EasyMock.createMock(ServiceRegistrationJpaImpl.class);
    expect(serviceRegistrationJpaImpl.getHost()).andReturn("http://localhost:8080");
    expect(serviceRegistrationJpaImpl.getServiceType()).andReturn(operation);
    replay(serviceRegistrationJpaImpl);
    List<String> arguments = new LinkedList<String>();
    arguments.add(Long.toString(id));

    Job job = createNiceMock(Job.class);
    expect(job.getId()).andStubReturn(id);
    expect(job.getCreator()).andStubReturn(securityService.getUser().getUsername());
    expect(job.getOrganization()).andStubReturn(securityService.getOrganization().getId());
    expect(job.getOperation()).andStubReturn("RESUME");
    expect(job.getArguments()).andStubReturn(arguments);
    expect(job.isDispatchable()).andStubReturn(false);
    expect(job.getStatus()).andStubReturn(Job.Status.INSTANTIATED);
    replay(job);
    expect(mockServiceRegistry.getJob(id)).andReturn(job).anyTimes();
    expect(mockServiceRegistry.updateJob(job)).andReturn(job);
  }

  private void setupExceptionJob(long id, Exception e, ServiceRegistry mockServiceRegistry)
          throws ServiceRegistryException, NotFoundException {
    expect(mockServiceRegistry.getJob(id)).andThrow(e).anyTimes();
  }

  private OrganizationDirectoryService setupMockOrganizationDirectoryService() {
    List<Organization> organizations = new LinkedList<Organization>();
    organizations.add(securityService.getOrganization());
    OrganizationDirectoryService orgDirService = EasyMock.createMock(OrganizationDirectoryService.class);
    expect(orgDirService.getOrganizations()).andReturn(organizations).anyTimes();
    replay(orgDirService);
    return orgDirService;
  }

  /**
   * Test for {@link WorkflowServiceImpl#remove(long)}
   *
   * @throws Exception
   *           if anything fails
   */
  @Test
  public void testRemove() throws Exception {
    WorkflowInstance wi1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance wi2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    // reload instances, because operations have no id before
    wi1 = service.getWorkflowById(wi1.getId());
    wi2 = service.getWorkflowById(wi2.getId());

    service.remove(wi1.getId());
    assertEquals(1, service.getWorkflowInstances(new WorkflowQuery()).size());
    for (WorkflowOperationInstance op : wi1.getOperations()) {
      assertEquals(0, serviceRegistry.getChildJobs(op.getId()).size());
    }

    service.remove(wi2.getId());
    assertEquals(0, service.getWorkflowInstances(new WorkflowQuery()).size());
  }

  /**
   * Test for {@link WorkflowServiceImpl#cleanupWorkflowInstances(int, WorkflowState)}
   *
   * @throws Exception
   *           if anything fails
   */
  @Test
  public void testCleanupWorkflowInstances() throws Exception {
    WorkflowInstance wi1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);

    // reload instances, because operations have no id before
    wi1 = service.getWorkflowById(wi1.getId());

    service.cleanupWorkflowInstances(0, WorkflowState.FAILED);
    assertEquals(2, service.getWorkflowInstances(new WorkflowQuery()).size());

    service.cleanupWorkflowInstances(0, WorkflowState.SUCCEEDED);
    assertEquals(0, service.getWorkflowInstances(new WorkflowQuery()).size());
    for (WorkflowOperationInstance op : wi1.getOperations()) {
      assertEquals(0, serviceRegistry.getChildJobs(op.getId()).size());
    }
  }

  class SucceedingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    @Override
    public String getId() {
      return this.getClass().getName();
    }

    @Override
    public String getDescription() {
      return "ContinuingWorkflowOperationHandler";
    }

    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
            throws WorkflowOperationException {
      return createResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
    }
  }

  class FailingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    @Override
    public String getId() {
      return this.getClass().getName();
    }

    @Override
    public String getDescription() {
      return "ContinuingWorkflowOperationHandler";
    }

    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
            throws WorkflowOperationException {
      throw new WorkflowOperationException("this operation handler always fails.  that's the point.");
    }
  }

  class FailOnceWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    /** Whether this handler has been run yet */
    private boolean hasRun = false;

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
     *      org.opencastproject.job.api.JobContext)
     */
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
            throws WorkflowOperationException {
      if (!hasRun) {
        hasRun = true;
        throw new WorkflowOperationException("This operation handler fails on the first run, but succeeds thereafter");
      }
      return createResult(CONTINUE);
    }
  }

  class FailTwiceWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    /** How many times this handler has been run */
    private int times = 0;

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
     *      org.opencastproject.job.api.JobContext)
     */
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
            throws WorkflowOperationException {
      times++;
      if (times < 3) {
        throw new WorkflowOperationException(
                "This operation handler fails on the first two runs, but succeeds thereafter. This is run number "
                        + times + ".");
      }
      return createResult(CONTINUE);
    }
  }

  class FailOnHostWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    private String oldExecutionHost = null;

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
     *      org.opencastproject.job.api.JobContext)
     */
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext jobContext)
            throws WorkflowOperationException {
      if (oldExecutionHost == null) {
        oldExecutionHost = workflowInstance.getCurrentOperation().getExecutionHost();
        throw new WorkflowOperationException("This operation handler fails on the first run on host: "
                + oldExecutionHost);
      }
      if (workflowInstance.getCurrentOperation().getExecutionHost().equals(oldExecutionHost))
        throw new WorkflowOperationException("This operation handler fails on the second run at the same host: "
                + oldExecutionHost);
      return createResult(CONTINUE);
    }

  }
}
