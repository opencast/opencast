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


package org.opencastproject.index.service.impl.index.series;

import org.opencastproject.matterhorn.search.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface SeriesIndexSchema extends IndexSchema {

  /** The unique identifier */
  String UID = "uid";

  String OBJECT = "object";

  /** The series title */
  String TITLE = "title";

  /** Boost values for ranking */
  double TITLE_BOOST = 6.0;

  /** The description of the series */
  String DESCRIPTION = "description";

  /** The subject of the series */
  String SUBJECT = "subject";

  /** The organization for the series */
  String ORGANIZATION = "organization";

  /** The language for the series */
  String LANGUAGE = "language";

  /** The creator of the series */
  String CREATOR = "creator";

  /** The license of the series */
  String LICENSE = "license";

  /** The access policy of the series */
  String ACCESS_POLICY = "access_policy";

  /** The key in the input documents representing the prefix to an access control entry */
  String ACL_PERMISSION_PREFIX = "acl_permission_";

  /** The name of the managed ACL used by the series (if set) */
  String MANAGED_ACL = "managed_acl";

  /** The date and time the series was created in UTC format e.g. 2011-07-16T20:39:05Z */
  String CREATED_DATE_TIME = "createdDateTime";

  /** The organizers for the series */
  String ORGANIZERS = "organizers";

  /** The contributors to the series */
  String CONTRIBUTORS = "contributors";

  /** The publisher of the series */
  String PUBLISHERS = "publisher";

  /** The series opted out status from the participation management, whether the series is opted out or not opted out */
  String OPT_OUT = "opt_out";

  /** The abstract description for a series */
  String ABSTRACT = "abstract";

  /** The rights holder for a series */
  String RIGHTS_HOLDER = "rights_holder";

  /** The theme used by the series */
  String THEME = "theme";

}
