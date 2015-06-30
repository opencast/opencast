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

/**
 * Common security constant definitions.
 */
public interface SecurityConstants {

  /** Header name for the digest authorization */
  String AUTHORIZATION_HEADER = "X-Opencast-Matterhorn-Authorization";

  /** Header name for the current organization */
  String ORGANIZATION_HEADER = "X-Opencast-Matterhorn-Organization";

  /** Header name for the current user */
  String USER_HEADER = "X-Opencast-Matterhorn-User";

  /** Name of the Matterhorn admin role */
  String GLOBAL_ADMIN_ROLE = "MATTERHORN_ADMINISTRATOR";

  /** Name of the Matterhorn anonymous role */
  String GLOBAL_ANONYMOUS_USERNAME = "anonymous";

}
