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

package org.opencastproject.list.common.provider;

import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(
    service = ResourceListProvider.class,
    property = {
        "service.description=Themes list provider",
        "opencast.service.type=org.opencastproject.list.provider.ThemesListProvider"
    }
)
public class ThemesListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "THEMES";
  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String DESCRIPTION = PROVIDER_PREFIX + ".DESCRIPTION";

  private static final String[] NAMES = { PROVIDER_PREFIX, NAME, DESCRIPTION };

  private static final Logger logger = LoggerFactory.getLogger(ThemesListProvider.class);

  private ThemesServiceDatabase themesServiceDatabase;

  @Activate
  protected void activate(BundleContext bundleContext) {
    logger.info("Themes list provider activated!");
  }

  /** OSGi callback for the themes service database. */
  @Reference
  public void setThemesServiceDatabase(ThemesServiceDatabase themesServiceDatabase) {
    this.themesServiceDatabase = themesServiceDatabase;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query)
          throws ListProviderException {
    Map<String, String> list = new HashMap<String, String>();

    int offset = query.getOffset().orElse(0);
    int limit = query.getLimit().orElse(Integer.MAX_VALUE - offset);
    SortCriterion sortCriterion = new SortCriterion("name", Order.Ascending);
    ArrayList<SortCriterion> sortCriteria = new ArrayList<>();
    sortCriteria.add(sortCriterion);
    List<Theme> themes = themesServiceDatabase.findThemes(
        Optional.ofNullable(offset),
        Optional.ofNullable(limit),
        sortCriteria,
        Optional.empty(),
        Optional.empty()
    );

    if (NAME.equals(listName)) {
      for (Theme theme : themes) {
        list.put(Long.toString(theme.getId().get()), theme.getName());
      }
    }
    else if (DESCRIPTION.equals(listName)) {
      for (Theme theme : themes) {
        String description = theme.getDescription();
        if (theme.getDescription() == null) {
          description = "";
        }
        list.put(Long.toString(theme.getId().get()), description);
      }
    }

    return list;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
