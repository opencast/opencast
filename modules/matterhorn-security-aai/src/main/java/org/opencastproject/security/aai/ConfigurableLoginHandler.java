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
package org.opencastproject.security.aai;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUserReference;
import org.opencastproject.security.shibboleth.ShibbolethLoginHandler;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.userdirectory.JpaUserReferenceProvider;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * This configurable implementation of the ShibbolethLoginHandler uses the JpaUserReferenceProvider to create and
 * update users that are external to Opencast.
 * Note that this configurable implementation aims at requiring the minimum number of Shibboleth attributes
 * to make Opencast work with most Shibboleth-based Authentication and Authorization Infrastractures (AAI).
 */
public class ConfigurableLoginHandler implements ShibbolethLoginHandler, RoleProvider, ManagedService {

  /** Is AAI enabled? This is used to avoid log messages in case the module is included in the 
      distribution but not in use */
  private static final String CFG_AAI_ENABLED = "enabled";

  /** Default value of CFG_AAI_ENABLED **/
  private static final boolean CFG_AAI_ENABLED_DEFAULT = false;

  /** Shibboleth header configuration */

  /** Configuration property specifying the name of the HTTP request header where the users name can be extracted */
  private static final String CFG_HEADER_GIVEN_NAME = "header.given_name";

  /** Configuration property specifying the name of the HTTP request header where the users surname can be extracted */
  private static final String CFG_HEADER_SURNAME = "header.surname";

  /** Configuration property specifying the name of the HTTP request header where the users e-mail can be extracted */
  private static final String CFG_HEADER_MAIL = "header.mail";

  /** Optional configuration property specifying a list of home organizations */
  private static final String CFG_HEADER_ORGANIZATION = "header.organization";

  /**
   * Shibboleth roles configuration
   * At login time, the Shibboleth login handler assigns some basic roles to authenticated users.
   */

  /** Role assigned to all Shibboleth authenticated users, i.e. members of an Sibboleth federation */
  private static final String CFG_ROLE_FEDERATION_MEMBER = "role.federation";

  /** Default value of CFG_ROLE_FEDERATION_MEMBER */
  private static final String CFG_ROLE_FEDERATION_MEMBER_DEFAULT = "ROLE_AAI_USER";

  /**
   * Prefix of role uniquely identifying a Shibboleth authenticated users
   * The user role will be of the form CFG_USER_ROLE_PREFIX + SHIBBOLETH_ID
   */
  private static final String CFG_USER_ROLE_PREFIX = "role.prefix";

  /** Role assigned to all Shibboleth authenticated users */
  private static final String CFG_USER_ROLE_PREFIX_DEFAULT = "ROLE_AAI_USER_";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableLoginHandler.class);

  /** The user reference provider */
  private JpaUserReferenceProvider userReferenceProvider = null;

  /** The group role provider */
  private JpaGroupRoleProvider groupRoleProvider = null;

  /** The security service */
  private SecurityService securityService = null;

  /** Whether the configurable Shibboleth login handler */
  private boolean enabled = CFG_AAI_ENABLED_DEFAULT;

  /** Header to extract the given name (first name) from */
  private String headerGivenName = null;

  /** Header to extract to surname */
  private String headerSurname = null;

  /** Header to extract the e-mail address */
  private String headerMail = null;

  /** Header to extract the organization */
  private String headerOrganization = null;

  /** Role assigned to all Shibboleth authenticated users */
  private String roleFederationMember = CFG_ROLE_FEDERATION_MEMBER_DEFAULT;

  /** Prefix of unique Shibboleth user role */
  private String userRolePrefix = CFG_USER_ROLE_PREFIX_DEFAULT;

  public ConfigurableLoginHandler() {
    BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    bundleContext.registerService(RoleProvider.class.getName(), this, null);
  }

  protected ConfigurableLoginHandler(BundleContext bundleContext) {
    bundleContext.registerService(RoleProvider.class.getName(), this, null);
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }

    String cfgEnabled = StringUtils.trimToNull((String) properties.get(CFG_AAI_ENABLED));
    if (cfgEnabled != null) {
      enabled = BooleanUtils.toBoolean(cfgEnabled);
      logger.info("AAI login handler is enabled.");
    } else {
      logger.info("AAI login handler is disabled.");
      return;
    }

    /* Shibboleth header configuration */

    String cfgOrganization = StringUtils.trimToNull((String) properties.get(CFG_HEADER_ORGANIZATION));
    if (cfgOrganization != null) {
      headerOrganization = cfgOrganization;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_ORGANIZATION, headerOrganization);
    } else {
      logger.warn("Header '{}' is not configured ", CFG_HEADER_ORGANIZATION);
    }

    String cfgGivenName = StringUtils.trimToNull((String) properties.get(CFG_HEADER_GIVEN_NAME));
    if (cfgGivenName != null) {
      headerGivenName = cfgGivenName;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_GIVEN_NAME, headerGivenName);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_GIVEN_NAME);
    }

    String cfgSurname = StringUtils.trimToNull((String) properties.get(CFG_HEADER_SURNAME));
    if (cfgSurname != null) {
      headerSurname = cfgSurname;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_SURNAME, headerSurname);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_SURNAME);
    }

    String cfgMail = StringUtils.trimToNull((String) properties.get(CFG_HEADER_MAIL));
    if (cfgMail != null) {
      headerMail = cfgMail;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_MAIL, headerMail);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_MAIL);
    }

    /* Shibboleth user roles configuration */

    String cfgRoleFederationMember = StringUtils.trimToNull((String) properties.get(CFG_ROLE_FEDERATION_MEMBER));
    if (cfgRoleFederationMember != null) {
      roleFederationMember = cfgRoleFederationMember;
      logger.info("Header '{}' set to '{}'", CFG_ROLE_FEDERATION_MEMBER, roleFederationMember);
    } else {
      roleFederationMember = CFG_ROLE_FEDERATION_MEMBER_DEFAULT;
      logger.error("Header '{}' is not configured, using '{}'", CFG_ROLE_FEDERATION_MEMBER, roleFederationMember);
    }

    String cfgUserRolePrefix = StringUtils.trimToNull((String) properties.get(CFG_USER_ROLE_PREFIX));
    if (cfgUserRolePrefix != null) {
      userRolePrefix = cfgUserRolePrefix;
      logger.info("Header '{}' set to '{}'", CFG_USER_ROLE_PREFIX, userRolePrefix);
    } else {
      userRolePrefix = CFG_USER_ROLE_PREFIX_DEFAULT;
      logger.error("Header '{}' is not configured, using '{}'", CFG_USER_ROLE_PREFIX, userRolePrefix);
    }
  }

  /**
   * Handle a new user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  @Override
  public void newUserLogin(String id, HttpServletRequest request) {
    String name = extractName(request);
    String email = extractEmail(request);
    Date loginDate = new Date();
    JpaOrganization organization = fromOrganization(securityService.getOrganization());

    // Compile the list of roles
    Set<JpaRole> roles = extractRoles(id, request);

    // Create the user reference
    JpaUserReference userReference = new JpaUserReference(id, name, email, MECH_SHIBBOLETH, loginDate, organization,
            roles);

    logger.debug("Shibboleth user '{}' logged in for the first time", id);
    userReferenceProvider.addUserReference(userReference, MECH_SHIBBOLETH);
  }

  /**
   * Handle an existing user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  @Override
  public void existingUserLogin(String id, HttpServletRequest request) {
    Organization organization = securityService.getOrganization();

    // Load the user reference
    JpaUserReference userReference = userReferenceProvider.findUserReference(id, organization.getId());
    if (userReference == null) {
      throw new IllegalStateException("User reference '" + id + "' was not found");
    }

    // Update the reference
    userReference.setName(extractName(request));
    userReference.setEmail(extractEmail(request));
    userReference.setLastLogin(new Date());
    Set<JpaRole> roles = extractRoles(id, request);
    userReference.setRoles(roles);

    logger.debug("Shibboleth user '{}' logged in", id);
    userReferenceProvider.updateUserReference(userReference);
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the user reference provider.
   *
   * @param userReferenceProvider
   *          the user reference provider
   */
  public void setUserReferenceProvider(JpaUserReferenceProvider userReferenceProvider) {
    this.userReferenceProvider = userReferenceProvider;
  }

  /**
   * Sets the group role provider.
   *
   * @param groupRoleProvider
   *          the group role provider.
   */
  public void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /**
   * Extracts the name from the request.
   *
   * @param request
   *          the request
   * @return the name
   */
  private String extractName(HttpServletRequest request) {
    String givenName = StringUtils.isBlank(request.getHeader(headerGivenName)) ? ""
            : new String(request.getHeader(headerGivenName).getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);
    String surname = StringUtils.isBlank(request.getHeader(headerSurname)) ? ""
            : new String(request.getHeader(headerSurname).getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);

    return StringUtils.join(new String[] { givenName, surname }, " ");
  }

  /**
   * Extracts the e-mail from the request.
   *
   * @param request
   *          the request
   * @return the e-mail address
   */
  private String extractEmail(HttpServletRequest request) {
    return request.getHeader(headerMail);
  }

  /**
   * Extracts the roles from the request.
   *
   * @param request
   *          the request
   * @return the roles
   */
  private Set<JpaRole> extractRoles(String id, HttpServletRequest request) {
    JpaOrganization organization = fromOrganization(securityService.getOrganization());
    Set<JpaRole> roles = new HashSet<JpaRole>();
    roles.add(new JpaRole(roleFederationMember, organization));
    roles.add(new JpaRole(userRolePrefix + id, organization));
    roles.add(new JpaRole(organization.getAnonymousRole(), organization));
    return roles;
  }

  /**
   * Creates a JpaOrganization from an organization
   *
   * @param org
   *          the organization
   */
  private JpaOrganization fromOrganization(Organization org) {
    if (org instanceof JpaOrganization) {
      return (JpaOrganization) org;
    } else {
      return new JpaOrganization(org.getId(), org.getName(), org.getServers(), org.getAdminRole(),
           org.getAnonymousRole(), org.getProperties());
    }
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    HashSet<Role> roles = new HashSet<Role>();
    roles.add(new JaxbRole(roleFederationMember, organization));
    roles.add(new JaxbRole(organization.getAnonymousRole(), organization));
    return roles.iterator();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    return Collections.emptyList();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query))
        foundRoles.add(role);
    }
    return offsetLimitCollection(offset, limit, foundRoles).iterator();
  }

  private <T> HashSet<T> offsetLimitCollection(int offset, int limit, HashSet<T> entries) {
    HashSet<T> result = new HashSet<T>();
    int i = 0;
    for (T entry : entries) {
      if (limit != 0 && result.size() >= limit)
        break;
      if (i >= offset)
        result.add(entry);
      i++;
    }
    return result;
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

}
