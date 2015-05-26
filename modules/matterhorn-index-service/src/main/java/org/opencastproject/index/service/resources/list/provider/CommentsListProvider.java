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

import org.opencastproject.comments.CommentException;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(CommentsListProvider.class);

  public static final String PROVIDER_PREFIX = "comments";

  /** The list of filter criteria for this provider */
  public static enum COMMENTS_FILTER_LIST {
    REASON, RESOLUTION;
  };

  /** The resolutions */
  public enum RESOLUTION {
    ALL, UNRESOLVED, RESOLVED;
  };

  /** The names of the different list available through this provider */
  private final List<String> listNames = new ArrayList<String>();

  private EventCommentService eventCommentService;

  protected void activate(BundleContext bundleContext) {
    // Fill the list names
    for (COMMENTS_FILTER_LIST value : COMMENTS_FILTER_LIST.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    logger.info("Comments list provider activated!");
  }

  /** OSGi callback for the event comment service. */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  @Override
  public String[] getListNames() {
    return listNames.toArray(new String[listNames.size()]);
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> result = new HashMap<String, Object>();

    if (COMMENTS_FILTER_LIST.REASON.equals(listName)) {
      List<String> reasons;
      try {
        reasons = eventCommentService.getReasons();
      } catch (CommentException e) {
        logger.error("Error retreiving reasons from event comment service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving reasons from event comment service", e);
      }

      for (String reason : reasons) {
        result.put(reason, reason);
      }
    } else if (COMMENTS_FILTER_LIST.RESOLUTION.equals(listName)) {
      for (RESOLUTION value : RESOLUTION.values()) {
        result.put(value.toString(), value.toString());
      }
    } else {
      logger.warn("No comments list for list name {} found", listName);
      throw new ListProviderException("No comments list for list name " + listName + " found!");
    }

    return result;
  }

  /**
   * Returns the list name related to the given filter
   *
   * @param filter
   *          the filter from which the list name is needed
   * @return the list name related to the given filter
   */
  public static String getListNameFromFilter(COMMENTS_FILTER_LIST filter) {
    return PROVIDER_PREFIX.toLowerCase() + "_" + filter.toString().toLowerCase();
  }
}
