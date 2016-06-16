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

package org.opencastproject.staticfiles.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Properties;

public class StaticFileServiceImplTest {

  private static String videoFilename = "av.mov";
  private static String imageFilename = "image.jpg";
  /** The File object that is an example image */
  private static File imageFile;
  /** Location where the files are copied to */
  private static File rootDir;
  /** The File object that is an example video */
  private static File videoFile;
  /** The org to use for the tests */
  private static Organization org = new DefaultOrganization();
  /** The org directory service */
  private OrganizationDirectoryService orgDir;

  @BeforeClass
  public static void beforeClass() throws Exception {
    rootDir = Files.createTempDirectory("static-file-service-test").toFile();
    imageFile = new File(StaticFileServiceImplTest.class.getResource("/" + imageFilename).getPath());
    videoFile = new File(StaticFileServiceImplTest.class.getResource("/" + videoFilename).getPath());
  }

  @Before
  public void setUp() throws IOException {
    FileUtils.forceMkdir(rootDir);

    orgDir = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDir.getOrganizations()).andReturn(new ArrayList<Organization>()).anyTimes();
    EasyMock.replay(orgDir);
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(rootDir);
  }

  @SuppressWarnings("rawtypes")
  private static ComponentContext getComponentContext(String useWebserver) {
    // Create BundleContext
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(StaticFileServiceImpl.STATICFILES_ROOT_DIRECTORY_KEY)).andReturn(
            rootDir.getAbsolutePath());
    EasyMock.replay(bundleContext);
    // Create ComponentContext
    Dictionary properties = new Properties();
    ComponentContext cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
    return cc;
  }

  private static SecurityService getSecurityService() {
    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(securityService);
    return securityService;
  }

  /**
   * Without the root directory the service should throw a RuntimeException.
   */
  @Test(expected = RuntimeException.class)
  public void testStoreStaticFileThrowsConfigurationException() throws Exception {
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(EasyMock.anyObject(String.class))).andStubReturn(null);
    EasyMock.replay(bundleContext);

    ComponentContext cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andStubReturn(bundleContext);
    EasyMock.replay(cc);

    // Run the test
    StaticFileServiceImpl staticFile = new StaticFileServiceImpl();
    staticFile.setOrganizationDirectoryService(orgDir);
    staticFile.activate(cc);
  }

  @Test
  public void testGetStaticFile() throws Exception {
    StaticFileServiceImpl staticFileServiceImpl = new StaticFileServiceImpl();
    staticFileServiceImpl.setOrganizationDirectoryService(orgDir);
    staticFileServiceImpl.activate(getComponentContext(null));
    staticFileServiceImpl.setSecurityService(getSecurityService());

    String videoUUID = staticFileServiceImpl.storeFile(videoFilename, new FileInputStream(videoFile));
    IOUtils.contentEquals(new FileInputStream(videoFile), staticFileServiceImpl.getFile(videoUUID));

    String imageUUID = staticFileServiceImpl.storeFile(imageFilename, new FileInputStream(imageFile));
    IOUtils.contentEquals(new FileInputStream(imageFile), staticFileServiceImpl.getFile(imageUUID));
  }

  @Test
  public void testPersistFile() throws Exception {
    StaticFileServiceImpl staticFileServiceImpl = new StaticFileServiceImpl();
    staticFileServiceImpl.setOrganizationDirectoryService(orgDir);
    staticFileServiceImpl.activate(getComponentContext(null));
    staticFileServiceImpl.setSecurityService(getSecurityService());

    String videoUUID = staticFileServiceImpl.storeFile(videoFilename, new FileInputStream(videoFile));
    String imageUUID = staticFileServiceImpl.storeFile(imageFilename, new FileInputStream(imageFile));

    staticFileServiceImpl.persistFile(videoUUID);
    staticFileServiceImpl.purgeTemporaryStorageSection(getSecurityService().getOrganization().getId(), 0);

    IOUtils.contentEquals(new FileInputStream(videoFile), staticFileServiceImpl.getFile(videoUUID));
    try {
      staticFileServiceImpl.getFile(imageUUID);
      fail("File should no longer exist");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testDeleteStaticFile() throws ConfigurationException, FileNotFoundException, IOException {
    StaticFileServiceImpl staticFileServiceImpl = new StaticFileServiceImpl();
    staticFileServiceImpl.setOrganizationDirectoryService(orgDir);
    staticFileServiceImpl.activate(getComponentContext(null));
    staticFileServiceImpl.setSecurityService(getSecurityService());
    String imageUUID = staticFileServiceImpl.storeFile(imageFilename, new FileInputStream(imageFile));

    try {
      staticFileServiceImpl.deleteFile(imageUUID);
    } catch (NotFoundException e) {
      Assert.fail("File not found for deletion");
    }

    try {
      staticFileServiceImpl.getFile(imageUUID);
      Assert.fail("File not deleted");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      staticFileServiceImpl.deleteFile(imageUUID);
      Assert.fail("File not deleted");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetFileName() throws Exception {
    StaticFileServiceImpl staticFileServiceImpl = new StaticFileServiceImpl();
    staticFileServiceImpl.setOrganizationDirectoryService(orgDir);
    staticFileServiceImpl.activate(getComponentContext(null));
    staticFileServiceImpl.setSecurityService(getSecurityService());

    String imageUUID = staticFileServiceImpl.storeFile(imageFilename, new FileInputStream(imageFile));
    assertEquals(imageFile.getName(), staticFileServiceImpl.getFileName(imageUUID));
  }

}
