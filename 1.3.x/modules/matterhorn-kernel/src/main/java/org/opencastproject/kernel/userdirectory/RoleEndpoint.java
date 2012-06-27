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

import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONArray;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

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

  @GET
  @Path("list.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "roles", description = "Lists the roles as a json array", returnDescription = "The list of roles as a json array", reponses = { @RestResponse(responseCode = 200, description = "OK, roles returned") })
  @SuppressWarnings("unchecked")
  public String getRoles() {
    SortedSet<String> knownRoles = new TreeSet<String>();
    knownRoles.addAll(Arrays.asList(roleDirectoryService.getRoles()));
    JSONArray json = new JSONArray();
    json.addAll(knownRoles);
    return json.toJSONString();
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
