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
package org.opencastproject.engage.theodul.api;

public interface EngagePlugin {

  /* Service property key; Plugin's human readable name */
  String PROPKEY_PLUGIN_NAME = "opencast.engage.plugin.name";

  /* Service property key; Description of the plugin's functionality, license etc. */
  String PROPKEY_PLUGIN_DESCRIPTION = "opencast.engage.plugin.description";

  /* Service property key; boolean wether or not the plugin provides a REST endpoint */
  String PROPKEY_PLUGIN_REST = "opencast.engage.plugin.rest";

  /* path under which a plugin's static resources can be found */
  String STATIC_RESOURCES_PATH = "static";

  /* path under which a plugin's REST endpoint can be found */
  String REST_ENDPOINT_PATH = "rest";
}
