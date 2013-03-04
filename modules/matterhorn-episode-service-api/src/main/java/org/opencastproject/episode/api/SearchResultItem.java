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
package org.opencastproject.episode.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * An item that was found as part of a search. Typically a {@link SearchResultItem} will be included in a
 * {@link SearchResult}
 */
public interface SearchResultItem {
  /**
   * A search result item can either represent an episode ({@link SearchResultItemType#AudioVisual}) or a series (
   * {@link SearchResultItemType#Series})
   */
  public enum SearchResultItemType {
    AudioVisual, Series
  }

  /** @return the id */
  String getId();

  /** Returns the media package that was used to create the entry in the search index. */
  MediaPackage getMediaPackage();

  /** Returns the organization that produced this item. */
  String getOrganization();

  /** @return the dcExtent */
  long getDcExtent();

  /** @return the dcTitle */
  String getDcTitle();

  /** @return the dcSubject */
  String getDcSubject();

  /** @return the dcDescription */
  String getDcDescription();

  /** @return the dcCreator */
  String getDcCreator();

  /** @return the dcPublisher */
  String getDcPublisher();

  /** @return the dcContributor */
  String getDcContributor();

  /** @return the dcAbtract */
  String getDcAbstract();

  /** @return the dcCreated */
  Date getDcCreated();

  /** @return the dcAvailableFrom */
  Date getDcAvailableFrom();

  /** @return the dcAvailableTo */
  Date getDcAvailableTo();

  /** @return the dcLanguage */
  String getDcLanguage();

  /** @return the dcRightsHolder */
  String getDcRightsHolder();

  /** @return the dcSpatial */
  String getDcSpatial();

  /** @return the dcIsPartOf */
  String getDcIsPartOf();

  /** @return the dcReplaces */
  String getDcReplaces();

  /** @return the dcType */
  String getDcType();

  /** @return the dcAccessRights */
  String getDcAccessRights();

  /** @return the dcLicense */
  String getDcLicense();

  /** @return the acl serialized to XML */
  String getOcAcl();

  /** @return the mediaType */
  SearchResultItemType getType();

  /** @return the keywords */
  String[] getKeywords();

  /** @return the cover */
  String getCover();

  /** @return the modified */
  Date getTimestamp();

  /** @return the score */
  double getScore();

  /** Get the result item segment list. */
  MediaSegment[] getSegments();

  /** Return the time of deletion or none. */
  Option<Date> getOcDeleted();

  /** @return the version in the archive */
  Version getOcVersion();

  /** True, if this is currently the latest version. */
  boolean isOcLatestVersion();
}
