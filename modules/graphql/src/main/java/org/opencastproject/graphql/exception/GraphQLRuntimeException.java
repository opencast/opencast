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

package org.opencastproject.graphql.exception;

import java.util.List;
import java.util.Map;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

public class GraphQLRuntimeException extends RuntimeException implements GraphQLError {
  private static final long serialVersionUID = 7752209655703366697L;

  private final OpencastErrorType errorType;
  private Map<String,Object> extensions;

  public GraphQLRuntimeException(OpencastErrorType errorType) {
    this.errorType = errorType;
  }

  public GraphQLRuntimeException(String message, OpencastErrorType errorType) {
    super(message);
    this.errorType = errorType;
  }

  public GraphQLRuntimeException(String message, OpencastErrorType errorType, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
  }

  public GraphQLRuntimeException(String message, Map<String,Object> extensions, Throwable cause) {
    super(message, cause);
    this.errorType = OpencastErrorType.Undefined;
    this.extensions = extensions;
  }

  public GraphQLRuntimeException(String message, OpencastErrorType errorType, Map<String,Object> extensions,
      Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
    this.extensions = extensions;
  }

  public GraphQLRuntimeException(OpencastErrorType errorType, Throwable cause) {
    super(cause);
    this.errorType = errorType;
  }

  @Override
  public List<SourceLocation> getLocations() {
    return null;
  }

  public ErrorClassification getErrorType() {
    return errorType;
  }

  @Override
  public List<Object> getPath() {
    return GraphQLError.super.getPath();
  }

  @Override
  public Map<String, Object> toSpecification() {
    return GraphQLError.super.toSpecification();
  }

  public Map<String, Object> getExtensions() {
    return extensions;
  }

}
