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

package org.opencastproject.composer.impl.episode;

import java.util.Map;

/**
 * Wrapper class for episode settings.
 */
class EpisodeSettings {

  /** Name of the 'class' parameter */
  static final String PARAM_CLASS = "___class___";

  /** Name of the 'is_group' parameter */
  static final String PARAM_IS_GROUP = "is_group";

  /** Name of the 'name' parameter */
  static final String PARAM_NAME = "name";

  /** Name of the 'path' parameter */
  static final String PARAM_PATH = "path";

  /** Settings name */
  private String name = null;

  /** Path to the settings file */
  private String path = null;

  /** True if this is a settings group */
  private boolean isGroup = false;

  /**
   * Creates a new settings object from the given string array.
   *
   * @param s
   *          the settings
   */
  EpisodeSettings(Map<String, Object> s) {
    name = (String) s.get(PARAM_NAME);
    path = (String) s.get(PARAM_PATH);
    isGroup = (Boolean) s.get(PARAM_IS_GROUP);
  }

  /**
   * Returns the settings name.
   *
   * @return the name
   */
  String getName() {
    return name;
  }

  /**
   * Returns the path to the settings file, relative to the engine's root folder for settings. Usually, this is
   * <tt>/Users/Shared/Episode Engine/Settings</tt>.
   *
   * @return path to the settings file
   */
  String getPath() {
    return path;
  }

  /**
   * Returns <code>true</code> if this is a settings group.
   *
   * @return <code>true</code> if this is a settings group
   */
  boolean isGroup() {
    return isGroup;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EpisodeSettings) {
      EpisodeSettings es = (EpisodeSettings) obj;
      return path.equals(es.getPath()) && name.equals(es.getName());
    }
    return false;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name;
  }

}
