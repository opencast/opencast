/**
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
package org.opencastproject.assetmanager.impl.util;

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Equality.hash;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;

/**
 * Implementation of {@link Role} for unit tests.
 */
public final class TestRole implements Role {
  private final String name;
  private final String description;
  private final String organizationId;

  public TestRole(String name, String description, Organization organization) {
    this.name = name;
    this.description = description;
    this.organizationId = organization.getId();
  }

  public static Role mk(String name, String description, Organization organization) {
    return new TestRole(name, description, organization);
  }

  public static Role mk(String name, Organization organization) {
    return new TestRole(name, name, organization);
  }

  @Override public String getName() {
    return name;
  }

  @Override public String getDescription() {
    return description;
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  @Override public int hashCode() {
    return hash(name, description, organizationId);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof TestRole && eqFields((TestRole) that));
  }

  private boolean eqFields(TestRole that) {
    return eq(name, that.name) && eq(description, that.description) && eq(organizationId, that.organizationId);
  }

  @Override public String toString() {
    return "TestRole{"
        + "name='" + name + '\''
        + '}';
  }

  @Override
  public Type getType() {
    return Type.INTERNAL;
  }

}
