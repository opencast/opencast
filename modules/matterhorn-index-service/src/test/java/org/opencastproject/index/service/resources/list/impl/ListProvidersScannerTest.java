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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.security.api.Organization;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class ListProvidersScannerTest {
  private static final Logger logger = LoggerFactory.getLogger(ListProvidersScannerTest.class);

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
  public void testInstall() throws Exception {
    String listName = "BLACKLISTS.USERS.REASONS";
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-GoodProperties.properties").toURI());
    Capture<ResourceListProvider> resourceListProvider = new Capture<>();
    Capture<String> captureListName = new Capture<>();
    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    listProvidersService.addProvider(EasyMock.capture(captureListName), EasyMock.capture(resourceListProvider));
    EasyMock.expectLastCall();
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.install(file);

    assertEquals(1, resourceListProvider.getValues().size());
    assertEquals(listName, resourceListProvider.getValue().getListNames()[0]);
    assertEquals("Sick Leave",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.SICK_LEAVE"));
    assertEquals("Leave",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.LEAVE"));
    assertEquals("Family Emergency",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.FAMILY_EMERGENCY"));
    assertNull(resourceListProvider.getValue().getList("Wrong List Name", null, null));
  }

  @Test
  public void testUpdate() throws Exception {
    String listName = "BLACKLISTS.USERS.REASONS";
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-GoodProperties.properties").toURI());
    Capture<ResourceListProvider> resourceListProvider = new Capture<>();
    Capture<String> captureListName = new Capture<>();
    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    listProvidersService.addProvider(EasyMock.capture(captureListName), EasyMock.capture(resourceListProvider));
    EasyMock.expectLastCall();
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.update(file);

    assertEquals(1, resourceListProvider.getValues().size());
    assertEquals(listName, resourceListProvider.getValue().getListNames()[0]);
    assertEquals("Sick Leave",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.SICK_LEAVE"));
    assertEquals("Leave",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.LEAVE"));
    assertEquals("Family Emergency",
            resourceListProvider.getValue().getList(listName, null, null).get("PM.BLACKLIST.REASONS.FAMILY_EMERGENCY"));
  }

  @Test
  public void testUninstall() throws Exception {
    String listName = "BLACKLISTS.USERS.REASONS";
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-GoodProperties.properties").toURI());
    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    listProvidersService.removeProvider(listName);
    EasyMock.expectLastCall();
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.uninstall(file);
  }

  @Test
  public void testInstallInputMissingListNameInPropertiesFileExpectsNotAddedToService() throws Exception {
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-MissingListName.properties").toURI());
    ListProvidersService listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.install(file);
  }

  @Test
  public void testInstallInputEmptyListNameInPropertiesFileExpectsNotAddedToService() throws Exception {
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-EmptyListName.properties").toURI());
    ListProvidersService listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.install(file);
  }

  @Test
  public void testInstallInputOrgInPropertiesFileExpectsAddedToService() throws Exception {
    Organization org1 = EasyMock.createMock(Organization.class);
    EasyMock.expect(org1.getId()).andReturn("org1").anyTimes();
    EasyMock.replay(org1);

    Organization org2 = EasyMock.createMock(Organization.class);
    EasyMock.expect(org2.getId()).andReturn("org2").anyTimes();
    EasyMock.replay(org2);

    String listName = "BLACKLISTS.USERS.REASONS";
    File file = new File(
            ListProvidersScannerTest.class.getResource("/ListProvidersScannerTest-WithOrg.properties").toURI());
    Capture<ResourceListProvider> resourceListProvider = new Capture<>();
    Capture<String> captureListName = new Capture<>();
    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    listProvidersService.addProvider(EasyMock.capture(captureListName), EasyMock.capture(resourceListProvider));
    EasyMock.expectLastCall();
    EasyMock.replay(listProvidersService);

    ListProvidersScanner listProvidersScanner = new ListProvidersScanner();
    listProvidersScanner.setListProvidersService(listProvidersService);
    listProvidersScanner.install(file);

    ResourceListQuery query = new ResourceListQueryImpl();

    assertEquals(1, resourceListProvider.getValues().size());
    assertEquals(listName, resourceListProvider.getValue().getListNames()[0]);
    Map<String, String> stuff = resourceListProvider.getValue().getList(listName, query, org1);
    for (String key : stuff.keySet()) {
      logger.info("Key: {}, Value {}.", key, stuff.get(key));
    }
    assertEquals(3, resourceListProvider.getValue().getList(listName, query, org1).size());
    assertNull(resourceListProvider.getValue().getList(listName, query, org2));
    assertNull(resourceListProvider.getValue().getList(listName, query, null));
    assertEquals("Sick Leave",
            resourceListProvider.getValue().getList(listName, null, org1).get("PM.BLACKLIST.REASONS.SICK_LEAVE"));
    assertEquals("Leave",
            resourceListProvider.getValue().getList(listName, null, org1).get("PM.BLACKLIST.REASONS.LEAVE"));
    assertEquals("Family Emergency",
            resourceListProvider.getValue().getList(listName, null, org1).get("PM.BLACKLIST.REASONS.FAMILY_EMERGENCY"));
  }
}
