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
import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.fns.Booleans;

import java.util.Collections;
import java.util.Set;

/**
 * Implementation of {@link User} for unit tests.
 */
public class TestUser implements User {
  private final String username;
  private final String password;
  private final String name;
  private final String email;
  private final String provider;
  private final boolean manageable;
  private final Organization organization;
  private final Set<Role> roles;

  public TestUser(
      String username,
      String password,
      String name,
      String email,
      String provider,
      boolean manageable,
      Organization organization,
      Set<Role> roles) {
    this.username = username;
    this.password = password;
    this.name = name;
    this.email = email;
    this.provider = provider;
    this.manageable = manageable;
    this.organization = organization;
    this.roles = roles;
  }

  public static User mk(String userName, Organization organization,
                        Set<Role> roles) {
    return new TestUser(userName, "password", "name", "email", "provider", true, organization, roles);
  }

  public static User mk(Organization organization, Set<Role> roles) {
    return new TestUser("user", "password", "name", "email", "provider", true, organization, roles);
  }

  public static User mk(Organization organization) {
    return new TestUser("user", "password", "name", "email", "provider",
        true, organization, Collections.<Role>emptySet());
  }

  public static User mk(Organization organization, Role... roles) {
    return new TestUser("user", "password", "name", "email", "provider", true, organization, $(roles).toSet());
  }

  /**
   * Create a new user of organization <code>organization</code> with a set of rules.
   * The created roles all belong to <code>organization</code>.
   */
  public static User mk(final Organization organization, String... roles) {
    return new TestUser(
        "user", "password", "name", "email", "provider", true, organization,
        $(roles).map(new Fn<String, Role>() {
          @Override public Role apply(String role) {
            return TestRole.mk(role, organization);
          }
        }).toSet());
  }

  @Override public String getUsername() {
    return username;
  }

  @Override public String getPassword() {
    return password;
  }

  @Override public String getName() {
    return name;
  }

  @Override public String getEmail() {
    return email;
  }

  @Override public String getProvider() {
    return provider;
  }

  @Override public boolean isManageable() {
    return manageable;
  }

  @Override public Organization getOrganization() {
    return organization;
  }

  @Override public Set<Role> getRoles() {
    return roles;
  }

  @Override public boolean hasRole(String role) {
    return $(roles).map(getRoleName).exists(Booleans.eq(role));
  }

  private final Fn<Role, String> getRoleName = new Fn<Role, String>() {
    @Override public String apply(Role role) {
      return role.getName();
    }
  };

  @Override public int hashCode() {
    return hash(username, password, name, email, provider, manageable, organization, roles);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof TestUser && eqFields((TestUser) that));
  }

  private boolean eqFields(TestUser that) {
    return eq(username, that.username) && eq(password, that.password) && eq(name, that.name) && eq(email, that.email)
        && eq(provider, that.provider) && eq(manageable, that.manageable) && eq(organization, that.organization)
        && eq(roles, that.roles);
  }

  @Override public String toString() {
    return "TestUser{"
        + "username='" + username + '\''
        + ", roles=[" + $(roles).mkString(",")
        + "]}";
  }
}
