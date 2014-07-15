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
package org.opencastproject.dictionary.regexp;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;
import org.opencastproject.util.ReadinessIndicator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.io.UnsupportedEncodingException;
import org.osgi.service.cm.ManagedService;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;

/**
 * This dictionary implementation is a dummy implementation which which will
 * just let the whole text pass through without any kind of filtering.
 */
public class DictionaryServiceImpl implements DictionaryService, ManagedService {

  /** The logging facility */
  private static final Logger logger =
    LoggerFactory.getLogger(DictionaryServiceImpl.class);

  public static final String PATTERN_CONFIG_KEY = "pattern";

  /* The regular expression to use for string matching */
  private String pattern = "\\w+";

  /* The compiles pattern to use for matching */
  private Pattern compilesPattern = Pattern.compile(pattern);

  public void setPattern(String p) {
    try {
      compilesPattern = Pattern.compile(p);
      pattern = p;
    } catch (RuntimeException e) {
      logger.error("Failed to compile pattern '{}'", p);
    }
  }

  public String getPattern() {
    return pattern;
  }

  /**
   * Load configuration
   */
  public synchronized void updated(Dictionary properties) {
    if (properties != null && properties.get(PATTERN_CONFIG_KEY) != null) {
      String pattern = properties.get(PATTERN_CONFIG_KEY).toString();
      /* Fix special characters */
      try {
        pattern = new String(pattern.getBytes("ISO-8859-1"), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        logger.warn("Error decoding pattern string");
      }
      logger.info("Setting pattern for regexp based DictionaryService to '{}'", pattern);
      setPattern(pattern);
    }
  }

  /**
   * OSGi callback on component activation.
   *
   * @param  ctx  the bundle context
   */
  void activate(BundleContext ctx) {
    logger.info("Activating regexp based DictionaryService");
    Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(ARTIFACT, "dictionary");
    ctx.registerService(ReadinessIndicator.class.getName(),
        new ReadinessIndicator(), properties);
  }

  /**
   * Filter the text according to the rules defined by the dictionary
   * implementation used. This implementation uses a regular expression to find
   * matching terms.
   *
   * @return filtered text
   **/
  @Override
  public Textual cleanUpText(String text) {

    logger.info("Text input: “{}”", text);
    LinkedList<String> words = new LinkedList<String>();
    Matcher matcher = compilesPattern.matcher(text);
    while (matcher.find()) {
      words.add(matcher.group());
    }
    String result = org.apache.commons.lang.StringUtils.join(words, " ");
    logger.info("Resulting text: “{}”", result);
    if ("".equals(result)) {
      return null;
    }
    return new TextualImpl(result);
  }

}
