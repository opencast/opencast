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
package org.opencastproject.oaipmh.server;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.RestUtil.R.ok;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Assistant REST endpoint to support a distributed deployment of the {@link OaiPmhServer}. */
public abstract class AbstractOaiPmhServerInfoRestEndpoint {
  public abstract OaiPmhServerInfo getOaiPmhServerInfo();

  @GET
  @Path("/hasrepo/{repoId}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "hasRepo",
             description = "Ask if the OAI-PMH server knows about a repository",
             returnDescription = "true|false",
             pathParameters = {
                     @RestParameter(name = "repoId", isRequired = true,
                             description = "The id of the repository",
                             type = RestParameter.Type.STRING)},
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "true|false")})
  public Response hasRepo(@PathParam("repoId") String repoId) {
    return ok(getOaiPmhServerInfo().hasRepo(repoId));
  }

  @GET
  @Path("/mountpoint")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "mountPoint",
             description = "Ask for the mount point of the OAI-PMH server",
             returnDescription = "The mount point",
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "The mount point")})
  public Response getMountPoint() {
    return ok(getOaiPmhServerInfo().getMountPoint());
  }
}
