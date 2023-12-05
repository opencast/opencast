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

package org.opencastproject.security.api;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface representing an authorization check for a given list of URL patterns.
 * These rules are to be applied when delivering static content.
 */
public interface StaticFileAuthorization {

  /**
   * Get the list of URL patterns the service is responsible for.
   *
   * @return List of pattern matchers
   */
  List<Pattern> getProtectedUrlPattern();

  /**
   * Check a given path for access.
   *
   * @param path Path to check
   * @return if access is allowed
   */
  boolean verifyUrlAccess(String path);
}
