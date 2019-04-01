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

package org.opencastproject.workingfilerepository.remote

import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_OK

import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ContentBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.util.EntityUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A remote service proxy for a working file repository
 */
class WorkingFileRepositoryRemoteImpl : RemoteBase(SERVICE_TYPE), WorkingFileRepository {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getDiskSpace
     */
    override val diskSpace: String
        get() = storageReport["summary"] as String

    protected val storageReport: JSONObject
        get() {
            val url = UrlSupport.concat(*arrayOf("storage"))
            val get = HttpGet(url)
            val response = getResponse(get)
            try {
                if (response != null) {
                    val json = EntityUtils.toString(response.entity)
                    return JSONValue.parse(json) as JSONObject
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                closeConnection(response)
            }
            throw RuntimeException("Error getting storage report")
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getTotalSpace
     */
    override val totalSpace: Option<Long>
        get() = Option.some(storageReport["size"] as Long)

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getUsableSpace
     */
    override val usableSpace: Option<Long>
        get() = Option.some(storageReport["usable"] as Long)

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getUsedSpace
     */
    override val usedSpace: Option<Long>
        get() = Option.some(storageReport["used"] as Long)

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getBaseUri
     */
    override val baseUri: URI
        get() {
            val get = HttpGet("/baseUri")
            val response = getResponse(get)
            try {
                if (response != null)
                    return URI(EntityUtils.toString(response.entity, "UTF-8"))
            } catch (e: Exception) {
                throw IllegalStateException("Unable to determine the base URI of the file repository")
            } finally {
                closeConnection(response)
            }
            throw IllegalStateException("Unable to determine the base URI of the file repository")
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.copyTo
     */
    @Throws(IOException::class, NotFoundException::class)
    override fun copyTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
                        toFileName: String): URI {
        val urlSuffix = UrlSupport.concat(
                *arrayOf("copy", fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName))
        val post = HttpPost(urlSuffix)
        val response = getResponse(post, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("File from collection to copy not found: $fromCollection/$fromFileName")
                } else {
                    val uri = URI(EntityUtils.toString(response.entity, "UTF-8"))
                    logger.info("Copied collection file {}/{} to {}", fromCollection, fromFileName, uri)
                    return uri
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Unable to copy file", e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to copy file from collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.moveTo
     */
    @Throws(IOException::class, NotFoundException::class)
    override fun moveTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
                        toFileName: String): URI {
        val urlSuffix = UrlSupport.concat(
                *arrayOf("move", fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName))
        val post = HttpPost(urlSuffix)
        val response = getResponse(post, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("File from collection to move not found: $fromCollection/$fromFileName")
                } else {
                    val uri = URI(EntityUtils.toString(response.entity, "UTF-8"))
                    logger.info("Moved collection file {}/{} to {}", fromCollection, fromFileName, uri)
                    return uri
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Unable to move file", e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to move file from collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.delete
     */
    override fun delete(mediaPackageID: String, mediaPackageElementID: String): Boolean {
        val urlSuffix = UrlSupport
                .concat(*arrayOf(MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID))
        val del = HttpDelete(urlSuffix)
        val response = getResponse(del, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                return HttpStatus.SC_OK == response.statusLine.statusCode
            }
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Error removing file")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.get
     */
    @Throws(NotFoundException::class)
    override fun get(mediaPackageID: String, mediaPackageElementID: String): InputStream {
        val urlSuffix = UrlSupport
                .concat(*arrayOf(MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID))
        val get = HttpGet(urlSuffix)
        val response = getResponse(get, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                return if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException()
                } else {
                    // Do not close this response. It will be closed when the caller closes the input stream
                    RemoteBase.HttpClientClosingInputStream(response)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException()
        }

        throw RuntimeException("Error getting file")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionContents
     */
    @Throws(NotFoundException::class)
    override fun getCollectionContents(collectionId: String): Array<URI> {
        val urlSuffix = UrlSupport.concat(*arrayOf("list", "$collectionId.json"))
        val get = HttpGet(urlSuffix)
        val response = getResponse(get, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException()
                } else {
                    val json = EntityUtils.toString(response.entity)
                    val jsonArray = JSONValue.parse(json) as JSONArray
                    val uris = arrayOfNulls<URI>(jsonArray.size)
                    for (i in jsonArray.indices) {
                        uris[i] = URI(jsonArray[i] as String)
                    }
                    return uris
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException()
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Error getting collection contents")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionSize
     */
    @Throws(NotFoundException::class)
    override fun getCollectionSize(id: String): Long {
        return getCollectionContents(id).size.toLong()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.cleanupOldFilesFromCollection
     */
    @Throws(IOException::class)
    override fun cleanupOldFilesFromCollection(collectionId: String, days: Long): Boolean {
        val url = UrlSupport.concat(*arrayOf(COLLECTION_PATH_PREFIX, collectionId, java.lang.Long.toString(days)))
        val del = HttpDelete(url)
        val response = getResponse(del, SC_NO_CONTENT, SC_NOT_FOUND)
        try {
            if (response != null)
                return SC_NO_CONTENT == response.statusLine.statusCode
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Error removing older files from collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getFromCollection
     */
    @Throws(NotFoundException::class)
    override fun getFromCollection(collectionId: String, fileName: String): InputStream {
        val url = UrlSupport.concat(*arrayOf(COLLECTION_PATH_PREFIX, collectionId, fileName))
        val get = HttpGet(url)
        val response = getResponse(get, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode)
                    throw NotFoundException()
                // Do not close this response. It will be closed when the caller closes the input stream
                return RemoteBase.HttpClientClosingInputStream(response)
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException()
        }

        throw RuntimeException("Error get from collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionURI
     */
    override fun getCollectionURI(collectionID: String, fileName: String): URI {
        val url = UrlSupport.concat(*arrayOf("collectionuri", collectionID, fileName))
        val get = HttpGet(url)
        val response = getResponse(get)
        try {
            if (response != null) {
                return URI(EntityUtils.toString(response.entity))
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to get collection URI")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getURI
     */
    override fun getURI(mediaPackageID: String, mediaPackageElementID: String): URI {
        return getURI(mediaPackageID, mediaPackageElementID, null!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getURI
     */
    override fun getURI(mediaPackageID: String, mediaPackageElementID: String, fileName: String): URI {
        var url = UrlSupport.concat(*arrayOf("uri", mediaPackageID, mediaPackageElementID))
        if (fileName != null)
            url = UrlSupport.concat(url, fileName)
        val get = HttpGet(url)
        val response = getResponse(get)
        try {
            if (response != null) {
                return URI(EntityUtils.toString(response.entity))
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to get URI")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.put
     */
    override fun put(mediaPackageID: String, mediaPackageElementID: String, filename: String, `in`: InputStream): URI {
        val url = UrlSupport.concat(*arrayOf(MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID))
        val post = HttpPost(url)
        val entity = MultipartEntity()
        val body = InputStreamBody(`in`, filename)
        entity.addPart("file", body)
        post.entity = entity
        val response = getResponse(post)
        try {
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                return URI(content)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to put file")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.putInCollection
     */
    override fun putInCollection(collectionId: String, fileName: String, `in`: InputStream): URI {
        val url = UrlSupport.concat(*arrayOf(COLLECTION_PATH_PREFIX, collectionId))
        val post = HttpPost(url)
        val entity = MultipartEntity()
        val body = InputStreamBody(`in`, fileName)
        entity.addPart("file", body)
        post.entity = entity
        val response = getResponse(post)
        try {
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                return URI(content)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to put file in collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.deleteFromCollection
     */
    override fun deleteFromCollection(collectionId: String, fileName: String, removeCollection: Boolean): Boolean {
        // The removeCollection parameter is ignored here, as this remote implementation is not currently used.
        val url = UrlSupport.concat(*arrayOf(COLLECTION_PATH_PREFIX, collectionId, fileName))
        val del = HttpDelete(url)
        val response = getResponse(del, SC_NO_CONTENT, SC_NOT_FOUND)
        try {
            if (response != null)
                return SC_NO_CONTENT == response.statusLine.statusCode
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Error removing file from collection")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.deleteFromCollection
     */
    override fun deleteFromCollection(collectionId: String, fileName: String): Boolean {
        return deleteFromCollection(collectionId, fileName, false)
    }

    companion object {

        /** the logger  */
        private val logger = LoggerFactory.getLogger(WorkingFileRepositoryRemoteImpl::class.java)
    }

}
