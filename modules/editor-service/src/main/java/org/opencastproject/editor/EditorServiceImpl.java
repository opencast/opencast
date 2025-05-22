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

package org.opencastproject.editor;

import static java.util.Collections.emptyList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.editor.api.EditingData;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.editor.api.ErrorStatus;
import org.opencastproject.editor.api.LockData;
import org.opencastproject.editor.api.SegmentData;
import org.opencastproject.editor.api.TrackData;
import org.opencastproject.editor.api.TrackSubData;
import org.opencastproject.editor.api.WorkflowData;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.util.EventUtils;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
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
import org.opencastproject.util.MimeType;
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

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;


@Component(
    property = {
        "service.description=Editor Service"
    },
    immediate = true,
    service = EditorService.class
)
public class EditorServiceImpl implements EditorService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(EditorServiceImpl.class);

  /** Tag that marks workflow for being used from the editor tool */
  private static final String EDITOR_WORKFLOW_TAG = "editor";

  private static EditorLock editorLock;

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
  private AuthorizationService authorizationService;


  private MediaPackageElementFlavor smilCatalogFlavor;
  private String previewVideoSubtype;
  private String previewTag;
  private String previewSubtype;
  private String waveformSubtype;
  private String thumbnailSubType;
  private MediaPackageElementFlavor smilSilenceFlavor;
  private ElasticsearchIndex searchIndex;
  private MediaPackageElementFlavor captionsFlavor;
  private String thumbnailWfProperty;
  private List<MediaPackageElementFlavor> thumbnailSourcePrimary;
  private String distributionDirectory;
  private Boolean localPublication = null;

  private static final String DEFAULT_PREVIEW_SUBTYPE = "source";
  private static final String DEFAULT_PREVIEW_TAG = "editor";
  private static final String DEFAULT_WAVEFORM_SUBTYPE = "waveform";
  private static final String DEFAULT_SMIL_CATALOG_FLAVOR = "smil/cutting";
  private static final String DEFAULT_SMIL_CATALOG_TAGS = "archive";
  private static final String DEFAULT_SMIL_SILENCE_FLAVOR = "*/silence";
  private static final String DEFAULT_PREVIEW_VIDEO_SUBTYPE = "video+preview";
  private static final String DEFAULT_CAPTIONS_FLAVOR = "captions/*";
  private static final String DEFAULT_THUMBNAIL_SUBTYPE = "player+preview";
  private static final String DEFAULT_THUMBNAIL_WF_PROPERTY = "thumbnail_edited";
  private static final List<MediaPackageElementFlavor> DEFAULT_THUMBNAIL_PRIORITY_FLAVOR = new ArrayList<>();
  private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 300; // ( 5 mins )
  private static final int DEFAULT_LOCK_REFRESH_SECONDS = 60;  // ( 1 min )

  public static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  public static final String OPT_PREVIEW_TAG = "preview.tag";
  public static final String OPT_WAVEFORM_SUBTYPE = "waveform.subtype";
  public static final String OPT_SMIL_CATALOG_FLAVOR = "smil.catalog.flavor";
  public static final String OPT_SMIL_CATALOG_TAGS = "smil.catalog.tags";
  public static final String OPT_SMIL_SILENCE_FLAVOR = "smil.silence.flavor";
  public static final String OPT_PREVIEW_VIDEO_SUBTYPE = "preview.video.subtype";
  public static final String OPT_CAPTIONS_FLAVOR = "captions.flavor";
  public static final String OPT_THUMBNAILSUBTYPE = "thumbnail.subtype";
  public static final String OPT_THUMBNAIL_WF_PROPERTY = "thumbnail.workflow.property";
  public static final String OPT_THUMBNAIL_PRIORITY_FLAVOR = "thumbnail.priority.flavor";
  public static final String OPT_LOCAL_PUBLICATION = "publication.local";
  public static final String OPT_LOCK_ENABLED = "lock.enable";
  public static final String OPT_LOCK_TIMEOUT = "lock.release.after.seconds";
  public static final String OPT_LOCK_REFRESH = "lock.refresh.after.seconds";

  private Boolean lockingActive;
  private int lockRefresh = DEFAULT_LOCK_REFRESH_SECONDS;
  private int lockTimeout = DEFAULT_LOCK_TIMEOUT_SECONDS;

  private final Set<String> smilCatalogTagSet = new HashSet<>();

  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  @Reference
  void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Reference
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  @Reference
  void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.searchIndex = elasticsearchIndex;
  }

  @Reference
  public void setIndexService(IndexService index) {
    this.index = index;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
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

  private String getWaveformSubtype() {
    return waveformSubtype;
  }

  private String getThumbnailSubtype() {
    return thumbnailSubType;
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

    // Waveform subtype
    waveformSubtype = Objects.toString(properties.get(OPT_WAVEFORM_SUBTYPE), DEFAULT_WAVEFORM_SUBTYPE);
    logger.debug("Waveform subtype configuration set to '{}'", waveformSubtype);

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

    // Flavor for captions
    captionsFlavor = MediaPackageElementFlavor.parseFlavor(
            StringUtils.defaultString((String) properties.get(OPT_CAPTIONS_FLAVOR), DEFAULT_CAPTIONS_FLAVOR));
    logger.debug("Caption flavor set to '{}'", captionsFlavor);

    thumbnailSubType =  Objects.toString(properties.get(OPT_THUMBNAILSUBTYPE), DEFAULT_THUMBNAIL_SUBTYPE);
    logger.debug("Thumbnail subtype set to '{}'", thumbnailSubType);

    thumbnailWfProperty = Objects.toString(properties.get(OPT_THUMBNAIL_WF_PROPERTY), DEFAULT_THUMBNAIL_WF_PROPERTY);
    logger.debug("Thumbnail workflow property set to '{}'", thumbnailWfProperty);

    String thumbnailPriorities = Objects.toString(properties.get(OPT_THUMBNAIL_PRIORITY_FLAVOR));
    if ("null".equals(thumbnailPriorities)  || thumbnailPriorities.isEmpty()) {
      thumbnailSourcePrimary = DEFAULT_THUMBNAIL_PRIORITY_FLAVOR;
    } else {
      thumbnailSourcePrimary = Arrays.stream(thumbnailPriorities.split(",", -1))
                                .map(MediaPackageElementFlavor::parseFlavor)
                                .collect(Collectors.toList());
    }

    String localPublicationConfig = Objects.toString(properties.get(OPT_LOCAL_PUBLICATION), "auto");
    if (!"auto".equals(localPublicationConfig)) {
      // If this is not set to `auto`, we expect this to be a boolean
      localPublication = BooleanUtils.toBoolean(localPublicationConfig);
    }

    distributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    if (StringUtils.isEmpty(distributionDirectory)) {
      final String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (StringUtils.isNotEmpty(storageDir)) {
        distributionDirectory = new File(storageDir, "downloads").getPath();
      }
    }
    logger.debug("Thumbnail track priority set to '{}'", thumbnailSourcePrimary);

    lockingActive = Boolean.parseBoolean(StringUtils.trimToEmpty((String) properties.get(OPT_LOCK_ENABLED)));

    try {
      lockTimeout = Integer.parseUnsignedInt(
           Objects.toString(properties.get(OPT_LOCK_TIMEOUT)));
    } catch (NumberFormatException e) {
      logger.info("Configuration {} contains invalid value, defaulting to {}", OPT_LOCK_TIMEOUT, lockTimeout);
    }

    try {
      lockRefresh = Integer.parseUnsignedInt(
            Objects.toString(properties.get(OPT_LOCK_REFRESH)));
    } catch (NumberFormatException e) {
      logger.info("Configuration {} contains invalid value, defaulting to {}", OPT_LOCK_REFRESH, lockRefresh);
    }

    editorLock = new EditorLock(lockTimeout);

  }

  /**
   * Check if a media URL can be served from this server.
   *
   * @param uri
   *      URL locating a media file
   * @return
   *      If the file is available locally
   */
  private boolean isLocal(URI uri) {
    var path = uri.normalize().getPath();
    if (!path.startsWith("/static/")) {
      return false;
    }
    var localFile = new File(distributionDirectory, path.substring("/static".length()));
    return localFile.exists();
  }

  private Boolean elementHasPreviewTag(MediaPackageElement element) {
    return element.getTags() != null
            && Arrays.asList(element.getTags()).contains(getPreviewTag());
  }

  private Boolean elementHasPreviewFlavor(MediaPackageElement element) {
    return element.getFlavor() != null
            && getPreviewSubtype().equals(element.getFlavor().getSubtype());
  }

  private Boolean elementHasWaveformFlavor(MediaPackageElement element) {
    return element.getFlavor() != null
            && getWaveformSubtype().equals(element.getFlavor().getSubtype());
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
        track = Arrays.stream(getInternalPublication(mediaPackage)
            .orElseThrow(() -> new IllegalStateException("Event has no internal publication"))
            .getTracks())
            .filter(t -> trackId.equals(t.getIdentifier()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                  String.format("The track '%s' doesn't exist in media package '%s'", trackId, mediaPackage)));
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
   * Adds the SMIL file as {@link Catalog} to the media package
   * Does not send the updated media package to the archive.
   *
   * @param mediaPackage
   *          the media package to at the SMIL catalog
   * @param smil
   *          the SMIL catalog
   * @throws IOException
   *           if the SMIL catalog cannot be read or not be written to the archive
   */
  MediaPackage addSmilToArchive(MediaPackage mediaPackage, final Smil smil) throws IOException {
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
    return mediaPackage;
  }

  /**
   * Adds subtitles {@link EditingData.Subtitle} to the media package and sends the updated media package
   * to the archive. If a subtitle flavor already exists, the subtitle is overwritten
   *
   * @param mediaPackage
   *          the media package to at the SMIL catalog
   * @param subtitles
   *          the subtitles to be added
   * @throws IOException
   */
  private MediaPackage processSubtitleTrack(MediaPackage mediaPackage, List<EditingData.Subtitle> subtitles)
          throws IOException, IllegalArgumentException {
    for (EditingData.Subtitle subtitle : subtitles) {
      // Generate ID for new tracks
      String subtitleId = UUID.randomUUID().toString();
      String trackId = null;

      // Check if subtitle already exists
      for (Track t : mediaPackage.getTracks()) {
        if (t.getIdentifier().matches(subtitle.getId())) {
          logger.debug("Set Identifier for Subtitle-Track to: {}", t.getIdentifier());
          subtitleId = t.getIdentifier();
          trackId = t.getIdentifier();
          break;
        }
      }

      Track track = mediaPackage.getTrack(trackId);

      if (subtitle.isDeleted()) {
        // If the subtitle is empty, remove the track
        if (trackId != null) {
          mediaPackage.remove(track);
        }
        continue;
      }

      // Memorize uri of the previous track file for deletion
      URI oldTrackURI = null;
      if (track != null) {
        oldTrackURI = track.getURI();
      }

      // Put updated filename in working file repository and update the track.
      try (InputStream is = IOUtils.toInputStream(subtitle.getSubtitle(), "UTF-8")) {
        URI subtitleUri = workspace.put(mediaPackage.getIdentifier().toString(), subtitleId, "subtitle.vtt", is);

        // If not exists, create new Track
        if (track == null) {
          MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
          // TODO: Figure out which flavor new subtitles from the editor should have
          track = (Track) mpeBuilder.elementFromURI(subtitleUri, MediaPackageElement.Type.Track,
                  new MediaPackageElementFlavor(captionsFlavor.getType(),"source"));
          mediaPackage.add(track);
          logger.info("Creating new subtitle track " + track.getIdentifier() + " with tags "
                  + track.getTags().toString());
        }

        track.setURI(subtitleUri);
        track.setIdentifier(subtitleId);
        track.setChecksum(null);
        for (String tag : subtitle.getTags()) {
          track.addTag(tag);
        }

        if (oldTrackURI != null && oldTrackURI != subtitleUri) {
          // Delete the old files from the working file repository and workspace if they were in there
          logger.info("Removing old track file {}", oldTrackURI);
          try {
            workspace.delete(oldTrackURI);
          } catch (NotFoundException | IOException e) {
            logger.info("Could not remove track from workspace. Could be it was never there.");
          }
        }
      }
    }

    return mediaPackage;
  }

  /**
   * Adds base64 encoded thumbnail images to the mediapackage and takes a snapshot
   *
   * @param editingData
   *          the editing information
   * @param mediaPackage
   *          the media package
   * @throws MimeTypeParseException
   * @throws IOException
   */
  private MediaPackage addThumbnailsToArchive(EditingData editingData, MediaPackage mediaPackage)
          throws MimeTypeParseException, IOException {
    for (TrackData track : editingData.getTracks()) {
      String id = track.getId();
      MediaPackageElementFlavor flavor = new MediaPackageElementFlavor(track.getFlavor().getType(),
              getThumbnailSubtype());
      String uri = track.getThumbnailURI();

      // If no uri, what do?
      if (uri == null || uri.isEmpty()) {
        continue;
      }
      // If uri not base64 encoded, what do?
      if (!uri.startsWith("data")) {
        continue;
      }

      // Decode
      uri = uri.substring(uri.indexOf(",") + 1);
      byte[] byteArray = Base64.getMimeDecoder().decode(uri);
      InputStream inputStream = new ByteArrayInputStream(byteArray);

      // Get MimeType
      String stringMimeType = detectMimeType(uri);
      MimeType mimeType = MimeType.mimeType(stringMimeType.split("/")[0], stringMimeType.split("/")[1]);

      // Store image in workspace
      final String filename = "thumbnail_" + id + "." + mimeType.getSubtype();
      final String originalThumbnailId = UUID.randomUUID().toString();
      URI tempThumbnail = null;
      try {
        tempThumbnail = workspace
                .put(mediaPackage.getIdentifier().toString(), originalThumbnailId, filename, inputStream);
      } catch (IOException e) {
        throw new IOException("Could not add thumbnail to workspace", e);
      }

      // Build thumbnail attachment
      final Attachment attachment = AttachmentImpl.fromURI(tempThumbnail);
      attachment.setFlavor(flavor);
      attachment.setMimeType(mimeType);
      Arrays.stream(mediaPackage.getElementsByFlavor(flavor))
          .map(MediaPackageElement::getTags)
          .flatMap(Arrays::stream)
          .distinct()
          .forEach(attachment::addTag);

      // Remove old thumbnails
      Arrays.stream(mediaPackage.getElementsByFlavor(flavor)).forEach(mediaPackage::remove);

      // Add new thumbnail
      mediaPackage.add(attachment);

      // Update publications here in the future?

      // Set workflow property
      WorkflowPropertiesUtil
              .storeProperty(assetManager, mediaPackage,
                      flavor.getType() + "/" + thumbnailWfProperty, "true");
    }

    return mediaPackage;
  }

  /**
   * Determines if mimetype of a base64 encoded string is one of the listed image mimetypes and returns it.
   *
   * @param b64
   *          the encoded string that is supposed to be an image
   * @return
   *          the mimetype
   * @throws MimeTypeParseException
   */
  private String detectMimeType(String b64) throws MimeTypeParseException {
    var signatures = new HashMap<String, String>();
    signatures.put("R0lGODdh", "image/gif");
    signatures.put("iVBORw0KGgo", "image/png");
    signatures.put("/9j/", "image/jpg");

    for (var s : signatures.entrySet()) {
      if (b64.indexOf(s.getKey()) == 0) {
        return s.getValue();
      }
    }
    throw new MimeTypeParseException("No image mimetype found");
  }

  private Optional<Publication> getInternalPublication(MediaPackage mp) {
    return Arrays.stream(mp.getPublications())
        .filter(publication -> InternalPublicationChannel.CHANNEL_ID.equals(publication.getChannel()))
        .findFirst();
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
    try {
      return workflowService.listAvailableWorkflowDefinitions().stream()
          .filter(workflow -> workflow.containsTag(EDITOR_WORKFLOW_TAG))
          .collect(Collectors.toList());
    } catch (WorkflowDatabaseException e) {
      logger.warn("Error while retrieving list of workflow definitions:", e);
    }
    return emptyList();
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
              logger.warn("Media element '{}' of SMIL catalog '{}' seems to be invalid",
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
  public void lockMediaPackage(final String mediaPackageId, LockData lockRequest) throws EditorServiceException {
    // Does mediaPackage exist
    getEvent(mediaPackageId);

    // Try to get lock, throws Exception if not owner
    editorLock.lock(mediaPackageId, lockRequest);
  }

  @Override
  public void unlockMediaPackage(final String mediaPackageId, LockData lockRequest) throws EditorServiceException {
    // Does mediaPackage exist
    getEvent(mediaPackageId);

    // Try to release lock, throws Exception if not owner
    editorLock.unlock(mediaPackageId, lockRequest);
  }

  @Override
  public EditingData getEditData(final String mediaPackageId) throws EditorServiceException, UnauthorizedException {

    Event event = getEvent(mediaPackageId);
    MediaPackage mp = getMediaPackage(event);

    if (!isAdmin() && !authorizationService.hasPermission(mp, "write")) {
      throw new UnauthorizedException("User has no write access to this event");
    }

    boolean workflowActive = WorkflowUtil.isActive(event.getWorkflowState());

    final Optional<Publication> internalPubOpt = getInternalPublication(mp);
    if (internalPubOpt.isEmpty()) {
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

    // Get subtitles from the asset manager, so they are guaranteed to be up-to-date after saving
    Track[] subtitleTracks = mp.getTracks(captionsFlavor);
    List<EditingData.Subtitle> subtitles = new ArrayList<>();
    for (Track t: subtitleTracks) {
      try {
        File subtitleFile = workspace.get(t.getURI());
        String subtitleString = FileUtils.readFileToString(subtitleFile, StandardCharsets.UTF_8);
        subtitles.add(new EditingData.Subtitle(t.getIdentifier(), subtitleString, t.getTags()));
      } catch (NotFoundException | IOException e) {
        errorExit("Could not read subtitle from file", mediaPackageId, ErrorStatus.UNKNOWN);
      }
    }

    // Get tracks from the internal publication because it is a lot faster than getting them from the asset manager
    // for some reason.
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

      // Get thumbnail from archive
      // If a thumbnail got generated in the frontend, it will be saved to the archive. So if no workflow runs,
      // the saved, thumbnail will not show up in the frontend if we get it from the internal publication
      String thumbnailURI = Arrays.stream(mp.getAttachments())
          .filter(attachment -> attachment.getFlavor().getType().equals(track.getFlavor().getType()))
          .filter(attachment -> attachment.getFlavor().getSubtype().equals(getThumbnailSubtype()))
          .map(MediaPackageElement::getURI).map(this::signIfNecessary)
          .findAny()
          .orElse(null);

      // If thumbnail is not in archive, try getting it from the internal publication
      // Because our default workflows don't save thumbnails in the archive but only publish them.
      if (thumbnailURI == null) {
        thumbnailURI = Arrays.stream(internalPub.getAttachments())
            .filter(attachment -> attachment.getFlavor().getType().equals(track.getFlavor().getType()))
            .filter(attachment -> attachment.getFlavor().getSubtype().equals(getThumbnailSubtype()))
            .map(MediaPackageElement::getURI).map(this::signIfNecessary)
            .findAny()
            .orElse(null);
      }

      final int priority = thumbnailSourcePrimary.indexOf(track.getFlavor());

      if (localPublication == null) {
        localPublication = isLocal(track.getURI());
      }

      return new TrackData(track.getFlavor().getType(), track.getFlavor().getSubtype(), audio, video, uri,
          track.getIdentifier(), thumbnailURI, priority);
    }).collect(Collectors.toList());

    List<String> waveformList = Arrays.stream(internalPub.getAttachments())
            .filter(this::elementHasWaveformFlavor)
            .map(Attachment::getURI).map(this::signIfNecessary)
            .collect(Collectors.toList());

    User user = securityService.getUser();

    return new EditingData(segments, tracks, workflows, mp.getDuration(), mp.getTitle(), event.getRecordingStartDate(),
            event.getSeriesId(), event.getSeriesName(), workflowActive, waveformList, subtitles, localPublication,
            lockingActive, lockRefresh, user, "");
  }


  private boolean isAdmin() {
    final User currentUser = securityService.getUser();

    // Global admin
    if (currentUser.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE)) {
      return true;
    }

    // Organization admin
    final Organization currentOrg = securityService.getOrganization();
    return currentUser.getOrganization().getId().equals(currentOrg.getId())
            && currentUser.hasRole(currentOrg.getAdminRole());
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
  public void setEditData(String mediaPackageId, EditingData editingData) throws EditorServiceException,
          IOException {
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
      mediaPackage = addSmilToArchive(mediaPackage, smil);
    } catch (IOException e) {
      errorExit("Unable to add SMIL cutting catalog to archive", mediaPackageId, ErrorStatus.UNKNOWN, e);
    }

    try {
      if (editingData.getSubtitles() != null) {
        mediaPackage = processSubtitleTrack(mediaPackage, editingData.getSubtitles());
      }
    } catch (IOException e) {
      errorExit("Unable to add subtitle track to archive", mediaPackageId, ErrorStatus.UNKNOWN, e);
    } catch (IllegalArgumentException e) {
      errorExit("Illegal subtitle given", mediaPackageId, ErrorStatus.UNKNOWN, e);
    }

    try {
      mediaPackage = addThumbnailsToArchive(editingData, mediaPackage);
    } catch (MimeTypeParseException e) {
      errorExit("Thumbnail had an illegal MimeType", mediaPackageId, ErrorStatus.UNKNOWN, e);
    } catch (IOException e) {
      errorExit("Unable to add thumbnail to archive", mediaPackageId, ErrorStatus.UNKNOWN, e);
    }

    try {
      assetManager.takeSnapshot(mediaPackage);
    } catch (AssetManagerException e) {
      logger.error("Error while adding the updated media package ({}) to the archive",
              mediaPackage.getIdentifier(), e);
      throw new IOException(e);
    }

    // Update Metadata
    try {
      index.updateAllEventMetadata(mediaPackageId, editingData.getMetadataJSON(), searchIndex);
    } catch (SearchIndexException | IndexServiceException | IllegalArgumentException e) {
      errorExit("Event metadata can't be updated.", mediaPackageId, ErrorStatus.METADATA_UPDATE_FAIL, e);
    } catch (NotFoundException e) {
      errorExit("Event not found.", mediaPackageId, ErrorStatus.MEDIAPACKAGE_NOT_FOUND, e);
    } catch (UnauthorizedException e) {
      errorExit("Not authorized to update event metadata .", mediaPackageId, ErrorStatus.NOT_AUTHORIZED, e);
    }

    if (editingData.getPostProcessingWorkflow() != null) {
      final String workflowId = editingData.getPostProcessingWorkflow();
      try {
        final Map<String, String> workflowParameters = WorkflowPropertiesUtil
                .getLatestWorkflowProperties(assetManager, mediaPackage.getIdentifier().toString());
        final Workflows workflows = new Workflows(assetManager, workflowService);
        workflows.applyWorkflowToLatestVersion(Collections.singletonList(mediaPackage.getIdentifier().toString()),
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
}
