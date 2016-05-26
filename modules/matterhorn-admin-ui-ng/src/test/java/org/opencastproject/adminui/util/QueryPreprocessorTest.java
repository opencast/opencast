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

package org.opencastproject.adminui.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryPreprocessorTest {

  @Test
  public void testBlankQueries() {
    assertEquals("", QueryPreprocessor.sanitize(""));
    assertEquals("", QueryPreprocessor.sanitize("   "));
  }

  public void testDoubleQuotes() {
    assertEquals("Hello \"World\"", QueryPreprocessor.sanitize("Hello \"World\""));
    assertEquals("Hello \"World\"*", QueryPreprocessor.sanitize("Hello \"World"));
    assertEquals("\"Hello World\"", QueryPreprocessor.sanitize("\"Hello World\""));
    assertEquals("*He\"llo Wor\"ld*", QueryPreprocessor.sanitize("He\"llo Wor\"ld"));
  }

  @Test
  public void testUnaryOperators() {
    assertEquals("\\-", QueryPreprocessor.sanitize("-"));
    assertEquals("\\-", QueryPreprocessor.sanitize(" - "));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("test-unit"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("*test-unit"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("test-unit*"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("*test-unit*"));
    assertEquals("-*test\\-unit*", QueryPreprocessor.sanitize("-test-unit"));
    assertEquals("*test\\-unit\\-*", QueryPreprocessor.sanitize("test-unit-"));
    assertEquals("-*\\-test\\-\\-unit\\-\\-*", QueryPreprocessor.sanitize("--test--unit--"));
  }

  public void testPartialMatches() {
    assertEquals("*Hello*", QueryPreprocessor.sanitize("Hello"));
    assertEquals("*Hello*", QueryPreprocessor.sanitize("*Hello"));
    assertEquals("*Hello*", QueryPreprocessor.sanitize("Hello*"));
    assertEquals("*Hello*", QueryPreprocessor.sanitize("*Hello*"));
    assertEquals("*Hello* *World*", QueryPreprocessor.sanitize("Hello World"));
    assertEquals("*Hello* *World*", QueryPreprocessor.sanitize("Hello* World"));
    assertEquals("*Hello* *World*", QueryPreprocessor.sanitize("Hello *World"));
    assertEquals("*Hello* *World*", QueryPreprocessor.sanitize("*Hello* *World*"));


    assertEquals("", QueryPreprocessor.sanitize(""));
    assertEquals("", QueryPreprocessor.sanitize(""));
    assertEquals("", QueryPreprocessor.sanitize(""));
    assertEquals("", QueryPreprocessor.sanitize(""));
  }

}
