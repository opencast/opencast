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

package org.opencastproject.graphql.execution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import graphql.GraphQLError;

public class ExecutionErrorResult implements ExecutionResult {

  private final List<GraphQLError> errors;

  private final Map<Object, Object> extensions;

  public ExecutionErrorResult(List<GraphQLError> errors) {
    this(errors, null);
  }

  public ExecutionErrorResult(List<GraphQLError> errors, Map<Object, Object> extensions) {
    this.errors = errors;
    this.extensions = extensions;
  }


  @Override
  public List<GraphQLError> getErrors() {
    return errors;
  }

  @Override
  public <T> T getData() {
    return null;
  }

  @Override
  public boolean isDataPresent() {
    return false;
  }

  @Override
  public Map<Object, Object> getExtensions() {
    return extensions;
  }

  @Override
  public Map<String, Object> toSpecification() {
    return null;
  }

  public static ExecutionErrorResult.Builder newExecutionResult() {
    return new ExecutionErrorResult.Builder();
  }

  public static class Builder implements ExecutionResult.Builder<ExecutionErrorResult.Builder> {

    private List<GraphQLError> errors = new ArrayList<>();
    private Map<Object, Object> extensions;

    @Override
    public ExecutionErrorResult.Builder from(ExecutionResult executionResult) {
      errors = new ArrayList<>(executionResult.getErrors());
      extensions = executionResult.getExtensions();
      return this;
    }

    @Override
    public ExecutionErrorResult.Builder data(Object data) {
      throw new UnsupportedOperationException("Cannot set data on an error result");
    }

    @Override
    public ExecutionErrorResult.Builder errors(List<GraphQLError> errors) {
      this.errors = errors;
      return this;
    }

    @Override
    public ExecutionErrorResult.Builder addErrors(List<GraphQLError> errors) {
      this.errors.addAll(errors);
      return this;
    }

    @Override
    public ExecutionErrorResult.Builder addError(GraphQLError error) {
      this.errors.add(error);
      return this;
    }

    @Override
    public ExecutionErrorResult.Builder extensions(Map<Object, Object> extensions) {
      this.extensions = extensions;
      return this;
    }

    @Override
    public ExecutionErrorResult.Builder addExtension(String key, Object value) {
      this.extensions = (this.extensions == null ? new LinkedHashMap<>() : this.extensions);
      this.extensions.put(key, value);
      return this;
    }

    @Override
    public ExecutionResult build() {
      return new ExecutionErrorResult(errors, extensions);
    }
  }

}
