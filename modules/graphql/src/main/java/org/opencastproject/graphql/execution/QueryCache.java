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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;

public class QueryCache implements PreparsedDocumentProvider {

  private final Cache<String, PreparsedDocumentEntry> cache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.HOURS)
      .maximumSize(2048)
      .build();

  @Override
  public PreparsedDocumentEntry getDocument(ExecutionInput executionInput,
      Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidateFunction) {
    try {
      return getDocumentAsync(executionInput, parseAndValidateFunction).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput executionInput,
      Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidateFunction) {
    return CompletableFuture.completedFuture(
        cache.get(executionInput.getQuery(), key -> parseAndValidateFunction.apply(executionInput))
    );
  }
}
