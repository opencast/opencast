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

import org.opencastproject.security.aai.api.AttributeMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import junit.framework.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:attributemapper.xml"})
public class AttributeMapperTest {

  @Resource(name = "attributeMapper")
  protected AttributeMapper attributeMapper;

  @Test
  public void testAttributeMapping() {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>();
    attributes.put("eduPersonEntitlement",
        CollectionUtils.arrayToList("OCEdit;OCEgal;urn:mace:wurst".split(";")));
    List<String> roles = attributeMapper.getMappedAttributes(attributes,
        "roles");
    Assert.assertNotNull(roles);
    Assert.assertEquals(2, roles.size());
  }

  @Test
  public void testServletMapping() {
    final MockHttpServletRequest mockRequest = new MockHttpServletRequest(
        "GET", null);
    mockRequest
        .addHeader("eduPersonEntitlement",
            "OCEdit;OCEgal;urn:mace:wurst;urn:mace:hm.edu:permission:shibboleth:opencast");
    mockRequest.addHeader("sn", "Musterman");
    mockRequest.addHeader("givenName", "Max");
    mockRequest.addHeader("eduPersonPrincipalName", "test@hm.edu");
    List<String> roles = attributeMapper.getMappedAttributes(mockRequest,
        "roles");
    Assert.assertNotNull(roles);
    Assert.assertEquals(3, roles.size());

    List<String> sn = attributeMapper.getMappedAttributes(mockRequest, "sn");
    Assert.assertNotNull(sn);
    Assert.assertEquals(1, sn.size());
    Assert.assertEquals("Musterman", sn.get(0));

  }

  @Test
  @ExpectedException(IllegalArgumentException.class)
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
    mockRequest.addHeader("sn", "Musterman");
    mockRequest.addHeader("givenName", "Max");
    mockRequest.addHeader("eduPersonPrincipalName",
        "strack@hm.edu;schwaner@hm.edu");
    List<String> roles = attributeMapper.getMappedAttributes(mockRequest,
        "roles");
    Assert.assertNotNull(roles);
    Assert.assertEquals(4, roles.size());

    List<String> sn = attributeMapper.getMappedAttributes(mockRequest, "sn");
    Assert.assertNotNull(sn);
    Assert.assertEquals(1, sn.size());
    Assert.assertEquals("Musterman", sn.get(0));
  }

}
