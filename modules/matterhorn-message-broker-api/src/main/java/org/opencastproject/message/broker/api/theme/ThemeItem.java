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

package org.opencastproject.message.broker.api.theme;

import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a ThemeService queue.
 */
public class ThemeItem implements Serializable {

  private static final long serialVersionUID = 3318918491810662792L;

  public static final String THEME_QUEUE_PREFIX = "THEME.";

  public static final String THEME_QUEUE = THEME_QUEUE_PREFIX + "QUEUE";

  private final Long id;
  private final SerializableTheme theme;

  /** The type of the message being sent. */
  private final Type type;

  public enum Type {
    Update, Delete
  };

  /**
   * @param themeId
   *          The id of the theme to update.
   * @return Builds {@link ThemeItem} for creating or updating a theme.
   */
  public static ThemeItem update(SerializableTheme theme) {
    return new ThemeItem(theme);
  }

  /**
   * @param themeId
   *          The unique id of the theme to update.
   * @return Builds {@link ThemeItem} for deleting a theme.
   */
  public static ThemeItem delete(Long themeId) {
    return new ThemeItem(themeId);
  }

  /**
   * Constructor used to create or update a theme.
   *
   * @param theme
   *          The theme details to update
   */
  public ThemeItem(SerializableTheme theme) {
    this.id = theme.getId();
    this.theme = theme;
    this.type = Type.Update;
  }

  /**
   * Constructor to build a delete theme {@link ThemeItem}.
   *
   * @param seriesId
   *          The id of the series to delete.
   */
  public ThemeItem(Long themeId) {
    this.id = themeId;
    this.theme = null;
    this.type = Type.Delete;
  }

  public Long getThemeId() {
    return id;
  }

  public SerializableTheme getTheme() {
    return theme;
  }

  public Type getType() {
    return type;
  }

}
