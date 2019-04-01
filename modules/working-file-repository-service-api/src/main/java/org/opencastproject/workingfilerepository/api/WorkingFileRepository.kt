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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workingfilerepository.api

import org.opencastproject.storage.StorageUsage
import org.opencastproject.util.NotFoundException

import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * The Working File Repository is a file storage service that supports the lecture capture system. It may be used by
 * other clients, but is neither intended nor required to be used by other systems.
 */
interface WorkingFileRepository : StorageUsage {

    /**
     * Gets the base URI for this service.
     *
     * @return The base URI
     */
    val baseUri: URI

    /**
     * A textual representation of available and total storage
     *
     * @return Percentage and numeric values of used storage space
     */
    val diskSpace: String

    /**
     * Store the data stream under the given media package and element IDs with filename as name of the file.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @param filename
     * the file name to use
     * @param in
     * the input stream
     * @return The URL to access this file
     * @throws IOException
     * if the input stream cannot be accessed or the element cannot be written to the repository
     * @throws IllegalArgumentException
     * if a `URI` cannot be created from the arguments
     */
    @Throws(IOException::class, IllegalArgumentException::class)
    fun put(mediaPackageID: String, mediaPackageElementID: String, filename: String, `in`: InputStream): URI

    /**
     * Stream the file stored under the given media package and element IDs.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @return the media package element contents
     * @throws IOException
     * if there is a problem reading the data
     * @throws NotFoundException
     * if the media package element can't be found
     */
    @Throws(IOException::class, NotFoundException::class)
    operator fun get(mediaPackageID: String, mediaPackageElementID: String): InputStream

    /**
     * Get the URL for a file stored under the given collection.
     *
     * @param collectionID
     * the collection identifier
     * @param fileName
     * the file name
     * @return the file's uri
     * @throws IllegalArgumentException
     * if a `URI` cannot be created from the arguments
     */
    @Throws(IllegalArgumentException::class)
    fun getCollectionURI(collectionID: String, fileName: String): URI

    /**
     * Get the URL for a file stored under the given media package and element IDs. This may be called for mediapackages,
     * elements, or files that have not yet been stored in the repository.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @return the URI to this resource
     * @throws IllegalArgumentException
     * if a `URI` cannot be created from the arguments
     */
    @Throws(IllegalArgumentException::class)
    fun getURI(mediaPackageID: String, mediaPackageElementID: String): URI

    /**
     * Get the URL for a file stored under the given media package and element IDs. This may be called for mediapackages,
     * elements, or files that have not yet been stored in the repository.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @param fileName
     * the file name
     * @return the URI to this resource
     * @throws IllegalArgumentException
     * if a `URI` cannot be created from the arguments
     */
    @Throws(IllegalArgumentException::class)
    fun getURI(mediaPackageID: String, mediaPackageElementID: String, fileName: String): URI

    /**
     * Delete the file stored at the given media package and element IDs.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @throws IOException
     * if the element cannot be deleted
     */
    @Throws(IOException::class)
    fun delete(mediaPackageID: String, mediaPackageElementID: String): Boolean

    /**
     * Gets the number of files in a collection.
     *
     * @param collectionId
     * the collection identifier
     * @return the number of files in a collection
     * @throws NotFoundException
     * if the collection does not exist
     */
    @Throws(NotFoundException::class)
    fun getCollectionSize(collectionId: String): Long

    /**
     * Puts a file into a collection, overwriting the existing file if present.
     *
     * @param collectionId
     * The collection identifier
     * @param fileName
     * The filename to use in storing the input stream
     * @param in
     * the data to store
     * @return The URI identifying the file
     * @throws IOException
     * if the input stream cannot be accessed or the file cannot be written to the repository
     */
    @Throws(IOException::class)
    fun putInCollection(collectionId: String, fileName: String, `in`: InputStream): URI

    /**
     * Gets the URIs of the members of this collection
     *
     * @param collectionId
     * the collection identifier
     * @return the URIs for each member of the collection
     * @throws NotFoundException
     * if the collectionId does not exist
     */
    @Throws(NotFoundException::class)
    fun getCollectionContents(collectionId: String): Array<URI>

    /**
     * Gets data from a collection
     *
     * @param collectionId
     * the collection identifier
     * @param fileName
     * The filename to retrieve
     * @return the data as a stream, or null if not found
     */
    @Throws(NotFoundException::class, IOException::class)
    fun getFromCollection(collectionId: String, fileName: String): InputStream

    /**
     * Removes a file from a collection
     *
     * @param collectionId
     * the collection identifier
     * @param fileName
     * the filename to remove
     * @return `true` if the file existed and was removed
     */
    @Throws(IOException::class)
    fun deleteFromCollection(collectionId: String, fileName: String): Boolean

    /**
     * Removes a file from a collection, and the parent folder if empty
     *
     * @param collectionId
     * the collection identifier
     * @param fileName
     * the filename to remove
     * @param removeCollection
     * remove the parent collection folder if empty
     * @return `true` if the file existed and was removed
     */
    @Throws(IOException::class)
    fun deleteFromCollection(collectionId: String, fileName: String, removeCollection: Boolean): Boolean

    /**
     * Moves a file from a collection into a mediapackage
     *
     * @param fromCollection
     * The collection holding the file
     * @param fromFileName
     * The filename
     * @param toMediaPackage
     * The media package ID to move the file into
     * @param toMediaPackageElement
     * the media package element ID of the file
     * @param toFileName
     * the name of the resulting file
     * @return the URI pointing to the file's new location
     */
    @Throws(NotFoundException::class, IOException::class)
    fun moveTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
               toFileName: String): URI

    /**
     * Copies a file from a collection into a mediapackage
     *
     * @param fromCollection
     * The collection holding the file
     * @param fromFileName
     * The filename
     * @param toMediaPackage
     * The media package ID to copy the file into
     * @param toMediaPackageElement
     * the media package element ID of the file
     * @param toFileName
     * the name of the resulting file
     * @return the URI pointing to the file's new location
     */
    @Throws(NotFoundException::class, IOException::class)
    fun copyTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
               toFileName: String): URI

    /**
     * Cleans up collection files older than the number of days passed.
     *
     * @param collectionId
     * the collection identifier
     * @param days
     * files older than that will be deleted
     */
    @Throws(IOException::class)
    fun cleanupOldFilesFromCollection(collectionId: String, days: Long): Boolean

    companion object {

        /** The character encoding used for URLs  */
        val CHAR_ENCODING = "UTF-8"

        /** Path prefix for working file repository uris  */
        val URI_PREFIX = "/files/"

        /** Path prefix for collection items  */
        val COLLECTION_PATH_PREFIX = "/collection/"

        /** Path prefix for mediapackage elements  */
        val MEDIAPACKAGE_PATH_PREFIX = "/mediapackage/"

        /** The job type we use to register with the remote services manager  */
        val SERVICE_TYPE = "org.opencastproject.files"
    }
}
