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




import org.opencastproject.security.api.SecurityService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
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

import javax.servlet.http.HttpServletRequest;

/**
 * Callback interface for handing authentication details that are used when an authenticated request for a protected
 * resource is received.
 */
public class LtiLaunchAuthenticationHandler implements
        org.springframework.security.oauth.provider.OAuthAuthenticationHandler {



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

  /** The prefix for LTI user ids   */
  public static final String LTI_USER_ID_PREFIX = "lti";

  /** The delimiter to use in generated OAUTH id's **/
  public static final String LTI_ID_DELIMITER = ":";

  /** The Matterhorn Role for OAUTH users **/
  private static final String ROLE_OAUTH_USER = "ROLE_OAUTH_USER";

  /** The user details service */
  protected UserDetailsService userDetailsService = null;

  /** the Security Service **/
  protected SecurityService securityService;

  /** list of keys that will be highly */
  protected List<String> highlyTrustedKeys = new ArrayList<String>();

  /**
   * Constructs a new LTI authentication handler, using the supplied user details service for performing user lookups.
   *
   * @param userDetailsService
   *          the user details service used to map user identifiers to more detailed information
   */
  public LtiLaunchAuthenticationHandler(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  /**
   * Full constructor for a LTI authentication handler that includes a list of highly trusted keys
   * @param userDetailsService
   * @param highlyTrustedkeys
   */
  public LtiLaunchAuthenticationHandler(UserDetailsService userDetailsService, SecurityService securityService, List<String> highlyTrustedkeys) {
    this.userDetailsService = userDetailsService;
    this.securityService = securityService;
    this.highlyTrustedKeys = highlyTrustedkeys;
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

    // Get the comser guid if provided
    String consumerGUID = request.getParameter(LTI_CONSUMER_GUID);
    //This is an optional field it could be blank
    if (StringUtils.isBlank(consumerGUID)) {
      consumerGUID = "UknownConsumer";
    }

    //We need to construct a complex ID to avoid confusion
    userIdFromConsumer = LTI_USER_ID_PREFIX + LTI_ID_DELIMITER + consumerGUID + LTI_ID_DELIMITER + userIdFromConsumer;

    //if this is a trusted consumer we trust their details
    String oaAuthKey = request.getParameter("oauth_consumer_key");
    if (highlyTrustedKeys.contains(oaAuthKey)) {
      logger.debug("{} is a trusted key", oaAuthKey);
      //If supplied we use the human readable name
      String suppliedEid = request.getParameter("lis_person_sourcedid");
      //This is an optional field it could be null
      if (suppliedEid != null) {
        userIdFromConsumer = suppliedEid;
      } else {
        //if no eid is set we use the supplied ID
        userIdFromConsumer = request.getParameter(LTI_USER_ID_PARAM);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("LTI user id is : {}", userIdFromConsumer);
    }

    UserDetails userDetails = null;
    Collection<GrantedAuthority> userAuthorities = null;
    try {
      userDetails = userDetailsService.loadUserByUsername(userIdFromConsumer);
      userAuthorities = (Collection<GrantedAuthority>) userDetails.getAuthorities();
      //This list is potentially an modifiable collection
      userAuthorities = new HashSet<GrantedAuthority>(userAuthorities);
      //we still need to enrich this user with the LTI Roles
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      enrichRoleGrants(roles, context, userAuthorities);
    } catch (UsernameNotFoundException e) {
      // This user is known to the tool consumer, but not to Matterhorn. Create a user "on the fly"
      userAuthorities = new HashSet<GrantedAuthority>();
      // We should add the authorities passed in from the tool consumer?
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_ANONYMOUS"));
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      enrichRoleGrants(roles, context, userAuthorities);
      //all users need the OATH ROLE, the user Role and the Anon Role
      userAuthorities.add(new GrantedAuthorityImpl(ROLE_OAUTH_USER));
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_ANONYMOUS"));

      logger.info("Returning user with {} authorities", userAuthorities.size());

      userDetails = new User(userIdFromConsumer, "oauth", true, true, true, true, userAuthorities);
    }
    Authentication ltiAuth = new PreAuthenticatedAuthenticationToken(userDetails, authentication.getCredentials(),
            userAuthorities);
    SecurityContextHolder.getContext().setAuthentication(ltiAuth);
    return ltiAuth;
  }

  /**
   * Enrich A collection of role grants with specified LTI memberships
   * @param roles
   * @param context
   * @param userGrants
   */
  private void enrichRoleGrants(String roles, String context, Collection<GrantedAuthority> userAuthorities) {
  //Roles could be a list
      if (roles != null) {
        List<String> roleList = Arrays.asList(roles.split(","));
        for (int i = 0; i < roleList.size(); i++) {
          String role = context + "_" + roleList.get(i);
          logger.debug("adding role: {}", role);
          userAuthorities.add(new GrantedAuthorityImpl(role));
        }
      }
  }

}
