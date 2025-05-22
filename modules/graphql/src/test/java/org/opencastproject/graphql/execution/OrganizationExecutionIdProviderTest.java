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

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.execution.ExecutionId;

public class OrganizationExecutionIdProviderTest {
  private OrganizationExecutionIdProvider provider;

  @BeforeEach
  public void setup() {
    provider = new OrganizationExecutionIdProvider("testOrganization");
  }

  @Test
  public void testProvide() {
    ExecutionId id1 = provider.provide("query", "operation", null);
    ExecutionId id2 = provider.provide("query", "operation", null);
    assertNotEquals(id1, id2, "Execution IDs should be unique");
  }
}
