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
package org.opencastproject.external.common;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.junit.Assert.assertEquals;

import com.entwinemedia.fn.data.json.JValue;

import org.apache.http.HttpStatus;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class ApiResponsesTest {

  @Test
  public void testJsonStringOk() throws Exception {
    Response response = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, "body");

    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertEquals("application/v1.0.0+json", response.getMetadata().get("Content-Type").get(0));
  }

  @Test
  public void testJsonOk() throws Exception {
    final JValue json = obj(f("id", v("abcd")), f("values", arr(v("a"), v("b"))));
    Response response = ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, json);

    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertEquals("application/v1.0.0+json", response.getMetadata().get("Content-Type").get(0));
  }

}
