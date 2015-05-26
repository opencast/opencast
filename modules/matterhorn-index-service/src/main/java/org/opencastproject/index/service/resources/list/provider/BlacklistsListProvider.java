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
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.security.api.Organization;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlacklistsListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(BlacklistsListProvider.class);

  public static final String PROVIDER_PREFIX = "blacklists";

  /** The list of filter criteria for this provider */
  public static enum BLACKLISTS_FILTER_LIST {
    PERSON_NAME, ROOM_NAME, PERSON_PURPOSE, ROOM_PURPOSE;
  };

  /** The names of the different list available through this provider */
  private final List<String> listNames = new ArrayList<String>();

  private ParticipationManagementDatabase database;

  protected void activate(BundleContext bundleContext) {
    // Fill the list names
    for (BLACKLISTS_FILTER_LIST value : BLACKLISTS_FILTER_LIST.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    logger.info("Blacklists list provider activated!");
  }

  /** OSGi callback for the participation management database. */
  public void setDatabase(ParticipationManagementDatabase database) {
    this.database = database;
  }

  @Override
  public String[] getListNames() {
    return listNames.toArray(new String[listNames.size()]);
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> result = new HashMap<String, Object>();

    if (getListNameFromFilter(BLACKLISTS_FILTER_LIST.PERSON_NAME).equals(listName)) {
      List<Person> personList;
      try {
        personList = database.getPersons();
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Error retreiving persons from participation service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving persons from participation service", e);
      }

      for (Person p : personList) {
        result.put(p.getName(), p.getName());
      }
    } else if (getListNameFromFilter(BLACKLISTS_FILTER_LIST.ROOM_NAME).equals(listName)) {
      List<Room> roomList;
      try {
        roomList = database.getRooms();
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Error retreiving rooms from participation service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving rooms from participation service", e);
      }

      for (Room r : roomList) {
        result.put(Long.toString(r.getId()), r.getName());
      }
    } else if (getListNameFromFilter(BLACKLISTS_FILTER_LIST.PERSON_PURPOSE).equals(listName)) {
      List<String> purposeList;
      try {
        purposeList = database.getPurposesByType(Person.TYPE);
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Error retreiving person periods from participation service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving person periods from participation service", e);
      }

      for (String purpose : purposeList) {
        result.put(purpose, purpose);
      }
    } else if (getListNameFromFilter(BLACKLISTS_FILTER_LIST.ROOM_PURPOSE).equals(listName)) {
      List<String> purposeList;
      try {
        purposeList = database.getPurposesByType(Room.TYPE);
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Error retreiving room periods from participation service: {}", ExceptionUtils.getStackTrace(e));
        throw new ListProviderException("Error retreiving room periods from participation service", e);
      }

      for (String purpose : purposeList) {
        result.put(purpose, purpose);
      }
    } else {
      logger.warn("No blacklists list for list name {} found", listName);
      throw new ListProviderException("No blacklists list for list name " + listName + " found!");
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
  public static String getListNameFromFilter(BLACKLISTS_FILTER_LIST filter) {
    return PROVIDER_PREFIX.toLowerCase() + "_" + filter.toString().toLowerCase();
  }
}
