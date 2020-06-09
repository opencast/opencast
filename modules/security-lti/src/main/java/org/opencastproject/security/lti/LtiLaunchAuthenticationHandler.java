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

package org.opencastproject.security.lti;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUserReference;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.userdirectory.api.UserReferenceProvider;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth.provider.ConsumerAuthentication;
import org.springframework.security.oauth.provider.OAuthAuthenticationHandler;
import org.springframework.security.oauth.provider.token.OAuthAccessProviderToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.RollbackException;
import javax.servlet.http.HttpServletRequest;

/**
 * Callback interface for handing authentication details that are used when an authenticated request for a protected
 * resource is received.
 */
public class LtiLaunchAuthenticationHandler implements OAuthAuthenticationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LtiLaunchAuthenticationHandler.class);

  /** The Http request parameter, sent by the LTI consumer, containing the user ID. */
  private static final String LTI_USER_ID_PARAM = "user_id";

  /** The http request parameter containing the Consumer GUI **/
  private static final String LTI_CONSUMER_GUID = "tool_consumer_instance_guid";

  /** LTI field containing a comma delimited list of roles */
  private static final String ROLES = "roles";

  /** The LTI field containing the context_id */
  private static final String CONTEXT_ID = "context_id";

  /** The prefix for LTI user ids */
  private static final String LTI_USER_ID_PREFIX = "lti";

  /** The delimiter to use in generated OAUTH id's **/
  private static final String LTI_ID_DELIMITER = ":";

  /** The Opencast Role for OAUTH users **/
  private static final String ROLE_OAUTH_USER = "ROLE_OAUTH_USER";

  /** The default context for LTI **/
  private static final String DEFAULT_CONTEXT = "LTI";

  /** The default learner for LTI **/
  private static final String DEFAULT_LEARNER = "USER";

  /** The prefix of the key to look up a consumer key. */
  private static final String HIGHLY_TRUSTED_CONSUMER_KEY_PREFIX = "lti.oauth.highly_trusted_consumer_key.";

  /** The prefix of the key to look up a blacklisted user. */
  private static final String BLACKLIST_USER_PREFIX = "lti.blacklist.user.";

  /** The key to look up whether the admin user should be able to authenticate via LTI **/
  private static final String ALLOW_SYSTEM_ADMINISTRATOR_KEY = "lti.allow_system_administrator";

  /** The key to look up whether the digest user should be able to authenticate via LTI **/
  private static final String ALLOW_DIGEST_USER_KEY = "lti.allow_digest_user";

  /** The key to look up whether a JpaUserReferences should be created on login **/
  private static final String CREATE_JPA_USER_REFERENCE_KEY = "lti.create_jpa_user_reference";

  /** The security service */
  private SecurityService securityService = null;

  /** The user reference provider */
  private UserReferenceProvider userReferenceProvider = null;

  /** The role name of the user to add custom Roles to **/
  private static final String CUSTOM_ROLE_NAME = "lti.custom_role_name";

  /** A List of Roles to add to the user if he has the custom role name **/
  private static final String CUSTOM_ROLES = "lti.custom_roles";

  private String customRoleName = "";

  private String[] customRoles;

  /** The user details service */
  private UserDetailsService userDetailsService;

  /** OSGi component context */
  private ComponentContext componentContext;

  /** Set of OAuth consumer keys that are highly trusted */
  private Set<String> highlyTrustedConsumerKeys = new HashSet<>();

  /** Set of usernames that should not authenticated as themselves even if the OAuth consumer keys is trusted */
  private Set<String> usernameBlacklist = new HashSet<>();

  /** concurrent attemtps */
  private Map<String, Boolean> activePersistenceTransactions = new ConcurrentHashMap<>(128);

  /** Determines whether a JpaUserReference should be created on lti login */
  private boolean createJpaUserReference = false;

  public void setUserDetailsService(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
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
   * Sets the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected void activate(ComponentContext cc) {
    logger.info("Activating LtiLaunchAuthenticationHandler");
    componentContext = cc;
    Dictionary properties = cc.getProperties();

    logger.debug("Updating LtiLaunchAuthenticationHandler");

    highlyTrustedConsumerKeys.clear();
    usernameBlacklist.clear();

    if (properties == null) {
      logger.warn("LtiLaunchAuthenticationHandler is not configured");
      return;
    }

    // Highly trusted OAuth consumer keys
    for (int i = 1; true; i++) {
      logger.debug("Looking for configuration of {}", HIGHLY_TRUSTED_CONSUMER_KEY_PREFIX + i);
      String consumerKey = StringUtils.trimToNull((String) properties.get(HIGHLY_TRUSTED_CONSUMER_KEY_PREFIX + i));
      if (consumerKey == null) {
        break;
      }
      highlyTrustedConsumerKeys.add(consumerKey);
    }

    // User blacklist
    if (!BooleanUtils.toBoolean(StringUtils.trimToNull((String) properties.get(ALLOW_SYSTEM_ADMINISTRATOR_KEY)))) {
      String adminUsername = StringUtils.trimToNull((String)
              properties.get(SecurityConstants.GLOBAL_ADMIN_USER_PROPERTY));
      if (adminUsername != null) {
        usernameBlacklist.add(adminUsername);
      }
    }
    if (!BooleanUtils.toBoolean(StringUtils.trimToNull((String) properties.get(ALLOW_DIGEST_USER_KEY)))) {
      usernameBlacklist.add(SecurityUtil.getSystemUserName(componentContext));
    }

    for (int i = 1; true; i++) {
      logger.debug("Looking for configuration of {}", BLACKLIST_USER_PREFIX + i);
      String username = StringUtils.trimToNull((String) properties.get(BLACKLIST_USER_PREFIX + i));
      if (username == null) {
        break;
      }
      usernameBlacklist.add(username);
    }

    createJpaUserReference = BooleanUtils.toBooleanDefaultIfNull(
      BooleanUtils.toBooleanObject(StringUtils.trimToNull((String) properties.get(CREATE_JPA_USER_REFERENCE_KEY))),
      false);

    customRoleName = StringUtils.trimToNull((String) properties.get(CUSTOM_ROLE_NAME));
    if (customRoleName != null) {
      String custumRolesString = StringUtils.trimToNull((String) properties.get(CUSTOM_ROLES));
      customRoles = custumRolesString.split(",");
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.oauth.provider.OAuthAuthenticationHandler#createAuthentication(javax.servlet.http.HttpServletRequest,
   *      org.springframework.security.oauth.provider.ConsumerAuthentication,
   *      org.springframework.security.oauth.provider.token.OAuthAccessProviderToken)
   */
  @Override
  public Authentication createAuthentication(HttpServletRequest request, ConsumerAuthentication authentication,
          OAuthAccessProviderToken authToken) {
    // The User ID must be provided by the LTI consumer
    String userIdFromConsumer = request.getParameter(LTI_USER_ID_PARAM);

    if (StringUtils.isBlank(userIdFromConsumer)) {
      logger.warn("Received authentication request without user id ({})", LTI_USER_ID_PARAM);
      return null;
    }

    // Get the consumer guid if provided
    String consumerGUID = request.getParameter(LTI_CONSUMER_GUID);
    // This is an optional field, so it could be blank
    if (StringUtils.isBlank(consumerGUID)) {
      consumerGUID = "UnknownConsumer";
    }

    // We need to construct a complex ID to avoid confusion
    String username = LTI_USER_ID_PREFIX + LTI_ID_DELIMITER + consumerGUID + LTI_ID_DELIMITER + userIdFromConsumer;

    // if this is a trusted consumer we trust their details
    String oaAuthKey = request.getParameter("oauth_consumer_key");
    if (highlyTrustedConsumerKeys.contains(oaAuthKey)) {
      logger.debug("{} is a trusted key", oaAuthKey);

      // If supplied we use the human readable name coming from:
      //   1. ext_user_username    (optional Moodle-only field)
      //   2. lis_person_sourcedid (optional standard field)
      String ltiUsername = request.getParameter("ext_user_username");
      if (StringUtils.isBlank(ltiUsername)) {
        ltiUsername = request.getParameter("lis_person_sourcedid");
        if (StringUtils.isBlank(ltiUsername)) {
          // If no eid is set we use the supplied ID
          ltiUsername = userIdFromConsumer;
        }
      }

      // Check if the provided username should be trusted
      if (usernameBlacklist.contains(ltiUsername)) {
        // Do not trust the username
        logger.debug("{} is blacklisted", ltiUsername);
      } else {
        username = ltiUsername;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("LTI user id is : {}", username);
    }

    UserDetails userDetails;
    Collection<GrantedAuthority> userAuthorities;
    try {
      userDetails = userDetailsService.loadUserByUsername(username);

      // userDetails returns a Collection<? extends GrantedAuthority>, which cannot be directly casted to a
      // Collection<GrantedAuthority>.
      // On the other hand, one cannot add non-null elements or modify the existing ones in a Collection<? extends
      // GrantedAuthority>. Therefore, we *must* instantiate a new Collection<GrantedAuthority> (an ArrayList in this
      // case) and populate it with whatever elements are returned by getAuthorities()
      userAuthorities = new HashSet<>(userDetails.getAuthorities());

      // we still need to enrich this user with the LTI Roles
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      enrichRoleGrants(roles, context, userAuthorities);
    } catch (UsernameNotFoundException e) {
      logger.trace("This user is known to the tool consumer only. Creating an Opencast user on the fly.", e);

      userAuthorities = new HashSet<>();
      // We should add the authorities passed in from the tool consumer?
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      enrichRoleGrants(roles, context, userAuthorities);

      logger.info("Returning user with {} authorities", userAuthorities.size());

      userDetails = new User(username, "oauth", true, true, true, true, userAuthorities);
    }

    // All users need the OAUTH, USER and ANONYMOUS roles
    userAuthorities.add(new SimpleGrantedAuthority(ROLE_OAUTH_USER));
    userAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    userAuthorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));


    // Create/Update the user reference
    if (createJpaUserReference) {
      if (activePersistenceTransactions.putIfAbsent(username, Boolean.TRUE) != null) {
        logger.debug("Concurrent access of user {}. Ignoring.", username);
      } else {

        try {
          JpaOrganization organization = fromOrganization(securityService.getOrganization());
          JpaUserReference jpaUserReference = userReferenceProvider.findUserReference(username, organization.getId());
          Set<JpaRole> jpaRoles = new HashSet<>();
          for (GrantedAuthority authority : userAuthorities) {
            jpaRoles.add(new JpaRole(authority.getAuthority(), organization));
          }

          Date loginDate = new Date();

          // Create new JpaUserReference if none exists or update existing
          if (jpaUserReference == null) {
            final String jpaContext = Objects.toString(request.getParameter(CONTEXT_ID), DEFAULT_CONTEXT);
            JpaUserReference userReference = new JpaUserReference(username, username, null, jpaContext, loginDate,
                    organization, jpaRoles);
            userReferenceProvider.addUserReference(userReference, jpaContext);
          } else {
            jpaUserReference.setLastLogin(loginDate);
            jpaUserReference.setRoles(jpaRoles);
            userReferenceProvider.updateUserReference(jpaUserReference);
          }
        } catch (RollbackException e) {
          // In the unlikely case that concurrency happens at the same time on multiple servers, we catch the
          // rollback, assuming that the user is already persisted and let the user pass. Worst case, this means that
          // the user is only temporarily authenticated.
          logger.warn("Could not store reference since database was changed during update by another process", e);
        } finally {
          activePersistenceTransactions.remove(username);
        }
      }
    }
    //Create/Update UserReference End

    Authentication ltiAuth = new PreAuthenticatedAuthenticationToken(userDetails, authentication.getCredentials(),
            userAuthorities);
    SecurityContextHolder.getContext().setAuthentication(ltiAuth);
    return ltiAuth;
  }

  /**
   * Enrich A collection of role grants with specified LTI memberships.
   *
   * @param roles
   *          String of LTI roles.
   * @param context
   *          LTI context ID.
   * @param userAuthorities
   *          Collection to append to.
   */
  private void enrichRoleGrants(String roles, String context, Collection<GrantedAuthority> userAuthorities) {
    // Roles could be a list
    if (roles != null) {
      String[] roleList = roles.split(",");

      // Use a generic context and learner if none is given:
      context = StringUtils.isBlank(context) ? DEFAULT_CONTEXT : context;

      for (String learner : roleList) {
        // Build the role
        String role;
        String group;
        if (learner.equals(customRoleName)) {
          for (String rolename : customRoles) {
            userAuthorities.add(new SimpleGrantedAuthority(rolename));
          }
        }

        if (StringUtils.isBlank(learner)) {
          role = context + "_" + DEFAULT_LEARNER;
        } else {
          role = context + "_" + learner;
          group = "ROLE_GROUP_" + learner.toUpperCase();
          logger.debug("Adding group: {}", group);
        }

        // Make sure to not accept ROLE_…
        if (role.trim().toUpperCase().startsWith("ROLE_")) {
          logger.warn("Discarding attempt to acquire role “{}”", role);
          continue;
        }

        // Add this role
        logger.debug("Adding role: {}", role);
        userAuthorities.add(new SimpleGrantedAuthority(role));
      }
    }
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



}
