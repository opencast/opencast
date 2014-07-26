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
package org.opencastproject.workflow.api;

import java.util.Set;

/**
 * A configurable entity.
 *
 */
public interface Configurable {
  /**
   * Returns the value of property <code>name</code> or <code>null</code> if no such property has been set.
   *
   * @param key
   *          the configuration key
   * @return the configuration value
   */
  String getConfiguration(String key);

  /**
   * Sets the configuration with name <code>key</code> to value <code>value</code>, or adds it if it doesn't already
   * exist.
   *
   * @param key
   *          the configuration key
   * @param value
   *          the configuration value
   */
  void setConfiguration(String key, String value);

  /**
   * Gets the configuration keys that are currently set for this configurable entity.
   *
   * @return the configuration keys
   */
  Set<String> getConfigurationKeys();

  /**
   * Removes the <code>key</code> configuration.
   *
   * @param key
   *          the configuration key
   */
  void removeConfiguration(String key);

}
