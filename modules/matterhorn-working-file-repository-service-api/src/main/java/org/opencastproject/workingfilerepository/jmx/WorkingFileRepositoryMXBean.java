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

package org.opencastproject.workingfilerepository.jmx;

/**
 * JMX Bean interface exposing working file repository storage information
 */
public interface WorkingFileRepositoryMXBean {

  /**
   * Gets the free working file repository space in bytes
   *
   * @return free space in bytes
   */
  long getFreeSpace();

  /**
   * Gets the used working file repository space in bytes
   *
   * @return used space in bytes
   */
  long getUsedSpace();

  /**
   * Gets the total available working file repository space in bytes
   *
   * @return total available space in bytes
   */
  long getTotalSpace();

}
