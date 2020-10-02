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

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.SharedHttpContext;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import javax.servlet.Servlet;

/** Contains general purpose OSGi utility functions. */
public final class OsgiUtil {

  private OsgiUtil() {
  }

  /**
   * Get a mandatory, non-blank value from the <em>bundle</em> context.
   *
   * @throws RuntimeException
   *           key does not exist or its value is blank
   */
  public static String getContextProperty(ComponentContext cc, String key) {
    String p = cc.getBundleContext().getProperty(key);
    if (StringUtils.isBlank(p))
      throw new RuntimeException("Please provide context property " + key);
    return StringUtils.trimToEmpty(p);
  }

  /**
   * Get an optional, non-blank value from the <em>bundle</em> context.
   *
   * @throws RuntimeException
   *           key does not exist or its value is blank
   */
  public static Option<String> getOptContextProperty(ComponentContext cc, String key) {
    return option(cc.getBundleContext().getProperty(key)).bind(Strings.trimToNone);
  }

  /**
   * Get a mandatory, non-blank value from the <em>component</em> context.
   *
   * @throws RuntimeException
   *           key does not exist or its value is blank
   */
  public static String getComponentContextProperty(ComponentContext cc, String key) {
    String p = (String) cc.getProperties().get(key);
    if (StringUtils.isBlank(p))
      throw new RuntimeException("Please provide context property " + key);
    return StringUtils.trimToEmpty(p);
  }

  /**
   * Get a mandatory, non-blank value from the <em>component</em> context.
   * In case the propertie is not defined (null), the default value will be returned.
   */
  public static String getComponentContextProperty(ComponentContext cc, String key, String defaultValue) {
    return Objects.toString(cc.getProperties().get(key), defaultValue);
  }

  /**
   * Get a mandatory, non-blank value from a dictionary.
   *
   * @throws ConfigurationException
   *           key does not exist or its value is blank
   */
  public static String getCfg(Dictionary d, String key) throws ConfigurationException {
    Object p = d.get(key);
    if (p == null)
      throw new ConfigurationException(key, "does not exist");
    String ps = p.toString();
    if (StringUtils.isBlank(ps))
      throw new ConfigurationException(key, "is blank");
    return StringUtils.trimToEmpty(ps);
  }

  /** Get a value from a dictionary. Return none if the key does either not exist or the value is blank. */
  public static Option<String> getOptCfg(Dictionary d, String key) {
    return option(d.get(key)).bind(Strings.asString()).bind(Strings.trimToNone);
  }

  /** Get a value from a dictionary. Return none if the key does either not exist or the value is blank. */
  public static Option<Integer> getOptCfgAsInt(Dictionary d, String key) {
    return option(d.get(key)).bind(Strings.asString()).bind(Strings.toInt);
  }

  /**
   * Filter a dictionary by key prefix. For example the following map
   * <code>{w.p.key1: "value1", w.p.key2: "value2", x: "1"}</code> filtered by <code>filterByPrefix(d, "w.p.")</code>
   * returns <code>{key1: "value1", key2: "value"}</code>.
   */
  public static Map<String, String> filterByPrefix(Dictionary d, String prefix) {
    final Map<String, String> filtered = new HashMap<>();
    final int prefixLength = prefix.length();
    final Enumeration keys = d.keys();
    while (keys.hasMoreElements()) {
      final String key = keys.nextElement().toString();
      if (key.startsWith(prefix)) {
        filtered.put(key.substring(prefixLength), d.get(key).toString());
      }
    }
    return filtered;
  }

  /**
   * Get an optional boolean from a dictionary.
   */
  public static Option<Boolean> getOptCfgAsBoolean(Dictionary d, String key) {
    return option(d.get(key)).bind(Strings.asString()).map(Strings.toBool);
  }

  /**
   * Get a mandatory integer from a dictionary.
   *
   * @throws ConfigurationException
   *           key does not exist or is not an integer
   */
  public static int getCfgAsInt(Dictionary d, String key) throws ConfigurationException {
    try {
      return Integer.parseInt(getCfg(d, key));
    } catch (NumberFormatException e) {
      throw new ConfigurationException(key, "not an integer");
    }
  }

  public static ServiceRegistration<?> registerServlet(BundleContext bundleContext, Object service, String alias) {
    Dictionary<String, String> resourceProps = new Hashtable<>();
    resourceProps.put(SharedHttpContext.CONTEXT_ID, RestConstants.HTTP_CONTEXT_ID);
    resourceProps.put(SharedHttpContext.SHARED, "true");
    resourceProps.put(SharedHttpContext.ALIAS, alias);
    return bundleContext.registerService(Servlet.class.getName(), service, resourceProps);
  }

}
