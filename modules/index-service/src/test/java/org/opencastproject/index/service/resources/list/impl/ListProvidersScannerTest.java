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

package org.opencastproject.index.service.resources.list.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class ListProvidersScannerTest {
  private static final Logger logger = LoggerFactory.getLogger(ListProvidersScannerTest.class);
  private String listName;
  private ListProvidersService listProvidersService;
  private ListProvidersScanner listProvidersScanner;

  private static File getResourceFile(String resourcePath) throws Exception {
    return new File(ListProvidersScannerTest.class.getResource(resourcePath).toURI());
  }

  @Before
  public void setUp() {
    listName = "BLACKLISTS.USERS.REASONS";
    listProvidersService = new ListProvidersServiceImpl();
    listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
  }

  @Test
  public void testCanHandle() {
    File wrongDirectory = new File("wrong");
    File correctDirectory = new File(ListProvidersScanner.LIST_PROVIDERS_DIRECTORY);
    File wrongFilenameWrongDirectory = new File(wrongDirectory, "wrong.xml");
    File wrongFilenameRightDirectory = new File(correctDirectory, "wrong.xml");
    File rightFilenameWrongDirectory = new File(wrongDirectory, "right.properties");
    File rightFilenameRightDirectory = new File(correctDirectory, "right.properties");

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    assertFalse(listProvidersScanner.canHandle(wrongFilenameWrongDirectory));
    assertFalse(listProvidersScanner.canHandle(wrongFilenameRightDirectory));
    assertFalse(listProvidersScanner.canHandle(rightFilenameWrongDirectory));
    assertTrue(listProvidersScanner.canHandle(rightFilenameRightDirectory));
  }

  @Test
  public void testDefaultInstall() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.install(file);

    assertTrue("Provider has not been registered", listProvidersService.hasProvider(listName));
    assertEquals(1, listProvidersService.getAvailableProviders().size());
    assertEquals(3, listProvidersService
            .getList(listName, null, new DefaultOrganization(), false).size()
    );
    assertEquals(listName, listProvidersService.getAvailableProviders().get(0));
    assertEquals("Sick Leave", listProvidersService
            .getList(listName, null, new DefaultOrganization(), false)
            .get("PM.BLACKLIST.REASONS.SICK_LEAVE")
    );
  }

  @Test
  public void testDefaultUpdate() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.update(file);
    Map<String, String> dictionary = listProvidersService.getList(listName, null, new DefaultOrganization(), false);

    assertEquals("Sick Leave", dictionary.get("PM.BLACKLIST.REASONS.SICK_LEAVE"));
    assertEquals("Leave", dictionary.get("PM.BLACKLIST.REASONS.LEAVE"));
    assertEquals("Family Emergency", dictionary.get("PM.BLACKLIST.REASONS.FAMILY_EMERGENCY"));
  }

  @Test
  public void testDefaultUninstall() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.uninstall(file);
    assertTrue("Provider was not removed", !listProvidersService.hasProvider(listName));
  }

  @Test
  public void testSpecificOrgInstall() throws Exception {
    listName = "DEBUG";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(file);
    assertTrue("Provider has not been registered", listProvidersService.hasProvider(listName, "ch-switch"));
  }

  @Test
  public void testSpecificOrgUninstall() throws Exception {
    listName = "DEBUG";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.uninstall(file);
    assertTrue("Provider was not removed", !listProvidersService.hasProvider(listName, "ch-switch"));
  }

  @Test
  public void testIsTranslatable() throws Exception {
    listName = "DEBUG";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(file);

    assertTrue("Translatable property not read correctly", listProvidersService.isTranslatable(listName, "ch-switch"));
  }

  @Test
  public void testGetDefault() throws Exception {
    listName = "DEBUG";
    File fileWithDefault = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(fileWithDefault);

    assertEquals("dokay", listProvidersService.getDefault(listName, "ch-switch"));
  }

  @Test
  public void testThrowsExceptions() throws Exception {
    File fileWithDefault = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.install(fileWithDefault);

    try {
      String defaultValue = listProvidersService.getDefault(listName, "ch-switch");
    } catch (ListProviderException e) {
      assertEquals("No provider found for organisation <ch-switch> with the name " + listName, e.getMessage());
    }
  }

  @Test
  public void testInstallInputMissingListNameInPropertiesFileExpectsNotAddedToService() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-MissingListName.properties");
    listProvidersScanner.install(file);

    assertFalse("Provider should not be added without a name", listProvidersService.hasProvider(listName));
  }

  @Test
  public void testInstallInputEmptyListNameInPropertiesFileExpectsNotAddedToService() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-EmptyListName.properties");
    listProvidersScanner.install(file);

    assertFalse("Provider should not be added without a name", listProvidersService.hasProvider(listName));
  }

  @Test
  public void testInstallInputOrgInPropertiesFileExpectsAddedToService() throws Exception {
    Organization org1 = EasyMock.createMock(Organization.class);
    EasyMock.expect(org1.getId()).andReturn("org1").anyTimes();
    EasyMock.replay(org1);

    Organization org2 = EasyMock.createMock(Organization.class);
    EasyMock.expect(org2.getId()).andReturn("org2").anyTimes();
    EasyMock.replay(org2);

    File file = getResourceFile("/ListProvidersScannerTest-WithOrg.properties");
    listProvidersScanner.install(file);

    ResourceListQuery query = new ResourceListQueryImpl();

    assertEquals(1, listProvidersService.getAvailableProviders().size());
    assertEquals(listName, listProvidersService.getAvailableProviders().get(0));
    assertEquals("org1", org1.getId());
    assertTrue("Provider is not registered", listProvidersService.hasProvider(listName, org1.getId()));
    Map<String, String> dictionary = listProvidersService.getList(listName, query, org1, false);
    for (String key : dictionary.keySet()) {
      logger.info("Key: {}, Value {}.", key, dictionary.get(key));
    }

    assertEquals(3, dictionary.size());

    assertEquals("Sick Leave", dictionary.get("PM.BLACKLIST.REASONS.SICK_LEAVE"));
    assertEquals("Leave", dictionary.get("PM.BLACKLIST.REASONS.LEAVE"));
    assertEquals("Family Emergency", dictionary.get("PM.BLACKLIST.REASONS.FAMILY_EMERGENCY"));
  }
}
