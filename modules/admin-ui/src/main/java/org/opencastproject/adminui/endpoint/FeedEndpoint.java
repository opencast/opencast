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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@RestService(name = "FeedService", title = "Admin UI Feed Service",
  abstractText = "Provides Feed Information",
  notes = {"This service offers Feed information for the admin UI."})
public class FeedEndpoint extends RemoteBase {

  public FeedEndpoint() {
    super("org.opencastproject.feed.impl.FeedServiceImpl");
  }

  @GET
  @Path("/listFeedServices")
  @Produces(MediaType.APPLICATION_JSON)
  public String listFeedServices() {

    Gson gson = new Gson();
    List<Map<String, String>> feedServices = new ArrayList<>();

    Map<String, String> details = new HashMap<>();
    details.put("identifier", "localhost test");
    details.put("name", "some text");
    details.put("description", "some desc test");
    details.put("copyright", "MIT");
    details.put("type", "SomeClass");
    feedServices.add(details);

    HttpGet get = new HttpGet("/listFeedServices");
    try {
      HttpResponse response = getResponse(get);
      // response.getEntity().getContent()
    } catch (Exception e) {

    }


    return gson.toJson(feedServices);
  }

}
