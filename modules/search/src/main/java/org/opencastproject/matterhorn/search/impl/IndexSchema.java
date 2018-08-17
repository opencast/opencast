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


package org.opencastproject.matterhorn.search.impl;

/**
 * Standard fields for an Elasticsearch index.
 */
public interface IndexSchema {

  /** Version field name */
  String VERSION = "version";

  /** Extension for fuzzy field names */
  String FUZZY_FIELDNAME_EXTENSION = "_fuzzy";

  /** Accumulative text field */
  String TEXT = "text";

  /** Accumulative text field with analysis targeted for fuzzy search */
  String TEXT_FUZZY = "text" + FUZZY_FIELDNAME_EXTENSION;

  /** The date format */
  String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr highlighting tag to use. */
  String HIGHLIGHT_MATCH = "b";

}
