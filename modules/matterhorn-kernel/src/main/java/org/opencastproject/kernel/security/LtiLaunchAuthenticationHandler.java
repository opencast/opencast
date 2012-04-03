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
  
  public static final String LTI_ID_DELIMITER = ":";

  /** The user details service */
  protected UserDetailsService userDetailsService = null;

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

    // We need to construct a complex ID to avoid confusion
    // TODO if this is a trusted consumer we won't want to do this
    StringBuffer userId = new StringBuffer(LTI_USER_ID_PREFIX);
    if (StringUtils.isNotBlank(consumerGUID))
      userId.append(LTI_ID_DELIMITER).append(consumerGUID);
    userId.append(LTI_ID_DELIMITER).append(userIdFromConsumer);
    
    userIdFromConsumer = userId.toString();
    if (logger.isDebugEnabled()) {
      logger.debug("LTI user id is : {}", userIdFromConsumer);
    }
    
    UserDetails userDetails = null;
    Collection<GrantedAuthority> userAuthorities = null;
    try {
      userDetails = userDetailsService.loadUserByUsername(userIdFromConsumer);
      userAuthorities = userDetails.getAuthorities();
    } catch (UsernameNotFoundException e) {
      // This user is known to the tool consumer, but not to Matterhorn. Create a user "on the fly"
      userAuthorities = new HashSet<GrantedAuthority>();
      // We should add the authorities passed in from the tool consumer?
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_OAUTH_USER"));
      userAuthorities.add(new GrantedAuthorityImpl("ROLE_ANONYMOUS"));
      String roles = request.getParameter(ROLES);
      String context = request.getParameter(CONTEXT_ID);
      //Roles could be a list
      if (roles != null) {
        List<String> roleList = Arrays.asList(roles.split(","));
        for (int i = 0; i < roleList.size(); i++) {
          String role = context + "_" + roleList.get(i);
          logger.debug("adding role: {}", role);
          userAuthorities.add(new GrantedAuthorityImpl(role));
        }
      }
      
      userDetails = new User(userIdFromConsumer, "oauth", true, true, true, true, userAuthorities);
    }
    Authentication ltiAuth = new PreAuthenticatedAuthenticationToken(userDetails, authentication.getCredentials(),
            userAuthorities);
    SecurityContextHolder.getContext().setAuthentication(ltiAuth);
    return ltiAuth;
  }

}
