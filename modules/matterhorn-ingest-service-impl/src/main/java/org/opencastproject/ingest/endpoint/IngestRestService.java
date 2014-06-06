/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.ingest.endpoint;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Catalog;
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
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParser;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.spi.PersistenceProvider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Creates and augments Matterhorn MediaPackages using the api. Stores media into the Working File Repository.
 */
@Path("/")
@RestService(name = "ingestservice", title = "Ingest Service", abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata "
        + "catalogs and attachments.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
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

  /** Dublin Core Terms: http://purl.org/dc/terms/ */
  private static List<String> dcterms = Arrays.asList("abstract", "accessRights", "accrualMethod",
          "accrualPeriodicity", "accrualPolicy", "alternative", "audience", "available", "bibliographicCitation",
          "conformsTo", "contributor", "coverage", "created", "creator", "date", "dateAccepted", "dateCopyrighted",
          "dateSubmitted", "description", "educationLevel", "extent", "format", "hasFormat", "hasPart", "hasVersion",
          "identifier", "instructionalMethod", "isFormatOf", "isPartOf", "isReferencedBy", "isReplacedBy",
          "isRequiredBy", "issued", "isVersionOf", "language", "license", "mediator", "medium", "modified",
          "provenance", "publisher", "references", "relation", "replaces", "requires", "rights", "rightsHolder",
          "source", "spatial", "subject", "tableOfContents", "temporal", "title", "type", "valid");

  private MediaPackageBuilderFactory factory = null;
  private IngestService ingestService = null;
  private ServiceRegistry serviceRegistry = null;
  private DublinCoreCatalogService dublinCoreService;
  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  // For the progress bar -1 bug workaround, keeping UploadJobs in memory rather than saving them using JPA
  private HashMap<String, UploadJob> jobs;
  // The number of ingests this service can handle concurrently.
  private int ingestLimit = -1;

  public IngestRestService() {
    factory = MediaPackageBuilderFactory.newInstance();
    jobs = new HashMap<String, UploadJob>();
  }

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
  public void activate(ComponentContext cc) {
    try {
      emf = persistenceProvider
              .createEntityManagerFactory("org.opencastproject.ingest.endpoint", persistenceProperties);
    } catch (Exception e) {
      logger.error("Unable to initialize JPA EntityManager: " + e.getMessage());
    }
    if (cc != null) {
      defaultWorkflowDefinitionId = StringUtils.trimToNull(cc.getBundleContext().getProperty(
              DEFAULT_WORKFLOW_DEFINITION));
      if (defaultWorkflowDefinitionId == null) {
        throw new IllegalStateException("Default workflow definition is null: " + DEFAULT_WORKFLOW_DEFINITION);
      }
      if (cc.getBundleContext().getProperty(MAX_INGESTS_KEY) != null) {
        try {
          ingestLimit = Integer.parseInt(StringUtils.trimToNull(cc.getBundleContext().getProperty(MAX_INGESTS_KEY)));
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

  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackage")
  @RestQuery(name = "createMediaPackage", description = "Create an empty media package", reponses = {
          @RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response createMediaPackage() {
    MediaPackage mp;
    try {
      mp = ingestService.createMediaPackage();
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("discardMediaPackage")
  @RestQuery(name = "discardMediaPackage", description = "Discard a media package", restParameters = { @RestParameter(description = "Given media package to be destroyed", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response discardMediaPackage(@FormParam("mediaPackage") String mpx) {
    logger.debug("discardMediaPackage(MediaPackage): {}", mpx);
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      ingestService.discardMediaPackage(mp);
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addTrack")
  @RestQuery(name = "addTrackURL", description = "Add a media track to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      mp = ingestService.addTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrack")
  @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageTrack(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Track);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogURL", description = "Add a metadata catalog to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the catalog", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      MediaPackage resultingMediaPackage = ingestService.addCatalog(new URI(url),
              MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(resultingMediaPackage).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogInputStream", description = "Add a metadata catalog to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of media catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The metadata catalog file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Catalog);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentURL", description = "Add an attachment to a given media package using an URL", restParameters = {
          @RestParameter(description = "The location of the attachment", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      if (MediaPackageSupport.sanityCheck(mp).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      mp = ingestService.addAttachment(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentInputStream", description = "Add an attachment to a given media package using an input stream", restParameters = {
          @RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(description = "The attachment file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageAttachment(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Attachment);
  }

  protected Response addMediaPackageElement(HttpServletRequest request, MediaPackageElement.Type type) {
    MediaPackageElementFlavor flavor = null;
    try {
      InputStream in = null;
      String fileName = null;
      MediaPackage mp = null;
      if (ServletFileUpload.isMultipartContent(request)) {
        boolean isDone = false;
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            if ("flavor".equals(fieldName)) {
              String flavorString = Streams.asString(item.openStream());
              if (flavorString != null) {
                flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
              }
            } else if ("mediaPackage".equals(fieldName)) {
              mp = factory.newMediaPackageBuilder().loadFromXml(item.openStream());
            }
          } else {
            // once the body gets read iter.hasNext must not be invoked or the stream can not be read
            // MH-9579
            fileName = item.getName();
            in = item.openStream();
            isDone = true;
          }
          if (isDone) {
            break;
          }
        }
        if (MediaPackageSupport.sanityCheck(mp).isSome())
          return Response.serverError().status(Status.BAD_REQUEST).build();
        switch (type) {
          case Attachment:
            mp = ingestService.addAttachment(in, fileName, flavor, mp);
            break;
          case Catalog:
            mp = ingestService.addCatalog(in, fileName, flavor, mp);
            break;
          case Track:
            mp = ingestService.addTrack(in, fileName, flavor, mp);
            break;
          default:
            throw new IllegalStateException("Type must be one of track, catalog, or attachment");
        }
        // ingestService.ingest(mp);
        return Response.ok(MediaPackageParser.getAsXml(mp)).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  @RestQuery(name = "addMediaPackage", description = "Create and ingest media package from media tracks with additional Dublin Core metadata. Notice: It is mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC catalog with a title included.", restParameters = {
          @RestParameter(description = "The kind of media track. This has to be specified in the request prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
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
          @RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING), }, bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    return addMediaPackage(request, null);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage/{wdID}")
  @RestQuery(name = "addMediaPackage", description = "Create and ingest media package from media tracks with additional Dublin Core metadata. Notice: It is mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC catalog with a title included.", pathParameters = { @RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The kind of media track. This has to be specified in the request prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
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
          @RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request, @PathParam("wdID") String wdID) {
    MediaPackageElementFlavor flavor = null;
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      DublinCoreCatalog dcc = null;
      Map<String, String> workflowProperties = new HashMap<String, String>();
      int seriesDCCatalogNumber = 0;
      int episodeDCCatalogNumber = 0;
      boolean hasMedia = false;
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            String value = Streams.asString(item.openStream());
            /* Ignore empty fields */
            if ("".equals(value)) {
              continue;
            }

            /* “Remember” the flavor for the next media. */
            if ("flavor".equals(fieldName)) {
              flavor = MediaPackageElementFlavor.parseFlavor(value);

              /* Fields for DC catalog */
            } else if (dcterms.contains(fieldName)) {
              if ("identifier".equals(fieldName)) {
                /* Use the identifier for the mediapackage */
                mp.setIdentifier(new IdImpl(value));
              }
              EName en = new EName(DublinCore.TERMS_NS_URI, fieldName);
              if (dcc == null) {
                dcc = dublinCoreService.newInstance();
              }
              dcc.add(en, value);

              /* Episode metadata by URL */
            } else if ("episodeDCCatalogUri".equals(fieldName)) {
              try {
                URI dcurl = new URI(value);
                ingestService.addCatalog(dcurl, MediaPackageElements.EPISODE, mp);
                episodeDCCatalogNumber += 1;
              } catch (java.net.URISyntaxException e) {
                /* Parameter was not a valid URL: Return 400 Bad Request */
                logger.warn(e.getMessage(), e);
                return Response.serverError().status(Status.BAD_REQUEST).build();
              }

              /* Episode metadata DC catalog (XML) as string */
            } else if ("episodeDCCatalog".equals(fieldName)) {
              String fileName = "episode" + episodeDCCatalogNumber + ".xml";
              episodeDCCatalogNumber += 1;
              InputStream is = new ByteArrayInputStream(value.getBytes("UTF-8"));
              ingestService.addCatalog(is, fileName, MediaPackageElements.EPISODE, mp);

              /* Series by URL */
            } else if ("seriesDCCatalogUri".equals(fieldName)) {
              try {
                URI dcurl = new URI(value);
                ingestService.addCatalog(dcurl, MediaPackageElements.SERIES, mp);
              } catch (java.net.URISyntaxException e) {
                /* Parameter was not a valid URL: Return 400 Bad Request */
                logger.warn(e.getMessage(), e);
                return Response.serverError().status(Status.BAD_REQUEST).build();
              }

              /* Series DC catalog (XML) as string */
            } else if ("seriesDCCatalog".equals(fieldName)) {
              String fileName = "series" + seriesDCCatalogNumber + ".xml";
              seriesDCCatalogNumber += 1;
              InputStream is = new ByteArrayInputStream(value.getBytes("UTF-8"));
              ingestService.addCatalog(is, fileName, MediaPackageElements.SERIES, mp);

              /* Add media files by URL */
            } else if ("mediaUri".equals(fieldName)) {
              if (flavor == null) {
                /* A flavor has to be specified in the request prior the media file */
                return Response.serverError().status(Status.BAD_REQUEST).build();
              }
              URI mediaUrl;
              try {
                mediaUrl = new URI(value);
              } catch (java.net.URISyntaxException e) {
                /* Parameter was not a valid URL: Return 400 Bad Request */
                logger.warn(e.getMessage(), e);
                return Response.serverError().status(Status.BAD_REQUEST).build();
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
              return Response.serverError().status(Status.BAD_REQUEST).build();
            }
            ingestService.addTrack(item.openStream(), item.getName(), flavor, mp);
            hasMedia = true;
          }
        }

        /* Check if we got any media. Fail if not. */
        if (!hasMedia) {
          logger.warn("Rejected ingest without actual media.");
          return Response.serverError().status(Status.BAD_REQUEST).build();
        }

        Catalog[] dccs = mp.getCatalogs(MediaPackageElements.EPISODE);
        for (int i = 0; i < dccs.length; i++) {
          EName en = new EName(DublinCore.TERMS_NS_URI, "identifier");
          String id = ((DublinCoreCatalog) dccs[i]).getFirst(en);
          if (id != null) {
            mp.setIdentifier(new IdImpl(id));
          }
        }

        /* Add episode mediapackage if metadata were send separately */
        if (dcc != null) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          dcc.toXml(out, true);
          InputStream in = new ByteArrayInputStream(out.toByteArray());
          ingestService.addCatalog(in, "dublincore.xml", MediaPackageElements.EPISODE, mp);

          /* Check if we have metadata for the episode */
        } else if (episodeDCCatalogNumber == 0) {
          logger.warn("Rejected ingest without episode metadata. At least provide a title.");
          return Response.serverError().status(Status.BAD_REQUEST).build();
        }

        WorkflowInstance workflow = (wdID == null) ? ingestService.ingest(mp) : ingestService.ingest(mp, wdID,
                workflowProperties);
        return Response.ok(workflow).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("addZippedMediaPackage/{workflowDefinitionId}")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", pathParameters = { @RestParameter(description = "Workflow definition id", isRequired = true, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response addZippedMediaPackage(@Context HttpServletRequest request,
          @PathParam("workflowDefinitionId") String wdID, @QueryParam("id") String wiID) {
    logger.debug("addZippedMediaPackage(HttpRequest)");
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
                  + "(This parameter is deprecated. Please use /addZippedMediaPackage/{workflowDefinitionId} with a path parameter instead)", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response addZippedMediaPackage(@Context HttpServletRequest request) {
    logger.debug("addZippedMediaPackage(HttpRequest)");
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

    logger.info("Received new request from {} to ingest a zipped mediapackage", request.getRemoteHost());

    try {
      String workflowDefinitionId = wdID;
      String workflowIdAsString = wiID;
      Long workflowInstanceIdAsLong = null;
      Map<String, String> workflowConfig = new HashMap<String, String>();
      if (ServletFileUpload.isMultipartContent(request)) {
        boolean isDone = false;
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            if (WORKFLOW_INSTANCE_ID_PARAM.equals(item.getFieldName())) {
              workflowIdAsString = IOUtils.toString(item.openStream(), "UTF-8");
              continue;
            } else if (WORKFLOW_DEFINITION_ID_PARAM.equals(item.getFieldName())) {
              workflowDefinitionId = IOUtils.toString(item.openStream(), "UTF-8");
              continue;
            } else {
              logger.debug("Processing form field: " + item.getFieldName());
              workflowConfig.put(item.getFieldName(), IOUtils.toString(item.openStream(), "UTF-8"));
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

      /* Try to convert the workflowId to integer */
      if (!StringUtils.isBlank(workflowIdAsString)) {
        try {
          workflowInstanceIdAsLong = Long.parseLong(workflowIdAsString);
        } catch (NumberFormatException e) {
          logger.warn("{} '{}' is not numeric", WORKFLOW_INSTANCE_ID_PARAM, workflowIdAsString);
        }
      }
      if (StringUtils.isBlank(workflowDefinitionId)) {
        workflowDefinitionId = defaultWorkflowDefinitionId;
      }

      WorkflowInstance workflow = ingestService.addZippedMediaPackage(in, workflowDefinitionId, workflowConfig,
              workflowInstanceIdAsLong);
      return Response.ok(WorkflowParser.toXml(workflow)).build();
    } catch (MediaPackageException e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
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
  @Path("ingest")
  @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files", restParameters = {
          @RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT),
          @RestParameter(description = "Workflow definition id", isRequired = false, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING),
          @RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response ingest(MultivaluedMap<String, String> formData) {
    /**
     * Note: We use a MultivaluedMap here to ensure that we can get any arbitrary form parameters. This is required to
     * enable things like holding for trim or distributing to YouTube.
     */
    logger.debug("ingest(MediaPackage)");
    try {
      MediaPackage mp = null;
      Map<String, String> wfConfig = new HashMap<String, String>();
      for (String key : formData.keySet()) {
        if (!"mediaPackage".equals(key)) {
          wfConfig.put(key, formData.getFirst(key));
        } else {
          mp = factory.newMediaPackageBuilder().loadFromXml(formData.getFirst(key));
        }
      }
      return ingest(mp, wfConfig);
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest/{wdID}")
  @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files, and starting a specified workflow", pathParameters = { @RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(description = "The ID of the given media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response ingest(@PathParam("wdID") String wdID, MultivaluedMap<String, String> formData) {
    if (StringUtils.isBlank(wdID)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      MediaPackage mp = null;
      Map<String, String> wfConfig = new HashMap<String, String>();
      wfConfig.put(WORKFLOW_DEFINITION_ID_PARAM, wdID);
      for (String key : formData.keySet()) {
        if (!"mediaPackage".equals(key)) {
          wfConfig.put(key, formData.getFirst(key));
        } else {
          mp = factory.newMediaPackageBuilder().loadFromXml(formData.getFirst(key));
        }
      }
      return ingest(mp, wfConfig);
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private Response ingest(MediaPackage mp, Map<String, String> wfConfig) {
    if (MediaPackageSupport.sanityCheck(mp).isSome())
      return Response.serverError().status(Status.BAD_REQUEST).build();

    try {
      String workflowInstance = wfConfig.get(WORKFLOW_INSTANCE_ID_PARAM);
      String workflowDefinition = wfConfig.get(WORKFLOW_DEFINITION_ID_PARAM);

      // Double check that the required params exist.
      if (mp == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      WorkflowInstance workflow = null;

      // a workflow instance has been specified
      if (StringUtils.isNotBlank(workflowInstance)) {
        Long workflowInstanceId = null;
        try {
          workflowInstanceId = Long.parseLong(workflowInstance);
        } catch (NumberFormatException e) {
          /*
           * Eat the exception, we don't *really* care since the system will just make up a new ID if needed. This may
           * also be an unscheduled capture, which might not have a Long ID.
           */
        }

        // a workflow defintion was specified
        if (StringUtils.isNotBlank(workflowDefinition)) {
          workflow = ingestService.ingest(mp, workflowDefinition, wfConfig, workflowInstanceId);
        } else {
          workflow = ingestService.ingest(mp, null, wfConfig, workflowInstanceId);
        }
      }
      // a workflow definition was specified, but not a workflow id
      else if (StringUtils.isNotBlank(workflowDefinition)) {
        workflow = ingestService.ingest(mp, workflowDefinition, wfConfig, null);
      }
      // nothing was specified, so we start a new workflow
      else {
        workflow = ingestService.ingest(mp, null, wfConfig, null);
      }
      return Response.ok(WorkflowParser.toXml(workflow)).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  protected UploadJob createUploadJob() {
    /*
     * EntityManager em = emf.createEntityManager(); EntityTransaction tx = em.getTransaction(); try { UploadJob job =
     * new UploadJob(); tx.begin(); em.persist(job); tx.commit(); return job; } catch (RollbackException ex) {
     * logger.error(ex.getMessage(), ex); tx.rollback(); throw new RuntimeException(ex); } finally { em.close(); }
     */
    UploadJob job = new UploadJob();
    jobs.put(job.getId(), job);
    return job;
  }

  /**
   * Creates an upload job and returns an HTML form ready for uploading the file to the newly created upload job.
   * Returns 500 if something goes wrong unexpectedly
   *
   * @return HTML form ready for uploading the file
   */
  @GET
  @Path("filechooser-local.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createUploadJobHtml(@QueryParam("elementType") String elementType) {
    InputStream is = null;
    elementType = (elementType == null) ? "track" : elementType;
    try {
      UploadJob job = createUploadJob();
      is = getClass().getResourceAsStream("/templates/uploadform.html");
      String html = IOUtils.toString(is, "UTF-8");
      // String uploadURL = serverURL + "/ingest/addElementMonitored/" + job.getId();
      String uploadURL = "addElementMonitored/" + job.getId();
      html = html.replaceAll("\\{uploadURL\\}", uploadURL);
      html = html.replaceAll("\\{jobId\\}", job.getId());
      html = html.replaceAll("\\{elementType\\}", elementType);
      logger.debug("New upload job created: " + job.getId());
      jobs.put(job.getId(), job);
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @GET
  @Path("filechooser-inbox.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createInboxHtml() {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/templates/inboxform.html");
      String html = IOUtils.toString(is, "UTF-8");
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @GET
  @Path("filechooser-archive.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createArchiveHtml(@QueryParam("elementType") String elementType) {
    InputStream is = null;
    elementType = (elementType == null) ? "track" : elementType;
    try {
      UploadJob job = createUploadJob();
      is = getClass().getResourceAsStream("/templates/uploadform.html");
      String html = IOUtils.toString(is, "UTF-8");
      // String uploadURL = serverURL + "/ingest/addElementMonitored/" + job.getId();
      String uploadURL = "/ingest/addZippedMediaPackage";
      html = html.replaceAll("\\{uploadURL\\}", uploadURL);
      html = html.replaceAll("\\{jobId\\}", job.getId());
      html = html.replaceAll("\\{elementType\\}", elementType);
      logger.debug("New upload job created: " + job.getId());
      jobs.put(job.getId(), job);
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Add an elements to a MediaPackage and keeps track of the progress of the upload. Returns an HTML that triggers the
   * host sites UploadListener.uploadComplete javascript event Returns an HTML that triggers the host sites
   * UploadListener.uplaodFailed javascript event in case of error
   *
   * @param jobId
   *          of the upload job
   * @param request
   *          containing the file, the flavor and the MediaPackage to which it should be added
   * @return HTML that calls the UploadListener.uploadComplete javascript handler
   */
  @POST
  @Path("addElementMonitored/{jobId}")
  @Produces(MediaType.TEXT_HTML)
  public Response addElementMonitored(@PathParam("jobId") String jobId, @Context HttpServletRequest request) {
    UploadJob job = null;
    MediaPackage mp = null;
    String fileName = null;
    MediaPackageElementFlavor flavor = null;
    String elementType = "track";
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      try { // try to get UploadJob, responde 404 if not successful
        // job = em.find(UploadJob.class, jobId);
        if (jobs.containsKey(jobId)) {
          job = jobs.get(jobId);
        } else {
          throw new NoResultException("Job not found");
        }
      } catch (NoResultException e) {
        logger.warn("Upload job not found for Id: " + jobId);
        return buildUploadFailedRepsonse(job);
      }
      if (ServletFileUpload.isMultipartContent(request)) {
        ServletFileUpload upload = new ServletFileUpload();
        UploadProgressListener listener = new UploadProgressListener(job, this.emf);
        upload.setProgressListener(listener);
        for (FileItemIterator iter = upload.getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if ("mediaPackage".equalsIgnoreCase(fieldName)) {
            mp = factory.newMediaPackageBuilder().loadFromXml(item.openStream());
          } else if ("flavor".equals(fieldName)) {
            String flavorString = Streams.asString(item.openStream());
            if (flavorString != null) {
              flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
            }
          } else if ("elementType".equalsIgnoreCase(fieldName)) {
            String typeString = Streams.asString(item.openStream());
            if (typeString != null) {
              elementType = typeString;
            }
          } else if ("file".equalsIgnoreCase(fieldName)) {
            fileName = item.getName();
            job.setFilename(fileName);
            if ((mp != null) && (flavor != null) && (fileName != null)) {
              // decide which element type to add
              if ("TRACK".equalsIgnoreCase(elementType)) {
                mp = ingestService.addTrack(item.openStream(), fileName, flavor, mp);
              } else if ("CATALOG".equalsIgnoreCase(elementType)) {
                logger.info("Adding Catalog: " + fileName + " - " + flavor);
                mp = ingestService.addCatalog(item.openStream(), fileName, flavor, mp);
              }
              InputStream is = null;
              try {
                is = getClass().getResourceAsStream("/templates/complete.html");
                String html = IOUtils.toString(is, "UTF-8");
                html = html.replaceAll("\\{mediaPackage\\}", MediaPackageParser.getAsXml(mp));
                html = html.replaceAll("\\{jobId\\}", job.getId());
                return Response.ok(html).build();
              } finally {
                IOUtils.closeQuietly(is);
              }
            }
          }
        }
      } else {
        logger.warn("Job " + job.getId() + ": message is not multipart/form-data encoded");
      }
      return buildUploadFailedRepsonse(job);
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return buildUploadFailedRepsonse(job);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Builds a Response containing an HTML that calls the UploadListener.uploadFailed javascript handler.
   *
   * @return HTML that calls the UploadListener.uploadFailed js function
   */
  private Response buildUploadFailedRepsonse(UploadJob job) {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/templates/error.html");
      String html = IOUtils.toString(is, "UTF-8");
      html = html.replaceAll("\\{jobId\\}", job.getId());
      return Response.ok(html).build();
    } catch (IOException ex) {
      logger.error("Unable to build upload failed Response");
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
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
          @RestParameter(defaultValue = "dublincore/episode", description = "DublinCore Flavor", isRequired = false, name = "flavor", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addDCCatalog(@FormParam("mediaPackage") String mp, @FormParam("dublinCore") String dc,
          @FormParam("flavor") String flavor) {
    MediaPackageElementFlavor dcFlavor = MediaPackageElements.EPISODE;
    if (flavor != null) {
      try {
        dcFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
      } catch (IllegalArgumentException e) {
        logger.warn("Unable to set dublin core flavor to {}, using {} instead", flavor, MediaPackageElements.EPISODE);
      }
    }
    try {
      MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mp); // @FormParam("mediaPackage")
      if (MediaPackageSupport.sanityCheck(mediaPackage).isSome())
        return Response.serverError().status(Status.BAD_REQUEST).build();
      // MediaPackage
      // mp
      // yields
      // Exception
      mediaPackage = ingestService.addCatalog(IOUtils.toInputStream(dc, "UTF-8"), "dublincore.xml", dcFlavor,
              mediaPackage);
      return Response.ok(mediaPackage).build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      return Response.serverError().build();
    }
  }

  /**
   * Returns information about the progress of a file upload as a JSON string. Returns 404 if upload job id doesn't
   * exists Returns 500 if something goes wrong unexpectedly
   *
   * TODO cache UploadJobs because endpoint is asked periodically so that not each request yields a DB query operation
   *
   * @param jobId
   * @return progress JSON string
   */
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getProgress/{jobId}")
  public Response getProgress(@PathParam("jobId") String jobId) throws NotFoundException {
    // try to get UploadJob, responde 404 if not successful
    UploadJob job = null;
    if (jobs.containsKey(jobId)) {
      job = jobs.get(jobId);
    } else {
      throw new NotFoundException("Job not found");
    }
    /*
     * String json = "{total:" + Long.toString(job.getBytesTotal()) + ", received:" +
     * Long.toString(job.getBytesReceived()) + "}"; return Response.ok(json).build();
     */
    JSONObject out = new JSONObject();
    out.put("filename", job.getFilename());
    out.put("total", Long.toString(job.getBytesTotal()));
    out.put("received", Long.toString(job.getBytesReceived()));
    return Response.ok(out.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
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
  void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the dublin core service.
   *
   * @param dcService
   *          the dublin core service
   */
  void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dublinCoreService = dcService;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the persistence provider.
   *
   * @param persistenceProvider
   *          the persistence provider
   */
  void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * OSGi Declarative Services callback to set the reference to the persistence properties.
   *
   * @param persistenceProperties
   *          the persistence properties
   */
  void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

}
