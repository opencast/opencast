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
    // No partial matching for quoted strings
    assertEquals("\"Hello\"", QueryPreprocessor.sanitize("\"Hello\""));
    assertEquals("*Hello* \"World\"", QueryPreprocessor.sanitize("Hello \"World\""));

    // Auto-completion and partial matching in case of missing double-quote
    assertEquals("Hello \"World\"*", QueryPreprocessor.sanitize("Hello \"World"));
    assertEquals("Hello \"World Again\"*", QueryPreprocessor.sanitize("Hello \"World Again"));

    // Partial matching for tokens containing quoted strings but starting and ending with characters
    assertEquals("*He\"llo Wor\"ld*", QueryPreprocessor.sanitize("He\"llo Wor\"ld"));

    // double quotes do not delimit tokens
    assertEquals("*Hello\"World\"Again*", QueryPreprocessor.sanitize("Hello\"World\"Again"));
    assertEquals("\"Hello\"\"World\"", QueryPreprocessor.sanitize("\"Hello\"\"World\""));
  }

  @Test
  public void testWildcars() {
    // Don't escape wildcards occuring as individual tokens
    assertEquals("*", QueryPreprocessor.sanitize("*"));
    assertEquals("*", QueryPreprocessor.sanitize(" * "));
    assertEquals("*?*", QueryPreprocessor.sanitize("?"));
    assertEquals("*?*", QueryPreprocessor.sanitize(" ? "));

    // Don't escape wildcards occuring within tokens
    assertEquals("*H*llo* *Worl*d*", QueryPreprocessor.sanitize("H*llo Worl*d"));
    assertEquals("*H?llo* *Worl?d*", QueryPreprocessor.sanitize("H?llo Worl?d"));
  }

  @Test
  public void testUnaryOperators() {
    // Escape operator if operand is missing
    assertEquals("\\-", QueryPreprocessor.sanitize("-"));
    assertEquals("\\-", QueryPreprocessor.sanitize(" - "));

    // Escape operator if occuring within a token
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("test-unit"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("*test-unit"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("test-unit*"));
    assertEquals("*test\\-unit*", QueryPreprocessor.sanitize("*test-unit*"));
    assertEquals("*test\\-unit\\-*", QueryPreprocessor.sanitize("test-unit-"));
    assertEquals("-*\\-test\\-\\-unit\\-\\-*", QueryPreprocessor.sanitize("--test--unit--"));

    // Partial matching for operands
    assertEquals("-*test\\-unit*", QueryPreprocessor.sanitize("-test-unit"));
    assertEquals("-*test\\-unit*", QueryPreprocessor.sanitize("-*test-unit"));
    assertEquals("-*test\\-unit*", QueryPreprocessor.sanitize("-test-unit*"));
    assertEquals("-*test\\-unit*", QueryPreprocessor.sanitize("-*test-unit*"));
  }

  @Test
  public void testBinaryOperators() {
    // Escape operator if operands are missing
    assertEquals("\\&&", QueryPreprocessor.sanitize("&&"));
    assertEquals("*Hello* \\&&", QueryPreprocessor.sanitize("Hello &&"));
    assertEquals("\\&& *World*", QueryPreprocessor.sanitize("&& World"));

    // Don't escape operator if used correctly
    assertEquals("*Hello* && *World*", QueryPreprocessor.sanitize("Hello && World"));
    assertEquals("*Hello* || *World*", QueryPreprocessor.sanitize("Hello || World"));
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
  }

}
