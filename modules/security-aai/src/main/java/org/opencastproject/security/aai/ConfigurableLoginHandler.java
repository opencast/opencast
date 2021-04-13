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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

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
import org.opencastproject.userdirectory.api.UserReferenceProvider;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * This configurable implementation of the ShibbolethLoginHandler uses the UserReferenceProvider interface to create
 * and update Opencast reference users provided and authenticated by an external identity provider.
 * Note that this configurable implementation aims at requiring the minimum number of Shibboleth attributes
 * to make Opencast work with most Shibboleth-based Authentication and Authorization Infrastractures (AAI).
 */
public class ConfigurableLoginHandler implements ShibbolethLoginHandler, RoleProvider, ManagedService {

  /** Name of the configuration property that specifies whether AAI authencation is enabled. This is used to avoid log
      messages in case the module is included in the distribution but not in use */
  private static final String CFG_AAI_ENABLED_KEY = "enabled";

  /** Default value of the configuration property CFG_AAI_ENABLED_KEY **/
  private static final boolean CFG_AAI_ENABLED_DEFAULT = false;

  /** Name of the configuration property specifying the ID of the bootstrap user. The bootstrap user
    * will be assigned the global admin role */
  private static final String CFG_BOOTSTRAP_USER_ID_KEY = "bootstrap.user.id";

  /** Shibboleth header configuration */

  /** Name of the configuration property specifying the name of the HTTP request header where the users name can be
      extracted */
  private static final String CFG_HEADER_GIVEN_NAME_KEY = "header.given_name";

  /** Name of the configuration property specifying the name of the HTTP request header where the users surname can be
      extracted */
  private static final String CFG_HEADER_SURNAME_KEY = "header.surname";

  /** Name of the configuration property specifying the name of the HTTP request header where the users e-mail can be
      extracted */
  private static final String CFG_HEADER_MAIL_KEY = "header.mail";

  /** Name of the optional configuration property specifying a list of home organizations */
  private static final String CFG_HEADER_HOME_ORGANIZATION_KEY = "header.home_organization";

  /** Name of the optional configuration property specifying the name of the HTTP request header where affiliations
      can be extracted */
  private static final String CFG_HEADER_AFFILIATION_KEY = "header.affiliation";

  /** Shibboleth roles configuration */

  /**
   * Name of the configuration property that specifies the prefix of the user role uniquely identifying a Shibboleth
   * authenticated users. The user role will be of the form ROLE_USER_PREFIX + SHIBBOLETH_UNIQUE_ID
   */
  private static final String CFG_ROLE_USER_PREFIX_KEY = "role.user.prefix";

  /** Default value of configuration property CFG_ROLE_USER_PREFIX_KEY */
  private static final String CFG_ROLE_USER_PREFIX_DEFAULT = "ROLE_AAI_USER_";

  /** The organization membership role indicates that a user belong to a specific AAI home organization.
      It has the from: valueOf(role.organization.prefix) + homeOrganization + valueOf(role.organization.suffix) */

  /** Name of the configuration property that specifies the prefix of the organization membership role */
  private static final String CFG_ROLE_ORGANIZATION_PREFIX_KEY = "role.organization.prefix";

  /** Default value of configuration property CFG_ROLE_ORGANIZATION_PREFIX_KEY */
  private static final String CFG_ROLE_ORGANIZATION_PREFIX_DEFAULT = "ROLE_AAI_ORG_";

  /** Name of the configuration property that specifies the prefix of the organization membership role */
  private static final String CFG_ROLE_ORGANIZATION_SUFFIX_KEY = "role.organization.suffix";

  /** Default value of configuration property CFG_ROLE_ORGANIZATION_SUFFIX_KEY */
  private static final String CFG_ROLE_ORGANIZATION_SUFFIX_DEFAULT = "_MEMBER";

  /** Name of the configuration property that specifies the name of the role assigned to all Shibboleth authenticated
      users, i.e. members of an Sibboleth federation */
  private static final String CFG_ROLE_FEDERATION_KEY = "role.federation";

  /** Default value of the configuration property CFG_ROLE_FEDERATION_KEY */
  private static final String CFG_ROLE_FEDERATION_DEFAULT = "ROLE_AAI_USER";

  /**
   * Name of the configuration property that specifies the prefix of the affiliation role for Shibboleth
   * authenticated users. The role will be of the form ROLE_AFFILIATION_PREFIX + SHIBBOLETH_EDUPERSONAFFILIATION
   */
  private static final String CFG_ROLE_AFFILIATION_PREFIX_KEY = "role.affiliation.prefix";

  /** Default value of configuration property CFG_ROLE_USER_PREFIX_KEY */
  private static final String CFG_ROLE_AFFILIATION_PREFIX_DEFAULT = "ROLE_AAI_USER_AFFILIATION_";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableLoginHandler.class);

  /** The user reference provider */
  private UserReferenceProvider userReferenceProvider = null;

  /** The security service */
  private SecurityService securityService = null;

  /** Whether the configurable Shibboleth login handler */
  private boolean enabled = CFG_AAI_ENABLED_DEFAULT;

  /** The ID of the bootstrap user if configured */
  private String bootstrapUserId = null;

  /** Header to extract the given name (first name) from */
  private String headerGivenName = null;

  /** Header to extract to surname */
  private String headerSurname = null;

  /** Header to extract the e-mail address */
  private String headerMail = null;

  /** Header to extract the home organization */
  private String headerHomeOrganization = null;

  /** Header to extract the affiliation */
  private String headerAffiliation = null;

  /** Role assigned to all Shibboleth authenticated users */
  private String roleFederationMember = CFG_ROLE_FEDERATION_DEFAULT;

  /** Prefix of unique Shibboleth user role */
  private String roleUserPrefix = CFG_ROLE_USER_PREFIX_DEFAULT;

  /** Prefix of the home organization membership role */
  private String roleOrganizationPrefix = CFG_ROLE_ORGANIZATION_PREFIX_DEFAULT;

  /** Suffix of the home organization membership role */
  private String roleOrganizationSuffix = CFG_ROLE_ORGANIZATION_SUFFIX_DEFAULT;

  /** Prefix of the affiliation role */
  private String roleAffiliationPrefix = CFG_ROLE_AFFILIATION_PREFIX_DEFAULT;

  /*
   * It is the bundle kernel what will need to instantiate the ConfigurableLoginHandler
   * since it is wired using Spring Security.
   * Since Shibboleth support is supposed to be an optional extension of the bundle kernel,
   * we implement this as fragment bundle.
   * The use of the Service Component Runtime (SCR) would require us to declare this bundle as service
   * component in kernel which we don't want since it is optional.
   * To make us visible to the config admin and take advantage of the ManagedService mechanism, we
   * register us as ManagedService in the constructor.
   * An alternative solution would be to include the manifest of all fragments in kernel, i.e.
   * by specifying OSGI-INF/*.xml as service component in kernel.
   */
  public ConfigurableLoginHandler() {
    BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    registerAsManagedService(bundleContext);
  }

  protected ConfigurableLoginHandler(BundleContext bundleContext) {
    registerAsManagedService(bundleContext);
  }

  private void registerAsManagedService(BundleContext bundleContext) {
    Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put("service.pid", this.getClass().getName());
    bundleContext.registerService(ManagedService.class.getName(), this, properties);
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }

    String cfgEnabled = StringUtils.trimToNull((String) properties.get(CFG_AAI_ENABLED_KEY));
    if (cfgEnabled != null) {
      enabled = BooleanUtils.toBoolean(cfgEnabled);
    }

    if (enabled) {
      logger.info("AAI login handler is enabled.");
    } else {
      logger.info("AAI login handler is disabled.");
      return;
    }

    String cfgBootstrapUserId = StringUtils.trimToNull((String) properties.get(CFG_BOOTSTRAP_USER_ID_KEY));
    if (cfgBootstrapUserId != null) {
      bootstrapUserId = cfgBootstrapUserId;
      logger.warn("AAI User ID '{}' is configured as AAI boostrap user. You want to disable this after bootstrapping.",
              bootstrapUserId);
    } else {
      bootstrapUserId = null;
    }

    /* Shibboleth header configuration */

    String cfgGivenName = StringUtils.trimToNull((String) properties.get(CFG_HEADER_GIVEN_NAME_KEY));
    if (cfgGivenName != null) {
      headerGivenName = cfgGivenName;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_GIVEN_NAME_KEY, headerGivenName);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_GIVEN_NAME_KEY);
    }

    String cfgSurname = StringUtils.trimToNull((String) properties.get(CFG_HEADER_SURNAME_KEY));
    if (cfgSurname != null) {
      headerSurname = cfgSurname;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_SURNAME_KEY, headerSurname);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_SURNAME_KEY);
    }

    String cfgMail = StringUtils.trimToNull((String) properties.get(CFG_HEADER_MAIL_KEY));
    if (cfgMail != null) {
      headerMail = cfgMail;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_MAIL_KEY, headerMail);
    } else {
      logger.error("Header '{}' is not configured ", CFG_HEADER_MAIL_KEY);
    }

    String cfgHomeOrganization = StringUtils.trimToNull((String) properties.get(CFG_HEADER_HOME_ORGANIZATION_KEY));
    if (cfgHomeOrganization != null) {
      headerHomeOrganization = cfgHomeOrganization;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_HOME_ORGANIZATION_KEY, headerHomeOrganization);
    } else {
      logger.warn("Optional header '{}' is not configured ", CFG_HEADER_HOME_ORGANIZATION_KEY);
    }

    String cfgAffiliation = StringUtils.trimToNull((String) properties.get(CFG_HEADER_AFFILIATION_KEY));
    if (cfgAffiliation != null) {
      headerAffiliation = cfgAffiliation;
      logger.info("Header '{}' set to '{}'", CFG_HEADER_AFFILIATION_KEY, headerAffiliation);
    } else {
      logger.warn("Optional header '{}' is not configured ", CFG_HEADER_AFFILIATION_KEY);
    }

    /* Shibboleth roles configuration */

    String cfgRoleFederationMember = StringUtils.trimToNull((String) properties.get(CFG_ROLE_FEDERATION_KEY));
    if (cfgRoleFederationMember != null) {
      roleFederationMember = cfgRoleFederationMember;
      logger.info("AAI federation membership role '{}' set to '{}'", CFG_ROLE_FEDERATION_KEY,
              roleFederationMember);
    } else {
      roleFederationMember = CFG_ROLE_FEDERATION_DEFAULT;
      logger.info("AAI federation membership role '{}' is not configured, using default '{}'",
              CFG_ROLE_FEDERATION_KEY, roleFederationMember);
    }

    String cfgRoleUserPrefix = StringUtils.trimToNull((String) properties.get(CFG_ROLE_USER_PREFIX_KEY));
    if (cfgRoleUserPrefix != null) {
      roleUserPrefix = cfgRoleUserPrefix;
      logger.info("AAI user role prefix '{}' set to '{}'", CFG_ROLE_USER_PREFIX_KEY, roleUserPrefix);
    } else {
      roleUserPrefix = CFG_ROLE_USER_PREFIX_DEFAULT;
      logger.info("AAI user role prefix '{}' is not configured, using default '{}'", CFG_ROLE_USER_PREFIX_KEY,
              roleUserPrefix);
    }

    String cfgRoleOrganizationPrefix = StringUtils.trimToNull((String) properties.get(
            CFG_ROLE_ORGANIZATION_PREFIX_KEY));
    if (cfgRoleOrganizationPrefix != null) {
      roleOrganizationPrefix = cfgRoleOrganizationPrefix;
      logger.info("AAI organization membership role prefix '{}' set to '{}'", CFG_ROLE_ORGANIZATION_PREFIX_KEY,
              cfgRoleOrganizationPrefix);
    } else {
      roleOrganizationPrefix = CFG_ROLE_ORGANIZATION_PREFIX_DEFAULT;
      logger.info("AAI organization membership role prefix '{}' is not configured, using default '{}'",
              CFG_ROLE_ORGANIZATION_PREFIX_KEY, roleOrganizationPrefix);
    }

    String cfgRoleOrganizationSuffix = StringUtils.trimToNull((String) properties.get(
            CFG_ROLE_ORGANIZATION_SUFFIX_KEY));
    if (cfgRoleOrganizationSuffix != null) {
      roleOrganizationSuffix = cfgRoleOrganizationSuffix;
      logger.info("AAI organization membership role suffix '{}' set to '{}'", CFG_ROLE_ORGANIZATION_SUFFIX_KEY,
              cfgRoleOrganizationSuffix);
    } else {
      roleOrganizationSuffix = CFG_ROLE_ORGANIZATION_SUFFIX_DEFAULT;
      logger.info("AAI organization membership role suffix '{}' is not configured, using default '{}'",
              CFG_ROLE_ORGANIZATION_SUFFIX_KEY, roleOrganizationSuffix);
    }

    String cfgRoleAffiliationPrefix = StringUtils.trimToNull((String) properties.get(
            CFG_ROLE_AFFILIATION_PREFIX_KEY));
    if (cfgRoleAffiliationPrefix != null) {
      roleAffiliationPrefix = cfgRoleAffiliationPrefix;
      logger.info("AAI affiliation role prefix '{}' set to '{}'", CFG_ROLE_AFFILIATION_PREFIX_KEY,
              cfgRoleAffiliationPrefix);
    } else {
      roleAffiliationPrefix = CFG_ROLE_AFFILIATION_PREFIX_DEFAULT;
      logger.info("AAI affiliation role prefix '{}' is not configured, using default '{}'",
              CFG_ROLE_AFFILIATION_PREFIX_KEY, roleAffiliationPrefix);
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
      throw new UsernameNotFoundException("User reference '" + id + "' was not found");
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
  public void setUserReferenceProvider(UserReferenceProvider userReferenceProvider) {
    this.userReferenceProvider = userReferenceProvider;
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
    roles.add(new JpaRole(roleUserPrefix + id, organization));
    roles.add(new JpaRole(organization.getAnonymousRole(), organization));
    if (headerHomeOrganization != null) {
      String homeOrganization = request.getHeader(headerHomeOrganization);
      roles.add(new JpaRole(roleOrganizationPrefix + homeOrganization + roleOrganizationSuffix, organization));
    }
    if (StringUtils.equals(id, bootstrapUserId)) {
      roles.add(new JpaRole(GLOBAL_ADMIN_ROLE, organization));
    }
    if (headerAffiliation != null) {
      String affiliation = request.getHeader(headerAffiliation);
      if (affiliation != null) {
        List<String> affiliations = Arrays.asList(affiliation.split(";"));
        for (String eachAffiliation : affiliations) {
          roles.add(new JpaRole(roleAffiliationPrefix + eachAffiliation, organization));
        }
      }
    }

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
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    HashSet<Role> roles = new HashSet<>(2);
    final String[] roleNames = new String[] {roleFederationMember, organization.getAnonymousRole()};
    for (String name: roleNames) {
      if (like(name, query)) {
        roles.add(new JaxbRole(name, organization));
      }
    }
    return offsetLimitCollection(offset, limit, roles).iterator();
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
