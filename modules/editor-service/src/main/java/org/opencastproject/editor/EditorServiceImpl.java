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

package org.opencastproject.editor;

import static com.entwinemedia.fn.Stream.$;
import static java.util.Collections.emptyList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.editor.api.EditingData;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.editor.api.ErrorStatus;
import org.opencastproject.editor.api.SegmentData;
import org.opencastproject.editor.api.TrackData;
import org.opencastproject.editor.api.TrackSubData;
import org.opencastproject.editor.api.WorkflowData;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.util.EventUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
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
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowUtil;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;


//@Component(
//        property = {
//                "service.description=Editor Service"
//        },
//        immediate = true,
//        service =  { EditorService.class }
//)
public class EditorServiceImpl implements EditorService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(EditorServiceImpl.class);

  /** Tag that marks workflow for being used from the editor tool */
  private static final String EDITOR_WORKFLOW_TAG = "editor";


  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;

  // service references
  private IndexService index;
  private AssetManager assetManager;
  private SecurityService securityService;
  private SmilService smilService;
  private UrlSigningService urlSigningService;
  private WorkflowService workflowService;
  private Workspace workspace;

  private MediaPackageElementFlavor smilCatalogFlavor;
  private String previewVideoSubtype;
  private String previewTag;
  private String previewSubtype;
  private MediaPackageElementFlavor smilSilenceFlavor;
  private AdminUISearchIndex searchIndex;

  private static final String DEFAULT_PREVIEW_SUBTYPE = "prepared";
  private static final String DEFAULT_PREVIEW_TAG = "editor";
  private static final String DEFAULT_SMIL_CATALOG_FLAVOR = "smil/cutting";
  private static final String DEFAULT_SMIL_CATALOG_TAGS = "archive";
  private static final String DEFAULT_SMIL_SILENCE_FLAVOR = "*/silence";
  private static final String DEFAULT_PREVIEW_VIDEO_SUBTYPE = "video+preview";

  public static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  public static final String OPT_PREVIEW_TAG = "preview.tag";
  public static final String OPT_SMIL_CATALOG_FLAVOR = "smil.catalog.flavor";
  public static final String OPT_SMIL_CATALOG_TAGS = "smil.catalog.tags";
  public static final String OPT_SMIL_SILENCE_FLAVOR = "smil.silence.flavor";

  public static final String OPT_PREVIEW_VIDEO_SUBTYPE = "preview.video.subtype";

  private final Set<String> smilCatalogTagSet = new HashSet<>();

  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  public void setAdminUISearchIndex(AdminUISearchIndex adminUISearchIndex) {
    this.searchIndex = adminUISearchIndex;
  }

  public void setIndexService(IndexService index) {
    this.index = index;
  }

  public MediaPackageElementFlavor getSmilCatalogFlavor() {
    return smilCatalogFlavor;
  }

  public Set<String> getSmilCatalogTags() {
    return smilCatalogTagSet;
  }

  public String getPreviewVideoSubtype() {
    return previewVideoSubtype;
  }

  public MediaPackageElementFlavor getSmilSilenceFlavor() {
    return smilSilenceFlavor;
  }

  private String getPreviewSubtype() {
    return previewSubtype;
  }

  public String getPreviewTag() {
    return previewTag;
  }

  @Activate
  @Modified
  public void activate(ComponentContext cc) {
    Dictionary<String, Object> properties = cc.getProperties();
    if (properties == null) {
      return;
    }

    expireSeconds =  UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,this.getClass().getSimpleName());
    // Preview tag
    previewTag = Objects.toString(properties.get(OPT_PREVIEW_TAG), DEFAULT_PREVIEW_TAG);
    logger.debug("Preview tag configuration set to '{}'", previewTag);

    // Preview subtype
    previewSubtype = Objects.toString(properties.get(OPT_PREVIEW_SUBTYPE), DEFAULT_PREVIEW_SUBTYPE);
    logger.debug("Preview subtype configuration set to '{}'", previewSubtype);

    // SMIL catalog flavor
    smilCatalogFlavor = MediaPackageElementFlavor.parseFlavor(
            StringUtils.defaultString((String) properties.get(OPT_SMIL_CATALOG_FLAVOR), DEFAULT_SMIL_CATALOG_FLAVOR));
    logger.debug("Smil catalog flavor configuration set to '{}'", smilCatalogFlavor);

    // SMIL catalog tags
    String tags =  Objects.toString(properties.get(OPT_SMIL_CATALOG_TAGS), DEFAULT_SMIL_CATALOG_TAGS);
    String[] smilCatalogTags = StringUtils.split(tags, ",");
    smilCatalogTagSet.clear();
    if (smilCatalogTags != null) {
      smilCatalogTagSet.addAll(Arrays.asList(smilCatalogTags));
    }

    // SMIL silence flavor
    smilSilenceFlavor = MediaPackageElementFlavor.parseFlavor(
            StringUtils.defaultString((String) properties.get(OPT_SMIL_SILENCE_FLAVOR), DEFAULT_SMIL_SILENCE_FLAVOR));
    logger.debug("Smil silence flavor configuration set to '{}'", smilSilenceFlavor);

    // Preview Video subtype
    previewVideoSubtype =  Objects.toString(properties.get(OPT_PREVIEW_VIDEO_SUBTYPE), DEFAULT_PREVIEW_VIDEO_SUBTYPE);
    logger.debug("Preview video subtype set to '{}'", previewVideoSubtype);
  }

  private Boolean elementHasPreviewTag(MediaPackageElement element) {
    return element.getTags() != null
            && Arrays.asList(element.getTags()).contains(getPreviewTag());
  }

  private Boolean elementHasPreviewFlavor(MediaPackageElement element) {
    return element.getFlavor() != null
            && getPreviewSubtype().equals(element.getFlavor().getSubtype());
  }

  private String signIfNecessary(final URI uri) {
    if (!urlSigningService.accepts(uri.toString())) {
      return uri.toString();
    }
    String clientIP = signWithClientIP ? securityService.getUserIP() : null;
    try {
      return new URI(urlSigningService.sign(uri.toString(), expireSeconds, null, clientIP)).toString();
    } catch (URISyntaxException | UrlSigningException e) {
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
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
  Smil createSmilCuttingCatalog(final EditingData editingInfo, final MediaPackage mediaPackage) throws SmilException {
    // Create initial SMIL catalog
    SmilResponse smilResponse = smilService.createNewSmil(mediaPackage);

    // Add tracks to the SMIL catalog
    ArrayList<Track> tracks = new ArrayList<>();

    for (final TrackData trackdata : editingInfo.getTracks()) {
      String trackId = trackdata.getId();
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
        if (trackOpt.isNone()) {
          throw new IllegalStateException(
                  String.format("The track '%s' doesn't exist in media package '%s'", trackId, mediaPackage));
        }
        track = trackOpt.get();
      }
      tracks.add(track);
    }

    for (SegmentData segment : editingInfo.getSegments()) {
      smilResponse = smilService.addParallel(smilResponse.getSmil());
      final String parentId = smilResponse.getEntity().getId();

      final long duration = segment.getEnd() - segment.getStart();
      if (!segment.isDeleted()) {
        smilResponse = smilService.addClips(smilResponse.getSmil(), parentId, tracks.toArray(new Track[0]),
                segment.getStart(), duration);
      }
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
   * @throws IOException
   *           if the SMIL catalog cannot be read or not be written to the archive
   */
  void addSmilToArchive(MediaPackage mediaPackage, final Smil smil) throws IOException {
    MediaPackageElementFlavor mediaPackageElementFlavor = getSmilCatalogFlavor();
    //set default catalog Id if there is none existing
    String catalogId = smil.getId();
    Catalog[] catalogs = mediaPackage.getCatalogs();

    //get the first smil/cutting  catalog-ID to overwrite it with new smil info
    for (Catalog p: catalogs) {
      if (p.getFlavor().matches(mediaPackageElementFlavor)) {
        logger.debug("Set Identifier for Smil-Catalog to: {}", p.getIdentifier());
        catalogId = p.getIdentifier();
        break;
      }
    }
    Catalog catalog = mediaPackage.getCatalog(catalogId);

    URI smilURI;
    try (InputStream is = IOUtils.toInputStream(smil.toXML(), "UTF-8")) {
      smilURI = workspace.put(mediaPackage.getIdentifier().toString(), catalogId, EditorService.TARGET_FILE_NAME, is);
    } catch (SAXException | JAXBException e) {
      throw new IOException("Error while serializing the SMIL catalog to XML" ,e);
    }

    if (catalog == null) {
      MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog, getSmilCatalogFlavor());
      mediaPackage.add(catalog);
    }
    catalog.setURI(smilURI);
    catalog.setIdentifier(catalogId);
    catalog.setMimeType(MimeTypes.XML);
    for (String tag : getSmilCatalogTags()) {
      catalog.addTag(tag);
    }
    // setting the URI to a new source so the checksum will most like be invalid
    catalog.setChecksum(null);

    try {
      assetManager.takeSnapshot(mediaPackage);
    } catch (AssetManagerException e) {
      logger.error("Error while adding the updated media package ({}) to the archive", mediaPackage.getIdentifier(), e);
      throw new IOException(e);
    }

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
   * Get an {@link Event}
   *
   * @param mediaPackageId
   *          The mediapackage id that is also the event id.
   * @return The event if available or none if it is missing.
   */
  private Event getEvent(final String mediaPackageId) throws EditorServiceException {
    try {
      Opt<Event> optEvent = index.getEvent(mediaPackageId, searchIndex);
      if (optEvent.isNone()) {
        errorExit("Event not found", mediaPackageId,
                ErrorStatus.MEDIAPACKAGE_NOT_FOUND);
      } else {
        return optEvent.get();
      }
    } catch (SearchIndexException e) {
      errorExit("Error while reading event from search index:", mediaPackageId,
              ErrorStatus.MEDIAPACKAGE_NOT_FOUND, e);
    }
    return null;
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
      logger.warn("Error while retrieving list of workflow definitions:", e);
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
  private List<SegmentData> getSegments(final MediaPackage mediaPackage) {
    List<SegmentData> segments = new ArrayList<>();
    for (Catalog smilCatalog : mediaPackage.getCatalogs(getSmilCatalogFlavor())) {
      try {
        Smil smil = smilService.fromXml(workspace.get(smilCatalog.getURI())).getSmil();
        segments = mergeSegments(segments, getSegmentsFromSmil(smil));

      } catch (NotFoundException e) {
        logger.warn("File '{}' could not be loaded by workspace service:", smilCatalog.getURI(), e);
      } catch (IOException e) {
        logger.warn("Reading file '{}' from workspace service failed:", smilCatalog.getURI(), e);
      } catch (SmilException e) {
        logger.warn("Error while parsing SMIL catalog '{}':", smilCatalog.getURI(), e);
      }
    }

    if (!segments.isEmpty()) {
      return segments;
    }

    // Read from silence detection flavors
    for (Catalog smilCatalog : mediaPackage.getCatalogs(getSmilSilenceFlavor())) {
      try {
        Smil smil = smilService.fromXml(workspace.get(smilCatalog.getURI())).getSmil();
        segments = getSegmentsFromSmil(smil);
      } catch (NotFoundException e) {
        logger.warn("File '{}' could not be loaded by workspace service:", smilCatalog.getURI(), e);
      } catch (IOException e) {
        logger.warn("Reading file '{}' from workspace service failed:", smilCatalog.getURI(), e);
      } catch (SmilException e) {
        logger.warn("Error while parsing SMIL catalog '{}':", smilCatalog.getURI(), e);
      }
    }

    // Check for single segment to ignore
    if (segments.size() == 1) {
      SegmentData singleSegment = segments.get(0);
      if (singleSegment.getStart() == 0 && singleSegment.getEnd() >= mediaPackage.getDuration()) {
        segments.remove(0);
      }
    }

    return segments;
  }

  protected List<SegmentData> getDeletedSegments(MediaPackage mediaPackage, List<SegmentData> segments) {
    // add deletedElements
    long lastTime = 0;
    List<SegmentData> deletedElements = new ArrayList<>();
    for (int i = 0; i < segments.size(); i++) {
      SegmentData segmentData = segments.get(i);
      if (segmentData.getStart() != lastTime) {
        SegmentData deleted = new SegmentData(lastTime, segmentData.getStart(), true);
        deletedElements.add(deleted);
      }
      lastTime = segmentData.getEnd();
      // check for last segment
      if (segments.size() - 1 == i) {
        if (mediaPackage.getDuration() != null && lastTime < mediaPackage.getDuration()) {
          deletedElements.add(new SegmentData(lastTime, mediaPackage.getDuration(), true));
        }
      }
    }
    return deletedElements;
  }

  protected List<SegmentData> mergeSegments(List<SegmentData> segments, List<SegmentData> segments2) {
    // Merge conflicting segments
    List<SegmentData> mergedSegments = mergeInternal(segments, segments2);

    // Sort segments
    sortSegments(mergedSegments);

    return mergedSegments;
  }

  private void sortSegments(List<SegmentData> mergedSegments) {
    mergedSegments.sort(Comparator.comparing(SegmentData::getStart));
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
  private List<SegmentData> mergeInternal(List<SegmentData> segments, List<SegmentData> segments2) {
    for (Iterator<SegmentData> it = segments.iterator(); it.hasNext();) {
      SegmentData seg = it.next();
      for (Iterator<SegmentData> it2 = segments2.iterator(); it2.hasNext();) {
        SegmentData seg2 = it2.next();
        long combinedStart = Math.max(seg.getStart(), seg2.getStart());
        long combinedEnd = Math.min(seg.getEnd(), seg2.getEnd());
        if (combinedEnd > combinedStart) {
          it.remove();
          it2.remove();
          List<SegmentData> newSegments = new ArrayList<>(segments);
          newSegments.add(new SegmentData(combinedStart, combinedEnd));
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
  List<SegmentData> getSegmentsFromSmil(Smil smil) {
    List<SegmentData> segments = new ArrayList<>();
    for (SmilMediaObject elem : smil.getBody().getMediaElements()) {
      if (elem instanceof SmilMediaContainer) {
        SmilMediaContainer mediaContainer = (SmilMediaContainer) elem;

        SegmentData tuple = null;
        for (SmilMediaObject video : mediaContainer.getElements()) {
          if (video instanceof SmilMediaElement) {
            SmilMediaElement videoElem = (SmilMediaElement) video;
            try {
              // pick longest element
              if (tuple == null || (videoElem.getClipEndMS()
                      - videoElem.getClipBeginMS()) > tuple.getEnd() - tuple.getStart()) {
                tuple = new SegmentData(videoElem.getClipBeginMS(), videoElem.getClipEndMS());
              }
            } catch (SmilException e) {
              logger.warn("Media element '{}' of SMIL catalog '{}' seems to be invalid: {}",
                      videoElem, smil, e);
            }
          }
        }
        if (tuple != null) {
          segments.add(tuple);
        }
      }
    }
    return segments;
  }

  @Override
  public EditingData getEditData(final String mediaPackageId) throws EditorServiceException {
    // Select tracks
    Event event = getEvent(mediaPackageId);
    MediaPackage mp = getMediaPackage(event);

    final Opt<Publication> internalPubOpt = getInternalPublication(mp);
    if (internalPubOpt.isNone() || internalPubOpt.isEmpty()) {
      errorExit("No internal publication", mediaPackageId, ErrorStatus.NO_INTERNAL_PUBLICATION);
    }
    Publication internalPub = internalPubOpt.get();

    // Get existing segments
    List<SegmentData> segments = getSegments(mp);
    segments.addAll(getDeletedSegments(mp, segments));
    sortSegments(segments);


    // Get workflows
    List<WorkflowData> workflows = new ArrayList<>();
    for (WorkflowDefinition workflow : getEditingWorkflows()) {
      workflows.add(new WorkflowData(workflow.getId(), workflow.getTitle(), workflow.getDisplayOrder(),
              workflow.getDescription()));
    }

    final Map<String, String> latestWfProperties = WorkflowPropertiesUtil
            .getLatestWorkflowProperties(assetManager, mediaPackageId);
    // The properties have the format "hide_flavor_audio" or "hide_flavor_video", where flavor is preconfigured.
    // We filter all the properties that have this format, and then those which have values "true".
    final Collection<Tuple<String, String>> hiddens = latestWfProperties.entrySet()
            .stream()
            .map(property -> tuple(property.getKey().split("_"), property.getValue()))
            .filter(property -> property.getA().length == 3)
            .filter(property -> property.getA()[0].equals("hide"))
            .filter(property -> property.getB().equals("true"))
            .map(property -> tuple(property.getA()[1], property.getA()[2]))
            .collect(Collectors.toSet());

    List<Track> trackList = Arrays.stream(internalPub.getTracks()).filter(this::elementHasPreviewTag)
            .collect(Collectors.toList());
    if (trackList.isEmpty()) {
      trackList = Arrays.stream(internalPub.getTracks()).filter(this::elementHasPreviewFlavor)
              .collect(Collectors.toList());
      if (trackList.isEmpty()) {
        trackList = Arrays.asList(internalPub.getTracks());
      }
    }

    final List<TrackData> tracks = trackList.stream().map(track -> {
      final String uri = signIfNecessary(track.getURI());
      final boolean audioEnabled = !hiddens.contains(tuple(track.getFlavor().getType(), "audio"));
      final TrackSubData audio = new TrackSubData(track.hasAudio(), null,
                        audioEnabled);
      final boolean videoEnable = !hiddens.contains(tuple(track.getFlavor().getType(), "video"));
      final String videoPreview = Arrays.stream(internalPub.getAttachments())
                        .filter(attachment -> attachment.getFlavor().getType().equals(track.getFlavor().getType()))
                        .filter(attachment -> attachment.getFlavor().getSubtype().equals(getPreviewVideoSubtype()))
                        .map(MediaPackageElement::getURI).map(this::signIfNecessary)
                        .findAny()
                        .orElse(null);
      final TrackSubData video = new TrackSubData(track.hasVideo(), videoPreview,
                        videoEnable);

      return new TrackData(track.getFlavor().getType(), track.getFlavor().getSubtype(), audio, video, uri,
                        track.getIdentifier());
    }).collect(Collectors.toList());

    return new EditingData(segments, tracks, workflows, mp.getDuration(), mp.getTitle(), event.getRecordingStartDate(),
            event.getSeriesId(), event.getSeriesName());
  }

  private MediaPackage getMediaPackage(Event event) throws EditorServiceException {
    if (event == null) {
      errorExit("No Event provided", "", ErrorStatus.UNKNOWN);
      return null;
    }
    try {
      return index.getEventMediapackage(event);
    } catch (IndexServiceException e) {
      errorExit("Not Found", event.getIdentifier(), ErrorStatus.MEDIAPACKAGE_NOT_FOUND);
      return null;
    }
  }

  private void errorExit(final String message, final String mediaPackageId, ErrorStatus status)
          throws EditorServiceException {
    errorExit(message, mediaPackageId, status, null);
  }

  private void errorExit(final String message, final String mediaPackageId, ErrorStatus status, Exception e)
          throws EditorServiceException {
    String errorMessage = MessageFormat.format("{0}. Event ID: {1}", message, mediaPackageId);
    throw new EditorServiceException(errorMessage, status, e);
  }

  @Override
  public void setEditData(String mediaPackageId, EditingData editingData) throws EditorServiceException {
    final Event event = getEvent(mediaPackageId);

    if (WorkflowUtil.isActive(event.getWorkflowState())) {
      errorExit("Workflow is running", mediaPackageId, ErrorStatus.WORKFLOW_ACTIVE);
    }

    MediaPackage mediaPackage = getMediaPackage(event);
    Smil smil = null;
    try {
      smil = createSmilCuttingCatalog(editingData, mediaPackage);
    } catch (Exception e) {
      errorExit("Unable to create SMIL cutting catalog", mediaPackageId, ErrorStatus.UNABLE_TO_CREATE_CATALOG, e);
    }

    final Map<String, String> workflowProperties = new HashMap<String, String>();
    for (TrackData track : editingData.getTracks()) {
      MediaPackageElementFlavor flavor = track.getFlavor();
      String type = null;
      if (flavor != null) {
        type = flavor.getType();
      } else {
        Track mpTrack = mediaPackage.getTrack(track.getId());
        if (mpTrack != null) {
          type = mpTrack.getFlavor().getType();
        } else {
          errorExit("Unable to determine track type", mediaPackageId, ErrorStatus.UNKNOWN);
        }
      }
      workflowProperties.put("hide_" + type + "_audio", Boolean.toString(!track.getAudio().isEnabled()));
      workflowProperties.put("hide_" + type + "_video", Boolean.toString(!track.getVideo().isEnabled()));
    }
    WorkflowPropertiesUtil.storeProperties(assetManager, mediaPackage, workflowProperties);

    try {
      addSmilToArchive(mediaPackage, smil);
    } catch (IOException e) {
      errorExit("Unable to add SMIL cutting catalog to archive", mediaPackageId, ErrorStatus.UNKNOWN, e);
    }

    if (editingData.getPostProcessingWorkflow() != null) {
      final String workflowId = editingData.getPostProcessingWorkflow();
      try {
        final Map<String, String> workflowParameters = WorkflowPropertiesUtil
                .getLatestWorkflowProperties(assetManager, mediaPackage.getIdentifier().toString());
        final Workflows workflows = new Workflows(assetManager, workflowService);
        workflows.applyWorkflowToLatestVersion($(mediaPackage.getIdentifier().toString()),
                ConfiguredWorkflow.workflow(workflowService.getWorkflowDefinitionById(workflowId), workflowParameters))
                .run();
      } catch (AssetManagerException e) {
        errorExit("Unable to start workflow" + workflowId, mediaPackageId, ErrorStatus.WORKFLOW_ERROR, e);
      } catch (WorkflowDatabaseException e) {
        errorExit("Unable to load workflow" + workflowId, mediaPackageId, ErrorStatus.WORKFLOW_ERROR, e);
      } catch (NotFoundException e) {
        errorExit("Unable to load workflow" + workflowId, mediaPackageId, ErrorStatus.WORKFLOW_NOT_FOUND, e);
      }
    }
  }

  @Override
  public String getMetadata(String mediaPackageId) throws EditorServiceException {
    final Event event = getEvent(mediaPackageId);
    MediaPackage mediaPackage = getMediaPackage(event);
    MetadataList metadataList = new MetadataList();
    List<EventCatalogUIAdapter> catalogUIAdapters = index.getEventCatalogUIAdapters();
    catalogUIAdapters.remove(index.getCommonEventCatalogUIAdapter());
    for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage));
    }

    DublinCoreMetadataCollection metadataCollection = null;
    try {
      metadataCollection = EventUtils.getEventMetadata(event,
              index.getCommonEventCatalogUIAdapter());
    } catch (Exception e) {
      errorExit("Unable to retrieve event metadata", mediaPackageId, ErrorStatus.UNKNOWN);
    }
    metadataList.add(index.getCommonEventCatalogUIAdapter(), metadataCollection);

    final String wfState = event.getWorkflowState();
    if (wfState != null && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(wfState))) {
      metadataList.setLocked(MetadataList.Locked.WORKFLOW_RUNNING);
    }

    return MetadataJson.listToJson(metadataList, true).toString();
  }

  @Override
  public void setMetadata(String mediaPackageId, String metadata) throws EditorServiceException {
    try {
      index.updateAllEventMetadata(mediaPackageId, metadata, searchIndex);
    } catch (SearchIndexException | IndexServiceException | IllegalArgumentException e) {
      errorExit("Event metadata can't be updated.", mediaPackageId, ErrorStatus.METADATA_UPDATE_FAIL, e);
    } catch (NotFoundException e) {
      errorExit("Event not found.", mediaPackageId, ErrorStatus.MEDIAPACKAGE_NOT_FOUND, e);
    } catch (UnauthorizedException e) {
      errorExit("Not authorized to update event metadata .", mediaPackageId, ErrorStatus.NOT_AUTHORIZED, e);
    }
  }

}
