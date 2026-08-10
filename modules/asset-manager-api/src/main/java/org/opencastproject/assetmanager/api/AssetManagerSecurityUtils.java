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
package org.opencastproject.assetmanager.api;

/**
 * Shared constants and logic for how the AssetManager flattens a media package's ACL into properties for its own
 * permission checks. This is used both by the AssetManager implementation itself and by the static file
 * authorization handler, which reads the same underlying data directly rather than through the AssetManager
 * service, since it needs to run on every node while the AssetManager implementation only runs on the admin node.
 */
public final class AssetManagerSecurityUtils {

  private AssetManagerSecurityUtils() {
  }

  /** The namespace under which ACL-derived permission properties are stored. */
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";

  /** Builds the name of the property under which permission for the given role and action is stored. */
  public static String mkPropertyName(String role, String action) {
    return role + " | " + action;
  }

  /**
   * Determines whether a role should be considered when evaluating permission, based on the configured role-type
   * filters. Some role types (API, capture agent, UI) are excluded by default since they are not meant to carry
   * per-object permissions.
   */
  public static boolean isRoleAllowed(String roleName, boolean includeAPIRoles, boolean includeCARoles,
      boolean includeUIRoles) {
    return (includeAPIRoles || !roleName.startsWith("ROLE_API_"))
        && (includeCARoles || !roleName.startsWith("ROLE_CAPTURE_AGENT_"))
        && (includeUIRoles || !roleName.startsWith("ROLE_UI_"));
  }
}
