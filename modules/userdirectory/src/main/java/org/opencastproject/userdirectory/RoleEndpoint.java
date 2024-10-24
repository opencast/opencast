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

package org.opencastproject.userdirectory;

import org.opencastproject.security.api.JaxbRoleList;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides a sorted set of known roles
 */
@Path("/roles")
@RestService(
    name = "roles",
    title = "User Roles",
    notes = { "" },
    abstractText = "Displays the roles available in the current user's organization"
)
@Component(
    property = {
        "service.description=Role listing REST endpoint",
        "opencast.service.type=org.opencastproject.userdirectory.roles",
        "opencast.service.path=/roles",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = { RoleEndpoint.class }
)
@JaxrsResource
public class RoleEndpoint {

  /** The role directory service */
  protected RoleDirectoryService roleDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * @param organizationDirectory
   *          the organization directory
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  @GET
  @Path("roles.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(
      name = "rolesasxml",
      description = "Lists the roles as XML",
      returnDescription = "The list of roles as XML",
      responses = { @RestResponse(responseCode = 200, description = "OK, roles returned") }
  )
  public JaxbRoleList getRolesAsXml() {
    JaxbRoleList roleList = new JaxbRoleList();
    for (Role role: roleDirectoryService.findRoles("%", Role.Target.ALL, 0, 0)) {
      roleList.add(role);
    }
    return roleList;
  }

  @GET
  @Path("roles.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "rolesasjson",
      description = "Lists the roles as JSON",
      returnDescription = "The list of roles as JSON",
      responses = { @RestResponse(responseCode = 200, description = "OK, roles returned") }
  )
  public JaxbRoleList getRolesAsJson() {
    return getRolesAsXml();
  }

  /**
   * Sets the role directory service
   *
   * @param roleDirectoryService
   *          the roleDirectoryService to set
   */
  @Reference
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

}
