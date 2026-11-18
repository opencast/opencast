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

package org.opencastproject.util.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assume;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests that the FreeMarker configuration used by {@link DocUtil} is safe for templates whose content
 * is influenced by user-controlled values. Workflow properties are substituted into operation
 * configuration values before those values are rendered as FreeMarker templates, so the engine must
 * never allow template content to instantiate classes, access the Java API or load other templates,
 * and template errors must not leak the template or the data into the rendered output or into
 * thrown exceptions (whose stack traces are stored in incident details).
 */
public class DocUtilTest {

  /**
   * @param entries
   *          key/value pairs
   * @return a data model (never empty: an empty model makes DocUtil return the template unprocessed)
   */
  private static Map<String, Object> data(String... entries) {
    Map<String, Object> data = new HashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      data.put(entries[i], entries[i + 1]);
    }
    return data;
  }

  @Test
  public void interpolatesData() {
    assertEquals("Hello World!", DocUtil.processTextTemplate("test", "Hello ${name}!", data("name", "World")));
  }

  @Test
  public void supportsDefaultValues() {
    assertEquals("fallback", DocUtil.processTextTemplate("test", "${missing!'fallback'}", data("name", "value")));
  }

  @Test
  public void supportsConditionals() {
    assertEquals("yes", DocUtil.processTextTemplate("test", "<#if flag == 'on'>yes<#else>no</#if>",
            data("flag", "on")));
  }

  @Test
  public void cannotInstantiateClasses() throws Exception {
    Path marker = Files.createTempFile("docutil-ssti", null);
    Files.delete(marker);
    try {
      // The command is only escaped for the FreeMarker string literal: FreeMarker's Execute uses
      // Runtime.exec(String), which splits the command on whitespace without a shell, so shell
      // quoting would end up in the path as literal characters
      Assume.assumeFalse("The marker path must not contain whitespace for the payload to work",
              marker.toAbsolutePath().toString().matches(".*\\s.*"));
      String command = "touch " + marker.toAbsolutePath();
      String payload = "${\"freemarker.template.utility.Execute\"?new()(" + freeMarkerString(command) + ")}";
      String result = DocUtil.processTextTemplate("test", payload, data("name", "value"));
      assertTrue("Template errors must produce the generic error result, got: " + result,
              result.startsWith("ERROR::"));
      assertFalse("The template must not be able to instantiate classes (e.g. to execute commands)",
              Files.exists(marker));
    } finally {
      Files.deleteIfExists(marker);
    }
  }

  @Test
  public void cannotAccessTheApi() {
    // With the API built-in disabled, ?api fails while the template is processed
    String result = DocUtil.processTextTemplate("test", "${name?api}", data("name", "value"));
    assertTrue("The ?api built-in must be disabled, got: " + result, result.startsWith("ERROR::"));
  }

  @Test
  public void cannotIncludeTemplates() {
    String result = DocUtil.processTextTemplate("test", "<#include \"/etc/passwd\">", data("name", "value"));
    assertTrue("#include must fail without a template loader, got: " + result, result.startsWith("ERROR::"));
    assertFalse(result.contains("root:"));
  }

  @Test
  public void errorResultDoesNotLeakTemplateOrData() {
    String result = DocUtil.processTextTemplate("test",
            "${\"freemarker.template.utility.Execute\"?new()(\"id\")}",
            data("name", "value", "secret", "super-secret-value"));
    assertTrue(result.startsWith("ERROR::"));
    assertFalse("The data model must not leak into the error result", result.contains("super-secret-value"));
    assertFalse("The template source must not leak into the error result",
            result.contains("freemarker.template.utility.Execute"));
  }

  @Test
  public void parseErrorDoesNotLeakTemplate() {
    String payload = "${top-secret-payload-12345";
    try {
      DocUtil.processTextTemplate("test", payload, data("name", "value"));
      fail("Expected a RuntimeException for an invalid template");
    } catch (RuntimeException e) {
      // Assert on the full stack trace including the cause chain: incident details store it,
      // so the template must not leak through a cause either
      String stackTrace = ExceptionUtils.getStackTrace(e);
      assertFalse("The template source must not leak into the exception or its cause chain, got: " + stackTrace,
              stackTrace.contains("top-secret-payload-12345"));
    }
  }

  /**
   * @param value
   *          a string
   * @return the value escaped for use inside a double-quoted FreeMarker string literal
   */
  private static String freeMarkerString(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
