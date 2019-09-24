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

package org.opencastproject.workingfilerepository.impl;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.MimeTypes.getMimeType;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.fileResponse;
import static org.opencastproject.util.RestUtil.partialFileResponse;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.doc.rest.RestParameter.Type.FILE;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "filerepo", title = "Working File Repository", abstractText = "Stores and retrieves files for use during media processing.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class WorkingFileRepositoryRestEndpoint extends WorkingFileRepositoryImpl {

  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRestEndpoint.class);

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  @Override
  public void activate(ComponentContext cc) throws IOException {
    super.activate(cc);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "put", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID", returnDescription = "The URL to access the stored file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") }, restParameters = { @RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE) })
  public Response restPut(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @Context HttpServletRequest request)
          throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField()) {
          continue;

        }
        URI url = this.put(mediaPackageID, mediaPackageElementID, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{filename}")
  @RestQuery(name = "putWithFilename", description = "Store a file in working repository under ./mediaPackageID/mediaPackageElementID/filename", returnDescription = "The URL to access the stored file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "filename", description = "the filename", isRequired = true, type = FILE) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") })
  public Response restPutURLEncoded(@Context HttpServletRequest request,
          @PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("filename") String filename,
          @FormParam("content") String content) throws Exception {
    String encoding = request.getCharacterEncoding();
    if (encoding == null)
      encoding = "utf-8";

    URI url = this.put(mediaPackageID, mediaPackageElementID, filename, IOUtils.toInputStream(content, encoding));
    return Response.ok(url.toString()).build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}")
  @RestQuery(name = "putInCollection", description = "Store a file in working repository under ./collectionId/filename", returnDescription = "The URL to access the stored file", pathParameters = { @RestParameter(name = "collectionId", description = "the colection identifier", isRequired = true, type = STRING) }, restParameters = { @RestParameter(name = "file", description = "the filename", isRequired = true, type = FILE) }, reponses = { @RestResponse(responseCode = SC_OK, description = "OK, file stored") })
  public Response restPutInCollection(@PathParam("collectionId") String collectionId,
          @Context HttpServletRequest request) throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField()) {
          continue;

        }
        URI url = this.putInCollection(collectionId, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @DELETE
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "delete", description = "Remove the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "File did not exist") })
  public Response restDelete(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    try {
      if (delete(mediaPackageID, mediaPackageElementID))
        return Response.ok().build();
      else
        return Response.status(HttpStatus.SC_NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Unable to delete element '{}' from mediapackage '{}': {}", mediaPackageElementID,
              mediaPackageID, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  @RestQuery(name = "deleteFromCollection", description = "Remove the file from the working repository under /collectionId/filename", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "File deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Collection or file not found") })
  public Response restDeleteFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) {
    try {
      if (this.deleteFromCollection(collectionId, fileName))
        return Response.noContent().build();
      else
        return Response.status(SC_NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Unable to delete element '{}' from collection '{}': {}", fileName, collectionId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "cleanup/{collectionId}/{numberOfDays}")
  @RestQuery(name = "cleanupOldFilesFromCollection", description = "Remove the files from the working repository under /collectionId that are older than N days", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "numberOfDays", description = "files older than this number of days will be deleted", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(responseCode = SC_NO_CONTENT, description = "Files deleted"),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "Collection not found") })
  public Response restCleanupOldFilesFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("numberOfDays") long days) {
    try {
      if (this.cleanupOldFilesFromCollection(collectionId, days))
        return Response.noContent().build();
      else
        return Response.status(SC_NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Unable to delete files older than '{}' days from collection '{}': {}",
              days, collectionId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "get", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_MODIFIED, description = "If file not modified"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGet(@PathParam("mediaPackageID") final String mediaPackageID,
          @PathParam("mediaPackageElementID") final String mediaPackageElementID,
          @HeaderParam("If-None-Match") String ifNoneMatch) throws NotFoundException {
    // Check the If-None-Match header first
    String md5 = null;
    try {
      md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID);
      if (StringUtils.isNotBlank(ifNoneMatch) && md5.equals(ifNoneMatch)) {
        return Response.notModified(md5).build();
      }
    } catch (IOException e) {
      logger.warn("Error reading digest of {}/{}", mediaPackageElementID, mediaPackageElementID);
    }
    try {
      String contentType;
      File file = getFile(mediaPackageID, mediaPackageElementID);
      try {
        contentType = MimeTypes.fromString(file.getPath()).toString();
      } catch (UnknownFileTypeException e) {
        contentType = "application/octet-stream";
      }
      try {
        return ok(get(mediaPackageID, mediaPackageElementID), contentType, some(file.length()), none(""));
      } catch (IOException e) {
        throw new NotFoundException();
      }
    } catch (IllegalStateException e) {
      logger.error("Unable to provide element '{}' from mediapackage '{}': {}", mediaPackageElementID,
              mediaPackageID, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  @RestQuery(name = "getWithFilename", description = "Gets the file from the working repository under /mediaPackageID/mediaPackageElementID/filename", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGet(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName,
          @HeaderParam("If-None-Match") String ifNoneMatch, @HeaderParam("Range") String range)
          throws NotFoundException {
    String md5 = null;
    // Check the If-None-Match header first
    try {
      md5 = getMediaPackageElementDigest(mediaPackageID, mediaPackageElementID);
      if (StringUtils.isNotBlank(ifNoneMatch) && md5.equals(ifNoneMatch)) {
        return Response.notModified(md5).build();
      }
    } catch (IOException e) {
      logger.warn("Error reading digest of {}/{}/{}", mediaPackageElementID, mediaPackageElementID,
              fileName);
    }

    try {
      if (StringUtils.isNotBlank(range)) {
        logger.debug("trying to retrieve range: {}", range);
        return partialFileResponse(getFile(mediaPackageID, mediaPackageElementID), getMimeType(fileName),
                some(fileName), range).tag(md5).build();

      } else {
        // No If-Non-Match header provided, or the file changed in the meantime
        return fileResponse(getFile(mediaPackageID, mediaPackageElementID), getMimeType(fileName),
                some(fileName)).tag(md5).build();
      }
    } catch (Exception e) {
      logger.error("Unable to provide element '{}' from mediapackage '{}': {}", mediaPackageElementID,
              mediaPackageID, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  @RestQuery(name = "getFromCollection", description = "Gets the file from the working repository under /collectionId/filename", returnDescription = "The file", pathParameters = {
          @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "File returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found") })
  public Response restGetFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) throws NotFoundException {
    return fileResponse(getFileFromCollection(collectionId, fileName), getMimeType(fileName), some(fileName))
            .build();
  }

  @GET
  @Path("/collectionuri/{collectionID}/{fileName}")
  @RestQuery(name = "getUriFromCollection", description = "Gets the URL for a file to be stored in the working repository under /collectionId/filename", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "collectionID", description = "the collection identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the file name", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URL returned") })
  public Response restGetCollectionUri(@PathParam("collectionID") String collectionId,
          @PathParam("fileName") String fileName) {
    URI uri = this.getCollectionURI(collectionId, fileName);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}")
  @RestQuery(name = "getUri", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URL returned") })
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  @RestQuery(name = "getUriWithFilename", description = "Gets the URL for a file to be stored in the working repository under /mediaPackageID", returnDescription = "The url to this file", pathParameters = {
          @RestParameter(name = "mediaPackageID", description = "the mediaPackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "mediaPackageElementID", description = "the mediaPackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "fileName", description = "the filename", isRequired = true, type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "URL returned") })
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID, fileName);
    return Response.ok(uri.toString()).build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/list/{collectionId}.json")
  @RestQuery(name = "filesInCollection", description = "Lists files in a collection", returnDescription = "Links to the URLs in a collection", pathParameters = { @RestParameter(name = "collectionId", description = "the collection identifier", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URLs returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Collection not found") })
  public Response restGetCollectionContents(@PathParam("collectionId") String collectionId) throws NotFoundException {
    URI[] uris = super.getCollectionContents(collectionId);
    JSONArray jsonArray = new JSONArray();
    for (URI uri : uris) {
      jsonArray.add(uri.toString());
    }
    return Response.ok(jsonArray.toJSONString()).build();
  }

  @POST
  @Path("/copy/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  @RestQuery(name = "copy", description = "Copies a file from a collection to a mediapackage", returnDescription = "A URL to the copied file", pathParameters = {
          @RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING),
          @RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URL returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "File to copy not found") })
  public Response restCopyTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName)
          throws NotFoundException {
    try {
      URI uri = super.copyTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (IOException e) {
      logger.error("Unable to copy file '{}' from collection '{}' to mediapackage {}/{}: {}",
              fromFileName, fromCollection, toMediaPackage, toMediaPackageElement, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/move/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  @RestQuery(name = "move", description = "Moves a file from a collection to a mediapackage", returnDescription = "A URL to the moved file", pathParameters = {
          @RestParameter(name = "fromCollection", description = "the collection identifier hosting the source", isRequired = true, type = STRING),
          @RestParameter(name = "fromFileName", description = "the source file name", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackage", description = "the destination mediapackage identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toMediaPackageElement", description = "the destination mediapackage element identifier", isRequired = true, type = STRING),
          @RestParameter(name = "toFileName", description = "the destination file name", isRequired = true, type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "URL returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "File to move not found") })
  public Response restMoveTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName)
          throws NotFoundException {
    try {
      URI uri = super.moveTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (IOException e) {
      logger.error("Unable to move file '{}' from collection '{}' to mediapackage {}/{}: {}",
              fromFileName, fromCollection, toMediaPackage, toMediaPackageElement, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("storage")
  @RestQuery(name = "storage", description = "Returns a report on the disk usage and availability", returnDescription = "Plain text containing the report", reponses = { @RestResponse(responseCode = SC_OK, description = "Report returned") })
  public Response restGetTotalStorage() {
    long total = this.getTotalSpace().get();
    long usable = this.getUsableSpace().get();
    long used = this.getUsedSpace().get();
    String summary = this.getDiskSpace();
    JSONObject json = new JSONObject();
    json.put("size", total);
    json.put("usable", usable);
    json.put("used", used);
    json.put("summary", summary);
    return Response.ok(json.toJSONString()).build();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/baseUri")
  @RestQuery(name = "baseuri", description = "Returns a base URI for this repository", returnDescription = "Plain text containing the base URI", reponses = { @RestResponse(responseCode = SC_OK, description = "Base URI returned") })
  public Response restGetBaseUri() {
    return Response.ok(super.getBaseUri().toString()).build();
  }
}
