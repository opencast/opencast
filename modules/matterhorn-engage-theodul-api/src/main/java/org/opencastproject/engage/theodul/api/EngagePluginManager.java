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

import java.util.List;

/**
 * Interface for the EngagePluginManager
 * Stores registered engage plugins
 */
public interface EngagePluginManager {

  /**
   * Returns a list of <code>EngagePluginRegistration</code> for all registered
   * Engage plugins.
   * 
   * @return List of <code>EngagePluginRegistration</code>
   */
  List<EngagePluginRegistration> getAllRegisteredPlugins();
}
