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
package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EncoderEngineProcessParametersTest {

  private String processParameters(String cmd, Map<String, String> params) throws Exception {
    EncoderEngine engine = new EncoderEngine("ffmpeg");
    Method method = EncoderEngine.class.getDeclaredMethod("processParameters", String.class, Map.class);
    method.setAccessible(true);
    return (String) method.invoke(engine, cmd, params);
  }

  @Test
  public void testRandomPlaceholderRangeIsInclusive() throws Exception {
    boolean sawUpperBound = false;
    for (int i = 0; i < 200; i++) {
      String processed = processParameters("out-#{random:0:3}", Collections.emptyMap());
      assertTrue(processed.matches("out-[0-3]"));
      if ("out-3".equals(processed)) {
        sawUpperBound = true;
      }
    }
    assertTrue(sawUpperBound);
  }

  @Test
  public void testRandomPlaceholderSupportsReverseBounds() throws Exception {
    for (int i = 0; i < 50; i++) {
      String processed = processParameters("out-#{random:3:0}", Collections.emptyMap());
      assertTrue(processed.matches("out-[0-3]"));
    }
  }

  @Test
  public void testRandomPlaceholderWorksInNestedTemplateAndUnknownsAreRemoved() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("name", "#{random:0:3}");
    String processed = processParameters("out-#{name}#{missing}", params);
    assertTrue(processed.matches("out-[0-3]"));
    assertFalse(processed.contains("#{"));
    assertEquals(5, processed.length());
  }
}
