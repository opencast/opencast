/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.impl;

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ADMIN;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

/**
 * A security service useful for testing.
 */
public class SecurityServiceStub implements SecurityService {

//  private User user = new User("admin", DEFAULT_ORGANIZATION_ID, new String[] { DEFAULT_ORGANIZATION_ADMIN });
//
  public static final User DEFAULT_ORG_ADMIN = new User("admin", DEFAULT_ORGANIZATION_ID, new String[] { DEFAULT_ORGANIZATION_ADMIN });

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> user = new ThreadLocal<User>();

  /** Holds organization responsible for the current thread */
  private static final ThreadLocal<Organization> organization = new ThreadLocal<Organization>();

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
   * @see org.opencastproject.security.api.SecurityService#setOrganization(org.opencastproject.security.api.Organization)
   */
  @Override
  public void setOrganization(Organization organization) {
    SecurityServiceStub.organization.set(organization);
  }

}
