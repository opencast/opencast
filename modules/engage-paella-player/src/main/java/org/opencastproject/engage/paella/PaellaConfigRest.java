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

package org.opencastproject.engage.paella;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
//import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
//import org.opencastproject.security.api.User;
//import org.opencastproject.systems.OpencastConstants;
//import org.opencastproject.userdirectory.UserIdRoleProvider;
//import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;


import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.Constants;
//import org.osgi.framework.InvalidSyntaxException;
//import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
//import java.io.FileNotFoundException;

//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Map.Entry;
//import java.util.SortedSet;
//import java.util.TreeSet;

//import javax.servlet.Servlet;
//import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
//import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * This REST endpoint provides information about the runtime environment, including the services and user interfaces
 * deployed and the current login context.
 *
 * If the 'org.opencastproject.anonymous.feedback.url' is set in config.properties, this service will also update the
 * opencast project with the contents of the getRuntimeInfo() json feed.
 */
@Path("/")
@RestService(
  name = "PaellaConfigRest",
  title = "Paella Config Information",
  abstractText = "This service provides information about the runtime environment, including the services that are "
    + "deployed and the current user context.",
  notes = {})
public class PaellaConfigRest {

  private static final Logger logger = LoggerFactory.getLogger(PaellaConfigRest.class);

  /** Configuration properties id */
  private static final String PAELLA_CONFIG_FOLDER_PROPERTY = "org.opencastproject.engage.paella.config.folder";

  /**
   * The rest publisher looks for any non-servlet with the 'opencast.service.path' property
   */
  public static final String SERVICE_FILTER = "(&(!(objectClass=javax.servlet.Servlet))("
          + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

  private String paellaConfigFolder;
  private SecurityService securityService;

  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
  private UserIdRoleProvider userIdRoleProvider;
  
  private BundleContext bundleContext;

  protected void setUserIdRoleProvider(UserIdRoleProvider userIdRoleProvider) {
    this.userIdRoleProvider = userIdRoleProvider;
  }

  protected ServiceReference[] getRestServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(null, SERVICES_FILTER);
  }

  protected ServiceReference[] getUserInterfaceServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(Servlet.class.getName(), "(&(alias=*)(classpath=*))");
  }
  */
  public void activate(ComponentContext cc) {
    logger.debug("activate()");

    paellaConfigFolder = cc.getBundleContext().getProperty(PAELLA_CONFIG_FOLDER_PROPERTY);
    logger.debug("Paella configuration folder is {}", paellaConfigFolder);
  }

  public void deactivate() {
    // Nothing to do
    logger.debug("deactivate()");
  }

  @GET
  @Path("config.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "me", description = "Information about the curent user", reponses = { @RestResponse(description = "Returns information about the current user", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  @SuppressWarnings("unchecked")
  public String getMyInfo() throws IOException {
    // Add the current user's organizational information
    Organization org = securityService.getOrganization();

    File configFile = new File(PathSupport.concat(new String[] { paellaConfigFolder, org.getId(), "config.json" }));
    String configContent = FileUtils.readFileToString(configFile);


    return configContent;
  }
}
