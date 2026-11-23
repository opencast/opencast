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

package org.opencastproject.textanalyzer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

public class TextAnalyzerServiceImplTest {

  private TextAnalyzerServiceImpl service;

  @Before
  public void setUp() {
    service = new TextAnalyzerServiceImpl();
  }

  private void configure(String enabled, String pattern) throws ConfigurationException {
    Map<String, Object> properties = new HashMap<>();
    if (enabled != null) {
      properties.put(TextAnalyzerServiceImpl.FILTER_ENABLED_KEY, enabled);
    }
    if (pattern != null) {
      properties.put(TextAnalyzerServiceImpl.FILTER_PATTERN_KEY, pattern);
    }
    service.updateFilter(properties);
  }

  @Test
  public void testDefaultFilter() throws Exception {
    assertEquals("This is a test sentence", service.filterText("This is a test sentence."));
    configure(null, null);
    assertEquals("This is a test sentence", service.filterText("This is a test sentence."));
  }

  @Test
  public void testEmpty() throws Exception {
    assertNull(service.filterText(""));
    assertNull(service.filterText(" .,; "));
    configure("false", null);
    assertNull(service.filterText(""));
    assertNull(service.filterText("   "));
  }

  @Test
  public void testFilterDisabled() throws Exception {
    configure("false", "[a-z]+");
    String in = "This is a test sentence.";
    assertEquals(in, service.filterText(in));
  }

  @Test
  public void testInvalidPattern() throws Exception {
    configure(null, "[a-z]+");
    try {
      configure(null, "*[[[[");
      fail("Invalid pattern should be rejected");
    } catch (ConfigurationException e) {
      assertEquals(TextAnalyzerServiceImpl.FILTER_PATTERN_KEY, e.getProperty());
    }
    // the previous pattern is still in use
    assertEquals("his is", service.filterText("This is"));
  }

  @Test
  public void testSpecialCharactersDE() throws Exception {
    String in = "Zwölf Boxkämpfer jagten Victor quer über den großen Sylter Deich.";
    configure(null, "[\\wßäöüÄÖÜ,.!]+");
    assertEquals(in, service.filterText(in));
  }

  @Test
  public void testSpecialCharactersES() throws Exception {
    String in = "El veloz murciélago hindú comía feliz cardillo y kiwi. "
        + "La cigüeña tocaba el saxofón detrás del palenque de paja.";
    configure(null, "[¿¡(]*[\\wáéíóúÁÉÍÓÚüÜñÑ]+[)-.,:;!?]*");
    assertEquals(in, service.filterText(in));
  }
}
