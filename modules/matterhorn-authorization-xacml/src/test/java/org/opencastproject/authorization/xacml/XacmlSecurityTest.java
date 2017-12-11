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

package org.opencastproject.authorization.xacml;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.schlichtherle.io.FileOutputStream;

/**
 * Tests XACML features of the security service
 */
public class XacmlSecurityTest {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(XacmlSecurityTest.class);

  /** The username to use with the security service */
  protected final String currentUser = "me";

  /** The organization to use */
  protected final JaxbOrganization organization = new DefaultOrganization();

  /** The roles to use with the security service */
  protected final Set<JaxbRole> currentRoles = new HashSet<>();

  // Override the behavior of the security service to use the current user and roles defined here
  protected SecurityService securityService = null;

  protected XACMLAuthorizationService authzService = null;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    authzService = new XACMLAuthorizationService();

    // Mock security service
    securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(
            () -> new JaxbUser(currentUser, "test", organization, currentRoles)).anyTimes();

    // Mock workspace
    Workspace workspace = EasyMock.createMock(Workspace.class);
    final Capture<InputStream> in = EasyMock.newCapture();
    final Capture<URI> uri = EasyMock.newCapture();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.capture(in))).andAnswer(() -> {
        final File file = testFolder.newFile();
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copyLarge(in.getValue(), out);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(in.getValue());
        return file.toURI();
      }).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.capture(uri))).andAnswer(() -> new File(uri.getValue())).anyTimes();
    workspace.delete(EasyMock.anyObject(URI.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(securityService, workspace);
    authzService.setWorkspace(workspace);
    authzService.setSecurityService(securityService);
  }

  @Test
  public void testSecurity() throws Exception {

    // Create a mediapackage and some role/action tuples
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    // Get default ACL
    AccessControlList defaultAcl = authzService.getActiveAcl(mediapackage).getA();
    Assert.assertEquals(0, defaultAcl.getEntries().size());

    // Default with series
    mediapackage.setSeries("123");
    defaultAcl = authzService.getActiveAcl(mediapackage).getA();
    Assert.assertEquals(0, defaultAcl.getEntries().size());

    AccessControlList aclSeries1 = new AccessControlList();
    List<AccessControlEntry> entriesSeries1 = aclSeries1.getEntries();
    entriesSeries1.add(new AccessControlEntry("admin", "delete", true));
    entriesSeries1.add(new AccessControlEntry("admin", "read", true));

    entriesSeries1.add(new AccessControlEntry("student", "read", true));
    entriesSeries1.add(new AccessControlEntry("student", "comment", true));

    entriesSeries1.add(new AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "read", true));
    entriesSeries1.add(new AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "comment", false));

    AccessControlList aclSeries2 = new AccessControlList();
    List<AccessControlEntry> entriesSeries2 = aclSeries2.getEntries();
    entriesSeries2.add(new AccessControlEntry("admin", "delete", true));
    entriesSeries2.add(new AccessControlEntry("admin", "read", true));

    entriesSeries2.add(new AccessControlEntry("student", "read", false));
    entriesSeries2.add(new AccessControlEntry("student", "comment", false));

    entriesSeries2.add(new AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "read", true));
    entriesSeries2.add(new AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "comment", false));

    AccessControlList aclEpisode = new AccessControlList();

    // Add the security policy to the mediapackage
    authzService.setAcl(mediapackage, AclScope.Series, aclSeries1);

    // Ensure that the permissions specified are respected by the security service
    currentRoles.clear();
    currentRoles.add(new JaxbRole("admin", organization, ""));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));
    currentRoles.clear();
    currentRoles.add(new JaxbRole("student", organization, ""));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "comment"));
    currentRoles.clear();
    currentRoles.add(new JaxbRole("admin", organization));

    mediapackage = authzService.setAcl(mediapackage, AclScope.Episode, aclEpisode).getA();
    Assert.assertEquals(AclScope.Episode, authzService.getActiveAcl(mediapackage).getB());
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));

    mediapackage = authzService.removeAcl(mediapackage, AclScope.Episode);

    AccessControlList computedAcl = authzService.getActiveAcl(mediapackage).getA();
    Assert.assertEquals("ACLs are the same size?", entriesSeries1.size(), computedAcl.getEntries().size());
    Assert.assertTrue("ACLs contain the same ACEs?", computedAcl.getEntries().containsAll(entriesSeries1));

    authzService.setAcl(mediapackage, AclScope.Series, aclSeries2);

    currentRoles.clear();
    currentRoles.add(new JaxbRole("student", organization));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));

    currentRoles.clear();
    currentRoles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, organization, ""));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));
  }
}
