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

import org.opencastproject.graphql.execution.ExecutionService;
import org.opencastproject.graphql.util.GraphQLObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import graphql.ExecutionResult;

@Component(
    service = { HttpServlet.class, Servlet.class }
)
@HttpWhiteboardServletName(GraphQLServlet.GRAPHQL_ENDPOINT)
@HttpWhiteboardServletPattern(GraphQLServlet.GRAPHQL_ENDPOINT + "/*")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=opencast)")
public class GraphQLServlet extends HttpServlet implements Servlet {

  // QUERY is a constant that represents the "query" string
  private static final String QUERY = "query";

  // OPERATION_NAME is a constant that represents the "operationName" string
  private static final String OPERATION_NAME = "operationName";

  // VARIABLES is a constant that represents the "variables" string
  private static final String VARIABLES = "variables";

  // EXTENSIONS is a constant that represents the "extensions" string
  private static final String EXTENSIONS = "extensions";

  // APPLICATION_GRAPHQL is a constant that represents the "application/graphql" string
  private static final String APPLICATION_GRAPHQL = "application/graphql-response+json";

  // GRAPHQL_ENDPOINT is a constant that represents the "/graphql" string
  static final String GRAPHQL_ENDPOINT = "/graphql";

  private final ExecutionService executionService;

  private final ObjectMapper objectMapper;

  @Activate
  public GraphQLServlet(
      @Reference ExecutionService executionService
  ) {
    this.executionService = executionService;
    this.objectMapper = GraphQLObjectMapper.newInstance();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String query = req.getParameter(QUERY);
    String operationName = req.getParameter(OPERATION_NAME);
    String variablesParameter = req.getParameter(VARIABLES);
    String extensionsParameter = req.getParameter(EXTENSIONS);

    Map<String, Object> variables = deserializeMap(variablesParameter);
    Map<String, Object> extensions = deserializeMap(extensionsParameter);

    validateQuery(query);
    ExecutionResult result = executionService.execute(query, operationName, variables, extensions);
    resp.setContentType(APPLICATION_GRAPHQL);
    try (PrintWriter out = resp.getWriter()) {
      objectMapper.writeValue(out, result);
    }
  }

  private Map<String, Object> deserializeMap(String parameter) {
    Map<String, Object> variables = new HashMap<>();
    if ((parameter != null) && (!parameter.isBlank())) {
      TypeReference<Map<String, Object>> typeRef = new TypeReference<>() { };
      try {
        variables = objectMapper.readValue(parameter, typeRef);
      } catch (JsonProcessingException e) {
        throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
      }
    }
    return variables;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() { };
    Map<String, Object> body =  objectMapper.readValue(req.getInputStream(), typeRef);

    String query = getAsString(QUERY, body);
    String operationName = getAsString(OPERATION_NAME, body);
    Map<String, Object> variables = getAsMap(VARIABLES, body);
    Map<String, Object> extensions = getAsMap(EXTENSIONS, body);

    validateQuery(query);
    ExecutionResult result = executionService.execute(query, operationName, variables, extensions);

    resp.setContentType(APPLICATION_GRAPHQL);
    try (PrintWriter out = resp.getWriter()) {
      objectMapper.writeValue(out, result);
    }
  }

  private static String getAsString(String key, Map<String, Object> map) {
    var value = map.get(key);
    if (value != null && !(value instanceof String)) {
      throw new WebApplicationException("Invalid value for '" + key + "'",
          Response.Status.BAD_REQUEST);
    }
    return (String) value;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getAsMap(String key, Map<String, Object> map) {
    var value = map.get(key);
    if (value != null && !(value instanceof Map)) {
      throw new WebApplicationException("Invalid value for '" + key + "'",
          Response.Status.BAD_REQUEST);
    }
    return (Map<String, Object>) value;
  }

  private void validateQuery(String query) {
    if (query == null || query.isBlank()) {
      throw new WebApplicationException("Invalid value for '" + QUERY + "'", Response.Status.BAD_REQUEST);
    }
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    corsHeaders(req, resp);
    resp.flushBuffer();
  }

  private void corsHeaders(HttpServletRequest req,HttpServletResponse resp) {
    resp.setHeader("Access-Control-Allow-Origin", originHeaderFromRequest(req));
    resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    resp.setHeader("Access-Control-Allow-Headers", "Accept, Content-Type, Origin");
  }

  private String originHeaderFromRequest(final HttpServletRequest req) {
    return req != null && req.getHeader("Origin") != null
        ? req.getHeader("Origin")
        : "*";
  }

}
