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


package org.opencastproject.elasticsearch.index.group;

import org.opencastproject.elasticsearch.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface GroupIndexSchema extends IndexSchema {

  /** The unique identifier */
  String UID = "uid";

  /** The serialized version of this group in xml format. */
  String OBJECT = "object";

  /** The name of the group */
  String NAME = "name";

  /** The description of the group */
  String DESCRIPTION = "description";

  /** The organization for the group */
  String ORGANIZATION = "organization";

  /** The role of the group */
  String ROLE = "role";

  /** The roles for the group */
  String ROLES = "roles";

  /** The members of the group */
  String MEMBERS = "members";

}
