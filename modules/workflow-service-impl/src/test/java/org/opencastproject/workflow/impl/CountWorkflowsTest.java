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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.elasticsearch.api.SearchResult;
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
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowServiceDatabaseImpl;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
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
import java.util.Set;

public class CountWorkflowsTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowDefinition def = null;
  private MediaPackage mp = null;
  private ResumableWorkflowOperationHandler holdingOperationHandler;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;
  private SecurityService securityService = null;
  private Workspace workspace = null;

  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    InputStream is = CountWorkflowsTest.class.getResourceAsStream("/mediapackage-1.xml");
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

    workspace = createNiceMock(Workspace.class);
    expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.expect(workspace.read(anyObject()))
            .andAnswer(() -> getClass().getResourceAsStream("/dc-1.xml")).anyTimes();
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    {
      final AssetManager assetManager = createNiceMock(AssetManager.class);
      EasyMock.expect(assetManager.selectProperties(EasyMock.anyString(), EasyMock.anyString()))
              .andReturn(Collections.emptyList())
              .anyTimes();
      EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString())).andReturn(Opt.none()).anyTimes();
      EasyMock.expect(assetManager.snapshotExists(EasyMock.anyString())).andReturn(true).anyTimes();
      EasyMock.replay(assetManager);
      service.setAssetManager(assetManager);
    }

    WorkflowServiceDatabaseImpl workflowDb = new WorkflowServiceDatabaseImpl();
    workflowDb.setEntityManagerFactory(newEntityManagerFactory(WorkflowServiceDatabaseImpl.PERSISTENCE_UNIT));
    workflowDb.setDBSessionFactory(getDbSessionFactory());
    workflowDb.setSecurityService(securityService);
    workflowDb.activate(null);
    service.setPersistence(workflowDb);

    service.activate(null);

    service.setServiceRegistry(serviceRegistry);

    is = CountWorkflowsTest.class.getResourceAsStream("/workflow-definition-holdstate.xml");
    def = XmlWorkflowParser.parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);

    serviceRegistry.registerService(service);

    SearchResult result = EasyMock.createNiceMock(SearchResult.class);

    final ElasticsearchIndex index = EasyMock.createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(index.getIndexName()).andReturn("index").anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result).anyTimes();
    EasyMock.replay(result, index);

    service.setIndex(index);
  }

  @Test
  public void testHoldAndResume() throws Exception {
    // Wait for all workflows to be in paused state
    WorkflowStateListener listener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(listener);

    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    WorkflowInstance workflow1 = null;
    workflow1 = service.start(def, mp, initialProps);
    WorkflowTestSupport.poll(listener, 1);
    mp.setIdentifier(IdImpl.fromUUID());
    service.start(def, mp, initialProps);
    WorkflowTestSupport.poll(listener, 2);
    service.removeWorkflowListener(listener);

    // Test for two paused workflows in "op1"
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(2, service.countWorkflowInstances(WorkflowState.PAUSED));
    assertEquals(0, service.countWorkflowInstances(WorkflowState.SUCCEEDED));

    // Continue one of the two workflows, waiting for success
    listener = new WorkflowStateListener(WorkflowState.SUCCEEDED);
    service.addWorkflowListener(listener);
    service.resume(workflow1.getId());
    WorkflowTestSupport.poll(listener, 1);
    service.removeWorkflowListener(listener);

    // Make sure one workflow is still on hold, the other is finished.
    assertEquals(2, service.countWorkflowInstances());
    assertEquals(1, service.countWorkflowInstances(WorkflowState.PAUSED));
    assertEquals(1, service.countWorkflowInstances(WorkflowState.SUCCEEDED));
  }

  /**
   * Test implementatio for a workflow operation handler that will go on hold, and continue when resume() is called.
   */
  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) {
      return createResult(Action.CONTINUE);
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
