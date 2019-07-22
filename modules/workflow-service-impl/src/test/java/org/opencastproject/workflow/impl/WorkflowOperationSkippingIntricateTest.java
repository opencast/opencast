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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
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
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class WorkflowOperationSkippingIntricateTest {

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition workingDefinition = null;
  private MediaPackage mediapackage1 = null;
  private WorkflowServiceSolrIndex dao = null;
  private Set<HandlerRegistration> handlerRegistrations = null;
  private Property property = null;

  private AccessControlList acl = new AccessControlList();

  private static String getStorageRoot() {
    return "." + File.separator + "target" + File.separator + System.currentTimeMillis();
  }

  @Before
  public void setUp() throws Exception {
    File sRoot = new File(getStorageRoot());
    try {
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    // create operation handlers for our workflows
    SucceedingWorkflowOperationHandler succeedingOperationHandler = new SucceedingWorkflowOperationHandler(
            mediapackage1);
    handlerRegistrations = new HashSet<>();
    handlerRegistrations.add(new HandlerRegistration("op1", succeedingOperationHandler));
    handlerRegistrations.add(new HandlerRegistration("op2", succeedingOperationHandler));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };
    WorkflowDefinitionScanner scanner = new WorkflowDefinitionScanner();
    service.addWorkflowDefinitionScanner(scanner);
    MediaPackageMetadataService mds = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents(EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    // security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(SecurityServiceStub.DEFAULT_ORG_ADMIN).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    service.setSecurityService(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject())).andReturn(DEFAULT_ORG_ADMIN)
            .anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    Organization organization = new DefaultOrganization();
    List<Organization> organizationList = new ArrayList<>();
    organizationList.add(organization);
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    ServiceRegistryInMemoryImpl serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService,
            userDirectoryService, organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    dao = new WorkflowServiceSolrIndex();
    dao.solrRoot = sRoot + File.separator + "solr." + System.currentTimeMillis();

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getActiveAcl(EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);

    {
      final AssetManager assetManager = createNiceMock(AssetManager.class);
      property = EasyMock.createMock(Property.class);
      EasyMock.expect(assetManager.selectProperties(EasyMock.anyString(), EasyMock.anyString()))
              .andReturn(Collections.singletonList(property))
              .anyTimes();
      EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString())).andReturn(Opt.none()).anyTimes();
      EasyMock.expect(assetManager.snapshotExists(EasyMock.anyString())).andReturn(true).anyTimes();
      EasyMock.replay(assetManager);
      service.setAssetManager(assetManager);
    }

    AssetManager assetManager = EasyMock.createNiceMock(AssetManager.class);
    Version version = EasyMock.createNiceMock(Version.class);
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    // Just needs to return a mp, not checking which one
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(mediapackage1).anyTimes();
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(securityService.getOrganization().getId()).anyTimes();
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
    EasyMock.replay(assetManager, version, snapshot, p, r, t, selectQuery, query, v, aRec);

    dao.setServiceRegistry(serviceRegistry);
    dao.setSecurityService(securityService);
    dao.setAuthorizationService(authzService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.setAssetManager(assetManager);
    dao.activate("System Admin");
    service.setDao(dao);
    service.setServiceRegistry(serviceRegistry);
    service.setMessageSender(messageSender);
    service.setUserDirectoryService(userDirectoryService);
    service.activate(null);

    try (InputStream is = getClass().getResourceAsStream("/workflow-definition-skipping-intricate.xml")) {
      workingDefinition = WorkflowParser.parseWorkflowDefinition(is);

      MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
      try (InputStream is2 = WorkflowOperationSkippingIntricateTest.class.getResourceAsStream("/mediapackage-1.xml")) {
        mediapackage1 = mediaPackageBuilder.loadFromXml(is2);
        Assert.assertNotNull(mediapackage1.getIdentifier());
      } catch (Exception e) {
        e.printStackTrace();
        Assert.fail(e.getMessage());
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

  }

  @After
  public void tearDown() {
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testIfSuccessful() throws Exception {

    EasyMock.expect(property.getId()).andReturn(new PropertyId(mediapackage1.getIdentifier().toString(),
            OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE, "executecondition")).anyTimes();
    EasyMock.expect(property.getValue()).andReturn(new Value.StringValue("a")).once();

    EasyMock.replay(property);

    Map<String, String> properties1 = new HashMap<>();
    properties1.put("executecondition", "a");

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, properties1);

    // See if the skip operation has been executed
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    assertNotNull(instanceFromDb);
    assertEquals(OperationState.SUCCEEDED, instanceFromDb.getOperations().get(0).getState());
  }

  @Test
  public void testIfFailure() throws Exception {

    EasyMock.expect(property.getId()).andReturn(new PropertyId(mediapackage1.getIdentifier().toString(),
            OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE, "executecondition")).anyTimes();
    EasyMock.expect(property.getValue()).andReturn(new Value.StringValue("b")).once();

    EasyMock.replay(property);

    Map<String, String> properties1 = new HashMap<>();
    properties1.put("executecondition", "b");

    WorkflowInstance instance = startAndWait(workingDefinition, mediapackage1, properties1);

    // See if the skip operation has been executed
    WorkflowInstance instanceFromDb = service.getWorkflowById(instance.getId());
    assertNotNull(instanceFromDb);
    assertEquals(OperationState.SKIPPED, instanceFromDb.getOperations().get(0).getState());
  }

  private WorkflowInstance startAndWait(WorkflowDefinition definition, MediaPackage mp, Map<String, String> properties) throws Exception {

    WorkflowStateListener stateListener = new WorkflowStateListener(WorkflowState.SUCCEEDED);
    service.addWorkflowListener(stateListener);
    WorkflowInstance instance;
    synchronized (stateListener) {
      instance = service.start(definition, mp, properties);
      stateListener.wait();
    }
    service.removeWorkflowListener(stateListener);

    return instance;
  }

  class SucceedingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    private MediaPackage mp;

    SucceedingWorkflowOperationHandler(MediaPackage mp) {
      this.mp = mp;
    }

    @Override
    public String getId() {
      return this.getClass().getName();
    }

    @Override
    public String getDescription() {
      return "ContinuingWorkflowOperationHandler";
    }

    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) {
      return createResult(mp, Action.CONTINUE);
    }
  }

}
