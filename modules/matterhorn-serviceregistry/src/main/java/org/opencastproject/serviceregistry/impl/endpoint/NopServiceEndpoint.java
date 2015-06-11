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

package org.opencastproject.serviceregistry.impl.endpoint;

import org.opencastproject.rest.OsgiAbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.impl.NopServiceImpl;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@Path("/")
@RestService(
        name = "nopservice",
        title = "Nop Service",
        notes = {},
        abstractText = "No operation service. Creates empty jobs for testing purposes.")
public class NopServiceEndpoint extends OsgiAbstractJobProducerEndpoint<NopServiceImpl> {
  @GET
  @Path("nop")
  @RestQuery(
          name = "nop",
          description = "Create an empty job for testing purposes.",
          returnDescription = "The service statistics.",
          reponses = { @RestResponse(responseCode = SC_OK, description = "OK") })
  public Response nop() {
    return RestUtil.R.ok(getSvc().nop());
  }
}
