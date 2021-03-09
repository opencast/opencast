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


package org.opencastproject.elasticsearch.index.event;

import org.opencastproject.elasticsearch.impl.IndexSchema;

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

  /** The event has open comment that it needs cutting */
  String NEEDS_CUTTING = "needs_cutting";

  /** The publications */
  String PUBLICATION = "publication";

  /** The event status */
  String EVENT_STATUS = "event_status";

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

  /** The publisher */
  String PUBLISHER = "publisher";

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

  /*
   * Scheduler specific fields
   */

  /** The recording status */
  String RECORDING_STATUS = "recording_status";

  /*
   * Technical Metadata
   */
  /** Id of the agent that will record. */
  String AGENT_ID = "agent_id";

  /** The ending time of the recording. */
  String TECHNICAL_END = "technical_end";

  /** The start time of the recording. */
  String TECHNICAL_START = "technical_start";

  /** The technical presenters of the recording. */
  String TECHNICAL_PRESENTERS = "technical_presenters";

  /*
   * Archive specific fields
   */

  /** The archive version */
  String ARCHIVE_VERSION = "archive_version";

}
