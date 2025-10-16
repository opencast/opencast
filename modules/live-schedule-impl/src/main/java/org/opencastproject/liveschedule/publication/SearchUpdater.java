/*
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
package org.opencastproject.liveschedule.publication;

import org.opencastproject.liveschedule.api.LiveScheduleException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchUpdater {

  private static final Logger logger = LoggerFactory.getLogger(SearchUpdater.class);

  private final SearchService searchService; // to publish/retract live media package
  private final SecurityService securityService;

  private final String systemUserName;

  public SearchUpdater(SearchService searchService, SecurityService securityService, String systemUserName) {
    this.securityService = securityService;
    this.searchService = searchService;

    this.systemUserName = systemUserName;
  }

  public void publishToSearch(MediaPackage mp) throws LiveScheduleException {
    try {
      logger.info("Publishing LIVE media package {} to search index", mp);
      searchService.addSynchronously(mp); // TODO we might need jobs here
    } catch (Exception e) {
      throw new LiveScheduleException(e);
    }
  }

  public void retractFromSearch(String mpId) {
    Organization org = securityService.getOrganization();
    User prevUser = org != null ? securityService.getUser() : null;
    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemUserName, org));
      logger.debug("Removing LIVE media package {} from the search index", mpId);
      searchService.deleteSynchronously(mpId);
    } finally {
      securityService.setUser(prevUser);
    }
  }

  /**
   * Retrieves the media package from the search index.
   *
   * @param mediaPackageId the media package id
   * @return the media package in the search index
   */
  public MediaPackage getMediaPackageFromSearch(String mediaPackageId) throws NotFoundException {
    Organization org = securityService.getOrganization();
    User prevUser = org != null ? securityService.getUser() : null;
    securityService.setUser(SecurityUtil.createSystemUser(systemUserName, org));
    try {
      return searchService.get(mediaPackageId);
    } catch (UnauthorizedException e) {
      logger.error("Received Unauthorized as System User, this shouldn't happen!");
      throw new NotFoundException(e);
    } finally {
      securityService.setUser(prevUser);
    }
  }
}
