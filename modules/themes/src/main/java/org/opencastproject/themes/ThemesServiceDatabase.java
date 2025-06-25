/*
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

package org.opencastproject.themes;

import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
   * Return themes that match the query parameters
   *
   * @param limit
   *          Maximum amount of themes to return (optional)
   * @param offset
   *          The offset to read data from (optional)
   * @param sortCriteria
   *          How the resulting list should be sorted
   * @param creatorFilter
   *          filter by creator name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   * @return A {@link List} of {@link Theme} that match the query parameters
   */
  List<Theme> findThemes(
      Optional<Integer> limit,
      Optional<Integer> offset,
      ArrayList<SortCriterion> sortCriteria,
      Optional<String> creatorFilter,
      Optional<String> textFilter
  );

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
