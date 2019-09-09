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
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowStateListener;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

public class HoldStateTest {
  private static final Logger logger = LoggerFactory.getLogger(HoldStateTest.class);

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private WorkflowServiceSolrIndex dao = null;
  private SecurityService securityService = null;
  private ResumableTestWorkflowOperationHandler holdingOperationHandler;
  private Property property = null;

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
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    MediaPackageMetadataService mds = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

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

    ServiceRegistryInMemoryImpl serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService,
            userDirectoryService, organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    AssetManager assetManager = EasyMock.createNiceMock(AssetManager.class);
    Version version = EasyMock.createNiceMock(Version.class);
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    // Just needs to return a mp, not checking which one
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(mp).anyTimes();
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

    dao = new WorkflowServiceSolrIndex();
    dao.solrRoot = sRoot + File.separator + "solr";
    dao.setServiceRegistry(serviceRegistry);
    dao.setAuthorizationService(authzService);
    dao.setSecurityService(securityService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.setAssetManager(assetManager);
    dao.activate("System Admin");
    service.setDao(dao);
    service.setMessageSender(messageSender);
    service.activate(null);
    service.setServiceRegistry(serviceRegistry);

    serviceRegistry.registerService(service);

    is = HoldStateTest.class.getResourceAsStream("/workflow-definition-holdstate.xml");
    def = WorkflowParser.parseWorkflowDefinition(is);
    IOUtils.closeQuietly(is);
    service.registerWorkflowDefinition(def);
  }

  @After
  public void tearDown() throws Exception {
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {

    EasyMock.expect(property.getId()).andReturn(new PropertyId(mp.getIdentifier().toString(),
            OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE, "testproperty")).anyTimes();
    EasyMock.expect(property.getValue()).andReturn(new Value.StringValue("foo")).once();

    EasyMock.replay(property);

    // Add a listener for paused workflow instances
    WorkflowStateListener pauseListener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(pauseListener);

    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    synchronized (pauseListener) {
      workflow = service.start(def, mp, initialProps);
      pauseListener.wait();
    }
    service.removeWorkflowListener(pauseListener);

    // The variable "testproperty" should have been replaced by "foo", but not "anotherproperty"
    Assert.assertEquals("foo", workflow.getOperations().get(0).getConfiguration("testkey"));
    Assert.assertEquals("${anotherproperty}", workflow.getOperations().get(1).getConfiguration("testkey"));

    // Simulate a user resuming and submitting new properties (this time, with a value for "anotherproperty") to the
    // workflow
    Map<String, String> resumeProps = new HashMap<String, String>();
    resumeProps.put("anotherproperty", "bar");

    WorkflowStateListener succeedListener = new WorkflowStateListener(WorkflowState.SUCCEEDED);
    service.addWorkflowListener(succeedListener);
    synchronized (succeedListener) {
      service.resume(workflow.getId(), resumeProps);
      succeedListener.wait();
    }
    service.removeWorkflowListener(succeedListener);

    Assert.assertEquals("Workflow expected to succeed", 1, succeedListener.countStateChanges(WorkflowState.SUCCEEDED));

    WorkflowInstance fromDb = service.getWorkflowById(workflow.getId());
    logger.info("checking for the existence of 'anotherproperty', which should have been replaced");
    Assert.assertEquals("foo", fromDb.getOperations().get(0).getConfiguration("testkey"));
    Assert.assertEquals("bar", fromDb.getOperations().get(1).getConfiguration("testkey"));
  }

  @Test
  public void testMultipleHolds() throws Exception {

    EasyMock.expect(property.getId()).andReturn(new PropertyId(mp.getIdentifier().toString(),
            OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE, "testproperty")).anyTimes();
    EasyMock.expect(property.getValue()).andReturn(new Value.StringValue("foo")).anyTimes();

    EasyMock.replay(property);

    WorkflowStateListener pauseListener = new WorkflowStateListener(WorkflowState.PAUSED);
    service.addWorkflowListener(pauseListener);
    synchronized (pauseListener) {
      workflow = service.start(def, mp);
      pauseListener.wait();
    }

    // Simulate a user resuming the workflow, but the handler still keeps the workflow in a hold state
    holdingOperationHandler.setResumeAction(Action.PAUSE);

    // Resume the workflow again. It should quickly reenter the paused state
    synchronized (pauseListener) {
      service.resume(workflow.getId());
      pauseListener.wait();
    }

    // remove the pause listener
    service.removeWorkflowListener(pauseListener);

    WorkflowInstance fromDb = service.getWorkflowById(workflow.getId());
    Assert.assertEquals(WorkflowState.PAUSED, fromDb.getState());

    // Resume the workflow again, and this time continue with the workflow
    holdingOperationHandler.setResumeAction(Action.CONTINUE);

    WorkflowStateListener succeedListener = new WorkflowStateListener(WorkflowState.SUCCEEDED, WorkflowState.FAILED);
    service.addWorkflowListener(succeedListener);
    synchronized (succeedListener) {
      service.resume(workflow.getId());
      succeedListener.wait();
    }
    service.removeWorkflowListener(succeedListener);

    Assert.assertEquals(WorkflowState.SUCCEEDED, service.getWorkflowById(workflow.getId()).getState());
  }

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
