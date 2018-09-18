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
package org.opencastproject.assetmanager.impl.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;
import static org.opencastproject.systems.OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE;
import static org.opencastproject.util.MimeTypeUtil.Fns.suffix;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.forbidden;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.impl.TieredStorageAssetManager;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.MimeTypeUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A base REST endpoint for the {@link AssetManager}.
 * <p>
 * The endpoint provides assets over http (see {@link org.opencastproject.assetmanager.impl.HttpAssetProvider}).
 * <p>
 * No @Path annotation here since this class cannot be created by JAX-RS. Put it on the concrete implementations.
 */
@RestService(name = "assetManager", title = "AssetManager",
    notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
            + "You should file an error report with your server logs from the time when the error occurred: "
            + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>"
    },
    abstractText = "This service indexes and queries available (distributed) episodes.")
public abstract class AbstractAssetManagerRestEndpoint extends AbstractJobProducerEndpoint {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractAssetManagerRestEndpoint.class);

  private final Gson gson = new Gson();

  private final java.lang.reflect.Type stringMapType = new TypeToken<Map<String, String>>() { }.getType();

  public abstract TieredStorageAssetManager getAssetManager();

  /**
   * @deprecated use {@link #snapshot} instead
   */
  @POST
  @Path("add")
  @RestQuery(name = "add", description = "Adds a media package to the asset manager. This method is deprecated in favor of method POST 'snapshot'.",
      restParameters = {
          @RestParameter(
              name = "mediapackage",
              isRequired = true,
              type = Type.TEXT,
              defaultValue = "${this.sampleMediaPackage}",
              description = "The media package to add to the search index.")},
      reponses = {
          @RestResponse(
              description = "The media package was added, no content to return.",
              responseCode = SC_NO_CONTENT),
          @RestResponse(
              description = "Not allowed to add a media package.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(
              description = "There has been an internal error and the media package could not be added",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "No content is returned.")
  @Deprecated
  public Response add(@FormParam("mediapackage") final MediaPackageImpl mediaPackage) {
    return snapshot(mediaPackage);
  }

  @POST
  @Path("snapshot")
  @RestQuery(name = "snapshot", description = "Take a versioned snapshot of a media package.",
      restParameters = {
          @RestParameter(
              name = "mediapackage",
              isRequired = true,
              type = Type.TEXT,
              defaultValue = "${this.sampleMediaPackage}",
              description = "The media package to take a snapshot from.")},
      reponses = {
          @RestResponse(
              description = "A snapshot of the media package has been taken, no content to return.",
              responseCode = SC_NO_CONTENT),
          @RestResponse(
              description = "Not allowed to take a snapshot.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(
              description = "There has been an internal error and no snapshot could be taken.",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "No content is returned.")
  public Response snapshot(@FormParam("mediapackage") final MediaPackageImpl mediaPackage) {
    try {
      getAssetManager().takeSnapshot(DEFAULT_OWNER, mediaPackage);
      return noContent();
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @DELETE
  @Path("delete/{id}")
  @RestQuery(name = "deleteSnapshots",
      description = "Removes snapshots of an episode, owned by the default owner from the asset manager.",
      pathParameters = {
          @RestParameter(
              name = "id",
              isRequired = true,
              type = Type.STRING,
              description = "The media package ID of the episode whose snapshots shall be removed"
                  + " from the asset manager.")},
      reponses = {
          @RestResponse(
              description = "Snapshots have been removed, no content to return.",
              responseCode = SC_NO_CONTENT),
          @RestResponse(
              description = "The episode does either not exist or no snapshots are owned by the default owner.",
              responseCode = SC_NOT_FOUND),
          @RestResponse(
              description = "Not allowed to delete this episode.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(
              description = "There has been an internal error and the episode could not be deleted.",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "No content is returned.")
  public Response delete(@PathParam("id") final String mediaPackageId) {
    if (StringUtils.isEmpty(mediaPackageId)) {
      return notFound();
    }
    try {
      final AQueryBuilder q = getAssetManager().createQuery();
      if (q.delete(AssetManager.DEFAULT_OWNER, q.snapshot()).where(q.mediaPackageId(mediaPackageId)).run() > 0) {
        return noContent();
      }
      return notFound();
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("episode/{mediaPackageID}")
  @RestQuery(name = "getLatestEpisode",
      description = "Get the media package from the last snapshot of an episode.",
      returnDescription = "The media package",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageID",
              description = "the media package ID",
              isRequired = true,
              type = STRING)
      },
      reponses = {
          @RestResponse(responseCode = SC_OK, description = "Media package returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found"),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not allowed to read media package."),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "There has been an internal error.")
      })
  public Response getMediaPackage(@PathParam("mediaPackageID") final String mediaPackageId) {
    try {
      final AQueryBuilder q = getAssetManager().createQuery();
      final AResult r = q.select(q.snapshot())
              .where(q.mediaPackageId(mediaPackageId).and(q.version().isLatest()))
              .run();
      if (r.getSize() == 1) {
        return ok(r.getRecords().head2().getSnapshot().get().getMediaPackage());
      } else if (r.getSize() == 0) {
        return notFound();
      }
      return serverError();
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @GET
  @Path("assets/{mediaPackageID}/{mediaPackageElementID}/{version}/{filenameIgnore}")
  @RestQuery(name = "getAsset",
      description = "Get an asset",
      returnDescription = "The file",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageID",
              description = "the media package identifier",
              isRequired = true,
              type = STRING),
          @RestParameter(
              name = "mediaPackageElementID",
              description = "the media package element identifier",
              isRequired = true,
              type = STRING),
          @RestParameter(
              name = "version",
              description = "the media package version",
              isRequired = true,
              type = STRING),
          @RestParameter(
              name = "filenameIgnore",
              description = "a descriptive filename which will be ignored though",
              isRequired = false,
              type = STRING)},
      reponses = {
          @RestResponse(
              responseCode = SC_OK,
              description = "File returned"),
          @RestResponse(
              responseCode = SC_NOT_FOUND,
              description = "Not found"),
          @RestResponse(
              description = "Not allowed to read assets of this snapshot.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(
              description = "There has been an internal error.",
              responseCode = SC_INTERNAL_SERVER_ERROR)})
  public Response getAsset(@PathParam("mediaPackageID") final String mediaPackageID,
                           @PathParam("mediaPackageElementID") final String mediaPackageElementID,
                           @PathParam("version") final String version) {
    try {
      for (final Version v : getAssetManager().toVersion(version)) {
        for (Asset asset : getAssetManager().getAsset(v, mediaPackageID, mediaPackageElementID)) {
          final String fileName = mediaPackageElementID
                  .concat(".")
                  .concat(asset.getMimeType().bind(suffix).getOr("unknown"));
          asset.getMimeType().map(MimeTypeUtil.Fns.toString);
          // Write the file contents back
          Option<Long> length = asset.getSize() > 0 ? Option.some(asset.getSize()) : Option.none();
          return ok(asset.getInputStream(),
                  Option.fromOpt(asset.getMimeType().map(MimeTypeUtil.Fns.toString)),
                  length,
                  Option.some(fileName));
        }
        // none
        return notFound();
      }
      // cannot parse version
      return badRequest("malformed version");
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{mediaPackageID}/properties.json")
  @RestQuery(name = "getProperties",
          description = "Get stored properties for an episode.",
          returnDescription = "Properties as JSON",
          pathParameters = {
                  @RestParameter(
                          name = "mediaPackageID",
                          description = "the media package ID",
                          isRequired = true,
                          type = STRING)
          }, restParameters = {
                  @RestParameter(
                          name = "namespace",
                          description = "property namespace",
                          isRequired = false,
                          type = STRING)
          },
          reponses = {
                  @RestResponse(responseCode = SC_OK, description = "Media package returned"),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found"),
                  @RestResponse(responseCode = SC_FORBIDDEN, description = "Not allowed to read media package."),
                  @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "There has been an internal error.")
          })
  public Response getProperties(@PathParam("mediaPackageID") final String mediaPackageId,
          @FormParam("namespace") final String namespace) {
    try {
      final AQueryBuilder queryBuilder = getAssetManager().createQuery();
      ASelectQuery query;
      if (StringUtils.isEmpty(namespace)) {
        query = queryBuilder.select(queryBuilder.properties());
      } else {
        query = queryBuilder.select(queryBuilder.propertiesOf(namespace));
      }
      query = query.where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest()));
      final AResult result = query.run();

      // we expect exactly one result when specifying a media package id
      if (result.getSize() < 1) {
        return notFound();
      } else if (result.getSize() > 1) {
        return serverError();
      }

      // build map from properties
      HashMap<String, HashMap<String, String>> properties = new HashMap<>();
      if (result.getRecords().head().isSome()) {
        for (final Property property : result.getRecords().head().get().getProperties()) {
          final String key = property.getId().getNamespace() + "." + property.getId().getName();
          final HashMap<String, String> val = new HashMap<>();
          val.put("type", property.getValue().getType().getClass().getSimpleName());
          val.put("value", property.getValue().get().toString());
          properties.put(key, val);
        }
      }
      return ok(gson.toJson(properties));
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{mediaPackageID}/workflowProperties.json")
  @RestQuery(name = "getWorkflowProperties",
          description = "Get stored workflow properties for an episode.",
          returnDescription = "Properties as JSON",
          pathParameters = {
                  @RestParameter(
                          name = "mediaPackageID",
                          description = "the media package ID",
                          isRequired = true,
                          type = STRING)
          },
          reponses = {
                  @RestResponse(responseCode = SC_OK, description = "Media package returned"),
                  @RestResponse(responseCode = SC_OK, description = "Invalid parameters"),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "Not found"),
                  @RestResponse(responseCode = SC_FORBIDDEN, description = "Not allowed to read media package."),
                  @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "There has been an internal error.")
          })
  public Response getWorkflowProperties(@PathParam("mediaPackageID") final String mediaPackageId) {
    try {
      final AQueryBuilder queryBuilder = getAssetManager().createQuery();
      final AResult result = queryBuilder.select(queryBuilder.propertiesOf(WORKFLOW_PROPERTIES_NAMESPACE))
              .where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest())).run();

      // we expect exactly one result when specifying a media package id
      if (result.getSize() < 1) {
        return notFound();
      } else if (result.getSize() > 1) {
        return serverError();
      }

      // build map from properties
      HashMap<String, String> properties = new HashMap<>();
      if (result.getRecords().head().isSome()) {
        for (final Property property : result.getRecords().head().get().getProperties()) {
          properties.put(property.getId().getName(), property.getValue().get(Value.STRING));
        }
      }
      return ok(gson.toJson(properties));
    } catch (Exception e) {
      return handleException(e);
    }
  }


  @POST
  @Path("{mediaPackageID}/workflowProperties")
  @RestQuery(name = "setWorkflowProperties",
          description = "Set additional workflow properties",
          pathParameters = {
                  @RestParameter(
                          name = "mediaPackageID",
                          description = "the media package ID",
                          isRequired = true,
                          type = STRING)
          },
          restParameters = {
                  @RestParameter(
                          name = "properties",
                          isRequired = true,
                          type = STRING,
                          description = "JSON object containing new properties")
          },
          reponses = {
                  @RestResponse(description = "Properties successfully set", responseCode = SC_CREATED),
                  @RestResponse(description = "Invalid data", responseCode = SC_BAD_REQUEST),
                  @RestResponse(description = "Internal error", responseCode = SC_INTERNAL_SERVER_ERROR) },
          returnDescription = "Returned status code indicates success")
  public Response setWorkflowProperties(@PathParam("mediaPackageID") final String mediaPackageId,
          @FormParam("properties") final String propertiesJSON) {
    Map<String, String> properties;
    try {
      properties = gson.fromJson(propertiesJSON, stringMapType);
    } catch (Exception e) {
      return badRequest();
    }
    for (final Map.Entry<String, String> entry : properties.entrySet()) {
      final PropertyId propertyId = PropertyId.mk(mediaPackageId, WORKFLOW_PROPERTIES_NAMESPACE, entry.getKey());
      final Property property = Property.mk(propertyId, Value.mk(entry.getValue()));
      if (!getAssetManager().setProperty(property)) {
        return notFound();
      }
    }
    return noContent();
  }

  /** Unify exception handling. */
  public static Response handleException(Exception e) {
    logger.debug("Error calling REST method", e);
    Throwable cause = e;
    if (e.getCause() != null) {
      cause = e.getCause();
    }
    if (cause instanceof UnauthorizedException) {
      return forbidden();
    }

    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
  }
}
