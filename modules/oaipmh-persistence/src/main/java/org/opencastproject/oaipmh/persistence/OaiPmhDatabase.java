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
package org.opencastproject.oaipmh.persistence;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;

/**
 * API that defines persistent storage of OAI-PMH.
 */
public interface OaiPmhDatabase {
  /**
   * Stores or updates a mediapackage from the OAI-PMH persistence
   * 
   * @param mediaPackage
   *          the mediapackage
   * @param repository
   *          the OAI-PMH repository ID
   * @throws OaiPmhDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void store(MediaPackage mediaPackage, String repository) throws OaiPmhDatabaseException;

  /**
   * Marks a existing mediapackage as deleted
   * 
   * @param mediaPackageId
   *          the mediapackage id
   * @param repository
   *          the OAI-PMH repository ID
   * @throws NotFoundException
   *           if the mediapackage does not exist
   * @throws OaiPmhDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void delete(String mediaPackageId, String repository) throws OaiPmhDatabaseException, NotFoundException;

  /**
   * Searches mediapackages from the OAI-PMH persistence storage
   * 
   * @param q
   *          the query
   * @return a search result
   */
  SearchResult search(Query q);
}
