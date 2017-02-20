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

package org.opencastproject.inspection.api;

/**
 * Implementation-specific options for the media inspection service implementation
 *
 * The media inspection service API supports passing options to the media service implementation
 * in form of key/value pairs that might be implementation-specific.
 * In case an implementation cannot provide such an option, it is supposed to raise an exception.
 *
 */
public interface MediaInspectionOptions {

  /** Whether the media inspection service should determine the number of frames accurately.
   * Accurate determination of the number of frames requires the stream to be fully decoded which
   * imposes more workload than just reading the number of frames from the media header.
   * This option is supposed to be useful when the exact number of frames is needed in case of
   * media files that aren't in mint state so that the information from the media header may
   * not be exact.
   * Valid values are 'true' and 'false' */
  String OPTION_ACCURATE_FRAME_COUNT = "accurate-frame-count";

}
