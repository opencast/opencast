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

package org.opencastproject.adopterstatistics.registration;

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the adopter statistics service
 */
@Path("/")
@RestService(name = "registrationController",
        title = "Adopter Statistics Registration Service Endpoint",
        abstractText = "Rest Endpoint for the registration form.",
        notes = {"Provides operations regarding the adopter registration form"})
public class Controller {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Controller.class);

  /** The rest docs */
  protected String docs;

  /** The service that provides methods for the registration */
  protected Service registrationService;

  public void setRegistrationService(Service registrationService) {
    this.registrationService = registrationService;
  }


  private static final Type stringMapType = new TypeToken<Map<String, String>>() { }.getType();
  private static final Gson gson = new Gson();
  private static final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy/MM/dd");


  @GET
  @Path("registration")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getregistrationform", description = "GETs the form data for the current logged in user", reponses = {
          @RestResponse(description = "Successful retrieved form data.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The underlying service could not output something.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "GETs the form data for a specific user.")
  public Response getRegistrationForm() throws Exception {

    logger.info("Retrieving statistics registration form for logged in user");
    return RestUtils.okJson(formToJson(registrationService.retrieveFormData()));
  }

  @POST
  @Path("registration")
  @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "saveregistrationform",
          description = "Saves the adopter statistics registration form",
          returnDescription = "Status", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Theme created"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The theme references a non-existing file") })
  public Response register(String data) {

    Map<String, String> dataMap = gson.fromJson(data, stringMapType);
    Form f = jsonToForm(dataMap);

    try {
      registrationService.saveFormData(f);
    } catch (Exception e) {
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

  /**
   * Constructs a form object from a map
   * @param dataMap Form data as map
   *
   * @return The form object
   */
  private Form jsonToForm(Map<String, String> dataMap) {
    Form f = new Form();
    f.setOrganisationName(dataMap.get("organisationName"));
    f.setDepartmentName(dataMap.get("departmentName"));
    f.setFirstName(dataMap.get("firstName"));
    f.setLastName(dataMap.get("lastName"));
    f.setEmail(dataMap.get("email"));
    f.setCountry(dataMap.get("country"));
    f.setPostalCode(dataMap.get("postalCode"));
    f.setCity(dataMap.get("city"));
    f.setStreet(dataMap.get("street"));
    f.setStreetNo(dataMap.get("streetNo"));
    f.setDateModified(new Date());

    String contactMe = dataMap.get("contactMe");
    if (contactMe != null) {
      f.setContactMe(Boolean.parseBoolean(contactMe));
    }

    String allowStatistics = dataMap.get("allowsStatistics");
    if (allowStatistics != null) {
      f.setAllowsStatistics(Boolean.parseBoolean(allowStatistics));
    }

    String allowErrorReports = dataMap.get("allowsErrorReports");
    if (allowErrorReports != null) {
      f.setAllowsErrorReports(Boolean.parseBoolean(allowErrorReports));
    }

    String allowTechData = dataMap.get("allowsTechData");
    if (allowTechData != null) {
      f.setAllowsTechData(Boolean.parseBoolean(allowTechData));
    }

    return f;
  }

  /**
   * @return The JSON representation of the form.
   */
  private JValue formToJson(IForm iform) {
    Form form = (Form) iform;
    List<Field> fields = new ArrayList<Field>();
    fields.add(f("organisationName", v(form.getOrganisationName(), Jsons.BLANK)));
    fields.add(f("departmentName", v(form.getDepartmentName(), Jsons.BLANK)));
    fields.add(f("firstName", v(form.getFirstName(), Jsons.BLANK)));
    fields.add(f("lastName", v(form.getLastName(), Jsons.BLANK)));
    fields.add(f("email", v(form.getEmail(), Jsons.BLANK)));
    fields.add(f("country", v(form.getCountry(), Jsons.BLANK)));
    fields.add(f("postalCode", v(form.getPostalCode(), Jsons.BLANK)));
    fields.add(f("city", v(form.getCity(), Jsons.BLANK)));
    fields.add(f("street", v(form.getStreet(), Jsons.BLANK)));
    fields.add(f("streetNo", v(form.getStreetNo(), Jsons.BLANK)));
    fields.add(f("contactMe", v(form.isContactMe(), Jsons.FALSE)));
    fields.add(f("allowsStatistics", v(form.isAllowsStatistics(), Jsons.FALSE)));
    fields.add(f("allowsErrorReports", v(form.isAllowsErrorReports(), Jsons.FALSE)));
    fields.add(f("allowsTechData", v(form.isAllowsTechData(), Jsons.FALSE)));
    if (form.getDateModified() != null) {
      String lastModified = jsonDateFormat.format(form.getDateModified());
      fields.add(f("lastModified", v(lastModified, Jsons.BLANK)));
    }
    return obj(fields);
  }

}
