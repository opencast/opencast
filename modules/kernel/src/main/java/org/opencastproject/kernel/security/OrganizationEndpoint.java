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

package org.opencastproject.kernel.security;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbOrganizationList;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides access to the organizations served by this Opencast instance.
 */
@Component(
  property = {
    "service.description=Organization listing REST endpoint",
    "opencast.service.type=org.opencastproject.organization",
    "opencast.service.path=/org",
    "opencast.service.jobproducer=false"
  },
  immediate = true,
  service = { OrganizationEndpoint.class }
)
@Path("/org")
@RestService(name = "organization", title = "Organizations", notes = { "" }, abstractText = "Displays the organizations served by this system")
@JaxrsResource
public class OrganizationEndpoint {

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectoryService = null;

  @GET
  @Path("all.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "orgsasxml", description = "Lists the organizations as xml", returnDescription = "The list of org as xml", responses = { @RestResponse(responseCode = 200, description = "Organizations returned") })
  public JaxbOrganizationList getOrganizationsAsXml() {
    JaxbOrganizationList organizationList = new JaxbOrganizationList();
    for (Organization org : orgDirectoryService.getOrganizations()) {
      organizationList.add(org);
    }
    return organizationList;
  }

  @GET
  @Path("all.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "orgsasjson", description = "Lists the organizations as a json array", returnDescription = "The list of org as a json array", responses = { @RestResponse(responseCode = 200, description = "Organizations returned") })
  public JaxbOrganizationList getOrganizationsAsJson() {
    return getOrganizationsAsXml();
  }

  @GET
  @Path("{id}.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "orgasxml", description = "Gets an organizations as xml", returnDescription = "The org as xml", pathParameters = { @RestParameter(name = "id", type = Type.STRING, description = "The job identifier", isRequired = true) }, responses = {
          @RestResponse(responseCode = 200, description = "Organization returned"),
          @RestResponse(responseCode = 404, description = "No organization with this identifier found") })
  public JaxbOrganization getOrganizationAsXml(@PathParam("id") String id) {
    try {
      return JaxbOrganization.fromOrganization(orgDirectoryService.getOrganization(id));
    } catch (NotFoundException e) {
      return null;
    }
  }

  @GET
  @Path("{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "orgasjson", description = "Gets an organizations as json", returnDescription = "The org as json", pathParameters = { @RestParameter(name = "id", type = Type.STRING, description = "The job identifier", isRequired = true) }, responses = {
          @RestResponse(responseCode = 200, description = "Organization returned"),
          @RestResponse(responseCode = 404, description = "No organization with this identifier found") })
  public JaxbOrganization getOrganizationAsJson(@PathParam("id") String id) {
    return getOrganizationAsXml(id);
  }

  /**
   * @param orgDirectoryService
   *          the orgDirectoryService to set
   */
  @Reference
  public void setOrgDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }
}
