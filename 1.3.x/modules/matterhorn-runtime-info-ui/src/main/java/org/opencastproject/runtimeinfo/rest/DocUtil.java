/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.runtimeinfo.rest;


import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * This provides methods for handling documentation generation The is mainly for generating REST documentation but it
 * could be used for other things as well
 * 
 * @see DocData
 * @see org.opencastproject.runtimeinfo.rest.DocRestData
 */
public final class DocUtil {

  private static final Logger logger = LoggerFactory.getLogger(DocUtil.class);

  private static Configuration freemarkerConfig; // reusable template processor

  static {
    // initialize the freemarker template engine
    reset();
  }

  /** Disable construction of this utility class */
  private DocUtil() {
  }

  public static void reset() {
    freemarkerConfig = null;
    // static initializer
    freemarkerConfig = new Configuration();
    freemarkerConfig.setObjectWrapper(new DefaultObjectWrapper());
    freemarkerConfig.clearTemplateCache();
    logger.debug("Created new freemarker template processor for DocUtils");
  }

  /**
   * Handles the replacement of the variable strings within textual templates and also allows the setting of variables
   * for the control of logical branching within the text template as well<br/>
   * Uses and expects freemarker (http://freemarker.org/) style templates (that is using ${name} as the marker for a
   * replacement)<br/>
   * NOTE: These should be compatible with Velocity (http://velocity.apache.org/) templates if you use the formal
   * notation (formal: ${variable}, shorthand: $variable)
   * 
   * @param templateName
   *          this is the key to cache the template under
   * @param textTemplate
   *          a freemarker/velocity style text template, cannot be null or empty string
   * @param data
   *          a set of replacement values which are in the map like so:<br/>
   *          key => value (String => Object)<br/>
   *          "username" => "aaronz"<br/>
   * @return the processed template
   */
  private static String processTextTemplate(String templateName, String textTemplate, Map<String, Object> data) {
    if (freemarkerConfig == null) {
      throw new IllegalStateException("freemarkerConfig is not initialized");
    }
    if (StringUtils.isEmpty(templateName)) {
      throw new IllegalArgumentException("The templateName cannot be null or empty string, "
              + "please specify a key name to use when processing this template (can be anything moderately unique)");
    }
    if (data == null || data.size() == 0) {
      return textTemplate;
    }
    if (StringUtils.isEmpty(textTemplate)) {
      throw new IllegalArgumentException("The textTemplate cannot be null or empty string, "
              + "please pass in at least something in the template or do not call this method");
    }

    // get the template
    Template template;
    try {
      template = new Template(templateName, new StringReader(textTemplate), freemarkerConfig);
    } catch (ParseException e) {
      String msg = "Failure while parsing the Doc template (" + templateName + "), template is invalid: " + e
              + " :: template=" + textTemplate;
      logger.error(msg);
      throw new RuntimeException(msg, e);
    } catch (IOException e) {
      throw new RuntimeException("Failure while creating freemarker template", e);
    }

    // process the template
    String result;
    try {
      Writer output = new StringWriter();
      template.process(data, output);
      result = output.toString();
      logger.debug("Generated complete document ({} chars) from template ({})", result.length(), templateName);
    } catch (TemplateException e) {
      logger.error("Failed while processing the Doc template ({}): {}", templateName, e);
      result = "ERROR:: Failed while processing the template (" + templateName + "): " + e + "\n Template: "
              + textTemplate + "\n Data: " + data;
    } catch (IOException e) {
      throw new RuntimeException("Failure while sending freemarker output to stream", e);
    }

    return result;
  }

  /**
   * Use this method to generate the documentation using passed in document data
   * 
   * @param data
   *          any populated DocData object
   * @return the documentation (e.g. REST html) as a string
   * @throws IllegalArgumentException
   *           if the input data is invalid in some way
   * @see DocData
   * @see org.opencastproject.runtimeinfo.rest.DocRestData
   */
  public static String generate(DocData data) {
    String template = loadTemplate(data.getDefaultTemplatePath());
    return generate(data, template);
  }

  /**
   * Use this method to generate the documentation using passed in document data, allows the user to specify the
   * template that is used
   * 
   * @param data
   *          any populated DocData object
   * @param template
   *          any freemarker template which works with the DocData data structure
   * @return the documentation (e.g. REST html) as a string
   * @throws IllegalArgumentException
   *           if the input data is invalid in some way
   * @see DocData
   * @see org.opencastproject.runtimeinfo.rest.DocRestData
   */
  public static String generate(DocData data, String template) {
    if (template == null) {
      throw new IllegalArgumentException("template must be set");
    }
    return processTextTemplate(data.getMetaData("name"), template, data.toMap());
  }

  /**
   * Loads a template based on the given path
   * 
   * @param path
   *          the path to load the template from (uses the current classloader)
   * @return the template as a string
   */
  public static String loadTemplate(String path) {
    String textTemplate;
    InputStream in = null;
    try {
      in = DocUtil.class.getResourceAsStream(path);
      if (in == null) {
        throw new NullPointerException("No template file could be found at: " + path);
      }
      textTemplate = new String(IOUtils.toByteArray(in));
    } catch (Exception e) {
      logger.error("failed to load template file from path (" + path + "): " + e, e);
      textTemplate = null;
    } finally {
      IOUtils.closeQuietly(in);
    }
    return textTemplate;
  }

}
