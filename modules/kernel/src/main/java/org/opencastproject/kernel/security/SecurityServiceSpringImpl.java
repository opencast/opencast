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

package org.opencastproject.kernel.security;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SecurityServiceSpringImpl.class);

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> delegatedUserHolder = new ThreadLocal<User>();

  /** Holds the IP address for the delegated user for the current thread */
  private static final ThreadLocal<String> delegatedUserIPHolder = new ThreadLocal<String>();

  /** Holds organization responsible for the current thread */
  private static final ThreadLocal<Organization> organization = new ThreadLocal<Organization>();

  /** The user directory */
  private UserDirectoryService userDirectory;

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
   * @see org.opencastproject.security.api.SecurityService#setOrganization(Organization)
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
  public User getUser() throws IllegalStateException {
    Organization org = getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set in security context");

    User delegatedUser = delegatedUserHolder.get();

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof AnonymousAuthenticationToken) {
      return SecurityUtil.createAnonymousUser(org);
    }

    if (delegatedUser != null) {
      return delegatedUser;
    }

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
    if (auth != null) {
      Object principal = auth.getPrincipal();
      if ((principal instanceof UserDetails)) {
        UserDetails userDetails = (UserDetails) principal;

        User user = null;

        // If user exists, fetch it from the userDirectory
        if (userDirectory != null) {
          user = userDirectory.loadUser(userDetails.getUsername());
          if (user == null) {
            logger.debug("Authenticated user '{}' could not be found in any of the current UserProviders. "
                + "Continuing anyway...", userDetails.getUsername());
          }
        } else {
          logger.debug("No UserDirectory was found when trying to search for user '{}'", userDetails.getUsername());
        }

        // Add the roles (authorities) in the security context
        Set<JaxbRole> roles = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null) {
          for (GrantedAuthority ga : authorities) {
            roles.add(new JaxbRole(ga.getAuthority(), jaxbOrganization));
          }
        }

        if (user == null) {
          // No user was found. Create one to hold the auth information from the security context
          user = new JaxbUser(userDetails.getUsername(), null, jaxbOrganization, roles);
        } else {
          // Combine the existing user with the roles in the security context
          user = JaxbUser.fromUser(user, roles);
        }

        // Save the user to retrieve it quicker the next time(s) this method is called (by this thread)
        delegatedUserHolder.set(user);

        return user;
      }
    }

    // Return the anonymous user by default
    return SecurityUtil.createAnonymousUser(jaxbOrganization);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.SecurityService#setUser(User)
   */
  @Override
  public void setUser(User user) {
    delegatedUserHolder.set(user);
  }

  @Override
  public String getUserIP() {
    return delegatedUserIPHolder.get();
  }

  @Override
  public void setUserIP(String userIP) {
    delegatedUserIPHolder.set(userIP);
  }

  /**
   * OSGi callback for setting the user directory.
   *
   * @param userDirectory
   *          the user directory
   */
  void setUserDirectory(UserDirectoryService userDirectory) {
    this.userDirectory = userDirectory;
  }

  /**
   * OSGi callback for removing the user directory.
   */
  void removeUserDirectory() {
    this.userDirectory = null;
  }

}
