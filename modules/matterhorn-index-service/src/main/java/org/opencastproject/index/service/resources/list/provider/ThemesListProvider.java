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
package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ThemesListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "THEMES";
  public static final String NAME = PROVIDER_PREFIX + ".NAME";

  private static final String[] NAMES = { PROVIDER_PREFIX, NAME };

  private static final Logger logger = LoggerFactory.getLogger(ThemesListProvider.class);

  private AbstractSearchIndex searchIndex;

  private SecurityService securityService;

  protected void activate(BundleContext bundleContext) {
    logger.info("Themes list provider activated!");
  }

  /** OSGi callback for the search index. */
  public void setIndex(AbstractSearchIndex index) {
    this.searchIndex = index;
  }

  /** OSGi callback for the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> list = new HashMap<String, Object>();

    if (NAME.equals(listName)) {
      ThemeSearchQuery themeQuery = new ThemeSearchQuery(securityService.getOrganization().getId(),
              securityService.getUser());
      SearchResult<Theme> results = null;
      try {
        results = searchIndex.getByQuery(themeQuery);
      } catch (SearchIndexException e) {
        logger.error("The admin UI Search Index was not able to get the themes: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("No themes list for list name " + listName + " found!");
      }

      for (SearchResultItem<Theme> item : results.getItems()) {
        Theme theme = item.getSource();
        list.put(Long.toString(theme.getIdentifier()), theme.getName());
      }
    }

    return list;
  }

}
