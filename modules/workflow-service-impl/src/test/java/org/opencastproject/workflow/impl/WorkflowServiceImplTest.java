/*
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.CONTINUE;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
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
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowIdentifier;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowServiceDatabaseImpl;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workflow.handler.workflow.ErrorResolutionWorkflowOperationHandler;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  protected Set<HandlerRegistration> handlerRegistrations = null;
  private Workspace workspace = null;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;
  private SecurityService securityService = null;
  private DefaultOrganization organization = null;

  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {

    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));

    try (var in = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml")) {
      mediapackage1 = mediaPackageBuilder.loadFromXml(in);
    }
    try (var in = WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-2.xml")) {
      mediapackage2 = mediaPackageBuilder.loadFromXml(in);
    }

    Assert.assertNotNull(mediapackage1.getIdentifier());
    Assert.assertNotNull(mediapackage2.getIdentifier());
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
      // This is the asset manager the workflow service uses.
      final AssetManager assetManager = createNiceMock(AssetManager.class);
      EasyMock.expect(assetManager.selectProperties(EasyMock.anyString(), EasyMock.anyString()))
              .andReturn(Collections.emptyList())
              .anyTimes();
      EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString()))
          .andReturn(Optional.of(mediapackage1)).anyTimes();
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
    EasyMock.expect(workspace.read(anyObject()))
            .andAnswer(() -> getClass().getResourceAsStream("/dc-1.xml")).anyTimes();
    replay(workspace);

    IncidentService incidentService = createNiceMock(IncidentService.class);
    replay(incidentService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, incidentService);
    serviceRegistry.registerHost(REMOTE_HOST, REMOTE_HOST, "remote", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().
            availableProcessors(), Runtime.getRuntime().availableProcessors());
    serviceRegistry.registerService(REMOTE_SERVICE, REMOTE_HOST, "/path", true);
    service.setWorkspace(workspace);

    WorkflowServiceDatabaseImpl workflowDb = new WorkflowServiceDatabaseImpl();
    workflowDb.setEntityManagerFactory(newEntityManagerFactory(WorkflowServiceDatabaseImpl.PERSISTENCE_UNIT));
    workflowDb.setDBSessionFactory(getDbSessionFactory());
    workflowDb.setSecurityService(securityService);
    workflowDb.activate(null);
    service.setPersistence(workflowDb);

    service.setServiceRegistry(serviceRegistry);
    service.activate(null);

    InputStream is = null;
    try {
      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-exception-handler.xml");
      WorkflowDefinition exceptionHandler = XmlWorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      /* The exception handler workflow definition needs to be registered as the reference to it in
         workflow-definition-3 will be checked */
      scanner.putWorkflowDefinition(
              new WorkflowIdentifier("exception-handler", securityService.getOrganization().getId()), exceptionHandler);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-1.xml");
      workingDefinition = XmlWorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-2.xml");
      failingDefinitionWithoutErrorHandler = XmlWorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-3.xml");
      failingDefinitionWithErrorHandler = XmlWorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

      is = WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-4.xml");
      pausingWorkflowDefinition = XmlWorkflowParser.parseWorkflowDefinition(is);
      IOUtils.closeQuietly(is);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    SearchResult result = EasyMock.createNiceMock(SearchResult.class);
    EasyMock.expect(result.getItems()).andReturn(new SearchResultItem[0]).anyTimes();

    final ElasticsearchIndex index = EasyMock.createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(index.getIndexName()).andReturn("index").anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result).anyTimes();
    EasyMock.replay(result, index);

    service.setIndex(index);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.deactivate();
    serviceRegistry.unRegisterService(REMOTE_SERVICE, REMOTE_HOST);
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
            service.getWorkflowInstancesByMediaPackage(mediapackage1.getIdentifier().toString()).size());
    Assert.assertEquals(
            0,
            service.getWorkflowInstancesByMediaPackage(mediapackage2.getIdentifier().toString()).size());

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

    List<WorkflowInstance> workflowsInDb = service.getWorkflowInstancesByMediaPackage(
            mediapackage1.getIdentifier().toString());
    Assert.assertEquals(1, workflowsInDb.size());
  }

  @Test
  public void testParentWorkflow() throws Exception {
    WorkflowInstance originalInstance = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance childInstance = startAndWait(workingDefinition, mediapackage1, originalInstance.getId(),
            WorkflowState.SUCCEEDED);

    // Create a child workflow with a wrong parent id
    try {
      service.start(workingDefinition, mediapackage1, 1876234678L, Collections.emptyMap());
      Assert.fail("Workflows should not be started with bad parent IDs");
    } catch (NotFoundException e) {
    } // the exception is expected
  }

  @Test
  public void testGetWorkflowByEpisodeId() throws Exception {
    // Ensure that the database doesn't have a workflow instance with this episode
    Assert.assertEquals(0, service.countWorkflowInstances());

    startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);

    List<WorkflowInstance> workflowsInDb = service.getWorkflowInstancesByMediaPackage(
        mediapackage1.getIdentifier().toString());
    Assert.assertEquals(1, workflowsInDb.size());
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
    if (parentId == null) {
      instance = service.start(definition, mp);
    } else {
      instance = service.start(definition, mp, parentId, Collections.emptyMap());
    }
    WorkflowTestSupport.poll(stateListener, 1);
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
    wfInstance = service.resume(instance.getId(), props);
    WorkflowTestSupport.poll(stateListener, 1);
    service.removeWorkflowListener(stateListener);

    return wfInstance;
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
      mp.setIdentifier(IdImpl.fromUUID());
      instances.add(service.start(workingDefinition, mp, Collections.emptyMap()));
    }

    WorkflowTestSupport.poll(stateListener, count);

    Assert.assertEquals(count, service.countWorkflowInstances());
    Assert.assertEquals(count, stateListener.countStateChanges(WorkflowState.SUCCEEDED));
  }

  /**
   * Test for {@link WorkflowServiceImpl#remove(long, boolean)}
   *
   * @throws Exception
   *           if anything fails
   */
  @Test
  public void testRemove() throws Exception {
    WorkflowInstance wi1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    WorkflowInstance wi2 = startAndWait(workingDefinition, mediapackage2, WorkflowState.SUCCEEDED);
    WorkflowInstance wi3 = startAndWait(pausingWorkflowDefinition, mediapackage1, WorkflowState.PAUSED);

    // reload instances, because operations have no id before
    wi1 = service.getWorkflowById(wi1.getId());
    wi2 = service.getWorkflowById(wi2.getId());

    service.remove(wi1.getId());
    assertEquals(2, service.countWorkflowInstances());
    for (WorkflowOperationInstance op : wi1.getOperations()) {
      assertEquals(0, serviceRegistry.getChildJobs(op.getId()).size());
    }

    service.remove(wi2.getId(), false);
    assertEquals(1, service.countWorkflowInstances());

    try {
      service.remove(wi3.getId(), false);
      Assert.fail("A paused workflow shouldn't be removed without using force");
    } catch (WorkflowStateException e) {
      assertEquals(1, service.countWorkflowInstances());
    }

    try {
      service.remove(wi3.getId(), true);
      assertEquals(0, service.countWorkflowInstances());
    } catch (WorkflowStateException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testRemoveWithDeletedCreator() throws Exception {
    WorkflowInstance wi1 = startAndWait(workingDefinition, mediapackage1, WorkflowState.SUCCEEDED);
    // reload instances, because operations have no id before
    wi1 = service.getWorkflowById(wi1.getId());

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(null).anyTimes();
    replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);
    service.remove(wi1.getId());
    assertEquals(0, service.countWorkflowInstances());
    for (WorkflowOperationInstance op : wi1.getOperations()) {
      assertEquals(0, serviceRegistry.getChildJobs(op.getId()).size());
    }
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
    assertEquals(2, service.countWorkflowInstances());

    service.cleanupWorkflowInstances(0, WorkflowState.SUCCEEDED);
    assertEquals(0, service.countWorkflowInstances());
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
