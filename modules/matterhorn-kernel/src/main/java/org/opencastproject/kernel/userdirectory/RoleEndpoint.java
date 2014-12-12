/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.userdirectory;

import org.opencastproject.security.api.JaxbRoleList;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides a sorted set of known roles
 */
@Path("/")
@RestService(name = "roles", title = "User Roles", notes = { "" }, abstractText = "Displays the roles available in "
        + "the current user's organization")
public class RoleEndpoint {

  /** The role directory service */
  protected RoleDirectoryService roleDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  @GET
  @Path("roles.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "rolesasxml", description = "Lists the roles as XML", returnDescription = "The list of roles as XML", reponses = { @RestResponse(responseCode = 200, description = "OK, roles returned") })
  public JaxbRoleList getRolesAsXml() {
    JaxbRoleList roleList = new JaxbRoleList();
    for (Iterator<Role> i = roleDirectoryService.getRoles(); i.hasNext();) {
      roleList.add(i.next());
    }
    return roleList;
  }

  @GET
  @Path("roles.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "rolesasjson", description = "Lists the roles as JSON", returnDescription = "The list of roles as JSON", reponses = { @RestResponse(responseCode = 200, description = "OK, roles returned") })
  public JaxbRoleList getRolesAsJson() {
    return getRolesAsXml();
  }

  /**
   * Sets the role directory service
   *
   * @param roleDirectoryService
   *          the roleDirectoryService to set
   */
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

}
