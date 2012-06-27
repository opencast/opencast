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

package org.opencastproject.deliver.youtube;

import org.opencastproject.deliver.actions.DeliveryAction;
import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.RetryException;
import org.opencastproject.deliver.store.InvalidKeyException;

import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.mediarss.MediaCategory;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.media.mediarss.MediaTitle;
import com.google.gdata.data.youtube.PlaylistEntry;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YouTubeNamespace;
import com.google.gdata.data.youtube.YtPublicationState;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.InvalidEntryException;
import com.google.gdata.util.ServiceException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * DeliveryAction to upload YouTube video.
 * 
 * @author Jonathan A. Smith
 */

public class YouTubeDeliveryAction extends DeliveryAction {

  private static final long serialVersionUID = -7214750182728860058L;

  /** Base URL of playlists. */
  private final static String PLAYLIST_URL = "http://gdata.youtube.com/feeds/api/playlists/";

  /** Time to wait between polling for status (milliseconds.) */
  private final long POLL_MILLISECONDS = 30L * 1000L;

  /** Configuration parameters for YouTube service. */
  private YouTubeConfiguration configuration;

  /** YouTube service connector. */
  private YouTubeService service;

  /** URL for accessing uploaded entry. */
  private String entry_url;

  /**
   * Constructs a YouTubeDeliveryAction.
   */

  public YouTubeDeliveryAction() {
    super();
    this.configuration = YouTubeConfiguration.getInstance();
    service = new YouTubeService(configuration.getClientId(), configuration.getDeveloperKey());
  }

  /**
   * Returns the YouTube video entry URL or null if not yet set.
   * 
   * @return video entry URL
   */

  public String getEntryUrl() {
    return entry_url;
  }

  /**
   * Sets the YouTube video entry URL.
   * 
   * @param entry_url
   *          URL for video entry
   */

  public void setEntryUrl(String entry_url) {
    this.entry_url = entry_url;
  }

  /**
   * Deliver a video clip to YouTube.
   * 
   * @throws Exception
   */

  @Override
  protected void execute() throws Exception {
    status("Upload in progress");

    // Authenticate to service
    authenticate(configuration.getUserId(), configuration.getPassword());

    // Upload video
    VideoEntry entry = createVideoEntry();
    VideoEntry created_entry = uploadVideo(entry);
    entry_url = created_entry.getSelfLink().getHref();

    // Add to playlist
    addToPlaylist(created_entry, getDestination());

    // Wait for publication or for entry to be rejected
    waitForPublication();
  }

  /**
   * Upload the video clip to YouTube.
   * 
   * @param entry
   *          request VideoEntry
   * @return created VideoEntry
   * @throws Exception
   */

  private VideoEntry uploadVideo(VideoEntry entry) throws Exception {
    createMediaGroup(entry);
    VideoEntry created_entry = insertEntry(entry);
    status("Video Uploaded");
    return created_entry;
  }

  /**
   * Authenticate the delivery user.
   * 
   * @param user
   *          YouTube user id or gmail name
   * @param password
   *          user's password
   * @throws RetryException
   */

  private void authenticate(String user, String password) throws RetryException {
    log("Auth user=" + user);
    try {
      service.setUserCredentials(user, password);
    } catch (AuthenticationException except) {
      throw new RetryException(except);
    }
  }

  /**
   * Creates a VideoEntry to be posted to YouTube.
   * 
   * @return initialized VideoEntry
   */

  private VideoEntry createVideoEntry() {
    VideoEntry entry = new VideoEntry();
    String file_name = getMediaPath();
    MediaFileSource source = new MediaFileSource(new File(file_name), mimeType(file_name));
    entry.setMediaSource(source);
    return entry;
  }

  /**
   * Returns a mime type for the video file.
   * 
   * @param file_name
   *          name of file ending in video extension
   * @return mime type
   */

  private String mimeType(String file_name) {
    String extension = file_name.substring(file_name.lastIndexOf(".") + 1);
    return "video/" + extension;
  }

  /**
   * Creates an initialized YouTubeMediaGroup to be used to post clip metadata.
   * 
   * @param entry
   *          VideoEntry
   * @return YouTubeMediaGroup
   */

  private YouTubeMediaGroup createMediaGroup(VideoEntry entry) {
    YouTubeMediaGroup media_group = entry.getOrCreateMediaGroup();

    // Title
    media_group.setTitle(new MediaTitle());
    media_group.getTitle().setPlainTextContent(getTitle());

    // Category
    media_group.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME, configuration.getCategory()));

    // Abstract
    media_group.setDescription(new MediaDescription());
    media_group.getDescription().setPlainTextContent(getAbstract());

    // Tags
    setKeywords(media_group, getTags());

    // Private?
    media_group.setPrivate(configuration.isVideoPrivate());

    return media_group;
  }

  /**
   * Adds the clip's tags as keywords for the YouTube entry.
   * 
   * @param media_group
   *          YouTubeMediaGroup specifying clip metadata
   * @param tags
   *          array of tags
   */

  private void setKeywords(YouTubeMediaGroup media_group, String[] tags) {
    media_group.setKeywords(new MediaKeywords());
    MediaKeywords keywords = media_group.getKeywords();
    if (tags != null && tags.length > 0) {
      for (String tag : tags)
        keywords.addKeyword(tag);
    } else
      keywords.addKeyword("Northwestern");
  }

  /**
   * Posts the VideoEntry to YouTube.
   * 
   * @param entry
   *          VideoEntry to be posted
   * @return ceaated video entry
   * @throws Exception
   */

  private VideoEntry insertEntry(VideoEntry entry) throws Exception {
    try {
      log("Starting upload of " + getMediaPath());
      VideoEntry created_entry = service.insert(new URL(configuration.getUploadUrl()), entry);
      if (created_entry != null)
        log("Inserted video entry: " + created_entry);
      return created_entry;
    } catch (InvalidEntryException except) {
      throw decodeEntryException(except);
    } catch (IOException except) {
      throw new RetryException(except);
    } catch (ServiceException except) {
      throw new RetryException(except);
    }
  }

  /**
   * Parses the error XML in an InvalidEntryRequest. Attempts to set the action status to a string rendering of the
   * error. Retrns a FailedException or RetryException with error information depending on the severity of the error
   * code.
   * 
   * @param except
   *          InvalidEntryException
   * @return FailedException of RetryException
   * @throws FailedException
   */

  private Exception decodeEntryException(InvalidEntryException except) throws FailedException {

    try {
      StringReader reader = new StringReader(except.getResponseBody());
      SAXBuilder builder = new SAXBuilder();
      Document document = builder.build(reader);
      Element root_element = document.getRootElement();
      Element error_element = root_element.getChild("error");

      String domain = "";
      Element domain_element = error_element.getChild("domain");
      if (domain_element != null)
        domain = domain_element.getText();

      String code = "";
      Element code_element = error_element.getChild("code");
      if (code_element != null)
        code = code_element.getText();

      String location = "";
      Element location_element = error_element.getChild("location");
      if (location_element != null)
        location = location_element.getText();

      String status_message = decodeYouTubeError(domain, code, location);
      status(status_message);
      log(status_message);

      if (domain.equals("yt:validation"))
        return new FailedException(except);
      else
        return new RetryException("Entry exception", except);
    } catch (Exception except2) {
      throw new FailedException(except2);
    }
  }

  /**
   * Returns an english description of a YouTube error: domain, code, and location.
   * 
   * @param domain
   *          YouTube error "domain" or class
   * @param code
   *          YouTube error code
   * @param location
   *          YouTube error location (typically xpath to service request)
   * @return error message as String
   */

  private String decodeYouTubeError(String domain, String code, String location) {
    if (domain.equals("yt:validation")) {
      if (code.equals("required"))
        return "Missing: " + decodeLocation(location);
      if (code.equals("deprecated"))
        return "Invalid: " + decodeLocation(location);
      if (code.equals("invalid_format"))
        return "Format invalid: " + decodeLocation(location);
      if (code.equals("invalid_character"))
        return "Invalid character: " + decodeLocation(location);
      if (code.equals("too_long"))
        return "Too long: " + decodeLocation(location);
      else
        return "Validation error: " + decodeLocation(location);
    } else if (domain.equals("yt:quota")) {
      if (code.equals("too_many_recent_calls"))
        return "Too many uploads";
      else if (code.equals("too_many_entries"))
        return "Too many entries";
      else
        return "Quota exceeded";
    } else if (domain.equals("yt:authentication")) {
      if (code.equals("InvalidToken"))
        return "Invalid access to service";
      if (code.equals("TokenExpired"))
        return "Service session expired";
    } else if (domain.equals("yt:service")) {
      if (code.equals("disabled_in_maintenance_mode"))
        return "Service temporarily unavailable";
    }
    return "Delivery failed [3]";
  }

  /**
   * Returns a YouTube error "location" as a String if recognized. Returns the original location, otherwise.
   * 
   * @param location
   *          typically an xpath to the data in the service request
   * @return translated or original location
   */

  private String decodeLocation(String location) {
    if (location.contains("media:title"))
      return "title";
    if (location.contains("media:keywords"))
      return "item tags";
    return location;
  }

  /**
   * Adds a video clip to a playlist.
   * 
   * @param entry
   *          VideoEntry to be added
   * @param playlist_id
   *          YouTube id of destination playlist
   * @throws RetryException
   */

  private void addToPlaylist(VideoEntry entry, String playlist_id) throws RetryException {
    try {
      StringBuilder url = new StringBuilder(PLAYLIST_URL);
      if (!PLAYLIST_URL.endsWith("/"))
        url.append('/');
      url.append(playlist_id);
      log("Adding to playlist URL: " + url.toString());
      PlaylistEntry playlistEntry = new PlaylistEntry(entry);
      service.insert(new URL(url.toString()), playlistEntry);
    } catch (IOException except) {
      throw new RetryException(except);
    } catch (ServiceException except) {
      throw new RetryException(except);
    }
  }

  /**
   * Waits for publication to complete.
   * 
   * @throws FailedException
   * @throws InvalidKeyException 
   */

  private void waitForPublication() throws FailedException, InvalidKeyException {
    status("YouTube processing started");

    while (true) {
      VideoEntry checked_entry = refreshEntry();
      YtPublicationState publication_state = checked_entry.getPublicationState();
      if (!checked_entry.isDraft()) {
        status("Published");
        succeed("Video delivered: " + getMediaPath());
        break;
      } else if (publication_state.getState() != YtPublicationState.State.PROCESSING) {
        log(publication_state.getDescription());
        status(publication_state.getDescription());
        throw new FailedException(getMediaPath() + ": " + publication_state.getDescription());
      } else {
        try {
          Thread.sleep(POLL_MILLISECONDS);
        } catch (Exception except) {
          // Blank
        }
      }
    }
  }

  /**
   * Gets a new copy of the video entry.
   * 
   * @return current entry
   * @throws FailedException
   */

  private VideoEntry refreshEntry() throws FailedException {
    try {
      return service.getEntry(new URL(entry_url), VideoEntry.class);
    } catch (IOException except) {
      throw new FailedException(except);
    } catch (ServiceException except) {
      throw new FailedException(except);
    }
  }
}
