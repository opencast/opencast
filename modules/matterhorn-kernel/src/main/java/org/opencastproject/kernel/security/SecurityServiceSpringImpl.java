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
package org.opencastproject.kernel.security;

import static org.opencastproject.security.api.SecurityConstants.ANONYMOUS_USERNAME;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> delegatedUserHolder = new ThreadLocal<User>();

  /** Holds organization responsible for the current thread */
  private static final ThreadLocal<Organization> organization = new ThreadLocal<Organization>();

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getOrganization()
   */
  @Override
  public Organization getOrganization() {
    return SecurityServiceSpringImpl.organization.get();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#setOrganization(org.opencastproject.security.api.Organization)
   */
  @Override
  public void setOrganization(Organization organization) {
    SecurityServiceSpringImpl.organization.set(organization);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getUser()
   */
  @Override
  public User getUser() {
    Organization org = getOrganization();
    User delegatedUser = delegatedUserHolder.get();
    if (delegatedUser != null) {
      return delegatedUser;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String anonymousRole = org.getAnonymousRole();
    if (auth == null) {
      return new User(ANONYMOUS_USERNAME, org.getId(), new String[] { anonymousRole });
    } else {
      Object principal = auth.getPrincipal();
      if (principal == null) {
        return new User(ANONYMOUS_USERNAME, org.getId(), new String[] { anonymousRole });
      }
      if (principal instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) principal;

        String[] roles = null;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
          roles = new String[authorities.size()];
          int i = 0;
          for (GrantedAuthority ga : authorities) {
            roles[i++] = ga.getAuthority();
          }
        }
        return new User(userDetails.getUsername(), org.getId(), roles);
      } else {
        return new User(ANONYMOUS_USERNAME, org.getId(), new String[] { anonymousRole });
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#setUser(org.opencastproject.security.api.User)
   */
  @Override
  public void setUser(User user) {
    delegatedUserHolder.set(user);
  }

}
