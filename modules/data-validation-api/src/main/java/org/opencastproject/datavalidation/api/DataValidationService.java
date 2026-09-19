/*
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

package org.opencastproject.datavalidation.api;

/**
 * API for the Snapshot Validator Service
 */
public interface DataValidationService {

  /**
   * Outputs a detailed report of snapshots on asset specified by UID
   * 
   * @param uid
   *         the unique identifier of the asset
   * @return String with the report
   */
  String checkAclMatching(int offset, int limit);

  /**
   * Iterates through all assets and checks if the asset's data is corrupted by
   * checking if all snapshots contain the same number of video files.
   * 
   * @return String with the report
   */
  String checkAssetsForCorruptedData(int offset, int limit);
}
