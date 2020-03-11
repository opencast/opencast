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

import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventCommentsListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(EventCommentsListProvider.class);

  public static final String PROVIDER_PREFIX = "comments";

  /** The list of filter criteria for this provider */
  public enum CommentsFilterList {
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
    for (CommentsFilterList value : CommentsFilterList.values()) {
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
  public Map<String, String> getList(String listName, ResourceListQuery query)
          throws ListProviderException {
    Map<String, String> result = new HashMap<String, String>();

    if (CommentsFilterList.REASON.equals(listName)) {
      List<String> reasons;
      try {
        reasons = eventCommentService.getReasons();
      } catch (EventCommentException e) {
        logger.error("Error retreiving reasons from event comment service", e);
        throw new ListProviderException("Error retreiving reasons from event comment service", e);
      }

      for (String reason : reasons) {
        result.put(reason, reason);
      }
    } else if (CommentsFilterList.RESOLUTION.equals(listName)) {
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
  public static String getListNameFromFilter(CommentsFilterList filter) {
    return PROVIDER_PREFIX.toLowerCase() + "_" + filter.toString().toLowerCase();
  }

  @Override
  public boolean isTranslatable(String listName) {
    return true;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
