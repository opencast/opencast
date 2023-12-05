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


package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.FeedExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * Feed entry extension for iTunes compatibility.
 */
public class ITunesFeedEntryExtension implements FeedExtension {

  /** The dublin core module uri */
  public static final String URI = "http://www.itunes.com/dtds/podcast-1.0.dtd";

  /** The episode author */
  private String author = null;

  /** True if this episode should be blocked from distribution */
  private boolean isBlocked = false;

  /** True if this episode features explicit content */
  private boolean isExplicit = false;

  /** Episode duration */
  private long duration = 0L;

  /** Keywords for this episode */
  private List<String> keywords = null;

  /** The episode subtitle */
  private String subtitle = null;

  /** The episode summary */
  private String summary = null;

  /**
   * Creates a new iTunes feed entry extension.
   */
  public ITunesFeedEntryExtension() {
    keywords = new ArrayList<String>();
  }

  /**
   * @see org.opencastproject.feed.api.FeedExtension#getUri()
   */
  public String getUri() {
    return URI;
  }

  /**
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * @param author
   *          the author to set
   */
  public void setAuthor(String author) {
    this.author = author;
  }

  /**
   * @return the isBlocked
   */
  public boolean isBlocked() {
    return isBlocked;
  }

  /**
   * @param isBlocked
   *          the isBlocked to set
   */
  public void setBlocked(boolean isBlocked) {
    this.isBlocked = isBlocked;
  }

  /**
   * @return the isExplicit
   */
  public boolean isExplicit() {
    return isExplicit;
  }

  /**
   * @param isExplicit
   *          the isExplicit to set
   */
  public void setExplicit(boolean isExplicit) {
    this.isExplicit = isExplicit;
  }

  /**
   * @return the duration
   */
  public long getDuration() {
    return duration;
  }

  /**
   * @param duration
   *          the duration to set
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  /**
   * @return the keywords
   */
  public List<String> getKeywords() {
    return keywords;
  }

  /**
   * @param keyword
   *          the keyword to add
   */
  public void addKeyword(String keyword) {
    keywords.add(keyword);
  }

  /**
   * @param keywords
   *          the keywords to set
   */
  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  /**
   * @return the subtitle
   */
  public String getSubtitle() {
    return subtitle;
  }

  /**
   * @param subtitle
   *          the subtitle to set
   */
  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  /**
   * @return the summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * @param summary
   *          the summary to set
   */
  public void setSummary(String summary) {
    this.summary = summary;
  }

}
