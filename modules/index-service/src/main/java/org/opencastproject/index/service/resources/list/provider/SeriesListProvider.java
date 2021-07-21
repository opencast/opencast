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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesIndexSchema;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.resources.list.query.SeriesListQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesListProvider implements ResourceListProvider {
  private static final Logger logger = LoggerFactory.getLogger(SeriesListProvider.class);

  public static final String PROVIDER_PREFIX = "SERIES";

  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String CONTRIBUTORS = PROVIDER_PREFIX + ".CONTRIBUTORS";
  public static final String SUBJECT = PROVIDER_PREFIX + ".SUBJECT";
  public static final String TITLE = PROVIDER_PREFIX + ".TITLE";
  public static final String TITLE_EXTENDED = PROVIDER_PREFIX + ".TITLE_EXTENDED";
  public static final String LANGUAGE = PROVIDER_PREFIX + ".LANGUAGE";
  public static final String CREATOR = PROVIDER_PREFIX + ".CREATOR";
  public static final String ORGANIZERS = PROVIDER_PREFIX + ".ORGANIZERS";
  public static final String LICENSE = PROVIDER_PREFIX + ".LICENSE";
  public static final String ACCESS_POLICY = PROVIDER_PREFIX + ".ACCESS_POLICY";
  public static final String CREATION_DATE = PROVIDER_PREFIX + ".CREATION_DATE";

  private static final String[] NAMES = { PROVIDER_PREFIX, CONTRIBUTORS, ORGANIZERS, TITLE_EXTENDED };

  /** The search index. */
  private AbstractSearchIndex searchIndex;

  /** The security service. */
  private SecurityService securityService;

  protected void activate(BundleContext bundleContext) {
    logger.info("Series list provider activated!");
  }

  /** OSGi callback for series services. */
  public void setSearchIndex(AbstractSearchIndex searchIndex) {
    this.searchIndex = searchIndex;
  }

  /** OSGi callback for security service */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query)
          throws ListProviderException {
    SeriesSearchQuery seriesQuery = toSearchQuery(query);
    Map<String, String> result = new HashMap<>();
    if (TITLE.equals(listName)) {
      seriesQuery.sortByTitle(SortCriterion.Order.Ascending);
      for (String title : searchIndex.getTermsForField(SeriesIndexSchema.TITLE,
          Option.some(new String[] { Series.DOCUMENT_TYPE }))) {
        result.put(title, title);
      }
    } else if (CONTRIBUTORS.equals(listName)) {
      seriesQuery.sortByContributors(SortCriterion.Order.Ascending);
      for (String contributor : searchIndex.getTermsForField(SeriesIndexSchema.CONTRIBUTORS,
          Option.some(new String[] { Series.DOCUMENT_TYPE }))) {
        result.put(contributor, contributor);
      }
    } else if (ORGANIZERS.equals(listName)) {
      seriesQuery.sortByOrganizers(SortCriterion.Order.Ascending);
      for (String organizer : searchIndex.getTermsForField(SeriesIndexSchema.ORGANIZERS,
          Option.some(new String[] { Series.DOCUMENT_TYPE }))) {
        result.put(organizer, organizer);
      }
    } else {
      try {
        seriesQuery.sortByTitle(SortCriterion.Order.Ascending);
        seriesQuery.sortByCreatedDateTime(SortCriterion.Order.Descending);
        seriesQuery.sortByOrganizers(SortCriterion.Order.Ascending);
        SearchResult searchResult = searchIndex.getByQuery(seriesQuery);
        Calendar calendar = Calendar.getInstance();
        for (SearchResultItem<Series> item : searchResult.getItems()) {
          Series s = item.getSource();
          if (TITLE_EXTENDED.equals(listName)) {
            Date created = s.getCreatedDateTime();
            List<String> organizers = s.getOrganizers();
            StringBuilder sb = new StringBuilder(s.getTitle());
            if (created != null || (organizers != null && !organizers.isEmpty())) {
              List<String> extendedTitleData = new ArrayList<>();
              if (created != null) {
                calendar.setTime(created);
                extendedTitleData.add(Integer.toString(calendar.get(Calendar.YEAR)));
              }
              if (organizers != null && !organizers.isEmpty())
                extendedTitleData.addAll(organizers);
              sb.append(" (").append(StringUtils.join(extendedTitleData, ", ")).append(")");
            }
            result.put(s.getIdentifier(), sb.toString());
          } else {
            result.put(s.getIdentifier(), s.getTitle());
          }
        }
      } catch (SearchIndexException e) {
        logger.warn("Unable to query series.", e);
      }
    }
    return result;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }

  /**
   * Creates a series search query from resource list query.
   *
   * @param query a resource list query
   * @return a series search query
   */
  protected SeriesSearchQuery toSearchQuery(ResourceListQuery query) {
    SeriesSearchQuery seriesQuery = new SeriesSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
    if (query.getLimit().isSome()) {
      seriesQuery.withLimit(query.getLimit().get());
    }
    if (query.getOffset().isSome()) {
      seriesQuery.withOffset(query.getOffset().get());
    }
    if (query instanceof SeriesListQuery) {
      if (((SeriesListQuery) query).getReadPermission().isSome()
          || ((SeriesListQuery) query).getWritePermission().isSome()) {
        seriesQuery.withoutActions();
        if (((SeriesListQuery) query).getReadPermission().getOrElse(true)) {
          seriesQuery.withAction(Permissions.Action.READ);
        }
        if (((SeriesListQuery) query).getWritePermission().getOrElse(false)) {
          seriesQuery.withAction(Permissions.Action.WRITE);
        }
      }
      for (ResourceListFilter filter : query.getFilters()) {
        if (filter.getValue().isNone()) {
          continue;
        } else if (SeriesListQuery.FILTER_CREATIONDATE_NAME.equals(filter.getName())) {
          Tuple<Date, Date> creationDate = (Tuple<Date, Date>) filter.getValue().get();
          if (creationDate.getA() != null) {
            seriesQuery.withCreatedFrom(creationDate.getA());
          }
          if (creationDate.getB() != null) {
            seriesQuery.withCreatedTo(creationDate.getB());
          }
        } else if (SeriesListQuery.FILTER_CREATOR_NAME.equals(filter.getName())) {
          seriesQuery.withCreator((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_CONTRIBUTORS_NAME.equals(filter.getName())) {
          seriesQuery.withContributor((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_LANGUAGE_NAME.equals(filter.getName())) {
          seriesQuery.withLanguage((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_LICENSE_NAME.equals(filter.getName())) {
          seriesQuery.withLicense((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_ORGANIZERS_NAME.equals(filter.getName())) {
          seriesQuery.withOrganizer((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_SUBJECT_NAME.equals(filter.getName())) {
          seriesQuery.withSubject((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_TEXT_NAME.equals(filter.getName())) {
          seriesQuery.withText((String)filter.getValue().get());
        } else if (SeriesListQuery.FILTER_TITLE_NAME.equals(filter.getName())) {
          seriesQuery.withTitle((String)filter.getValue().get());
        }
      }
    }
    return seriesQuery;
  }
}
