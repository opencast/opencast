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

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Map a series of LDAP attributes to user authorities in Opencast */
public class OpencastLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

  public static final String ROLE_CLEAN_REGEXP = "[\\s_]+";
  public static final String ROLE_CLEAN_REPLACEMENT = "_";

  private Set<String> attributeNames;
  private String[] additionalAuthorities;
  private String prefix = "";
  private Set<String> excludedPrefixes = new HashSet<>();
  private boolean uppercase = true;
  private Organization organization;
  private SecurityService securityService;
  private JpaGroupRoleProvider groupRoleProvider;
  private static final Logger logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulator.class);

  /**
   * Activate component
   */
  public OpencastLdapAuthoritiesPopulator(String attributeNames, String prefix, String[] aExcludedPrefixes,
          boolean uppercase, Organization organization, SecurityService securityService,
          JpaGroupRoleProvider groupRoleProvider, String... additionalAuthorities) {

    debug("Creating new instance");

    if (attributeNames == null) {
      throw new IllegalArgumentException("The attribute list cannot be null");
    }

    if (securityService == null) {
      throw new IllegalArgumentException("The security service cannot be null");
    }
    this.securityService = securityService;

    if (organization == null) {
      throw new IllegalArgumentException("The organization cannot be null");
    }
    this.organization = organization;

    this.attributeNames = new HashSet<>();
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
      info("Provided GroupRoleProvider was null. Group roles will therefore not be expanded");
    }
    this.groupRoleProvider = groupRoleProvider;

    this.uppercase = uppercase;
    if (uppercase)
      debug("Roles will be converted to uppercase");
    else
      debug("Roles will NOT be converted to uppercase");

    if (uppercase)
      this.prefix = StringUtils.trimToEmpty(prefix).replaceAll(ROLE_CLEAN_REGEXP, ROLE_CLEAN_REPLACEMENT).toUpperCase();
    else
      this.prefix = StringUtils.trimToEmpty(prefix).replaceAll(ROLE_CLEAN_REGEXP, ROLE_CLEAN_REPLACEMENT);
    debug("Role prefix set to: {}", this.prefix);

    if (aExcludedPrefixes != null)
      for (String origExcludedPrefix : aExcludedPrefixes) {
        String excludedPrefix;
        if (uppercase)
          excludedPrefix = StringUtils.trimToEmpty(origExcludedPrefix).toUpperCase();
        else
          excludedPrefix = StringUtils.trimToEmpty(origExcludedPrefix);
        if (!excludedPrefix.isEmpty()) {
          excludedPrefixes.add(excludedPrefix);
        }
      }

    if (additionalAuthorities == null)
      this.additionalAuthorities = new String[0];
    else
      this.additionalAuthorities = additionalAuthorities;

    if (logger.isDebugEnabled()) {
      debug("Authenticated users will receive the following extra roles:");
      for (String role : this.additionalAuthorities) {
        logger.debug("\t* {}", role);
      }
    }
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {

    Set<GrantedAuthority> authorities = new HashSet<>();
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
        logger.debug("\t{}", authority);
      }
    }

    // Update the user in the security service if it matches the user whose authorities are being returned
    if ((securityService.getOrganization().equals(organization))
            && ((securityService.getUser() == null) || (securityService.getUser().getUsername().equals(username)))) {
      Set<JaxbRole> roles = new HashSet<>();
      // Get the current roles
      for (Role existingRole : securityService.getUser().getRoles()) {
        authorities.add(new SimpleGrantedAuthority(existingRole.getName()));
      }
      // Convert GrantedAuthority's into JaxbRole's
      for (GrantedAuthority authority : authorities)
        roles.add(new JaxbRole(authority.getAuthority(), JaxbOrganization.fromOrganization(organization)));
      JaxbUser user = new JaxbUser(username, LdapUserProviderInstance.PROVIDER_NAME,
              JaxbOrganization.fromOrganization(organization), roles.toArray(new JaxbRole[0]));

      securityService.setUser(user);
    }

    return authorities;
  }

  /**
   * Return the attributes names this object will search for
   *
   * @return a {@link Collection} containing such attribute names
   */
  public Collection<String> getAttributeNames() {
    return new HashSet<>(attributeNames);
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
   * Get the exclude prefixes being used by this object.
   *
   * @return the role prefix in use.
   */
  public String[] getExcludePrefixes() {
    return excludedPrefixes.toArray(new String[0]);
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
    return additionalAuthorities.clone();
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
      if (!organization.equals(org)) {
        throw new SecurityException(String.format("Current request belongs to the organization \"%s\". Expected \"%s\"",
                org.getId(), organization.getId()));
      }

      for (String value : values) {
        /*
         * Please note the prefix logic for roles:
         *
         * - Roles that start with any of the "exclude prefixes" are left intact
         * - In any other case, the "role prefix" is prepended to the roles read from LDAP
         *
         * This only applies to the prefix addition. The conversion to uppercase is independent from these
         * considerations
         */
        String authority;
        if (uppercase)
          authority = StringUtils.trimToEmpty(value).replaceAll(ROLE_CLEAN_REGEXP, ROLE_CLEAN_REPLACEMENT)
                  .toUpperCase();
        else
          authority = StringUtils.trimToEmpty(value).replaceAll(ROLE_CLEAN_REGEXP, ROLE_CLEAN_REPLACEMENT);

        // Ignore the empty parts
        if (!authority.isEmpty()) {
          // Check if this role is a group role and assign the groups appropriately
          List<Role> groupRoles;
          if (groupRoleProvider != null)
            groupRoles = groupRoleProvider.getRolesForGroup(authority);
          else
            groupRoles = Collections.emptyList();

          // Try to add the prefix if appropriate
          String prefix = this.prefix;

          if (!prefix.isEmpty()) {
            boolean hasExcludePrefix = false;
            for (String excludePrefix : excludedPrefixes) {
              if (authority.startsWith(excludePrefix)) {
                hasExcludePrefix = true;
                break;
              }
            }
            if (hasExcludePrefix)
              prefix = "";
          }

          authority = (prefix + authority).replaceAll(ROLE_CLEAN_REGEXP, ROLE_CLEAN_REPLACEMENT);

          debug("Parsed LDAP role \"{}\" to role \"{}\"", value, authority);

          if (!groupRoles.isEmpty()) {
            // The authority is a group role
            debug("Found group for the group with group role \"{}\"", authority);
            for (Role role : groupRoles) {
              authorities.add(new SimpleGrantedAuthority(role.getName()));
              logger.debug("\tAdded role from role \"{}\"'s group: {}", authority, role);
            }
          }

          // Finally, add the authority itself
          authorities.add(new SimpleGrantedAuthority(authority));

        } else {
          debug("Found empty authority. Ignoring...");
        }
      }
    }
  }

  /**
   * Utility class to print this instance's hash code before the debug messages
   *
   * @param message
   * @param params
   */
  private void debug(String message, Object... params) {
    logger.debug(format("(%s) %s", hashCode(), message), params);
  }

  /**
   * Utility class to print this instance's hash code before the error messages
   *
   * @param message
   * @param params
   */
  private void error(String message, Object... params) {
    logger.error(format("(%s) %s", hashCode(), message), params);
  }

  /**
   * Utility class to print this instance's hash code before INFO messages
   *
   * @param message
   * @param params
   */
  private void info(String message, Object... params) {
    logger.info(format("(%s) %s", hashCode(), message), params);
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
