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

package org.opencastproject.security.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.security.api.Organization;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SecurityUtilTest {

  private Organization organization;

  @Before
  public void setUp() {
    organization = createMock(Organization.class);
  }

  @Test
  public void returnsEmptyMapWhenNoAdditionalActions() {
    expect(organization.getProperties()).andReturn(Collections.emptyMap()).once();
    replay(organization);

    Map<String, String> result = SecurityUtil.additionalAclActions(organization);

    verify(organization);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void returnsAdditionalActionsWhenDefined() {
    Map<String, String> properties = new HashMap<>();
    properties.put("org.opencastproject.acl.additional.actions.action1", "value1");
    properties.put("org.opencastproject.acl.additional.actions.action2", "value2");

    expect(organization.getProperties()).andReturn(properties).once();
    replay(organization);

    Map<String, String> result = SecurityUtil.additionalAclActions(organization);

    verify(organization);
    assertEquals(2, result.size());
    assertEquals("value1", result.get("action1"));
    assertEquals("value2", result.get("action2"));
  }

  @Test
  public void ignoresNonAdditionalActionsProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put("org.opencastproject.acl.additional.actions.action1", "value1");
    properties.put("some.other.property", "value2");

    expect(organization.getProperties()).andReturn(properties).once();
    replay(organization);

    Map<String, String> result = SecurityUtil.additionalAclActions(organization);

    verify(organization);
    assertEquals(1, result.size());
    assertEquals("value1", result.get("action1"));
  }

  @Test
  public void returnedMapIsUnmodifiable() {
    Map<String, String> properties = new HashMap<>();
    properties.put("org.opencastproject.acl.additional.actions.action1", "value1");

    expect(organization.getProperties()).andReturn(properties).once();
    replay(organization);

    Map<String, String> result = SecurityUtil.additionalAclActions(organization);

    verify(organization);
    try {
      result.put("new", "x");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }
}
