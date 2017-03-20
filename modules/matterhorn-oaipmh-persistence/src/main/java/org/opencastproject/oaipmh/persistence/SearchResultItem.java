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
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * An item that was found as part of a search. Typically a {@link SearchResultItem} will be included in a
 * {@link SearchResult}
 */
public interface SearchResultItem {

  /** @return the media package id */
  String getId();

  /** @return the media package */
  MediaPackage getMediaPackage();

  /** @return the serialized media package */
  String getMediaPackageXml();

  /** @return the organization id */
  String getOrganization();

  /** @return the repository id */
  String getRepository();

  /** @return the series dublincore catalog */
  Option<DublinCoreCatalog> getSeriesDublinCore();

  /** @return the serialized series dublincore catalog */
  Option<String> getSeriesDublinCoreXml();

  /** @return the serialized ACL XML of the series */
  Option<String> getSeriesAclXml();

  /** @return the episode dublincore catalog */
  Option<DublinCoreCatalog> getEpisodeDublinCore();

  /** @return the serialized episode dublincore catalog */
  Option<String> getEpisodeDublinCoreXml();

  /** @return the last modification date */
  Date getModificationDate();

  /** Return <code>true</code> if the mediapackage has been deleted. */
  boolean isDeleted();

}
