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

package org.opencastproject.uiconfig;

import static org.opencastproject.uiconfig.UIConfigRest.UI_CONFIG_FOLDER_PROPERTY;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;

public class UIConfigTest {
  private static final Logger logger = LoggerFactory.getLogger(UIConfigTest.class);

  private UIConfigRest uiConfigRest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    // create the needed mocks
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn("org1").anyTimes();

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();

    EasyMock.replay(organization, securityService);

    // Set up UI config service
    uiConfigRest = new UIConfigRest();
    uiConfigRest.setSecurityService(securityService);
  }

  @Test
  public void testActivate() throws Exception {
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn(null).times(2);
    EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn("/xy").once();
    EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn(null).once();
    EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn("/xy").once();

    ComponentContext componentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();

    EasyMock.replay(bundleContext, componentContext);

    try {
      uiConfigRest.activate(componentContext);
      Assert.fail();
    } catch (ConfigurationException e) {
      // config and default are null. We expect this to fail
    }

    // Providing proper configuration now. This should work
    uiConfigRest.activate(componentContext);
    uiConfigRest.activate(componentContext);
  }

  @Test
  public void testGetFile() throws Exception {
    final File testDir = temporaryFolder.newFolder();
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn(testDir.getAbsolutePath()).once();

    ComponentContext componentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();

    EasyMock.replay(bundleContext, componentContext);

    uiConfigRest.activate(componentContext);

    // test non-existing file
    try {
      uiConfigRest.getConfigFile("player", "config.json");
      Assert.fail();
    } catch (NotFoundException e) {
      // We expect this to not be found
    }

    // test existing file
    File target = Paths.get(testDir.getAbsolutePath(), "org1", "player", "config.json").toFile();
    Assert.assertTrue(target.getParentFile().mkdirs());
    new FileOutputStream(target).close();
    Response response = uiConfigRest.getConfigFile("player", "config.json");
    Assert.assertEquals(200, response.getStatus());

    // test path traversal
    try {
      uiConfigRest.getConfigFile("../player", "config.json");
      Assert.fail();
    } catch (AccessDeniedException e) {
      // we expect access to be denied
    }
  }
}
