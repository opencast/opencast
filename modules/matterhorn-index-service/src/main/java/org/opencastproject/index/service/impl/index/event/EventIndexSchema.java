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

package org.opencastproject.index.service.impl.index.event;

import org.opencastproject.matterhorn.search.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface EventIndexSchema extends IndexSchema {

  /** The unique identifier */
  String UID = "uid";

  /** The organization */
  String ORGANIZATION = "organization";

  /** The recording object */
  String OBJECT = "object";

  /** The event series identifier */
  String SERIES_ID = "series_id";

  /** The event series name */
  String SERIES_NAME = "series_name";

  /** The track mimetype */
  String TRACK_MIMETYPE = "track_mimetype";

  /** The track stream resolution */
  String TRACK_STREAM_RESOLUTION = "track_stream_resolution";

  /** The track stream resolution */
  String TRACK_FLAVOR = "track_flavor";

  /** The metadata mimetype */
  String METADATA_MIMETYPE = "metadata_mimetype";

  /** The metadata flavor */
  String METADATA_FLAVOR = "metadata_flavor";

  /** The attachment flavor */
  String ATTACHMENT_FLAVOR = "attachment_flavor";

  /** The publication flavor */
  String PUBLICATION_FLAVOR = "publication_flavor";

  /** The access policy */
  String ACCESS_POLICY = "access_policy";

  /** The key in the input documents representing the prefix to an access control entry */
  String ACL_PERMISSION_PREFIX = "acl_permission_";

  /** The name of the managed ACL used by the event (if set) */
  String MANAGED_ACL = "managed_acl";

  /** The has comments field name */
  String HAS_COMMENTS = "has_comments";

  /** The has open comments field name */
  String HAS_OPEN_COMMENTS = "has_open_comments";

  /** The publications */
  String PUBLICATION = "publication";

  /*
   * Dublincore fields
   */

  /** The event description */
  String DESCRIPTION = "description";

  /** The event location */
  String LOCATION = "location";

  /** The event language */
  String LANGUAGE = "language";

  /** The recording title */
  String TITLE = "title";

  /** Boost values for ranking */
  double TITLE_BOOST = 6.0;

  /** The recording start date */
  String START_DATE = "start_date";

  /** The recording end date */
  String END_DATE = "end_date";

  /** The recording duration */
  String DURATION = "duration";

  /** The contributors */
  String CONTRIBUTOR = "contributor";

  /** The contributors */
  String PRESENTER = "presenter";

  /** The subject */
  String SUBJECT = "subject";

  /** The event source */
  String SOURCE = "source";

  /** The creation date */
  String CREATED = "created";

  /** The creator */
  String CREATOR = "creator";

  /** The license */
  String LICENSE = "license";

  /** The rights */
  String RIGHTS = "rights";

  /*
   * Workflow specific fields
   */

  /** The workflow state */
  String WORKFLOW_STATE = "workflow_state";

  /** The workflow id */
  String WORKFLOW_ID = "workflow_id";

  /** The workflow definition id */
  String WORKFLOW_DEFINITION_ID = "workflow_definition_id";

  /** The workflow scheduled date */
  String WORKFLOW_SCHEDULED_DATETIME = "workflow_scheduled_datetime";

  /*
   * Scheduler specific fields
   */

  /** The review status */
  String REVIEW_STATUS = "review_status";

  /** The review date */
  String REVIEW_DATE = "review_date";

  /** The recording status (opted-out) */
  String OPTED_OUT = "opted_out";

  /** The recording status (blacklisted) */
  String BLACKLISTED = "blacklisted";

  /** The scheduling status */
  String SCHEDULING_STATUS = "scheduling_status";

  /*
   * Archive specific fields
   */

  /** The archive version */
  String ARCHIVE_VERSION = "archive_version";

}
