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

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.Date;

/**
 * Represents a query to find search results
 */
public class EpisodeQuery {
  protected boolean includeEpisode = true;
  protected boolean includeSeries = false;
  protected boolean includeLocked = false;
  protected boolean sortByCreationDate = false;
  protected boolean sortByPublicationDate = false;
  protected String id;
  protected String text;
  protected String query;
  protected int limit = -1;
  protected int offset = -1;
  protected String[] tags = null;
  protected MediaPackageElementFlavor[] flavors = null;
  protected Date deletedDate = null;

  public EpisodeQuery includeEpisodes(boolean includeEpisode) {
    this.includeEpisode = includeEpisode;
    return this;
  }

  public EpisodeQuery includeSeries(boolean includeSeries) {
    this.includeSeries = includeSeries;
    return this;
  }

  public EpisodeQuery includeLocked(boolean includeLocked) {
    this.includeLocked = includeLocked;
    return this;
  }

  public EpisodeQuery withId(String id) {
    this.id = id;
    return this;
  }

  public EpisodeQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public EpisodeQuery withOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public EpisodeQuery withQuery(String q) {
    this.query = q;
    return this;
  }

  public EpisodeQuery withText(String text) {
    this.text = text;
    return this;
  }

  public String getId() {
    return id;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }

  public String getQuery() {
    return query;
  }

  public String getText() {
    return text;
  }

  public boolean isIncludeEpisodes() {
    return includeEpisode;
  }

  public boolean isIncludeSeries() {
    return includeSeries;
  }

  public EpisodeQuery withCreationDateSort(boolean sortByDate) {
    this.sortByCreationDate = sortByDate;
    return this;
  }

  public boolean isSortByCreationDate() {
    return sortByCreationDate;
  }

  public EpisodeQuery withPublicationDateSort(boolean sortByDate) {
    this.sortByPublicationDate = sortByDate;
    return this;
  }

  public boolean isSortByPublicationDate() {
    return sortByPublicationDate;
  }

  public MediaPackageElementFlavor[] getElementFlavors() {
    return flavors;
  }

  public String[] getElementTags() {
    return tags;
  }

  public EpisodeQuery withElementFlavors(MediaPackageElementFlavor[] flavors) {
    this.flavors = flavors;
    return this;
  }

  public EpisodeQuery withElementTags(String[] tags) {
    this.tags = tags;
    return this;
  }

  public EpisodeQuery withDeletedSince(Date date) {
    this.deletedDate = date;
    return this;
  }

  public Date getDeletedDate() {
    return deletedDate;
  }
}
