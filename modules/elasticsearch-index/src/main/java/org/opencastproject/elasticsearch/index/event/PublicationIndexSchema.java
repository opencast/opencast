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

public interface PublicationIndexSchema extends IndexSchema {

  /** The publications */
  String CHANNEL = "channel";

  /** The publication mimetype */
  String MIMETYPE = "mimetype";

  /** The publication attachment(s) */
  String ATTACHMENT = "attachment";

  /** The publication catalog(s) */
  String CATALOG = "catalog";

  /** The publication track(s) */
  String TRACK = "track";

  /** The element id */
  String ELEMENT_ID = "id";

  /** The element mime type */
  String ELEMENT_MIMETYPE = "mimetype";

  /** The element type */
  String ELEMENT_TYPE = "type";

  /** The element tag */
  String ELEMENT_TAG = "tag";

  /** The element url */
  String ELEMENT_URL = "tag";

  /** The element size */
  String ELEMENT_SIZE = "size";

  /** The duration for the track element */
  String TRACK_DURATION = "duration";

  /** The transport for the track element */
  String TRACK_TRANSPORT = "transport";

}
