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

package org.opencastproject.security.shibboleth;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for pre-login handling. These events occur after the Id is found, and before UserManager attempts a login
 * of the user.
 *
 * @see ShibbolethRequestHeaderAuthenticationFilter
 */
public interface ShibbolethLoginHandler {

  /** String constant identifying the login mechanism */
  String MECH_SHIBBOLETH = "shibboleth";

  /**
   * Handle a new user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  void newUserLogin(String id, HttpServletRequest request);

  /**
   * Handle an existing user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  void existingUserLogin(String id, HttpServletRequest request);

}
