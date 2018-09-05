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

import org.opencastproject.security.api.SecurityService;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.security.oauth.provider.token.OAuthAccessProviderToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;

/**
 * Callback interface for handing authentication details that are used when an authenticated request for a protected
 * resource is received.
 */
public class LtiLaunchAuthenticationHandler
        implements org.springframework.security.oauth.provider.OAuthAuthenticationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LtiLaunchAuthenticationHandler.class);

  /** The Http request parameter, sent by the LTI consumer, containing the user ID. */
  public static final String LTI_USER_ID_PARAM = "user_id";

  /** The http request paramater containing the Consumer GUI **/
  public static final String LTI_CONSUMER_GUID = "tool_consumer_instance_guid";

  /** LTI field containing a comma delimeted list of roles */
  public static final String ROLES = "roles";

  /** The LTI field containing the context_id */
  public static final String CONTEXT_ID = "context_id";

  /** The prefix for LTI user ids */
  public static final String LTI_USER_ID_PREFIX = "lti";

  /** The delimiter to use in generated OAUTH id's **/
  public static final String LTI_ID_DELIMITER = ":";

  /** The Opencast Role for OAUTH users **/
  private static final String ROLE_OAUTH_USER = "ROLE_OAUTH_USER";

  /** The default context for LTI x **/
  private static final String DEFAULT_CONTEXT = "LTI";

  /** The default learner for LTI **/
  private static final String DEFAULT_LEARNER = "USER";

  /** The user details service */
  protected UserDetailsService userDetailsService;

  /** the Security Service **/
  protected SecurityService securityService;

  /** list of keys that will be highly */
  protected List<String> highlyTrustedKeys;

  /** Pattern that matches user names, which should not be trusted from LTI */
  protected Pattern untrustedUsersPattern;

  /**
   * Constructs a new LTI authentication handler, using the supplied user details service for performing user lookups.
   *
   * @param userDetailsService
   *          the user details service used to map user identifiers to more detailed information
   */
  public LtiLaunchAuthenticationHandler(UserDetailsService userDetailsService) {
    this(userDetailsService, null, new ArrayList<String>());
  }

  /**
   * Constructor for a LTI authentication handler that includes a list of highly trusted keys
   *
   * @param userDetailsService
   * @param highlyTrustedkeys
   */
  public LtiLaunchAuthenticationHandler(UserDetailsService userDetailsService, SecurityService securityService,
          List<String> highlyTrustedkeys) {
    this(userDetailsService, securityService, highlyTrustedkeys, null);
  }

  /**
   * Full constructor for a LTI authentication handler that includes a list of highly trusted keys with exceptions.
   *
   * @param userDetailsService
   * @param highlyTrustedkeys
   * @param untrustedUsersPattern
   */
  public LtiLaunchAuthenticationHandler(UserDetailsService userDetailsService, SecurityService securityService,
          List<String> highlyTrustedkeys, String untrustedUsersPattern) {
    this.userDetailsService = userDetailsService;
    this.securityService = securityService;
    this.highlyTrustedKeys = highlyTrustedkeys;

    if (StringUtils.isNotBlank(untrustedUsersPattern)) {
      try {
        this.untrustedUsersPattern = Pattern.compile(untrustedUsersPattern);
      } catch (PatternSyntaxException e) {
        logger.warn("The configured untrusted users pattern is invalid - disabling checks:", e);
      }
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
    if (highlyTrustedKeys.contains(oaAuthKey)) {
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
      if (untrustedUsersPattern != null && untrustedUsersPattern.matcher(ltiUsername).matches()) {
        // Do not trust the username
        logger.debug("{} is an untrusted username", ltiUsername);
      } else {
        username = ltiUsername;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("LTI user id is : {}", username);
    }

    UserDetails userDetails = null;
    Collection<GrantedAuthority> userAuthorities = null;
    try {
      userDetails = userDetailsService.loadUserByUsername(username);

      // userDetails returns a Collection<? extends GrantedAuthority>, which cannot be directly casted to a
      // Collection<GrantedAuthority>.
      // On the other hand, one cannot add non-null elements or modify the existing ones in a Collection<? extends
      // GrantedAuthority>. Therefore, we *must* instantiate a new Collection<GrantedAuthority> (an ArrayList in this
      // case) and populate it with whatever elements are returned by getAuthorities()
      userAuthorities = new HashSet<GrantedAuthority>(userDetails.getAuthorities());

      // we still need to enrich this user with the LTI Roles
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      enrichRoleGrants(roles, context, userAuthorities);
    } catch (UsernameNotFoundException e) {
      // This user is known to the tool consumer, but not to Opencast. Create a user "on the fly"
      userAuthorities = new HashSet<GrantedAuthority>();
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

    Authentication ltiAuth = new PreAuthenticatedAuthenticationToken(userDetails, authentication.getCredentials(),
            userAuthorities);
    SecurityContextHolder.getContext().setAuthentication(ltiAuth);
    return ltiAuth;
  }

  /**
   * Enrich A collection of role grants with specified LTI memberships
   *
   * @param roles
   * @param context
   * @param userAuthorities
   */
  private void enrichRoleGrants(String roles, String context, Collection<GrantedAuthority> userAuthorities) {
    // Roles could be a list
    if (roles != null) {
      List<String> roleList = Arrays.asList(roles.split(","));

      /* Use a generic context and learner if none is given: */
      context = StringUtils.isBlank(context) ? DEFAULT_CONTEXT : context;

      for (String learner : roleList) {

        /* Build the role */
        String role;
        if (StringUtils.isBlank(learner)) {
          role = context + "_" + DEFAULT_LEARNER;
        } else {
          role = context + "_" + learner;
        }

        /* Make sure to not accept ROLE_… */
        if (role.trim().toUpperCase().startsWith("ROLE_")) {
          logger.warn("Discarding attempt to acquire role “{}”", role);
          continue;
        }

        /* Add this role */
        logger.debug("Adding role: {}", role);
        userAuthorities.add(new SimpleGrantedAuthority(role));
      }
    }
  }

}
