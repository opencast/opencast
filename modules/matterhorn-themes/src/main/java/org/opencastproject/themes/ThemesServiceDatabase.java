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
package org.opencastproject.themes;

import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.util.NotFoundException;

/**
 * API that defines persistent storage of themes.
 */
public interface ThemesServiceDatabase {

  /**
   * Return the theme by the unique given id.
   *
   * @param id
   *          The unique id of the theme.
   * @return A {@link Theme} that matches the id.
   * @throws NotFoundException
   *           if the theme could not be found
   * @throws ThemesServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  Theme getTheme(long id) throws ThemesServiceDatabaseException, NotFoundException;

  /**
   * Crate or update a theme.
   *
   * @param theme
   *          The theme to create or update.
   * @return The updated {@link Theme}.
   * @throws ThemesServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  Theme updateTheme(Theme theme) throws ThemesServiceDatabaseException;

  /**
   * Delete a theme by using a unique id to find it.
   *
   * @param id
   *          The unique id of the theme.
   * @throws ThemesServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void deleteTheme(long id) throws ThemesServiceDatabaseException, NotFoundException;

  /**
   * @return Count the total number of themes.
   *
   * @throws ThemesServiceDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  int countThemes() throws ThemesServiceDatabaseException;

}
