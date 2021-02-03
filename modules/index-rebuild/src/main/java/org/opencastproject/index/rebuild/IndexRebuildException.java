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

package org.opencastproject.index.rebuild;

import org.opencastproject.security.api.Organization;

/**
 * An exception which indicates an error when rebuilding an ElasticSearch index.
 */
public class IndexRebuildException extends Exception {

  private static final long serialVersionUID = -3312895786363366343L;

  /**
   * Constructor without Cause.
   *
   * @param message
   *           The error message.
   *
   */
  public IndexRebuildException(String message) {
    super(message);
  }

  /**
   * Full fledged constructor.
   *
   * @param message
   *           The error message.
   * @param cause
   *           The cause.
   */
  public IndexRebuildException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor that builds an error message.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param service
   *           The service that's adding to the index.
   * @param cause
   *           The cause.
   */
  public IndexRebuildException(String indexName, IndexRebuildService.Service service, Throwable cause) {
    super(String.format("Error updating index %s for service %s", indexName, service.name()), cause);
  }

  /**
   * Constructor that builds an error message.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param service
   *           The service that's adding to the index.
   * @param org
   *           The organization.
   * @param cause
   *           The cause.
   */
  public IndexRebuildException(String indexName, IndexRebuildService.Service service, Organization org,
          Throwable cause) {
    super(String.format("Error updating index %s for service %s and organization %s", indexName, service.name(),
            org.getId()), cause);
  }
}
