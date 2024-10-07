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

package org.opencastproject.graphql.scalar;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

public final class DurationScalar {

  public static final GraphQLScalarType INSTANCE;

  private DurationScalar() { }

  static {
    Coercing<Duration, String> coercing = new Coercing<Duration, String>() {

      @Override
      public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale)
              throws CoercingSerializeException {
        Duration duration;
        if (dataFetcherResult instanceof Duration) {
          duration = (Duration) dataFetcherResult;
        } else if (dataFetcherResult instanceof String) {
          duration = parseDuration((String) dataFetcherResult, CoercingSerializeException::new);
        } else if (dataFetcherResult instanceof Long) {
          duration = Duration.of((Long) dataFetcherResult, java.time.temporal.ChronoUnit.MILLIS);
        } else {
          throw new CoercingSerializeException("Expected something we can convert to 'java.time.Duration' but was '"
              + dataFetcherResult.getClass().getSimpleName() + "'.");
        }

        return duration.toString();
      }

      @Override
      public Duration parseValue(Object input, GraphQLContext graphQLContext, Locale locale)
              throws CoercingParseValueException {
        Duration duration;
        if (input instanceof Duration) {
          duration = (Duration) input;
        } else if (input instanceof String) {
          duration = parseDuration((String) input, CoercingParseValueException::new);
        } else if (input instanceof Long) {
          duration = Duration.of((Long) input, java.time.temporal.ChronoUnit.MILLIS);
        } else {
          throw new CoercingSerializeException("Expected something we can convert to 'java.time.Duration' but was '"
              + input.getClass().getSimpleName() + "'.");
        }

        return duration;
      }

      @Override
      public Duration parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext,
          Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
          return parseDuration(((StringValue) input).getValue(), CoercingParseLiteralException::new);
        } else {
          throw new CoercingParseLiteralException("Expected AST type 'StringValue' but was '"
              + input.getClass().getSimpleName() + "'.");
        }
      }

      @Override
      public Value<?> valueToLiteral(Object input, GraphQLContext graphQLContext, Locale locale) {
        String s = serialize(input, graphQLContext, locale);
        return StringValue.newStringValue(s).build();
      }

      private Duration parseDuration(String input, Function<String, RuntimeException> exceptionMaker) {
        try {
          if (input.isEmpty()) {
            return Duration.ZERO;
          }
          if (!input.contains("P")) {
            return Duration.of(Long.parseLong(input), java.time.temporal.ChronoUnit.MILLIS);
          }
          return Duration.parse(input);
        } catch (Exception e) {
          throw exceptionMaker.apply("Invalid RFC-3339 compliant Duration value: " + input);
        }
      }

    };

    INSTANCE = GraphQLScalarType.newScalar()
        .name("Duration")
        .description("A slightly refined version of RFC-3339 compliant DateTime Scalar")
        .specifiedByUrl("https://scalars.opencast.org/duration")
        .coercing(coercing)
        .build();
  }
}
