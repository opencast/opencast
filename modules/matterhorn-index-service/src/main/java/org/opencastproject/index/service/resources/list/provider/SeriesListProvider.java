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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.StringListFilter;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesListProvider implements ResourceListProvider {
  public static final String FILTER_TEXT = "text";

  public static final String PROVIDER_PREFIX = "SERIES";

  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String CONTRIBUTORS = PROVIDER_PREFIX + ".CONTRIBUTORS";
  public static final String SUBJECT = PROVIDER_PREFIX + ".SUBJECT";
  public static final String TITLE = PROVIDER_PREFIX + ".TITLE";
  public static final String LANGUAGE = PROVIDER_PREFIX + ".LANGUAGE";
  public static final String CREATOR = PROVIDER_PREFIX + ".CREATOR";
  public static final String ORGANIZERS = PROVIDER_PREFIX + ".ORGANIZERS";
  public static final String LICENSE = PROVIDER_PREFIX + ".LICENSE";
  public static final String ACCESS_POLICY = PROVIDER_PREFIX + ".ACCESS_POLICY";
  public static final String CREATION_DATE = PROVIDER_PREFIX + ".CREATION_DATE";

  private static final String[] NAMES = { PROVIDER_PREFIX, NAME, CONTRIBUTORS, SUBJECT };

  private SeriesService seriesService;

  private static final Logger logger = LoggerFactory.getLogger(SeriesListProvider.class);

  protected void activate(BundleContext bundleContext) {
    logger.info("Series list provider activated!");
  }

  /** OSGi callback for series services. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {

    Map<String, Object> series = new HashMap<String, Object>();
    SeriesQuery q = new SeriesQuery().setCount(Integer.MAX_VALUE);

    if (query != null) {
      if (query.hasFilter(FILTER_TEXT)) {
        StringListFilter filter = (StringListFilter) query.getFilter(FILTER_TEXT);

        if (filter.getValue().isSome())
          q.setText(filter.getValue().get());
      }

      if (query.getLimit().isSome())
        q.setCount(query.getLimit().get());

      if (query.getOffset().isSome())
        q.setStartPage(query.getOffset().get());
    }

    List<DublinCoreCatalog> result = null;

    try {
      result = seriesService.getSeries(q).getCatalogList();
    } catch (SeriesException e) {
      throw new ListProviderException("Error appends on the series service: " + e);
    } catch (UnauthorizedException e) {
      throw new ListProviderException("Unauthorized access to series service: " + e);
    }

    for (DublinCoreCatalog dc : result) {
      if (CONTRIBUTORS.equals(listName)) {
        String contributor = dc.getFirst(DublinCore.PROPERTY_CONTRIBUTOR);
        if (StringUtils.isNotBlank(contributor))
          series.put(contributor, contributor);
      } else if (SUBJECT.equals(listName)) {
        String subject = dc.getFirst(DublinCore.PROPERTY_SUBJECT);
        if (StringUtils.isNotBlank(subject))
          series.put(subject, subject);
      } else {
        series.put(dc.getFirst(DublinCore.PROPERTY_IDENTIFIER), dc.getFirst(DublinCoreCatalog.PROPERTY_TITLE));
      }
    }

    return series;
  }
}
