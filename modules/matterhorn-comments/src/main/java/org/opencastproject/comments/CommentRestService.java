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

package org.opencastproject.comments;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Date;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Comment Service.
 */
@Path("/")
@RestService(name = "commentservice", title = "Comment Service", abstractText = "This service creates, edits and retrieves and helps managing comments.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class CommentRestService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CommentRestService.class);

  /** Series Service */
  private CommentService commentService;

  /** The security service */
  private SecurityService securityService;

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  /**
   * OSGi callback for setting series service.
   * 
   * @param commentService
   */
  public void setCommentService(CommentService commentService) {
    this.commentService = commentService;
  }

  /**
   * OSGi callback for setting the security service
   * 
   * @param securityService
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Activates REST service.
   * 
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
      if (StringUtils.isNotBlank(ccServerUrl))
        this.serverUrl = ccServerUrl;
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  private URI getCommentUrl(long commentId) {
    return UrlSupport.uri(serverUrl, serviceUrl, Long.toString(commentId));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{commentId}")
  @RestQuery(name = "getcomment", description = "Returns the comment with the given identifier", returnDescription = "Returns the comment as JSON", pathParameters = { @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The comment as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No comment with this identifier was found.") })
  public Response getComment(@PathParam("commentId") long commentId) throws NotFoundException {
    try {
      Comment comment = commentService.getComment(commentId);
      return Response.ok(comment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve comments: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("{commentId}")
  @RestQuery(name = "deletecomment", description = "Delete a comment", returnDescription = "No content.", pathParameters = { @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No comment with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The comment has been deleted.") })
  public Response deleteComment(@PathParam("commentId") long commentId) throws NotFoundException {
    try {
      commentService.deleteComment(commentId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not delete comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{commentId}")
  @RestQuery(name = "updatecomment", description = "Updates a comment", returnDescription = "The updated comment as JSON.", pathParameters = { @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, restParameters = {
          @RestParameter(name = "text", isRequired = false, description = "The comment text", type = TEXT),
          @RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING),
          @RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = Type.BOOLEAN) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The comment to update has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response updateComment(@PathParam("commentId") long commentId, @FormParam("text") String text,
          @FormParam("reason") String reason, @FormParam("resolved") Boolean resolved) throws NotFoundException {
    try {
      Comment dto = commentService.getComment(commentId);

      if (StringUtils.isNotBlank(text)) {
        text = text.trim();
      } else {
        text = dto.getText();
      }

      if (StringUtils.isNotBlank(reason)) {
        reason = reason.trim();
      } else {
        reason = dto.getReason();
      }

      if (resolved == null)
        resolved = dto.isResolvedStatus();

      Comment updatedComment = Comment.create(dto.getId(), text, dto.getAuthor(), reason, resolved,
              dto.getCreationDate(), new Date(), dto.getReplies());
      updatedComment = commentService.updateComment(updatedComment);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (CommentException e) {
      logger.warn("Could not update comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("")
  @RestQuery(name = "createcomment", description = "Creates a comment", returnDescription = "The created comment as JSON.", restParameters = {
          @RestParameter(name = "text", isRequired = true, description = "The comment text", type = TEXT),
          @RestParameter(name = "reason", isRequired = false, description = "The comment reason", type = STRING),
          @RestParameter(name = "resolved", isRequired = false, description = "The comment resolved status", type = Type.BOOLEAN) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The comment to update has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The created comment as JSON.") })
  public Response createComment(@FormParam("text") String text, @FormParam("reason") String reason,
          @FormParam("resolved") Boolean resolved) throws NotFoundException {
    try {
      User author = securityService.getUser();

      Comment createdComment = Comment.create(Option.<Long> none(), text, author, reason, resolved);
      createdComment = commentService.updateComment(createdComment);
      return Response.created(getCommentUrl(createdComment.getId().get())).entity(createdComment.toJson().toJson())
              .build();
    } catch (CommentException e) {
      logger.warn("Could not create comment: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("{commentId}")
  @RestQuery(name = "resolvecomment", description = "Resolves a comment", returnDescription = "The resolved comment.", pathParameters = { @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The comment to resolve has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The resolved comment as JSON.") })
  public Response resolveComment(@PathParam("commentId") long commentId) throws NotFoundException {
    try {
      Comment dto = commentService.getComment(commentId);

      Comment updatedComment = Comment.create(dto.getId(), dto.getText(), dto.getAuthor(), dto.getReason(), true,
              dto.getCreationDate(), new Date(), dto.getReplies());
      updatedComment = commentService.updateComment(updatedComment);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (CommentException e) {
      logger.warn("Could not resolve comment {}: {}", commentId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("{commentId}/{replyId}")
  @RestQuery(name = "deletereply", description = "Delete a comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING),
          @RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No comment or reply with this identifier was found."),
          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response deleteReply(@PathParam("commentId") long commentId, @PathParam("replyId") long replyId)
          throws NotFoundException {
    Comment comment = null;
    CommentReply reply = null;
    try {
      comment = commentService.getComment(commentId);
      for (CommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      comment.removeReply(reply);

      Comment updatedComment = commentService.updateComment(comment);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (CommentException e) {
      logger.warn("Could not remove comment reply {} from comment {}: {}",
              new String[] { Long.toString(replyId), Long.toString(commentId), ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{commentId}/{replyId}")
  @RestQuery(name = "updatecommentreply", description = "Updates a comment reply", returnDescription = "The updated comment as JSON.", pathParameters = {
          @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING),
          @RestParameter(name = "replyId", isRequired = true, description = "The comment reply identifier", type = STRING) }, restParameters = { @RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The comment to extend with a reply or the reply has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response updateCommentReply(@PathParam("commentId") long commentId, @PathParam("replyId") long replyId,
          @FormParam("text") String text) throws NotFoundException {
    Comment comment = null;
    CommentReply reply = null;
    try {
      comment = commentService.getComment(commentId);
      for (CommentReply r : comment.getReplies()) {
        if (r.getId().isNone() || replyId != r.getId().get().longValue())
          continue;
        reply = r;
        break;
      }

      if (reply == null)
        throw new NotFoundException("Reply with id " + replyId + " not found!");

      if (StringUtils.isNotBlank(text)) {
        text = text.trim();
      } else {
        text = reply.getText();
      }
      CommentReply updatedReply = CommentReply.create(reply.getId(), text, reply.getAuthor(), reply.getCreationDate(),
              new Date());
      comment.removeReply(reply);
      comment.addReply(updatedReply);

      Comment updatedComment = commentService.updateComment(comment);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (CommentException e) {
      logger.warn("Could not update comment reply {} from comment {}: {}",
              new String[] { Long.toString(replyId), Long.toString(commentId), ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("{commentId}/reply")
  @RestQuery(name = "createcommentreply", description = "Creates a comment reply", returnDescription = "The updated comment as JSON.", pathParameters = { @RestParameter(name = "commentId", isRequired = true, description = "The comment identifier", type = STRING) }, restParameters = { @RestParameter(name = "text", isRequired = true, description = "The comment reply text", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The comment to extend with a reply has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The updated comment as JSON.") })
  public Response createCommentReply(@PathParam("commentId") long commentId, @FormParam("text") String text)
          throws NotFoundException {
    Comment comment = null;
    try {
      comment = commentService.getComment(commentId);

      User author = securityService.getUser();
      CommentReply reply = CommentReply.create(Option.<Long> none(), text, author);
      comment.addReply(reply);

      Comment updatedComment = commentService.updateComment(comment);
      return Response.ok(updatedComment.toJson().toJson()).build();
    } catch (CommentException e) {
      logger.warn("Could not create comment reply on comment {}: {}", comment, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

}
