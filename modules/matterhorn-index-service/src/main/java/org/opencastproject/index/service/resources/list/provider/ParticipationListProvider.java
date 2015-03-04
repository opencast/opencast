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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipationListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(ParticipationListProvider.class);

  public static final String PROVIDER_PREFIX = "participation";

  /** The list of filter criteria for this provider */
  public static enum PARTICIPATION_FILTER_LIST {
    RECORDING_STATUS;
  };

  /** The names of the different list available through this provider */
  private final List<String> listNames = new ArrayList<String>();

  protected void activate(BundleContext bundleContext) {
    // Fill the list names
    for (PARTICIPATION_FILTER_LIST value : PARTICIPATION_FILTER_LIST.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    logger.info("Participation list provider activated!");
  }

  @Override
  public String[] getListNames() {
    return listNames.toArray(new String[listNames.size()]);
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> result = new HashMap<String, Object>();
    if (PARTICIPATION_FILTER_LIST.RECORDING_STATUS.equals(listName)) {
      result.put("READY", "READY");
      result.put("OPTED_OUT", "OPTED_OUT");
      result.put("BLACKLISTED", "BLACKLISTED");
    } else {
      logger.warn("No participation list for list name {} found", listName);
      throw new ListProviderException("No participation list for list name " + listName + " found!");
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
  public static String getListNameFromFilter(PARTICIPATION_FILTER_LIST filter) {
    return PROVIDER_PREFIX.toLowerCase() + "_" + filter.toString().toLowerCase();
  }
}
