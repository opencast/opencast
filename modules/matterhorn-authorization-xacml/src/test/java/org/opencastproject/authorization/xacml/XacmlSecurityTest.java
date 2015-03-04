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
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import de.schlichtherle.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests XACML features of the security service
 */
public class XacmlSecurityTest {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(XacmlSecurityTest.class);

  /** The stub workspace to store xacml files */
  protected WorkspaceStub workspace = null;

  /** The username to use with the security service */
  protected final String currentUser = "me";

  /** The organization to use */
  protected final JaxbOrganization organization = new DefaultOrganization();

  /** The roles to use with the security service */
  protected final Set<JaxbRole> currentRoles = new HashSet<JaxbRole>();

  // Override the behavior of the security service to use the current user and roles defined here
  protected SecurityService securityService = null;

  protected XACMLAuthorizationService authzService = null;

  @Before
  public void setUp() throws Exception {
    workspace = new WorkspaceStub();
    securityService = new SecurityService() {
      @Override
      public User getUser() {
        return new JaxbUser(currentUser, "test", organization, currentRoles);
      }

      @Override
      public void setUser(User user) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Organization getOrganization() {
        return organization;
      }

      @Override
      public void setOrganization(Organization organization) {
        throw new UnsupportedOperationException();
      }
    };
    authzService = new XACMLAuthorizationService();
    authzService.setWorkspace(new WorkspaceStub());
    authzService.setSecurityService(securityService);
  }

  @After
  public void tearDown() throws Exception {
    // workspace.file.delete();
  }

  @Test
  public void testSecurity() throws Exception {

    // Create a mediapackage and some role/action tuples
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

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

    authzService.setAcl(mediapackage, AclScope.Episode, aclEpisode);
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

  static class WorkspaceStub implements Workspace {

    /** The default workspace base, this is set to the target directory within the module. */
    private static File workspaceBase = new File("target");

    @Override
    public File get(URI uri) throws NotFoundException, IOException {
      return new File(uri);
    }

    @Override
    public URI getBaseUri() {
      throw new Error();
    }

    @Override
    public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
            throws IOException {
      final File file = new File(getURI(mediaPackageID, mediaPackageElementID, fileName));
      FileOutputStream out = new FileOutputStream(file);
      IOUtils.copyLarge(in, out);
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
      return file.toURI();
    }

    @Override
    public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
      return null;
    }

    @Override
    public URI[] getCollectionContents(String collectionId) throws NotFoundException {
      throw new Error();
    }

    @Override
    public void delete(URI uri) throws NotFoundException, IOException {
      new File(uri).delete();
    }

    @Override
    public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
      throw new Error();
    }

    @Override
    public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
      throw new Error();
    }

    @Override
    public URI getURI(String mediaPackageID, String mediaPackageElementID) {
      throw new Error();
    }

    @Override
    public URI getCollectionURI(String collectionID, String fileName) {
      throw new Error();
    }

    @Override
    public URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
            throws NotFoundException, IOException {
      throw new Error();
    }

    @Override
    public URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
            throws NotFoundException, IOException {
      throw new Error();
    }

    @Override
    public URI getURI(String mediaPackageID, String mediaPackageElementID, String filename) {
      return new File(workspaceBase, mediaPackageID + "-" + mediaPackageElementID + "-" + filename)
              .toURI();
    }

    @Override
    public Option<Long> getTotalSpace() {
      return Option.<Long> none();
    }

    @Override
    public Option<Long> getUsableSpace() {
      return Option.<Long> none();
    }

    @Override
    public Option<Long> getUsedSpace() {
      return Option.<Long> none();
    }

    @Override
    public void cleanup(Option<Integer> maxAge) {
      // TODO Auto-generated method stub
    }

  }
}
