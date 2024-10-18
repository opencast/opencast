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

package org.opencastproject.maintenance.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.maintenance.api.MaintenanceService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link MaintenanceService} service
 */
@Component(property = { "service.description=Maintenance REST Endpoint",
    "opencast.service.type=org.opencastproject.maintenance", "opencast.service.path=/maintenance",
    "opencast.service.jobproducer=false" }, immediate = true, service = MaintenanceRestService.class)
@Path("/")
@RestService(name = "MaintenanceServiceEndpoint", title = "Maintenance Service Endpoint", abstractText = "Provides maintenance related operations.", notes = {
    "" })
public class MaintenanceRestService {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceRestService.class);

  private static final DateTimeFormatter maintenanceDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  /** The rest docs */
  protected String docs;

  /** The service */
  protected MaintenanceService maintenanceService;

  @POST
  @Path("maintenance")
  @RestQuery(name = "scheduleMaintenance",
      description = "Sets a time span when opencast goes into maintenance mode.",
      returnDescription = "Status", restParameters = {

      @RestParameter(name = "activateMaintenance", type = BOOLEAN, isRequired = true,
          description = "Activate maintenance mode for all servers"),

      @RestParameter(name = "activateReadOnly", type = BOOLEAN, isRequired = true,
          description = "Activate read-only mode"),

      @RestParameter(name = "startDate", type = STRING, isRequired = true,
          description = "Start of the maintenance"),

      @RestParameter(name = "endDate", type = STRING, isRequired = true,
          description = "End of the maintenance")
  },
      responses = {
      @RestResponse(responseCode = SC_OK, description = "Maintenance was scheduled."),
      @RestResponse(responseCode = SC_BAD_REQUEST, description = "Couldn't schedule maintenance.")
  })
  public Response scheduleMaintenance(
      @FormParam("activateMaintenance") boolean activateMaintenance,
      @FormParam("activateReadOnly") boolean activateReadOnly,
      @FormParam("startDate") String startDate,
      @FormParam("endDate") String endDate) {

    logger.info("Scheduling maintenance.");

    try {
      if (!activateMaintenance && !activateReadOnly) {
        throw new IllegalArgumentException("Neither maintenance nor read-only mode activated.");
      }
      LocalDateTime parsedStartDate = LocalDateTime.parse(startDate, maintenanceDateFormatter);
      LocalDateTime parsedEndDate = LocalDateTime.parse(endDate, maintenanceDateFormatter);
      if (parsedEndDate.isBefore(parsedStartDate)) {
        throw new IllegalArgumentException("End date must be after start date.");
      }
      maintenanceService.scheduleMaintenance(activateMaintenance, activateReadOnly, parsedStartDate, parsedEndDate);
    } catch (IllegalArgumentException e) {
      logger.error("Error scheduling maintenance.", e);
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (DateTimeParseException e) {
      logger.error("Error parsing dates. Expected format: {}", maintenanceDateFormatter, e);
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid date format.").build();
    } catch (Exception e) {
      logger.error("Error while scheduling maintenance.", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  @Reference
  public void setMaintenanceService(MaintenanceService service) {
    this.maintenanceService = service;
  }
}
