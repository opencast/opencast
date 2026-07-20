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
package org.opencastproject.basicstatisticssecret.endpoint;

import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretServiceException;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * For internal communication between nodes
 */

@Component(
    property = {
        "service.description=Basic Statistics Internal REST Endpoint",
        "opencast.service.type=org.opencastproject.basicstatisticssecret",
        "opencast.service.path=/basicstatistics-internal",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = BasicStatisticsSecretRestEndpoint.class
)
@Path("/basicstatistics-secret")
@RestService(
    name = "BasicStatisticsInternalEndpoint",
    title = "Basic Statistics Internal Endpoint",
    abstractText = "For internal communication between nodes for Opencasts basic statistics",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the "
            + "underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
@JaxrsResource
public class BasicStatisticsSecretRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsSecretRestEndpoint.class);

  /** The service */
  protected BasicStatisticsSecretService basicStatisticsSecretService;

  @GET
  @Path("daily-secret")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getDailySecret() throws BasicStatisticsSecretServiceException {
    return Response.ok(Base64.getEncoder().encodeToString(basicStatisticsSecretService.getCurrentSecret()))
      .build();
  }

  @Reference
  public void setBasicStatisticsSecretService(BasicStatisticsSecretService service) {
    this.basicStatisticsSecretService = service;
  }
}
