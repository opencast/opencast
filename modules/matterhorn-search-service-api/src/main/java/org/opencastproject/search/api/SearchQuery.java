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
package org.opencastproject.search.api;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.Date;

/**
 * Represents a query to find search results
 */
public class SearchQuery {
  protected boolean includeEpisode = true;
  protected boolean includeSeries = false;
  protected boolean sortByCreationDate = false;
  protected boolean sortByPublicationDate = false;
  protected String episodeId;
  protected String seriesId;
  protected String text;
  protected String query;
  protected int limit = 0;
  protected int offset = 0;
  protected String[] tags = null;
  protected MediaPackageElementFlavor[] flavors = null;
  protected Date deletedDate = null;
  protected String partOf;
  protected String id;

  public SearchQuery id(String id) {
    this.id = id;
    return this;
  }

  public String id() {
    return id;
  }

  public SearchQuery partOf(String partOf) {
    this.partOf = partOf;
    return this;
  }

  public String partOf() {
    return partOf;
  }

  public SearchQuery includeEpisodes(boolean includeEpisode) {
    this.includeEpisode = includeEpisode;
    return this;
  }

  public SearchQuery includeSeries(boolean includeSeries) {
    this.includeSeries = includeSeries;
    return this;
  }

  public SearchQuery episodeId(String id) {
    this.episodeId = id;
    return this;
  }

  public SearchQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public SearchQuery withOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public SearchQuery withQuery(String q) {
    this.query = q;
    return this;
  }

  public SearchQuery withText(String text) {
    this.text = text;
    return this;
  }

  public SearchQuery seriesId(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  public String episodeId() {
    return episodeId;
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

  public String seriesId() {
    return seriesId;
  }

  public boolean isIncludeEpisodes() {
    return includeEpisode;
  }

  public boolean isIncludeSeries() {
    return includeSeries;
  }

  public SearchQuery withCreationDateSort(boolean sortByDate) {
    this.sortByCreationDate = sortByDate;
    return this;
  }

  public boolean isSortByCreationDate() {
    return sortByCreationDate;
  }

  public SearchQuery withPublicationDateSort(boolean sortByDate) {
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

  public SearchQuery withElementFlavors(MediaPackageElementFlavor[] flavors) {
    this.flavors = flavors;
    return this;
  }

  public SearchQuery withElementTags(String[] tags) {
    this.tags = tags;
    return this;
  }

  public SearchQuery withDeletedSince(Date date) {
    this.deletedDate = date;
    return this;
  }

  public Date getDeletedDate() {
    return deletedDate;
  }
}
