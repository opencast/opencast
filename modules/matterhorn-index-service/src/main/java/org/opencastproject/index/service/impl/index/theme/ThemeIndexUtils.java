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


package org.opencastproject.index.service.impl.index.theme;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchMetadata;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.impl.SearchMetadataCollection;
import org.opencastproject.security.api.User;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Utility implementation to deal with the conversion of theme and its corresponding index data structures.
 */
public final class ThemeIndexUtils {

  private static final Logger logger = LoggerFactory.getLogger(ThemeIndexUtils.class);

  /**
   * This is a utility class and should therefore not be instantiated.
   */
  private ThemeIndexUtils() {
  }

  /**
   * Creates a search result item based on the data returned from the search index.
   *
   * @param metadata
   *          the search metadata
   * @return the search result item
   * @throws IOException
   *           if unmarshalling fails
   */
  public static Theme toTheme(SearchMetadataCollection metadata) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String themeXml = (String) metadataMap.get(ThemeIndexSchema.OBJECT).getValue();
    return Theme.valueOf(IOUtils.toInputStream(themeXml));
  }

  /**
   * Creates search metadata from a theme such that the theme can be stored in the search index.
   *
   * @param theme
   *          the theme
   * @return the set of metadata
   */
  public static SearchMetadataCollection toSearchMetadata(Theme theme) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(Long.toString(theme.getIdentifier()).concat(
            theme.getOrganization()), Theme.DOCUMENT_TYPE);
    // Mandatory fields
    metadata.addField(ThemeIndexSchema.UID, theme.getIdentifier(), true);
    metadata.addField(ThemeIndexSchema.ORGANIZATION, theme.getOrganization(), false);
    metadata.addField(ThemeIndexSchema.OBJECT, theme.toXML(), false);

    // Optional fields
    if (StringUtils.isNotBlank(theme.getName())) {
      metadata.addField(ThemeIndexSchema.NAME, theme.getName(), true);
    }

    if (StringUtils.isNotBlank(theme.getDescription())) {
      metadata.addField(ThemeIndexSchema.DESCRIPTION, theme.getDescription(), true);
    }

    metadata.addField(ThemeIndexSchema.DEFAULT, theme.isDefault(), false);

    if (theme.getCreationDate() != null) {
      metadata.addField(ThemeIndexSchema.CREATION_DATE, theme.getCreationDate(), true);
    }

    if (StringUtils.isNotBlank(theme.getCreator())) {
      metadata.addField(ThemeIndexSchema.CREATOR, theme.getCreator(), true);
    }

    metadata.addField(ThemeIndexSchema.BUMPER_ACTIVE, theme.isBumperActive(), false);

    if (StringUtils.isNotBlank(theme.getBumperFile())) {
      metadata.addField(ThemeIndexSchema.BUMPER_FILE, theme.getBumperFile(), false);
    }

    metadata.addField(ThemeIndexSchema.TRAILER_ACTIVE, theme.isTrailerActive(), false);

    if (StringUtils.isNotBlank(theme.getTrailerFile())) {
      metadata.addField(ThemeIndexSchema.TRAILER_FILE, theme.getTrailerFile(), false);
    }

    metadata.addField(ThemeIndexSchema.TITLE_SLIDE_ACTIVE, theme.isTrailerActive(), false);

    if (StringUtils.isNotBlank(theme.getTitleSlideMetadata())) {
      metadata.addField(ThemeIndexSchema.TITLE_SLIDE_METADATA, theme.getTitleSlideMetadata(), false);
    }

    if (StringUtils.isNotBlank(theme.getTitleSlideBackground())) {
      metadata.addField(ThemeIndexSchema.TITLE_SLIDE_BACKGROUND, theme.getTitleSlideBackground(), false);
    }

    metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_ACTIVE, theme.isLicenseSlideActive(), false);

    if (StringUtils.isNotBlank(theme.getLicenseSlideDescription())) {
      metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_DESCRIPTION, theme.getLicenseSlideDescription(), false);
    }

    if (StringUtils.isNotBlank(theme.getLicenseSlideBackground())) {
      metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_BACKGROUND, theme.getLicenseSlideBackground(), false);
    }

    metadata.addField(ThemeIndexSchema.WATERMARK_ACTIVE, theme.isWatermarkActive(), false);

    if (StringUtils.isNotBlank(theme.getWatermarkFile())) {
      metadata.addField(ThemeIndexSchema.WATERMARK_FILE, theme.getWatermarkFile(), false);
    }

    if (StringUtils.isNotBlank(theme.getWatermarkPosition())) {
      metadata.addField(ThemeIndexSchema.WATERMARK_POSITION, theme.getWatermarkPosition(), false);
    }
    return metadata;
  }

  /**
   * Loads the theme from the search index or creates a new one that can then be persisted.
   *
   * @param themeId
   *          the theme identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the {@link AdminUISearchIndex} to search in
   * @return the theme
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple themes with the same identifier are found
   */
  public static Theme getOrCreate(long themeId, String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException {
    ThemeSearchQuery query = new ThemeSearchQuery(organization, user).withIdentifier(themeId);
    SearchResult<Theme> searchResult = searchIndex.getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return new Theme(themeId, organization);
    } else if (searchResult.getDocumentCount() == 1) {
      return searchResult.getItems()[0].getSource();
    } else {
      throw new IllegalStateException("Multiple themes with identifier " + themeId + " found in search index");
    }
  }

}
