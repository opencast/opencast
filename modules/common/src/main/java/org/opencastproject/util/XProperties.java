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

package org.opencastproject.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension to the {@code Properties} class which performs the following when you call getProperty:
 *
 * 1. Checks to see if there are any variables in the property 2. If so it replaces the variable with with the first
 * match it finds in the following order - The java properties (java -Dkey=value) - The component context properties
 * (set using setBundleContext) - The properties set in the object itself - The container's environment variables This
 * class operates identically to a standard Properties object in all other respects.
 */
public class XProperties extends Properties {

  private static final long serialVersionUID = -7497116948581078334L;

  public static final String START_REPLACEMENT = "${";

  public static final String END_REPLACEMENT = "}";

  /** Logging facility provided by log4j */
  private static final Logger log = LoggerFactory.getLogger(XProperties.class);

  /** The {@code BundleContext} for this properties object */
  private transient BundleContext context = null;

  /** The {@link Bundle} that loaded this object */
  private transient Bundle bundle = null;

  /**
   * {@inheritDoc} See the class description for more details.
   *
   * @see java.util.Properties#getProperty(java.lang.String)
   */
  @Override
  public String getProperty(String key) {
    String prop = getUninterpretedProperty(key);
    if (prop != null) {
      int start = prop.indexOf(START_REPLACEMENT);
      while (start != -1) {
        int end = prop.indexOf(END_REPLACEMENT);
        int next = prop.indexOf(START_REPLACEMENT, start + START_REPLACEMENT.length());
        if (next > 0 && next <= end) {
          log.error("Start of next subkey before end of last subkey, unable to resolve replacements for key {}!", key);
          return null;
        }
        String subkey = prop.substring(start + START_REPLACEMENT.length(), end);
        prop = findReplacement(prop, subkey);
        if (prop == null) {
          log.error("Unable to find replacement for subkey {} in key {}, returning null!", subkey, key);
          return null;
        }
        start = prop.indexOf(START_REPLACEMENT);
      }
    }
    return prop;
  }

  /**
   * Wrapper around the actual search and replace functionality. This function will value with all of the instances of
   * subkey replaced.
   *
   * @param value
   *          The original string you wish to replace.
   * @param subkey
   *          The substring you wish to replace. This must be the substring rather than the full variable - M2_REPO
   *          rather than ${M2_REPO}
   * @return The value string with all instances of subkey replaced, or null in the case of an error.
   */
  private String findReplacement(String value, String subkey) {
    if (subkey == null) {
      return null;
    }
    Pattern p = Pattern.compile(START_REPLACEMENT + subkey + END_REPLACEMENT, Pattern.LITERAL);
    String replacement = null;

    if (System.getProperty(subkey) != null) {
      replacement = System.getProperty(subkey);
    } else if (this.getProperty(subkey) != null) {
      replacement = this.getProperty(subkey);
    } else if (this.context != null && this.bundle != null && this.bundle.getState() == Bundle.ACTIVE
            && this.context.getProperty(subkey) != null) {
      replacement = this.context.getProperty(subkey);
    } else if (System.getenv(subkey) != null) {
      replacement = System.getenv(subkey);
    }

    if (replacement != null)
      return p.matcher(value).replaceAll(Matcher.quoteReplacement(replacement));
    else
      return null;
  }

  /**
   * A wrapper around the old getProperty behaviour, this method does not do any variable expansion.
   *
   * @param key
   *          The key of the property
   * @return The property exactly as it appears in the properties list without any variable expansion
   */
  public String getUninterpretedProperty(String key) {
    return super.getProperty(key);
  }

  /**
   * Merges the properties from p into this properties object
   *
   * @param p
   *          The {@code Dictionary} you wish to add to this object
   */
  public void merge(Dictionary<String, String> p) {
    Enumeration<String> keys = p.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      this.put(key, p.get(key));
    }
  }

  /**
   * Sets the {@code BundleContext} for this object. Set this to null if you wish to skip checking the context for a
   * property.
   *
   * @param ctx
   *          The {@code BundleContext} for this instance.
   */
  public void setBundleContext(BundleContext ctx) {
    context = ctx;
    if (ctx != null) {
      bundle = ctx.getBundle();
    }
  }

  /**
   * Return the current {@code BundleContext} that's in use by this object.
   *
   * @return The current {@code BundleContext}
   */
  public BundleContext getBundleContext() {
    return this.context;
  }
}
