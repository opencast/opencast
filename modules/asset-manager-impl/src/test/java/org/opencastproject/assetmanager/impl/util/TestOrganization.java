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

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of {@link Organization} for unit tests.
 */
public final class TestOrganization implements Organization {
  private final String id;
  private final String anonymousRole;
  private final String adminRole;
  private final String name;

  public TestOrganization(String id, String anonymousRole, String adminRole, String name) {
    this.id = id;
    this.anonymousRole = anonymousRole;
    this.adminRole = adminRole;
    this.name = name;
  }

  public static TestOrganization mkDefault() {
    return new TestOrganization(
        DefaultOrganization.DEFAULT_ORGANIZATION_ID,
        DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS,
        DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
        DefaultOrganization.DEFAULT_ORGANIZATION_NAME);
  }

  public static Organization mk(String id, String anonymousRole, String adminRole) {
    return new TestOrganization(id, anonymousRole, adminRole, id);
  }

  @Override public String getId() {
    return id;
  }

  @Override public String getAnonymousRole() {
    return anonymousRole;
  }

  @Override public String getAdminRole() {
    return adminRole;
  }

  @Override public String getName() {
    return name;
  }

  @Override public Map<String, String> getProperties() {
    return Collections.EMPTY_MAP;
  }

  @Override public Map<String, Integer> getServers() {
    return Collections.EMPTY_MAP;
  }

  @Override public int hashCode() {
    return hash(id, anonymousRole, adminRole, name);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof TestOrganization && eqFields((TestOrganization) that));
  }

  private boolean eqFields(TestOrganization that) {
    return eq(id, that.id)
        && eq(anonymousRole, that.anonymousRole)
        && eq(adminRole, that.adminRole)
        && eq(name, that.name);
  }

  @Override public String toString() {
    return "TestOrganization{"
        + "id='" + id + '\''
        + '}';
  }
}
