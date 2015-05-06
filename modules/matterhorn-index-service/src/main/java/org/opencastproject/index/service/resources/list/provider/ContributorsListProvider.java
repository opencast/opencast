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

import static org.opencastproject.index.service.util.ListProviderUtil.splitStringList;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesIndexSchema;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ContributorsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "CONTRIBUTORS";

  public static final String DEFAULT = PROVIDER_PREFIX;

  protected static final String[] NAMES = { PROVIDER_PREFIX };

  private static final Logger logger = LoggerFactory.getLogger(ContributorsListProvider.class);

  private UserDirectoryService userDirectoryService;
  private AbstractSearchIndex searchIndex;

  protected void activate(BundleContext bundleContext) {
    logger.info("Contributors list provider activated!");
  }

  /** OSGi callback for users services. */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /** OSGi callback for the search index. */
  public void setIndex(AbstractSearchIndex index) {
    this.searchIndex = index;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
    Map<String, Object> usersList = new HashMap<String, Object>();
    int offset = 0;
    int limit = 0;
    SortedSet<String> contributorsList = new TreeSet<String>(new Comparator<String>() {
      @Override
      public int compare(String name1, String name2) {
        return name1.compareTo(name2);
      }
    });

    Iterator<User> users = userDirectoryService.findUsers("%", offset, limit);
    while (users.hasNext()) {
      User u = users.next();
      if (StringUtils.isNotBlank(u.getName()))
        contributorsList.add(u.getName());
    }

    contributorsList.addAll(splitStringList(searchIndex.getTermsForField(EventIndexSchema.CONTRIBUTOR,
            Option.some(new String[] { Event.DOCUMENT_TYPE }))));
    contributorsList.addAll(splitStringList(searchIndex.getTermsForField(EventIndexSchema.PRESENTER,
            Option.some(new String[] { Event.DOCUMENT_TYPE }))));
    contributorsList.addAll(splitStringList(searchIndex.getTermsForField(SeriesIndexSchema.CONTRIBUTORS,
            Option.some(new String[] { Series.DOCUMENT_TYPE }))));
    contributorsList.addAll(splitStringList(searchIndex.getTermsForField(SeriesIndexSchema.ORGANIZERS,
            Option.some(new String[] { Series.DOCUMENT_TYPE }))));
    contributorsList.addAll(splitStringList(searchIndex.getTermsForField(SeriesIndexSchema.PUBLISHERS,
            Option.some(new String[] { Series.DOCUMENT_TYPE }))));

    if (query != null) {
      if (query.getLimit().isSome())
        limit = query.getLimit().get();

      if (query.getOffset().isSome())
        offset = query.getOffset().get();
    }

    int i = 0;

    for (String contributor : contributorsList) {
      if (i >= offset && (limit == 0 || i < limit)) {
        usersList.put(contributor, contributor);
      }
      i++;
    }

    return usersList;
  }
}
