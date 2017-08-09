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
package org.opencastproject.userdirectory.ldap;

import static java.lang.String.format;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Map a series of LDAP attributes to user authorities in Opencast */
public class OpencastLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

  private Set<String> attributeNames;
  private String[] additionalAuthorities;
  private String prefix = "";
  private boolean uppercase = true;
  private Organization organization;
  private SecurityService securityService;
  private JpaGroupRoleProvider groupRoleProvider;
  private static final Logger logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulator.class);

  /**
   * Activate component
   */
  public OpencastLdapAuthoritiesPopulator(String attributeNames, String prefix, boolean uppercase,
          Organization organization, SecurityService securityService, JpaGroupRoleProvider groupRoleProvider,
          String... additionalAuthorities) {

    debug("Creating new instance");

    if (attributeNames == null) {
      throw new NullPointerException("The attribute list cannot be null");
    }

    if (securityService == null) {
      throw new NullPointerException("The security service cannot be null");
    }
    this.securityService = securityService;

    if (organization == null) {
      throw new NullPointerException("The organization cannot be null");
    }
    this.organization = organization;

    this.attributeNames = new HashSet<String>();
    for (String attributeName : attributeNames.split(",")) {
      String temp = attributeName.trim();
      if (!temp.isEmpty())
        this.attributeNames.add(temp);
    }
    if (this.attributeNames.size() == 0) {
      throw new IllegalArgumentException("At least one valid attribute must be provided");
    }

    if (logger.isDebugEnabled()) {
      debug("Roles will be read from the LDAP attributes:");
      for (String attribute : this.attributeNames) {
        logger.debug("\t* {}", attribute);
      }
    }

    if (groupRoleProvider == null) {
      warn("Provided GroupRoleProvider was null. Group roles will therefore not be expanded");
    }
    this.groupRoleProvider = groupRoleProvider;

    this.prefix = prefix;
    debug("Role prefix set to: {}", this.prefix);

    this.uppercase = uppercase;
    if (uppercase)
      debug("Roles will be converted to uppercase");
    else
      debug("Roles will NOT be converted to uppercase");

    this.additionalAuthorities = additionalAuthorities;
    if (logger.isDebugEnabled() && (additionalAuthorities != null)) {
      debug("Authenticated users will receive the following extra roles:");
      for (String role : additionalAuthorities) {
        logger.debug("\t* {}", role);
      }
    }
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
    for (String attributeName : attributeNames) {
      try {
        String[] attributeValues = userData.getStringAttributes(attributeName);
        // Should the attribute not be defined, the returned array is null
        if (attributeValues != null) {
          for (String attributeValue : attributeValues) {
            // The attribute value may be a single authority (a single role) or a list of roles
            addAuthorities(authorities, attributeValue.split(","));
          }
        } else {
          debug("({}) Could not find any attribute named '{}' in user '{}'", attributeName, userData.getDn());
        }
      } catch (ClassCastException e) {
        error("Specified attribute containing user roles ('{}') was not of expected type String: {}", attributeName, e);
      }
    }

    // Add the list of additional roles
    addAuthorities(authorities, additionalAuthorities);

    if (logger.isDebugEnabled()) {
      debug("Returning user {} with authorities:", username);
      for (GrantedAuthority authority : authorities) {
        logger.error("\t{}", authority);
      }
    }

    return authorities;
  }

  /**
   * Return the attributes names this object will search for
   *
   * @return a {@link Collection} containing such attribute names
   */
  public Collection<String> getAttributeNames() {
    return new HashSet<String>(this.attributeNames);
  }

  /**
   * Get the role prefix being used by this object. Please note that such prefix can be empty.
   *
   * @return the role prefix in use.
   */
  public String getRolePrefix() {
    return prefix;
  }

  /**
   * Get the property that defines whether or not the role names should be converted to uppercase.
   *
   * @return {@code true} if this class converts the role names to uppercase. {@code false} otherwise.
   */
  public boolean getConvertToUpperCase() {
    return uppercase;
  }

  /**
   * Get the extra roles to be added to any user returned by this authorities populator
   *
   * @return A {@link Collection} of {@link String}s representing the additional roles
   */
  public String[] getAdditionalAuthorities() {
    if (additionalAuthorities != null)
      return additionalAuthorities.clone();
    else
      return null;
  }

  /**
   * Add the specified authorities to the provided set
   *
   * @param authorities
   *          a set containing the authorities
   * @param values
   *          the values to add to the set
   */
  private void addAuthorities(Set<GrantedAuthority> authorities, String[] values) {

    if (values != null) {
      Organization org = securityService.getOrganization();
      if (!this.organization.equals(org)) {
        throw new SecurityException(String.format("Current request belongs to the organization \"%s\". Expected \"%s\"",
                org.getId(), this.organization.getId()));
      }

      for (String value : values) {
        String authority = parseAuthority(value);
        debug("Parsed LDAP role \"{}\" to \"{}\"", value, authority);
        // Ignore the empty parts
        if (!authority.isEmpty()) {
          authorities.add(new SimpleGrantedAuthority(authority));

          // Check if this role is a group role and assign the groups appropriately
          if (groupRoleProvider != null) {
            List<Role> roles = groupRoleProvider.getRolesForGroup(authority);
            if (!roles.isEmpty()) {
              debug("Found group for the group role \"{}\": {}", authority);
              for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
                logger.debug("\tAdded role from role \"{}\"'s group: {}", authority, role);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Processes a {@link String} representing a {@link GrantedAuthority} to make it comply with the expected format
   *
   * In particular, the method adds a prefix (if defined), capitalises the {@String}, substitutes the white spaces by
   * underscores ("_") and collapses any series of underscores into a single one.
   *
   * @param authority
   *          A {@code String} representing the authority
   * @return a {@code String} representing the authority in a standard format
   */
  private String parseAuthority(String authority) {
    // Trim the authority
    // We do not substitute the whitespace here because we must make sure that the prefix follows the spacing
    // conventions, too
    authority = authority.trim();
    String prefix = (this.prefix == null) ? "" : this.prefix.trim();

    // Add prefix only if the authority is not empty
    if (!authority.isEmpty()) {
      if (uppercase) {
        return (prefix + authority).replaceAll("[\\s_]+", "_").toUpperCase();
      } else {
        return (prefix + authority).replaceAll("[\\s_]+", "_");
      }
    }

    return authority;
  }

  /**
   * Utility class to print this instance's hash code before the debug messages
   *
   * @param message
   * @param params
   */
  private void debug(String message, Object... params) {
    logger.debug(format("(%s) %s", this.hashCode(), message), params);
  }

  /**
   * Utility class to print this instance's hash code before the error messages
   *
   * @param message
   * @param params
   */
  private void error(String message, Object... params) {
    logger.error(format("(%s) %s", this.hashCode(), message), params);
  }

  /**
   * Utility class to print this instance's hash code before the warning messages
   *
   * @param message
   * @param params
   */
  private void warn(String message, Object... params) {
    logger.warn(format("(%s) %s", this.hashCode(), message), params);
  }

  /** OSGi callback for setting the role group service. */
  public void setOrgDirectory(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /** OSGi callback for setting the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
