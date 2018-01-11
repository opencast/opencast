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

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.api.IndexService.Source;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.message.broker.api.eventstatuschange.EventStatusChangeItem;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.Jsons.Functions;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

@Path("/")
@RestService(name = "toolsService", title = "Tools API Service",
  abstractText = "Provides a location for the tools API.",
  notes = { "This service provides a location for the tools API for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class ToolsEndpoint extends AsynchronousEndpoint implements ManagedService {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ToolsEndpoint.class);

  /** The default file name for generated Smil catalogs. */
  private static final String TARGET_FILE_NAME = "cut.smil";

  /** The Json key for the cutting details object. */
  private static final String CONCAT_KEY = "concat";

  /** The Json key for the end of a segment. */
  private static final String END_KEY = "end";

  /** The Json key for the beginning of a segment. */
  private static final String START_KEY = "start";

  /** The Json key for the segments array. */
  private static final String SEGMENTS_KEY = "segments";

  /** The Json key for the tracks array. */
  private static final String TRACKS_KEY = "tracks";

  /** Tag that marks workflow for being used from the editor tool */
  private static final String EDITOR_WORKFLOW_TAG = "editor";

  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;

  // service references
  private AdminUIConfiguration adminUIConfiguration;
  private AdminUISearchIndex searchIndex;
  private AssetManager assetManager;
  private IndexService index;
  private SmilService smilService;
  private UrlSigningService urlSigningService;
  private WorkflowService workflowService;
  private Workspace workspace;

  /** OSGi DI. */
  void setAdminUIConfiguration(AdminUIConfiguration adminUIConfiguration) {
    this.adminUIConfiguration = adminUIConfiguration;
  }

  /** OSGi DI */
  void setAdminUISearchIndex(AdminUISearchIndex adminUISearchIndex) {
    this.searchIndex = adminUISearchIndex;
  }

  /** OSGi DI */
  void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi DI */
  void setIndexService(IndexService index) {
    this.index = index;
  }

  /** OSGi DI */
  void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  /** OSGi DI */
  void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  /** OSGi DI */
  void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  protected void activate(BundleContext bundleContext) {
    logger.info("Activate tools endpoint");
    super.activate(bundleContext);
  }

  @Override
  protected void deactivate(BundleContext bundleContext) {
    logger.info("Deactivate tools endpoint");
    super.deactivate(bundleContext);
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
            this.getClass().getSimpleName());
  }

  @GET
  @Path("{mediapackageid}.json")
  @RestQuery(name = "getAvailableTools", description = "Returns a list of tools which are currently available for the given media package.", returnDescription = "A JSON array with tools identifiers", pathParameters = {
          @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Available tools evaluated", responseCode = HttpServletResponse.SC_OK) })
  public Response getAvailableTools(@PathParam("mediapackageid") final String mediaPackageId) {
    final List<JValue> jTools = new ArrayList<>();
    if (isEditorAvailable(mediaPackageId))
      jTools.add(v("editor"));

    return RestUtils.okJson(obj(f("available", arr(jTools))));
  }

  private List<MediaPackageElement> getPreviewElementsFromPublication(Opt<Publication> publication) {
    List<MediaPackageElement> previewElements = new LinkedList<>();
    for (Publication p : publication) {
      for (Attachment attachment : p.getAttachments()) {
        if (elementHasPreviewFlavor(attachment)) {
          previewElements.add(attachment);
        }
      }
      for (Catalog catalog : p.getCatalogs()) {
        if (elementHasPreviewFlavor(catalog)) {
          previewElements.add(catalog);
        }
      }
      for (Track track : p.getTracks()) {
        if (elementHasPreviewFlavor(track)) {
          previewElements.add(track);
        }
      }
    }
    return previewElements;
  }

  private Boolean elementHasPreviewFlavor(MediaPackageElement element) {
    return element.getFlavor() != null
            && adminUIConfiguration.getPreviewSubtype().equals(element.getFlavor().getSubtype());
  }

  @GET
  @Path("{mediapackageid}/editor.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getVideoEditor", description = "Returns all the information required to get the editor tool started", returnDescription = "JSON object", pathParameters = {
          @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Media package found", responseCode = SC_OK),
                  @RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND) })
  public Response getVideoEditor(@PathParam("mediapackageid") final String mediaPackageId)
          throws IndexServiceException, NotFoundException {
    if (!isEditorAvailable(mediaPackageId))
      return R.notFound();

    // Select tracks
    final Event event = getEvent(mediaPackageId).get();
    final MediaPackage mp = index.getEventMediapackage(event);
    List<MediaPackageElement> previewPublications = getPreviewElementsFromPublication(getInternalPublication(mp));

    // Collect previews and tracks
    List<JValue> jPreviews = new ArrayList<>();
    List<JValue> jTracks = new ArrayList<>();
    for (MediaPackageElement element : previewPublications) {
      final URI elementUri;
      if (urlSigningService.accepts(element.getURI().toString())) {
        try {
          String clientIP = null;
          if (signWithClientIP) {
            clientIP = securityService.getUserIP();
          }
          elementUri = new URI(urlSigningService.sign(element.getURI().toString(), expireSeconds, null, clientIP));
        } catch (URISyntaxException e) {
          logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e));
          throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
        } catch (UrlSigningException e) {
          logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e));
          throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
        }
      } else {
        elementUri = element.getURI();
      }
      jPreviews.add(obj(f("uri", v(elementUri.toString())), f("flavor", v(element.getFlavor().getType()))));

      if (!Type.Track.equals(element.getElementType()))
        continue;

      JObject jTrack = obj(f("id", v(element.getIdentifier())), f("flavor", v(element.getFlavor().getType())));
      // Check if there's a waveform for the current track
      Opt<Attachment> optWaveform = getWaveformForTrack(mp, element);
      if (optWaveform.isSome()) {
        final URI waveformUri;
        if (urlSigningService.accepts(element.getURI().toString())) {
          try {
            waveformUri = new URI(
                    urlSigningService.sign(optWaveform.get().getURI().toString(), expireSeconds, null, null));
          } catch (URISyntaxException e) {
            logger.error("Error while trying to serialize the waveform urls because: {}", getStackTrace(e));
            throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
          } catch (UrlSigningException e) {
            logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e));
            throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
          }
        } else {
          waveformUri = optWaveform.get().getURI();
        }
        jTracks.add(jTrack.merge(obj(f("waveform", v(waveformUri.toString())))));
      } else {
        jTracks.add(jTrack);
      }

    }

    // Get existing segments
    List<JValue> jSegments = new ArrayList<>();
    for (Tuple<Long, Long> segment : getSegments(mp)) {
      jSegments.add(obj(f(START_KEY, v(segment.getA())), f(END_KEY, v(segment.getB()))));
    }

    // Get workflows
    List<JValue> jWorkflows = new ArrayList<>();
    for (WorkflowDefinition workflow : getEditingWorkflows()) {
      jWorkflows.add(obj(f("id", v(workflow.getId())), f("name", v(workflow.getTitle(), Jsons.BLANK))));
    }

    return RestUtils.okJson(obj(f("title", v(mp.getTitle(), Jsons.BLANK)),
            f("date", v(event.getRecordingStartDate(), Jsons.BLANK)),
            f("series", obj(f("id", v(event.getSeriesId(), Jsons.BLANK)), f("title", v(event.getSeriesName(), Jsons.BLANK)))),
            f("presenters", arr($(event.getPresenters()).map(Functions.stringToJValue))),
            f("previews", arr(jPreviews)), f(TRACKS_KEY, arr(jTracks)),
            f("duration", v(mp.getDuration())), f(SEGMENTS_KEY, arr(jSegments)), f("workflows", arr(jWorkflows))));
  }

  @POST
  @Path("{mediapackageid}/editor.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it", returnDescription = "", pathParameters = {
          @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
                  @RestResponse(description = "Editing information saved and processed", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND),
                  @RestResponse(description = "The editing information cannot be parsed", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response editVideo(@PathParam("mediapackageid") final String mediaPackageId,
          @Context HttpServletRequest request) throws IndexServiceException, NotFoundException {
    String details;
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is);
    } catch (IOException e) {
      logger.error("Error reading request body: {}", getStackTrace(e));
      return R.serverError();
    }

    JSONParser parser = new JSONParser();
    EditingInfo editingInfo;
    try {
      JSONObject detailsJSON = (JSONObject) parser.parse(details);
      editingInfo = EditingInfo.parse(detailsJSON);
    } catch (Exception e) {
      logger.warn("Unable to parse concat information ({}): {}", details, ExceptionUtils.getStackTrace(e));
      return R.badRequest("Unable to parse details");
    }

    final Opt<Event> optEvent = getEvent(mediaPackageId);
    if (optEvent.isNone()) {
      return R.notFound();
    } else {
      submit(new VideoEditingRunnable(optEvent.get(), editingInfo, details));
    }

    return R.ok();
  }

  /**
   * Creates a SMIL cutting catalog based on the passed editing information and the media package.
   *
   * @param editingInfo
   *          the editing information
   * @param mediaPackage
   *          the media package
   * @return a SMIL catalog
   * @throws SmilException
   *           if creating the SMIL catalog failed
   */
  Smil createSmilCuttingCatalog(final EditingInfo editingInfo, final MediaPackage mediaPackage) throws SmilException {
    // Create initial SMIL catalog
    SmilResponse smilResponse = smilService.createNewSmil(mediaPackage);

    // Add tracks to the SMIL catalog
    ArrayList<Track> tracks = new ArrayList<>();

    for (final String trackId : editingInfo.getConcatTracks()) {
      Track track = mediaPackage.getTrack(trackId);
      if (track == null) {
        Opt<Track> trackOpt = getInternalPublication(mediaPackage).toStream().bind(new Fn<Publication, List<Track>>() {
          @Override
          public List<Track> apply(Publication a) {
            return Arrays.asList(a.getTracks());
          }
        }).filter(new Fn<Track, Boolean>() {
          @Override
          public Boolean apply(Track a) {
            return trackId.equals(a.getIdentifier());
          }
        }).head();
        if (trackOpt.isNone())
          throw new IllegalStateException(
                  format("The track '%s' doesn't exist in media package '%s'", trackId, mediaPackage));

        track = trackOpt.get();
      }
      tracks.add(track);
    }

    for (Tuple<Long, Long> segment : editingInfo.getConcatSegments()) {
      smilResponse = smilService.addParallel(smilResponse.getSmil());
      final String parentId = smilResponse.getEntity().getId();

      final Long duration = segment.getB() - segment.getA();
      smilResponse = smilService.addClips(smilResponse.getSmil(), parentId, tracks.toArray(new Track[tracks.size()]),
              segment.getA(), duration);
    }

    return smilResponse.getSmil();
  }

  /**
   * Adds the SMIL file as {@link Catalog} to the media package and sends the updated media package to the archive.
   *
   * @param mediaPackage
   *          the media package to at the SMIL catalog
   * @param smil
   *          the SMIL catalog
   * @return the updated media package
   * @throws IOException
   *           if the SMIL catalog cannot be read or not be written to the archive
   */
  MediaPackage addSmilToArchive(MediaPackage mediaPackage, final Smil smil) throws IOException {
   MediaPackageElementFlavor mediaPackageElementFlavor = adminUIConfiguration.getSmilCatalogFlavor();
   //set default catalog Id if there is none existing
    String catalogId = smil.getId();
    Catalog[] catalogs = mediaPackage.getCatalogs();

    //get the first smil/cutting  catalog-ID to overwrite it with new smil info
    for (Catalog p: catalogs) {
       if (p.getFlavor().matches(mediaPackageElementFlavor)) {
         logger.debug("Set Idendifier for Smil-Catalog to: " + p.getIdentifier());
         catalogId = p.getIdentifier();
       break;
       }
     }
     Catalog catalog = mediaPackage.getCatalog(catalogId);

    URI smilURI;
    try (InputStream is = IOUtils.toInputStream(smil.toXML(), "UTF-8")) {
      smilURI = workspace.put(mediaPackage.getIdentifier().compact(), catalogId, TARGET_FILE_NAME, is);
    } catch (SAXException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new IOException(e);
    } catch (JAXBException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new IOException(e);
    }

    if (catalog == null) {
      MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
              adminUIConfiguration.getSmilCatalogFlavor());
      mediaPackage.add(catalog);
    }
    catalog.setURI(smilURI);
    catalog.setIdentifier(catalogId);
    catalog.setMimeType(MimeTypes.XML);
    for (String tag : adminUIConfiguration.getSmilCatalogTags()) {
      catalog.addTag(tag);
    }
    // setting the URI to a new source so the checksum will most like be invalid
    catalog.setChecksum(null);

    try {
      // FIXME SWITCHP-333: Start in new thread
      assetManager.takeSnapshot(DEFAULT_OWNER, mediaPackage);
    } catch (AssetManagerException e) {
      logger.error("Error while adding the updated media package ({}) to the archive: {}", mediaPackage.getIdentifier(),
              e.getMessage());
      throw new IOException(e);
    }

    return mediaPackage;
  }

  private Opt<Publication> getInternalPublication(MediaPackage mp) {
    return $(mp.getPublications()).filter(new Fn<Publication, Boolean>() {
      @Override
      public Boolean apply(Publication a) {
        return InternalPublicationChannel.CHANNEL_ID.equals(a.getChannel());
      }
    }).head();
  }

  /**
   * Returns {@code true} if the media package is ready to be edited.
   *
   * @param mediaPackageId
   *          the media package identifier
   */
  private boolean isEditorAvailable(final String mediaPackageId) {
    final Opt<Event> optEvent = getEvent(mediaPackageId);
    if (optEvent.isSome()) {
      return Source.ARCHIVE.equals(index.getEventSource(optEvent.get()));
    } else {
      // No event found
      return false;
    }
  }

  /**
   * Get an {@link Event}
   *
   * @param mediaPackageId
   *          The mediapackage id that is also the event id.
   * @return The event if available or none if it is missing.
   */
  private Opt<Event> getEvent(final String mediaPackageId) {
    try {
      return index.getEvent(mediaPackageId, searchIndex);
    } catch (SearchIndexException e) {
      logger.error("Error while reading event '{}' from search index: {}", mediaPackageId, getStackTrace(e));
      return Opt.none();
    }
  }

  /**
   * Tries to find a waveform for a given track in the media package. If a waveform is found the corresponding
   * {@link Publication} is returned, {@link Opt#none()} otherwise.
   *
   * @param mp
   *          the media package to scan for the waveform
   * @param track
   *          the track
   */
  private Opt<Attachment> getWaveformForTrack(final MediaPackage mp, final MediaPackageElement track) {
    return $(getInternalPublication(mp)).bind(new Fn<Publication, List<Attachment>>() {
      @Override
      public List<Attachment> apply(Publication a) {
        return Arrays.asList(a.getAttachments());
      }
    }).filter(new Fn<Attachment, Boolean>() {
      @Override
      public Boolean apply(Attachment att) {
        if (track.getFlavor() == null || att.getFlavor() == null)
          return false;

        return track.getFlavor().getType().equals(att.getFlavor().getType())
                && att.getFlavor().getSubtype().equals(adminUIConfiguration.getWaveformSubtype());
      }
    }).head();
  }

  /**
   * Returns a list of workflow definitions that may be applied to a media package after segments have been defined with
   * the editor tool.
   *
   * @return a list of workflow definitions
   */
  private List<WorkflowDefinition> getEditingWorkflows() {
    List<WorkflowDefinition> workflows;
    try {
      workflows = workflowService.listAvailableWorkflowDefinitions();
    } catch (WorkflowDatabaseException e) {
      logger.warn("Error while retrieving list of workflow definitions: {}", getStackTrace(e));
      return emptyList();
    }

    return $(workflows).filter(new Fn<WorkflowDefinition, Boolean>() {
      @Override
      public Boolean apply(WorkflowDefinition a) {
        return a.containsTag(EDITOR_WORKFLOW_TAG);
      }
    }).toList();
  }

  /**
   * Analyzes the media package and tries to get information about segments out of it.
   *
   * @param mediaPackage
   *          the media package
   * @return a list of segments or an empty list if no segments could be found.
   */
  private List<Tuple<Long, Long>> getSegments(final MediaPackage mediaPackage) {
    List<Tuple<Long, Long>> segments = new ArrayList<>();
    for (Catalog smilCatalog : mediaPackage.getCatalogs(adminUIConfiguration.getSmilCatalogFlavor())) {
      try {
        Smil smil = smilService.fromXml(workspace.get(smilCatalog.getURI())).getSmil();
        segments = mergeSegments(segments, getSegmentsFromSmil(smil));
      } catch (NotFoundException e) {
        logger.warn("File '{}' could not be loaded by workspace service: {}", smilCatalog.getURI(), getStackTrace(e));
      } catch (IOException e) {
        logger.warn("Reading file '{}' from workspace service failed: {}", smilCatalog.getURI(), getStackTrace(e));
      } catch (SmilException e) {
        logger.warn("Error while parsing SMIL catalog '{}': {}", smilCatalog.getURI(), getStackTrace(e));
      }
    }

    if (!segments.isEmpty())
      return segments;

    // Read from silence detection flavors
    for (Catalog smilCatalog : mediaPackage.getCatalogs(adminUIConfiguration.getSmilSilenceFlavor())) {
      try {
        Smil smil = smilService.fromXml(workspace.get(smilCatalog.getURI())).getSmil();
        segments = mergeSegments(segments, getSegmentsFromSmil(smil));
      } catch (NotFoundException e) {
        logger.warn("File '{}' could not be loaded by workspace service: {}", smilCatalog.getURI(), getStackTrace(e));
      } catch (IOException e) {
        logger.warn("Reading file '{}' from workspace service failed: {}", smilCatalog.getURI(), getStackTrace(e));
      } catch (SmilException e) {
        logger.warn("Error while parsing SMIL catalog '{}': {}", smilCatalog.getURI(), getStackTrace(e));
      }
    }

    // Check for single segment to ignore
    if (segments.size() == 1) {
      Tuple<Long, Long> singleSegment = segments.get(0);
      if (singleSegment.getA() == 0 && singleSegment.getB() >= mediaPackage.getDuration())
        segments.remove(0);
    }

    return segments;
  }

  protected List<Tuple<Long, Long>> mergeSegments(List<Tuple<Long, Long>> segments, List<Tuple<Long, Long>> segments2) {
    // Merge conflicting segments
    List<Tuple<Long, Long>> mergedSegments = mergeInternal(segments, segments2);

    // Sort segments
    Collections.sort(mergedSegments, new Comparator<Tuple<Long, Long>>() {
      @Override
      public int compare(Tuple<Long, Long> t1, Tuple<Long, Long> t2) {
        return t1.getA().compareTo(t2.getA());
      }
    });

    return mergedSegments;
  }

  /**
   * Merges two different segments lists together. Keeps untouched segments and combines touching segments by the
   * overlapping points.
   *
   * @param segments
   *          the first segments to be merge
   * @param segments2
   *          the second segments to be merge
   * @return the merged segments
   */
  private List<Tuple<Long, Long>> mergeInternal(List<Tuple<Long, Long>> segments, List<Tuple<Long, Long>> segments2) {
    for (Iterator<Tuple<Long, Long>> it = segments.iterator(); it.hasNext();) {
      Tuple<Long, Long> seg = it.next();
      for (Iterator<Tuple<Long, Long>> it2 = segments2.iterator(); it2.hasNext();) {
        Tuple<Long, Long> seg2 = it2.next();
        long combinedStart = Math.max(seg.getA(), seg2.getA());
        long combinedEnd = Math.min(seg.getB(), seg2.getB());
        if (combinedEnd > combinedStart) {
          it.remove();
          it2.remove();
          List<Tuple<Long, Long>> newSegments = new ArrayList<>(segments);
          newSegments.add(tuple(combinedStart, combinedEnd));
          return mergeInternal(newSegments, segments2);
        }
      }
    }
    segments.addAll(segments2);
    return segments;
  }

  /**
   * Extracts the segments of a SMIL catalog and returns them as a list of tuples (start, end).
   *
   * @param smil
   *          the SMIL catalog
   * @return the list of segments
   */
  List<Tuple<Long, Long>> getSegmentsFromSmil(Smil smil) {
    List<Tuple<Long, Long>> segments = new ArrayList<>();
    for (SmilMediaObject elem : smil.getBody().getMediaElements()) {
      if (elem instanceof SmilMediaContainer) {
        SmilMediaContainer mediaContainer = (SmilMediaContainer) elem;
        for (SmilMediaObject video : mediaContainer.getElements()) {
          if (video instanceof SmilMediaElement) {
            SmilMediaElement videoElem = (SmilMediaElement) video;
            try {
              segments.add(Tuple.tuple(videoElem.getClipBeginMS(), videoElem.getClipEndMS()));
              break;
            } catch (SmilException e) {
              logger.warn("Media element '{}' of SMIL catalog '{}' seems to be invalid: {}",
                      videoElem, smil, e);
            }
          }
        }
      }
    }
    return segments;
  }

  private final class VideoEditingRunnable extends WorkStartingRunnable {

    private Event event;
    private EditingInfo editingInfo;
    private String details;

    private VideoEditingRunnable(Event event, EditingInfo editingInfo, String details) {
      super(Collections.singletonList(event.getIdentifier()));
      this.event = event;
      this.editingInfo = editingInfo;
      this.details = details;
    }

    @Override
    public void doWork() {

      MediaPackage mediaPackage = null;
      try {
        mediaPackage = index.getEventMediapackage(event);
      } catch (IndexServiceException e) {
        reportEventStatusChange(EventStatusChangeItem.Type.Failed,
          "Unable to retrieve mediapackage with id: " + event.getIdentifier() + " from IndexService",
          eventIds);
        return;
      }
      if (mediaPackage == null) {
        reportEventStatusChange(EventStatusChangeItem.Type.Failed,
          "Could not find mediapackage with id: " + event.getIdentifier(),
          eventIds);
        return;
      }
      Smil smil;
      try {
        smil = createSmilCuttingCatalog(editingInfo, mediaPackage);
      } catch (Exception e) {
        logger.warn("Unable to create a SMIL cutting catalog ({}): {}", details, getStackTrace(e));
        reportEventStatusChange(EventStatusChangeItem.Type.Failed,
          "Unable to create SMIL cutting catalog",
          eventIds);
        return;
      }

      try {
        addSmilToArchive(mediaPackage, smil);
      } catch (IOException e) {
        logger.warn("Unable to add SMIL cutting catalog to archive: {}", getStackTrace(e));
        reportEventStatusChange(EventStatusChangeItem.Type.Failed,
          "Unable to add SMIL cutting catalog to archive",
          eventIds);
        return;
      }

      if (editingInfo.getPostProcessingWorkflow().isSome()) {
        final String workflowId = editingInfo.getPostProcessingWorkflow().get();
        try {
          final Workflows workflows = new Workflows(assetManager, workspace, workflowService);
          workflows.applyWorkflowToLatestVersion($(mediaPackage.getIdentifier().toString()),
            ConfiguredWorkflow.workflow(workflowService.getWorkflowDefinitionById(workflowId))).run();
        } catch (AssetManagerException e) {
          logger.warn("Unable to start workflow '{}' on archived media package '{}': {}",
                  workflowId, mediaPackage, getStackTrace(e));
          reportEventStatusChange(EventStatusChangeItem.Type.Failed,
            "Unable to start workflow '" + workflowId + "' on archived media package: "
              + event.getIdentifier(), eventIds);
          return;
        } catch (WorkflowDatabaseException e) {
          logger.warn("Unable to load workflow '{}' from workflow service: {}", workflowId, getStackTrace(e));
          reportEventStatusChange(EventStatusChangeItem.Type.Failed,
            "Unable to load workflow '" + workflowId + "'", eventIds);
          return;
        } catch (NotFoundException e) {
          logger.warn("Workflow '{}' not found", workflowId);
          reportEventStatusChange(EventStatusChangeItem.Type.Failed,
            "Unable to find workflow '" + workflowId + "'", eventIds);
          return;
        }
      }

    }
  }



  /** Provides access to the parsed editing information */
  static final class EditingInfo {

    private final List<Tuple<Long, Long>> segments;
    private final List<String> tracks;
    private final Opt<String> workflow;

    private EditingInfo(List<Tuple<Long, Long>> segments, List<String> tracks, Opt<String> workflow) {
      this.segments = segments;
      this.tracks = tracks;
      this.workflow = workflow;
    }

    /**
     * Parse {@link JSONObject} to {@link EditingInfo}.
     *
     * @param obj
     *          the JSON object to parse
     * @return all editing information found in the JSON object
     */
    static EditingInfo parse(JSONObject obj) {

      JSONObject concatObject = requireNonNull((JSONObject) obj.get(CONCAT_KEY));
      JSONArray jsonSegments = requireNonNull((JSONArray) concatObject.get(SEGMENTS_KEY));
      JSONArray jsonTracks = requireNonNull((JSONArray) concatObject.get(TRACKS_KEY));

      List<Tuple<Long, Long>> segments = new ArrayList<>();
      for (Object segment : jsonSegments) {
        final JSONObject jSegment = (JSONObject) segment;
        final Long start = (Long) jSegment.get(START_KEY);
        final Long end = (Long) jSegment.get(END_KEY);
        if (end < start)
          throw new IllegalArgumentException("The end date of a segment must be after the start date of the segment");
        segments.add(Tuple.tuple(start, end));
      }

      List<String> tracks = new ArrayList<>();
      for (Object track : jsonTracks) {
        tracks.add((String) track);
      }

      return new EditingInfo(segments, tracks, Opt.nul((String) obj.get("workflow")));
    }

    /**
     * Returns a list of {@link Tuple} that each represents a segment. {@link Tuple#getA()} marks the start point,
     * {@link Tuple#getB()} the endpoint of the segement.
     */
    List<Tuple<Long, Long>> getConcatSegments() {
      return Collections.unmodifiableList(segments);
    }

    /** Returns a list of track identifiers. */
    List<String> getConcatTracks() {
      return Collections.unmodifiableList(tracks);
    }

    /** Returns the optional workflow to start */
    Opt<String> getPostProcessingWorkflow() {
      return workflow;
    }
  }

}
