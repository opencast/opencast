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

package org.opencastproject.assetmanager.aws.s3.endpoint;

import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.aws.s3.AwsS3AssetStore;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.amazonaws.services.s3.model.StorageClass;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/assets/aws/s3")
@RestService(name = "archive-aws-s3", title = "AWS S3 Archive",
    notes = {
        "All paths are relative to the REST endpoint base (something like http://your.server/files)",
        "If you notice that this service is not working as expected, there might be a bug! "
            + "You should file an error report with your server logs from the time when the error occurred: "
            + "<a href=\"http://opencast.jira.com\">Opencast Issue Tracker</a>"
    },
    abstractText = "This service handles AWS S3 archived assets")
@Component(
    immediate = true,
    service = AwsS3RestEndpoint.class,
    property = {
        "service.description=AssetManager S3 REST Endpoint",
        "opencast.service.type=org.opencastproject.assetmanager.aws-s3",
        "opencast.service.path=/assets/aws/s3",
    }
)
public class AwsS3RestEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(AwsS3RestEndpoint.class);

  private AwsS3AssetStore awsS3AssetStore = null;
  private AssetManager assetManager = null;
  private SecurityService securityService = null;

  @GET
  @Path("{mediaPackageId}/assets/storageClass")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "getStorageClass",
      description = "Get the S3 Storage Class for each asset in the Media Package",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageId", isRequired = true,
              type = RestParameter.Type.STRING,
              description = "The media package indentifier.")},
      responses = {
          @RestResponse(
              description = "mediapackage found in S3",
              responseCode = HttpServletResponse.SC_OK),
          @RestResponse(
              description = "mediapackage not found or has no assets in S3",
              responseCode = HttpServletResponse.SC_NOT_FOUND)
      },
      returnDescription = "List each assets's Object Key and S3 Storage Class")
  public Response getStorageClass(@PathParam("mediaPackageId") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      @Override public Response apply() {
        AQueryBuilder q = assetManager.createQuery();
        final ASelectQuery idQuery = q.select(q.snapshot())
            .where(
                q.organizationId(securityService.getOrganization().getId())
                    .and(q.mediaPackageId(getMediaPackageId()))
                    .and(q.version().isLatest()));
        final AResult result = idQuery.run();
        if (result.getSize() > 1) {
          return serverError();
        }
        if (result.getSize() == 0) {
          return notFound();
        }
        final ARecord item = result.getRecords().stream().findFirst().get();

        StringBuilder info = new StringBuilder();
        for (MediaPackageElement e : assetManager.getMediaPackage(item.getMediaPackageId()).get().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(),
              getMediaPackageId(),
              item.getSnapshot().get().getVersion(),
              e.getIdentifier());
          if (awsS3AssetStore.contains(storagePath)) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath),
                                                   awsS3AssetStore.getAssetStorageClass(storagePath)));
            } catch (AssetStoreException ex) {
              throw new AssetManagerException(ex);
            }
          } else {
            info.append(String.format("%s,NONE\n", e.getURI()));
          }
        }
        return ok(info.toString());
      }

    });
  }

  @PUT
  @Path("{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "modifyStorageClass",
      description = "Move the Media Package assets to the specified S3 Storage Class if possible",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageId",
              isRequired = true,
              type = RestParameter.Type.STRING,
              description = "The media package indentifier.")
      },
      restParameters = {
          @RestParameter(
              name = "storageClass",
              isRequired = true,
              type = RestParameter.Type.STRING,
              description = "The S3 storage class, valid terms STANDARD, STANDARD_IA, INTELLIGENT_TIERING, ONEZONE_IA,"
                          + "GLACIER_IR, GLACIER, and DEEP_ARCHIVE. See https://aws.amazon.com/s3/storage-classes/")
      },
      responses = {
          @RestResponse(
              description = "mediapackage found in S3",
              responseCode = HttpServletResponse.SC_OK),
          @RestResponse(
              description = "mediapackage not found or has no assets in S3",
              responseCode = HttpServletResponse.SC_NOT_FOUND)      },
      returnDescription = "List each asset's Object Key and new S3 Storage Class")
  public Response modifyStorageClass(@PathParam("mediaPackageId") final String mediaPackageId,
                                     @FormParam("storageClass") final String storageClass) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      private String getStorageClass() {
        return StringUtils.trimToNull(storageClass);
      }

      @Override public Response apply() {
        AQueryBuilder q = assetManager.createQuery();
        final ASelectQuery idQuery = q.select(q.snapshot())
            .where(
                q.organizationId(securityService.getOrganization().getId())
                    .and(q.mediaPackageId(getMediaPackageId()))
                    .and(q.version().isLatest()));
        final AResult result = idQuery.run();
        if (result.getSize() > 1) {
          return serverError();
        }
        if (result.getSize() == 0) {
          return notFound();
        }
        final ARecord item = result.getRecords().stream().findFirst().get();

        StringBuilder info = new StringBuilder();
        for (MediaPackageElement e : assetManager.getMediaPackage(item.getMediaPackageId()).get().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(),
              getMediaPackageId(),
              item.getSnapshot().get().getVersion(),
              e.getIdentifier());
          if (awsS3AssetStore.contains(storagePath)) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath),
                                                   awsS3AssetStore.modifyAssetStorageClass(storagePath,
                                                   getStorageClass())));
            } catch (AssetStoreException ex) {
              throw new AssetManagerException(ex);
            }
          } else {
            info.append(String.format("%s,NONE\n", e.getURI()));
          }
        }
        return ok(info.toString());
      }

    });
  }

  @GET
  @Path("glacier/{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "restoreAssetsStatus",
      description = "Get the mediapackage asset's restored status",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageId",
              isRequired = true,
              type = RestParameter.Type.STRING,
              description = "The media package indentifier.")
      },
      responses = {
          @RestResponse(
              description = "mediapackage found in S3 and assets in Glacier",
              responseCode = HttpServletResponse.SC_OK),
          @RestResponse(
              description = "mediapackage found in S3 but no assets in Glacier",
              responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(
              description = "mediapackage not found or has no assets in S3",
              responseCode = HttpServletResponse.SC_NOT_FOUND)
      },
      returnDescription = "List each glacier asset's restoration status and expiration date")
  public Response getAssetRestoreState(@PathParam("mediaPackageId") final String mediaPackageId) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      @Override public Response apply() {
        AQueryBuilder q = assetManager.createQuery();
        final ASelectQuery idQuery = q.select(q.snapshot())
            .where(
                q.organizationId(securityService.getOrganization().getId())
                    .and(q.mediaPackageId(getMediaPackageId()))
                    .and(q.version().isLatest()));
        final AResult result = idQuery.run();
        if (result.getSize() > 1) {
          return serverError();
        }
        if (result.getSize() == 0) {
          return notFound();
        }
        final ARecord item = result.getRecords().stream().findFirst().get();

        StringBuilder info = new StringBuilder();
        for (MediaPackageElement e : assetManager.getMediaPackage(item.getMediaPackageId()).get().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(),
                                                    getMediaPackageId(),
                                                    item.getSnapshot().get().getVersion(),
                                                    e.getIdentifier());
          if (isFrozen(storagePath)) {
            try {
              info.append(String.format("%s,%s\n", awsS3AssetStore.getAssetObjectKey(storagePath),
                                                   awsS3AssetStore.getAssetRestoreStatusString(storagePath)));
            } catch (AssetStoreException ex) {
              throw new AssetManagerException(ex);
            }
          } else {
            info.append(String.format("%s,NONE\n", storagePath));
          }
        }
        if (info.length() == 0) {
          return noContent();
        }
        return ok(info.toString());
      }
    });
  }

  @PUT
  @Path("glacier/{mediaPackageId}/assets")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "restoreAssets",
      description = "Initiate the restore of any assets in Glacier storage class",
      pathParameters = {
          @RestParameter(
              name = "mediaPackageId",
              isRequired = true,
              type = RestParameter.Type.STRING,
              description = "The media package indentifier.")
      },
      restParameters = {
          @RestParameter(
              name = "restorePeriod",
              isRequired = false,
              type = RestParameter.Type.INTEGER,
              defaultValue = "2",
              description = "Number of days to restore the assets for, default see service configuration")
      },
      responses = {
          @RestResponse(
              description = "restore of assets started",
              responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(
              description = "invalid restore period, must be greater than zero",
              responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(
              description = "mediapackage not found or has no assets in S3",
              responseCode = HttpServletResponse.SC_NOT_FOUND)
      },
      returnDescription = "Restore of assets initiated")
  public Response restoreAssets(@PathParam("mediaPackageId") final String mediaPackageId,
                                @FormParam("restorePeriod") final Integer restorePeriod) {
    return handleException(new Function0<Response>() {
      private String getMediaPackageId() {
        return StringUtils.trimToNull(mediaPackageId);
      }

      private Integer getRestorePeriod() {
        return restorePeriod != null ? restorePeriod : awsS3AssetStore.getRestorePeriod();
      }

      @Override public Response apply() {
        Integer restorePeriod = getRestorePeriod();
        if (restorePeriod < 1) {
          throw new BadRequestException("Restore period must be greater than zero!");
        }

        AQueryBuilder q = assetManager.createQuery();
        final ASelectQuery idQuery = q.select(q.snapshot())
            .where(
                q.organizationId(securityService.getOrganization().getId())
                    .and(q.mediaPackageId(getMediaPackageId()))
                    .and(q.version().isLatest()));
        final AResult result = idQuery.run();
        if (result.getSize() > 1) {
          return serverError();
        }
        if (result.getSize() == 0) {
          return notFound();
        }
        final ARecord item = result.getRecords().stream().findFirst().get();


        for (MediaPackageElement e : assetManager.getMediaPackage(item.getMediaPackageId()).get().elements()) {
          if (e.getElementType() == MediaPackageElement.Type.Publication) {
            continue;
          }

          StoragePath storagePath = new StoragePath(securityService.getOrganization().getId(),
                                                    getMediaPackageId(),
                                                    item.getSnapshot().get().getVersion(),
                                                    e.getIdentifier());
          if (isFrozen(storagePath)) {
            try {
              // Initiate restore and return
              awsS3AssetStore.initiateRestoreAsset(storagePath, getRestorePeriod());
            } catch (AssetStoreException ex) {
              throw new AssetManagerException(ex);
            }
          }
        }
        return noContent();
      }
    });
  }

  private boolean isFrozen(StoragePath storagePath) {
    String assetStorageClass = awsS3AssetStore.getAssetStorageClass(storagePath);
    return awsS3AssetStore.contains(storagePath)
        && (StorageClass.Glacier == StorageClass.fromValue(assetStorageClass)
          || StorageClass.DeepArchive == StorageClass.fromValue(assetStorageClass));
  }


  /** Unify exception handling. */
  public static <A> A handleException(final Function0<A> f) {
    try {
      return f.apply();
    } catch (AssetManagerException e) {
      if (e.isCauseNotAuthorized()) {
        throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
      }
      if (e.isCauseNotFound()) {
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      }
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      logger.error("Error calling archive REST method", e);
      if (e instanceof NotFoundException) {
        throw new WebApplicationException(e, Response.Status.NOT_FOUND);
      }
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Reference()
  void setAwsS3AssetStore(AwsS3AssetStore store) {
    awsS3AssetStore = store;
  }

  @Reference()
  void setAssetManager(AssetManager service) {
    assetManager = service;
  }

  @Reference()
  void setSecurityService(SecurityService service) {
    securityService = service;
  }
}
