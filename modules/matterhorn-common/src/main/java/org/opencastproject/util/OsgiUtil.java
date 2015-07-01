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

import org.apache.commons.lang.StringUtils;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Strings;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;

/** Contains general purpose OSGi utility functions. */
public final class OsgiUtil {

  private OsgiUtil() {
  }

  /**
   * Get a mandatory, non-blank value from the <em>bundle</em> context.
   *
   * @throws RuntimeException
   *         key does not exist or its value is blank
   */
  public static String getContextProperty(ComponentContext cc, String key) {
    String p = cc.getBundleContext().getProperty(key);
    if (StringUtils.isBlank(p))
      throw new RuntimeException("Please provide context property " + key);
    return p;
  }

  /**
   * Get an optional, non-blank value from the <em>bundle</em> context.
   *
   * @throws RuntimeException
   *         key does not exist or its value is blank
   */
  public static Option<String> getOptContextProperty(ComponentContext cc, String key) {
    return option(cc.getBundleContext().getProperty(key)).bind(Strings.trimToNone);
  }

  /**
   * Get a mandatory, non-blank value from the <em>component</em> context.
   *
   * @throws RuntimeException
   *         key does not exist or its value is blank
   */
  public static String getComponentContextProperty(ComponentContext cc, String key) {
    String p = (String) cc.getProperties().get(key);
    if (StringUtils.isBlank(p))
      throw new RuntimeException("Please provide context property " + key);
    return p;
  }

  /**
   * Get a mandatory, non-blank value from a dictionary.
   *
   * @throws ConfigurationException
   *         key does not exist or its value is blank
   */
  public static String getCfg(Dictionary d, String key) throws ConfigurationException {
    Object p = d.get(key);
    if (p == null)
      throw new ConfigurationException(key, "does not exist");
    String ps = p.toString();
    if (StringUtils.isBlank(ps))
      throw new ConfigurationException(key, "is blank");
    return ps;
  }

  /** Get a value from a dictionary. Return none if the key does either not exist or the value is blank. */
  public static Option<String> getOptCfg(Dictionary d, String key) {
    return option(d.get(key)).bind(Strings.asString()).bind(Strings.trimToNone);
  }

  /**
   * Get a mandatory integer from a dictionary.
   *
   * @throws ConfigurationException
   *         key does not exist or is not an integer
   */
  public static int getCfgAsInt(Dictionary d, String key) throws ConfigurationException {
    try {
      return Integer.parseInt(getCfg(d, key));
    } catch (NumberFormatException e) {
      throw new ConfigurationException(key, "not an integer");
    }
  }

  /**
   * Check the existence of the given dictionary. Throw an exception if null.
   */
  public static void checkDictionary(Dictionary properties,
                                     ComponentContext componentContext) throws ConfigurationException {
    if (properties == null) {
      String dicName = componentContext.getProperties().get("service.pid").toString();
      throw new ConfigurationException("*", "Dictionary for " + dicName + " does not exist");
    }
  }

  /** Create a config info string suitable for logging purposes. */
  public static String showConfig(Tuple<String, ?>... cfg) {
    return "Config\n" + Collections.mkString(
            mlist(cfg).map(new Function<Tuple<String, ?>, String>() {
              @Override public String apply(Tuple<String, ?> t) {
                return t.getA() + "=" + t.getB().toString();
              }
            }).value(),
            "\n");
  }
}
