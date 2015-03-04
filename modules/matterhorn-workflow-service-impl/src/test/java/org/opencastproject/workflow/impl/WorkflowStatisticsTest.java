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
package org.opencastproject.workflow.impl;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
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
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport.OperationReport;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test cases for the implementation at {@link WorkflowStatistics}.
 */
public class WorkflowStatisticsTest {

  /** Number of operations per workflow */
  private static final int WORKFLOW_DEFINITION_COUNT = 2;

  /** Number of operations per workflow */
  private static final int OPERATION_COUNT = 3;

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private List<WorkflowDefinition> workflowDefinitions = null;
  protected Set<HandlerRegistration> workflowHandlers = null;
  private WorkflowServiceSolrIndex dao = null;
  private Workspace workspace = null;
  private SecurityService securityService = null;
  private MediaPackage mediaPackage = null;

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

    workflowDefinitions = new ArrayList<WorkflowDefinition>();
    workflowHandlers = new HashSet<HandlerRegistration>();
    String opId = "op";
    WorkflowOperationDefinition op = new WorkflowOperationDefinitionImpl(opId, "Pausing operation", null, true);
    WorkflowOperationHandler opHandler = new ResumableTestWorkflowOperationHandler(opId, Action.PAUSE, Action.CONTINUE);
    HandlerRegistration handler = new HandlerRegistration(opId, opHandler);
    workflowHandlers.add(handler);

    // create operation handlers for our workflows
    for (int i = 1; i <= WORKFLOW_DEFINITION_COUNT; i++) {
      WorkflowDefinition workflowDef = new WorkflowDefinitionImpl();
      workflowDef.setId("def-" + i);
      for (int opCount = 1; opCount <= OPERATION_COUNT; opCount++) {
        workflowDef.add(op);
      }
      workflowDefinitions.add(workflowDef);
    }

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return workflowHandlers;
      }
    };

    scanner = new WorkflowDefinitionScanner();
    service.addWorkflowDefinitionScanner(scanner);

    // security service
    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(SecurityServiceStub.DEFAULT_ORG_ADMIN).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    service.setSecurityService(securityService);

    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(DEFAULT_ORG_ADMIN)
            .anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    Organization organization = new DefaultOrganization();
    List<Organization> organizationList = new ArrayList<Organization>();
    organizationList.add(organization);
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);
    service.setMessageSender(messageSender);

    MediaPackageMetadataService mds = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    // Register the workflow definitions
    for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
      service.registerWorkflowDefinition(workflowDefinition);
    }

    // Mock the workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    // Mock the service registry
    ServiceRegistryInMemoryImpl serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService,
            userDirectoryService, organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    // Create the workflow database (solr)
    dao = new WorkflowServiceSolrIndex();
    dao.solrRoot = sRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.setSecurityService(securityService);
    dao.setServiceRegistry(serviceRegistry);
    dao.setAuthorizationService(authzService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.activate("System Admin");
    service.setDao(dao);
    service.setServiceRegistry(serviceRegistry);
    service.setSecurityService(securityService);
    service.activate(null);

    // Crate a media package
    InputStream is = null;
    try {
      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      is = WorkflowStatisticsTest.class.getResourceAsStream("/mediapackage-1.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
      IOUtils.closeQuietly(is);
      Assert.assertNotNull(mediaPackage.getIdentifier());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    // Register the workflow service with the service registry
    serviceRegistry.registerService(service);
  }

  @After
  public void tearDown() throws Exception {
    dao.deactivate();
    service.deactivate();
  }

  /**
   * Tests whether the workflow service statistics are gathered correctly while there are no workflows active in the
   * system. Since no workflows are known, not even empty definition reports are to be expected.
   */
  @Test
  public void testEmptyStatistics() throws Exception {
    WorkflowStatistics stats = service.getStatistics();
    assertEquals(0, stats.getDefinitions().size());
    assertEquals(0, stats.getFailed());
    assertEquals(0, stats.getFailing());
    assertEquals(0, stats.getFinished());
    assertEquals(0, stats.getInstantiated());
    assertEquals(0, stats.getPaused());
    assertEquals(0, stats.getRunning());
    assertEquals(0, stats.getStopped());
    assertEquals(0, stats.getTotal());
  }

  class IndividualWorkflowListener implements WorkflowListener {

    private long id = -1;

    IndividualWorkflowListener(long id) {
      this.id = id;
    }

    @Override
    public void operationChanged(WorkflowInstance workflow) {
    }

    @Override
    public void stateChanged(WorkflowInstance workflow) {
      if (workflow.getId() != id)
        return;
      synchronized (IndividualWorkflowListener.this) {
        WorkflowState state = workflow.getState();
        if (state.equals(WorkflowState.PAUSED) || state.equals(WorkflowState.SUCCEEDED)) {
          notifyAll();
        }
      }
    }
  }

  /**
   * Tests whether the workflow service statistics are gathered correctly.
   */
  @Test
  public void testStatistics() throws Exception {

    // Start the workflows and advance them in "random" order. With every definition, an instance is started for every
    // operation that is part of the definition. So we end up with an instance per definition and operation, and there
    // are no two workflows that are in the same operation.

    int total = 0;
    int paused = 0;
    int failed = 0;
    int failing = 0;
    int instantiated = 0;
    int running = 0;
    int stopped = 0;
    int succeeded = 0;

    WorkflowStateListener listener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(listener);

    List<WorkflowInstance> instances = new ArrayList<WorkflowInstance>();
    for (WorkflowDefinition def : workflowDefinitions) {
      for (int j = 0; j < def.getOperations().size(); j++) {
        mediaPackage.setIdentifier(new UUIDIdBuilderImpl().createNew());
        instances.add(service.start(def, mediaPackage));
        total++;
        paused++;
      }
    }

    // Wait for all the workflows to go into "paused" state
    synchronized (listener) {
      while (listener.countStateChanges() < WORKFLOW_DEFINITION_COUNT * OPERATION_COUNT) {
        listener.wait();
      }
    }

    service.removeWorkflowListener(listener);

    // Resume all of them, so some will be finished, some won't
    int j = 0;
    for (WorkflowInstance instance : instances) {
      WorkflowListener instanceListener = new IndividualWorkflowListener(instance.getId());
      service.addWorkflowListener(instanceListener);
      for (int k = 0; k <= (j % OPERATION_COUNT - 1); k++) {
        synchronized (instanceListener) {
          service.resume(instance.getId(), null);
          instanceListener.wait();
        }
      }
      j++;
    }

    // TODO: Add failed, failing, stopped etc. workflows as well

    // Get the statistics
    WorkflowStatistics stats = service.getStatistics();
    assertEquals(failed, stats.getFailed());
    assertEquals(failing, stats.getFailing());
    assertEquals(instantiated, stats.getInstantiated());
    assertEquals(succeeded, stats.getFinished());
    assertEquals(paused, stats.getPaused());
    assertEquals(running, stats.getRunning());
    assertEquals(stopped, stats.getStopped());
    assertEquals(total, stats.getTotal());

    // TODO: Test the operations
    // Make sure they are as expected
    // for (WorkflowDefinitionReport report : stats.getDefinitions()) {
    //
    // }

  }

  /**
   *
   * @throws Exception
   */
  @Test
  public void testStatisticsMarshalling() throws Exception {
    WorkflowStatistics stats = new WorkflowStatistics();
    stats.setFailed(100);
    stats.setInstantiated(20);

    OperationReport op1 = new OperationReport();
    op1.setId("compose");
    op1.setInstantiated(10);
    op1.setFailing(1);

    List<OperationReport> ops1 = new ArrayList<WorkflowStatistics.WorkflowDefinitionReport.OperationReport>();
    ops1.add(op1);

    WorkflowDefinitionReport def1 = new WorkflowDefinitionReport();
    def1.setFailed(40);
    def1.setInstantiated(10);
    def1.setOperations(ops1);
    def1.setId("def1");
    def1.setOperations(ops1);

    WorkflowDefinitionReport def2 = new WorkflowDefinitionReport();
    def1.setFailed(60);
    def1.setInstantiated(10);

    List<WorkflowDefinitionReport> reports = new ArrayList<WorkflowDefinitionReport>();
    reports.add(def1);
    reports.add(def2);
    stats.setDefinitions(reports);
  }

}
