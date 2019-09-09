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

package org.opencastproject.systems;

/**
 * Opencast system constant definitions.
 */
public interface OpencastConstants {

  /** The property key for the current machine's hostname defined in the config.properties */
  String SERVER_URL_PROPERTY = "org.opencastproject.server.url";

  /** The property key for the streaming server defined in the config.properties */
  String STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url";

  /** The property key for the Admin UI URL defined in the organization properties */
  String ADMIN_URL_ORG_PROPERTY = "org.opencastproject.admin.ui.url";

  /** The property key for the external API URL defined in the organization properties */
  String EXTERNAL_API_URL_ORG_PROPERTY = "org.opencastproject.external.api.url";

  /** The property key for the AssetManager URL in the organization properties */
  String ASSET_MANAGER_URL_ORG_PROPERTY = "org.opencastproject.assetmanager.url";

  /** The property key for the feeds URL in the organization properties */
  String FEED_URL_ORG_PROPERTY = "org.opencastproject.feed.url";

  /** The property key for the Admin UI documentation URL in the organization properties */
  String ADMIN_DOC_URL_ORG_PROPERTY = "org.opencastproject.admin.documentation.url";

  /** The property key for the Working File Repository URL defined in the organization properties */
  String WFR_URL_ORG_PROPERTY = "org.opencastproject.file.repo.url";

  String WORKFLOW_PROPERTIES_NAMESPACE = "org.opencastproject.workflow.configuration";
}
