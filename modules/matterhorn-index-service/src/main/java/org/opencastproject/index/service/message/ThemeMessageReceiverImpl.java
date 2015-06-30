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

package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeIndexUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.theme.SerializableTheme;
import org.opencastproject.message.broker.api.theme.ThemeItem;
import org.opencastproject.security.api.User;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeMessageReceiverImpl extends BaseMessageReceiverImpl<ThemeItem> {

  private static final Logger logger = LoggerFactory.getLogger(ThemeMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the destination of the theme queue.
   */
  public ThemeMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(ThemeItem themeItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (themeItem.getType()) {
      case Update:
        SerializableTheme serializableTheme = themeItem.getTheme();

        logger.debug("Update the theme with id '{}', name '{}', description '{}', organization '{}'", new Object[] {
                serializableTheme.getId(), serializableTheme.getName(), serializableTheme.getDescription(),
                organization });
        try {
          Theme theme = ThemeIndexUtils.getOrCreate(serializableTheme.getId(), organization, user, getSearchIndex());
          theme.setCreationDate(serializableTheme.getCreationDate());
          theme.setDefault(serializableTheme.isDefault());
          theme.setName(serializableTheme.getName());
          theme.setDescription(serializableTheme.getDescription());
          theme.setCreator(serializableTheme.getCreator());
          theme.setBumperActive(serializableTheme.isBumperActive());
          theme.setBumperFile(serializableTheme.getBumperFile());
          theme.setTrailerActive(serializableTheme.isTrailerActive());
          theme.setTrailerFile(serializableTheme.getTrailerFile());
          theme.setTitleSlideActive(serializableTheme.isTitleSlideActive());
          theme.setTitleSlideBackground(serializableTheme.getTitleSlideBackground());
          theme.setTitleSlideMetadata(serializableTheme.getTitleSlideMetadata());
          theme.setLicenseSlideActive(serializableTheme.isLicenseSlideActive());
          theme.setLicenseSlideBackground(serializableTheme.getLicenseSlideBackground());
          theme.setLicenseSlideDescription(serializableTheme.getLicenseSlideDescription());
          theme.setWatermarkActive(serializableTheme.isWatermarkActive());
          theme.setWatermarkFile(serializableTheme.getWatermarkFile());
          theme.setWatermarkPosition(serializableTheme.getWatermarkPosition());
          getSearchIndex().addOrUpdate(theme);
        } catch (SearchIndexException e) {
          logger.error("Error storing the theme {} to the search index: {}", serializableTheme.getId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }
        break;
      case Delete:
        logger.debug("Received Delete Theme Event {}", themeItem.getThemeId());

        // Remove the theme from the search index
        try {
          getSearchIndex().delete(Theme.DOCUMENT_TYPE, Long.toString(themeItem.getThemeId()).concat(organization));
          logger.debug("Theme {} removed from {} search index", themeItem.getThemeId(), getSearchIndex().getIndexName());
        } catch (SearchIndexException e) {
          logger.error("Error deleting the group {} from the search index: {}", themeItem.getThemeId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of ThemeItem");
    }
  }
}
