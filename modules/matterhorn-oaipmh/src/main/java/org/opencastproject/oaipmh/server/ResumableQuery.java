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


package org.opencastproject.oaipmh.server;

/**
 * Stores information about a query with a paged response so that the next page can be retrieved.
 */
class ResumableQuery {
  private final String query;
  private final String metadataPrefix;
  private final int offset;
  private final int limit;

  ResumableQuery(String query, String metadataPrefix, int offset, int limit) {
    this.query = query;
    this.metadataPrefix = metadataPrefix;
    this.offset = offset;
    this.limit = limit;
  }

  String getQuery() {
    return query;
  }

  int getOffset() {
    return offset;
  }

  int getLimit() {
    return limit;
  }

  String getMetadataPrefix() {
    return metadataPrefix;
  }
}
