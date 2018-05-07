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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApiMediaTypeTest {

  @Test
  public void testDefaultVersionAndFormat() throws Exception {
    ApiMediaType type = ApiMediaType.parse("*/*");
    assertEquals(ApiVersion.VERSION_1_1_0, type.getVersion());
    assertEquals(ApiFormat.JSON, type.getFormat());
    assertEquals("application/v1.1.0+json", type.toExternalForm());

    type = ApiMediaType.parse("application/*");
    assertEquals(ApiVersion.VERSION_1_1_0, type.getVersion());
    assertEquals(ApiFormat.JSON, type.getFormat());
    assertEquals("application/v1.1.0+json", type.toExternalForm());

    type = ApiMediaType.parse("application/json");
    assertEquals(ApiVersion.VERSION_1_1_0, type.getVersion());
    assertEquals(ApiFormat.JSON, type.getFormat());
    assertEquals("application/v1.1.0+json", type.toExternalForm());
  }

  @Test
  public void testParseJsonWithVersion() throws Exception {
    ApiMediaType type = ApiMediaType.parse("application/v1.0.0+json");
    assertEquals(ApiVersion.VERSION_1_0_0, type.getVersion());
    assertEquals(ApiFormat.JSON, type.getFormat());
    assertEquals("application/v1.0.0+json", type.toExternalForm());

    type = ApiMediaType.parse("application/v1.1.0+json");
    assertEquals(ApiVersion.VERSION_1_1_0, type.getVersion());
    assertEquals(ApiFormat.JSON, type.getFormat());
    assertEquals("application/v1.1.0+json", type.toExternalForm());
  }

  @Test(expected = ApiMediaTypeException.class)
  public void testParseJsonWithInvalidVersion() throws Exception {
    final ApiMediaType type = ApiMediaType.parse("application/v0.0.0+json");
  }
}
