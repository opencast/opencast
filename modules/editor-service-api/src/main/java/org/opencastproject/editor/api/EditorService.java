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

package org.opencastproject.editor.api;

import org.opencastproject.security.api.UnauthorizedException;

import java.io.IOException;

/**
 * Api for the Editor Service
 */
public interface EditorService {

  String JOB_TYPE = "org.opencastproject.editor";

  /** The default file name for generated Smil catalogs. */
  String TARGET_FILE_NAME = "cut.smil";

  /**
   * Provide information to edit video and audio data relevant to the given mediaPackageId
   */
  EditingData getEditData(String mediaPackageId)
          throws EditorServiceException, UnauthorizedException;

  /**
   * Create or refresh lock for the mediapackage
   * @param mediaPackageId
   * @param lockData identify the owner of the lock
   * @return lock state
   * @throws EditorServiceException if invalid mediapackge or locked by other
   */
  void  lockMediaPackage(String mediaPackageId, LockData lockData) throws EditorServiceException;

  /**
   * Remove lock for the mediapackage
   * @param mediaPackageId
   * @param lockData identify the owner of the lock
   * @return lock state
   * @throws EditorServiceException if invalid mediapackge or locked by other
   */
  void unlockMediaPackage(String mediaPackageId, LockData lockData) throws EditorServiceException;

  /**
   * Store information about edited data relevant to the given mediaPackageId
   * @param editingData
   */
  void setEditData(String mediaPackageId, EditingData editingData) throws EditorServiceException, IOException;

  /**
   * Provide all meta information about the given mediaPackageId
   */
  String getMetadata(String mediaPackageId) throws EditorServiceException;
}
