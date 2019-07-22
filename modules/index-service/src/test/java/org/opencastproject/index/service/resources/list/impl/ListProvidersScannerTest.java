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
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

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
  private Organization defaultOrg;
  private Organization specificOrg;
  private ListProvidersScanner listProvidersScanner;
  private ListProvidersServiceImpl listProvidersService;
  private SecurityService securityService;

  private static File getResourceFile(String resourcePath) throws Exception {
    return new File(ListProvidersScannerTest.class.getResource(resourcePath).toURI());
  }

  @Before
  public void setUp() {
    listName = "TEST.LIST.NAME";
    defaultOrg = EasyMock.createNiceMock(Organization.class);
    specificOrg = EasyMock.createNiceMock(Organization.class);
    listProvidersService = new ListProvidersServiceImpl();
    listProvidersScanner = new ListProvidersScanner();
    securityService = EasyMock.createNiceMock(SecurityService.class);
    listProvidersService.setSecurityService(securityService);
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
    EasyMock.expect(securityService.getOrganization()).andReturn(defaultOrg).anyTimes();
    EasyMock.expect(defaultOrg.getId()).andReturn("mh_default_org").anyTimes();

    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.install(file);

    EasyMock.replay(securityService, defaultOrg);

    assertTrue("Provider has not been registered", listProvidersService.hasProvider(listName));
    assertEquals(1, listProvidersService.getAvailableProviders().size());
    assertEquals(3, listProvidersService
            .getList(listName, null, false).size()
    );
    assertEquals(listName, listProvidersService.getAvailableProviders().get(0));
    assertEquals("TEST.VALUE.1", listProvidersService
            .getList(listName, null, false)
            .get("TEST.KEY.1")
    );
  }

  @Test
  public void testDefaultUpdate() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.update(file);
    Map<String, String> dictionary = listProvidersService.getList(listName, null, false);

    assertEquals("TEST.VALUE.1", dictionary.get("TEST.KEY.1"));
    assertEquals("TEST.VALUE.2", dictionary.get("TEST.KEY.2"));
    assertEquals("TEST.VALUE.3", dictionary.get("TEST.KEY.3"));
  }

  @Test
  public void testDefaultUninstall() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-GoodProperties.properties");
    listProvidersScanner.uninstall(file);
    assertTrue("Provider was not removed", !listProvidersService.hasProvider(listName));
  }

  @Test
  public void testSpecificOrgInstall() throws Exception {
    EasyMock.expect(securityService.getOrganization()).andReturn(specificOrg).anyTimes();
    EasyMock.expect(specificOrg.getId()).andReturn("org1").anyTimes();

    listName = "TEST.LIST.NAME";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(file);

    EasyMock.replay();

    assertTrue("Provider has not been registered", listProvidersService.hasProvider(listName, "org1"));
  }

  @Test
  public void testSpecificOrgUninstall() throws Exception {
    EasyMock.expect(securityService.getOrganization()).andReturn(specificOrg).anyTimes();
    EasyMock.expect(specificOrg.getId()).andReturn("org1").anyTimes();
    listName = "TEST.LIST.NAME";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.uninstall(file);

    EasyMock.replay();

    assertTrue("Provider was not removed", !listProvidersService.hasProvider(listName, "org1"));
  }

  @Test
  public void testIsTranslatable() throws Exception {
    EasyMock.expect(securityService.getOrganization()).andReturn(specificOrg).anyTimes();
    EasyMock.expect(specificOrg.getId()).andReturn("org1").anyTimes();
    listName = "TEST.LIST.NAME";
    File file = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(file);

    EasyMock.replay(securityService, specificOrg);

    assertTrue("Translatable property not read correctly", listProvidersService.isTranslatable(listName));
  }

  @Test
  public void testGetDefault() throws Exception {
    EasyMock.expect(securityService.getOrganization()).andReturn(specificOrg).anyTimes();
    EasyMock.expect(specificOrg.getId()).andReturn("org1").anyTimes();
    listName = "TEST.LIST.NAME";
    File fileWithDefault = getResourceFile("/ListProvidersScannerTest-AllProperties.properties");
    listProvidersScanner.install(fileWithDefault);

    EasyMock.replay(securityService, specificOrg);

    assertEquals("TEST.VALUE.1", listProvidersService.getDefault(listName));
  }

  @Test
  public void testThrowsExceptions() throws Exception {
    File file = getResourceFile("/ListProvidersScannerTest-WithOrg.properties");
    listProvidersScanner.install(file);
    try {
      String defaultValue = listProvidersService.getDefault(listName);
    } catch (ListProviderException e) {
      assertEquals("No provider found for organisation <*> with the name " + listName, e.getMessage());
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

    EasyMock.expect(securityService.getOrganization()).andReturn(org1).anyTimes();
    EasyMock.replay(securityService);

    File file = getResourceFile("/ListProvidersScannerTest-WithOrg.properties");
    listProvidersScanner.install(file);

    ResourceListQuery query = new ResourceListQueryImpl();

    assertEquals(1, listProvidersService.getAvailableProviders().size());
    assertEquals(listName, listProvidersService.getAvailableProviders().get(0));
    assertEquals("org1", org1.getId());
    assertTrue("Provider is not registered", listProvidersService.hasProvider(listName, org1.getId()));
    Map<String, String> dictionary = listProvidersService.getList(listName, query, false);

    assertEquals(3, dictionary.size());

    assertEquals("TEST.VALUE.1", dictionary.get("TEST.KEY.1"));
    assertEquals("TEST.VALUE.2", dictionary.get("TEST.KEY.2"));
    assertEquals("TEST.VALUE.3", dictionary.get("TEST.KEY.3"));
  }
}
