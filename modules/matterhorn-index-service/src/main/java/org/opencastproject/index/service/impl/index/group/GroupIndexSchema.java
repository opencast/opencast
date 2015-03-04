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

package org.opencastproject.index.service.impl.index.group;

import org.opencastproject.matterhorn.search.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface GroupIndexSchema extends IndexSchema {

  /** The unique identifier */
  String UID = "uid";

  /** The serialized version of this group in xml format. */
  String OBJECT = "object";

  /** Boost values for ranking */
  double TITLE_BOOST = 6.0;

  /** The name of the group */
  String NAME = "name";

  /** The description of the group */
  String DESCRIPTION = "description";

  /** The organization for the group */
  String ORGANIZATION = "organization";

  /** The roles for the group */
  String ROLES = "roles";

  /** The members of the group */
  String MEMBERS = "members";

}
