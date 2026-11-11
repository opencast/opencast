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

package org.opencastproject.util.doc;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrganizationTemplateDataTest {

  @Test
  public void nullPropertiesAreIgnored() {
    Map<String, Object> dataModel = new HashMap<>();
    OrganizationTemplateData.addOrganizationProperties(dataModel, null);
    Assert.assertTrue(dataModel.isEmpty());
  }

  @Test
  public void undottedPropertiesAreAvailableInTheFlattenedForm() {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("transcriber", "azure-1");

    OrganizationTemplateData.addOrganizationProperties(dataModel, properties);

    Assert.assertEquals("azure-1", dataModel.get("org_transcriber"));
    Assert.assertEquals(1, dataModel.size());
  }

  @Test
  public void dottedPropertiesAreAvailableInTheDocumentedAndTheFlattenedForm() {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("org.opencastproject.admin.ui.url", "http://admin.example.com");

    OrganizationTemplateData.addOrganizationProperties(dataModel, properties);

    // The flattened form
    Assert.assertEquals("http://admin.example.com", dataModel.get("org_org_opencastproject_admin_ui_url"));
    // The dotted form, as documented in the workflow configuration guide: ${org_org.opencastproject.admin.ui.url}
    Map org = (Map) dataModel.get("org_org");
    Assert.assertNotNull(org);
    Map opencastproject = (Map) org.get("opencastproject");
    Assert.assertNotNull(opencastproject);
    Map admin = (Map) opencastproject.get("admin");
    Assert.assertNotNull(admin);
    Map ui = (Map) admin.get("ui");
    Assert.assertNotNull(ui);
    Assert.assertEquals("http://admin.example.com", ui.get("url"));
  }

  @Test
  public void propertiesSharingAPrefixShareTheNestedStructure() {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("org.opencastproject.admin.ui.url", "http://admin.example.com");
    properties.put("org.opencastproject.engage.ui.url", "http://engage.example.com");

    OrganizationTemplateData.addOrganizationProperties(dataModel, properties);

    Map org = (Map) dataModel.get("org_org");
    Map opencastproject = (Map) org.get("opencastproject");
    Map adminUi = (Map) ((Map) opencastproject.get("admin")).get("ui");
    Assert.assertEquals("http://admin.example.com", adminUi.get("url"));
    Map engageUi = (Map) ((Map) opencastproject.get("engage")).get("ui");
    Assert.assertEquals("http://engage.example.com", engageUi.get("url"));
  }

  @Test
  public void aPropertyThatIsADotPrefixOfAnotherKeepsOnlyItsFlattenedForm() {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("a.b", "short-value");
    properties.put("a.b.c", "long-value");

    OrganizationTemplateData.addOrganizationProperties(dataModel, properties);

    // Both properties keep their flattened form
    Assert.assertEquals("short-value", dataModel.get("org_a_b"));
    Assert.assertEquals("long-value", dataModel.get("org_a_b_c"));
    // The nested structure is created by the longer key; the shorter key's dotted form is a map, not its value
    Map orgA = (Map) dataModel.get("org_a");
    Assert.assertTrue(orgA.get("b") instanceof Map);
    Assert.assertEquals("long-value", ((Map) orgA.get("b")).get("c"));
  }

  @Test
  public void aDottedPropertyExtendingAnUndottedOneKeepsOnlyItsFlattenedForm() {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("player", "/play/");
    properties.put("player.mobile", "/m/");

    OrganizationTemplateData.addOrganizationProperties(dataModel, properties);

    // Both properties keep their flattened form
    Assert.assertEquals("/play/", dataModel.get("org_player"));
    Assert.assertEquals("/m/", dataModel.get("org_player_mobile"));
    // The undotted property occupies the root with its value, so the longer key has no dotted form
    Assert.assertEquals(2, dataModel.size());
  }
}
