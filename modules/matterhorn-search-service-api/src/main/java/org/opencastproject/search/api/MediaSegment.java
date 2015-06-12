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


package org.opencastproject.search.api;

/**
 * Part of a search result that models a video segment.
 */
public interface MediaSegment {

  /**
   * Get the segment number.
   *
   * @return The number.
   */
  int getIndex();

  /**
   * Get the segment time.
   *
   * @return The time.
   */
  long getTime();

  /**
   * Get the segment duration.
   *
   * @return The duration.
   */
  long getDuration();

  /**
   * Get the image url.
   *
   * @return the image
   */
  String getImageUrl();

  /**
   * Get the segment text.
   *
   * @return The text.
   */
  String getText();

  /**
   * Get the 'segment is a hit' flag.
   *
   * @return The flag.
   */
  boolean isHit();

  /**
   * Get the segment relevance.
   *
   * @return The relevance.
   */
  int getRelevance();

  /**
   * Adds a preview url.
   *
   * @param url
   *          url to the preview image
   * @param reference
   *          reference of the preview's source track
   */
  void addPreview(String url, String reference);

}
