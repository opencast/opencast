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

package org.opencastproject.editor;

import org.opencastproject.editor.api.EditorRestEndpointBase;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.Path;

/**
 * The REST endpoint for the {@link EditorService} service
 */
@Path("/editor")
@Component(
    property = {
        "service.description=Editor REST Endpoint",
        "opencast.service.type=org.opencastproject.editor",
        "opencast.service.path=/editor"
    },
    immediate = true,
    service = EditorServiceEndpoint.class
)
@RestService(name = "EditorServiceEndpoint",
    title = "Editor Service Endpoint",
    abstractText = "This is the editor service.",
    notes = { })
@JaxrsResource
public class EditorServiceEndpoint extends EditorRestEndpointBase {

  @Reference
  public void setEditorService(EditorService service) {
    this.editorService = service;
  }

}
