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

package org.opencastproject.authorization.xacml.manager.impl;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.authorization.xacml.XACMLParsingException;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    List<Organization> orgs = new ArrayList<>();
    orgs.add(org1);
    orgs.add(org2);
    orgs.add(org3);

    aclDb = EasyMock.createNiceMock(AclDb.class);

    orgService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgService.getOrganizations()).andReturn(orgs).anyTimes();

    User user = EasyMock.createNiceMock(User.class);
    EasyMock.expect(user.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();

    final SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    SearchResultImpl<Event> eventSearchResult = EasyMock.createNiceMock(SearchResultImpl.class);
    EasyMock.expect(eventSearchResult.getItems()).andReturn(new SearchResultItem[] {}).anyTimes();

    SearchResultImpl<Series> seriesSearchResult = EasyMock.createNiceMock(SearchResultImpl.class);
    EasyMock.expect(seriesSearchResult.getItems()).andReturn(new SearchResultItem[] {}).anyTimes();

    final AbstractSearchIndex index = EasyMock.createNiceMock(AbstractSearchIndex.class);
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(eventSearchResult)
            .anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class))).andReturn(seriesSearchResult)
            .anyTimes();

    EasyMock.replay(orgService, securityService, index, user, eventSearchResult, seriesSearchResult);

    AclServiceFactory aclServiceFactory = new AclServiceFactory() {
      @Override
      public AclService serviceFor(Organization org) {
        return new AclServiceImpl(new DefaultOrganization(), aclDb, null, null, null,
                index, index, securityService);
      }
    };

    aclScanner = new AclScanner();
    aclScanner.setAclServiceFactory(aclServiceFactory);
    aclScanner.setOrganizationDirectoryService(orgService);
    aclScanner.setSecurityService(securityService);
  }

  @Test
  public void testCorrectFileInstall() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_correct.xml").toURI());

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
    File file = new File(AclScannerTest.class.getResource("/xacml_errors.xml").toURI());

    try {
      aclScanner.install(file);
      fail("Should not be parsed.");
    } catch (XACMLParsingException e) {
      assertTrue("The file can not be parsed.", e.getCause() instanceof SAXParseException);
    }
  }

  @Test
  public void testCorrectFileUpdate() throws Exception {
    File file = new File(AclScannerTest.class.getResource("/xacml_correct.xml").toURI());

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
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").toURI());
    File file2 = new File(AclScannerTest.class.getResource("/xacml_correct2.xml").toURI());

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
    File file = new File(AclScannerTest.class.getResource("/xacml_errors.xml").toURI());

    try {
      aclScanner.update(file);
      fail("Should not be parsed.");
    } catch (XACMLParsingException e) {
      assertTrue("The file can not be parsed.", e.getCause() instanceof SAXParseException);
    }
  }

  @Test
  public void testRemoveFile() throws Exception {
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").toURI());
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
    File file1 = new File(AclScannerTest.class.getResource("/xacml_correct.xml").toURI());
    File file2 = new File(AclScannerTest.class.getResource("/xacml_correct2.xml").toURI());
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
