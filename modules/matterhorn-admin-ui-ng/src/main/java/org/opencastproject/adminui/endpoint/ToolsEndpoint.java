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

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.api.IndexService.Source;
import org.opencastproject.index.service.exception.InternalServerErrorException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.TrackSelector;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
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
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JObjectWrite;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

@Path("/")
@RestService(name = "toolsService", title = "Tools API Service", notes = "", abstractText = "Provides a location for the tools API.")
public class ToolsEndpoint {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ToolsEndpoint.class);

  private static final String DEFAULT_SOURCE_TRACK_FLAVOR = "*/work";
  private static final String DEFAULT_PREVIEW_PUBLICATION_FLAVOR_SUBTYPE = "preview";
  private static final String DEFAULT_WAVEFORM_PUBLICATION_FLAVOR_SUBTYPE = "waveform";

  /** The default file name for generated Smil catalogs. */
  static final String TARGET_FILE_NAME = "cut.smil";

  /** The flavor for the SMIL cutting catalog */
  static final MediaPackageElementFlavor SMIL_CATALOG_FLAVOR = new MediaPackageElementFlavor("smil", "cutting");

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

  /** A parser for handling JSON documents inside the body of a request. **/
  private final JSONParser parser = new JSONParser();

  // service references
  private AdminUISearchIndex searchIndex;
  private Archive<?> archive;
  private HttpMediaPackageElementProvider mpElementProvider;
  private IndexService index;
  private SmilService smilService;
  private WorkflowService workflowService;
  private Workspace workspace;

  /** OSGi DI */
  void setAdminUISearchIndex(AdminUISearchIndex adminUISearchIndex) {
    this.searchIndex = adminUISearchIndex;
  }

  /** OSGi DI */
  void setArchive(Archive<?> archive) {
    this.archive = archive;
  }

  /** OSGi DI */
  void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider mpElementProvider) {
    this.mpElementProvider = mpElementProvider;
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
  void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @GET
  @Path("{mediapackageid}.json")
  @RestQuery(name = "getAvailableTools", description = "Returns a list of tools which are currently available for the given media package.", returnDescription = "A JSON array with tools identifiers", pathParameters = { @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Available tools evaluated", responseCode = HttpServletResponse.SC_OK) })
  public Response getAvailableTools(@PathParam("mediapackageid") final String mediaPackageId) {
    final List<JValue> jTools = new ArrayList<JValue>();
    if (isEditorAvailable(mediaPackageId))
      jTools.add(v("editor"));

    return RestUtils.okJson(j(f("available", a(jTools))));
  }

  @GET
  @Path("{mediapackageid}/editor.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getVideoEditor", description = "Returns all the information required to get the editor tool started", returnDescription = "JSON object", pathParameters = { @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Media package found", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getVideoEditor(@PathParam("mediapackageid") final String mediaPackageId) {
    if (!isEditorAvailable(mediaPackageId)) {
      return R.notFound();
    } else {
      // Select tracks
      final Opt<MediaPackage> optMP = getMediaPackage(mediaPackageId);
      if (optMP.isSome()) {
        final MediaPackage mp = optMP.get();
        final List<Publication> previewPublications = Stream.mk(mp.getPublications())
                .filter(new Fn<Publication, Boolean>() {
                  @Override
                  public Boolean ap(Publication pub) {
                    return pub.getFlavor() != null
                            && DEFAULT_PREVIEW_PUBLICATION_FLAVOR_SUBTYPE.equals(pub.getFlavor().getSubtype());
                  }
                }).toList();
        final Collection<Track> sourceTracks = getEditorSourceTrackSelector().select(mp, false);

        // Collect previews
        List<JValue> jPreviews = new ArrayList<JValue>();
        for (Publication pub : previewPublications) {
          jPreviews.add(j(f("uri", v(pub.getURI().toString()))));
        }

        // Collect tracks
        List<JValue> jTracks = new ArrayList<JValue>();
        for (Track track : sourceTracks) {
          JObjectWrite jTrack = j(f("id", v(track.getIdentifier())), f("flavor", v(track.getFlavor().toString())),
                  f("mimetype", v(track.getMimeType().toString())));
          // Check if there's a waveform for the current track
          Opt<Publication> optWaveform = getWaveformForTrack(mp, track);
          if (optWaveform.isSome()) {
            jTracks.add(jTrack.merge(j(f("waveform", v(optWaveform.get().getURI().toString())))));
          } else {
            jTracks.add(jTrack);
          }
        }

        // Get existing segments
        List<JValue> jSegments = new ArrayList<JValue>();
        for (Tuple<Long, Long> segment : getSegments(mediaPackageId)) {
          jSegments.add(j(f(START_KEY, v(segment.getA())), f(END_KEY, v(segment.getB()))));
        }

        // Get workflows
        List<JValue> jWorkflows = new ArrayList<JValue>();
        for (WorkflowDefinition workflow : getEditingWorkflows()) {
          jWorkflows.add(j(f("id", v(workflow.getId())), f("name", vN(workflow.getTitle()))));
        }

        return RestUtils.okJson(j(f("previews", a(jPreviews)), f(TRACKS_KEY, a(jTracks)),
                f("duration", v(mp.getDuration())), f(SEGMENTS_KEY, a(jSegments)), f("workflows", a(jWorkflows))));
      } else {
        return R.notFound();
      }
    }
  }

  @POST
  @Path("{mediapackageid}/editor.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it", returnDescription = "", pathParameters = { @RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Editing information saved and processed", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "The editing information cannot be parsed", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response editVideo(@PathParam("mediapackageid") final String mediaPackageId,
          @Context HttpServletRequest request) {
    String details;
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is);
    } catch (IOException e) {
      logger.error("Error reading request body: {}", ExceptionUtils.getStackTrace(e));
      return R.serverError();
    }

    EditingInfo editingInfo;
    try {
      JSONObject detailsJSON = (JSONObject) parser.parse(details);
      editingInfo = EditingInfo.parse(detailsJSON);
    } catch (Exception e) {
      logger.warn("Unable to parse concat information ({}): {}", details, ExceptionUtils.getStackTrace(e));
      return R.badRequest("Unable to parse details");
    }

    Opt<MediaPackage> optMediaPackage = getMediaPackage(mediaPackageId);
    if (optMediaPackage.isNone()) {
      return R.notFound();
    } else {
      MediaPackage mediaPackage = optMediaPackage.get();
      Smil smil;
      try {
        smil = createSmilCuttingCatalog(editingInfo, mediaPackage);
      } catch (Exception e) {
        logger.warn("Unable to create a SMIL cutting catalog ({}): {}", details, ExceptionUtils.getStackTrace(e));
        return R.badRequest("Unable to create SMIL cutting catalog");
      }

      try {
        addSmilToArchive(mediaPackage, smil);
      } catch (IOException e) {
        logger.warn("Unable to add SMIL cutting catalog to archive: {}", ExceptionUtils.getStackTrace(e));
        return R.serverError();
      }

      if (editingInfo.getPostProcessingWorkflow().isSome()) {
        final String workflowId = editingInfo.getPostProcessingWorkflow().get();
        try {
          archive.applyWorkflow(ConfiguredWorkflow.workflow(workflowService.getWorkflowDefinitionById(workflowId)),
                  mpElementProvider.getUriRewriter(), Stream.$(mediaPackage.getIdentifier().toString()).toList());
        } catch (ArchiveException e) {
          logger.warn("Unable to start workflow '{}' on archived media package '{}': {}", new Object[] { workflowId,
                  mediaPackage, getStackTrace(e) });
          return R.serverError();
        } catch (WorkflowDatabaseException e) {
          logger.warn("Unable to load workflow '{}' from workflow service: {}", workflowId, getStackTrace(e));
          return R.serverError();
        } catch (NotFoundException e) {
          logger.warn("Workflow '{}' not found", workflowId);
          return R.badRequest("Workflow not found");
        }
      }

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

    for (String trackId : editingInfo.getConcatTracks()) {
      Track track = mediaPackage.getTrack(trackId);
      if (track == null) {
        throw new IllegalStateException(format("The track '%s' doesn't exist in media package '%s'", trackId,
                mediaPackage));
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

    URI smilURI;
    try (InputStream is = IOUtils.toInputStream(smil.toXML(), "UTF-8")) {
      smilURI = workspace.put(mediaPackage.getIdentifier().compact(), smil.getId(), TARGET_FILE_NAME, is);
    } catch (SAXException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new IOException(e);
    } catch (JAXBException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new IOException(e);
    }

    MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Catalog catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
            SMIL_CATALOG_FLAVOR);
    catalog.setIdentifier(smil.getId());
    mediaPackage.add(catalog);

    try {
      // FIXME SWITCHP-333: Start in new thread
      archive.add(mediaPackage);
    } catch (ArchiveException e) {
      logger.error("Error while adding the updated media package ({}) to the archive: {}",
              mediaPackage.getIdentifier(), e.getMessage());
      throw new IOException(e);
    }

    return mediaPackage;
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
    } catch (InternalServerErrorException e) {
      logger.error("Error while reading event '{}' from search index: {}", mediaPackageId,
              ExceptionUtils.getStackTrace(e));
      return Opt.none();
    }
  }

  /**
   * Get the {@link MediaPackage} to be used in this cutting operation.
   *
   * @param mediaPackageId
   *          The UUID of the mediapackage.
   * @return The {@link MediaPackage} if it is available.
   */
  private Opt<MediaPackage> getMediaPackage(final String mediaPackageId) {
    final Opt<Event> optEvent = getEvent(mediaPackageId);
    if (optEvent.isSome()) {
      try {
        return index.getEventMediapackage(optEvent.get());
      } catch (InternalServerErrorException e) {
        logger.error("Error while retrieving media package '{}': {}", mediaPackageId, getStackTrace(e));
        return Opt.none();
      }
    } else {
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
  private Opt<Publication> getWaveformForTrack(final MediaPackage mp, final Track track) {
    List<Publication> pubs = Stream.$(mp.getPublications()).filter(new Fn<Publication, Boolean>() {
      @Override
      public Boolean ap(Publication pub) {
        return track.getFlavor().getType().equals(pub.getFlavor().getType())
                && pub.getFlavor().getSubtype().equals(DEFAULT_WAVEFORM_PUBLICATION_FLAVOR_SUBTYPE);
      }
    }).toList();

    for (Publication pub : pubs) {
      return Opt.some(pub);
    }
    return Opt.none();
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

    return Stream.$(workflows).filter(new Fn<WorkflowDefinition, Boolean>() {
      @Override
      public Boolean ap(WorkflowDefinition a) {
        return a.containsTag(EDITOR_WORKFLOW_TAG);
      }
    }).toList();
  }

  /**
   * Analyzes the media package and tries to get information about segments out of it.
   * <p>
   * There are two possible sources for tracks:
   * <li>An existing SMIL cutting catalog
   * <li>A MPEG-7 catalog
   *
   * @param mediaPackageId
   *          the media package identifier
   * @return a list of segments or an empty list if no segments could be found.
   */
  private List<Tuple<Long, Long>> getSegments(final String mediaPackageId) {
    final Opt<MediaPackage> optMP = getMediaPackage(mediaPackageId);

    if (optMP.isSome()) {
      MediaPackage mp = optMP.get();
      for (Catalog smilCatalog : mp.getCatalogs(SMIL_CATALOG_FLAVOR)) {
        try {
          Smil smil = smilService.fromXml(workspace.get(smilCatalog.getURI())).getSmil();
          return getSegmentsFromSmil(smil);
        } catch (NotFoundException e) {
          logger.warn("File '{}' could not be loaded by workspace service: {}", smilCatalog.getURI(), getStackTrace(e));
        } catch (IOException e) {
          logger.warn("Reading file '{}' from workspace service failed: {}", smilCatalog.getURI(), getStackTrace(e));
        } catch (SmilException e) {
          logger.warn("Error while parsing SMIL catalog found in media package '{}': {}", mediaPackageId,
                  getStackTrace(e));
        }
      }
    }

    // Return an empty list if no segments could be found
    return Collections.emptyList();
  }

  /**
   * Extracts the segments of a SMIL catalog and returns them as a list of tuples (start, end).
   *
   * @param smil
   *          the SMIL catalog
   * @return the list of segments
   */
  List<Tuple<Long, Long>> getSegmentsFromSmil(Smil smil) {
    List<Tuple<Long, Long>> segments = new ArrayList<Tuple<Long, Long>>();
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
              logger.warn("Media element '{}' of SMIL catalog '{}' seems to be invalid: {}", new Object[] { videoElem,
                      smil, e });
            }
          }
        }
      }
    }
    return segments;
  }

  /**
   * Returns a {@link TrackSelector} to select the preview tracks of a {@link MediaPackage}.
   */
  private TrackSelector getEditorPreviewsTrackSelector() {
    final TrackSelector previewTracksSelector = new TrackSelector();
    previewTracksSelector.addFlavor(DEFAULT_PREVIEW_PUBLICATION_FLAVOR_SUBTYPE);
    return previewTracksSelector;
  }

  /**
   * Returns a {@link TrackSelector} to select the editor source tracks of a {@link MediaPackage}.
   */
  private TrackSelector getEditorSourceTrackSelector() {
    final TrackSelector sourceTrackSelector = new TrackSelector();
    sourceTrackSelector.addFlavor(DEFAULT_SOURCE_TRACK_FLAVOR);
    return sourceTrackSelector;
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
