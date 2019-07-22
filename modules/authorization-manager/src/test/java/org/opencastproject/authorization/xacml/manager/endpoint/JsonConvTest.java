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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static io.restassured.path.json.JsonPath.from;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.security.api.AccessControlUtil.acl;
import static org.opencastproject.security.api.AccessControlUtil.entry;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

import io.restassured.path.json.JsonPath;

public final class JsonConvTest {
  private static final Logger logger = LoggerFactory.getLogger(JsonConvTest.class);

  private static final ManagedAcl macl = new ManagedAcl() {
    @Override public Long getId() {
      return 1L;
    }

    @Override public String getOrganizationId() {
      return "default_org";
    }

    @Override public String getName() {
      return "Public";
    }

    @Override public AccessControlList getAcl() {
      return acl(entry("anonymous", "read", true));
    }
  };

  private static final Date now = new Date();

  @Test
  public void testManagedAclFull() {
    String json = JsonConv.full(macl).toJson();
    logger.info(json);
    JsonPath jp = from(json);
    assertEquals(4, ((Map) jp.get()).size());
    assertEquals(new Integer(1), jp.get("id"));
    assertEquals("default_org", jp.get("organizationId"));
    assertEquals("Public", jp.get("name"));
    assertEquals(1, ((List) jp.get("acl.ace")).size());
  }

  @Test
  public void testManagedAclDigest() {
    String json = JsonConv.digest(macl).toJson();
    JsonPath jp = from(json);
    logger.info(json);
    assertEquals(2, ((Map) jp.get()).size());
    assertEquals(new Integer(1), jp.get("id"));
    assertEquals("Public", jp.get("name"));
  }

}
