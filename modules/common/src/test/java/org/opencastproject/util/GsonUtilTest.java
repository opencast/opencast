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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the configuration of the shared Gson instance. Each test here pins one of the three defaults that had to be
 * overridden; none of them would hold for a plain <code>new Gson()</code>.
 */
public class GsonUtilTest {

  /** URLs are serialized all over Opencast, so their characters must not be escaped. */
  @Test
  public void testHtmlCharactersAreNotEscaped() {
    JsonObject json = new JsonObject();
    json.addProperty("url", "http://example.org/a?b=c&d=e");

    assertEquals("{\"url\":\"http://example.org/a?b=c&d=e\"}", GsonUtil.gson().toJson(json));
  }

  /** The External API requires empty fields to be present rather than dropped. */
  @Test
  public void testNullsAreSerialized() {
    JsonObject json = new JsonObject();
    json.add("empty", JsonNull.INSTANCE);

    assertEquals("{\"empty\":null}", GsonUtil.gson().toJson(json));
  }

  /** The default number strategy reads every number as a Double, which loses precision on large integers. */
  @Test
  public void testIntegralNumbersStayLong() {
    Map<String, Object> parsed = GsonUtil.gson().fromJson("{\"small\":1,\"large\":9007199254740993}", Map.class);

    assertEquals(Long.valueOf(1L), parsed.get("small"));
    assertEquals(Long.valueOf(9007199254740993L), parsed.get("large"));
  }

  /** Decimals must still come back as Double. */
  @Test
  public void testDecimalNumbersStayDouble() {
    Map<String, Object> parsed = GsonUtil.gson().fromJson("{\"decimal\":1.5}", Map.class);

    assertEquals(Double.valueOf(1.5d), parsed.get("decimal"));
  }

  /**
   * Untyped deserialization must yield plain {@link Map}s and {@link List}s rather than Gson's own tree types, since
   * callers test for those interfaces.
   */
  @Test
  public void testUntypedDeserializationYieldsCollections() {
    Map<String, Object> parsed = GsonUtil.gson().fromJson("{\"obj\":{\"k\":\"v\"},\"arr\":[1,2]}", Map.class);

    assertTrue(parsed.get("obj") instanceof Map);
    assertTrue(parsed.get("arr") instanceof List);
  }
}
