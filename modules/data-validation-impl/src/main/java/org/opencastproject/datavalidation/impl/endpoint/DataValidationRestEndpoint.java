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

package org.opencastproject.datavalidation.impl.endpoint;

import org.opencastproject.datavalidation.api.DataValidationService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link DataValidationService} service
 */
@Component(
    property = {
        "service.description=Data Validation REST Endpoint",
        "opencast.service.type=org.opencastproject.datavalidation",
        "opencast.service.path=/data-validation",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = DataValidationRestEndpoint.class
)
@Path("/data-validation")
@RestService(
    name = "DataValidationRestEndpoint",
    title = "Data Validation Service Endpoint",
    abstractText = "This service validates data.",
    notes = {
        // Auf jeden Fall überarbeiten!
        "This service provides endpoints to validate data."
    }
)

@JaxrsResource
public class DataValidationRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DataValidationRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected DataValidationService dataValidationService;

  /**
   * Simple example service call
   *
   * @return The Snapshot Validate statement
   * @throws Exception
   */
  @GET
  @Path("validateallassets")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "validateAllAssets",
      description = "Checks all assets for corrupted data",
      restParameters = {
        @RestParameter(
          name = "offset",
          description = "The offset to query the media packages with",
          isRequired = false,
          type = RestParameter.Type.INTEGER,
          defaultValue = "0"
        ),
        @RestParameter(
          name = "limit",
          description = "The limit to query the media packages with",
          isRequired = false,
          type = RestParameter.Type.INTEGER,
          defaultValue = "0"
        )
      },
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "Report on all assets checked for corrupted data"
          ),
          @RestResponse(
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              description = "The underlying service could not output something."
          )
      },
      returnDescription = "Report on all assets checked for corrupted data."
  )
  public Response checkAssetsForCorruptedData(@FormParam("offset") int offset, @FormParam("limit") int limit)
          throws Exception {
    logger.info("REST call for DataValidationService to check all assets for corrupted data");
    return Response.ok().entity(dataValidationService.checkAssetsForCorruptedData(offset, limit)).build();
  }

  /**
   * Outputs a detailed report on asset specified by UID
   *
   * @param uid
   *          the unique identifier of the asset
   * @return String with the report
   */
  @GET
  @Path("checkaclmatching")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
      name = "checkAclMatching",
      description = "Iterates through archived and published media packages and checks if ACLs match",
      restParameters = {
        @RestParameter(
          name = "offset",
          description = "The offset to query the media packages with",
          isRequired = false,
          type = RestParameter.Type.INTEGER,
          defaultValue = "0"
        ),
        @RestParameter(
          name = "limit",
          description = "The limit to query the media packages with",
          isRequired = false,
          type = RestParameter.Type.INTEGER,
          defaultValue = "0"
        )
      },
      responses = {
        @RestResponse(
          responseCode = HttpServletResponse.SC_OK,
          description = "Detailed report on the asset"
        ),
        @RestResponse(
          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          description = "The underlying service could not output something."
        )
      },
      returnDescription = "The detailed report on the asset."
  )
  public Response checkAclMatching(@FormParam("offset") int offset, @FormParam("limit") int limit) throws Exception {
    logger.info("REST call for DataValidationService to check single asset with offset: {} and limit: {}",
        offset, limit);
    return Response.ok().entity(dataValidationService.checkAclMatching(offset, limit)).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  @Reference
  public void setDataValidationService(DataValidationService dataValidationService) {
    this.dataValidationService = dataValidationService;
  }
}
