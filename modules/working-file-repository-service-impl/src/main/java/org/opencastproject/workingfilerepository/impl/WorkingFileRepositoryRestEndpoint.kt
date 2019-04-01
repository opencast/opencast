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

package org.opencastproject.workingfilerepository.impl

import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED
import javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
import javax.servlet.http.HttpServletResponse.SC_OK
import org.opencastproject.util.MimeTypes.getMimeType
import org.opencastproject.util.RestUtil.R.ok
import org.opencastproject.util.RestUtil.fileResponse
import org.opencastproject.util.RestUtil.partialFileResponse
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.doc.rest.RestParameter.Type.FILE
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.DELETE
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "filerepo", title = "Working File Repository", abstractText = "Stores and retrieves files for use during media processing.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class WorkingFileRepositoryRestEndpoint : WorkingFileRepositoryImpl() {

    /**
     * Callback from OSGi that is called when this service is activated.
     *
     * @param cc
     * OSGi component context
     */
    @Throws(IOException::class)
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
    @RestQuery(name = "put", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID", returnDescription = "The URL to access the stored file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "OK, file stored")], restParameters = [RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE)])
    @Throws(Exception::class)
    fun restPut(@PathParam("mediaPackageID") mediaPackageID: String,
                @PathParam("mediaPackageElementID") mediaPackageElementID: String, @Context request: HttpServletRequest): Response {
        if (ServletFileUpload.isMultipartContent(request)) {
            val iter = ServletFileUpload().getItemIterator(request)
            while (iter.hasNext()) {
                val item = iter.next()
                if (item.isFormField) {
                    continue

                }
                val url = this.put(mediaPackageID, mediaPackageElementID, item.name, item.openStream())
                return Response.ok(url.toString()).build()
            }
        }
        return Response.serverError().status(400).build()
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{filename}")
    @RestQuery(name = "putWithFilename", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID/filename", returnDescription = "The URL to access the stored file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING), RestParameter(name = "filename", description = "the filename", isRequired = true, type = FILE)], reponses = [RestResponse(responseCode = SC_OK, description = "OK, file stored")])
    @Throws(Exception::class)
    fun restPutURLEncoded(@Context request: HttpServletRequest,
                          @PathParam("mediaPackageID") mediaPackageID: String,
                          @PathParam("mediaPackageElementID") mediaPackageElementID: String, @PathParam("filename") filename: String,
                          @FormParam("content") content: String): Response {
        var encoding: String? = request.characterEncoding
        if (encoding == null)
            encoding = "utf-8"

        val url = this.put(mediaPackageID, mediaPackageElementID, filename, IOUtils.toInputStream(content, encoding))
        return Response.ok(url.toString()).build()
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}")
    @RestQuery(name = "putInCollection", description = "Store a file in working repository under ./collectionId/filename", returnDescription = "The URL to access the stored file", pathParameters = [RestParameter(name = "collectionId", description = "the colection identifier", isRequired = true, type = STRING)], restParameters = [RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE)], reponses = [RestResponse(responseCode = SC_OK, description = "OK, file stored")])
    @Throws(Exception::class)
    fun restPutInCollection(@PathParam("collectionId") collectionId: String,
                            @Context request: HttpServletRequest): Response {
        if (ServletFileUpload.isMultipartContent(request)) {
            val iter = ServletFileUpload().getItemIterator(request)
            while (iter.hasNext()) {
                val item = iter.next()
                if (item.isFormField) {
                    continue

                }
                val url = this.putInCollection(collectionId, item.name, item.openStream())
                return Response.ok(url.toString()).build()
            }
        }
        return Response.serverError().status(400).build()
    }

    @DELETE
    @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
    @RestQuery(name = "delete", description = "Remove the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "No content", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "File deleted"), RestResponse(responseCode = SC_NOT_FOUND, description = "File did not exist")])
    fun restDelete(@PathParam("mediaPackageID") mediaPackageID: String,
                   @PathParam("mediaPackageElementID") mediaPackageElementID: String): Response {
        try {
            return if (delete(mediaPackageID, mediaPackageElementID))
                Response.ok().build()
            else
                Response.status(HttpStatus.SC_NOT_FOUND).build()
        } catch (e: Exception) {
            logger.error("Unable to delete element '{}' from mediapackage '{}': {}", mediaPackageElementID,
                    mediaPackageID, e)
            return Response.serverError().entity(e.message).build()
        }

    }

    @DELETE
    @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
    @RestQuery(name = "deleteFromCollection", description = "Remove the file from the working repository under /collectionId/filename", returnDescription = "No content", pathParameters = [RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING), RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_NO_CONTENT, description = "File deleted"), RestResponse(responseCode = SC_NOT_FOUND, description = "Collection or file not found")])
    fun restDeleteFromCollection(@PathParam("collectionId") collectionId: String,
                                 @PathParam("fileName") fileName: String): Response {
        try {
            return if (this.deleteFromCollection(collectionId, fileName))
                Response.noContent().build()
            else
                Response.status(SC_NOT_FOUND).build()
        } catch (e: Exception) {
            logger.error("Unable to delete element '{}' from collection '{}': {}", fileName, collectionId, e)
            return Response.serverError().entity(e.message).build()
        }

    }

    @DELETE
    @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "cleanup/{collectionId}/{numberOfDays}")
    @RestQuery(name = "cleanupOldFilesFromCollection", description = "Remove the files from the working repository under /collectionId that are older than N days", returnDescription = "No content", pathParameters = [RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING), RestParameter(name = "numberOfDays", description = "files older than this number of days will be deleted", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_NO_CONTENT, description = "Files deleted"), RestResponse(responseCode = SC_NOT_FOUND, description = "Collection not found")])
    fun restCleanupOldFilesFromCollection(@PathParam("collectionId") collectionId: String,
                                          @PathParam("numberOfDays") days: Long): Response {
        try {
            return if (this.cleanupOldFilesFromCollection(collectionId, days))
                Response.noContent().build()
            else
                Response.status(SC_NOT_FOUND).build()
        } catch (e: Exception) {
            logger.error("Unable to delete files older than '{}' days from collection '{}': {}",
                    days, collectionId, e)
            return Response.serverError().entity(e.message).build()
        }

    }

    @GET
    @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
    @RestQuery(name = "get", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "The file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "File returned"), RestResponse(responseCode = SC_NOT_MODIFIED, description = "If file not modified"), RestResponse(responseCode = SC_NOT_FOUND, description = "Not found")])
    @Throws(NotFoundException::class)
    fun restGet(@PathParam("mediaPackageID") mediaPackageID: String,
                @PathParam("mediaPackageElementID") mediaPackageElementID: String,
                @HeaderParam("If-None-Match") ifNoneMatch: String): Response {
        // Check the If-None-Match header first
        var md5: String? = null
        try {
            md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID)
            if (StringUtils.isNotBlank(ifNoneMatch) && md5 == ifNoneMatch) {
                return Response.notModified(md5).build()
            }
        } catch (e: IOException) {
            logger.warn("Error reading digest of {}/{}", mediaPackageElementID, mediaPackageElementID)
        }

        try {
            var contentType: String
            val file = getFile(mediaPackageID, mediaPackageElementID)
            try {
                contentType = MimeTypes.fromString(file.path).toString()
            } catch (e: UnknownFileTypeException) {
                contentType = "application/octet-stream"
            }

            try {
                return ok(get(mediaPackageID, mediaPackageElementID), contentType, some(file.length()), none(""))
            } catch (e: IOException) {
                throw NotFoundException()
            }

        } catch (e: IllegalStateException) {
            logger.error("Unable to provide element '{}' from mediapackage '{}': {}", mediaPackageElementID,
                    mediaPackageID, e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @GET
    @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{fileName}")
    @RestQuery(name = "getWithFilename", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID/filename", returnDescription = "The file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING), RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "File returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "Not found")])
    @Throws(NotFoundException::class)
    fun restGet(@PathParam("mediaPackageID") mediaPackageID: String,
                @PathParam("mediaPackageElementID") mediaPackageElementID: String, @PathParam("fileName") fileName: String,
                @HeaderParam("If-None-Match") ifNoneMatch: String, @HeaderParam("Range") range: String): Response {
        var md5: String? = null
        // Check the If-None-Match header first
        try {
            md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID)
            if (StringUtils.isNotBlank(ifNoneMatch) && md5 == ifNoneMatch) {
                return Response.notModified(md5).build()
            }
        } catch (e: IOException) {
            logger.warn("Error reading digest of {}/{}/{}", mediaPackageElementID, mediaPackageElementID,
                    fileName)
        }

        try {
            if (StringUtils.isNotBlank(range)) {
                logger.debug("trying to retrieve range: {}", range)
                return partialFileResponse(getFile(mediaPackageID, mediaPackageElementID), getMimeType(fileName),
                        some(fileName), range).tag(md5).build()

            } else {
                // No If-Non-Match header provided, or the file changed in the meantime
                return fileResponse(getFile(mediaPackageID, mediaPackageElementID), getMimeType(fileName),
                        some(fileName)).tag(md5).build()
            }
        } catch (e: Exception) {
            logger.error("Unable to provide element '{}' from mediapackage '{}': {}", mediaPackageElementID,
                    mediaPackageID, e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @GET
    @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
    @RestQuery(name = "getFromCollection", description = "Gets the file from the working repository under /collectionId/filename", returnDescription = "The file", pathParameters = [RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING), RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "File returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "Not found")])
    @Throws(NotFoundException::class)
    fun restGetFromCollection(@PathParam("collectionId") collectionId: String,
                              @PathParam("fileName") fileName: String): Response {
        return fileResponse(getFileFromCollection(collectionId, fileName), getMimeType(fileName), some(fileName))
                .build()
    }

    @GET
    @Path("/collectionuri/{collectionID}/{fileName}")
    @RestQuery(name = "getUriFromCollection", description = "Gets the URL for a file to be stored in the working repository under /collectionId/filename", returnDescription = "The url to this file", pathParameters = [RestParameter(name = "collectionID", description = "the collection identifier", isRequired = true, type = STRING), RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URL returned")])
    fun restGetCollectionUri(@PathParam("collectionID") collectionId: String,
                             @PathParam("fileName") fileName: String): Response {
        val uri = this.getCollectionURI(collectionId, fileName)
        return Response.ok(uri.toString()).build()
    }

    @GET
    @Path("/uri/{mediaPackageID}/{mediaPackageElementID}")
    @RestQuery(name = "getUri", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URL returned")])
    fun restGetUri(@PathParam("mediaPackageID") mediaPackageID: String,
                   @PathParam("mediaPackageElementID") mediaPackageElementID: String): Response {
        val uri = this.getURI(mediaPackageID, mediaPackageElementID)
        return Response.ok(uri.toString()).build()
    }

    @GET
    @Path("/uri/{mediaPackageID}/{mediaPackageElementID}/{fileName}")
    @RestQuery(name = "getUriWithFilename", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = [RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING), RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING), RestParameter(name = "fileName", description = "the filename", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URL returned")])
    fun restGetUri(@PathParam("mediaPackageID") mediaPackageID: String,
                   @PathParam("mediaPackageElementID") mediaPackageElementID: String, @PathParam("fileName") fileName: String): Response {
        val uri = this.getURI(mediaPackageID, mediaPackageElementID, fileName)
        return Response.ok(uri.toString()).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list/{collectionId}.json")
    @RestQuery(name = "filesInCollection", description = "Lists files in a collection", returnDescription = "Links to the URLs in a collection", pathParameters = [RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URLs returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "Collection not found")])
    @Throws(NotFoundException::class)
    fun restGetCollectionContents(@PathParam("collectionId") collectionId: String): Response {
        val uris = super.getCollectionContents(collectionId)
        val jsonArray = JSONArray()
        for (uri in uris) {
            jsonArray.add(uri.toString())
        }
        return Response.ok(jsonArray.toJSONString()).build()
    }

    @POST
    @Path("/copy/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
    @RestQuery(name = "copy", description = "Copies a file from a collection to a mediapackage", returnDescription = "A URL to the copied file", pathParameters = [RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING), RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING), RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING), RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URL returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "File to copy not found")])
    @Throws(NotFoundException::class)
    fun restCopyTo(@PathParam("fromCollection") fromCollection: String,
                   @PathParam("fromFileName") fromFileName: String, @PathParam("toMediaPackage") toMediaPackage: String,
                   @PathParam("toMediaPackageElement") toMediaPackageElement: String, @PathParam("toFileName") toFileName: String): Response {
        try {
            val uri = super.copyTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName)
            return Response.ok().entity(uri.toString()).build()
        } catch (e: IOException) {
            logger.error("Unable to copy file '{}' from collection '{}' to mediapackage {}/{}: {}",
                    fromFileName, fromCollection, toMediaPackage, toMediaPackageElement, e)
            return Response.serverError().entity(e.message).build()
        }

    }

    @POST
    @Path("/move/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
    @RestQuery(name = "move", description = "Moves a file from a collection to a mediapackage", returnDescription = "A URL to the moved file", pathParameters = [RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING), RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING), RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING), RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING), RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "URL returned"), RestResponse(responseCode = SC_NOT_FOUND, description = "File to move not found")])
    @Throws(NotFoundException::class)
    fun restMoveTo(@PathParam("fromCollection") fromCollection: String,
                   @PathParam("fromFileName") fromFileName: String, @PathParam("toMediaPackage") toMediaPackage: String,
                   @PathParam("toMediaPackageElement") toMediaPackageElement: String, @PathParam("toFileName") toFileName: String): Response {
        try {
            val uri = super.moveTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName)
            return Response.ok().entity(uri.toString()).build()
        } catch (e: IOException) {
            logger.error("Unable to move file '{}' from collection '{}' to mediapackage {}/{}: {}",
                    fromFileName, fromCollection, toMediaPackage, toMediaPackageElement, e)
            return Response.serverError().entity(e.message).build()
        }

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("storage")
    @RestQuery(name = "storage", description = "Returns a report on the disk usage and availability", returnDescription = "Plain text containing the report", reponses = [RestResponse(responseCode = SC_OK, description = "Report returned")])
    fun restGetTotalStorage(): Response {
        val total = this.totalSpace.get()
        val usable = this.usableSpace.get()
        val used = this.usedSpace.get()
        val summary = this.diskSpace
        val json = JSONObject()
        json["size"] = total
        json["usable"] = usable
        json["used"] = used
        json["summary"] = summary
        return Response.ok(json.toJSONString()).build()
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/baseUri")
    @RestQuery(name = "baseuri", description = "Returns a base URI for this repository", returnDescription = "Plain text containing the base URI", reponses = [RestResponse(responseCode = SC_OK, description = "Base URI returned")])
    fun restGetBaseUri(): Response {
        return Response.ok(super.baseUri.toString()).build()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(WorkingFileRepositoryRestEndpoint::class.java)
    }
}
