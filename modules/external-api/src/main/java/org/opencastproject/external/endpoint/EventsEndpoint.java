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
package org.opencastproject.external.endpoint;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_11_0;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_1_0;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_4_0;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_7_0;
import static org.opencastproject.external.util.SchedulingUtils.SchedulingInfo;
import static org.opencastproject.external.util.SchedulingUtils.convertConflictingEvents;
import static org.opencastproject.external.util.SchedulingUtils.getConflictingEvents;
import static org.opencastproject.index.service.util.JSONUtils.arrayToJsonArray;
import static org.opencastproject.index.service.util.JSONUtils.collectionToJsonArray;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.QueryPreprocessor;
import org.opencastproject.elasticsearch.index.objects.IndexObject;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventIndexSchema;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponseBuilder;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.util.AclUtils;
import org.opencastproject.external.util.ExternalMetadataUtils;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.util.EventHttpServletRequest;
import org.opencastproject.index.service.impl.util.EventUtils;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.list.impl.EmptyResourceListQuery;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.metadata.dublincore.MetadataList.Locked;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/api/events")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0,
            ApiMediaType.VERSION_1_3_0, ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0,
            ApiMediaType.VERSION_1_6_0, ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0,
            ApiMediaType.VERSION_1_9_0, ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0 })
@RestService(name = "externalapievents", title = "External API Events Service", notes = {},
             abstractText = "Provides resources and operations related to the events")
@Tag(name = "External API")
@Tag(name = "External API - Events",
    description = "The events endpoint provides resources and operations related to the events")
@Component(
    immediate = true,
    service = { EventsEndpoint.class,ManagedService.class },
    property = {
        "service.description=External API - Events Endpoint",
        "opencast.service.type=org.opencastproject.external.events",
        "opencast.service.path=/api/events"
    }
)
@JaxrsResource
public class EventsEndpoint implements ManagedService {

  protected static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /** The default time before a piece of signed content expires. 2 Hours. */
  protected static final Long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60L;

  /** Subtype of previews required by the video editor */
  private static final String PREVIEW_SUBTYPE = "preview.subtype";

  /** Subtype of previews required by the video editor */
  private static final String DEFAULT_PREVIEW_SUBTYPE = "preview";

  /** ID of the workflow used to retract published events */
  private static final String RETRACT_WORKFLOW = "retract.workflow.id";

  /** Default ID of the workflow used to retract published events */
  private static final String DEFAULT_RETRACT_WORKFLOW = "delete";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EventsEndpoint.class);

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  private static long expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private String previewSubtype = DEFAULT_PREVIEW_SUBTYPE;

  private Map<String, MetadataField> configuredMetadataFields = new TreeMap<>();

  private String retractWorkflowId = DEFAULT_RETRACT_WORKFLOW;

  /** The resolutions */
  private enum CommentResolution {
    ALL, UNRESOLVED, RESOLVED;
  };

  /* OSGi service references */
  private AssetManager assetManager;
  private ElasticsearchIndex elasticsearchIndex;
  private IndexService indexService;
  private IngestService ingestService;
  private SecurityService securityService;
  private final List<EventCatalogUIAdapter> catalogUIAdapters = new CopyOnWriteArrayList<>();
  private final Map<String, List<EventCatalogUIAdapter>> orgCatalogUIAdaptersMap = new ConcurrentHashMap<>();
  private UrlSigningService urlSigningService;
  private SchedulerService schedulerService;
  private CaptureAgentStateService agentStateService;
  private WorkflowService workflowService;

  /** OSGi DI */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi DI */
  @Reference
  void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  /** OSGi DI */
  @Reference
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi DI */
  @Reference
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi DI */
  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI */
  @Reference
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  @Reference
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /** OSGi DI. */
  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeCatalogUIAdapter"
  )
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.add(catalogUIAdapter);
    invalidateOrgCatalogUIAdaptersMapFor(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.remove(catalogUIAdapter);
    invalidateOrgCatalogUIAdaptersMapFor(catalogUIAdapter);
  }

  /**
   * Invalidates caches for organizations that are handled by given catalog.
   *
   * @param catalogUIAdapter catalog used to identify affected organizations.
   */
  private void invalidateOrgCatalogUIAdaptersMapFor(EventCatalogUIAdapter catalogUIAdapter) {
    // clean cached org to catalog map
    for (String orgName : orgCatalogUIAdaptersMap.keySet()) {
      if (catalogUIAdapter.handlesOrganization(orgName)) {
        orgCatalogUIAdaptersMap.remove(orgName);
      }
    }
  }

  /** OSGi DI */
  public CaptureAgentStateService getAgentStateService() {
    return agentStateService;
  }

  /** OSGi DI */
  @Reference
  public void setAgentStateService(CaptureAgentStateService agentStateService) {
    this.agentStateService = agentStateService;
  }

  /** OSGi DI */
  @Reference
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }


  private List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return getEventCatalogUIAdapters(getSecurityService().getOrganization().getId());
  }

  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    List<EventCatalogUIAdapter> cachedCatalogUIAdapters = orgCatalogUIAdaptersMap.computeIfAbsent(organization,
        org -> new ArrayList<>(catalogUIAdapters.stream()
            .filter(a -> a.handlesOrganization(org))
            .collect(Collectors.toList())));
    // create a shallow copy as callers may change it
    return new ArrayList<>(cachedCatalogUIAdapters);
  }

  /** OSGi activation method */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Events Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Ensure properties is not null
    if (properties == null) {
      properties = new Hashtable<>();
      logger.debug("No configuration set");
    }

    // Read URL Signing Expiration duration
    // Default to DEFAULT_URL_SIGNING_EXPIRE_DURATION.toString()));
    expireSeconds = Long.parseLong(Objects.toString(
        properties.get(URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY),
        DEFAULT_URL_SIGNING_EXPIRE_DURATION.toString()));
    logger.debug("URLs signatures are configured to expire in {}.", DateTimeSupport.humanReadableTime(expireSeconds));

    // Read preview subtype configuration
    // Default to DEFAULT_PREVIEW_SUBTYPE
    previewSubtype = StringUtils.defaultString((String) properties.get(PREVIEW_SUBTYPE), DEFAULT_PREVIEW_SUBTYPE);
    logger.debug("Preview subtype is '{}'", previewSubtype);

    configuredMetadataFields = DublinCoreMetadataUtil.getDublinCoreProperties(properties);

    retractWorkflowId = StringUtils.defaultString((String) properties.get(RETRACT_WORKFLOW), DEFAULT_RETRACT_WORKFLOW);
    logger.debug("Retract Workflow is '{}'", retractWorkflowId);
  }

  public static <T> boolean isNullOrEmpty(List<String> list) {
    return list == null || list.isEmpty();
  }

  @GET
  @Path("{eventId}")
  @RestQuery(name = "getevent", description = "Returns a single event. By setting the optional sign parameter to true, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the maximum validity of signed URLs when caching this response.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "sign", isRequired = false, description = "Whether public distribution urls should be signed.", type = Type.BOOLEAN),
                  @RestParameter(name = "withacl", isRequired = false, description = "Whether the acl metadata should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "withmetadata", isRequired = false, description = "Whether the metadata catalogs should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "withscheduling", isRequired = false, description = "Whether the scheduling information should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "withpublications", isRequired = false, description = "Whether the publication ids and urls should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "includeInternalPublication", isRequired = false, description = "Whether internal publications should be included.", type = Type.BOOLEAN)}, responses = {
                          @RestResponse(description = "The event is returned.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  @Operation(summary = "Get a single event", description = "Returns a single event. By setting the optional sign parameter to true, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the maximum validity of signed URLs when caching this response.")
  public Response getEvent(
      @HeaderParam("Accept") String acceptHeader,
      @Parameter(description = "The event id", required = true)
      @PathParam("eventId") String id,
      @Parameter(description = "Whether public distribution urls should be signed.")
      @QueryParam("sign") boolean sign,
      @Parameter(description = "Whether the acl metadata should be included in the response.")
      @QueryParam("withacl") Boolean withAcl,
      @Parameter(description = "Whether the metadata catalogs should be included in the response.")
      @QueryParam("withmetadata") Boolean withMetadata,
      @Parameter(description = "Whether the scheduling information should be included in the response.")
      @QueryParam("withscheduling") Boolean withScheduling,
      @Parameter(description = "Whether the publication ids and urls should be included in the response.")
      @QueryParam("withpublications") Boolean withPublications,
      @Parameter(description = "Whether internal publications should be included.")
      @QueryParam("includeInternalPublication") Boolean includeInternalPublication)
        throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    if (requestedVersion.isSmallerThan(VERSION_1_1_0)) {
      // withScheduling was added in version 1.1.0 and should be ignored for smaller versions
      withScheduling = false;
    }
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      event.updatePreview(previewSubtype);
      return ApiResponseBuilder.Json.ok(
          requestedVersion, eventToJSON(event, withAcl, withMetadata, withScheduling, withPublications, includeInternalPublication, sign, requestedVersion));
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/media")
  @RestQuery(name = "geteventmedia", description = "Returns media tracks of specific single event.", returnDescription = "", pathParameters = {
      @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, responses = {
      @RestResponse(description = "The event's media is returned.", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  @Operation(summary = "Get media tracks of a single event", description = "Returns media tracks of specific single event.")
  @Parameters({
      @Parameter(name = "eventId", description = "The event id", required = true, in = ParameterIn.PATH),
      @Parameter(name = "Accept", description = "The accept header", required = true, in = ParameterIn.HEADER)
  })
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "The event's media is returned."),
      @ApiResponse(responseCode = "404", description = "The specified event does not exist.")
  })
  public Response getEventMedia(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    List<TrackImpl> tracks = new ArrayList<>();

    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      final MediaPackage mp = indexService.getEventMediapackage(event);
      for (Track track : mp.getTracks()) {
        if (track instanceof TrackImpl) {
          tracks.add((TrackImpl) track);
        }
      }

      JsonArray tracksJson = new JsonArray();
      for (Track track : tracks) {
        JsonObject trackJson = new JsonObject();
        if (track.getChecksum() != null)
          trackJson.addProperty("checksum", track.getChecksum().toString());
        if (track.getDescription() != null)
          trackJson.addProperty("description", track.getDescription());
        if (track.getDuration() != null)
          trackJson.addProperty("duration", track.getDuration());
        if (track.getElementDescription() != null)
          trackJson.addProperty("element-description", track.getElementDescription());
        if (track.getFlavor() != null)
          trackJson.addProperty("flavor", track.getFlavor().toString());
        if (track.getIdentifier() != null)
          trackJson.addProperty("identifier", track.getIdentifier());
        if (track.getMimeType() != null)
          trackJson.addProperty("mimetype", track.getMimeType().toString());
        trackJson.addProperty("size", track.getSize());

        if (!requestedVersion.isSmallerThan(VERSION_1_7_0)) {
          trackJson.addProperty("has_video", track.hasVideo());
          trackJson.addProperty("has_audio", track.hasAudio());
          trackJson.addProperty("is_master_playlist", track.isMaster());
          trackJson.addProperty("is_live", track.isLive());
        }

        if (track.getStreams() != null) {
          JsonObject streamsJson = new JsonObject();
          for (Stream stream : track.getStreams()) {
            streamsJson.add(stream.getIdentifier(), getJsonStream(stream));
          }
          trackJson.add("streams", streamsJson);
        }

        trackJson.add("tags", arrayToJsonArray(track.getTags()));

        if (track.getURI() != null)
          trackJson.addProperty("uri", track.getURI().toString());

        tracksJson.add(trackJson);
      }

      return ApiResponseBuilder.Json.ok(acceptHeader, tracksJson);
    }

    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}")
  @RestQuery(name = "deleteevent", description = "Deletes an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, responses = {
          @RestResponse(description = "The event has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The retraction of publications has started.", responseCode = HttpServletResponse.SC_ACCEPTED),
          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws SearchIndexException, UnauthorizedException {
    final Opt<Event> event = indexService.getEvent(id, elasticsearchIndex);
    if (event.isNone()) {
      return RestUtil.R.notFound(id);
    }
    final IndexService.EventRemovalResult result;
    try {
      result = indexService.removeEvent(event.get(), retractWorkflowId);
    } catch (WorkflowDatabaseException e) {
      logger.error("Workflow database is not reachable. This may be a temporary problem.");
      return RestUtil.R.serverError();
    } catch (NotFoundException e) {
      logger.error("Configured retract workflow not found. Check your configuration.");
      return RestUtil.R.serverError();
    }
    switch (result) {
      case SUCCESS:
        return Response.noContent().build();
      case RETRACTING:
        return Response.accepted().build();
      case GENERAL_FAILURE:
        return Response.serverError().build();
      case NOT_FOUND:
        return RestUtil.R.notFound(id);
      default:
        throw new RuntimeException("Unknown EventRemovalResult type: " + result.name());
    }
  }

  @POST
  @Path("{eventId}")
  @RestQuery(name = "updateeventmetadata", description = "Updates an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = false, description = "A collection of roles with their possible action", type = Type.STRING),
                  @RestParameter(name = "metadata", isRequired = false, description = "Event metadata as Form param", type = Type.STRING),
                  @RestParameter(name = "scheduling", isRequired = false, description = "Scheduling information as Form param", type = Type.STRING),
                  @RestParameter(name = "presenter", isRequired = false, description = "Presenter movie track", type = Type.FILE),
                  @RestParameter(name = "presentation", isRequired = false, description = "Presentation movie track", type = Type.FILE),
                  @RestParameter(name = "audio", isRequired = false, description = "Audio track", type = Type.FILE),
                  @RestParameter(name = "processing", isRequired = false, description = "Processing instructions task configuration", type = Type.STRING), }, responses = {
                          @RestResponse(description = "The event has been updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The event could not be updated due to a scheduling conflict.", responseCode = HttpServletResponse.SC_CONFLICT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventMetadata(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request,
          @PathParam("eventId") String eventId) {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    try {
      String startDatePattern = configuredMetadataFields.containsKey("startDate") ? configuredMetadataFields.get("startDate").getPattern() : null;
      String startTimePattern = configuredMetadataFields.containsKey("startTime") ? configuredMetadataFields.get("startTime").getPattern() : null;
      for (final Event event : indexService.getEvent(eventId, elasticsearchIndex)) {
        EventHttpServletRequest eventHttpServletRequest = EventHttpServletRequest.updateFromHttpServletRequest(event,
                request, getEventCatalogUIAdapters(), startDatePattern, startTimePattern);

        // FIXME: All of these update operations should be a part of a transaction to avoid a partially updated event.
        if (eventHttpServletRequest.getMetadataList().isSome()) {
          indexService.updateEventMetadata(eventId, eventHttpServletRequest.getMetadataList().get(), elasticsearchIndex);
        }

        if (eventHttpServletRequest.getAcl().isSome()) {
          indexService.updateEventAcl(eventId, eventHttpServletRequest.getAcl().get(), elasticsearchIndex);
        }

        if (eventHttpServletRequest.getProcessing().isSome()) {

          if (!event.isScheduledEvent() || event.hasRecordingStarted()) {
            return RestUtil.R.badRequest("Processing can't be updated for events that are already uploaded.");
          }
          JSONObject processing = eventHttpServletRequest.getProcessing().get();

          String workflowId = (String) processing.get("workflow");
          if (workflowId == null)
            throw new IllegalArgumentException("No workflow template in metadata");

          Map<String, String> configuration = new HashMap<>();
          if (eventHttpServletRequest.getProcessing().get().get("configuration") != null) {
            configuration = new HashMap<>((JSONObject) eventHttpServletRequest.getProcessing().get().get("configuration"));
          }

          Opt<Map<String, String>> caMetadataOpt = Opt.none();
          Opt<Map<String, String>> workflowConfigOpt = Opt.none();

          Map<String, String> caMetadata = new HashMap<>(getSchedulerService().getCaptureAgentConfiguration(eventId));
          if (!workflowId.equals(caMetadata.get(CaptureParameters.INGEST_WORKFLOW_DEFINITION))) {
            caMetadata.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, workflowId);
            caMetadataOpt = Opt.some(caMetadata);
          }

          Map<String, String> oldWorkflowConfig = new HashMap<>(getSchedulerService().getWorkflowConfig(eventId));
          if (!oldWorkflowConfig.equals(configuration))
            workflowConfigOpt = Opt.some(configuration);

          if (!caMetadataOpt.isNone() || !workflowConfigOpt.isNone()) {
            getSchedulerService().updateEvent(eventId, Opt.none(), Opt.none(), Opt.none(),
                    Opt.none(), Opt.none(), workflowConfigOpt, caMetadataOpt);
          }
        }

        if (eventHttpServletRequest.getScheduling().isSome() && !requestedVersion.isSmallerThan(VERSION_1_1_0)) {
          // Scheduling is only available for version 1.1.0 and above
          Optional<Response> clientError = updateSchedulingInformation(
              eventHttpServletRequest.getScheduling().get(), eventId, requestedVersion, false);
          if (clientError.isPresent()) {
            return clientError.get();
          }
        }

        return Response.noContent().build();
      }
      return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", eventId);
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", eventId);
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to update event '{}'", eventId, e);
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      logger.error("Unable to get multi part fields or file for event '{}'", eventId, e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    } catch (SearchIndexException e) {
      logger.error("Unable to update event '{}'", eventId, e);
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "createevent", description = "Creates an event by sending metadata, access control list, processing instructions and files in a multipart request.", returnDescription = "", restParameters = {
          @RestParameter(name = "acl", isRequired = false, description = "A collection of roles with their possible action", type = STRING),
          @RestParameter(name = "metadata", description = "Event metadata as Form param", isRequired = false, type = STRING),
          @RestParameter(name = "scheduling", description = "Scheduling information as Form param", isRequired = false, type = STRING),
          @RestParameter(name = "presenter", description = "Presenter movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "presentation", description = "Presentation movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "audio", description = "Audio track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "processing", description = "Processing instructions task configuration", isRequired = false, type = STRING) }, responses = {
                  @RestResponse(description = "A new event is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The event could not be created due to a scheduling conflict.", responseCode = HttpServletResponse.SC_CONFLICT),
                  @RestResponse(description = "The request is invalid or inconsistent..", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createNewEvent(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request) {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    try {
      String startDatePattern = configuredMetadataFields.containsKey("startDate") ? configuredMetadataFields.get("startDate").getPattern() : null;
      String startTimePattern = configuredMetadataFields.containsKey("startTime") ? configuredMetadataFields.get("startTime").getPattern() : null;
      EventHttpServletRequest eventHttpServletRequest = EventHttpServletRequest.createFromHttpServletRequest(request,
          ingestService, getEventCatalogUIAdapters(), startDatePattern, startTimePattern);

      // If scheduling information is provided, the source has to be "SCHEDULE_SINGLE" or "SCHEDULE_MULTIPLE".
      if (eventHttpServletRequest.getScheduling().isSome() && !requestedVersion.isSmallerThan(VERSION_1_1_0)) {
        // Scheduling is only available for version 1.1.0 and above
        return scheduleNewEvent(eventHttpServletRequest, eventHttpServletRequest.getScheduling().get(), requestedVersion);
      }

      JSONObject source = new JSONObject();
      source.put("type", "UPLOAD");
      eventHttpServletRequest.setSource(source);
      String eventId = indexService.createEvent(eventHttpServletRequest);
      JsonObject json = new JsonObject();
      json.addProperty("identifier", eventId);
      return ApiResponseBuilder.Json.created(requestedVersion, URI.create(getEventUrl(eventId)), json);
    } catch (IllegalArgumentException | DateTimeParseException e) {
      logger.debug("Unable to create event", e);
      return RestUtil.R.badRequest(e.getMessage());
    } catch (SchedulerException | IndexServiceException e) {
      if (e.getCause() != null && e.getCause() instanceof NotFoundException
              || e.getCause() instanceof IllegalArgumentException) {
        logger.debug("Unable to create event", e);
        return RestUtil.R.badRequest(e.getCause().getMessage());
      } else {
        logger.error("Unable to create event", e);
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      logger.error("Unable to create event", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Response scheduleNewEvent(EventHttpServletRequest request, JSONObject scheduling, ApiVersion requestedVersion)
      throws MediaPackageException, IOException, IngestException, SchedulerException,
      NotFoundException, UnauthorizedException, SearchIndexException, java.text.ParseException {

    final SchedulingInfo schedulingInfo = SchedulingInfo.of(scheduling);
    final JSONObject source = schedulingInfo.toSource();
    request.setSource(source);

    try {
      final String eventId = indexService.createEvent(request);

      if (StringUtils.isEmpty(eventId)) {
        return RestUtil.R.badRequest("The date range provided did not include any events");
      }

      if (eventId.contains(",")) {
        // This the case when SCHEDULE_MULTIPLE is performed.
        JsonArray eventArray = new JsonArray();
        for (String id : eventId.split(",")) {
          JsonObject eventObj = new JsonObject();
          eventObj.addProperty("identifier", id);
          eventArray.add(eventObj);
        }
        return ApiResponseBuilder.Json.ok(requestedVersion, eventArray);
      }

      JsonObject eventJson = new JsonObject();
      eventJson.addProperty("identifier", eventId);
      return ApiResponseBuilder.Json.created(requestedVersion, URI.create(getEventUrl(eventId)), eventJson);
    } catch (SchedulerConflictException e) {
      List<MediaPackage> conflictingEvents =
          getConflictingEvents(schedulingInfo, agentStateService, schedulerService);
      logger.debug("Client tried to schedule conflicting event(s).");
      JsonArray conflictArray = new JsonArray();
      for (JsonObject conflict : convertConflictingEvents(
          Optional.empty(), conflictingEvents, indexService, elasticsearchIndex)) {
        conflictArray.add(conflict);
      }
      return ApiResponseBuilder.Json.conflict(requestedVersion, conflictArray);
    }
  }

  @GET
  @Path("/")
  @RestQuery(name = "getevents", description = "Returns a list of events. By setting the optional sign parameter to true, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the maximum validity of signed URLs when caching this response.", returnDescription = "", restParameters = {
          @RestParameter(name = "sign", isRequired = false, description = "Whether public distribution urls should be signed.", type = Type.BOOLEAN),
          @RestParameter(name = "withacl", isRequired = false, description = "Whether the acl metadata should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "withmetadata", isRequired = false, description = "Whether the metadata catalogs should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "withscheduling", isRequired = false, description = "Whether the scheduling information should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "withpublications", isRequired = false, description = "Whether the publication ids and urls should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "includeInternalPublication", description = "Whether internal publications should be included.", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "onlyWithWriteAccess", isRequired = false, description = "Whether only to get the events to which we have write access.", type = Type.BOOLEAN),
          @RestParameter(name = "filter", isRequired = false, description = "Usage [Filter Name]:[Value to Filter With]. Multiple filters can be used by combining them with commas \",\". Available Filters: presenters, contributors, location, textFilter, series, subject. If API ver > 1.1.0 also: identifier, title, description, series_name, language, created, license, rightsholder, is_part_of, source, status, agent_id, start, technical_start.", type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = RestParameter.Type.INTEGER) }, responses = {
                  @RestResponse(description = "A (potentially empty) list of events is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getEvents(@HeaderParam("Accept") String acceptHeader, @QueryParam("id") String id,
          @QueryParam("commentReason") String reasonFilter, @QueryParam("commentResolution") String resolutionFilter,
          @QueryParam("filter") List<String> filter, @QueryParam("sort") String sort, @QueryParam("offset") Integer offset,
          @QueryParam("limit") Integer limit, @QueryParam("sign") boolean sign, @QueryParam("withacl") Boolean withAcl,
          @QueryParam("withmetadata") Boolean withMetadata, @QueryParam("withscheduling") Boolean withScheduling,
          @QueryParam("onlyWithWriteAccess") Boolean onlyWithWriteAccess, @QueryParam("withpublications") Boolean withPublications, @QueryParam("includeInternalPublication") Boolean includeInternalPublication) {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    if (requestedVersion.isSmallerThan(VERSION_1_1_0)) {
      // withscheduling was added for version 1.1.0 and should be ignored for smaller versions.
      withScheduling = false;
    }

    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));
    EventSearchQuery query = new EventSearchQuery(getSecurityService().getOrganization().getId(),
            getSecurityService().getUser());
    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    //List of all events from the filters
    List<IndexObject> allEvents = new ArrayList<>();

    if (!isNullOrEmpty(filter)) {
      // API version 1.5.0: Additive filter
      if (!requestedVersion.isSmallerThan(ApiVersion.VERSION_1_5_0)) {
        filter = filter.subList(0,1);
      }
      for (String filterPart : filter) {
        // Parse the filters

        for (String f : filterPart.split(",")) {
          String[] filterTuple = f.split(":");
          if (filterTuple.length < 2) {
            logger.debug("No value for filter {} in filters list: {}", filterTuple[0], filter);
            continue;
          }

          String name = filterTuple[0];
          String value;

          if (!requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
            // MH-13038 - 1.1.0 and higher support colons in values
            value = f.substring(name.length() + 1);
          } else {
            value = filterTuple[1];
          }

          if ("presenters".equals(name)) {
            query.withPresenter(value);
          } else if ("contributors".equals(name)) {
            query.withContributor(value);
          } else if ("location".equals(name)) {
            query.withLocation(value);
          } else if ("textFilter".equals(name)) {
            query.withText(QueryPreprocessor.sanitize(value));
          } else if ("series".equals(name)) {
            query.withSeriesId(value);
          } else if ("subject".equals(name)) {
            query.withSubject(value);
          } else if (!requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
            // additional filters only available with Version 1.1.0 or higher
            if ("identifier".equals(name)) {
              query.withIdentifier(value);
            } else if ("title".equals(name)) {
              query.withTitle(value);
            } else if ("description".equals(name)) {
              query.withDescription(value);
            } else if ("series_name".equals(name)) {
              query.withSeriesName(value);
            } else if ("language".equals(name)) {
              query.withLanguage(value);
            } else if ("created".equals(name)) {
              query.withCreated(value);
            } else if ("license".equals(name)) {
              query.withLicense(value);
            } else if ("rightsholder".equals(name)) {
              query.withRights(value);
            } else if ("is_part_of".equals(name)) {
              query.withSeriesId(value);
            } else if ("source".equals(name)) {
              query.withSource(value);
            } else if ("status".equals(name)) {
              query.withEventStatus(value);
            } else if ("agent_id".equals(name)) {
              query.withAgentId(value);
            } else if ("start".equals(name)) {
              try {
                Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(value);
                query.withStartFrom(fromAndToCreationRange.getA());
                query.withStartTo(fromAndToCreationRange.getB());
              } catch (Exception e) {
                return RestUtil.R
                        .badRequest(String.format("Filter 'start' could not be parsed: %s", e.getMessage()));

              }
            } else if ("technical_start".equals(name)) {
              try {
                Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(value);
                query.withTechnicalStartFrom(fromAndToCreationRange.getA());
                query.withTechnicalStartTo(fromAndToCreationRange.getB());
              } catch (Exception e) {
                return RestUtil.R
                        .badRequest(String.format("Filter 'technical_start' could not be parsed: %s", e.getMessage()));

              }
            } else {
              logger.warn("Unknown filter criteria {}", name);
              return RestUtil.R.badRequest(String.format("Unknown filter criterion in request: %s", name));

            }
          }
        }

        if (optSort.isSome()) {
          ArrayList<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
          for (SortCriterion criterion : sortCriteria) {

            switch (criterion.getFieldName()) {
              case EventIndexSchema.TITLE:
                query.sortByTitle(criterion.getOrder());
                break;
              case EventIndexSchema.PRESENTER:
                query.sortByPresenter(criterion.getOrder());
                break;
              case EventIndexSchema.TECHNICAL_START:
              case "technical_date":
                query.sortByTechnicalStartDate(criterion.getOrder());
                break;
              case EventIndexSchema.TECHNICAL_END:
                query.sortByTechnicalEndDate(criterion.getOrder());
                break;
              case EventIndexSchema.START_DATE:
              case "date":
                query.sortByStartDate(criterion.getOrder());
                break;
              case EventIndexSchema.END_DATE:
                query.sortByEndDate(criterion.getOrder());
                break;
              case EventIndexSchema.WORKFLOW_STATE:
                query.sortByWorkflowState(criterion.getOrder());
                break;
              case EventIndexSchema.SERIES_NAME:
                query.sortBySeriesName(criterion.getOrder());
                break;
              case EventIndexSchema.LOCATION:
                query.sortByLocation(criterion.getOrder());
                break;
              // For compatibility, we mimic to support the old review_status and scheduling_status sort criteria (MH-13407)
              case "review_status":
              case "scheduling_status":
                break;
              default:
                return RestUtil.R.badRequest(String.format("Unknown sort criterion in request: %s", criterion.getFieldName()));
            }
          }
        }

        // TODO: Add the comment resolution filter to the query
        if (StringUtils.isNotBlank(resolutionFilter)) {
          try {
            CommentResolution.valueOf(resolutionFilter);
          } catch (Exception e) {
            logger.debug("Unable to parse comment resolution filter {}", resolutionFilter);
            return Response.status(Status.BAD_REQUEST).build();
          }
        }

        if (optLimit.isSome())
          query.withLimit(optLimit.get());
        if (optOffset.isSome())
          query.withOffset(offset);
        // TODO: Add other filters to the query

        SearchResult<Event> results = null;
        try {
          results = elasticsearchIndex.getByQuery(query);
        } catch (SearchIndexException e) {
          logger.error("The External Search Index was not able to get the events list", e);
          throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

        SearchResultItem<Event>[] items = results.getItems();
        List<IndexObject> events = new ArrayList<>();
        for (SearchResultItem<Event> item : items) {
          Event source = item.getSource();
          source.updatePreview(previewSubtype);
          events.add(source);
        }
        //Append  filtered results to the list
        allEvents.addAll(events);
      }
    } else {
      if (optSort.isSome()) {
        ArrayList<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
        for (SortCriterion criterion : sortCriteria) {

          switch (criterion.getFieldName()) {
            case EventIndexSchema.TITLE:
              query.sortByTitle(criterion.getOrder());
              break;
            case EventIndexSchema.PRESENTER:
              query.sortByPresenter(criterion.getOrder());
              break;
            case EventIndexSchema.TECHNICAL_START:
            case "technical_date":
              query.sortByTechnicalStartDate(criterion.getOrder());
              break;
            case EventIndexSchema.TECHNICAL_END:
              query.sortByTechnicalEndDate(criterion.getOrder());
              break;
            case EventIndexSchema.START_DATE:
            case "date":
              query.sortByStartDate(criterion.getOrder());
              break;
            case EventIndexSchema.END_DATE:
              query.sortByEndDate(criterion.getOrder());
              break;
            case EventIndexSchema.WORKFLOW_STATE:
              query.sortByWorkflowState(criterion.getOrder());
              break;
            case EventIndexSchema.SERIES_NAME:
              query.sortBySeriesName(criterion.getOrder());
              break;
            case EventIndexSchema.LOCATION:
              query.sortByLocation(criterion.getOrder());
              break;
            // For compatibility, we mimic to support the old review_status and scheduling_status sort criteria (MH-13407)
            case "review_status":
            case "scheduling_status":
              break;
            default:
              return RestUtil.R.badRequest(String.format("Unknown sort criterion in request: %s", criterion.getFieldName()));
          }
        }
      }

      // TODO: Add the comment resolution filter to the query
      if (StringUtils.isNotBlank(resolutionFilter)) {
        try {
          CommentResolution.valueOf(resolutionFilter);
        } catch (Exception e) {
          logger.debug("Unable to parse comment resolution filter {}", resolutionFilter);
          return Response.status(Status.BAD_REQUEST).build();
        }
      }

      if (optLimit.isSome())
        query.withLimit(optLimit.get());
      if (optOffset.isSome())
        query.withOffset(offset);

      if (onlyWithWriteAccess != null && onlyWithWriteAccess) {
        query.withoutActions();
        query.withAction(Permissions.Action.WRITE);
      }
      // TODO: Add other filters to the query

      SearchResult<Event> results = null;
      try {
        results = elasticsearchIndex.getByQuery(query);
      } catch (SearchIndexException e) {
        logger.error("The External Search Index was not able to get the events list", e);
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
      }

      SearchResultItem<Event>[] items = results.getItems();
      List<IndexObject> events = new ArrayList<>();
      for (SearchResultItem<Event> item : items) {
        Event source = item.getSource();
        source.updatePreview(previewSubtype);
        events.add(source);
      }
      //Append  filtered results to the list
      allEvents.addAll(events);
    }
    try {
      return getJsonEvents(
          acceptHeader, allEvents, withAcl, withMetadata, withScheduling, withPublications, includeInternalPublication, sign, requestedVersion);
    } catch (Exception e) {
      logger.error("Unable to get events", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Render a collection of {@link Event}s into a json array.
   *
   * @param acceptHeader
   *          The accept header to return to the client.
   * @param events
   *          The {@link List} of {@link Event}s to render into json.
   * @param withAcl
   *          Whether to include the events' ACLs.
   * @param withMetadata
   *          Whether to include the events' metadata.
   * @param withScheduling
   *          Whether to include the events' scheduling information.
   * @param withPublications
   *          Whether to include the events' publications.
   * @param withSignedUrls
   *          Whether to sign the included urls.
   * @return A {@link Response} with the accept header and body as the Json array of {@link Event}s.
   * @throws IndexServiceException
   * @throws SchedulerException
   * @throws UnauthorizedException
   */
  protected Response getJsonEvents(String acceptHeader, List<IndexObject> events, Boolean withAcl, Boolean withMetadata,
      Boolean withScheduling, Boolean withPublications, Boolean includeInternalPublication, Boolean withSignedUrls, ApiVersion requestedVersion)
      throws IndexServiceException, UnauthorizedException, SchedulerException {
    JsonArray eventsArray = new JsonArray();
    for (IndexObject item : events) {
      JsonObject jsonEvent = eventToJSON((Event) item, withAcl, withMetadata, withScheduling, withPublications,
          includeInternalPublication, withSignedUrls, requestedVersion);
      eventsArray.add(jsonEvent);
    }

    return ApiResponseBuilder.Json.ok(requestedVersion, eventsArray);
  }

  /**
   * Transform an {@link Event} to Json
   *
   * @param event
   *          The event to transform into json
   * @param withAcl
   *          Whether to add the acl information for the event
   * @param withMetadata
   *          Whether to add all the metadata for the event
   * @param withScheduling
   *          Whether to add the scheduling information for the event
   * @param withPublications
   *          Whether to add the publications
   * @param withSignedUrls
   *          Whether to sign the urls if they are protected by stream security.
   * @return The event in json format.
   * @throws IndexServiceException
   *           Thrown if unable to get the metadata for the event.
   * @throws SchedulerException
   * @throws UnauthorizedException
   */
  protected JsonObject eventToJSON(Event event, Boolean withAcl, Boolean withMetadata, Boolean withScheduling,
      Boolean withPublications, Boolean includeInternalPublication, Boolean withSignedUrls,
      ApiVersion requestedVersion) throws IndexServiceException, SchedulerException, UnauthorizedException {
    JsonObject json = new JsonObject();

    if (event.getArchiveVersion() != null)
      json.addProperty("archive_version", event.getArchiveVersion());
    json.addProperty("created", safeString(event.getCreated()));
    json.addProperty("creator", safeString(event.getCreator()));
    json.add("contributor", collectionToJsonArray(event.getContributors()));
    json.addProperty("description", safeString(event.getDescription()));
    json.addProperty("has_previews", event.hasPreview());
    json.addProperty("identifier", safeString(event.getIdentifier()));
    json.addProperty("location", safeString(event.getLocation()));
    json.add("presenter", collectionToJsonArray(event.getPresenters()));

    if (!requestedVersion.isSmallerThan(VERSION_1_1_0)) {
      json.addProperty("language", safeString(event.getLanguage()));
      json.addProperty("rightsholder", safeString(event.getRights()));
      json.addProperty("license", safeString(event.getLicense()));
      json.addProperty("is_part_of", safeString(event.getSeriesId()));
      json.addProperty("series", safeString(event.getSeriesName()));
      json.addProperty("source", safeString(event.getSource()));
      json.addProperty("status", safeString(event.getEventStatus()));
    }

    JsonArray publicationIds = new JsonArray();
    if (event.getPublications() != null) {
      for (Publication publication : event.getPublications()) {
        publicationIds.add(new JsonPrimitive(publication.getChannel()));
      }
    }
    json.add("publication_status", publicationIds);
    json.addProperty("processing_state", safeString(event.getWorkflowState()));

    if (requestedVersion.isSmallerThan(VERSION_1_4_0)) {
      json.addProperty("start", safeString(event.getTechnicalStartTime()));
      if (event.getTechnicalEndTime() != null) {
        long duration = new DateTime(event.getTechnicalEndTime()).getMillis()
            - new DateTime(event.getTechnicalStartTime()).getMillis();
        json.addProperty("duration", duration);
      }
    } else {
      json.addProperty("start", safeString(event.getRecordingStartDate()));
      if (event.getDuration() != null) {
        json.addProperty("duration", event.getDuration());
      } else {
        json.add("duration", JsonNull.INSTANCE);
      }
    }

    if (StringUtils.trimToNull(event.getSubject()) != null) {
      json.add("subjects", splitSubjectIntoArray(event.getSubject()));
    } else {
      json.add("subjects", new JsonArray());
    }

    json.addProperty("title", safeString(event.getTitle()));

    if (withAcl != null && withAcl) {
      AccessControlList acl = getAclFromEvent(event);
      json.add("acl", AclUtils.serializeAclToJson(acl));
    }

    if (withMetadata != null && withMetadata) {
      try {
        Opt<MetadataList> metadata = getEventMetadata(event);
        if (metadata.isSome()) {
          json.add("metadata", MetadataJson.listToJson(metadata.get(), true));
        }
      } catch (Exception e) {
        logger.error("Unable to get metadata for event '{}'", event.getIdentifier(), e);
        throw new IndexServiceException("Unable to add metadata to event", e);
      }
    }

    if (withScheduling != null && withScheduling) {
      json.add("scheduling", SchedulingInfo.of(event.getIdentifier(), schedulerService).toJson());
    }

    if (withPublications != null && withPublications) {
      List<JsonObject> publications = getPublications(event, withSignedUrls, includeInternalPublication, requestedVersion);
      JsonArray pubDetails = new JsonArray();
      for (JsonObject pub : publications) {
        pubDetails.add(pub);
      }
      json.add("publications", pubDetails);
    }

    return json;
  }

  private JsonArray splitSubjectIntoArray(final String subject) {
    JsonArray array = new JsonArray();
    if (subject != null && !subject.trim().isEmpty()) {
      for (String part : subject.split(",")) {
        array.add(new JsonPrimitive(part.trim()));
      }
    }
    return array;
  }

  @GET
  @Path("{eventId}/acl")
  @RestQuery(name = "geteventacl", description = "Returns an event's access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, responses = {
                  @RestResponse(description = "The access control list for the specified event is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws Exception {
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      AccessControlList acl = getAclFromEvent(event);
      return ApiResponseBuilder.Json.ok(acceptHeader, AclUtils.serializeAclToJson(acl));
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @PUT
  @Path("{eventId}/acl")
  @RestQuery(name = "updateeventacl", description = "Update an event's access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = true, description = "Access policy", type = STRING) }, responses = {
                          @RestResponse(description = "The access control list for the specified event is updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @FormParam("acl") String acl) throws Exception {
    if (indexService.getEvent(id, elasticsearchIndex).isSome()) {
      AccessControlList accessControlList;
      try {
        accessControlList = AclUtils.deserializeJsonToAcl(acl, false);
      } catch (ParseException e) {
        logger.debug("Unable to update event acl to '{}'", acl, e);
        return R.badRequest(String.format("Unable to parse acl '%s' because '%s'", acl, e.getMessage()));
      } catch (IllegalArgumentException e) {
        logger.debug("Unable to update event acl to '{}'", acl, e);
        return R.badRequest(e.getMessage());
      }
      try {
        accessControlList = indexService.updateEventAcl(id, accessControlList, elasticsearchIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to update event '{}' acl with '{}'", id, acl, e);
        return Response.status(Status.FORBIDDEN).build();
      }
      return Response.noContent().build();
    } else {
      return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
    }
  }

  @POST
  @Path("{eventId}/acl/{action}")
  @RestQuery(name = "addeventace", description = "Grants permission to execute action on the specified event to any user with role role. Note that this is a convenience method to avoid having to build and post a complete access control list.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
          @RestParameter(name = "action", description = "The action that is allowed to be executed", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "role", isRequired = true, description = "The role that is granted permission", type = STRING) }, responses = {
                          @RestResponse(description = "The permission has been created in the access control list of the specified event.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response addEventAce(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @PathParam("action") String action, @FormParam("role") String role) throws Exception {
    List<AccessControlEntry> entries = new ArrayList<>();
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      AccessControlList accessControlList = getAclFromEvent(event);
      AccessControlEntry newAce = new AccessControlEntry(role, action, true);
      boolean alreadyInAcl = false;
      for (AccessControlEntry ace : accessControlList.getEntries()) {
        if (ace.equals(newAce)) {
          // We have found an identical access control entry so just return.
          entries = accessControlList.getEntries();
          alreadyInAcl = true;
          break;
        } else if (ace.getAction().equals(newAce.getAction()) && ace.getRole().equals(newAce.getRole())
                && !ace.isAllow()) {
          entries.add(newAce);
          alreadyInAcl = true;
        } else {
          entries.add(ace);
        }
      }

      if (!alreadyInAcl) {
        entries.add(newAce);
      }

      AccessControlList withNewAce = new AccessControlList(entries);
      try {
        withNewAce = indexService.updateEventAcl(id, withNewAce, elasticsearchIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to update event '{}' acl entry with action '{}' and role '{}'", id, action, role, e);
        return Response.status(Status.FORBIDDEN).build();
      }
      return Response.noContent().build();
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}/acl/{action}/{role}")
  @RestQuery(name = "deleteeventace", description = "Revokes permission to execute action on the specified event from any user with role role.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
          @RestParameter(name = "action", description = "The action that is no longer allowed to be executed", isRequired = true, type = STRING),
          @RestParameter(name = "role", description = "The role that is no longer granted permission", isRequired = true, type = STRING) }, responses = {
                  @RestResponse(description = "The permission has been revoked from the access control list of the specified event.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEventAce(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @PathParam("action") String action, @PathParam("role") String role) throws Exception {
    List<AccessControlEntry> entries = new ArrayList<>();
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      AccessControlList accessControlList = getAclFromEvent(event);
      boolean foundDelete = false;
      for (AccessControlEntry ace : accessControlList.getEntries()) {
        if (ace.getAction().equals(action) && ace.getRole().equals(role)) {
          foundDelete = true;
        } else {
          entries.add(ace);
        }
      }

      if (!foundDelete) {
        return ApiResponseBuilder.notFound("Unable to find an access control entry with action '%s' and role '%s'", action,
                role);
      }

      AccessControlList withoutDeleted = new AccessControlList(entries);
      try {
        withoutDeleted = indexService.updateEventAcl(id, withoutDeleted, elasticsearchIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to delete event's '{}' acl entry with action '{}' and role '{}'", id, action, role, e);
        return Response.status(Status.FORBIDDEN).build();
      }
      return Response.noContent().build();
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/metadata")
  @RestQuery(name = "geteventmetadata", description = "Returns the event's metadata of the specified type. For a metadata catalog there is the flavor such as 'dublincore/episode' and this is the unique type.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = false, description = "The type of metadata to get", type = STRING) }, responses = {
                          @RestResponse(description = "The metadata collection is returned.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getAllEventMetadata(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type) throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    if (StringUtils.trimToNull(type) == null) {
      Opt<MetadataList> metadataList = getEventMetadataById(id);
      if (metadataList.isSome()) {
        MetadataList actualList = metadataList.get();

        // API v1 should return a two separate fields for start date and start time. Since those fields were merged in index service, we have to split them up.
        final DublinCoreMetadataCollection collection = actualList.getMetadataByFlavor("dublincore/episode");
        final boolean withOrderedText = collection == null;
        if (collection != null) {
          convertStartDateTimeToApiV1(collection);
        }

        return ApiResponseBuilder.Json.ok(requestedVersion, MetadataJson.listToJson(actualList, withOrderedText));
      }
      else
        return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
    } else {
      return getEventMetadataByType(id, type, requestedVersion);
    }
  }

  private void convertStartDateTimeToApiV1(DublinCoreMetadataCollection collection) throws java.text.ParseException {

    if (!collection.getOutputFields().containsKey("startDate")) return;

    MetadataField oldStartDateField = collection.getOutputFields().get("startDate");
    SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(oldStartDateField.getPattern());
    Date startDate = sdf.parse((String) oldStartDateField.getValue());

    if (configuredMetadataFields.containsKey("startDate")) {
      MetadataField startDateField = configuredMetadataFields.get("startDate");
      final String pattern = startDateField.getPattern() == null ? "yyyy-MM-dd" : startDateField.getPattern();
      startDateField = new MetadataField(startDateField);
      startDateField.setPattern(pattern);
      sdf.applyPattern(startDateField.getPattern());
      startDateField.setValue(sdf.format(startDate));
      collection.removeField(oldStartDateField);
      collection.addField(startDateField);
    }

    if (configuredMetadataFields.containsKey("startTime")) {
      MetadataField startTimeField = configuredMetadataFields.get("startTime");
      final String pattern = startTimeField.getPattern() == null ? "HH:mm" : startTimeField.getPattern();
      startTimeField = new MetadataField(startTimeField);
      startTimeField.setPattern(pattern);
      sdf.applyPattern(startTimeField.getPattern());
      startTimeField.setValue(sdf.format(startDate));
      collection.addField(startTimeField);
    }
  }

  protected Opt<MetadataList> getEventMetadataById(String id) throws IndexServiceException, Exception {
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      return getEventMetadata(event);
    }
    return Opt.<MetadataList> none();
  }

  protected Opt<MetadataList> getEventMetadata(Event event) throws IndexServiceException, Exception {
    MetadataList metadataList = new MetadataList();
    List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
    EventCatalogUIAdapter eventCatalogUIAdapter = indexService.getCommonEventCatalogUIAdapter();
    catalogUIAdapters.remove(eventCatalogUIAdapter);
    if (catalogUIAdapters.size() > 0) {
      MediaPackage mediaPackage = indexService.getEventMediapackage(event);
      for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
        // TODO: This is very slow:
        DublinCoreMetadataCollection fields = catalogUIAdapter.getFields(mediaPackage);
        if (fields != null) {
          ExternalMetadataUtils.removeCollectionList(fields);
          metadataList.add(catalogUIAdapter, fields);
        }
      }
    }
    DublinCoreMetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter,
        new EmptyResourceListQuery());
    ExternalMetadataUtils.changeSubjectToSubjects(collection);
    ExternalMetadataUtils.removeCollectionList(collection);
    metadataList.add(eventCatalogUIAdapter, collection);
    if (WorkflowInstance.WorkflowState.RUNNING.toString().equals(event.getWorkflowState())) {
      metadataList.setLocked(Locked.WORKFLOW_RUNNING);
    }
    return Opt.some(metadataList);
  }

  private Opt<MediaPackageElementFlavor> getFlavor(String flavorString) {
    try {
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
      return Opt.some(flavor);
    } catch (IllegalArgumentException e) {
      return Opt.none();
    }
  }

  private Response getEventMetadataByType(String id, String type, ApiVersion requestedVersion) throws Exception {
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      Opt<MediaPackageElementFlavor> flavor = getFlavor(type);
      if (flavor.isNone()) {
        return R.badRequest(
                String.format("Unable to parse type '%s' as a flavor so unable to find the matching catalog.", type));
      }
      // Try the main catalog first as we load it from the index.
      EventCatalogUIAdapter eventCatalogUIAdapter = indexService.getCommonEventCatalogUIAdapter();
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        DublinCoreMetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter,
            new EmptyResourceListQuery());
        ExternalMetadataUtils.changeSubjectToSubjects(collection);
        ExternalMetadataUtils.removeCollectionList(collection);
        convertStartDateTimeToApiV1(collection);
        return ApiResponseBuilder.Json.ok(requestedVersion, MetadataJson.collectionToJson(collection, false));
      }
      // Try the other catalogs
      List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
      catalogUIAdapters.remove(eventCatalogUIAdapter);
      if (catalogUIAdapters.size() > 0) {
        MediaPackage mediaPackage = indexService.getEventMediapackage(event);
        for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
          if (flavor.get().equals(catalogUIAdapter.getFlavor())) {
            DublinCoreMetadataCollection fields = catalogUIAdapter.getFields(mediaPackage);
            ExternalMetadataUtils.removeCollectionList(fields);
            convertStartDateTimeToApiV1(fields);
            return ApiResponseBuilder.Json.ok(requestedVersion, MetadataJson.collectionToJson(fields, false));
          }
        }
      }
      return ApiResponseBuilder.notFound("Cannot find a catalog with type '%s' for event with id '%s'.", type, id);
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @PUT
  @Path("{eventId}/metadata")
  @RestQuery(name = "updateeventmetadata", description = "Update the metadata with the matching type of the specified event. For a metadata catalog there is the flavor such as 'dublincore/episode' and this is the unique type.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to update", type = STRING),
                  @RestParameter(name = "metadata", description = "Metadata catalog in JSON format", isRequired = true, type = STRING) }, responses = {
                          @RestResponse(description = "The metadata of the given namespace has been updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventMetadataByType(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type, @FormParam("metadata") String metadataJSON) throws Exception {
    Map<String, String> updatedFields;
    JSONParser parser = new JSONParser();
    try {
      updatedFields = RequestUtils.getKeyValueMap(metadataJSON);
    } catch (ParseException e) {
      logger.debug("Unable to update event '{}' with metadata type '{}' and content '{}'", id, type, metadataJSON, e);
      return RestUtil.R.badRequest(String.format("Unable to parse metadata fields as json from '%s'", metadataJSON));
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to update event '{}' with metadata type '{}' and content '{}'", id, type, metadataJSON, e);
      return RestUtil.R.badRequest(e.getMessage());
    }

    if (updatedFields == null || updatedFields.size() == 0) {
      return RestUtil.R.badRequest(
              String.format("Unable to parse metadata fields as json from '%s' because there were no fields to update.",
                      metadataJSON));
    }

    Opt<MediaPackageElementFlavor> flavor = getFlavor(type);
    if (flavor.isNone()) {
      return R.badRequest(
              String.format("Unable to parse type '%s' as a flavor so unable to find the matching catalog.", type));
    }

    DublinCoreMetadataCollection collection = null;
    EventCatalogUIAdapter adapter = null;
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      MetadataList metadataList = new MetadataList();
      // Try the main catalog first as we load it from the index.
      EventCatalogUIAdapter eventCatalogUIAdapter = indexService.getCommonEventCatalogUIAdapter();
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter);
        adapter = eventCatalogUIAdapter;
      } else {
        metadataList.add(eventCatalogUIAdapter, EventUtils.getEventMetadata(event, eventCatalogUIAdapter));
      }

      // Try the other catalogs
      List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
      catalogUIAdapters.remove(eventCatalogUIAdapter);
      if (catalogUIAdapters.size() > 0) {
        MediaPackage mediaPackage = indexService.getEventMediapackage(event);
        for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
          if (flavor.get().equals(catalogUIAdapter.getFlavor())) {
            collection = catalogUIAdapter.getFields(mediaPackage);
            adapter = eventCatalogUIAdapter;
          } else {
            metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage));
          }
        }
      }

      if (collection == null) {
        return ApiResponseBuilder.notFound("Cannot find a catalog with type '%s' for event with id '%s'.", type, id);
      }

      for (String key : updatedFields.keySet()) {
        if ("subjects".equals(key)) {
          MetadataField field = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
          Opt<Response> error = validateField(field, key, id, type, updatedFields);
          if (error.isSome()) {
            return error.get();
          }
          collection.removeField(field);
          JSONArray subjectArray = (JSONArray) parser.parse(updatedFields.get(key));
          collection.addField(
                  MetadataJson.copyWithDifferentJsonValue(field, StringUtils.join(subjectArray.iterator(), ",")));
        } else if ("startDate".equals(key)) {
          // Special handling for start date since in API v1 we expect start date and start time to be separate fields.
          MetadataField field = collection.getOutputFields().get(key);
          Opt<Response> error = validateField(field, key, id, type, updatedFields);
          if (error.isSome()) {
            return error.get();
          }
          String apiPattern = field.getPattern();
          if (configuredMetadataFields.containsKey("startDate")) {
            final String startDate = configuredMetadataFields.get("startDate").getPattern();
            apiPattern = startDate == null ? apiPattern : startDate;
          }
          SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(apiPattern);
          SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern());
          DateTime oldStartDate = new DateTime(sdf.parse((String) field.getValue()), DateTimeZone.UTC);
          DateTime newStartDate = new DateTime(apiSdf.parse(updatedFields.get(key)), DateTimeZone.UTC);
          DateTime updatedStartDate = oldStartDate.withDate(newStartDate.year().get(), newStartDate.monthOfYear().get(), newStartDate.dayOfMonth().get());
          collection.removeField(field);
          collection.addField(
                  MetadataJson.copyWithDifferentJsonValue(field, sdf.format(updatedStartDate.toDate())));
        } else if ("startTime".equals(key)) {
          // Special handling for start time since in API v1 we expect start date and start time to be separate fields.
          MetadataField field = collection.getOutputFields().get("startDate");
          Opt<Response> error = validateField(field, "startDate", id, type, updatedFields);
          if (error.isSome()) {
            return error.get();
          }
          String apiPattern = "HH:mm";
          if (configuredMetadataFields.containsKey("startTime")) {
            final String startTime = configuredMetadataFields.get("startTime").getPattern();
            apiPattern = startTime == null ? apiPattern : startTime;
          }
          SimpleDateFormat apiSdf = MetadataField.getSimpleDateFormatter(apiPattern);
          SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(field.getPattern());
          DateTime oldStartDate = new DateTime(sdf.parse((String) field.getValue()), DateTimeZone.UTC);
          DateTime newStartDate = new DateTime(apiSdf.parse(updatedFields.get(key)), DateTimeZone.UTC);
          DateTime updatedStartDate = oldStartDate.withTime(
                  newStartDate.hourOfDay().get(),
                  newStartDate.minuteOfHour().get(),
                  newStartDate.secondOfMinute().get(),
                  newStartDate.millisOfSecond().get());
          collection.removeField(field);
          collection.addField(
                  MetadataJson.copyWithDifferentJsonValue(field, sdf.format(updatedStartDate.toDate())));
        } else {
          MetadataField field = collection.getOutputFields().get(key);
          Opt<Response> error = validateField(field, key, id, type, updatedFields);
          if (error.isSome()) {
            return error.get();
          }
          collection.removeField(field);
          collection.addField(
                  MetadataJson.copyWithDifferentJsonValue(field, updatedFields.get(key)));
        }
      }

      metadataList.add(adapter, collection);
      indexService.updateEventMetadata(id, metadataList, elasticsearchIndex);
      return Response.noContent().build();
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  private Opt<Response> validateField(MetadataField field, String key, String id, String type, Map<String, String> updatedFields) {
    if (field == null) {
      return Opt.some(ApiResponseBuilder.notFound(
              "Cannot find a metadata field with id '%s' from event with id '%s' and the metadata type '%s'.",
              key, id, type));
    } else if (field.isRequired() && StringUtils.isBlank(updatedFields.get(key))) {
      return Opt.some(R.badRequest(String.format(
              "The event metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
              key, type)));
    }
    return Opt.none();
  }

  @DELETE
  @Path("{eventId}/metadata")
  @RestQuery(name = "deleteeventmetadata", description = "Delete the metadata namespace catalog of the specified event. This will remove all fields and values of the catalog.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to delete", type = STRING) }, responses = {
                          @RestResponse(description = "The metadata of the given namespace has been updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The main metadata catalog dublincore/episode cannot be deleted as it has mandatory fields.", responseCode = HttpServletResponse.SC_FORBIDDEN),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEventMetadataByType(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type) throws SearchIndexException {
    for (final Event event : indexService.getEvent(id, elasticsearchIndex)) {
      Opt<MediaPackageElementFlavor> flavor = getFlavor(type);
      if (flavor.isNone()) {
        return R.badRequest(
                String.format("Unable to parse type '%s' as a flavor so unable to find the matching catalog.", type));
      }
      EventCatalogUIAdapter eventCatalogUIAdapter = indexService.getCommonEventCatalogUIAdapter();
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        return Response
                .status(Status.FORBIDDEN).entity(String
                        .format("Unable to delete mandatory metadata catalog with type '%s' for event '%s'", type, id))
                .build();
      }
      try {
        indexService.removeCatalogByFlavor(event, flavor.get());
      } catch (NotFoundException e) {
        return ApiResponseBuilder.notFound(e.getMessage());
      } catch (IndexServiceException e) {
        logger.error("Unable to remove metadata catalog with type '{}' from event '{}'", type, id, e);
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
      } catch (IllegalStateException e) {
        logger.debug("Unable to remove metadata catalog with type '{}' from event '{}'", type, id, e);
        throw new WebApplicationException(e, Status.BAD_REQUEST);
      } catch (UnauthorizedException e) {
        return Response.status(Status.UNAUTHORIZED).build();
      }
      return Response.noContent().build();
    }
    return ApiResponseBuilder.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/publications")
  @RestQuery(name = "geteventpublications", description = "Returns an event's list of publications.",
             returnDescription = "",
             pathParameters = {
               @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING)
             },
             restParameters = {
               @RestParameter(name = "sign", description = "Whether public distribution urls should be signed.",
                              isRequired = false, type = Type.BOOLEAN),
               @RestParameter(name = "includeInternalPublication", description = "Whether internal publications should be included.",
                              isRequired = false, type = Type.BOOLEAN)
             },
             responses = {
                  @RestResponse(description = "The list of publications is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })

    public Response getEventPublications(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("sign") boolean sign, @QueryParam("includeInternalPublication") boolean includeInternalPublication) throws Exception {
    try {
      final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
      final Opt<Event> event = indexService.getEvent(id, elasticsearchIndex);
      if (event.isSome()) {
        JsonArray jsonArray = new JsonArray();
        for (JsonElement pub : getPublications(event.get(), sign, includeInternalPublication, requestedVersion)) {
          jsonArray.add(pub);
        }
        return ApiResponseBuilder.Json.ok(acceptHeader, jsonArray);
      } else {
        return ApiResponseBuilder.notFound(String.format("Unable to find event with id '%s'", id));
      }
    } catch (SearchIndexException e) {
      logger.error("Unable to get list of publications from event with id '{}'", id, e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  private List<JsonObject> getPublications(Event event, Boolean withSignedUrls, Boolean includeInternalPublication, ApiVersion requestedVersion) {
    return event.getPublications().stream()
        .filter(publication -> {
          boolean isInternalAllowed = includeInternalPublication != null && includeInternalPublication && !requestedVersion.isSmallerThan(VERSION_1_11_0);
          return isInternalAllowed || EventUtils.internalChannelFilter.test(publication);
        })
        .map(p -> getPublication(p, withSignedUrls, requestedVersion))
        .collect(Collectors.toList());
  }

  public JsonObject getPublication(Publication publication, Boolean sign, ApiVersion requestedVersion) {
    // Signing URLs introduced in version 1.7.0
    URI publicationUrl = publication.getURI();
    if (!requestedVersion.isSmallerThan(VERSION_1_7_0)) {
      publicationUrl = getSignedUrl(publicationUrl, sign);
    }

    JsonObject json = new JsonObject();
    json.addProperty("id", publication.getIdentifier());
    json.addProperty("channel", publication.getChannel());
    json.addProperty("mediatype", safeString(publication.getMimeType()));
    json.addProperty("url", publicationUrl != null ? publicationUrl.toString() : "");
    JsonArray mediaArray = new JsonArray();
    for (JsonObject trackJson : getPublicationTracksJson(publication, sign, requestedVersion)) {
      mediaArray.add(trackJson);
    }
    json.add("media", mediaArray);
    JsonArray attachmentArray = new JsonArray();
    for (JsonObject attachmentJson : getPublicationAttachmentsJson(publication, sign)) {
      attachmentArray.add(attachmentJson);
    }
    json.add("attachments", attachmentArray);
    JsonArray metadataArray = new JsonArray();
    for (JsonObject catalogJson : getPublicationCatalogsJson(publication, sign)) {
      metadataArray.add(catalogJson);
    }
    json.add("metadata", metadataArray);

    return json;
  }

  private URI getSignedUrl(URI url, boolean sign) {
    if (url == null || !sign) {
      return url;
    }

    if (urlSigningService.accepts(url.toString())) {
      try {
        return URI.create(urlSigningService.sign(url.toString(), expireSeconds, null, null));
      } catch (UrlSigningException e) {
        logger.error("Unable to sign URI {}", url, e);
      }
    }
    return url;
  }

  private List<JsonObject> getPublicationTracksJson(Publication publication, Boolean sign, ApiVersion requestedVersion) {
    List<JsonObject> tracksJson = new ArrayList<>();

    for (Track track : publication.getTracks()) {
      JsonObject trackJson = new JsonObject();

      trackJson.addProperty("id", safeString(track.getIdentifier()));
      trackJson.addProperty("mediatype", safeString(track.getMimeType()));
      trackJson.addProperty("url", safeString(getSignedUrl(track.getURI(), sign)));
      trackJson.addProperty("flavor", safeString(track.getFlavor()));
      trackJson.addProperty("size", track.getSize());
      trackJson.addProperty("checksum", safeString(track.getChecksum()));
      trackJson.add("tags", arrayToJsonArray(track.getTags()));
      trackJson.addProperty("has_audio", track.hasAudio());
      trackJson.addProperty("has_video", track.hasVideo());
      trackJson.addProperty("duration", track.getDuration() != null ? track.getDuration() : null);
      trackJson.addProperty("description", safeString(track.getDescription()));

      VideoStream[] videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
      if (videoStreams.length > 0) {
        // Only supporting one stream, like in many other places...
        VideoStream videoStream = videoStreams[0];
        if (videoStream.getBitRate() != null)
          trackJson.addProperty("bitrate", videoStream.getBitRate());
        if (videoStream.getFrameRate() != null)
          trackJson.addProperty("framerate", videoStream.getFrameRate());
        if (videoStream.getFrameCount() != null)
          trackJson.addProperty("framecount", videoStream.getFrameCount());
        if (videoStream.getFrameWidth() != null)
          trackJson.addProperty("width", videoStream.getFrameWidth());
        if (videoStream.getFrameHeight() != null)
          trackJson.addProperty("height", videoStream.getFrameHeight());
      }

      if (!requestedVersion.isSmallerThan(VERSION_1_7_0)) {
        trackJson.addProperty("is_master_playlist", track.isMaster());
        trackJson.addProperty("is_live", track.isLive());
      }

      tracksJson.add(trackJson);
    }

    return tracksJson;
  }

  private List<JsonObject> getPublicationAttachmentsJson(Publication publication, Boolean sign) {
    List<JsonObject> attachmentsJson = new ArrayList<>();

    for (Attachment attachment : publication.getAttachments()) {
      JsonObject json = new JsonObject();

      json.addProperty("id", safeString(attachment.getIdentifier()));
      json.addProperty("mediatype", safeString(attachment.getMimeType()));
      json.addProperty("url", safeString(getSignedUrl(attachment.getURI(), sign)));
      json.addProperty("flavor", safeString(attachment.getFlavor()));
      json.addProperty("ref", safeString(attachment.getReference()));
      json.addProperty("size", attachment.getSize());
      json.addProperty("checksum", safeString(attachment.getChecksum()));
      json.add("tags", arrayToJsonArray(attachment.getTags()));

      attachmentsJson.add(json);
    }

    return attachmentsJson;
  }

  private List<JsonObject> getPublicationCatalogsJson(Publication publication, Boolean sign) {
    List<JsonObject> catalogsJson = new ArrayList<>();

    for (Catalog catalog : publication.getCatalogs()) {
      JsonObject json = new JsonObject();

      json.addProperty("id", safeString(catalog.getIdentifier()));
      json.addProperty("mediatype", safeString(catalog.getMimeType()));
      json.addProperty("url", safeString(getSignedUrl(catalog.getURI(), sign)));
      json.addProperty("flavor", safeString(catalog.getFlavor()));
      json.addProperty("size", catalog.getSize());
      json.addProperty("checksum", safeString(catalog.getChecksum()));
      json.add("tags", arrayToJsonArray(catalog.getTags()));

      catalogsJson.add(json);
    }

    return catalogsJson;
  }

  @GET
  @Path("{eventId}/publications/{publicationId}")
  @RestQuery(name = "geteventpublication", description = "Returns a single publication.", returnDescription = "",
             pathParameters = {
               @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
               @RestParameter(name = "publicationId", description = "The publication id", isRequired = true, type = STRING)
             },
             restParameters = {
               @RestParameter(name = "sign", description = "Whether public distribution urls should be signed.",
                              isRequired = false, type = Type.BOOLEAN)
             },
             responses = {
                  @RestResponse(description = "The track details are returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event or publication does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })

  public Response getEventPublication(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String eventId,
          @PathParam("publicationId") String publicationId, @QueryParam("sign") boolean sign) throws Exception {
    try {
      final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
      return ApiResponseBuilder.Json.ok(acceptHeader, getPublication(eventId, publicationId, sign, requestedVersion));
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound(e.getMessage());
    } catch (SearchIndexException e) {
      logger.error("Unable to get list of publications from event with id '{}'", eventId, e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }


  private JsonObject getPublication(String eventId, String publicationId, Boolean withSignedUrls, ApiVersion requestedVersion)
          throws SearchIndexException, NotFoundException {
    for (final Event event : indexService.getEvent(eventId, elasticsearchIndex)) {
      List<Publication> publications;
      publications = event.getPublications().stream().filter(publication -> (!requestedVersion.isSmallerThan(VERSION_1_11_0) || EventUtils.internalChannelFilter.test(publication))).collect(Collectors.toList());
      for (Publication publication : publications) {
        if (publicationId.equals(publication.getIdentifier())) {
          return getPublication(publication, withSignedUrls, requestedVersion);
        }
      }
      throw new NotFoundException(
              String.format("Unable to find publication with id '%s' in event with id '%s'", publicationId, eventId));
    }
    throw new NotFoundException(String.format("Unable to find event with id '%s'", eventId));
  }

  /**
   * Get an {@link AccessControlList} from an {@link Event}.
   *
   * @param event
   *          The {@link Event} to get the ACL from.
   * @return The {@link AccessControlList} stored in the {@link Event}
   */
  protected static AccessControlList getAclFromEvent(Event event) {
    AccessControlList activeAcl = new AccessControlList();
    try {
      if (event.getAccessPolicy() != null)
        activeAcl = AccessControlParser.parseAcl(event.getAccessPolicy());
    } catch (Exception e) {
      logger.error("Unable to parse access policy", e);
    }
    return activeAcl;
  }

  private JsonObject getJsonStream(Stream stream) {
    JsonObject json = new JsonObject();

    if (stream instanceof AudioStream) {
      AudioStream audio = (AudioStream) stream;

      if (audio.getBitDepth() != null) json.addProperty("bitdepth", audio.getBitDepth());
      if (audio.getBitRate() != null) json.addProperty("bitrate", audio.getBitRate());
      if (audio.getCaptureDevice() != null) json.addProperty("capturedevice", audio.getCaptureDevice());
      if (audio.getCaptureDeviceVendor() != null) json.addProperty("capturedevicevendor", audio.getCaptureDeviceVendor());
      if (audio.getCaptureDeviceVersion() != null) json.addProperty("capturedeviceversion", audio.getCaptureDeviceVersion());
      if (audio.getChannels() != null) json.addProperty("channels", audio.getChannels());
      if (audio.getEncoderLibraryVendor() != null) json.addProperty("encoderlibraryvendor", audio.getEncoderLibraryVendor());
      if (audio.getFormat() != null) json.addProperty("format", audio.getFormat());
      if (audio.getFormatVersion() != null) json.addProperty("formatversion", audio.getFormatVersion());
      if (audio.getFrameCount() != null) json.addProperty("framecount", audio.getFrameCount());
      if (audio.getIdentifier() != null) json.addProperty("identifier", audio.getIdentifier());
      if (audio.getPkLevDb() != null) json.addProperty("pklevdb", audio.getPkLevDb());
      if (audio.getRmsLevDb() != null) json.addProperty("rmslevdb", audio.getRmsLevDb());
      if (audio.getRmsPkDb() != null) json.addProperty("rmspkdb", audio.getRmsPkDb());
      if (audio.getSamplingRate() != null) json.addProperty("samplingrate", audio.getSamplingRate());

    } else if (stream instanceof VideoStream) {
      VideoStream video = (VideoStream) stream;

      if (video.getBitRate() != null) json.addProperty("bitrate", video.getBitRate());
      if (video.getCaptureDevice() != null) json.addProperty("capturedevice", video.getCaptureDevice());
      if (video.getCaptureDeviceVendor() != null) json.addProperty("capturedevicevendor", video.getCaptureDeviceVendor());
      if (video.getCaptureDeviceVersion() != null) json.addProperty("capturedeviceversion", video.getCaptureDeviceVersion());
      if (video.getEncoderLibraryVendor() != null) json.addProperty("encoderlibraryvendor", video.getEncoderLibraryVendor());
      if (video.getFormat() != null) json.addProperty("format", video.getFormat());
      if (video.getFormatVersion() != null) json.addProperty("formatversion", video.getFormatVersion());
      if (video.getFrameCount() != null) json.addProperty("framecount", video.getFrameCount());
      if (video.getFrameHeight() != null) json.addProperty("frameheight", video.getFrameHeight());
      if (video.getFrameRate() != null) json.addProperty("framerate", video.getFrameRate());
      if (video.getFrameWidth() != null) json.addProperty("framewidth", video.getFrameWidth());
      if (video.getIdentifier() != null) json.addProperty("identifier", video.getIdentifier());
      if (video.getScanOrder() != null) json.addProperty("scanorder", video.getScanOrder().toString());
      if (video.getScanType() != null) json.addProperty("scantype", video.getScanType().toString());
    }

    return json;
  }

  private String getEventUrl(String eventId) {
    return UrlSupport.concat(endpointBaseUrl, eventId);
  }

  @GET
  @Path("{eventId}/scheduling")
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0, ApiMediaType.VERSION_1_3_0,
              ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0, ApiMediaType.VERSION_1_6_0,
              ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0, ApiMediaType.VERSION_1_9_0,
              ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0 })
  @RestQuery(name = "geteventscheduling", description = "Returns an event's scheduling information.", returnDescription = "", pathParameters = {
      @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, responses = {
      @RestResponse(description = "The scheduling information for the specified event is returned.", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "The specified event has no scheduling information.", responseCode = HttpServletResponse.SC_NO_CONTENT),
      @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventScheduling(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
      throws Exception {
    try {
      final Opt<Event> event = indexService.getEvent(id, elasticsearchIndex);

      if (event.isNone()) {
        return ApiResponseBuilder.notFound(String.format("Unable to find event with id '%s'", id));
      }

      final JsonObject scheduling = SchedulingInfo.of(event.get().getIdentifier(), schedulerService).toJson();
      if (!scheduling.isEmpty()) {
        return ApiResponseBuilder.Json.ok(acceptHeader, scheduling);
      }
      return Response.noContent().build();
    } catch (SearchIndexException e) {
      logger.error("Unable to get list of publications from event with id '{}'", id, e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{eventId}/scheduling")
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0, ApiMediaType.VERSION_1_3_0,
              ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0, ApiMediaType.VERSION_1_6_0,
              ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0, ApiMediaType.VERSION_1_9_0,
              ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0 })
  @RestQuery(name = "updateeventscheduling", description = "Update an event's scheduling information.", returnDescription = "", pathParameters = {
      @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = Type.STRING) }, restParameters = {
      @RestParameter(name = "scheduling", isRequired = true, description = "Scheduling Information", type = Type.STRING),
      @RestParameter(name = "allowConflict", description = "Allow conflicts when updating scheduling", isRequired = false, type = Type.BOOLEAN) }, responses = {
      @RestResponse(description = "The  scheduling information for the specified event is updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
      @RestResponse(description = "The specified event has no scheduling information to update.", responseCode = HttpServletResponse.SC_NOT_ACCEPTABLE),
      @RestResponse(description = "The scheduling information could not be updated due to a conflict.", responseCode = HttpServletResponse.SC_CONFLICT),
      @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventScheduling(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
                                 @FormParam("scheduling") String scheduling,
                                 @FormParam("allowConflict") @DefaultValue("false") boolean allowConflict) throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    final Opt<Event> event = indexService.getEvent(id, elasticsearchIndex);

    if (requestedVersion.isSmallerThan(ApiVersion.VERSION_1_2_0)) {
        allowConflict = false;
    }
    if (event.isNone()) {
      return ApiResponseBuilder.notFound(String.format("Unable to find event with id '%s'", id));
    }
    final JSONParser parser = new JSONParser();
    JSONObject parsedJson;
    try {
       parsedJson = (JSONObject) parser.parse(scheduling);
    } catch (ParseException e) {
      logger.debug("Client sent unparsable scheduling information for event {}: {}", id, scheduling);
      return RestUtil.R.badRequest("Unparsable scheduling information");
    }
    Optional<Response> clientError = updateSchedulingInformation(parsedJson, id, requestedVersion, allowConflict);
    return clientError.orElse(Response.noContent().build());
  }

  private Optional<Response> updateSchedulingInformation(
      JSONObject parsedScheduling,
      String id,
      ApiVersion requestedVersion,
      boolean allowConflict) throws Exception {

    SchedulingInfo schedulingInfo;
    try {
      schedulingInfo = SchedulingInfo.of(parsedScheduling);
    } catch (DateTimeParseException e) {
      logger.debug("Client sent unparsable start or end date for event {}", id);
      return Optional.of(RestUtil.R.badRequest("Unparsable date in scheduling information"));
    }
    final TechnicalMetadata technicalMetadata = schedulerService.getTechnicalMetadata(id);

    // When "inputs" is updated, capture agent configuration needs to be merged
    Opt<Map<String, String>> caConfig = Opt.none();
    if (schedulingInfo.getInputs().isSome()) {
      final Map<String, String> configMap = new HashMap<>(technicalMetadata.getCaptureAgentConfiguration());
      configMap.put(CaptureParameters.CAPTURE_DEVICE_NAMES, schedulingInfo.getInputs().get());
      caConfig = Opt.some(configMap);
    }

    try {
      schedulerService.updateEvent(
          id,
          schedulingInfo.getStartDate(),
          schedulingInfo.getEndDate(),
          schedulingInfo.getAgentId(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          caConfig,
          allowConflict);
    } catch (SchedulerConflictException e) {
      final List<MediaPackage> conflictingEvents = getConflictingEvents(
          schedulingInfo.merge(technicalMetadata), agentStateService, schedulerService);
      logger.debug("Client tried to change scheduling information causing a conflict for event {}.", id);
      List<JsonObject> conflicts = convertConflictingEvents(
          Optional.of(id), conflictingEvents, indexService, elasticsearchIndex
      );

      JsonArray conflictArray = new JsonArray();
      for (JsonObject conflict : conflicts) {
        conflictArray.add(conflict);
      }

      return Optional.of(ApiResponseBuilder.Json.conflict(requestedVersion, conflictArray));
    }
    return Optional.empty();
  }

  @POST
  @Path("{eventId}/track")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "updateFlavorWithTrack", description = "Update an events track for a given flavor", returnDescription = "",
          pathParameters = {
                  @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) },
          restParameters = {
                  @RestParameter(description = "Flavor to add track to, e.g. captions/source",
                      isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
                  @RestParameter(description = "Comma separated list of tags for the given track, e.g. archive,publish. "
                      + "If a 'lang:LANG-CODE' tag exists and overwriteExisting=true "
                      + "only tracks with same lang tag and flavor will be replaced. This behavior is used for captions.",
                      isRequired = false, name = "tags", type = RestParameter.Type.STRING),
                  @RestParameter(description = "If true, all other tracks in the specified flavor are REMOVED. "
                      + "If tags argument contains a lang:LANG-CODE tag, only elements with same tag would be removed.",
                      isRequired = true, name = "overwriteExisting", type = RestParameter.Type.BOOLEAN),
                  @RestParameter(description = "The track file", isRequired = true, name = "track", type = RestParameter.Type.FILE),
          },
          responses = {
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND),
                  @RestResponse(description = "The track has been added to the event.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          })
  public Response updateFlavorWithTrack(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @Context HttpServletRequest request) {
    logger.debug("updateFlavorWithTrack called");
    try {
      boolean overwriteExisting = false;
      MediaPackageElementFlavor tmpFlavor = MediaPackageElementFlavor.parseFlavor("addTrack/temporary");
      MediaPackageElementFlavor newFlavor = null;
      Opt<Event> event;
      List<String> tags = null;
      String langTag = null;

      try {
        event = indexService.getEvent(id, elasticsearchIndex);
      } catch (SearchIndexException e) {
        return RestUtil.R.badRequest(String.format("Error while searching for event with id %s; %s", id, e.getMessage()));
      }

      if (event.isNone()) {
        return ApiResponseBuilder.notFound(String.format("Unable to find event with id '%s'", id));
      }
      MediaPackage mp = indexService.getEventMediapackage(event.get());

      try {
        if (workflowService.mediaPackageHasActiveWorkflows(mp.getIdentifier().toString())) {
          return RestUtil.R.conflict(String.format("Cannot update while a workflow is running on event '%s'", id));
        }
      } catch (WorkflowDatabaseException e) {
        return RestUtil.R.serverError();
      }

      if (!ServletFileUpload.isMultipartContent(request)) {
        throw new IllegalArgumentException("No multipart content");
      }
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        String fieldName = item.getFieldName();
        if (item.isFormField()) {
          if ("flavor".equals(fieldName)) {
            String flavorString = Streams.asString(item.openStream());
            try {
              newFlavor = MediaPackageElementFlavor.parseFlavor(flavorString);
            } catch (IllegalArgumentException e) {
              return RestUtil.R.badRequest(String.format("Could not parse flavor %s; %s", flavorString, e.getMessage()));
            }
          } else if ("tags".equals(fieldName)) {
            String tagsString = Streams.asString(item.openStream());
            if (StringUtils.isNotBlank(tagsString)) {
              tags = List.of(StringUtils.split(tagsString, ','));
              // find lang tag if exists
              for (String tag : tags) {
                if (StringUtils.startsWith(StringUtils.trimToEmpty(tag), "lang:")) {
                  // lang tag is set
                  langTag = StringUtils.trimToEmpty(tag);
                  break;
                }
              }
            }
          } else if ("overwriteExisting".equals(fieldName)) {
            overwriteExisting = Boolean.parseBoolean(Streams.asString(item.openStream()));
          }
        } else {
          // Add track with temporary flavor
          if ("track".equals(item.getFieldName())) {
            mp = ingestService.addTrack(item.openStream(), item.getName(), tmpFlavor, mp);
          }
        }
      }

      if (overwriteExisting) {
        // remove existing attachments of the new flavor
        Track[] existing = mp.getTracks(newFlavor);
        for (int i = 0; i < existing.length; i++) {
          // if lang tag is set, remove only matching elements
          if (null == langTag || existing[i].containsTag(langTag)) {
            mp.remove(existing[i]);
            logger.debug("Overwriting existing asset {} {}", tmpFlavor, newFlavor);
          }
        }
      }
      // correct the flavor of the new attachment
      for (Track track : mp.getTracks(tmpFlavor)) {
        track.setFlavor(newFlavor);
        if (null != tags) {
          for (String tag : tags) {
            track.addTag(tag);
          }
        }
      }
      logger.debug("Updated asset {} {}", tmpFlavor, newFlavor);

      try {
        assetManager.takeSnapshot(mp);
      } catch (AssetManagerException e) {
        logger.error("Error while adding the updated media package ({}) to the archive", mp.getIdentifier(), e);
        return RestUtil.R.badRequest(e.getMessage());
      }

      return Response.status(Status.OK).build();
    } catch (IllegalArgumentException | IOException | FileUploadException | IndexServiceException | IngestException
            | MediaPackageException e) {
      return RestUtil.R.badRequest(String.format("Could not add track: %s", e.getMessage()));
    }
  }
}
