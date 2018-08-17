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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.IoSupport.loadFileFromClassPathAsString;
import static org.opencastproject.util.JsonVal.asBoolean;
import static org.opencastproject.util.JsonVal.asInteger;
import static org.opencastproject.util.JsonVal.asString;

import org.junit.Before;
import org.junit.Test;

public class JsonObjTest {

  private String jsonString;

  @Before
  public void setUp() {
    jsonString = loadFileFromClassPathAsString("/org/opencastproject/util/obj-test.json").get();
  }

  @Test
  public void testParse() {
    final JsonObj json = JsonObj.jsonObj(jsonString);
    assertEquals("org.opencastproject.analytics", json.getObj("service").get(String.class, "type"));
    assertEquals(true, json.obj("service").get(Boolean.class, "active"));
    assertEquals(true, json.obj("service").val("active").as(asBoolean));
    assertEquals("http://localhost:8080", json.obj("service").val("host").as(asString));
    assertEquals(new Integer(1), json.obj("service").arr("array").val(0).as(asInteger));
  }

  @Test(expected = Exception.class)
  public void testTypeMismatch() {
    final JsonObj json = JsonObj.jsonObj(jsonString);
    json.getObj("service").get(String.class, "active");
  }

}
