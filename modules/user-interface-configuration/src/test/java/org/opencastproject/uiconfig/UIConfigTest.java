/*
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
import static org.opencastproject.uiconfig.UIConfigRest.X_ACCEL_REDIRECT_PROPERTY;

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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;

public class UIConfigTest {

  private UIConfigRest uiConfigRest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
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
    Map<String, String> properties = EasyMock.createMock(Map.class);
    EasyMock.expect(properties.get(UI_CONFIG_FOLDER_PROPERTY)).andReturn(null).times(2);
    EasyMock.expect(properties.get(UI_CONFIG_FOLDER_PROPERTY)).andReturn("/xy").once();
    EasyMock.expect(properties.get(X_ACCEL_REDIRECT_PROPERTY)).andReturn(null).once();
    EasyMock.expect(properties.get(X_ACCEL_REDIRECT_PROPERTY)).andReturn("/xy").once();

    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn(null).once();
    EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn("/xy").once();

    EasyMock.replay(bundleContext, properties);

    // Config and default are null. We expect this to fail
    Assert.assertThrows(ConfigurationException.class, () -> {
      uiConfigRest.activate(bundleContext, properties);
    });

    // Providing proper configuration now. This should work
    uiConfigRest.activate(bundleContext, properties);
    uiConfigRest.activate(bundleContext, properties);
  }

  @Test
  public void testGetFile() throws Exception {
    final File testDir = temporaryFolder.newFolder();

    // configure service
    uiConfigRest.activate(null, Collections.singletonMap(UI_CONFIG_FOLDER_PROPERTY, testDir.getAbsolutePath()));

    // Test non-existing file
    // We expect this to not be found
    Assert.assertThrows(NotFoundException.class, () -> {
      uiConfigRest.getConfigFile("player", "config.json");
    });

    // test existing file
    File target = Paths.get(testDir.getAbsolutePath(), "org1", "player", "config.json").toFile();
    Assert.assertTrue(target.getParentFile().mkdirs());
    new FileOutputStream(target).close();
    Response response = uiConfigRest.getConfigFile("player", "config.json");
    Assert.assertEquals(200, response.getStatus());

    // Test path traversal
    // we expect access to be denied
    Assert.assertThrows(AccessDeniedException.class, () -> {
      uiConfigRest.getConfigFile("../player", "config.json");
    });
  }

  @Test
  public void testXAccel() throws Exception {
    // configure service
    uiConfigRest.activate(null, Map.of(
        UI_CONFIG_FOLDER_PROPERTY, temporaryFolder.newFolder().getAbsolutePath(),
        X_ACCEL_REDIRECT_PROPERTY, "/test"));

    // Test response. It doesn't matter if the file exists since we rely on the reverse proxy to complain.
    // The code will fall back to the default organization due to the files non-existence.
    Response response = uiConfigRest.getConfigFile("player", "config.json");
    Assert.assertEquals(204, response.getStatus());
    Assert.assertEquals("/test/mh_default_org/player/config.json", response.getHeaderString(X_ACCEL_REDIRECT_PROPERTY));
  }
}
