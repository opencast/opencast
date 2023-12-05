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


package org.opencastproject.elasticsearch.index.objects.event;

import org.opencastproject.elasticsearch.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface CommentIndexSchema extends IndexSchema {

  /** The unique identifier */
  String ID = "uid";

  /** The organization */
  String REASON = "reason";

  /** The recording object */
  String TEXT = "text";

  /** The recording object */
  String RESOLVED_STATUS = "resolvedStatus";
}
