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

package org.opencastproject.workspace.api;

import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.storage.StorageUsage;
import org.opencastproject.util.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Provides efficient access java.io.File objects from potentially remote URIs. This helper service prevents different
 * service implementations running in the same osgi container from downloading remote files multiple times.
 *
 * Additionally, when the system is configured to use shared storage, this performance gain is also achieved across
 * distributed osgi containers. The methods from WorkingFileRepository are also available as a convenience to clients.
 */
public interface Workspace extends StorageUsage {

  /**
   * Gets a locally cached {@link File} for the given URI.
   *
   * @param uri
   * @return The locally cached file
   * @throws NotFoundException
   *           if the file does not exist
   * @throws IOException
   *           if reading the file from the workspace fails
   */
  File get(URI uri) throws NotFoundException, IOException;

  /**
   * Get a locally cached {@link File} for a given URI, optionally ensuring that the file is cached in a unique path
   * so that it can safely be removed afterwards.
   *
   * @param uri
   *          URI to the resource to get
   * @param uniqueFilename
   *          If a unique path should be used
   * @return The locally cached file
   * @throws NotFoundException
   *           if the file does not exist
   * @throws IOException
   *           if reading the file from the workspace fails
   */
  File get(URI uri, boolean uniqueFilename) throws NotFoundException, IOException;

  /**
   * Get the {@link File} for the given URI directly from the working file repository.
   * If shared storage is not available, then fall back to get(uri).
   *
   * @param uri
   *        URI identifying the resource to load
   * @return The file
   * @throws NotFoundException
   *           if the file does not exist
   * @throws IOException
   *           if reading the file from the working file repository fails
   */
  InputStream read(URI uri) throws NotFoundException, IOException;


  /**
   * Gets the base URI for files stored using this service.
   *
   * @return The base URI
   */
  URI getBaseUri();

  /**
   * Store the data stream under the given media package and element IDs, specifying a filename.
   *
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @param fileName
   * @param in
   * @throws IOException
   *           if writing the data to the workspace fails
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in) throws IOException,
          IllegalArgumentException;

  /**
   * Stores the data stream in the given collection, overwriting any data with the same collection id and file name.
   *
   * @param collectionId
   *          The collection to use for storing this data
   * @param fileName
   *          the filename to use in the collection.
   * @param in
   *          the inputstream
   * @return the URI of the stored data
   * @throws IOException
   *           if writing the data to the workspace fails
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException,
          IllegalArgumentException;

  /**
   * Gets the URIs of the members of this collection
   *
   * @param collectionId
   *          the collection identifier
   * @return the URIs for each member of the collection
   * @throws NotFoundException
   *           if the collection cannot be found
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI[] getCollectionContents(String collectionId) throws NotFoundException, IllegalArgumentException;

  /**
   * Delete the file stored at the given uri.
   *
   * @param uri
   *          the uri
   * @throws NotFoundException
   *           if there was not file stored under this combination of mediapackage and element IDs.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void delete(URI uri) throws NotFoundException, IOException;

  /**
   * Delete the file stored at the given media package and element IDs.
   *
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @throws NotFoundException
   *           if there was not file stored under this combination of mediapackage and element IDs.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException;

  /**
   * Removes a file from a collection
   *
   * @param collectionId
   *          the collection identifier
   * @param fileName
   *          the filename to remove
   * @throws NotFoundException
   *           if there was no file with the provided name stored under this collection.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException;

  /**
   * Removes a file from a collection, removing the parent collection folder if empty
   *
   * @param collectionId
   *          the collection identifier
   * @param fileName
   *          the filename to remove
   * @param removeCollection
   *          remove the parent collection folder if empty
   * @throws NotFoundException
   *           if there was no file with the provided name stored under this collection.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void deleteFromCollection(String collectionId, String fileName, boolean removeCollection) throws NotFoundException, IOException;

  /**
   * Get the URL for a file stored under the given media package and element IDs. MediaPackages may reference elements
   * that are not yet stored in the working file repository, so this method will return a URI even if the file is not
   * yet stored.
   *
   * @deprecated Please use {@link #getURI(String, String, String)} instead
   * @param mediaPackageID
   *          the mediapackage identifier
   * @param mediaPackageElementID
   *          the element identifier
   * @return the URI to the file
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI getURI(String mediaPackageID, String mediaPackageElementID) throws IllegalArgumentException;

  /**
   * Get the URL for a file stored under the given media package and element IDs. MediaPackages may reference elements
   * that are not yet stored in the working file repository, so this method will return a URI even if the file is not
   * yet stored.
   *
   * @param mediaPackageID
   *          the mediapackage identifier
   * @param mediaPackageElementID
   *          the element identifier
   * @param filename
   *          the filename
   * @return the URI to the file
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI getURI(String mediaPackageID, String mediaPackageElementID, String filename) throws IllegalArgumentException;

  /**
   * Get the URL for a file stored under the given collection.
   *
   * @param collectionID
   *          the collection id
   * @param fileName
   *          the file name
   * @return the file's uri
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI getCollectionURI(String collectionID, String fileName) throws IllegalArgumentException;

  /**
   * Moves a file from a collection into a mediapackage
   *
   * @param collectionURI
   *          the uri pointing to a workspace collection
   * @param toMediaPackage
   *          The media package ID to move the file into
   * @param toMediaPackageElement
   *          the media package element ID of the file
   * @param toFileName
   *          the name of the resulting file
   * @return the URI pointing to the file's new location
   * @throws NotFoundException
   *           if the element identified by <code>collectionURI</code> cannot be found
   * @throws IOException
   *           if either the original element cannot be read or it cannot be moved to the new location
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException;

  /**
   * Copies a file from a collection into a mediapackage
   *
   * @param collectionURI
   *          The uri pointing to a workspace collection
   * @param toMediaPackage
   *          The media package ID to copy the file into
   * @param toMediaPackageElement
   *          the media package element ID of the file
   * @param toFileName
   *          the name of the resulting file
   * @return the URI pointing to the file's new location
   * @throws NotFoundException
   *           if the element identified by <code>collectionURI</code> cannot be found
   * @throws IOException
   *           if either the original element cannot be read or the copy cannot be written to the new location
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException;

  /**
   * Cleans up files not belonging to a mediapackage or a collection. If the optional maxAge parameter is set, only
   * files older than the maxAge are deleted.
   *
   * @param maxAge
   *          the maximal age in seconds of a file before deletion is performed
   */
  void cleanup(int maxAge);

  /**
   * Clean up all elements of one media package from the local workspace, not touching the working file repository.
   *
   * @param mediaPackageId
   *          Id specifying the media package to remove files for.
   */
  void cleanup(Id mediaPackageId) throws IOException;

  /**
   * Clean up elements of one media package from the local workspace, not touching the working file repository.
   *
   * @param mediaPackageId
   *          Id specifying the media package to remove files for.
   * @param filesOnly
   *          Boolean specifying whether only files or also directories (including the root directory) are deleted.
   */
  void cleanup(Id mediaPackageId, boolean filesOnly) throws IOException;

  /**
   * Returns the workspace's root directory
   *
   * @return Path to the workspace root directory
   */
  String rootDirectory();

}
