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

import org.opencastproject.job.api.JobContext;
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
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CountWorkflowsTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowDefinition def = null;
  private WorkflowServiceSolrIndex dao = null;
  private MediaPackage mp = null;
  private ResumableWorkflowOperationHandler holdingOperationHandler;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;
  private SecurityService securityService = null;

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
      FileUtils.deleteDirectory(sRoot);
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    InputStream is = HoldStateTest.class.getResourceAsStream("/mediapackage-1.xml");
    mp = mediaPackageBuilder.loadFromXml(is);
    IOUtils.closeQuietly(is);

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    holdingOperationHandler = new ResumableTestWorkflowOperationHandler();
    handlerRegistrations.add(new HandlerRegistration("op1", holdingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", new ContinuingWorkflowOperationHandler()));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
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
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    MediaPackageMetadataService mds = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    dao = new WorkflowServiceSolrIndex();
    dao.setServiceRegistry(serviceRegistry);
    dao.solrRoot = sRoot + File.separator + "solr";
    dao.setAuthorizationService(authzService);
    dao.setSecurityService(securityService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.activate("System Admin");
    service.setDao(dao);
    service.setMessageSender(messageSender);
    service.activate(null);

    service.setServiceRegistry(serviceRegistry);

    is = CountWorkflowsTest.class.getResourceAsStream("/workflow-definition-holdstate.xml");
    def = WorkflowParser.parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);
    service.registerWorkflowDefinition(def);

    serviceRegistry.registerService(service);

  }

  @After
  public void tearDown() throws Exception {
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    // Wait for all workflows to be in paused state
    WorkflowStateListener listener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(listener);

    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    WorkflowInstance workflow1 = null;
    synchronized (listener) {
      workflow1 = service.start(def, mp, initialProps);
      listener.wait();
      mp.setIdentifier(new UUIDIdBuilderImpl().createNew());
      service.start(def, mp, initialProps);
      listener.wait();
    }
    service.removeWorkflowListener(listener);

    // Test for two paused workflows in "op1"
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(2, service.countWorkflowInstances(null, "op1"));
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
    assertEquals(0, service.countWorkflowInstances(null, "op2"));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED, "op1"));

    // Continue one of the two workflows, waiting for success
    listener = new WorkflowStateListener(WorkflowState.SUCCEEDED);
    service.addWorkflowListener(listener);
    synchronized (listener) {
      service.resume(workflow1.getId());
      listener.wait();
    }
    service.removeWorkflowListener(listener);

    // Make sure one workflow is still on hold, the other is finished.
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, null));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED, "op1"));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.SUCCEEDED, null));
  }

  /**
   * Test implementatio for a workflow operation handler that will go on hold, and continue when resume() is called.
   */
  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
            throws WorkflowOperationException {
      return createResult(Action.CONTINUE);
    }

    @Override
    public SortedMap<String, String> getConfigurationOptions() {
      return new TreeMap<String, String>();
    }

    @Override
    public String getId() {
      return this.getClass().getName();
    }

    @Override
    public String getDescription() {
      return "ContinuingWorkflowOperationHandler";
    }
  }

}
