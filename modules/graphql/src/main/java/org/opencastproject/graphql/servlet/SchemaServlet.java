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

package org.opencastproject.graphql.servlet;

import org.opencastproject.graphql.schema.SchemaService;
import org.opencastproject.security.api.SecurityService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;

@Component(
    service = { HttpServlet.class, Servlet.class }
)
@HttpWhiteboardServletName(GraphQLServlet.GRAPHQL_ENDPOINT + "/schema.json")
@HttpWhiteboardServletPattern(GraphQLServlet.GRAPHQL_ENDPOINT + "/schema.json")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=opencast)")
public class SchemaServlet extends HttpServlet implements Servlet {

  private static final String CONTENT_TYPE = "text/plain";

  private final SchemaService schemaService;

  private final SchemaPrinter schemaPrinter;

  private final SecurityService securityService;

  @Activate
  public SchemaServlet(
      @Reference SchemaService schemaService,
      @Reference SecurityService securityService
  ) {
    super();
    this.schemaService = schemaService;
    this.securityService = securityService;

    SchemaPrinter.Options options = SchemaPrinter.Options.defaultOptions();
    options = options.descriptionsAsHashComments(false);
    options = options.includeDirectives(true);
    options = options.includeIntrospectionTypes(true);
    options = options.includeScalarTypes(true);
    options = options.includeSchemaDefinition(true);
    options = options.useAstDefinitions(false);
    this.schemaPrinter = new SchemaPrinter(options);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType(CONTENT_TYPE);

    if (securityService.getOrganization() == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to determine organization");
      return;
    }

    try (PrintWriter out = resp.getWriter()) {
      GraphQLSchema schema = schemaService.buildSchema(securityService.getOrganization());
      out.print(schemaPrinter.print(schema));
      out.flush();
    }
  }
}
