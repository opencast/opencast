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

package org.opencastproject.smil.endpoint;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement {@link SmilService} as REST Endpoint.
 */
@Path("/")
@RestService(name = "smil", title = "SmilService Rest Endpoint",
        abstractText = "SmilRestService provide other services to create and modify Smil objects.",
        notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/smil)"})
public class SmilServiceRest {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(SmilServiceRest.class);

  /**
   * SmilService to interact with
   */
  private SmilService smilService;

  @POST
  @Path("create")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "create", description = "Create new SMIL. Add some MediaPackage metadata.",
          restParameters = {
            @RestParameter(name = "mediaPackage", description = "MediaPackage for metadata.",
                    isRequired = false, type = RestParameter.Type.TEXT)},
          returnDescription = "Returns new SmilResponse with SMIL document inside.",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Create new SMIL successfull"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Given mediaPackage is not valid")
          })
  public Response createNewSmil(@FormParam("mediaPackage") String mediaPackage) {
    SmilResponse smilResponse = null;
    try {
      if (mediaPackage != null && !mediaPackage.isEmpty()) {
        MediaPackage mp = MediaPackageParser.getFromXml(mediaPackage);
        smilResponse = smilService.createNewSmil(mp);
      } else {
        smilResponse = smilService.createNewSmil();
      }

      return Response.ok(smilResponse).build();
    } catch (MediaPackageException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("MediaPackage not valid.").build();
    }
  }

  @POST
  @Path("addPar")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "addPar", description = "Add an parallel element to SMIL (into SMIL body or an element with given parentId).",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document where to add a par-element.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "parentId", description = "An element Id, were to add new parallel.",
                    isRequired = false, type = RestParameter.Type.STRING)},
          returnDescription = "Returns SmilResponse with a parallel element inside "
          + "(the new par will be returned as response entity).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Add par to SMIL successfull."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document doesn't contain an element with given parentId.")
          })
  public Response addParallel(@FormParam("smil") String smil, @FormParam("parentId") String parentId) {
    SmilResponse smilResponse = null;
    try {
      smilResponse = smilService.fromXml(smil);
    } catch (SmilException ex) {
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    }

    try {
      if (parentId != null && !parentId.isEmpty()) {
        smilResponse = smilService.addParallel(smilResponse.getSmil(), parentId);
      } else {
        smilResponse = smilService.addParallel(smilResponse.getSmil());
      }
      return Response.ok(smilResponse).build();
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document doesn't contain an element with given parentId.").build();
    }
  }

  @POST
  @Path("addSeq")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "addSeq", description = "Add an sequence element to SMIL (into SMIL body or an element with given parentId).",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document where to add a seq-element.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "parentId", description = "An element Id, were to add new sequence.",
                    isRequired = false, type = RestParameter.Type.STRING)},
          returnDescription = "Returns SmilResponse with a sequence element inside "
          + "(the new seq will be returned as response entity).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Add seq to SMIL successfull"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid"),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document doesn't contain an element with given parentId")
          })
  public Response addSequence(@FormParam("smil") String smil, @FormParam("parentId") String parentId) {
    SmilResponse smilResponse = null;
    try {
      smilResponse = smilService.fromXml(smil);
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    }

    try {
      if (parentId != null && !parentId.isEmpty()) {
        smilResponse = smilService.addSequence(smilResponse.getSmil(), parentId);
      } else {
        smilResponse = smilService.addSequence(smilResponse.getSmil());
      }
      return Response.ok(smilResponse).build();
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document doesn't contain an element with given parentId.").build();
    }
  }

  @POST
  @Path("addClip")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "addClip", description = "Add new media element based on given Track information and start / duration parameters. "
          + "ParentId specifies where to put the new media element.",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document where to add new media element.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "parentId", description = "An element Id, were to add new media.",
                    isRequired = false, type = RestParameter.Type.STRING),
            @RestParameter(name = "track", description = "Track (MediaPackageElement) to add as media element. "
                    + "Some information like Track source and flavor will be stored in ParamGroup (in SMIL Head) "
                    + "and referenced by paramGroup media element attribute.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "start", description = "Track start position in milliseconds.",
                    isRequired = true, type = RestParameter.Type.INTEGER),
            @RestParameter(name = "duration", description = "Clip duration in milliseconds (should be positive).",
                    isRequired = true, type = RestParameter.Type.INTEGER)},
          returnDescription = "Returns new Smil with an media element inside "
          + "(the new media and metadata elements will be returned as response entities).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Add media element to SMIL successfull."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Track not valid."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document doesn't contain an element with given parentId."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Start plus duration is bigger than Track length.")
          })
  public Response addClip(@FormParam("smil") String smil, @FormParam("parentId") String parentId, @FormParam("track") String track,
          @FormParam("start") long start, @FormParam("duration") long duration) {
    SmilResponse smilResponse = null;
    Track trackObj = null;
    try {
      smilResponse = smilService.fromXml(smil);
      trackObj = (Track) MediaPackageElementParser.getFromXml(track);
    } catch (SmilException ex) {
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    } catch (MediaPackageException ex) {
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Track is not valid.").build();
    }
    try {
      smilResponse = smilService.addClip(smilResponse.getSmil(), parentId, trackObj, start, duration);
      return Response.ok(smilResponse).build();
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document doesn't contain an element with given parentId.").build();
    }
  }

  @POST
  @Path("addClips")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "addClips", description = "Add new media elements based on given Tracks information and start / duration parameters. "
          + "ParentId specifies where to put the new media.",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document where to add new media elements.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "parentId", description = "An element Id, were to add new media. ",
                    isRequired = false, type = RestParameter.Type.STRING),
            @RestParameter(name = "tracks", description = "Tracks (MediaPackageElements) to add as media elements."
                    + "Some information like Track source and flavor will be stored in ParamGroup (in SMIL Head) "
                    + "and referenced by paramGroup media element attribute.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "start", description = "Track start position in milliseconds. "
                    + "The start position will be applied to each media element.",
                    isRequired = true, type = RestParameter.Type.INTEGER),
            @RestParameter(name = "duration", description = "Clip duration in milliseconds (should be positive). "
                    + "The duration will be applied to each media element.",
                    isRequired = true, type = RestParameter.Type.INTEGER)},
          returnDescription = "Returns new Smil with new media elements inside "
          + "(the new media and metadata elements will be returned as response entities).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Add media elements to SMIL successfull."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Tracks are not valid."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document doesn't contain an element with given parentId."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "Start plus duration is bigger than Track length.")
          })
  public Response addClips(@FormParam("smil") String smil, @FormParam("parentId") String parentId, @FormParam("tracks") String tracks,
          @FormParam("start") long start, @FormParam("duration") long duration) {
    SmilResponse smilResponse = null;
    List<Track> tracksList = null;
    try {
      smilResponse = smilService.fromXml(smil);
      tracksList = (List<Track>) MediaPackageElementParser.getArrayFromXml(tracks);
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Tracks are not valid.").build();
    }

    Track[] tracksArr = tracksList.toArray(new Track[tracksList.size()]);
    try {
      smilResponse = smilService.addClips(smilResponse.getSmil(), parentId, tracksArr, start, duration);
      return Response.ok(smilResponse).build();
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document doesn't contain an element with given parentId.").build();
    }
  }

  @POST
  @Path("addMeta")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "addMeta", description = "Add a meta element to SMIL head.",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document where to add an meta element.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "name", description = "Value of meta name attribute.",
                    isRequired = true, type = RestParameter.Type.STRING),
            @RestParameter(name = "content", description = "Value of meta content attribute.",
                    isRequired = true, type = RestParameter.Type.STRING)},
          returnDescription = "Returns SmilResponse with a new meta element inside "
          + "(the new meta will be returned as response entity).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Add par to SMIL successfull."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid.")
          })
  public Response addMeta(@FormParam("smil") String smil, @FormParam("name") String metaName, @FormParam("content") String metaContent) {
    SmilResponse smilResponse = null;
    try {
      smilResponse = smilService.fromXml(smil);
    } catch (SmilException ex) {
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    }
    smilResponse = smilService.addMeta(smilResponse.getSmil(), metaName, metaContent);
    return Response.ok(smilResponse).build();
  }

  @POST
  @Path("remove")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @RestQuery(name = "remove", description = "Remove an element with given Id from SMIL.",
          restParameters = {
            @RestParameter(name = "smil", description = "SMIL document.",
                    isRequired = true, type = RestParameter.Type.TEXT),
            @RestParameter(name = "elementId", description = "Id of element to remove.",
                    isRequired = true, type = RestParameter.Type.STRING)},
          returnDescription = "Returns SMIL document without an element with given Id "
          + "(if SMIL document contains an element with given Id, this will be returned as entity).",
          reponses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Removing element from SMIL successfull."),
            @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "SMIL document not valid.")
          })
  public Response removeSmilElement(@FormParam("smil") String smil, @FormParam("elementId") String elementId) {
    SmilResponse smilResponse = null;
    try {
      smilResponse = smilService.fromXml(smil);
      smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), elementId);
      return Response.ok(smilResponse).build();
    } catch (SmilException ex) {
      logger.info(ex.getMessage(), ex);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("SMIL document invalid.").build();
    }
  }

  /**
   * Set {@link SmilService}.
   *
   * @param smilService {@link SmilService} to set
   */
  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }
}
