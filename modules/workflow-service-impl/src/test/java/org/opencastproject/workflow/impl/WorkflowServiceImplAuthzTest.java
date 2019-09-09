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
import static org.junit.Assert.fail;
import static org.opencastproject.workflow.impl.SecurityServiceStub.DEFAULT_ORG_ADMIN;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowServiceImplAuthzTest {

  private Organization defaultOrganization = null;
  private Organization otherOrganization = null;

  private User instructor1 = null;
  private User instructor2 = null;
  private User instructorFromDifferentOrg = null;
  private User globalAdmin = null;

  protected Map<String, User> users = null;
  private Responder<User> userResponder;
  private Responder<Organization> organizationResponder;

  private WorkflowServiceImpl service = null;
  private WorkflowDefinitionScanner scanner = null;
  private WorkflowServiceSolrIndex dao = null;
  private Workspace workspace = null;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;
  private SecurityService securityService = null;

  private File sRoot = null;

  protected static final String getStorageRoot() {
    return "." + File.separator + "target" + File.separator + System.currentTimeMillis();
  }

  private static class Responder<A> implements IAnswer<A> {
    private A response;

    Responder(A response) {
      this.response = response;
    }

    public void setResponse(A response) {
      this.response = response;
    }

    @Override
    public A answer() throws Throwable {
      return response;
    }
  }

  @Before
  public void setUp() throws Exception {
    Map<String, Integer> servers = new HashMap<String, Integer>();
    servers.put("http://somewhere", 80);
    defaultOrganization = new DefaultOrganization();
    otherOrganization = new JaxbOrganization("other_org", "Another organization", servers,
            defaultOrganization.getAdminRole(), defaultOrganization.getAnonymousRole(), null);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(defaultOrganization);

    instructor1 = new JaxbUser("instructor1", "test", jaxbOrganization, new JaxbRole("ROLE_INSTRUCTOR",
            jaxbOrganization));
    instructor2 = new JaxbUser("instructor2", "test", jaxbOrganization, new JaxbRole("ROLE_INSTRUCTOR",
            jaxbOrganization));

    JaxbOrganization differentOrg = new JaxbOrganization("differentorg");
    instructorFromDifferentOrg = new JaxbUser("instructor3", "test", differentOrg, new JaxbRole("ROLE_INSTRUCTOR",
            differentOrg));

    JaxbOrganization doesntMatterOrg = new JaxbOrganization("org doesn't matter");
    globalAdmin = new JaxbUser("global_admin", "test", doesntMatterOrg, new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, doesntMatterOrg));

    users = new HashMap<String, User>();
    users.put(instructor1.getUsername(), instructor1);
    users.put(instructor2.getUsername(), instructor2);
    users.put(instructorFromDifferentOrg.getUsername(), instructorFromDifferentOrg);
    users.put(DEFAULT_ORG_ADMIN.getUsername(), DEFAULT_ORG_ADMIN);
    users.put(globalAdmin.getUsername(), globalAdmin);

    service = new WorkflowServiceImpl() {
      @Override
      public Set<HandlerRegistration> getRegisteredHandlers() {
        return new HashSet<WorkflowServiceImpl.HandlerRegistration>();
      }
    };

    scanner = new WorkflowDefinitionScanner();
    service.addWorkflowDefinitionScanner(scanner);

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

    // Organization Service
    List<Organization> organizationList = new ArrayList<Organization>();
    organizationList.add(defaultOrganization);
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andAnswer(new IAnswer<Organization>() {
              @Override
              public Organization answer() throws Throwable {
                String orgId = (String) EasyMock.getCurrentArguments()[0];
                Map<String, Integer> servers = new HashMap<String, Integer>();
                servers.put("http://" + orgId, 80);
                defaultOrganization = new DefaultOrganization();
                return new JaxbOrganization(orgId, orgId, servers, "ROLE_ADMIN", "ROLE_ANONYMOUS", null);
              }
            }).anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganizations()).andReturn(organizationList).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    // Metadata Service
    MediaPackageMetadataService mds = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.replay(mds);
    service.addMetadataService(mds);

    // Workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getCollectionContents((String) EasyMock.anyObject())).andReturn(new URI[0]);
    EasyMock.replay(workspace);

    // User Directory
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andAnswer(new IAnswer<User>() {
      @Override
      public User answer() throws Throwable {
        String userName = (String) EasyMock.getCurrentArguments()[0];
        return users.get(userName);
      }
    }).anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    // security service
    userResponder = new Responder<User>(DEFAULT_ORG_ADMIN);
    organizationResponder = new Responder<Organization>(defaultOrganization);
    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);

    service.setSecurityService(securityService);

    // Authorization Service
    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    // Service Registry
    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    service.setServiceRegistry(serviceRegistry);

    // Search Index
    sRoot = new File(getStorageRoot());
    FileUtils.forceMkdir(sRoot);
    dao = new WorkflowServiceSolrIndex();
    dao.setServiceRegistry(serviceRegistry);
    dao.setAuthorizationService(authzService);
    dao.setSecurityService(securityService);
    dao.setOrgDirectory(organizationDirectoryService);
    dao.solrRoot = sRoot + File.separator + "solr." + System.currentTimeMillis();
    dao.activate("System Admin");
    service.setDao(dao);
    service.setMessageSender(messageSender);

    // Activate
    service.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.deactivate();
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testWorkflowWithSecurityPolicy() throws Exception {

    // Create an ACL for the authorization service to return
    AccessControlList acl = new AccessControlList();
    acl.getEntries().add(new AccessControlEntry("ROLE_INSTRUCTOR", Permissions.Action.READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry("ROLE_INSTRUCTOR", Permissions.Action.WRITE.toString(), true));

    // Mock up an authorization service that always returns "true" for hasPermission()
    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    EasyMock.expect(authzService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);
    dao.setAuthorizationService(authzService);

    // Create the workflow and its dependent object graph
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.add(new WorkflowOperationDefinitionImpl("op1", "op1", null, true));
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    // As an instructor, create a workflow. We don't care if it passes or fails. We just care about access to it.
    userResponder.setResponse(instructor1);
    WorkflowInstance workflow = service.start(def, mp);
    service.suspend(workflow.getId());

    // Ensure that this instructor can access the workflow
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the organization admin can access that workflow
    userResponder.setResponse(DEFAULT_ORG_ADMIN);
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the global admin can access that workflow
    userResponder.setResponse(globalAdmin);
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the other instructor from this organization can also see the workflow, since this is specified in the
    // security policy
    userResponder.setResponse(instructor2);
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // TODO change to answer show in episode or series how to do it. Cool stuff

    // Ensure the instructor from a different org can not see the workflow, even though they share the same role
    organizationResponder.setResponse(otherOrganization);
    userResponder.setResponse(instructorFromDifferentOrg);
    try {
      service.getWorkflowById(workflow.getId());
      fail();
    } catch (Exception e) {
      // expected
    }
    assertEquals(0, service.countWorkflowInstances());
  }

  @Test
  public void testWorkflowWithoutSecurityPolicy() throws Exception {
    // Mock up an authorization service that always returns "false" for hasPermission()
    AuthorizationService authzService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authzService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(new AccessControlList(), AclScope.Series)).anyTimes();
    EasyMock.expect(authzService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(false).anyTimes();
    EasyMock.replay(authzService);
    service.setAuthorizationService(authzService);
    dao.setAuthorizationService(authzService);

    // Create the workflow and its dependent object graph
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.add(new WorkflowOperationDefinitionImpl("op1", "op1", null, true));
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    // As an instructor, create a workflow
    userResponder.setResponse(instructor1);
    WorkflowInstance workflow = service.start(def, mp);
    service.suspend(workflow.getId());

    // Ensure that this instructor can access the workflow
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the organization admin can access that workflow
    userResponder.setResponse(DEFAULT_ORG_ADMIN);
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the global admin can access that workflow
    userResponder.setResponse(globalAdmin);
    try {
      service.getWorkflowById(workflow.getId());
      assertEquals(1, service.countWorkflowInstances());
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Ensure the other instructor can not see the workflow, since there is no security policy granting access
    userResponder.setResponse(instructor2);
    try {
      service.getWorkflowById(workflow.getId());
      fail();
    } catch (UnauthorizedException e) {
      // expected
    }
    assertEquals(0, service.countWorkflowInstances());

    // Ensure the instructor from a different org can not see the workflow, even though they share a role
    organizationResponder.setResponse(otherOrganization);
    userResponder.setResponse(instructorFromDifferentOrg);
    try {
      service.getWorkflowById(workflow.getId());
      fail();
    } catch (Exception e) {
      // expected
    }
    assertEquals(0, service.countWorkflowInstances());
  }

}
