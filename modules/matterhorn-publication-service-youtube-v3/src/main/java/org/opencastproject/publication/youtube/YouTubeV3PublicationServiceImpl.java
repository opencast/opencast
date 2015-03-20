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
package org.opencastproject.publication.youtube;

import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.publication.youtube.auth.ClientCredentials;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.XProperties;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

/**
 * Publishes media to a Youtube play list.
 */
public class YouTubeV3PublicationServiceImpl extends AbstractJobProducer implements YouTubePublicationService, ManagedService {

  /** Time to wait between polling for status (milliseconds.) */
  private static final long POLL_MILLISECONDS = 30L * 1000L;

  /** The channel name */
  private static final String CHANNEL_NAME = "youtube";

  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YouTubeV3PublicationServiceImpl.class);

  /** The mime-type of the published element */
  private static final String MIME_TYPE = "text/html";

  /** List of available operations on jobs */
  private enum Operation {
    Publish, Retract
  }

  /** workspace instance */
  protected Workspace workspace = null;

  /** The remote service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The security service */
  private SecurityService securityService;

  /** Youtube configuration instance */
  private final YouTubeAPIVersion3Service youTubeService;

  /**
   * The default playlist to publish to, in case there is not enough information in the mediapackage to find a playlist
   */
  private String defaultPlaylist;

  private boolean makeVideosPrivate;

  private String[] tags;

  private XProperties properties = new XProperties();

  /**
   * The maximum length of a Recording or Series title.
   * A value of zero will be treated as no limit
   */
  private int maxFieldLength;

  /**
   * Creates a new instance of the youtube publication service.
   */
  YouTubeV3PublicationServiceImpl(final YouTubeAPIVersion3Service youTubeService) throws Exception {
    super(JOB_TYPE);
    this.youTubeService = youTubeService;
  }

  /**
   * Creates a new instance of the youtube publication service.
   */
  public YouTubeV3PublicationServiceImpl() throws Exception {
    this(new YouTubeAPIVersion3ServiceImpl());
  }

  /**
   * Called when service activates. Defined in OSGi resource file.
   */
  public synchronized void activate(final ComponentContext cc) {
    properties.setBundleContext(cc.getBundleContext());
  }

  @Override
  public void updated(final Dictionary props) throws ConfigurationException {
    properties.merge(props);

    final String dataStore = YouTubeUtils.get(properties, YouTubeKey.credentialDatastore);

    try {
      final ClientCredentials clientCredentials = new ClientCredentials();
      clientCredentials.setCredentialDatastore(dataStore);
      final String path = YouTubeUtils.get(properties, YouTubeKey.clientSecretsV3);
      File secretsFile = new File(path);
      if (secretsFile.exists() && !secretsFile.isDirectory()) {
        clientCredentials.setClientSecrets(secretsFile);
        clientCredentials.setDataStoreDirectory(YouTubeUtils.get(properties, YouTubeKey.dataStore));
        //
        youTubeService.initialize(clientCredentials);
        //
        tags = StringUtils.split(YouTubeUtils.get(properties, YouTubeKey.keywords), ',');
        defaultPlaylist = YouTubeUtils.get(properties, YouTubeKey.defaultPlaylist);
        makeVideosPrivate = StringUtils.containsIgnoreCase(YouTubeUtils.get(properties, YouTubeKey.makeVideosPrivate), "true");
        defaultMaxFieldLength(YouTubeUtils.get(properties, YouTubeKey.maxFieldLength, false));
      } else {
        logger.error("Client information file does not exist: " + path);
      }
    } catch (final Exception e) {
      throw new ConfigurationException("Failed to load YouTube v3 properties", dataStore, e);
    }
  }

  @Override
  public Job publish(final MediaPackage mediaPackage, final Track track) throws PublicationException {
    if (mediaPackage.contains(track)) {
      try {
        final List<String> args = Arrays.asList(MediaPackageParser.getAsXml(mediaPackage), track.getIdentifier());
        return serviceRegistry.createJob(JOB_TYPE, Operation.Publish.toString(), args);
      } catch (ServiceRegistryException e) {
        throw new PublicationException("Unable to create a job for track: " + track.toString(), e);
      }
    } else {
      throw new IllegalArgumentException("Mediapackage does not contain track " + track.getIdentifier());
    }

  }

  /**
   * Publishes the element to the publication channel and returns a reference to the published version of the element.
   *
   * @param job
   *          the associated job
   * @param mediaPackage
   *          the mediapackage
   * @param elementId
   *          the mediapackage element id to publish
   * @return the published element
   * @throws PublicationException
   *           if publication fails
   */
  private Publication publish(final Job job, final MediaPackage mediaPackage, final String elementId) throws PublicationException {
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Mediapackage must be specified");
    } else if (elementId == null) {
      throw new IllegalArgumentException("Mediapackage ID must be specified");
    }
    final MediaPackageElement element = mediaPackage.getElementById(elementId);
    if (element == null) {
      throw new IllegalArgumentException("Mediapackage element must be specified");
    }
    if (element.getIdentifier() == null) {
      throw new IllegalArgumentException("Mediapackage element must have an identifier");
    }
    if (element.getMimeType().toString().matches("text/xml")) {
      throw new IllegalArgumentException("Mediapackage element cannot be XML");
    }
    try {
      // create context strategy for publication
      final YouTubePublicationAdapter c = new YouTubePublicationAdapter(mediaPackage, workspace);
      final File file = workspace.get(element.getURI());
      final String episodeName = c.getEpisodeName();
      final UploadProgressListener operationProgressListener = new UploadProgressListener(mediaPackage, file);
      final String privacyStatus = makeVideosPrivate ? "private" : "public";
      final VideoUpload videoUpload = new VideoUpload(truncateTitleToMaxFieldLength(episodeName, false), c.getEpisodeDescription(), privacyStatus, file, operationProgressListener, tags);
      final Video video = youTubeService.addVideoToMyChannel(videoUpload);
      final int timeoutMinutes = 60;
      final long startUploadMilliseconds = new Date().getTime();
      while (!operationProgressListener.isComplete()) {
        Thread.sleep(POLL_MILLISECONDS);
        final long howLongWaitingMinutes = (new Date().getTime() - startUploadMilliseconds) / 60000;
        if (howLongWaitingMinutes > timeoutMinutes) {
          throw new PublicationException("Upload to YouTube exceeded " + timeoutMinutes + " minutes for episode " + episodeName);
        }
      }
      String playlistName = StringUtils.trimToNull(truncateTitleToMaxFieldLength(mediaPackage.getSeriesTitle(), true));
      playlistName = (playlistName == null) ? this.defaultPlaylist : playlistName;
      final Playlist playlist;
      final Playlist existingPlaylist = youTubeService.getMyPlaylistByTitle(playlistName);
      if (existingPlaylist == null) {
        playlist = youTubeService.createPlaylist(playlistName, c.getContextDescription(), mediaPackage.getSeries());
      } else {
        playlist = existingPlaylist;
      }
      youTubeService.addPlaylistItem(playlist.getId(), video.getId());
      // Create new publication element
      final URL url = new URL("http://www.youtube.com/watch?v=" + video.getId());
      return PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_NAME, url.toURI(), MimeTypes.parseMimeType(MIME_TYPE));
    } catch (Exception e) {
      logger.error("failed publishing to Youtube", e);
      logger.warn("Error publishing {}, {}", element, e.getMessage());
      if (e instanceof PublicationException) {
        throw (PublicationException) e;
      } else {
        throw new PublicationException("YouTube publish failed on job: " + ToStringBuilder.reflectionToString(job, ToStringStyle.MULTI_LINE_STYLE), e);
      }
    }
  }

  @Override
  public Job retract(final MediaPackage mediaPackage) throws PublicationException {
    System.out.println(org.mortbay.jetty.Handler.class);
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Mediapackage must be specified");
    }
    try {
      List<String> arguments = new ArrayList<String>();
      arguments.add(MediaPackageParser.getAsXml(mediaPackage));
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new PublicationException("Unable to create a job", e);
    }
  }

  /**
   * Retracts the mediapackage from YouTube.
   *
   * @param job
   *          the associated job
   * @param mediaPackage
   *          the mediapackage
   * @throws PublicationException
   *           if retract did not work
   */
  private Publication retract(final Job job, final MediaPackage mediaPackage) throws PublicationException {
    logger.info("Retract video from YouTube: {}", mediaPackage);
    Publication youtube = null;
    for (Publication publication : mediaPackage.getPublications()) {
      if (CHANNEL_NAME.equals(publication.getChannel())) {
        youtube = publication;
        break;
      }
    }
    if (youtube == null) {
      return null;
    }
    final YouTubePublicationAdapter contextStrategy = new YouTubePublicationAdapter(mediaPackage, workspace);
    final String episodeName = contextStrategy.getEpisodeName();
    try {
      retract(mediaPackage.getSeriesTitle(), episodeName);
    } catch (final Exception e) {
      logger.error("Failure retracting YouTube media {}", e.getMessage());
      throw new PublicationException("YouTube media retract failed on job: "
          + ToStringBuilder.reflectionToString(job, ToStringStyle.MULTI_LINE_STYLE), e);
    }
    return youtube;
  }

  private void retract(final String seriesTitle, final String episodeName) throws Exception {
    final List<SearchResult> items = youTubeService.searchMyVideos(truncateTitleToMaxFieldLength(episodeName, false), null, 1).getItems();
    if (!items.isEmpty()) {
      final String videoId = items.get(0).getId().getVideoId();
      if (seriesTitle != null) {
        final Playlist playlist = youTubeService.getMyPlaylistByTitle(truncateTitleToMaxFieldLength(seriesTitle, true));
        youTubeService.removeVideoFromPlaylist(playlist.getId(), videoId);
      }
      youTubeService.removeMyVideo(videoId);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(final Job job) throws Exception {
    Operation op = null;
    try {
      op = Operation.valueOf(job.getOperation());
      List<String> arguments = job.getArguments();
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(0));
      switch (op) {
        case Publish:
          Publication publicationElement = publish(job, mediapackage, arguments.get(1));
          return (publicationElement == null) ? null : MediaPackageElementParser.getAsXml(publicationElement);
        case Retract:
          Publication retractedElement = retract(job, mediapackage);
          return (retractedElement == null) ? null : MediaPackageElementParser.getAsXml(retractedElement);
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + job.getOperation() + "'");
      }
    } catch (final IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (final IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (final Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Callback for the OSGi environment to set the workspace reference.
   *
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi environment to set the service registry reference.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  boolean isMaxFieldLengthSet() {
    return maxFieldLength != 0;
  }

  private String truncateTitleToMaxFieldLength(final String title, final boolean tolerateNull) {
    if (StringUtils.isBlank(title) && !tolerateNull) {
      throw new IllegalArgumentException("Title fields cannot be null, empty, or whitespace");
    }
    if (isMaxFieldLengthSet() && (title != null)) {
      return StringUtils.left(title, maxFieldLength);
    } else {
      return title;
    }
  }

  private void defaultMaxFieldLength(String maxFieldLength) {
    if (StringUtils.isBlank(maxFieldLength)) {
      this.maxFieldLength = 0;
    } else {
      try {
        this.maxFieldLength = Integer.parseInt(maxFieldLength);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("maxFieldLength must be an integer");
      }
      if (this.maxFieldLength <= 0) {
        throw new IllegalArgumentException("maxFieldLength must be greater than zero");
      }
    }
  }

}
