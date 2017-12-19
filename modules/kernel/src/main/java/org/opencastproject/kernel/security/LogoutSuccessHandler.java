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

package org.opencastproject.kernel.security;

import org.opencastproject.security.api.UserDirectoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sends user directory provider to invalidate user cache and logs out.
 */
public class LogoutSuccessHandler extends
org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LogoutSuccessHandler.class);

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
          throws IOException, ServletException {
    if (authentication != null) {
      userDirectoryService.invalidate(authentication.getName());
      logger.trace("Logging out user {} ...", authentication.getName());
    } else {
      logger.trace("Logout after session expiration");
    }
    super.onLogoutSuccess(request, response, authentication);
  }

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

}
