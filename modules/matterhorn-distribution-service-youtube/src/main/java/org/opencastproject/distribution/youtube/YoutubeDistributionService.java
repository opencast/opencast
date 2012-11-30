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
package org.opencastproject.distribution.youtube;

import static org.opencastproject.mediapackage.MediaPackageElements.YOUTUBE;
import static org.opencastproject.mediapackage.Track.TYPE;

import org.opencastproject.deliver.youtube.YoutubeConfiguration;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.google.gdata.client.media.MediaService;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.mediarss.MediaCategory;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.media.mediarss.MediaTitle;
import com.google.gdata.data.youtube.PlaylistEntry;
import com.google.gdata.data.youtube.PlaylistLinkEntry;
import com.google.gdata.data.youtube.PlaylistLinkFeed;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YouTubeNamespace;
import com.google.gdata.data.youtube.YtPublicationState;
import com.google.gdata.util.ServiceException;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Distributes media to a Youtube play list.
 */
public class YoutubeDistributionService extends AbstractJobProducer implements DistributionService, ManagedService {

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.youtube";

  /** Time to wait between polling for status (milliseconds.) */
  private static final long POLL_MILLISECONDS = 30L * 1000L;

  /** Base URL of playlists FIXME: Should be read in from properties file */
  private static final String PLAYLIST_URL = "http://gdata.youtube.com/feeds/api/users/{username}/playlists";

  /** Base URL of video entries FIXME: Should be read in from properties file */
  private static final String VIDEO_ENTRY_URL = "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads";

  /** Base URL of feed entries */
  private static final String VIDEO_FEED_URL = "http://gdata.youtube.com/feeds/api/users/{username}/uploads";

  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionService.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** workspace instance */
  protected Workspace workspace = null;

  /** workflow service instance */
  protected WorkflowService workflowService = null;

  /** The remote service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** Youtube configuration instance */
  protected static YoutubeConfiguration config = null;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The security service */
  private SecurityService securityService;

  /**
   * The default playlist to distribute to, in case there is not enough information in the mediapackage to find a
   * playlist
   */
  private String defaultPlaylist;

  /** chunk youtube upload */
  private boolean isDefaultChunked = true;

  /**
   * Creates a new instance of the youtube distribution service.
   */
  public YoutubeDistributionService() {
    super(JOB_TYPE);
  }

  /**
   * Called when service activates. Defined in OSGi resource file.
   */
  public synchronized void activate(ComponentContext cc) {
    config = YoutubeConfiguration.getInstance();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Update method from managed service");

    String username = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.username"));
    String password = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.password"));
    String clientid = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.clientid"));
    String developerkey = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.developerkey"));
    String category = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.category"));
    String keywords = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.keywords"));

    String privacy = StringUtils
            .trimToNull((String) properties.get("org.opencastproject.distribution.youtube.private"));

    String isChunked = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.chunked"));

    defaultPlaylist = StringUtils.trimToNull((String) properties
            .get("org.opencastproject.distribution.youtube.default.playlist"));

    // Setup configuration from properties
    if (StringUtils.isBlank(clientid))
      throw new IllegalArgumentException("Youtube clientid must be specified");
    if (StringUtils.isBlank(developerkey))
      throw new IllegalArgumentException("Youtube developerkey must be specified");
    if (StringUtils.isBlank(username))
      throw new IllegalArgumentException("Youtube username must be specified");
    if (StringUtils.isBlank(password))
      throw new IllegalArgumentException("Youtube password must be specified");
    if (StringUtils.isBlank(category))
      throw new IllegalArgumentException("Youtube category must be specified");
    if (StringUtils.isBlank(keywords))
      throw new IllegalArgumentException("Youtube keywords must be specified");
    if (StringUtils.isBlank(privacy))
      throw new IllegalArgumentException("Youtube privacy must be specified");

    String uploadUrl = VIDEO_ENTRY_URL.replaceAll("\\{username\\}", username);

    config.setClientId(clientid);
    config.setDeveloperKey(developerkey);
    config.setUserId(username);
    config.setUploadUrl(uploadUrl);
    config.setPassword(password);
    config.setKeywords(keywords);
    config.setCategory(category);
    config.setVideoPrivate(Boolean.getBoolean(privacy));

    if (StringUtils.isNotBlank(isChunked))
      isDefaultChunked = Boolean.getBoolean(isChunked);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(MediaPackage, String)
   */
  @Override
  public Job distribute(MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {

    if (mediapackage == null)
      throw new MediaPackageException("Mediapackage must be specified");
    if (elementId == null)
      throw new MediaPackageException("Element ID must be specified");
    MediaPackageElement element = mediapackage.getElementById(elementId);
    if (element == null)
      throw new IllegalStateException("Mediapackage does not contain element " + elementId);
    if (!Track.TYPE.equals(element.getElementType())) {
      logger.info("Ignoring media package element {}", element);
      return null;
    }

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Distributes the element to the distribution channel and returns a reference to the distributed version of the
   * element.
   * 
   * @param job
   *          the associated job
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param element
   *          the element to distribute
   * @return the distributed element
   * @throws DistributionException
   *           if distribution fails
   * @throws MediaPackageException
   *           if the mediapackage is in an inconsistent state
   */
  private MediaPackageElement distribute(Job job, String mediaPackageId, MediaPackage mediaPackage)
          throws DistributionException {

    if (mediaPackageId == null)
      throw new IllegalArgumentException("Mediapackage ID must be specified");
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    MediaPackageElement element = mediaPackage.getElementById(mediaPackageId);
    if (element == null)
      throw new IllegalArgumentException("Mediapackage element must be specified");
    if (element.getIdentifier() == null)
      throw new IllegalArgumentException("Mediapackage element must have an identifier");
    if (element.getMimeType().toString().matches("text/xml"))
      throw new IllegalArgumentException("Mediapackage element cannot be XML");

    try {
      // create context strategy for distribution
      // MediaPackage mp = getMediaPackageFromId(mediaPackageId);
      YoutubeDistributionContextStrategy contextStrategy = new YoutubeDistributionContextStrategy(mediaPackage,
              workspace);

      // Initialise the YT service for distribution of the mediapackage element
      YouTubeService ytService = new YouTubeService(config.getClientId(), config.getDeveloperKey());

      logger.debug("set youtube user credentails and authenticate");
      ytService.setUserCredentials(config.getUserId(), config.getPassword());
      if (!isDefaultChunked)
        ytService.setChunkedMediaUpload(MediaService.NO_CHUNKED_MEDIA_REQUEST);

      // generate the VideoEntry to upload
      VideoEntry newEntry = new VideoEntry();
      YouTubeMediaGroup mg = newEntry.getOrCreateMediaGroup();
      mg.setTitle(new MediaTitle());
      String episodeName = contextStrategy.getEpisodeName(mediaPackageId);
      if (episodeName == null) {
        episodeName = "unknown";
      }
      logger.debug("youtube episode name '{}' for media package element {}", episodeName, mediaPackageId);
      mg.getTitle().setPlainTextContent(episodeName);
      mg.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME, config.getCategory()));
      mg.setKeywords(new MediaKeywords());
      String[] keywords = contextStrategy.getEpisodeKeywords(mediaPackageId);
      logger.debug("youtube keywords '{}' for media element {}", Arrays.toString(keywords), mediaPackageId);
      Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

      if (keywords.length == 0) {
        mg.getKeywords().addKeyword("none");
      } else {
        for (String word : keywords) {
          List<String> matchList = new ArrayList<String>();
          Matcher regexMatcher = regex.matcher(word);
          while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
              // Add double-quoted string without the quotes
              matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
              // Add single-quoted string without the quotes
              matchList.add(regexMatcher.group(2));
            } else {
              // Add unquoted word
              matchList.add(regexMatcher.group());
            }
          }
          for (String item : matchList) {
            mg.getKeywords().addKeyword(item);
          }
        }
      }
      // Add default keywords
      for (String item : config.getKeywords().split(", ")) {
        mg.getKeywords().addKeyword(item);
      }

      mg.setDescription(new MediaDescription());
      String episodeDescription = contextStrategy.getEpisodeDescription(mediaPackageId);
      if (episodeDescription == null) {
        episodeDescription = "unknown";
      }
      logger.debug("youtube episode description '{}' for media package element {}", episodeDescription, mediaPackageId);
      mg.getDescription().setPlainTextContent(episodeDescription);
      mg.setPrivate(config.isVideoPrivate());

      // attach raw media file to video entry
      MediaFileSource ms = new MediaFileSource(workspace.get(element.getURI()), element.getMimeType().toString());
      newEntry.setMediaSource(ms);

      // begin the upload to YouTube
      String videoUrl = null;
      VideoEntry uploadedEntry = null;
      try {
        uploadedEntry = ytService.insert(new URL(VIDEO_ENTRY_URL), newEntry);
        videoUrl = uploadedEntry.getSelfLink().getHref();
      } catch (ServiceException e) {
        logger.error("Failed to upload to YouTube: {}", e.getMessage());
        throw new DistributionException(e);
      }

      // monitor the publication process before returning
      while (true) {
        VideoEntry checkedEntry = ytService.getEntry(new URL(videoUrl), VideoEntry.class);

        // if entry is not a draft it has been successfully delivered
        if (!checkedEntry.isDraft())
          break;

        // check the publication status
        YtPublicationState pubState = checkedEntry.getPublicationState();
        if (pubState.getState() != YtPublicationState.State.PROCESSING) {
          logger.error("Video distribution to YouTube failed in state: {}", pubState.getDescription());
          throw new DistributionException("Video distribution to YouTube failed in state: " + pubState.getDescription());
        }
        Thread.sleep(POLL_MILLISECONDS);
      }

      // insert the video into the proper yt playlist mapped from the context strategy
      boolean playlistExists = false;
      PlaylistLinkEntry playlist = null;
      String playlistName = contextStrategy.getContextName(mediaPackageId);
      String playlistDescription = contextStrategy.getContextDescription(mediaPackageId);

      if (playlistName == null) {
        playlistName = defaultPlaylist;
      }
      logger.debug("youtube playlist name '{}' for media package element {}", playlistName, mediaPackageId);
      logger.debug("youtube playlist description '{}' for media package element {}", playlistDescription,
              mediaPackageId);

      String feedUrl = PLAYLIST_URL.replaceAll("\\{username\\}", config.getUserId());

      while (feedUrl != null) {
        PlaylistLinkFeed playlistFeed = ytService.getFeed(new URL(feedUrl), PlaylistLinkFeed.class);
        for (PlaylistLinkEntry entry : playlistFeed.getEntries()) {
          if (entry.getTitle().getPlainText().matches(playlistName)) {
            logger.info("Playlist {} exists", playlistName);
            playlist = entry;
            playlistExists = true;
            break;
          }
        }
        if (playlistExists || playlistFeed.getNextLink() == null)
          break;
        feedUrl = playlistFeed.getNextLink().getHref();
      }

      // create playlist if it doesn't exist
      if (!playlistExists) {
        logger.info("Creating playlist {}", playlistName);
        playlist = new PlaylistLinkEntry();
        playlist.setTitle(new PlainTextConstruct(playlistName));
        playlist.setSummary(new PlainTextConstruct(playlistDescription));

        playlist = ytService.insert(new URL(feedUrl), playlist);
      }
      // add video to the playlist
      String playlistUrl = playlist.getFeedUrl();
      VideoEntry videoToAdd = ytService.getEntry(new URL(videoUrl), VideoEntry.class);
      PlaylistEntry playlistEntry = new PlaylistEntry(videoToAdd);
      ytService.insert(new URL(playlistUrl), playlistEntry);

      logger.info("Upload succeeded. URL of video: {}", videoUrl);
      return MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromURI(new URI(uploadedEntry.getMediaGroup().getPlayer().getUrl()), TYPE, YOUTUBE);

    } catch (Exception e) {
      logger.warn("Error distributing {}, {}", element, e.getMessage());
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  public Job retract(MediaPackage mediaPackage, String elementId) throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

    try {
      List<String> arguments = new ArrayList<String>();
      arguments.add(MediaPackageParser.getAsXml(mediaPackage));
      arguments.add(elementId);
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(), arguments);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Retracts the mediapackage from YouTube.
   * 
   * @param job
   *          the associated job
   * @param mediapackageId
   *          the mediapackage
   * @throws DistributionException
   *           if retract did not work
   */
  private void retract(Job job, String mediaPackageId, MediaPackage mediaPackage) throws DistributionException {
    logger.info("Trying to remove media from package: {}", mediaPackageId);
    try {
      // Initialise the YT service for distribution of the mediapackage element
      YouTubeService ytService = new YouTubeService(config.getClientId(), config.getDeveloperKey());
      ytService.setUserCredentials(config.getUserId(), config.getPassword());
      // create context strategy for distribution
      YoutubeDistributionContextStrategy contextStrategy = new YoutubeDistributionContextStrategy(mediaPackage,
              workspace);
      String episodeName = contextStrategy.getEpisodeName(mediaPackageId);
      if (episodeName == null) {
        logger.error("Unable to retract a recording with no episode name");
      } else {
        VideoFeed videoFeed = ytService.getFeed(
                new URL(VIDEO_FEED_URL.replaceAll("\\{username\\}", config.getUserId())), VideoFeed.class);
        for (VideoEntry videoEntry : videoFeed.getEntries()) {
          logger.info("Video title: {}", videoEntry.getTitle().getPlainText());
          if (videoEntry.getTitle().getPlainText().matches(episodeName)) {
            logger.info("Removing video with id: {}", videoEntry.getId());
            videoEntry.delete();
          }
        }
      }
    } catch (Exception e) {
      logger.error("Failure retracting YouTube media {}", e.getMessage());
      throw new DistributionException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    try {
      op = Operation.valueOf(job.getOperation());
      List<String> arguments = job.getArguments();
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(0));
      String elementId = arguments.get(1);
      switch (op) {
        case Distribute:
          MediaPackageElement distributedElement = distribute(job, elementId, mediapackage);
          return (distributedElement == null) ? null : MediaPackageElementParser.getAsXml(distributedElement);
        case Retract:
          retract(job, elementId, mediapackage);
          return null;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + job.getOperation() + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'");
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations");
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'");
    }
  }

  /**
   * Callback for the OSGi environment to set the workflow service
   * 
   * @param service
   *          instance of the workflow service
   */
  protected void setWorkflowService(WorkflowService service) {
    this.workflowService = service;
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

}
