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
package org.opencastproject.kernel.userdirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Map a series of LDAP attributes to user authorities in Opencast */
public class LdapAttributeAuthoritiesPopulator implements LdapAuthoritiesPopulator {

  private Set<String> attributeNames;
  private String prefix = "";
  private boolean uppercase = true;
  private Set<String> additionalAuthorities;
  private static final Logger logger = LoggerFactory.getLogger(LdapAttributeAuthoritiesPopulator.class);

  /**
   * Build a new authorities populator
   *
   * This assumes that the authenticated users contain an attribute consisting of a space- or comma-separated list of
   * authorities
   *
   * @param attributeNames
   *          The LDAP user attribute containing the roles
   */
  public LdapAttributeAuthoritiesPopulator(Collection<? extends String> attributeNames) {
    if (attributeNames == null) {
      throw new NullPointerException("The attribute list cannot be null");
    }

    if (attributeNames.size() == 0) {
      throw new IllegalArgumentException("At least one attribute must be provided");
    }

    this.attributeNames = new HashSet<String>(attributeNames);

    this.additionalAuthorities = new HashSet<String>();
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
          logger.debug("Could not find any attribute named '{}' in user '{}'", attributeName, userData.getDn());
        }
      } catch (ClassCastException e) {
        logger.error("Specified attribute containing user roles ('{}') was not of expected type String: {}",
                attributeName, e);
      }
    }

    // Add the additional authorities
    addAuthorities(authorities, additionalAuthorities.toArray(new String[additionalAuthorities.size()]));

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
   * Define the prefix that should be appended to all the roles obtained.
   *
   * The prefix is empty by default.
   *
   * @param prefix
   *          A {@link String} containing the desired role prefix.
   *
   */
  public void setRolePrefix(String prefix) {
    this.prefix = (prefix == null) ? "" : prefix;
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
   * Set whether or not to convert the role names to uppercase.
   *
   * Initially, this property defaults to {@code true}.
   *
   * @param convert
   *          {@code true} to convert the role names to uppercase. {@code false} otherwise.
   */
  public void setConvertToUpperCase(boolean convert) {
    uppercase = convert;
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
   * Define the set of additional authorities that are appended to the authority list of every authenticated user.
   *
   * The authorities must not be null nor empty.
   *
   * @param authorities
   *          a {@link Collection} of {@link String}s representing the additional authorities.
   */
  public void setAdditionalAuthorities(Set<? extends String> authorities) {
    if (authorities == null) {
      additionalAuthorities.clear();
    } else {
      additionalAuthorities = new HashSet<String>(authorities);
    }
  }

  /**
   * Return the set of additional authorities, that are appended to the authority list of every authenticated user.
   *
   * @return a {@link Collection} of {@link String}s representing the additional authorities.
   */
  public Set<? extends String> getAdditionalAuthorities() {
    return new HashSet<String>(additionalAuthorities);
  }

  private void addAuthorities(Set<GrantedAuthority> authorities, String[] values) {
    for (String value : values) {
      String authority = parseAuthority(value);
      // Ignore the empty parts
      if (!authority.isEmpty()) {
        authorities.add(new SimpleGrantedAuthority(authority));
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

    // Add prefix only if the authority is not empty
    if (!authority.isEmpty()) {
      if (uppercase) {
        return (this.prefix.trim() + authority).replaceAll("[\\s_]+", "_").toUpperCase();
      } else {
        return (this.prefix.trim() + authority).replaceAll("[\\s_]+", "_");
      }
    }

    return authority;
  }
}
