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

package org.opencastproject.publication.youtube.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.serverError;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.publication.youtube.YouTubeAPIVersion3Service;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Rest endpoint for publishing media to youtube.
 */
@Path("/youtube")
@RestService(name = "youtubepublicationservice",
    title = "YouTube Publication Service",
    abstractText = "",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
            + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
@Component(
    immediate = true,
    service = YouTubePublicationRestService.class,
    property = {
        "service.description=YouTube Publication REST Endpoint",
        "opencast.service.type=org.opencastproject.publication.youtube",
        "opencast.service.path=/youtube",
        "opencast.service.jobproducer=true"
    }
)
@JaxrsResource
public class YouTubePublicationRestService extends AbstractJobProducerEndpoint {

  private final Logger logger = LoggerFactory.getLogger(YouTubePublicationRestService.class);

  /** Valid values for the playlist "privacyStatus" parameter accepted by the playlist endpoints below. */
  private static final List<String> VALID_PRIVACY_STATUSES = Arrays.asList("public", "unlisted", "private");

  /** Maximum number of attempts when adding a video to a playlist hits a transient error. */
  private static final int MAX_ADD_PLAYLIST_ITEM_ATTEMPTS = 3;

  /** Delay between retries when adding a video to a playlist hits a transient error. */
  private static final long ADD_PLAYLIST_ITEM_RETRY_DELAY_MILLIS = 2000L;

  protected YouTubePublicationService service;

  protected ServiceRegistry serviceRegistry = null;

  /** The already-authenticated YouTube API v3 client wrapper, used by the synchronous playlist endpoints below. */
  protected YouTubeAPIVersion3Service youTubeApiService;

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "publish",
      description = "Publish a media package element to youtube publication channel",
      returnDescription = "The job that can be used to track the publication",
      restParameters = {
          @RestParameter(
              name = "mediapackage",
              isRequired = true,
              description = "The mediapackage",
              type = Type.TEXT
          ),
          @RestParameter(
              name = "elementId",
              isRequired = true,
              description = "The element to publish",
              type = Type.STRING
          ),
          @RestParameter(
              name = "thumbnailId",
              isRequired = false,
              description = "The optional thumbnail to publish",
              type = Type.STRING
          ),
          @RestParameter(
              name = "captionIds",
              isRequired = false,
              description = "The captions to publish",
              type = Type.STRING
          )
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "elementId does not reference a track")
      }
  )
  public Response publish(
      @FormParam("mediapackage") final String mediaPackageXml,
      @FormParam("elementId") final String elementId,
      @FormParam("thumbnailId") final String thumbnailId,
      @FormParam("captionIds") final String captionIds
  ) {
    final Job job;
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Track track = mediapackage.getTrack(elementId);
      final Attachment thumbnail = (Attachment) mediapackage.getElementById(thumbnailId);
      if (track != null) {
        final List<Track> captions = new ArrayList<>();
        if (StringUtils.isNotBlank(captionIds)) {
          for (String captionId : StringUtils.split(captionIds, ",")) {
            final Track caption = mediapackage.getTrack(captionId.trim());
            if (caption != null) {
              captions.add(caption);
            }
          }
        }
        job = service.publish(mediapackage, track, thumbnail, captions);
      } else {
        return badRequest();
      }
    } catch (Exception e) {
      logger.warn("Error publishing element '{}' to YouTube", elementId, e);
      return serverError();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
      name = "retract",
      description = "Retract a media package from the youtube publication channel",
      returnDescription = "The job that can be used to track the retraction",
      restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job")
      }
  )
  public Response retract(@FormParam("mediapackage") final String mediaPackageXml) {
    final Job job;
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(mediapackage);
    } catch (Exception e) {
      logger.warn("Unable to retract mediapackage '{}' from YouTube", mediaPackageXml, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    return (service instanceof JobProducer) ? (JobProducer) service : null;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry
   *
   * @param serviceRegistry
   *          the service registry
   */
  @Reference
  protected void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback from OSGi to set a reference to the youtube publication service.
   *
   * @param service
   *          the service to set
   */
  @Reference
  protected void setService(final YouTubePublicationService service) {
    this.service = service;
  }

  /**
   * Callback from OSGi to set a reference to the already-authenticated YouTube API v3 client wrapper.
   *
   * @param youTubeApiService
   *          the YouTube API v3 service
   */
  @Reference
  protected void setYouTubeApiService(final YouTubeAPIVersion3Service youTubeApiService) {
    this.youTubeApiService = youTubeApiService;
  }

  @POST
  @Path("/playlists")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createPlaylist",
      description = "Create a new YouTube playlist",
      returnDescription = "A JSON object with the id of the newly created playlist",
      restParameters = {
          @RestParameter(name = "title", isRequired = true, description = "The playlist title", type = Type.STRING),
          @RestParameter(name = "description", isRequired = false,
              description = "The playlist description", type = Type.STRING),
          @RestParameter(name = "privacyStatus", isRequired = false,
              description = "One of public, unlisted or private (defaults to unlisted)", type = Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "A JSON object with the playlist id"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "title is missing or privacyStatus is invalid"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "The playlist could not be created")
      }
  )
  public Response createPlaylist(
      @FormParam("title") final String title,
      @FormParam("description") final String description,
      @FormParam("privacyStatus") final String privacyStatus
  ) {
    final Response notInitialized = requireInitializedYouTubeService();
    if (notInitialized != null) {
      return notInitialized;
    }
    if (StringUtils.isBlank(title)) {
      return jsonResponse(Status.BAD_REQUEST, obj(p("error", "title is required")));
    }
    final String resolvedPrivacyStatus = StringUtils.defaultIfBlank(privacyStatus, "unlisted");
    if (!VALID_PRIVACY_STATUSES.contains(resolvedPrivacyStatus)) {
      return jsonResponse(Status.BAD_REQUEST,
          obj(p("error", "privacyStatus must be one of public, unlisted or private")));
    }
    logger.info("Creating YouTube playlist '{}' (privacy={})", title, resolvedPrivacyStatus);
    try {
      final Playlist playlist = youTubeApiService.createPlaylistWithPrivacyStatus(
          title, StringUtils.defaultString(description), resolvedPrivacyStatus);
      logger.info("Created YouTube playlist '{}' with id {}", title, playlist.getId());
      return jsonResponse(Status.OK, obj(p("playlistId", playlist.getId())));
    } catch (Exception e) {
      logger.warn("Error creating YouTube playlist '{}'", title, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    }
  }

  @POST
  @Path("/playlists/{playlistId}/items")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "addPlaylistItem",
      description = "Add a video to a YouTube playlist. Idempotent: adding an already-present video is a no-op",
      returnDescription = "A JSON object describing the resulting playlist item",
      pathParameters = {
          @RestParameter(name = "playlistId", isRequired = true,
              description = "The YouTube playlist id", type = Type.STRING)
      },
      restParameters = {
          @RestParameter(name = "videoId", isRequired = true,
              description = "The YouTube video id", type = Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The video is present in the playlist"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "videoId is missing"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The playlist or video does not exist"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "The video could not be added")
      }
  )
  public Response addPlaylistItem(
      @PathParam("playlistId") final String playlistId,
      @FormParam("videoId") final String videoId
  ) {
    final Response notInitialized = requireInitializedYouTubeService();
    if (notInitialized != null) {
      return notInitialized;
    }
    if (StringUtils.isBlank(videoId)) {
      return jsonResponse(Status.BAD_REQUEST, obj(p("error", "videoId is required")));
    }
    logger.info("Adding video {} to YouTube playlist {}", videoId, playlistId);
    try {
      final PlaylistItem existing = youTubeApiService.findPlaylistItem(playlistId, videoId);
      if (existing != null) {
        logger.info("Video {} already in YouTube playlist {}", videoId, playlistId);
        return jsonResponse(Status.OK,
            obj(p("ok", true), p("alreadyPresent", true), p("playlistItemId", existing.getId())));
      }
      final PlaylistItem created = addPlaylistItemWithRetry(playlistId, videoId);
      logger.info("Added video {} to YouTube playlist {} as playlist item {}",
          videoId, playlistId, created.getId());
      return jsonResponse(Status.OK,
          obj(p("ok", true), p("alreadyPresent", false), p("playlistItemId", created.getId())));
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == SC_NOT_FOUND) {
        logger.warn("Playlist {} or video {} not found on YouTube", playlistId, videoId);
        return jsonResponse(Status.NOT_FOUND, obj(p("error", "playlist or video not found")));
      }
      logger.warn("Error adding video '{}' to YouTube playlist '{}'", videoId, playlistId, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    } catch (Exception e) {
      logger.warn("Error adding video '{}' to YouTube playlist '{}'", videoId, playlistId, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    }
  }

  @PUT
  @Path("/playlists/{playlistId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updatePlaylist",
      description = "Update the title and/or description of a YouTube playlist",
      returnDescription = "A JSON object confirming the update",
      pathParameters = {
          @RestParameter(name = "playlistId", isRequired = true,
              description = "The YouTube playlist id", type = Type.STRING)
      },
      restParameters = {
          @RestParameter(name = "title", isRequired = false, description = "The new title", type = Type.STRING),
          @RestParameter(name = "description", isRequired = false,
              description = "The new description", type = Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The playlist was updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The playlist does not exist"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "The playlist could not be updated")
      }
  )
  public Response updatePlaylist(
      @PathParam("playlistId") final String playlistId,
      @FormParam("title") final String title,
      @FormParam("description") final String description
  ) {
    final Response notInitialized = requireInitializedYouTubeService();
    if (notInitialized != null) {
      return notInitialized;
    }
    logger.info("Updating YouTube playlist {} (title={}, description={})", playlistId, title, description);
    try {
      final Playlist updated = youTubeApiService.updatePlaylist(playlistId, title, description);
      if (updated == null) {
        logger.warn("YouTube playlist {} not found for update", playlistId);
        return jsonResponse(Status.NOT_FOUND, obj(p("error", "playlist not found")));
      }
      logger.info("Updated YouTube playlist {}", playlistId);
      return jsonResponse(Status.OK, obj(p("ok", true)));
    } catch (Exception e) {
      logger.warn("Error updating YouTube playlist '{}'", playlistId, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    }
  }

  @DELETE
  @Path("/playlists/{playlistId}/items/{videoId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "removePlaylistItem",
      description = "Remove a video from a YouTube playlist",
      returnDescription = "A JSON object indicating whether the video was removed",
      pathParameters = {
          @RestParameter(name = "playlistId", isRequired = true,
              description = "The YouTube playlist id", type = Type.STRING),
          @RestParameter(name = "videoId", isRequired = true,
              description = "The YouTube video id", type = Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "A JSON object indicating whether the video was present"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "The video could not be removed")
      }
  )
  public Response removePlaylistItem(
      @PathParam("playlistId") final String playlistId,
      @PathParam("videoId") final String videoId
  ) {
    final Response notInitialized = requireInitializedYouTubeService();
    if (notInitialized != null) {
      return notInitialized;
    }
    logger.info("Removing video {} from YouTube playlist {}", videoId, playlistId);
    try {
      final PlaylistItem existing = youTubeApiService.findPlaylistItem(playlistId, videoId);
      if (existing == null) {
        logger.info("Video {} was not in YouTube playlist {}", videoId, playlistId);
        return jsonResponse(Status.OK, obj(p("ok", true), p("removed", false)));
      }
      youTubeApiService.removeVideoFromPlaylist(playlistId, videoId);
      logger.info("Removed video {} from YouTube playlist {}", videoId, playlistId);
      return jsonResponse(Status.OK, obj(p("ok", true), p("removed", true)));
    } catch (Exception e) {
      logger.warn("Error removing video '{}' from YouTube playlist '{}'", videoId, playlistId, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    }
  }

  @DELETE
  @Path("/playlists/{playlistId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deletePlaylist",
      description = "Delete a YouTube playlist",
      returnDescription = "A JSON object confirming the deletion",
      pathParameters = {
          @RestParameter(name = "playlistId", isRequired = true,
              description = "The YouTube playlist id", type = Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The playlist was deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The playlist does not exist"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "The playlist could not be deleted")
      }
  )
  public Response deletePlaylist(@PathParam("playlistId") final String playlistId) {
    final Response notInitialized = requireInitializedYouTubeService();
    if (notInitialized != null) {
      return notInitialized;
    }
    logger.info("Deleting YouTube playlist {}", playlistId);
    try {
      final Playlist existing = youTubeApiService.getPlaylistById(playlistId);
      if (existing == null) {
        logger.warn("YouTube playlist {} not found for deletion", playlistId);
        return jsonResponse(Status.NOT_FOUND, obj(p("error", "playlist not found")));
      }
      youTubeApiService.removeMyPlaylist(playlistId);
      logger.info("Deleted YouTube playlist {}", playlistId);
      return jsonResponse(Status.OK, obj(p("ok", true)));
    } catch (Exception e) {
      logger.warn("Error deleting YouTube playlist '{}'", playlistId, e);
      return jsonResponse(Status.INTERNAL_SERVER_ERROR, obj(p("error", errorMessage(e))));
    }
  }

  /**
   * Adds a video to a playlist, retrying transient YouTube API errors (HTTP 409, or reason ABORTED /
   * SERVICE_UNAVAILABLE) up to {@link #MAX_ADD_PLAYLIST_ITEM_ATTEMPTS} times, waiting
   * {@link #ADD_PLAYLIST_ITEM_RETRY_DELAY_MILLIS} between attempts.
   */
  private PlaylistItem addPlaylistItemWithRetry(final String playlistId, final String videoId) throws IOException {
    for (int attempt = 1; attempt <= MAX_ADD_PLAYLIST_ITEM_ATTEMPTS; attempt++) {
      try {
        return youTubeApiService.addPlaylistItem(playlistId, videoId);
      } catch (GoogleJsonResponseException e) {
        if (!isTransientPlaylistError(e) || attempt == MAX_ADD_PLAYLIST_ITEM_ATTEMPTS) {
          throw e;
        }
        logger.warn("Transient error adding video '{}' to YouTube playlist '{}' (attempt {}/{}), retrying",
            videoId, playlistId, attempt, MAX_ADD_PLAYLIST_ITEM_ATTEMPTS, e);
        try {
          Thread.sleep(ADD_PLAYLIST_ITEM_RETRY_DELAY_MILLIS);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while retrying YouTube playlist item insert", ie);
        }
      }
    }
    throw new IllegalStateException("unreachable");
  }

  private static boolean isTransientPlaylistError(final GoogleJsonResponseException e) {
    if (e.getStatusCode() == 409) {
      return true;
    }
    final GoogleJsonError details = e.getDetails();
    if (details == null || details.getErrors() == null) {
      return false;
    }
    for (final GoogleJsonError.ErrorInfo error : details.getErrors()) {
      final String reason = error.getReason();
      if ("aborted".equalsIgnoreCase(reason) || "serviceUnavailable".equalsIgnoreCase(reason)) {
        return true;
      }
    }
    return false;
  }

  private static String errorMessage(final Exception e) {
    return StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
  }

  private Response requireInitializedYouTubeService() {
    if (youTubeApiService == null || !youTubeApiService.isInitialized()) {
      logger.warn("YouTube playlist endpoint called, but the YouTube publication service is not configured");
      return jsonResponse(Status.INTERNAL_SERVER_ERROR,
          obj(p("error", "YouTube publication service is not configured")));
    }
    return null;
  }

  private static Response jsonResponse(final Status status, final Jsons.Obj json) {
    return Response.status(status).entity(json.toJson()).type(MediaType.APPLICATION_JSON_TYPE).build();
  }

}
