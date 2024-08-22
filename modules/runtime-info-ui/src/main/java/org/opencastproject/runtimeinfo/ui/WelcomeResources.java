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

package org.opencastproject.runtimeinfo.ui;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Hidden;

@Path("/")
@Component(
    immediate = true,
    service = WelcomeResources.class,
    property = {
        "service.description=Welcome Resources",
        "opencast.service.type=org.opencastproject.welcome",
        "opencast.service.path=/"
    }
)
@Hidden
@JaxrsResource
public class WelcomeResources {

  private static final Map<String, MediaType> MEDIA_TYPES;

  static {
    MEDIA_TYPES = new HashMap<>();
    MEDIA_TYPES.put("js", new MediaType("text", "javascript", StandardCharsets.UTF_8.name()));
    MEDIA_TYPES.put("css", new MediaType("text", "css"));
    MEDIA_TYPES.put("svg", new MediaType("image", "svg+xml"));
    MEDIA_TYPES.put("png", new MediaType("image", "png"));
  }


  @GET
  @Path("{path: .*}")
  public Response img(@PathParam("path") String path) {
    if ("".equals(path)) {
      path = "index.html";
    }
    InputStream resource = getClass().getClassLoader().getResourceAsStream("ui/" + path);
    return Objects.isNull(resource)
        ? Response.status(NOT_FOUND).build()
        : Response.ok().entity(resource).build();
  }

  @GET
  @Path("{path: (img|scripts|styles)/.*}")
  public Response staticResources(@PathParam("path") final String path) {
    InputStream resource = getClass().getClassLoader().getResourceAsStream("ui/" + path);

    Response.ResponseBuilder builder = Objects.isNull(resource)
        ? Response.status(NOT_FOUND)
        : Response.ok().entity(resource);

    int index = path.lastIndexOf(".");
    if (index != -1) {
      String extension = path.substring(index + 1);
      if (MEDIA_TYPES.containsKey(extension)) {
        builder.type(MEDIA_TYPES.get(extension));
      }
    }

    return builder.build();
  }

}
