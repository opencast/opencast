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

package org.opencastproject.caption.api;

/**
 * Time representation of single caption (start time, end time, duration,...).
 */
public interface Time extends Comparable<Time> {

  /**
   * Get hours of a caption.
   *
   * @return hours
   */
  int getHours();

  /**
   * Get minutes of a caption.
   *
   * @return minutes
   */
  int getMinutes();

  /**
   * Get seconds of a caption.
   *
   * @return seconds
   */
  int getSeconds();

  /**
   * Get milliseconds of a caption.
   *
   * @return milliseconds
   */
  int getMilliseconds();

}
