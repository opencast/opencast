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

package org.opencastproject.ingest.endpoint;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;

import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.ingest.impl.IngestServiceImpl;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function0.X;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Creates and augments Opencast MediaPackages using the api. Stores media into the Working File Repository.
 */
@Path("/")
@RestService(name = "ingestservice", title = "Ingest Service", abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata "
        + "catalogs and attachments.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
@Component(
  immediate = true,
  service = IngestRestService.class,
  property = {
    "service.description=Ingest REST Endpoint",
    "opencast.service.type=org.opencastproject.ingest",
    "opencast.service.path=/ingest",
    "opencast.service.jobproducer=true"
  }
)
public class IngestRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);

  /** Key for the default workflow definition in config.properties */
  protected static final String DEFAULT_WORKFLOW_DEFINITION = "org.opencastproject.workflow.default.definition";

  /** Key for the default maximum number of ingests in config.properties */
  protected static final String MAX_INGESTS_KEY = "org.opencastproject.ingest.max.concurrent";

  /** The http request parameter used to provide the workflow instance id */
  protected static final String WORKFLOW_INSTANCE_ID_PARAM = "workflowInstanceId";

  /** The http request parameter used to provide the workflow definition id */
  protected static final String WORKFLOW_DEFINITION_ID_PARAM = "workflowDefinitionId";

  /** The default workflow definition */
  private String defaultWorkflowDefinitionId = null;

  /** The http client */
  private TrustedHttpClient httpClient;

  /** Dublin Core Terms: http://purl.org/dc/terms/ */
  private static final List<String> dcterms = Arrays.asList("abstract", "accessRights", "accrualMethod",
          "accrualPeriodicity", "accrualPolicy", "alternative", "audience", "available", "bibliographicCitation",
          "conformsTo", "contributor", "coverage", "created", "creator", "date", "dateAccepted", "dateCopyrighted",
          "dateSubmitted", "description", "educationLevel", "extent", "format", "hasFormat", "hasPart", "hasVersion",
          "identifier", "instructionalMethod", "isFormatOf", "isPartOf", "isReferencedBy", "isReplacedBy",
          "isRequiredBy", "issued", "isVersionOf", "language", "license", "mediator", "medium", "modified",
          "provenance", "publisher", "references", "relation", "replaces", "requires", "rights", "rightsHolder",
          "source", "spatial", "subject", "tableOfContents", "temporal", "title", "type", "valid");

  /** Formatter to for the date into a string */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(IngestService.UTC_DATE_FORMAT);

  /** Media package builder factory */
  private static final MediaPackageBuilderFactory MP_FACTORY = MediaPackageBuilderFactory.newInstance();

  private IngestService ingestService = null;
  private ServiceRegistry serviceRegistry = null;
  private DublinCoreCatalogService dublinCoreService;
  // The number of ingests this service can handle concurrently.
  private int ingestLimit = -1;
  /* Stores a map workflow ID and date to update the ingest start times post-hoc */
  private final Cache<String, Date> startCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build();

  /**
   * Returns the maximum number of concurrent ingest operations or <code>-1</code> if no limit is enforced.
   *
   * @return the maximum number of concurrent ingest operations
   * @see #isIngestLimitEnabled()
   */
  protected synchronized int getIngestLimit() {
    return ingestLimit;
  }

  /**
   * Sets the maximum number of concurrent ingest operations. Use <code>-1</code> to indicate no limit.
   *
   * @param ingestLimit
   *          the limit
   */
  private synchronized void setIngestLimit(int ingestLimit) {
    this.ingestLimit = ingestLimit;
  }

  /**
   * Returns <code>true</code> if a maximum number of concurrent ingest operations has been defined.
   *
   * @return <code>true</code> if there is a maximum number of concurrent ingests
   */
  protected synchronized boolean isIngestLimitEnabled() {
    return ingestLimit >= 0;
  }

  /**
   * Callback for activation of this component.
   */
  @Activate
  public void activate(ComponentContext cc) {
    if (cc != null) {
      defaultWorkflowDefinitionId = trimToNull(cc.getBundleContext().getProperty(DEFAULT_WORKFLOW_DEFINITION));
      if (defaultWorkflowDefinitionId == null) {
        defaultWorkflowDefinitionId = "schedule-and-upload";
      }
      if (cc.getBundleContext().getProperty(MAX_INGESTS_KEY) != null) {
        try {
          ingestLimit = Integer.parseInt(trimToNull(cc.getBundleContext().getProperty(MAX_INGESTS_KEY)));
          if (ingestLimit == 0) {
            ingestLimit = -1;
          }
        } catch (NumberFormatException e) {
          logger.warn("Max ingest property with key " + MAX_INGESTS_KEY
                  + " isn't defined so no ingest limit will be used.");
          ingestLimit = -1;
        }
      }
    }
  }

  @PUT
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackageWithID/{id}")
  @RestQuery(name = "createMediaPackageWithID", description = "Create an empty media package with ID /n Overrides Existing Mediapackage ", pathParameters = {
          @RestParameter(description = "The Id for the new Mediapackage", isRequired = true, name = "id", type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response createMediaPackage(@PathParam("id") String mediaPackageId) {
    MediaPackage mp;
    try {
      mp = ingestService.createMediaPackage(mediaPackageId);

      startCache.put(mp.getIdentifier().toString(), new Date());
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn("Unable to create mediapackage", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackage")
  @RestQuery(name = "createMediaPackage", description = "Create an empty media package", restParameters = {
         }, responses = {
          @RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response createMediaPackage() {
    MediaPackage mp;
    try {
      mp = ingestService.createMediaPackage();
      startCache.put(mp.getIdentifier().toString(), new Date());
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn("Unable to create empty mediapackage", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("discardMediaPackage")
  @RestQuery(name = "discardMediaPackage", description = "Discard a media package", restParameters = { @RestParameter(description = "Given media package to be destroyed", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, responses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response discardMediaPackage(@FormParam("mediaPackage") String mpx) {
    logger.debug("discardMediaPackage(MediaPackage): {}", mpx);
    try {
      MediaPackage mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mpx);
      ingestService.discardMediaPackage(mp);
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Unable to discard mediapackage {}", mpx, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addTrack")
  @RestQuery(name = "addTrackURL", description = "Add a media track to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The Tags of the  media track", isRequired = false, name = "tags", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,  @FormParam("tags")  String tags,
          @FormParam("mediaPackage") String mpx) {
    logger.trace("add media package from url: {} flavor: {} tags: {} mediaPackage: {}", url, flavor, tags, mpx);
    try {
      MediaPackage mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      String[] tagsArray = null;
      if (tags != null) {
        tagsArray = tags.split(",");
      }
      mp = ingestService.addTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), tagsArray, mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn("Unable to add mediapackage track", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrack")
  @RestQuery(
    name = "addTrackInputStream",
    description = "Add a media track to a given media package using an input stream",
    restParameters = {
      @RestParameter(description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
      @RestParameter(description = "The Tags of the  media track", isRequired = false, name = "tags", type = RestParameter.Type.STRING),
      @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) },
    bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE),
    responses = {
      @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
    returnDescription = "")
  public Response addMediaPackageTrack(@Context HttpServletRequest request) {
    logger.trace("add track as multipart-form-data");
    return addMediaPackageElement(request, MediaPackageElement.Type.Track);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addPartialTrack")
  @RestQuery(name = "addPartialTrackURL", description = "Add a partial media track to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The start time in milliseconds", isRequired = true, name = "startTime", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackagePartialTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("startTime") Long startTime, @FormParam("mediaPackage") String mpx) {
    logger.trace("add partial track with url: {} flavor: {} startTime: {} mediaPackage: {}",
            url, flavor, startTime, mpx);
    try {
      MediaPackage mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();

      mp = ingestService.addPartialTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), startTime, mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn("Unable to add partial track", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addPartialTrack")
  @RestQuery(name = "addPartialTrackInputStream", description = "Add a partial media track to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The start time in milliseconds", isRequired = true, name = "startTime", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackagePartialTrack(@Context HttpServletRequest request) {
    logger.trace("add partial track as multipart-form-data");
    return addMediaPackageElement(request, MediaPackageElement.Type.Track);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogURL", description = "Add a metadata catalog to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the catalog", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    logger.trace("add catalog with url: {} flavor: {} mediaPackage: {}", url, flavor, mpx);
    try {
      MediaPackage mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      MediaPackage resultingMediaPackage = ingestService.addCatalog(new URI(url),
              MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(resultingMediaPackage).build();
    } catch (Exception e) {
      logger.warn("Unable to add catalog", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogInputStream", description = "Add a metadata catalog to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of media catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The metadata catalog file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@Context HttpServletRequest request) {
    logger.trace("add catalog as multipart-form-data");
    return addMediaPackageElement(request, MediaPackageElement.Type.Catalog);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentURL", description = "Add an attachment to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the attachment", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    logger.trace("add attachment with url: {} flavor: {} mediaPackage: {}", url, flavor, mpx);
    try {
      MediaPackage mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      mp = ingestService.addAttachment(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn("Unable to add attachment", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentInputStream", description = "Add an attachment to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The attachment file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageAttachment(@Context HttpServletRequest request) {
    logger.trace("add attachment as multipart-form-data");
    return addMediaPackageElement(request, MediaPackageElement.Type.Attachment);
  }

  protected Response addMediaPackageElement(HttpServletRequest request, MediaPackageElement.Type type) {
    MediaPackageElementFlavor flavor = null;
    InputStream in = null;
    try {
      String fileName = null;
      MediaPackage mp = null;
      Long startTime = null;
      String[] tags = null;
      /* Only accept multipart/form-data */
      if (!ServletFileUpload.isMultipartContent(request)) {
        logger.trace("request isn't multipart-form-data");
        return Response.serverError().status(Status.BAD_REQUEST).build();
      }
      boolean isDone = false;
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        String fieldName = item.getFieldName();
        if (item.isFormField()) {
          if ("flavor".equals(fieldName)) {
            String flavorString = Streams.asString(item.openStream(), "UTF-8");
            logger.trace("flavor: {}", flavorString);
            if (flavorString != null) {
              try {
                flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
              } catch (IllegalArgumentException e) {
                String error = String.format("Could not parse flavor '%s'", flavorString);
                logger.debug(error, e);
                return Response.status(Status.BAD_REQUEST).entity(error).build();
              }
            }
          } else if ("tags".equals(fieldName)) {
            String tagsString = Streams.asString(item.openStream(), "UTF-8");
            logger.trace("tags: {}", tagsString);
            tags = tagsString.split(",");
          } else if ("mediaPackage".equals(fieldName)) {
            try {
              String mediaPackageString = Streams.asString(item.openStream(), "UTF-8");
              logger.trace("mediaPackage: {}", mediaPackageString);
              mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mediaPackageString);
            } catch (MediaPackageException e) {
              logger.debug("Unable to parse the 'mediaPackage' parameter: {}", ExceptionUtils.getMessage(e));
              return Response.serverError().status(Status.BAD_REQUEST).build();
            }
          } else if ("startTime".equals(fieldName) && "/addPartialTrack".equals(request.getPathInfo())) {
            String startTimeString = Streams.asString(item.openStream(), "UTF-8");
            logger.trace("startTime: {}", startTime);
            try {
              startTime = Long.parseLong(startTimeString);
            } catch (Exception e) {
              logger.debug("Unable to parse the 'startTime' parameter: {}", ExceptionUtils.getMessage(e));
              return Response.serverError().status(Status.BAD_REQUEST).build();
            }
          }
        } else {
          if (flavor == null) {
            /* A flavor has to be specified in the request prior the video file */
            logger.debug("A flavor has to be specified in the request prior to the content BODY");
            return Response.serverError().status(Status.BAD_REQUEST).build();
          }
          fileName = item.getName();
          in = item.openStream();
          isDone = true;
        }
        if (isDone) {
          break;
        }
      }
      /*
       * Check if we actually got a valid request including a message body and a valid mediapackage to attach the
       * element to
       */
      if (in == null || mp == null || MediaPackageSupport.sanityCheck(mp).isSome()) {
        return Response.serverError().status(Status.BAD_REQUEST).build();
      }
      switch (type) {
        case Attachment:
          mp = ingestService.addAttachment(in, fileName, flavor, tags, mp);
          break;
        case Catalog:
          try {
            mp = ingestService.addCatalog(in, fileName, flavor, tags, mp);
          } catch (IllegalArgumentException e) {
            logger.debug("Invalid catalog data", e);
            return Response.serverError().status(Status.BAD_REQUEST).build();
          }
          break;
        case Track:
          if (startTime == null) {
            mp = ingestService.addTrack(in, fileName, flavor, tags, mp);
          } else {
            mp = ingestService.addPartialTrack(in, fileName, flavor, startTime, mp);
          }
          break;
        default:
          throw new IllegalStateException("Type must be one of track, catalog, or attachment");
      }
      return Response.ok(MediaPackageParser.getAsXml(mp)).build();
    } catch (Exception e) {
      logger.warn("Unable to add mediapackage element", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  @RestQuery(name = "addMediaPackage",
      description = "<p>Create and ingest media package from media tracks with additional Dublin Core metadata. It is "
        + "mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC "
        + "catalog with a title included.  The identifier of the newly created media package will be taken from the "
        + "<em>identifier</em> field or the episode DublinCore catalog (deprecated<sup>*</sup>). If no identifier is "
        + "set, a new random UUIDv4 will be generated. This endpoint is not meant to be used by capture agents for "
        + "scheduled recordings. Its primary use is for manual ingests with command line tools like cURL.</p> "
        + "<p>Multiple tracks can be ingested by using multiple form fields. It is important to always set the "
        + "flavor of the next media file <em>before</em> sending the media file itself.</p>"
        + "<b>(*)</b> The special treatment of the identifier field is deprecated and may be removed in future versions "
        + "without further notice in favor of a random UUID generation to ensure uniqueness of identifiers. "
        + "<h3>Example cURL command:</h3>"
        + "<p>Ingest one video file:</p>"
        + "<p><pre>\n"
        + "curl -i -u admin:opencast http://localhost:8080/ingest/addMediaPackage \\\n"
        + "    -F creator='John Doe' -F title='Test Recording' \\\n"
        + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording.mp4' \n"
        + "</pre></p>"
        + "<p>Ingest two video files:</p>"
        + "<p><pre>\n"
        + "curl -i -u admin:opencast http://localhost:8080/ingest/addMediaPackage \\\n"
        + "    -F creator='John Doe' -F title='Test Recording' \\\n"
        + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording-vga.mp4' \\\n"
        + "    -F 'flavor=presenter/source' -F 'BODY=@test-recording-camera.mp4' \n"
        + "</pre></p>",
      restParameters = {
          @RestParameter(description = "The kind of media track. This has to be specified prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalogUri", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalog", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of series DublinCore Catalog", isRequired = false, name = "seriesDCCatalogUri", type = RestParameter.Type.STRING),
          @RestParameter(description = "Series DublinCore Catalog", isRequired = false, name = "seriesDCCatalog", type = RestParameter.Type.STRING),
          @RestParameter(description = "Access control list in XACML or JSON form", isRequired = false, name = "acl", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING) },
      bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE),
      responses = {
          @RestResponse(description = "Ingest successfull. Returns workflow instance as xml", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Ingest failed due to invalid requests.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "Ingest failed. Something went wrong internally. Please have a look at the log files",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    logger.trace("add mediapackage as multipart-form-data");
    return addMediaPackage(request, null);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage/{wdID}")
  @RestQuery(name = "addMediaPackage",
      description = "<p>Create and ingest media package from media tracks with additional Dublin Core metadata. It is "
        + "mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC "
        + "catalog with a title included.  The identifier of the newly created media package will be taken from the "
        + "<em>identifier</em> field or the episode DublinCore catalog (deprecated<sup>*</sup>). If no identifier is "
        + "set, a newa randumm UUIDv4 will be generated. This endpoint is not meant to be used by capture agents for "
        + "scheduled recordings. It's primary use is for manual ingests with command line tools like cURL.</p> "
        + "<p>Multiple tracks can be ingested by using multiple form fields. It's important, however, to always set the "
        + "flavor of the next media file <em>before</em> sending the media file itself.</p>"
        + "<b>(*)</b> The special treatment of the identifier field is deprecated any may be removed in future versions "
        + "without further notice in favor of a random UUID generation to ensure uniqueness of identifiers. "
        + "<h3>Example cURL command:</h3>"
        + "<p>Ingest one video file:</p>"
        + "<p><pre>\n"
        + "curl -i -u admin:opencast http://localhost:8080/ingest/addMediaPackage/fast \\\n"
        + "    -F creator='John Doe' -F title='Test Recording' \\\n"
        + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording.mp4' \n"
        + "</pre></p>"
        + "<p>Ingest two video files:</p>"
        + "<p><pre>\n"
        + "curl -i -u admin:opencast http://localhost:8080/ingest/addMediaPackage/fast \\\n"
        + "    -F creator='John Doe' -F title='Test Recording' \\\n"
        + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording-vga.mp4' \\\n"
        + "    -F 'flavor=presenter/source' -F 'BODY=@test-recording-camera.mp4' \n"
        + "</pre></p>",
      pathParameters = {
          @RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) },
      restParameters = {
          @RestParameter(description = "The kind of media track. This has to be specified prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalogUri", type = RestParameter.Type.STRING),
          @RestParameter(description = "Episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalog", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of series DublinCore Catalog", isRequired = false, name = "seriesDCCatalogUri", type = RestParameter.Type.STRING),
          @RestParameter(description = "Series DublinCore Catalog", isRequired = false, name = "seriesDCCatalog", type = RestParameter.Type.STRING),
          @RestParameter(description = "Access control list in XACML or JSON form", isRequired = false, name = "acl", type = RestParameter.Type.STRING),
          @RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING) },
      bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE),
      responses = {
          @RestResponse(description = "Ingest successful. Returns workflow instance as XML", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Ingest failed due to invalid requests.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "Ingest failed. Something went wrong internally. Please have a look at the log files",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request, @PathParam("wdID") String wdID) {
    logger.trace("add mediapackage as multipart-form-data with workflow definition id: {}", wdID);
    MediaPackageElementFlavor flavor = null;
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      DublinCoreCatalog dcc = null;
      Map<String, String> workflowProperties = new HashMap<>();
      int seriesDCCatalogNumber = 0;
      int episodeDCCatalogNumber = 0;
      boolean hasMedia = false;
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            String value = Streams.asString(item.openStream(), "UTF-8");
            logger.trace("form field {}: {}", fieldName, value);
            /* Ignore empty fields */
            if ("".equals(value)) {
              continue;
            }

            /* “Remember” the flavor for the next media. */
            if ("flavor".equals(fieldName)) {
              try {
                flavor = MediaPackageElementFlavor.parseFlavor(value);
              } catch (IllegalArgumentException e) {
                return badRequest(String.format("Could not parse flavor '%s'", value), e);
              }
              /* Fields for DC catalog */
            } else if (dcterms.contains(fieldName)) {
              if ("identifier".equals(fieldName)) {
                /* Use the identifier for the mediapackage */
                mp.setIdentifier(new IdImpl(value));
              }
              if (dcc == null) {
                dcc = dublinCoreService.newInstance();
              }
              dcc.add(new EName(DublinCore.TERMS_NS_URI, fieldName), value);

              /* Episode metadata by URL */
            } else if ("episodeDCCatalogUri".equals(fieldName)) {
              try {
                URI dcUrl = new URI(value);
                ingestService.addCatalog(dcUrl, MediaPackageElements.EPISODE, mp);
                updateMediaPackageID(mp, dcUrl);
                episodeDCCatalogNumber += 1;
              } catch (java.net.URISyntaxException e) {
                return badRequest(String.format("Invalid URI %s for episodeDCCatalogUri", value), e);
              } catch (Exception e) {
                return badRequest("Could not parse XML Dublin Core catalog", e);
              }

              /* Episode metadata DC catalog (XML) as string */
            } else if ("episodeDCCatalog".equals(fieldName)) {
              try (InputStream is = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
                final String fileName = "episode-" + episodeDCCatalogNumber + ".xml";
                ingestService.addCatalog(is, fileName, MediaPackageElements.EPISODE, mp);
                episodeDCCatalogNumber += 1;
                is.reset();
                updateMediaPackageID(mp, is);
              } catch (Exception e) {
                return badRequest("Could not parse XML Dublin Core catalog", e);
              }

              /* Series by URL */
            } else if ("seriesDCCatalogUri".equals(fieldName)) {
              try {
                URI dcUrl = new URI(value);
                ingestService.addCatalog(dcUrl, MediaPackageElements.SERIES, mp);
              } catch (java.net.URISyntaxException e) {
                return badRequest(String.format("Invalid URI %s for episodeDCCatalogUri", value), e);
              } catch (Exception e) {
                return badRequest("Could not parse XML Dublin Core catalog", e);
              }

              /* Series DC catalog (XML) as string */
            } else if ("seriesDCCatalog".equals(fieldName)) {
              final String fileName = "series-" + seriesDCCatalogNumber + ".xml";
              seriesDCCatalogNumber += 1;
              try (InputStream is = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
                ingestService.addCatalog(is, fileName, MediaPackageElements.SERIES, mp);
              } catch (Exception e) {
                return badRequest("Could not parse XML Dublin Core catalog", e);
              }

              // Add ACL in JSON, XML or XACML format
            } else if ("acl".equals(fieldName)) {
              InputStream inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
              AccessControlList acl;
              try {
                acl = AccessControlParser.parseAcl(inputStream);
                inputStream = new ByteArrayInputStream(XACMLUtils.getXacml(mp, acl).getBytes(StandardCharsets.UTF_8));
              } catch (AccessControlParsingException e) {
                // Couldn't parse this → already XACML. Why again are we using three different formats?
                logger.debug("Unable to parse ACL, guessing that this is already XACML");
                inputStream.reset();
              }
              ingestService.addAttachment(inputStream, "episode-security.xml", XACML_POLICY_EPISODE, mp);

              /* Add media files by URL */
            } else if ("mediaUri".equals(fieldName)) {
              if (flavor == null) {
                return badRequest("A flavor has to be specified in the request prior to the media file", null);
              }
              URI mediaUrl;
              try {
                mediaUrl = new URI(value);
              } catch (java.net.URISyntaxException e) {
                return badRequest(String.format("Invalid URI %s for media", value), e);
              }
              ingestService.addTrack(mediaUrl, flavor, mp);
              hasMedia = true;

            } else {
              /* Tread everything else as workflow properties */
              workflowProperties.put(fieldName, value);
            }

            /* Media files as request parameter */
          } else {
            if (flavor == null) {
              /* A flavor has to be specified in the request prior the video file */
              return badRequest("A flavor has to be specified in the request prior to the content BODY", null);
            }
            ingestService.addTrack(item.openStream(), item.getName(), flavor, mp);
            hasMedia = true;
          }
        }

        /* Check if we got any media. Fail if not. */
        if (!hasMedia) {
          return badRequest("Rejected ingest without actual media.", null);
        }

        /* Add episode mediapackage if metadata were send separately */
        if (dcc != null) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          try {
            dcc.toXml(out, true);
            try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
              ingestService.addCatalog(in, "dublincore.xml", MediaPackageElements.EPISODE, mp);
            }
          } catch (Exception e) {
            return badRequest("Could not create XML from ingested metadata", e);
          }

          /* Check if we have metadata for the episode */
        } else if (episodeDCCatalogNumber == 0) {
          return badRequest("Rejected ingest without episode metadata. At least provide a title.", null);
        }

        WorkflowInstance workflow = (wdID == null)
            ? ingestService.ingest(mp)
            : ingestService.ingest(mp, wdID, workflowProperties);
        return Response.ok(workflow).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (IllegalArgumentException e) {
      return badRequest(e.getMessage(), e);
    } catch (Exception e) {
      logger.warn("Unable to add mediapackage", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Try updating the identifier of a mediapackage with the identifier from a episode DublinCore catalog.
   *
   * @param mp
   *          MediaPackage to modify
   * @param is
   *          InputStream containing the episode DublinCore catalog
   */
  private void updateMediaPackageID(MediaPackage mp, InputStream is) throws IOException {
    DublinCoreCatalog dc = DublinCores.read(is);
    EName en = new EName(DublinCore.TERMS_NS_URI, "identifier");
    String id = dc.getFirst(en);
    if (id != null) {
      mp.setIdentifier(new IdImpl(id));
    }
  }

  /**
   * Try updating the identifier of a mediapackage with the identifier from a episode DublinCore catalog.
   *
   * @param mp
   *          MediaPackage to modify
   * @param uri
   *          URI to get the episode DublinCore catalog from
   */
  private void updateMediaPackageID(MediaPackage mp, URI uri) throws IOException {
    InputStream in = null;
    HttpResponse response = null;
    try {
      if (uri.toString().startsWith("http")) {
        HttpGet get = new HttpGet(uri);
        response = httpClient.execute(get);
        int httpStatusCode = response.getStatusLine().getStatusCode();
        if (httpStatusCode != 200) {
          throw new IOException(uri + " returns http " + httpStatusCode);
        }
        in = response.getEntity().getContent();
      } else {
        in = uri.toURL().openStream();
      }
      updateMediaPackageID(mp, in);
      in.close();
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
  }

  @POST
  @Path("addZippedMediaPackage/{workflowDefinitionId}")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", pathParameters = { @RestParameter(description = "Workflow definition id", isRequired = true, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), responses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response addZippedMediaPackage(@Context HttpServletRequest request,
          @PathParam("workflowDefinitionId") String wdID, @QueryParam("id") String wiID) {
    logger.trace("add zipped media package with workflow definition id: {} and workflow instance id: {}", wdID, wiID);
    if (!isIngestLimitEnabled() || getIngestLimit() > 0) {
      return ingestZippedMediaPackage(request, wdID, wiID);
    } else {
      logger.warn("Delaying ingest because we have exceeded the maximum number of ingests this server is setup to do concurrently.");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @POST
  @Path("addZippedMediaPackage")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", restParameters = {
          @RestParameter(description = "The workflow definition ID to run on this mediapackage. "
                  + "This parameter has to be set in the request prior to the zipped mediapackage "
                  + "(This parameter is deprecated. Please use /addZippedMediaPackage/{workflowDefinitionId} instead)", isRequired = false, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING),
          @RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage. "
                  + "This parameter has to be set in the request prior to the zipped mediapackage "
                  + "(This parameter is deprecated. Please use /addZippedMediaPackage/{workflowDefinitionId} with a path parameter instead)", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), responses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response addZippedMediaPackage(@Context HttpServletRequest request) {
    logger.trace("add zipped media package");
    if (!isIngestLimitEnabled() || getIngestLimit() > 0) {
      return ingestZippedMediaPackage(request, null, null);
    } else {
      logger.warn("Delaying ingest because we have exceeded the maximum number of ingests this server is setup to do concurrently.");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  private Response ingestZippedMediaPackage(HttpServletRequest request, String wdID, String wiID) {
    if (isIngestLimitEnabled()) {
      setIngestLimit(getIngestLimit() - 1);
      logger.debug("An ingest has started so remaining ingest limit is " + getIngestLimit());
    }
    InputStream in = null;
    Date started = new Date();

    logger.info("Received new request from {} to ingest a zipped mediapackage", request.getRemoteHost());

    try {
      String workflowDefinitionId = wdID;
      String workflowIdAsString = wiID;
      Long workflowInstanceIdAsLong = null;
      Map<String, String> workflowConfig = new HashMap<>();
      if (ServletFileUpload.isMultipartContent(request)) {
        boolean isDone = false;
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            String value = Streams.asString(item.openStream(), "UTF-8");
            logger.trace("{}: {}", fieldName, value);
            if (WORKFLOW_INSTANCE_ID_PARAM.equals(fieldName)) {
              workflowIdAsString = value;
              continue;
            } else if (WORKFLOW_DEFINITION_ID_PARAM.equals(fieldName)) {
              workflowDefinitionId = value;
              continue;
            } else {
              logger.debug("Processing form field: " + fieldName);
              workflowConfig.put(fieldName, value);
            }
          } else {
            logger.debug("Processing file item");
            // once the body gets read iter.hasNext must not be invoked or the stream can not be read
            // MH-9579
            in = item.openStream();
            isDone = true;
          }
          if (isDone)
            break;
        }
      } else {
        logger.debug("Processing file item");
        in = request.getInputStream();
      }

      // Adding ingest start time to workflow configuration
      DateFormat formatter = new SimpleDateFormat(IngestService.UTC_DATE_FORMAT);
      workflowConfig.put(IngestService.START_DATE_KEY, formatter.format(started));

      /* Legacy support: Try to convert the workflowId to integer */
      if (!StringUtils.isBlank(workflowIdAsString)) {
        try {
          workflowInstanceIdAsLong = Long.parseLong(workflowIdAsString);
        } catch (NumberFormatException e) {
          // The workflowId is not a long value and might be the media package identifier
          workflowConfig.put(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY, workflowIdAsString);
        }
      }
      if (StringUtils.isBlank(workflowDefinitionId)) {
        workflowDefinitionId = defaultWorkflowDefinitionId;
      }

      WorkflowInstance workflow;
      if (workflowInstanceIdAsLong != null) {
        workflow = ingestService.addZippedMediaPackage(in, workflowDefinitionId, workflowConfig,
                workflowInstanceIdAsLong);
      } else {
        workflow = ingestService.addZippedMediaPackage(in, workflowDefinitionId, workflowConfig);
      }
      return Response.ok(WorkflowParser.toXml(workflow)).build();
    } catch (NotFoundException e) {
      logger.info("Not found: {}", e.getMessage());
      return Response.status(Status.NOT_FOUND).build();
    } catch (MediaPackageException e) {
      logger.warn("Unable to ingest mediapackage: {}", e.getMessage());
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to ingest mediapackage", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(in);
      if (isIngestLimitEnabled()) {
        setIngestLimit(getIngestLimit() + 1);
        logger.debug("An ingest has finished so increased ingest limit to " + getIngestLimit());
      }
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest/{wdID}")
  @RestQuery(name = "ingest",
             description = "<p>Ingest the completed media package into the system and start a specified workflow.</p>"
             + "<p>In addition to the documented form parameters, workflow parameters are accepted as well.</p>",
    pathParameters = {
      @RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) },
    restParameters = {
      @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) },
    responses = {
      @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) },
    returnDescription = "")
  public Response ingest(@Context HttpServletRequest request, @PathParam("wdID") String wdID) {
    logger.trace("ingest media package with workflow definition id: {}", wdID);
    if (StringUtils.isBlank(wdID)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return ingest(wdID, request);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  @RestQuery(name = "ingest",
             description = "<p>Ingest the completed media package into the system</p>"
             + "<p>In addition to the documented form parameters, workflow parameters are accepted as well.</p>",
    restParameters = {
      @RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT),
      @RestParameter(description = "Workflow definition id", isRequired = false, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING),
      @RestParameter(description = "The workflow instance ID to associate this ingest with scheduled events.", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) },
    responses = {
      @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) },
    returnDescription = "")
  public Response ingest(@Context HttpServletRequest request) {
    return ingest(null, request);
  }

  private Map<String, String> getWorkflowConfig(MultivaluedMap<String, String> formData) {
    Map<String, String> wfConfig = new HashMap<>();
    for (String key : formData.keySet()) {
      if (!"mediaPackage".equals(key)) {
        wfConfig.put(key, formData.getFirst(key));
      }
    }
    return wfConfig;
  }

  private Response ingest(final String wdID, final HttpServletRequest request) {
    /* Note: We use a MultivaluedMap here to ensure that we can get any arbitrary form parameters. This is required to
     * enable things like holding for trim or distributing to YouTube. */
    final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
    if (ServletFileUpload.isMultipartContent(request)) {
      // parse form fields
      try {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            final String value = Streams.asString(item.openStream(), "UTF-8");
            formData.putSingle(item.getFieldName(), value);
          }
        }
      } catch (FileUploadException | IOException e) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } else {
      request.getParameterMap().forEach((key, value) -> formData.put(key, Arrays.asList(value)));
    }

    final Map<String, String> wfConfig = getWorkflowConfig(formData);
    if (StringUtils.isNotBlank(wdID))
      wfConfig.put(WORKFLOW_DEFINITION_ID_PARAM, wdID);

    final MediaPackage mp;
    try {
      mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(formData.getFirst("mediaPackage"));
      if (MediaPackageSupport.sanityCheck(mp).isSome()) {
        logger.warn("Rejected ingest with invalid mediapackage {}", mp);
        return Response.status(Status.BAD_REQUEST).build();
      }
    } catch (Exception e) {
      logger.warn("Rejected ingest without mediapackage");
      return Response.status(Status.BAD_REQUEST).build();
    }

    final String workflowInstance = wfConfig.get(WORKFLOW_INSTANCE_ID_PARAM);
    final String workflowDefinition = wfConfig.get(WORKFLOW_DEFINITION_ID_PARAM);

    // Adding ingest start time to workflow configuration
    final Date ingestDate = startCache.getIfPresent(mp.getIdentifier().toString());
    wfConfig.put(IngestService.START_DATE_KEY, DATE_FORMAT.format(ingestDate != null ? ingestDate : new Date()));

    final X<WorkflowInstance> ingest = new X<WorkflowInstance>() {
      @Override
      public WorkflowInstance xapply() throws Exception {
        /* Legacy support: Try to convert the workflowInstance to integer */
        Long workflowInstanceId = null;
        if (StringUtils.isNotBlank(workflowInstance)) {
          try {
            workflowInstanceId = Long.parseLong(workflowInstance);
          } catch (NumberFormatException e) {
            // The workflowId is not a long value and might be the media package identifier
            wfConfig.put(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY, workflowInstance);
          }
        }

        if (workflowInstanceId != null) {
          return ingestService.ingest(mp, trimToNull(workflowDefinition), wfConfig, workflowInstanceId);
        } else {
          return ingestService.ingest(mp, trimToNull(workflowDefinition), wfConfig);
        }
      }
    };

    try {
      WorkflowInstance workflow = ingest.apply();
      startCache.asMap().remove(mp.getIdentifier().toString());
      return Response.ok(WorkflowParser.toXml(workflow)).build();
    } catch (Exception e) {
      logger.warn("Unable to ingest mediapackage", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("schedule")
  @RestQuery(name = "schedule", description = "Schedule an event based on the given media package",
          restParameters = {
                  @RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) },
          responses = {
                  @RestResponse(description = "Event scheduled", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) },
          returnDescription = "")
  public Response schedule(MultivaluedMap<String, String> formData) {
    logger.trace("pass schedule with default workflow definition id {}", defaultWorkflowDefinitionId);
    return this.schedule(defaultWorkflowDefinitionId, formData);
  }

  @POST
  @Path("schedule/{wdID}")
  @RestQuery(name = "schedule", description = "Schedule an event based on the given media package",
          pathParameters = {
          @RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) },
          restParameters = {
          @RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) },
          responses = {
          @RestResponse(description = "Event scheduled", responseCode = HttpServletResponse.SC_CREATED),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) },
          returnDescription = "")
  public Response schedule(@PathParam("wdID") String wdID, MultivaluedMap<String, String> formData) {
    if (StringUtils.isBlank(wdID)) {
      logger.trace("workflow definition id is not specified");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    Map<String, String> wfConfig = getWorkflowConfig(formData);
    if (StringUtils.isNotBlank(wdID)) {
      wfConfig.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, wdID);
    }
    logger.debug("Schedule with workflow definition '{}'", wfConfig.get(WORKFLOW_DEFINITION_ID_PARAM));

    String mediaPackageXml = formData.getFirst("mediaPackage");
    if (StringUtils.isBlank(mediaPackageXml)) {
      logger.debug("Rejected schedule without media package");
      return Response.status(Status.BAD_REQUEST).build();
    }

    MediaPackage mp = null;
    try {
      mp = MP_FACTORY.newMediaPackageBuilder().loadFromXml(mediaPackageXml);
      if (MediaPackageSupport.sanityCheck(mp).isSome()) {
        throw new MediaPackageException("Insane media package");
      }
    } catch (MediaPackageException e) {
      logger.debug("Rejected ingest with invalid media package {}", mp);
      return Response.status(Status.BAD_REQUEST).build();
    }

    MediaPackageElement[] mediaPackageElements = mp.getElementsByFlavor(MediaPackageElements.EPISODE);
    if (mediaPackageElements.length != 1) {
      logger.debug("There can be only one (and exactly one) episode dublin core catalog: https://youtu.be/_J3VeogFUOs");
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      ingestService.schedule(mp, wdID, wfConfig);
      return Response.status(Status.CREATED).build();
    } catch (IngestException e) {
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (SchedulerConflictException e) {
      return Response.status(Status.CONFLICT).entity(e.getMessage()).build();
    } catch (NotFoundException | UnauthorizedException | SchedulerException e) {
      return Response.serverError().build();
    }
  }

  /**
   * Adds a dublinCore metadata catalog to the MediaPackage and returns the grown mediaPackage. JQuery Ajax functions
   * doesn't support multipart/form-data encoding.
   *
   * @param mp
   *          MediaPackage
   * @param dc
   *          DublinCoreCatalog
   * @return grown MediaPackage XML
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addDCCatalog")
  @RestQuery(name = "addDCCatalog", description = "Add a dublincore episode catalog to a given media package using an url", restParameters = {
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT),
          @RestParameter(description = "DublinCore catalog as XML", isRequired = true, name = "dublinCore", type = RestParameter.Type.TEXT),
          @RestParameter(defaultValue = "dublincore/episode", description = "DublinCore Flavor", isRequired = false, name = "flavor", type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addDCCatalog(@FormParam("mediaPackage") String mp, @FormParam("dublinCore") String dc,
          @FormParam("flavor") String flavor) {
    logger.trace("add DC catalog: {} with flavor: {} to media package: {}", dc, flavor, mp);
    MediaPackageElementFlavor dcFlavor = MediaPackageElements.EPISODE;
    if (flavor != null) {
      try {
        dcFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
      } catch (IllegalArgumentException e) {
        return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
      }
    }
    MediaPackage mediaPackage;
    /* Check if we got a proper mediapackage and try to parse it */
    try {
      mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mp);
    } catch (MediaPackageException e) {
      return Response.serverError().status(Status.BAD_REQUEST).build();
    }
    if (MediaPackageSupport.sanityCheck(mediaPackage).isSome()) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    /* Check if we got a proper catalog */
    try {
      DublinCoreXmlFormat.read(dc);
    } catch (Exception e) {
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    try (InputStream in = IOUtils.toInputStream(dc, "UTF-8")) {
      mediaPackage = ingestService.addCatalog(in, "dublincore.xml", dcFlavor, mediaPackage);
    } catch (MediaPackageException e) {
      return Response.serverError().status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (IOException e) {
      logger.error("Could not write catalog to disk", e);
      return Response.serverError().build();
    } catch (Exception e) {
      logger.error("Unable to add catalog", e);
      return Response.serverError().build();
    }
    return Response.ok(mediaPackage).build();
  }

  /**
   * Return a bad request response but log additional details in debug mode.
   *
   * @param message
   *          Message to send
   * @param e
   *          Exception to log. If <pre>null</pre>, a new exception is created to log a stack trace.
   * @return 400 BAD REQUEST HTTP response
   */
  private Response badRequest(final String message, final Exception e) {
    logger.debug(message, e == null && logger.isDebugEnabled() ? new IngestException(message) : e);
    return Response.status(Status.BAD_REQUEST)
        .entity(message)
        .build();
  }

  @Override
  public JobProducer getService() {
    return ingestService;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the ingest service.
   *
   * @param ingestService
   *          the ingest service
   */
  @Reference
  void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  @Reference
  void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the dublin core service.
   *
   * @param dcService
   *          the dublin core service
   */
  @Reference
  void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dublinCoreService = dcService;
  }

  /**
   * Sets the trusted http client
   *
   * @param httpClient
   *          the http client
   */
  @Reference
  public void setHttpClient(TrustedHttpClient httpClient) {
    this.httpClient = httpClient;
  }

}
