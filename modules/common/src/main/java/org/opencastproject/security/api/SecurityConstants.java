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

  /** Header name for the desired organization */
  String ORGANIZATION_HEADER = "X-Opencast-Matterhorn-Organization";

  /** Header name for the desired user */
  String USER_HEADER = "X-Opencast-Matterhorn-User";

  /** Header name for running an operation as a desired user. Same as X-Opencast-Matterhorn-User. */
  String RUN_AS_USER_HEADER = "X-RUN-AS-USER";

  /** Header name for the desired role */
  String ROLES_HEADER = "X-Opencast-Matterhorn-Roles";

  /** Header name for running an operation with a desired role. Same as X-Opencast-Matterhorn-Roles. */
  String RUN_WITH_ROLES = "X-RUN-WITH-ROLES";

  /** Name of the Opencast admin role */
  String GLOBAL_ADMIN_ROLE = "ROLE_ADMIN";

  /** Name of the Opencast capture agent role */
  String GLOBAL_CAPTURE_AGENT_ROLE = "ROLE_CAPTURE_AGENT";

  /** Name of the Opencast global sudo role */
  String GLOBAL_SUDO_ROLE = "ROLE_SUDO";

  /** Name of the Opencast anonymous role */
  String GLOBAL_ANONYMOUS_USERNAME = "anonymous";

  /** The roles associated with the Opencast system account */
  String[] GLOBAL_SYSTEM_ROLES = new String[] { GLOBAL_ADMIN_ROLE, GLOBAL_SUDO_ROLE };

  /** The roles associated with the Opencast capture agent account */
  String[] GLOBAL_CAPTURE_AGENT_ROLES = new String[] { GLOBAL_CAPTURE_AGENT_ROLE };

  /** The administrator user configuration option */
  String GLOBAL_ADMIN_USER_PROPERTY = "org.opencastproject.security.admin.user";

}
