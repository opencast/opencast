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

import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguagesListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "LANGUAGES";

  public static final String DEFAULT = PROVIDER_PREFIX + ".DEFAULT";

  protected static final String[] NAMES = { PROVIDER_PREFIX, DEFAULT };
  private final Map<String, Object> languages;

  private static final Logger logger = LoggerFactory.getLogger(LanguagesListProvider.class);

  protected void activate(BundleContext bundleContext) {
    logger.info("Languages list provider activated!");
  }

  public LanguagesListProvider() {
    languages = new HashMap<String, Object>();
    String[] languagesISO = Locale.getISOLanguages();

    for (String local : languagesISO) {
      Locale obj = new Locale(local);
      languages.put(obj.getLanguage(), obj.getDisplayLanguage());
    }
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
    return ListProviderUtil.filterMap(languages, query);
  }

}
