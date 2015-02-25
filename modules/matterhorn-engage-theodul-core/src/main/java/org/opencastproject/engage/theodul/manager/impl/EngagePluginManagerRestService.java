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
package org.opencastproject.engage.theodul.manager.impl;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opencastproject.engage.theodul.api.EngagePluginManager;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@RestService(name = "EngagePluginManager", title = "Engage Plugin Manager Service",
        abstractText = "REST endpoint for the service that manages plugins for the Engage Player.",
        notes = {
            "All paths above are relative to the REST endpoint base (something like http://your.server/engage/plugins/manager)",
            "If the service is down or not working it will return a status 503, this means the the underlying service is "
            + "not working and is either restarting or has failed",
            "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
            + "other words, there is a bug! You should file an error report with your server logs from the time when the "
            + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"})
public class EngagePluginManagerRestService {

    private static final Logger log = LoggerFactory.getLogger(EngagePluginManagerRestService.class);
    private EngagePluginManager manager;

    protected void setPluginManager(EngagePluginManager manager) {
        this.manager = manager;
    }

    protected void activate() {
        log.info("Activated.");
    }

    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
    @Path("list.{format:xml|json}")
    @RestQuery(name = "plugins", description = "Returns the list of all registered Engage Player plugins.",
            pathParameters = {
                @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING)
            },
            reponses = {
                @RestResponse(description = "the list of plugins was successfully retrieved.", responseCode = HttpServletResponse.SC_OK),
                @RestResponse(description = "something went wrong.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            }, returnDescription = "The list of all registered plugins.")
    public Response listPlugins(@PathParam("format") String format) {
        try {
            EngagePluginRegistrationList plugins = new EngagePluginRegistrationList(manager.getAllRegisteredPlugins());

            final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;

            return Response.ok().entity(plugins).type(type).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
        }
    }

    /**
     * Builds an error message in case of an unexpected error in an endpoint
     * method, includes the exception type and message if existing.
     *
     * @param e Exception that was thrown
     * @return error message
     */
    private String buildUnexpectedErrorMessage(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected error (").append(e.getClass().getName()).append(")");
        String message = e.getMessage();
        if (message != null && message.length() > 0) {
            sb.append(": ").append(message);
        }
        return sb.toString();
    }
}
