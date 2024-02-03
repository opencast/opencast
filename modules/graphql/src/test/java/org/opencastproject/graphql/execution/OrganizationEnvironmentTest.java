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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import graphql.GraphQL;

@ExtendWith(EasyMockExtension.class)
public class OrganizationEnvironmentTest {

  private OrganizationEnvironment organizationEnvironment;

  @Mock
  private GraphQL mockGraphQL;

  @BeforeEach
  public void setup() {
    organizationEnvironment = new OrganizationEnvironment("org1", mockGraphQL);
  }

  @Test
  public void organizationIdIsCorrectlySet() {
    assertEquals("org1", organizationEnvironment.getOrganizationId());
  }

  @Test
  public void graphQLIsCorrectlySet() {
    assertEquals(mockGraphQL, organizationEnvironment.getGraphQL());
  }

  @Test
  public void equalsReturnsTrueForSameInstance() {
    OrganizationEnvironment anotherOrganizationEnvironment = new OrganizationEnvironment("org1", mockGraphQL);
    assertEquals(organizationEnvironment, anotherOrganizationEnvironment);
  }

  @Test
  public void equalsReturnsFalseForDifferentInstance() {
    OrganizationEnvironment anotherOrganizationEnvironment = new OrganizationEnvironment("org2", mockGraphQL);
    assertNotEquals(organizationEnvironment, anotherOrganizationEnvironment);
  }

  @Test
  public void hashCodeIsConsistent() {
    int hashCode1 = organizationEnvironment.hashCode();
    int hashCode2 = organizationEnvironment.hashCode();
    assertEquals(hashCode1, hashCode2);
  }
}
