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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_0_0;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.external.util.AclUtils;
import org.opencastproject.external.util.ExternalMetadataUtils;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataList.Locked;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.IndexObject;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.Event.SchedulingStatus;
import org.opencastproject.index.service.impl.index.event.EventHttpServletRequest;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.event.EventUtils;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.Jsons.Functions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

@Path("/")
@RestService(name = "externalapievents", title = "External API Events Service", notes = "", abstractText = "Provides resources and operations related to the events")
public class EventsEndpoint implements ManagedService {
  private static final String METADATA_JSON_KEY = "metadata";

  protected static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /** The default time before a piece of signed content expires. 2 Hours. */
  protected static final Long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60L;

  /** Subtype of previews required by the video editor */
  private static final String PREVIEW_SUBTYPE = "preview.subtype";

  /** Subtype of previews required by the video editor */
  private static final String DEFAULT_PREVIEW_SUBTYPE = "preview";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EventsEndpoint.class);

  /** A parser for handling JSON documents inside the body of a request. **/
  public static final JSONParser parser = new JSONParser();

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  private static long expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private String previewSubtype = DEFAULT_PREVIEW_SUBTYPE;

  /** The resolutions */
  private enum CommentResolution {
    ALL, UNRESOLVED, RESOLVED;
  };

  /* OSGi service references */
  private ExternalIndex externalIndex;
  private IndexService indexService;
  private IngestService ingestService;
  private SecurityService securityService;
  private CommonEventCatalogUIAdapter eventCatalogUIAdapter;
  private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<>();
  private UrlSigningService urlSigningService;

  /** OSGi DI */
  void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  /** OSGi DI */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi DI */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI */
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  /** OSGi DI. */
  public void setCommonEventCatalogUIAdapter(CommonEventCatalogUIAdapter eventCatalogUIAdapter) {
    this.eventCatalogUIAdapter = eventCatalogUIAdapter;
  }

  /** OSGi DI. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.remove(catalogUIAdapter);
  }

  private List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return new ArrayList<>(getEventCatalogUIAdapters(getSecurityService().getOrganization().getId()));
  }

  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    List<EventCatalogUIAdapter> adapters = new ArrayList<>();
    for (EventCatalogUIAdapter adapter : catalogUIAdapters) {
      if (organization.equals(adapter.getOrganization())) {
        adapters.add(adapter);
      }
    }
    return adapters;
  }

  /** OSGi activation method */
  void activate(ComponentContext cc) {
    this.serverUrl = "http://localhost:8080";
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstants.EXTERNAL_API_URL_ORG_PROPERTY);
      if (ccServerUrl != null) {
        logger.debug("Configured server url is {}", ccServerUrl);
        this.serverUrl = ccServerUrl;
      }
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    logger.info("Activated External API - Events Endpoint");
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Ensure properties is not null
    if (properties == null) {
      properties = new Hashtable();
      logger.debug("No configuration set");
    }

    // Read URL Signing Expiration duration
    // Default to DEFAULT_URL_SIGNING_EXPIRE_DURATION.toString()));
    try {
      expireSeconds = Long.parseLong(StringUtils.defaultString(
              (String) properties.get(URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY),
              DEFAULT_URL_SIGNING_EXPIRE_DURATION.toString()));
    } catch (NumberFormatException e) {
      logger.error("Error parsing URL signing expiration configuration value", e);
    }
    logger.debug("URLs signatures are configured to expire in {}.", Log.getHumanReadableTimeString(expireSeconds));

    // Read preview subtype configuration
    // Default to DEFAULT_PREVIEW_SUBTYPE
    previewSubtype = StringUtils.defaultString((String) properties.get(PREVIEW_SUBTYPE), DEFAULT_PREVIEW_SUBTYPE);
    logger.debug("Preview subtype is '{}'", previewSubtype);
  }

  @GET
  @Path("{eventId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "getevent", description = "Returns a single event. By setting the optional sign parameter to true, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the maximum validity of signed URLs when caching this response.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "sign", isRequired = false, description = "Whether public distribution urls should be signed.", type = Type.BOOLEAN),
                  @RestParameter(name = "withacl", isRequired = false, description = "Whether the acl metadata should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "withmetadata", isRequired = false, description = "Whether the metadata catalogs should be included in the response.", type = Type.BOOLEAN),
                  @RestParameter(name = "withpublications", isRequired = false, description = "Whether the publication ids and urls should be included in the response.", type = Type.BOOLEAN), }, reponses = {
                          @RestResponse(description = "The event is returned.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("sign") boolean sign, @QueryParam("withacl") Boolean withAcl,
          @QueryParam("withmetadata") Boolean withMetadata, @QueryParam("withpublications") Boolean withPublications)
          throws Exception {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      event.updatePreview(previewSubtype);
      return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0,
              eventToJSON(event, withAcl, withMetadata, withPublications, sign));
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/media")
  @Produces({ "application/json", "application/v1.0.0+json" })
  public Response getEventMedia(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws Exception {
    ArrayList<TrackImpl> tracks = new ArrayList<>();

    for (final Event event : indexService.getEvent(id, externalIndex)) {
      final MediaPackage mp = indexService.getEventMediapackage(event);
      for (Track track : mp.getTracks()) {
        if (track instanceof TrackImpl) {
          tracks.add((TrackImpl) track);
        }
      }

      List<JValue> tracksJson = new ArrayList<>();
      for (Track track : tracks) {
        List<Field> fields = new ArrayList<>();
        if (track.getChecksum() != null)
          fields.add(f("checksum", v(track.getChecksum().toString())));
        if (track.getDescription() != null)
          fields.add(f("description", v(track.getDescription())));
        if (track.getDuration() != null)
          fields.add(f("duration", v(track.getDuration())));
        if (track.getElementDescription() != null)
          fields.add(f("element-description", v(track.getElementDescription())));
        if (track.getFlavor() != null)
          fields.add(f("flavor", v(track.getFlavor().toString())));
        if (track.getIdentifier() != null)
          fields.add(f("identifier", v(track.getIdentifier())));
        if (track.getMimeType() != null)
          fields.add(f("identifier", v(track.getMimeType().toString())));
        fields.add(f("size", v(track.getSize())));
        if (track.getStreams() != null) {
          List<Field> streams = new ArrayList<>();
          for (Stream stream : track.getStreams()) {
            streams.add(f(stream.getIdentifier(), getJsonStream(stream)));
          }
          fields.add(f("streams", obj(streams)));
        }
        if (track.getTags() != null) {
          List<JValue> tags = new ArrayList<>();
          for (String tag : track.getTags()) {
            tags.add(v(tag));
          }
          fields.add(f("tags", arr(tags)));
        }
        if (track.getURI() != null)
          fields.add(f("uri", v(track.getURI().toString())));
        tracksJson.add(obj(fields));
      }
      return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(tracksJson));
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "deleteevent", description = "Deletes an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The event has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws NotFoundException, UnauthorizedException {
    if (!indexService.removeEvent(id))
      return Response.serverError().build();

    return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
  }

  @POST
  @Path("{eventId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "updateeventmetadata", description = "Updates an event.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = false, description = "A collection of roles with their possible action", type = Type.STRING),
                  @RestParameter(name = "metadata", isRequired = false, description = "Event metadata as Form param", type = Type.STRING),
                  @RestParameter(name = "presenter", isRequired = false, description = "Presenter movie track", type = Type.FILE),
                  @RestParameter(name = "presentation", isRequired = false, description = "Presentation movie track", type = Type.FILE),
                  @RestParameter(name = "audio", isRequired = false, description = "Audio track", type = Type.FILE),
                  @RestParameter(name = "processing", isRequired = false, description = "Processing instructions task configuration", type = Type.STRING), }, reponses = {
                          @RestResponse(description = "The event has been updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventMetadata(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request,
          @PathParam("eventId") String eventId) {
    return updateEvent(eventId, request);
  }

  private Response updateEvent(String eventId, HttpServletRequest request) {
    try {
      for (final Event event : indexService.getEvent(eventId, externalIndex)) {
        EventHttpServletRequest eventHttpServletRequest = EventHttpServletRequest.updateFromHttpServletRequest(event,
                request, getEventCatalogUIAdapters());
        if (eventHttpServletRequest.getMetadataList().isSome()) {
          indexService.updateEventMetadata(eventId, eventHttpServletRequest.getMetadataList().get(), externalIndex);
        }

        if (eventHttpServletRequest.getAcl().isSome()) {
          indexService.updateEventAcl(eventId, eventHttpServletRequest.getAcl().get(), externalIndex);
        }
        return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
      }
      return ApiResponses.notFound("Cannot find an event with id '%s'.", eventId);
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find an event with id '%s'.", eventId);
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to update event '{}' because: {}", eventId, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      logger.error("Unable to get multi part fields or file for event '{}' because: {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    } catch (SearchIndexException e) {
      logger.error("Unable to update event '{}' because: {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RestQuery(name = "createevent", description = "Creates an event by sending metadata, access control list, processing instructions and files in a multipart request.", returnDescription = "", restParameters = {
          @RestParameter(name = "acl", isRequired = false, description = "A collection of roles with their possible action", type = STRING),
          @RestParameter(name = "metadata", description = "Event metadata as Form param", isRequired = false, type = STRING),
          @RestParameter(name = "presenter", description = "Presenter movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "presentation", description = "Presentation movie track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "audio", description = "Audio track", isRequired = false, type = Type.FILE),
          @RestParameter(name = "processing", description = "Processing instructions task configuration", isRequired = false, type = STRING) }, reponses = {
                  @RestResponse(description = "A new event is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The request is invalid or inconsistent..", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createNewEvent(@HeaderParam("Accept") String acceptHeader, @Context HttpServletRequest request) {
    try {
      return createNewEvent(request);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to create event because: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      if (e.getCause() != null && e.getCause() instanceof NotFoundException
              || e.getCause() instanceof IllegalArgumentException) {
        logger.debug("Unable to create event because: {}", ExceptionUtils.getStackTrace(e));
        return RestUtil.R.badRequest(e.getCause().getMessage());
      } else {
        logger.error("Unable to create event because: {}", ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      logger.error("Unable to create event because: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @SuppressWarnings("unchecked")
  private Response createNewEvent(HttpServletRequest request)
          throws ParseException, IOException, IndexServiceException, java.text.ParseException, MediaPackageException,
          IngestException, NotFoundException, SchedulerException, UnauthorizedException {
    JSONObject source = new JSONObject();
    source.put("type", "UPLOAD");
    EventHttpServletRequest eventHttpServletRequest = EventHttpServletRequest.createFromHttpServletRequest(request,
            ingestService, getEventCatalogUIAdapters(), source);
    String eventId = indexService.createEvent(eventHttpServletRequest);
    return ApiResponses.Json.created(VERSION_1_0_0, URI.create(getEventUrl(eventId)), obj(f("identifier", v(eventId))));
  }

  @GET
  @Path("/")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "getevents", description = "Returns a list of events. By setting the optional sign parameter to true, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the maximum validity of signed URLs when caching this response.", returnDescription = "", restParameters = {
          @RestParameter(name = "sign", isRequired = false, description = "Whether public distribution urls should be signed.", type = Type.BOOLEAN),
          @RestParameter(name = "withacl", isRequired = false, description = "Whether the acl metadata should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "withmetadata", isRequired = false, description = "Whether the metadata catalogs should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "withpublications", isRequired = false, description = "Whether the publication ids and urls should be included in the response.", type = Type.BOOLEAN),
          @RestParameter(name = "filter", isRequired = false, description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.", type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "Number of results to skip based on the limit. 0 is the first set of results up to the limit, 1 is the second set of results after the first limit, 2 is third set of results after skipping the first two sets of results etc.", isRequired = false, type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(description = "A (potentially empty) list of events is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getEvents(@HeaderParam("Accept") String acceptHeader, @QueryParam("id") String id,
          @QueryParam("commentReason") String reasonFilter, @QueryParam("commentResolution") String resolutionFilter,
          @QueryParam("filter") String filter, @QueryParam("sort") String sort, @QueryParam("offset") Integer offset,
          @QueryParam("limit") Integer limit, @QueryParam("sign") boolean sign, @QueryParam("withacl") Boolean withAcl,
          @QueryParam("withmetadata") Boolean withMetadata, @QueryParam("withpublications") Boolean withPublications) {
    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));
    EventSearchQuery query = new EventSearchQuery(getSecurityService().getOrganization().getId(),
            getSecurityService().getUser());
    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    // Parse the filters
    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        String[] filterTuple = f.split(":");
        if (filterTuple.length < 2) {
          logger.info("No value for filter {} in filters list: {}", filterTuple[0], filter);
          continue;
        }

        String name = filterTuple[0];
        String value = filterTuple[1];

        if ("presenters".equals(name))
          query.withPresenter(value);
        if ("contributors".equals(name))
          query.withContributor(value);
        if ("location".equals(name))
          query.withLocation(value);
        if ("textFilter".equals(name))
          query.withText("*" + value + "*");
        if ("series".equals(name))
          query.withSeriesId(value);
        if ("subject".equals(name))
          query.withSubject(value);
      }
    }

    if (optSort.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
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
          case EventIndexSchema.REVIEW_STATUS:
            query.sortByReviewStatus(criterion.getOrder());
            break;
          case EventIndexSchema.WORKFLOW_STATE:
            query.sortByWorkflowState(criterion.getOrder());
            break;
          case EventIndexSchema.SCHEDULING_STATUS:
            query.sortBySchedulingStatus(criterion.getOrder());
            break;
          case EventIndexSchema.SERIES_NAME:
            query.sortBySeriesName(criterion.getOrder());
            break;
          case EventIndexSchema.LOCATION:
            query.sortByLocation(criterion.getOrder());
            break;
          default:
            return RestUtil.R
                    .badRequest(String.format("Unknown search criterion in request: %s", criterion.getFieldName()));
        }
      }
    }

    // TODO: Add the comment resolution filter to the query
    CommentResolution resolution = null;
    if (StringUtils.isNotBlank(resolutionFilter)) {
      try {
        resolution = CommentResolution.valueOf(resolutionFilter);
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
      results = externalIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The External Search Index was not able to get the events list: {}",
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    SearchResultItem<Event>[] items = results.getItems();
    List<IndexObject> events = new ArrayList<>();
    for (SearchResultItem<Event> item : items) {
      Event source = item.getSource();
      source.updatePreview(previewSubtype);
      events.add(source);
    }
    try {
      return getJsonEvents(acceptHeader, events, withAcl, withMetadata, withPublications, sign);
    } catch (Exception e) {
      logger.error("Unable to get events because: {}", ExceptionUtils.getStackTrace(e));
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
   * @return A {@link Response} with the accept header and body as the Json array of {@link Event}s.
   * @throws NotFoundException
   * @throws IndexServiceException
   */
  protected Response getJsonEvents(String acceptHeader, List<IndexObject> events, Boolean withAcl, Boolean withMetadata,
          Boolean withPublications, Boolean withSignedUrls)
          throws IndexServiceException, SearchIndexException, NotFoundException {
    List<JValue> eventsList = new ArrayList<>();
    for (IndexObject item : events) {
      eventsList.add(eventToJSON((Event) item, withAcl, withMetadata, withPublications, withSignedUrls));
    }
    return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(eventsList));
  }

  private void extendEventsStatusOverview(List<Field> fields, Series series) throws SearchIndexException {
    EventSearchQuery query = new EventSearchQuery(getSecurityService().getOrganization().getId(),
            getSecurityService().getUser()).withoutActions().withSeriesId(series.getIdentifier());
    SearchResult<Event> result = externalIndex.getByQuery(query);

    // collect recording statuses
    int blacklisted = 0;
    int optOut = 0;
    int ready = 0;

    for (SearchResultItem<Event> item : result.getItems()) {
      Event event = item.getSource();
      if (event.getSchedulingStatus() == null)
        continue;

      SchedulingStatus schedulingStatus = SchedulingStatus.valueOf(event.getSchedulingStatus());
      if (SchedulingStatus.BLACKLISTED.equals(schedulingStatus)) {
        blacklisted++;
      } else if (series.isOptedOut() || SchedulingStatus.OPTED_OUT.equals(schedulingStatus)) {
        optOut++;
      } else {
        ready++;
      }
    }

    fields.add(f("events", obj(f("BLACKLISTED", v(blacklisted)), f("OPTED_OUT", v(optOut)), f("READY", v(ready)))));
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
   * @param withPublications
   *          Whether to add the publications
   * @param withSignedUrls
   *          Whether to sign the urls if they are protected by stream security.
   * @return The event in json format.
   * @throws IndexServiceException
   *           Thrown if unable to get the metadata for the event.
   * @throws SearchIndexException
   *           Thrown if unable to get event publications from search service
   * @throws NotFoundException
   *           Thrown if unable to find all of the metadata
   */
  protected JValue eventToJSON(Event event, Boolean withAcl, Boolean withMetadata, Boolean withPublications,
          Boolean withSignedUrls) throws IndexServiceException, SearchIndexException, NotFoundException {
    List<Field> fields = new ArrayList<>();
    if (event.getArchiveVersion() != null)
      fields.add(f("archive_version", v(event.getArchiveVersion())));
    fields.add(f("created", v(event.getCreated(), Jsons.BLANK)));
    fields.add(f("creator", v(event.getCreator(), Jsons.BLANK)));
    fields.add(f("contributor", arr($(event.getContributors()).map(Functions.stringToJValue))));
    fields.add(f("description", v(event.getDescription(), Jsons.BLANK)));
    fields.add(f("has_previews", v(event.hasPreview())));
    fields.add(f("identifier", v(event.getIdentifier(), BLANK)));
    fields.add(f("location", v(event.getLocation(), BLANK)));
    fields.add(f("presenter", arr($(event.getPresenters()).map(Functions.stringToJValue))));
    List<JValue> publicationIds = new ArrayList<>();
    if (event.getPublications() != null) {
      for (Publication publication : event.getPublications()) {
        publicationIds.add(v(publication.getChannel()));
      }
    }
    fields.add(f("publication_status", arr(publicationIds)));
    fields.add(f("processing_state", v(event.getWorkflowState(), BLANK)));
    fields.add(f("start", v(event.getTechnicalStartTime(), BLANK)));
    if (event.getTechnicalEndTime() != null) {
      long duration = new DateTime(event.getTechnicalStartTime()).getMillis()
              - new DateTime(event.getTechnicalEndTime()).getMillis();
      fields.add(f("duration", v(duration)));
    }
    if (StringUtils.trimToNull(event.getSubject()) != null) {
      fields.add(f("subjects", arr(splitSubjectIntoArray(event.getSubject()))));
    } else {
      fields.add(f("subjects", arr()));
    }
    fields.add(f("title", v(event.getTitle(), BLANK)));
    if (withAcl != null && withAcl) {
      AccessControlList acl = getAclFromEvent(event);
      fields.add(f("acl", arr(AclUtils.serializeAclToJson(acl))));
    }
    if (withMetadata != null && withMetadata) {
      try {
        Opt<MetadataList> metadata = getEventMetadata(event);
        if (metadata.isSome()) {
          fields.add(f("metadata", metadata.get().toJSON()));
        }
      } catch (Exception e) {
        logger.error("Unable to get metadata for event '{}' because: {}", event.getIdentifier(),
                ExceptionUtils.getStackTrace(e));
        throw new IndexServiceException("Unable to add metadata to event", e);
      }
    }
    if (withPublications != null && withPublications) {
      List<JValue> publications = getPublications(event.getIdentifier(), withSignedUrls);
      fields.add(f("publications", arr(publications)));
    }
    return obj(fields);
  }

  private List<JValue> splitSubjectIntoArray(final String subject) {
    return com.entwinemedia.fn.Stream.$(subject.split(",")).map(new Fn<String, JValue>() {
      @Override
      public JValue apply(String a) {
        return v(a.trim());
      }
    }).toList();
  }

  @GET
  @Path("{eventId}/acl")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "geteventacl", description = "Returns an event's access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The access control list for the specified event is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id)
          throws Exception {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      AccessControlList acl = getAclFromEvent(event);
      return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(AclUtils.serializeAclToJson(acl)));
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @PUT
  @Path("{eventId}/acl")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "updateeventacl", description = "Update an event's access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = true, description = "Access policy", type = STRING) }, reponses = {
                          @RestResponse(description = "The access control list for the specified event is updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @FormParam("acl") String acl) throws Exception {
    if (indexService.getEvent(id, externalIndex).isSome()) {
      AccessControlList accessControlList;
      try {
        accessControlList = AclUtils.deserializeJsonToAcl(acl, false);
      } catch (ParseException e) {
        logger.debug("Unable to update event acl to '{}' because: {}", acl, ExceptionUtils.getStackTrace(e));
        return R.badRequest(String.format("Unable to parse acl '%s' because '%s'", acl, e.getMessage()));
      } catch (IllegalArgumentException e) {
        logger.debug("Unable to update event acl to '{}' because: {}", acl, ExceptionUtils.getStackTrace(e));
        return R.badRequest(e.getMessage());
      }
      try {
        accessControlList = indexService.updateEventAcl(id, accessControlList, externalIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to update event '{}' acl with '{}' because: {}",
                new Object[] { id, acl, ExceptionUtils.getStackTrace(e) });
        return Response.status(Status.FORBIDDEN).build();
      }
      return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
    } else {
      return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
    }
  }

  @POST
  @Path("{eventId}/acl/{action}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "addeventace", description = "Grants permission to execute action on the specified event to any user with role role. Note that this is a convenience method to avoid having to build and post a complete access control list.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
          @RestParameter(name = "action", description = "The action that is allowed to be executed", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "role", isRequired = true, description = "The role that is granted permission", type = STRING) }, reponses = {
                          @RestResponse(description = "The permission has been created in the access control list of the specified event.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response addEventAce(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @PathParam("action") String action, @FormParam("role") String role) throws Exception {
    List<AccessControlEntry> entries = new ArrayList<>();
    for (final Event event : indexService.getEvent(id, externalIndex)) {
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
        withNewAce = indexService.updateEventAcl(id, withNewAce, externalIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to update event '{}' acl entry with action '{}' and role '{}' because: {}",
                new Object[] { id, action, role, ExceptionUtils.getStackTrace(e) });
        return Response.status(Status.FORBIDDEN).build();
      }
      return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}/acl/{action}/{role}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "deleteeventace", description = "Revokes permission to execute action on the specified event from any user with role role.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
          @RestParameter(name = "action", description = "The action that is no longer allowed to be executed", isRequired = true, type = STRING),
          @RestParameter(name = "role", description = "The role that is no longer granted permission", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The permission has been revoked from the access control list of the specified event.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEventAce(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @PathParam("action") String action, @PathParam("role") String role) throws Exception {
    List<AccessControlEntry> entries = new ArrayList<>();
    for (final Event event : indexService.getEvent(id, externalIndex)) {
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
        return ApiResponses.notFound("Unable to find an access control entry with action '%s' and role '%s'", action,
                role);
      }

      AccessControlList withoutDeleted = new AccessControlList(entries);
      try {
        withoutDeleted = indexService.updateEventAcl(id, withoutDeleted, externalIndex);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to delete event's '{}' acl entry with action '{}' and role '{}' because: {}",
                new Object[] { id, action, role, ExceptionUtils.getStackTrace(e) });
        return Response.status(Status.FORBIDDEN).build();
      }
      return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/metadata")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "geteventmetadata", description = "Returns the event's metadata of the specified type. For a metadata catalog there is the flavor such as 'dublincore/episode' and this is the unique type.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = false, description = "The type of metadata to get", type = STRING) }, reponses = {
                          @RestResponse(description = "The metadata collection is returned.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getAllEventMetadata(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type) throws Exception {
    if (StringUtils.trimToNull(type) == null) {
      Opt<MetadataList> metadataList = getEventMetadataById(id);
      if (metadataList.isSome())
        return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, metadataList.get().toJSON());
      else
        return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
    } else {
      return getEventMetadataByType(id, type);
    }
  }

  protected Opt<MetadataList> getEventMetadataById(String id) throws IndexServiceException, Exception {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      return getEventMetadata(event);
    }
    return Opt.<MetadataList> none();
  }

  protected Opt<MetadataList> getEventMetadata(Event event) throws IndexServiceException, Exception {
    MetadataList metadataList = new MetadataList();
    List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
    catalogUIAdapters.remove(this.eventCatalogUIAdapter);
    MediaPackage mediaPackage = indexService.getEventMediapackage(event);
    if (catalogUIAdapters.size() > 0) {
      for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
        // TODO: This is very slow:
        metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage));
      }
    }
    // TODO: This is slow:
    MetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter);
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

  private Response getEventMetadataByType(String id, String type) throws IndexServiceException, Exception {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      Opt<MediaPackageElementFlavor> flavor = getFlavor(type);
      if (flavor.isNone()) {
        return R.badRequest(
                String.format("Unable to parse type '%s' as a flavor so unable to find the matching catalog.", type));
      }
      // Try the main catalog first as we load it from the index.
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        MetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter);
        ExternalMetadataUtils.changeSubjectToSubjects(collection);
        ExternalMetadataUtils.removeCollectionList(collection);
        return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, collection.toJSON());
      }
      // Try the other catalogs
      List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
      catalogUIAdapters.remove(eventCatalogUIAdapter);
      MediaPackage mediaPackage = indexService.getEventMediapackage(event);
      if (catalogUIAdapters.size() > 0) {
        for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
          if (flavor.get().equals(catalogUIAdapter.getFlavor())) {
            MetadataCollection fields = catalogUIAdapter.getFields(mediaPackage);
            ExternalMetadataUtils.removeCollectionList(fields);
            return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, fields.toJSON());
          }
        }
      }
      return ApiResponses.notFound("Cannot find a catalog with type '%s' for event with id '%s'.", type, id);
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @PUT
  @Path("{eventId}/metadata")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "updateeventmetadata", description = "Update the metadata with the matching type of the specified event. For a metadata catalog there is the flavor such as 'dublincore/episode' and this is the unique type.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to update", type = STRING),
                  @RestParameter(name = "metadata", description = "Metadata catalog in JSON format", isRequired = true, type = STRING) }, reponses = {
                          @RestResponse(description = "The metadata of the given namespace has been updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEventMetadataByType(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type, @FormParam("metadata") String metadataJSON) throws Exception {
    Map<String, String> updatedFields;
    try {
      updatedFields = RequestUtils.getKeyValueMap(metadataJSON);
    } catch (ParseException e) {
      logger.debug("Unable to update event '{}' with metadata type '{}' and content '{}' because: {}",
              new Object[] { id, type, metadataJSON, ExceptionUtils.getStackTrace(e) });
      return RestUtil.R.badRequest(String.format("Unable to parse metadata fields as json from '%s' because '%s'",
              metadataJSON, ExceptionUtils.getStackTrace(e)));
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to update event '{}' with metadata type '{}' and content '{}' because: {}",
              new Object[] { id, type, metadataJSON, ExceptionUtils.getStackTrace(e) });
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

    MetadataCollection collection = null;
    EventCatalogUIAdapter adapter = null;
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      MetadataList metadataList = new MetadataList();
      // Try the main catalog first as we load it from the index.
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        collection = EventUtils.getEventMetadata(event, eventCatalogUIAdapter);
        adapter = eventCatalogUIAdapter;
      } else {
        metadataList.add(eventCatalogUIAdapter, EventUtils.getEventMetadata(event, eventCatalogUIAdapter));
      }

      // Try the other catalogs
      List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
      catalogUIAdapters.remove(eventCatalogUIAdapter);
      MediaPackage mediaPackage = indexService.getEventMediapackage(event);
      if (catalogUIAdapters.size() > 0) {
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
        return ApiResponses.notFound("Cannot find a catalog with type '%s' for event with id '%s'.", type, id);
      }

      for (String key : updatedFields.keySet()) {
        if ("subjects".equals(key)) {
          MetadataField<?> field = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
          if (field == null) {
            return ApiResponses.notFound(
                    "Cannot find a metadata field with id '%s' from event with id '%s' and the metadata type '%s'.",
                    key, id, type);
          } else if (field.isRequired() && StringUtils.isBlank(updatedFields.get(key))) {
            return R.badRequest(String.format(
                    "The event metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
                    key, type));
          }
          collection.removeField(field);
          JSONArray subjectArray = (JSONArray) parser.parse(updatedFields.get(key));
          collection.addField(
                  MetadataField.copyMetadataFieldWithValue(field, StringUtils.join(subjectArray.iterator(), ",")));
        } else {

          MetadataField<?> field = collection.getOutputFields().get(key);
          if (field == null) {
            return ApiResponses.notFound(
                    "Cannot find a metadata field with id '%s' from event with id '%s' and the metadata type '%s'.",
                    key, id, type);
          } else if (field.isRequired() && StringUtils.isBlank(updatedFields.get(key))) {
            return R.badRequest(String.format(
                    "The event metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
                    key, type));
          }
          collection.removeField(field);
          collection.addField(MetadataField.copyMetadataFieldWithValue(field, updatedFields.get(key)));
        }
      }

      metadataList.add(adapter, collection);
      indexService.updateEventMetadata(id, metadataList, externalIndex);
      return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{eventId}/metadata")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "deleteeventmetadata", description = "Delete the metadata namespace catalog of the specified event. This will remove all fields and values of the catalog.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to delete", type = STRING) }, reponses = {
                          @RestResponse(description = "The metadata of the given namespace has been updated.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The main metadata catalog dublincore/episode cannot be deleted as it has mandatory fields.", responseCode = HttpServletResponse.SC_FORBIDDEN),
                          @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEventMetadataByType(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("type") String type) throws SearchIndexException {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      Opt<MediaPackageElementFlavor> flavor = getFlavor(type);
      if (flavor.isNone()) {
        return R.badRequest(
                String.format("Unable to parse type '%s' as a flavor so unable to find the matching catalog.", type));
      }
      if (flavor.get().equals(eventCatalogUIAdapter.getFlavor())) {
        return Response
                .status(Status.FORBIDDEN).entity(String
                        .format("Unable to delete mandatory metadata catalog with type '%s' for event '%s'", type, id))
                .build();
      }
      try {
        indexService.removeCatalogByFlavor(event, flavor.get());
      } catch (NotFoundException e) {
        return ApiResponses.notFound(e.getMessage());
      } catch (IndexServiceException e) {
        logger.error("Unable to remove metadata catalog with type '{}' from event '{}' because {}",
                new Object[] { type, id, ExceptionUtils.getStackTrace(e) });
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
      } catch (IllegalStateException e) {
        logger.debug("Unable to remove metadata catalog with type '{}' from event '{}' because {}",
                new Object[] { type, id, ExceptionUtils.getStackTrace(e) });
        throw new WebApplicationException(e, Status.BAD_REQUEST);
      } catch (UnauthorizedException e) {
        return Response.status(Status.UNAUTHORIZED).build();
      }
      return ApiResponses.Json.noContent(ApiVersion.VERSION_1_0_0);
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @GET
  @Path("{eventId}/publications")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "geteventpublications", description = "Returns an event's list of publications.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The list of publications is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventPublications(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String id,
          @QueryParam("sign") boolean sign) throws Exception {
    try {
      return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, arr(getPublications(id, sign)));
    } catch (NotFoundException e) {
      return ApiResponses.notFound(e.getMessage());
    } catch (SearchIndexException e) {
      logger.error("Unable to get list of publications from event with id '{}' because {}", id,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  private final Fn2<Publication, Boolean, JObject> publicationToJson = new Fn2<Publication, Boolean, JObject>() {
    @Override
    public JObject apply(Publication publication, Boolean sign) {
      String url = publication.getURI() == null ? "" : publication.getURI().toString();
      return obj(f("id", v(publication.getIdentifier())), f("channel", v(publication.getChannel())),
              f("mediatype", v(publication.getMimeType(), BLANK)), f("url", v(url)),
              f("media", arr(getPublicationTracksJson(publication, sign))),
              f("attachments", arr(getPublicationAttachmentsJson(publication, sign))),
              f("metadata", arr(getPublicationCatalogsJson(publication, sign))));
    }

    private String getMediaPackageElementUri(MediaPackageElement element, Boolean sign) {
      String elementUri;
      if (sign) {
        elementUri = getSignedUri(element.getURI());
      } else {
        elementUri = element.getURI() == null ? null : element.getURI().toString();
      }
      return elementUri;
    }

    private String getSignedUri(URI uri) {
      if (uri == null) {
        return null;
      }

      String location = uri.toString();
      if (urlSigningService.accepts(location)) {
        try {
          location = urlSigningService.sign(location, expireSeconds, null, null);
        } catch (UrlSigningException e) {
          logger.error("Unable to sign URI {} because: {}", uri, ExceptionUtils.getStackTrace(e));
          return uri.toString();
        }
      }
      return location;
    }

    private List<JValue> getPublicationTracksJson(Publication publication, Boolean sign) {
      List<JValue> tracks = new ArrayList<>();
      for (Track track : publication.getTracks()) {

        VideoStream[] videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
        List<Field> trackInfo = new ArrayList<>();

        if (videoStreams.length > 0) {
          // Only supporting one stream, like in many other places...
          final Stream stream = videoStreams[0];
          if (stream instanceof VideoStream) {
            final VideoStream videoStream = (VideoStream) stream;
            if (videoStream.getBitRate() != null)
              trackInfo.add(f("bitrate", v(videoStream.getBitRate())));
            if (videoStream.getFrameRate() != null)
              trackInfo.add(f("framerate", v(videoStream.getFrameRate())));
            if (videoStream.getFrameCount() != null)
              trackInfo.add(f("framecount", v(videoStream.getFrameCount())));
            if (videoStream.getFrameWidth() != null)
              trackInfo.add(f("width", v(videoStream.getFrameWidth())));
            if (videoStream.getFrameHeight() != null)
              trackInfo.add(f("height", v(videoStream.getFrameHeight())));
          }
        }

        tracks.add(obj(f("id", v(track.getIdentifier(), BLANK)), f("mediatype", v(track.getMimeType(), BLANK)),
                f("url", v(getMediaPackageElementUri(track, sign), BLANK)), f("flavor", v(track.getFlavor(), BLANK)),
                f("size", v(track.getSize())), f("checksum", v(track.getChecksum(), BLANK)),
                f("tags", arr(track.getTags())), f("has_audio", v(track.hasAudio())),
                f("has_video", v(track.hasVideo())), f("duration", v(track.getDuration())),
                f("description", v(track.getDescription(), BLANK))).merge(trackInfo));
      }
      return tracks;
    }

    private List<JValue> getPublicationAttachmentsJson(Publication publication, Boolean sign) {
      List<JValue> attachments = new ArrayList<>();
      for (Attachment attachment : publication.getAttachments()) {
        attachments.add(
                obj(f("id", v(attachment.getIdentifier(), BLANK)), f("mediatype", v(attachment.getMimeType(), BLANK)),
                        f("url", v(getMediaPackageElementUri(attachment, sign), BLANK)),
                        f("flavor", v(attachment.getFlavor(), BLANK)), f("ref", v(attachment.getReference(), BLANK)),
                        f("size", v(attachment.getSize())), f("checksum", v(attachment.getChecksum(), BLANK)),
                        f("tags", arr(attachment.getTags()))));
      }
      return attachments;
    }

    private List<JValue> getPublicationCatalogsJson(Publication publication, Boolean sign) {
      List<JValue> catalogs = new ArrayList<>();
      for (Catalog catalog : publication.getCatalogs()) {
        catalogs.add(obj(f("id", v(catalog.getIdentifier(), BLANK)), f("mediatype", v(catalog.getMimeType(), BLANK)),
                f("url", v(getMediaPackageElementUri(catalog, sign), BLANK)),
                f("flavor", v(catalog.getFlavor(), BLANK)), f("size", v(catalog.getSize())),
                f("checksum", v(catalog.getChecksum(), BLANK)), f("tags", arr(catalog.getTags()))));
      }
      return catalogs;
    }
  };

  private List<JValue> getPublications(String id, Boolean withSignedUrls)
          throws NotFoundException, SearchIndexException {
    for (final Event event : indexService.getEvent(id, externalIndex)) {
      List<JValue> pubJSON = new ArrayList<>();
      pubJSON = new ArrayList<JValue>($(event.getPublications()).filter(EventUtils.internalChannelFilter)
              .map(publicationToJson._2(withSignedUrls)).toList());

      return pubJSON;
    }
    throw new NotFoundException(String.format("Unable to find event with id '%s'", id));
  }

  @GET
  @Path("{eventId}/publications/{publicationId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  @RestQuery(name = "geteventpublication", description = "Returns a single publication.", returnDescription = "", pathParameters = {
          @RestParameter(name = "eventId", description = "The event id", isRequired = true, type = STRING),
          @RestParameter(name = "publicationId", description = "The publication id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The track details are returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified event or publication does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventPublication(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String eventId,
          @PathParam("publicationId") String publicationId, @QueryParam("sign") boolean sign) throws Exception {
    try {
      return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, getPublication(eventId, publicationId, sign));
    } catch (NotFoundException e) {
      return ApiResponses.notFound(e.getMessage());
    } catch (SearchIndexException e) {
      logger.error("Unable to get list of publications from event with id '{}' because {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  private JObject getPublication(String eventId, String publicationId, Boolean withSignedUrls)
          throws SearchIndexException, NotFoundException {
    for (final Event event : indexService.getEvent(eventId, externalIndex)) {
      List<Publication> publications = $(event.getPublications()).filter(EventUtils.internalChannelFilter).toList();
      for (Publication publication : publications) {
        if (publicationId.equals(publication.getIdentifier())) {
          return $(publication).map(publicationToJson._2(withSignedUrls)).head2();
        }
      }
      throw new NotFoundException(
              String.format("Unable to find publication with id '%s' in event with id '%s'", publicationId, eventId));
    }
    throw new NotFoundException(String.format("Unable to find event with id '%s'", eventId));
  }

  /**
   * Change the simplified fields of key values provided to the external api into a {@link MetadataList}.
   *
   * @param json
   *          The json string that contains an array of metadata field lists for the different catalogs.
   * @return A {@link MetadataList} with the fields populated with the values provided.
   * @throws ParseException
   *           Thrown if unable to parse the json string.
   * @throws NotFoundException
   *           Thrown if unable to find the catalog or field that the json refers to.
   */
  protected MetadataList deserializeMetadataList(String json) throws ParseException, NotFoundException {
    MetadataList metadataList = new MetadataList();
    JSONArray jsonCatalogs = (JSONArray) parser.parse(json);
    for (int i = 0; i < jsonCatalogs.size(); i++) {
      JSONObject catalog = (JSONObject) jsonCatalogs.get(i);
      String flavorString = catalog.get("flavor").toString();
      if (StringUtils.trimToNull(flavorString) == null) {
        throw new IllegalArgumentException(
                "Unable to create new event as no flavor was given for one of the metadata collections");
      }

      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

      MetadataCollection collection = null;
      EventCatalogUIAdapter adapter = null;
      for (EventCatalogUIAdapter eventCatalogUIAdapter : getEventCatalogUIAdapters()) {
        if (eventCatalogUIAdapter.getFlavor().equals(flavor)) {
          adapter = eventCatalogUIAdapter;
          collection = eventCatalogUIAdapter.getRawFields();
        }
      }

      if (collection == null) {
        throw new IllegalArgumentException(
                String.format("Unable to find an EventCatalogUIAdapter with Flavor '%s'", flavorString));
      }

      String fieldsJson = catalog.get("fields").toString();
      if (StringUtils.trimToNull(fieldsJson) != null) {
        Map<String, String> fields = RequestUtils.getKeyValueMap(fieldsJson);
        for (String key : fields.keySet()) {
          MetadataField<?> field = collection.getOutputFields().get(key);
          if (field == null) {
            throw new NotFoundException(String.format(
                    "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
          }
          collection.removeField(field);
          collection.addField(MetadataField.copyMetadataFieldWithValue(field, fields.get(key)));
        }
      }
      metadataList.add(adapter, collection);
    }
    return metadataList;
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
      logger.error("Unable to parse access policy because: {}", ExceptionUtils.getStackTrace(e));
    }
    return activeAcl;
  }

  private JValue getJsonStream(Stream stream) {
    List<Field> fields = new ArrayList<>();
    if (stream instanceof AudioStream) {
      AudioStream audioStream = (AudioStream) stream;
      if (audioStream.getBitDepth() != null)
        fields.add(f("bitdepth", v(audioStream.getBitDepth())));
      if (audioStream.getBitRate() != null)
        fields.add(f("bitrate", v(audioStream.getBitRate())));
      if (audioStream.getCaptureDevice() != null)
        fields.add(f("capturedevice", v(audioStream.getCaptureDevice())));
      if (audioStream.getCaptureDeviceVendor() != null)
        fields.add(f("capturedevicevendor", v(audioStream.getCaptureDeviceVendor())));
      if (audioStream.getCaptureDeviceVersion() != null)
        fields.add(f("capturedeviceversion", v(audioStream.getCaptureDeviceVersion())));
      if (audioStream.getChannels() != null)
        fields.add(f("channels", v(audioStream.getChannels())));
      if (audioStream.getEncoderLibraryVendor() != null)
        fields.add(f("encoderlibraryvendor", v(audioStream.getEncoderLibraryVendor())));
      if (audioStream.getFormat() != null)
        fields.add(f("format", v(audioStream.getFormat())));
      if (audioStream.getFormatVersion() != null)
        fields.add(f("formatversion", v(audioStream.getFormatVersion())));
      if (audioStream.getFrameCount() != null)
        fields.add(f("framecount", v(audioStream.getFrameCount())));
      if (audioStream.getIdentifier() != null)
        fields.add(f("identifier", v(audioStream.getIdentifier())));
      if (audioStream.getPkLevDb() != null)
        fields.add(f("pklevdb", v(audioStream.getPkLevDb())));
      if (audioStream.getRmsLevDb() != null)
        fields.add(f("rmslevdb", v(audioStream.getRmsLevDb())));
      if (audioStream.getRmsPkDb() != null)
        fields.add(f("rmspkdb", v(audioStream.getRmsPkDb())));
      if (audioStream.getSamplingRate() != null)
        fields.add(f("samplingrate", v(audioStream.getSamplingRate())));
    } else if (stream instanceof VideoStream) {
      VideoStream videoStream = (VideoStream) stream;
      if (videoStream.getBitRate() != null)
        fields.add(f("bitrate", v(videoStream.getBitRate())));
      if (videoStream.getCaptureDevice() != null)
        fields.add(f("capturedevice", v(videoStream.getCaptureDevice())));
      if (videoStream.getCaptureDeviceVendor() != null)
        fields.add(f("capturedevicevendor", v(videoStream.getCaptureDeviceVendor())));
      if (videoStream.getCaptureDeviceVersion() != null)
        fields.add(f("capturedeviceversion", v(videoStream.getCaptureDeviceVersion())));
      if (videoStream.getEncoderLibraryVendor() != null)
        fields.add(f("encoderlibraryvendor", v(videoStream.getEncoderLibraryVendor())));
      if (videoStream.getFormat() != null)
        fields.add(f("format", v(videoStream.getFormat())));
      if (videoStream.getFormatVersion() != null)
        fields.add(f("formatversion", v(videoStream.getFormatVersion())));
      if (videoStream.getFrameCount() != null)
        fields.add(f("framecount", v(videoStream.getFrameCount())));
      if (videoStream.getFrameHeight() != null)
        fields.add(f("frameheight", v(videoStream.getFrameHeight())));
      if (videoStream.getFrameRate() != null)
        fields.add(f("framerate", v(videoStream.getFrameRate())));
      if (videoStream.getFrameWidth() != null)
        fields.add(f("framewidth", v(videoStream.getFrameWidth())));
      if (videoStream.getIdentifier() != null)
        fields.add(f("identifier", v(videoStream.getIdentifier())));
      if (videoStream.getScanOrder() != null)
        fields.add(f("scanorder", v(videoStream.getScanOrder().toString())));
      if (videoStream.getScanType() != null)
        fields.add(f("scantype", v(videoStream.getScanType().toString())));
    }
    return obj(fields);
  }

  private String getEventUrl(String eventId) {
    return UrlSupport.concat(serverUrl, serviceUrl, eventId);
  }

}
