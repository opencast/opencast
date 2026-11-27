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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests that the FreeMarker configuration used by {@link DocUtil} is safe for templates whose content or data
 * is influenced by user-controlled values (e.g. operation configuration values and workflow properties). The
 * engine must never allow template content to instantiate classes, access the Java API or load other
 * templates, and template errors must not leak the template or the data into the thrown exception (whose
 * stack trace is stored in incident details).
 */
public class DocUtilTest {

  /**
   * @param entries
   *          key/value pairs
   * @return a data model
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
  public void emptyDataModelDoesNotReturnTheTemplateUnprocessed() {
    // Static templates are rendered with an empty (or null) data model, and templates referencing
    // missing variables fail instead of passing through unrendered
    assertEquals("static text", DocUtil.processTextTemplate("test", "static text", new HashMap<>()));
    assertEquals("static text", DocUtil.processTextTemplate("test", "static text", null));
    try {
      DocUtil.processTextTemplate("test", "${missing}", new HashMap<>());
      fail("Expected a RuntimeException for a template that references a missing variable");
    } catch (RuntimeException e) {
      // expected
    }
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
      Assumptions.assumeFalse(marker.toAbsolutePath().toString().matches(".*\\s.*"),
              "The marker path must not contain whitespace for the payload to work");
      String command = "touch " + marker.toAbsolutePath();
      String payload = "${\"freemarker.template.utility.Execute\"?new()(" + freeMarkerString(command) + ")}";
      try {
        DocUtil.processTextTemplate("test", payload, data("name", "value"));
        fail("Expected a RuntimeException: class instantiation must be blocked");
      } catch (RuntimeException e) {
        // Assert on the full stack trace including the cause chain: incident details store it,
        // so the template must not leak through a cause either
        String stackTrace = ExceptionUtils.getStackTrace(e);
        assertFalse(stackTrace.contains(command),
                "The template source must not leak into the exception or its cause chain, got: " + stackTrace);
        assertFalse(Files.exists(marker),
                "The template must not be able to instantiate classes (e.g. to execute commands)");
      }
    } finally {
      Files.deleteIfExists(marker);
    }
  }

  @Test
  public void cannotAccessTheApi() {
    // With the API built-in disabled, ?api fails while the template is processed; if it were enabled,
    // the template would render the object's string representation instead
    try {
      DocUtil.processTextTemplate("test", "${name?api}", data("name", "value"));
      fail("Expected a RuntimeException: the ?api built-in must be disabled");
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void cannotIncludeTemplates() {
    try {
      DocUtil.processTextTemplate("test", "<#include \"/etc/passwd\">", data("name", "value"));
      fail("Expected a RuntimeException: #include must fail without a template loader");
    } catch (RuntimeException e) {
      // The include fails before any file is read; make sure that nothing of the file leaked anyway
      String stackTrace = ExceptionUtils.getStackTrace(e);
      assertFalse(stackTrace.contains("root:"),
              "The included file's content must not leak into the exception, got: " + stackTrace);
    }
  }

  @Test
  public void processErrorDoesNotLeakTemplateOrData() {
    try {
      DocUtil.processTextTemplate("test",
              "${\"freemarker.template.utility.Execute\"?new()(\"id\")}",
              data("name", "value", "secret", "super-secret-value"));
      fail("Expected a RuntimeException");
    } catch (RuntimeException e) {
      // Assert on the full stack trace including the cause chain: incident details store it,
      // so the template and the data must not leak through a cause either
      String stackTrace = ExceptionUtils.getStackTrace(e);
      assertFalse(stackTrace.contains("super-secret-value"),
              "The data model must not leak into the exception or its cause chain");
      assertFalse(stackTrace.contains("freemarker.template.utility.Execute"),
              "The template source must not leak into the exception or its cause chain");
    }
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
      assertFalse(stackTrace.contains("top-secret-payload-12345"),
              "The template source must not leak into the exception or its cause chain, got: " + stackTrace);
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
