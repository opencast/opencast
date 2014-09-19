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

/**
 * This object models a preview url for time segments, which consist of a url and a url type.
 */
public interface MediaSegmentPreview {

  /**
   * Returns the url to the preview image.
   *
   * @return the url
   */
  String getUrl();

  /**
   * Returns the reference to the source track that was used to produce the segment.
   *
   * @return the reference to the source track
   */
  String getReference();

}
