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

package org.opencastproject.workflow.impl;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import java.util.HashSet;

/**
 * A security service useful for testing.
 */
public class SecurityServiceStub implements SecurityService {

  public static final User DEFAULT_ORG_ADMIN;

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> user = new ThreadLocal<User>();

  /** Holds organization responsible for the current thread */
  private static final ThreadLocal<Organization> organization = new ThreadLocal<Organization>();

  static {
    HashSet<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, new DefaultOrganization(), ""));
    DEFAULT_ORG_ADMIN = new JaxbUser("admin", "test", new DefaultOrganization(), roles);
  }

  /**
   *
   */
  public SecurityServiceStub() {
    setUser(DEFAULT_ORG_ADMIN);
    setOrganization(new DefaultOrganization());
  }

  @Override
  public User getUser() {
    return user.get();
  }

  @Override
  public void setUser(User user) {
    SecurityServiceStub.user.set(user);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.SecurityService#getOrganization()
   */
  @Override
  public Organization getOrganization() {
    return organization.get();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.SecurityService#setOrganization(Organization)
   */
  @Override
  public void setOrganization(Organization organization) {
    SecurityServiceStub.organization.set(organization);
  }

}
