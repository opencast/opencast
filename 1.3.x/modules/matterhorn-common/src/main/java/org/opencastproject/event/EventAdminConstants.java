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
package org.opencastproject.event;

/**
 * Defines constants for common event admin topics.
 */
public interface EventAdminConstants {

  /** An object identifier */
  String ID = EventAdminConstants.class.getName() + ".ID";

  /** The event property that specified a message payload */
  String PAYLOAD = EventAdminConstants.class.getName() + "payload";

  /** The event admin topic for series dublin core updates */
  String SERIES_TOPIC = "org/opencastproject/series/dc";

  /** The event admin topic for series ACL updates */
  String SERIES_ACL_TOPIC = "org/opencastproject/series/acl";

}
