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
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.security.api.Organization;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A ListProvider that returns email details
 */
public class EmailListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(EmailListProvider.class);
  /** All list providers will start with this prefix. */
  public static final String PROVIDER_PREFIX = "email";
  /** The filter to match a template name exactly. */
  public static final String NAME = "name";
  /** The filter name to use if you want to match the templates with a name matching this. */
  public static final String STARTS_WITH_TEXT = "startsWith";

  /** The list of filter criteria for this provider */
  public static enum EMAIL_FILTER_LIST {
    TEMPLATE_NAMES;
  };

  /** The names of the different list available through this provider */
  private final List<String> listNames = new ArrayList<String>();

  private MailService mailService;

  protected void activate(BundleContext bundleContext) {
    // Fill the list names
    for (EMAIL_FILTER_LIST value : EMAIL_FILTER_LIST.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    logger.info("Email list provider activated!");
  }

  /** OSGi callback for the participation management database. */
  public void setMailService(MailService mailService) {
    this.mailService = mailService;
  }

  @Override
  public String[] getListNames() {
    return listNames.toArray(new String[listNames.size()]);
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> result = new HashMap<String, Object>();
    if (getListNameFromFilter(EMAIL_FILTER_LIST.TEMPLATE_NAMES).equals(listName)) {
      String nameText = null;
      if (query.hasFilter(NAME) && query.getFilter(NAME).getValue().isSome()
              && query.getFilter(NAME).getValue().get() instanceof String) {
        nameText = (String) query.getFilter(NAME).getValue().get();
      }
      String startsWithText = null;
      if (query.hasFilter(STARTS_WITH_TEXT) && query.getFilter(STARTS_WITH_TEXT).getValue().isSome()
              && query.getFilter(STARTS_WITH_TEXT).getValue().get() instanceof String) {
        startsWithText = (String) query.getFilter(STARTS_WITH_TEXT).getValue().get();
      }
      List<MessageTemplate> messageTemplateList;
      try {
        if (nameText != null) {
          logger.debug("Searching for a template with name '{}'", nameText);
          messageTemplateList = mailService.getMessageTemplateByName(nameText);
        } else if (startsWithText != null) {
          logger.debug("Searching for templates that start with '{}'", startsWithText);
          messageTemplateList = mailService.getMessageTemplatesStartingWith(startsWithText);
        } else {
          logger.debug("Getting all templates");
          messageTemplateList = mailService.getMessageTemplates();
        }
      } catch (MailServiceException e) {
        logger.error("Error retreiving message templates from mail service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving message templates from mail service", e);
      }

      for (MessageTemplate messageTemplate : messageTemplateList) {
        result.put(Long.toString(messageTemplate.getId()), messageTemplate.getName());
      }
    } else {
      logger.warn("No email list for list name {} found", listName);
      throw new ListProviderException("No email list for list name " + listName + " found!");
    }
    return ListProviderUtil.filterMap(result, query);
  }

  /**
   * Returns the list name related to the given filter
   *
   * @param filter
   *          the filter from which the list name is needed
   * @return the list name related to the given filter
   */
  public static String getListNameFromFilter(EMAIL_FILTER_LIST filter) {
    return PROVIDER_PREFIX.toUpperCase() + "_" + filter.toString().toUpperCase();
  }
}
