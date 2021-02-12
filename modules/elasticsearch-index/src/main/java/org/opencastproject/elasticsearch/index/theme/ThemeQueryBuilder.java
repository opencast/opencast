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

package org.opencastproject.elasticsearch.index.theme;

import org.opencastproject.elasticsearch.api.SearchTerms;
import org.opencastproject.elasticsearch.api.SearchTerms.Quantifier;
import org.opencastproject.elasticsearch.impl.AbstractElasticsearchQueryBuilder;
import org.opencastproject.elasticsearch.impl.IndexSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Opencast {@link ThemeSearchQuery} implementation of the Elasticsearch query builder.
 */
public class ThemeQueryBuilder extends AbstractElasticsearchQueryBuilder<ThemeSearchQuery> {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ThemeQueryBuilder.class);

  /**
   * Creates a new search query based on the theme query.
   *
   * @param query
   *          the theme query
   */
  public ThemeQueryBuilder(ThemeSearchQuery query) {
    super(query);
  }

  @Override
  public void buildQuery(ThemeSearchQuery query) {
    // Organization
    if (query.getOrganization() == null) {
      throw new IllegalStateException("No organization set on the theme search query!");
    }

    and(ThemeIndexSchema.ORGANIZATION, query.getOrganization());

    // theme identifier
    if (query.getIdentifiers().length > 0) {
      and(ThemeIndexSchema.ID, query.getIdentifiers());
    }

    if (query.getCreator() != null) {
      and(ThemeIndexSchema.CREATOR, query.getCreator());
    }

    if (query.getCreatedFrom() != null && query.getCreatedTo() != null) {
      and(ThemeIndexSchema.CREATION_DATE, query.getCreatedFrom(), query.getCreatedTo());
    }

    if (query.getIsDefault() != null) {
      and(ThemeIndexSchema.DEFAULT, query.getIsDefault());
    }

    if (query.getDescription() != null) {
      and(ThemeIndexSchema.DESCRIPTION, query.getDescription());
    }

    if (query.getName() != null) {
      and(ThemeIndexSchema.NAME, query.getName());
    }

    if (query.getBumperActive() != null) {
      and(ThemeIndexSchema.BUMPER_ACTIVE, query.getBumperActive());
    }

    if (query.getBumperFile() != null) {
      and(ThemeIndexSchema.BUMPER_FILE, query.getBumperFile());
    }

    if (query.getLicenseSlideActive() != null) {
      and(ThemeIndexSchema.LICENSE_SLIDE_ACTIVE, query.getLicenseSlideActive());
    }

    if (query.getLicenseSlideBackground() != null) {
      and(ThemeIndexSchema.LICENSE_SLIDE_BACKGROUND, query.getLicenseSlideBackground());
    }

    if (query.getLicenseSlideDescription() != null) {
      and(ThemeIndexSchema.LICENSE_SLIDE_DESCRIPTION, query.getLicenseSlideDescription());
    }

    if (query.getTrailerActive() != null) {
      and(ThemeIndexSchema.TRAILER_ACTIVE, query.getTrailerActive());
    }

    if (query.getTrailerFile() != null) {
      and(ThemeIndexSchema.TRAILER_FILE, query.getTrailerFile());
    }

    if (query.getTitleSlideActive() != null) {
      and(ThemeIndexSchema.TITLE_SLIDE_ACTIVE, query.getTitleSlideActive());
    }

    if (query.getTitleSlideBackground() != null) {
      and(ThemeIndexSchema.TITLE_SLIDE_BACKGROUND, query.getTitleSlideBackground());
    }

    if (query.getTitleSlideMetadata() != null) {
      and(ThemeIndexSchema.TITLE_SLIDE_METADATA, query.getTitleSlideMetadata());
    }

    if (query.getWatermarkActive() != null) {
      and(ThemeIndexSchema.WATERMARK_ACTIVE, query.getWatermarkActive());
    }

    if (query.getWatermarkFile() != null) {
      and(ThemeIndexSchema.WATERMARK_FILE, query.getWatermarkFile());
    }

    if (query.getWatermarkPosition() != null) {
      and(ThemeIndexSchema.WATERMARK_POSITION, query.getWatermarkPosition());
    }

    // Text
    if (query.getTerms() != null) {
      for (SearchTerms<String> terms : query.getTerms()) {
        StringBuilder queryText = new StringBuilder();
        for (String term : terms.getTerms()) {
          if (queryText.length() > 0) {
            queryText.append(" ");
          }
          queryText.append(term);
        }
        if (query.isFuzzySearch()) {
          fuzzyText = queryText.toString();
        } else {
          this.text = queryText.toString();
        }
        if (Quantifier.All.equals(terms.getQuantifier())) {
          if (groups == null) {
            groups = new ArrayList<ValueGroup>();
          }
          if (query.isFuzzySearch()) {
            logger.warn("All quantifier not supported in conjunction with wildcard text");
          }
          groups.add(new ValueGroup(IndexSchema.TEXT, (Object[]) terms.getTerms().toArray(new String[terms.size()])));
        }
      }
    }

    // Filter query
    if (query.getFilter() != null) {
      this.filter = query.getFilter();
    }

  }

}
