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
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
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
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowServiceDatabaseImpl;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PauseWorkflowTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private Workspace workspace = null;
  private SecurityService securityService = null;
  private ResumableTestWorkflowOperationHandler firstHandler = null;
  private ResumableTestWorkflowOperationHandler secondHandler = null;

  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    InputStream is = PauseWorkflowTest.class.getResourceAsStream("/mediapackage-1.xml");
    mp = mediaPackageBuilder.loadFromXml(is);
    IOUtils.closeQuietly(is);

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    firstHandler = new ResumableTestWorkflowOperationHandler();
    secondHandler = new ResumableTestWorkflowOperationHandler();
    handlerRegistrations.add(new HandlerRegistration("op1", firstHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", secondHandler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    scanner = new WorkflowDefinitionScanner();
    service.addWorkflowDefinitionScanner(scanner);

    final AssetManager assetManager = createNiceMock(AssetManager.class);
    EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString())).andReturn(Opt.none()).anyTimes();
    EasyMock.replay(assetManager);
    service.setAssetManager(assetManager);

    // security service
    DefaultOrganization organization = new DefaultOrganization();
    securityService = createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(SecurityServiceStub.DEFAULT_ORG_ADMIN).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    service.setSecurityService(securityService);

    AuthorizationService authzService = createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(DEFAULT_ORG_ADMIN)
            .anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    MediaPackageMetadataService mds = createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    List<Organization> organizationList = new ArrayList<Organization>();
    organizationList.add(organization);
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(securityService.getOrganization()).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    ServiceRegistryInMemoryImpl serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService,
            userDirectoryService, organizationDirectoryService, createNiceMock(IncidentService.class));

    workspace = createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.expect(workspace.read(anyObject()))
            .andAnswer(() -> getClass().getResourceAsStream("/dc-1.xml")).anyTimes();
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);

    WorkflowServiceDatabaseImpl workflowDb = new WorkflowServiceDatabaseImpl();
    workflowDb.setEntityManagerFactory(newTestEntityManagerFactory(WorkflowServiceDatabaseImpl.PERSISTENCE_UNIT));
    workflowDb.setSecurityService(securityService);
    workflowDb.activate(null);
    service.setPersistence(workflowDb);

    service.setServiceRegistry(serviceRegistry);
    service.activate(null);

    is = PauseWorkflowTest.class.getResourceAsStream("/workflow-definition-pause.xml");
    def = XmlWorkflowParser.parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);

    SearchResult result = createNiceMock(SearchResult.class);

    final ElasticsearchIndex index = createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(index.getIndexName()).andReturn("index").anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result).anyTimes();
    EasyMock.replay(result, index);

    service.setIndex(index);
  }

  @Test
  public void testHoldAndResume() throws Exception {

    // Start a new workflow
    WorkflowStateListener pauseListener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(pauseListener);
    workflow = service.start(def, mp, null);
    Thread.sleep(1000);

    // Ensure that the first operation handler was called, but not the second
    Assert.assertTrue(firstHandler.isStarted());
    Assert.assertTrue(!secondHandler.isStarted());

    // The workflow should be in the paused state
    Assert.assertEquals(WorkflowState.PAUSED, service.getWorkflowById(workflow.getId()).getState());

    // Resume the workflow with some properties
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("key", "value");

    service.addWorkflowListener(pauseListener);
    service.resume(workflow.getId(), properties);
    Thread.sleep(1000);
    service.removeWorkflowListener(pauseListener);

    // The handler should have picked up the properties
    Assert.assertEquals(properties, firstHandler.getProperties());
  }

}
