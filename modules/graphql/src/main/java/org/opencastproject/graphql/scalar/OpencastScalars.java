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

import graphql.schema.GraphQLScalarType;

/**
 * This class provides custom scalar types for the Opencast GraphQL API.
 * Scalar types are primitive data types that GraphQL uses to validate and serialize data.
 * In this case, a custom scalar type for Duration is provided.
 */
public final class OpencastScalars {

  private OpencastScalars() {
  }

  /**
   * An ISO 8601 compliant duration scalar that accepts string values like `PT1H30M10S` and produces
   * `java.time.Duration` objects at runtime.
   * <p>
   * Its {@link graphql.schema.Coercing#serialize(java.lang.Object)}
   * and {@link graphql.schema.Coercing#parseValue(java.lang.Object)} methods
   * accept OffsetDateTime, ZoneDateTime and formatted Strings as valid objects.
   *
   * @see java.time.Duration
   */
  public static final GraphQLScalarType Duration = DurationScalar.INSTANCE;

}
