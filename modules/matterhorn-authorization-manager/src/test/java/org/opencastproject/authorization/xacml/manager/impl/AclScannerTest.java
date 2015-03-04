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
package org.opencastproject.authorization.xacml.manager.impl;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

public class AclScannerTest {

  private AclDb aclDb;
  private OrganizationDirectoryService orgService;
  private AclScanner aclScanner;

  @Before
  public void setUp() throws Exception {
    Organization org1 = new JpaOrganization("org1", "org1", new HashMap<String, Integer>(), "ADMIN", "ANONYMOUS",
            new HashMap<String, String>());
    Organization org2 = new JpaOrganization("org2", "org2", new HashMap<String, Integer>(), "ADMIN", "ANONYMOUS",
            new HashMap<String, String>());
    Organization org3 = new JpaOrganization("org3", "org3", new HashMap<String, Integer>(), "ADMIN", "ANONYMOUS",
            new HashMap<String, String>());

    List<Organization> orgs = new ArrayList<Organization>();
    orgs.add(org1);
    orgs.add(org2);
    orgs.add(org3);

    aclDb = EasyMock.createNiceMock(AclDb.class);

    orgService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgService.getOrganizations()).andReturn(orgs).anyTimes();

    final MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    final HttpMediaPackageElementProvider httpMediaPackageElementProvider = EasyMock
            .createNiceMock(HttpMediaPackageElementProvider.class);

    final AclTransitionDb aclTransitionDb = EasyMock.createNiceMock(AclTransitionDb.class);
    List<EpisodeACLTransition> episodeTransitions = new ArrayList<EpisodeACLTransition>();
    List<SeriesACLTransition> seriesTransitions = new ArrayList<SeriesACLTransition>();
    EasyMock.expect(
            aclTransitionDb.getByQuery(EasyMock.anyObject(Organization.class),
                    EasyMock.anyObject(TransitionQuery.class)))
            .andReturn(new TransitionResultImpl(episodeTransitions, seriesTransitions)).anyTimes();

    // EasyMock.replay(aclDb);
    EasyMock.replay(orgService, messageSender, httpMediaPackageElementProvider, aclTransitionDb);

    AclServiceFactory aclServiceFactory = new AclServiceFactory() {
      @Override
      public AclService serviceFor(Organization org) {
        return new AclServiceImpl(new DefaultOrganization(), aclDb, aclTransitionDb, null, null, null, null, null,
                httpMediaPackageElementProvider, null, null, null, messageSender);
      }
    };

    aclScanner = new AclScanner();
    aclScanner.setAclServiceFactory(aclServiceFactory);
    aclScanner.setOrganizationDirectoryService(orgService);
  }

  @Test
  @Ignore
  public void testCanHandle() {
    File wrongDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(wrongDirectory.getName()).andReturn("wrong").anyTimes();
    EasyMock.replay(wrongDirectory);

    File correctDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(correctDirectory.getName()).andReturn(AclScanner.ACL_DIRECTORY).anyTimes();
    EasyMock.replay(correctDirectory);

    File wrongFilenameWrongDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(wrongFilenameWrongDirectory.getParentFile()).andReturn(wrongDirectory);
    EasyMock.expect(wrongFilenameWrongDirectory.getName()).andReturn("wrong.properties");
    EasyMock.replay(wrongFilenameWrongDirectory);

    File wrongFilenameRightDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(wrongFilenameRightDirectory.getParentFile()).andReturn(correctDirectory);
    EasyMock.expect(wrongFilenameRightDirectory.getName()).andReturn("wrong.properties");
    EasyMock.replay(wrongFilenameRightDirectory);

    File rightFilenameWrongDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(rightFilenameWrongDirectory.getParentFile()).andReturn(wrongDirectory);
    EasyMock.expect(rightFilenameWrongDirectory.getName()).andReturn("right.xml");
    EasyMock.replay(rightFilenameWrongDirectory);

    File rightFilenameRightDirectory = EasyMock.createNiceMock(File.class);
    EasyMock.expect(rightFilenameRightDirectory.getParentFile()).andReturn(correctDirectory).anyTimes();
    EasyMock.expect(rightFilenameRightDirectory.getName()).andReturn("right.xml").anyTimes();
    EasyMock.replay(rightFilenameRightDirectory);

    AclScanner listProvidersScanner = new AclScanner();
    assertFalse(listProvidersScanner.canHandle(wrongFilenameWrongDirectory));
    assertFalse(listProvidersScanner.canHandle(wrongFilenameRightDirectory));
    assertFalse(listProvidersScanner.canHandle(rightFilenameWrongDirectory));
    assertTrue(listProvidersScanner.canHandle(rightFilenameRightDirectory));
  }

  @Test
  public void testCorrectFileInstall() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_correct.xml").getFile());

    ManagedAcl acl = new ManagedAclImpl(1L, "TestAcl", "org", new AccessControlList());
    Option<ManagedAcl> managedAcl = Option.some(acl);
    EasyMock.expect(aclDb.createAcl(anyObject(Organization.class), anyObject(AccessControlList.class), anyString()))
            .andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.getAcls(anyObject(Organization.class))).andReturn(new ArrayList<ManagedAcl>()).times(3);
    EasyMock.replay(aclDb);

    aclScanner.install(file);

    EasyMock.verify(aclDb);
  }

  @Test
  public void testCorruptedFileInstall() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_errors.xml").getFile());

    try {
      aclScanner.install(file);
      fail("Should not be parsed.");
    } catch (JAXBException e) {
      assertTrue("The file can not be parsed.", e instanceof UnmarshalException);
    }
  }

  @Test
  public void testCorrectFileUpdate() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_correct.xml").getFile());

    ManagedAcl acl = new ManagedAclImpl(1L, "TestAcl", "org", new AccessControlList());
    Option<ManagedAcl> managedAcl = Option.some(acl);
    EasyMock.expect(aclDb.createAcl(anyObject(Organization.class), anyObject(AccessControlList.class), anyString()))
            .andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.getAcl(anyObject(Organization.class), anyLong())).andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.updateAcl(anyObject(ManagedAcl.class))).andReturn(true).times(3);
    EasyMock.expect(aclDb.getAcls(anyObject(Organization.class))).andReturn(new ArrayList<ManagedAcl>()).times(3);
    EasyMock.replay(aclDb);

    aclScanner.install(file);

    aclScanner.update(file);

    EasyMock.verify(aclDb);
  }

  @Test
  public void testMissingFileUpdate() throws Exception {
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").getFile());
    File file2 = new File(AclScannerTest.class.getResource("/xacml_correct2.xml").getFile());

    ManagedAcl acl = new ManagedAclImpl(1L, "TestAcl", "org", new AccessControlList());
    Option<ManagedAcl> managedAcl = Option.some(acl);
    EasyMock.expect(aclDb.createAcl(anyObject(Organization.class), anyObject(AccessControlList.class), anyString()))
            .andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.getAcls(anyObject(Organization.class))).andReturn(new ArrayList<ManagedAcl>()).times(3);
    EasyMock.replay(aclDb);

    aclScanner.install(file1);

    aclScanner.update(file2);

    EasyMock.verify(aclDb);
  }

  @Test
  public void testCorruptedFileUpdate() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_errors.xml").getFile());

    try {
      aclScanner.update(file);
      fail("Should not be parsed.");
    } catch (JAXBException e) {
      assertTrue("The file can not be parsed.", e instanceof UnmarshalException);
    }
  }

  @Test
  public void testRemoveFile() throws Exception {
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").getFile());
    Long id = 1L;
    String org = "org";
    ManagedAcl acl = new ManagedAclImpl(id, "TestAcl", org, new AccessControlList());
    Option<ManagedAcl> managedAcl = Option.some(acl);
    EasyMock.expect(aclDb.createAcl(anyObject(Organization.class), anyObject(AccessControlList.class), anyString()))
            .andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.getAcl(EasyMock.anyObject(Organization.class), anyLong())).andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.deleteAcl(anyObject(Organization.class), anyLong())).andReturn(true).times(3);
    EasyMock.expect(aclDb.getAcls(anyObject(Organization.class))).andReturn(new ArrayList<ManagedAcl>()).times(3);
    EasyMock.replay(aclDb);

    aclScanner.install(file1);
    aclScanner.uninstall(file1);

    EasyMock.verify(aclDb);
  }

  @Test
  public void testRemoveMissingFile() throws Exception {
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").getFile());
    File file2 = new File(AclScannerTest.class.getResource("/xacml_correct2.xml").getFile());
    ManagedAcl acl = new ManagedAclImpl(1L, "TestAcl", "org", new AccessControlList());
    Option<ManagedAcl> managedAcl = Option.some(acl);
    EasyMock.expect(aclDb.createAcl(anyObject(Organization.class), anyObject(AccessControlList.class), anyString()))
            .andReturn(managedAcl).times(3);
    EasyMock.expect(aclDb.getAcls(anyObject(Organization.class))).andReturn(new ArrayList<ManagedAcl>()).times(3);
    EasyMock.replay(aclDb);

    aclScanner.install(file1);
    aclScanner.uninstall(file2);

    EasyMock.verify(aclDb);
  }

}
