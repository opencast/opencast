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


package org.opencastproject.oaipmh.util;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

/**
 * Collection of utility function around OSGi.
 */
public final class OsgiUtil {

  private OsgiUtil() {
  }

  /**
   * Get a mandatory, non-blank value from the bundle context.
   *
   * @throws RuntimeException
   *          key does not exist or its value is blank
   */
  public static String getContextProperty(ComponentContext cc, String key) {
    String p = cc.getBundleContext().getProperty(key);
    if (StringUtils.isBlank(p))
      throw new RuntimeException("Please provide bundle context property " + key);
    return p;
  }

  /**
   * Get a mandatory, non-blank value from a dictionary.
   *
   * @throws ConfigurationException
   *          key does not exist or its value is blank
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

  /**
   * Get a mandatory integer from a dictionary.
   *
   * @throws ConfigurationException
   *          key does not exist or is not an integer
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
  public static void checkDictionary(Dictionary properties, ComponentContext componentContext) throws ConfigurationException {
    if (properties == null) {
      String dicName = componentContext.getProperties().get("service.pid").toString();
      throw new ConfigurationException("*", "Dictionary for " + dicName + " does not exist");
    }
  }

}
