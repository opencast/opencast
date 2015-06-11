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

package org.opencastproject.staticfiles.api;

import org.opencastproject.util.NotFoundException;

import java.io.IOException;
import java.io.InputStream;

public interface StaticFileService {

  /**
   * Returns an {@link InputStream} to read a file from the static file service by its UUID.
   *
   * @param uuid
   *          The UUID of the file.
   * @return The input stream to read the file
   * @throws NotFoundException
   *           Thrown if the file cannot be found.
   * @throws IOException
   *           If the file could not be read.
   */
  InputStream getFile(String uuid) throws NotFoundException, IOException;

  /**
   * Returns the original filename of a file stored in the static file service.
   *
   * @param uuid
   *          The UUID of the file.
   * @return The filename.
   * @throws NotFoundException
   *           If the file with the given UUID was not found.
   */
  String getFileName(String uuid) throws NotFoundException;

  /**
   * Stores a file in the temporary storage section of the static file service. Make sure you call
   * {@link #persistFile(String)} if you want to persist a file durable. A file that is stored in the temporary storage
   * section may be removed at any time without notice!
   *
   * @param filename
   *          The filename and extension for the file.
   * @param inputStream
   *          The {@link InputStream} that represents the file itself.
   * @return A UUID that represents the static file.
   * @throws IOException
   *           Thrown if there is a problem storing the file.
   */
  String storeFile(String filename, InputStream inputStream) throws IOException;

  /**
   * Persists a file that was previously uploaded to the temporary storage section with
   * {@code #storeFile(String, InputStream)} for long-term usage.
   *
   * @param uuid
   *          The UUID of the file to persist.
   * @throws NotFoundException
   *           If the file could not be found.
   */
  void persistFile(String uuid) throws NotFoundException, IOException;

  /**
   * Deletes a static file.
   *
   * @param uuid
   *          The uuid of the static file.
   * @throws NotFoundException
   *           if the file cannot be found.
   */
  void deleteFile(String uuid) throws NotFoundException, IOException;

}
