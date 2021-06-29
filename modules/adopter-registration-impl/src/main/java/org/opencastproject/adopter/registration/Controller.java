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

package org.opencastproject.adopter.registration;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the adopter statistics service.
 */
@Path("/")
@RestService(name = "registrationController",
        title = "Adopter Statistics Registration Service Endpoint",
        abstractText = "Rest Endpoint for the registration form.",
        notes = {"Provides operations regarding the adopter registration form"})
public class Controller {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Controller.class);

  /** The JSON parser */
  private static final Gson gson = new Gson();

  /** The rest docs */
  protected String docs;

  /** The service that provides methods for the registration */
  protected Service registrationService;

  /** OSGi setter for the registration service */
  public void setRegistrationService(Service registrationService) {
    this.registrationService = registrationService;
  }


  @GET
  @Path("registration")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getregistrationform", description = "GETs the adopter registration data.", responses = {
          @RestResponse(description = "Retrieved registration data.",
                        responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Error while retrieving adopter registration data.",
                        responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
                        returnDescription = "GETs the adopter registration data.")
  public String getRegistrationForm() {
    logger.debug("Retrieving adopter registration data.");
    return gson.toJson(registrationService.retrieveFormData());
  }


  @POST
  @Path("registration")
  @RestQuery(name = "saveregistrationform",
          description = "Saves the adopter registration data.",
          returnDescription = "Status",
          restParameters = {
                  @RestParameter(description = "The Name of the organisation.",
                          isRequired = false, name = "organisationName", type = STRING),
                  @RestParameter(description = "The Name of the department.",
                          isRequired = false, name = "departmentName", type = STRING),
                  @RestParameter(description = "The First name.",
                          isRequired = false, name = "firstName", type = STRING),
                  @RestParameter(description = "The Last name.",
                          isRequired = false, name = "lastName", type = STRING),
                  @RestParameter(description = "The e-mail address.",
                          isRequired = false, name = "email", type = STRING),
                  @RestParameter(description = "The country.",
                          isRequired = false, name = "country", type = STRING),
                  @RestParameter(description = "The postal code.",
                          isRequired = false, name = "postalCode", type = STRING),
                  @RestParameter(description = "The city.",
                          isRequired = false, name = "city", type = STRING),
                  @RestParameter(description = "The street.",
                          isRequired = false, name = "street", type = STRING),
                  @RestParameter(description = "The street number.",
                          isRequired = false, name = "streetNo", type = STRING),
                  @RestParameter(description = "Does the adopter allows to be contacted.",
                          isRequired = false, name = "contactMe", type = BOOLEAN),
                  @RestParameter(description = "Does the adopter agreed to the policy.",
                          isRequired = false, name = "agreedToPolicy", type = BOOLEAN),
                  @RestParameter(description = "Does the adopter allow the gathering of error reports.",
                          isRequired = false, name = "allowsErrorReports", type = BOOLEAN),
                  @RestParameter(description = "Does the adopter allow the gathering of statistic data.",
                          isRequired = false, name = "allowsStatistics", type = BOOLEAN),
                  @RestParameter(description = "Is the adopter already registered.",
                          isRequired = false, name = "registered", type = BOOLEAN)
          },
          responses = {
          @RestResponse(responseCode = SC_OK, description = "Adopter registration data saved."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Couldn't save adopter registration data.")})
  public Response register(
          @FormParam("organisationName") String organisationName,
          @FormParam("departmentName") String departmentName,
          @FormParam("firstName") String firstName,
          @FormParam("lastName") String lastName,
          @FormParam("email") String email,
          @FormParam("country") String country,
          @FormParam("postalCode") String postalCode,
          @FormParam("city") String city,
          @FormParam("street") String street,
          @FormParam("streetNo") String streetNo,
          @FormParam("contactMe") boolean contactMe,
          @FormParam("agreedToPolicy") boolean agreedToPolicy,
          @FormParam("allowsErrorReports") boolean allowsErrorReports,
          @FormParam("allowsStatistics") boolean allowsStatistics,
          @FormParam("registered") boolean registered) {
    logger.debug("Saving adopter registration data.");

    Form form = new Form(organisationName, departmentName, firstName, lastName, email, country, postalCode, city,
            street, streetNo, contactMe, agreedToPolicy, allowsErrorReports, allowsStatistics, registered
    );
    try {
      registrationService.saveFormData(form);
    } catch (Exception e) {
      logger.error("Error while saving adopter registration data.", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }


  @DELETE
  @Path("registration")
  @RestQuery(name = "deleteregistrationform", description = "Deletes the adopter registration data", responses = {
          @RestResponse(description = "Successful deleted form data.",
                  responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Error while deleting adopter registration data.",
                  responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
          returnDescription = "DELETEs the adopter registration data.")
  public Response deleteRegistrationData() {
    logger.debug("Deleting adopter registration data.");
    try {
      registrationService.deleteFormData();
      return ok();
    } catch (Exception e) {
      logger.error("Error while deleting adopter registration data.", e);
      return serverError();
    }
  }


  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

}
