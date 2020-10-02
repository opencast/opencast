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
package org.opencastporject.security.aai.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.aai.api.AttributeMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:attributemapper.xml"})
public class AttributeMapperTest {

  @Resource(name = "attributeMapper")
  protected AttributeMapper attributeMapper;

  @Test
  public void testAttributeMapping() {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>();
    attributes.put("eduPersonEntitlement",
        CollectionUtils.arrayToList("octest1;octest2;urn:mace:wurst".split(";")));
    List<String> roles = attributeMapper.getMappedAttributes(attributes,
        "roles");
    assertNotNull(roles);
    assertEquals(2, roles.size());
  }

  @Test
  public void testServletMapping() {
    final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", null);
    mockRequest.addHeader("eduPersonEntitlement",
        "octest1;octest2;urn:mace:wurst;urn:mace:example.org:permission:shibboleth:opencast");
    mockRequest.addHeader("sn", "Doe");
    mockRequest.addHeader("givenName", "John");
    mockRequest.addHeader("eduPersonPrincipalName", "john.doe@example.org");
    List<String> roles = attributeMapper.getMappedAttributes(mockRequest, "roles");
    assertNotNull(roles);
    assertEquals(4, roles.size());

    List<String> displayName = attributeMapper.getMappedAttributes(mockRequest, "displayName");
    assertNotNull(displayName);
    assertEquals(1, displayName.size());
    assertEquals("John Doe", displayName.get(0));
  }

  @Test (expected = IllegalArgumentException.class)
  public void testServletMappingFailure() {
    final MockHttpServletRequest mockRequest = new MockHttpServletRequest(
        "GET", null);

    List<String> any = attributeMapper.getMappedAttributes(mockRequest,
        "notExisting");
  }

  @Test
  public void testUserMapping() {
    final MockHttpServletRequest mockRequest = new MockHttpServletRequest(
        "GET", null);
    mockRequest.addHeader("sn", "Doe");
    mockRequest.addHeader("givenName", "John");
    mockRequest.addHeader("eduPersonPrincipalName",
        "john.doe@example.org;ron.smith@example.org");
    List<String> roles = attributeMapper.getMappedAttributes(mockRequest,
        "roles");
    assertNotNull(roles);
    assertEquals(4, roles.size());

    List<String> displayName = attributeMapper.getMappedAttributes(mockRequest, "displayName");
    assertNotNull(displayName);
    assertEquals(1, displayName.size());
    assertEquals("John Doe", displayName.get(0));
  }

  @Test
  public void testDuplicateAttributeMapping() {
    final MockHttpServletRequest mockRequest = new MockHttpServletRequest(
        "GET", null);
    mockRequest.addHeader("sn", "Doe");
    mockRequest.addHeader("givenName", "John");
    mockRequest.addHeader("givenName", "Charles");
    mockRequest.addHeader("eduPersonPrincipalName",
        "john.doe@example.org;ron.smith@example.org");

    boolean thrown = false;
    List<String> displayName = attributeMapper.getMappedAttributes(mockRequest, "displayName");
    assertNotNull(displayName);
    assertEquals(1, displayName.size());
    assertEquals("John Doe", displayName.get(0));
    assertNotEquals("Charles", displayName.get(0));
    try {
      assertEquals("Charles", displayName.get(1));
    } catch (ArrayIndexOutOfBoundsException e) {
      thrown = true;
    }
    assertTrue(thrown);
  }
}
