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

package org.opencastproject.staticfiles.jmx;

/**
 * JMX Bean interface exposing upload statistics.
 */
public interface UploadStatisticsMXBean {

  /**
   * Gets the number of successful upload operations
   *
   * @return the number of upload operations
   */
  int getSuccessfulUploadOperations();

  /**
   * Gets the number of failed upload operations
   *
   * @return the number of upload operations
   */
  int getFailedUploadOperations();

  /**
   * Gets the total number of uploaded bytes
   *
   * @return the number of bytes
   */
  long getTotalBytes();

  /**
   * Gets the total number of uploaded bytes in the last minute
   *
   * @return the number of bytes
   */
  long getBytesInLastMinute();

  /**
   * Gets the total number of uploaded bytes in the last five minutes
   *
   * @return the number of bytes
   */
  long getBytesInLastFiveMinutes();

  /**
   * Gets the total number of uploaded bytes in the last fifteen minutes
   *
   * @return the number of bytes
   */
  long getBytesInLastFifteenMinutes();

}
