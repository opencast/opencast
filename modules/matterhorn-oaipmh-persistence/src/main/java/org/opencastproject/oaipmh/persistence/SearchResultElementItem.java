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

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

/**
 * The OAI-PMH media package element result item
 */
public interface SearchResultElementItem {

  /**
   * @return the type of the media package element
   */
  String getType();

  /**
   * @return the flavor of the media package element
   */
  String getFlavor();

  /**
   * @return the XML serialized content of the media package element
   */
  String getXml();

  /**
   * @return true, if the content is an episode dublincore catalog, false otherwise
   */
  boolean isEpisodeDublinCore();

  /**
   * @return true, if the content is a series dublincore catalog, false otherwise
   */
  boolean isSeriesDublinCore();

  /**
   * Parse and return the content as dublincore catalog.
   * This method can be called only if {@link #isEpisodeDublinCore()} or {@link #isSeriesDublinCore()} returns true
   *
   * @return the serialized content as dublincore catalog
   * @throws OaiPmhDatabaseException if parsing dublincore catalog fail
   */
  DublinCoreCatalog asDublinCore() throws OaiPmhDatabaseException;
}
