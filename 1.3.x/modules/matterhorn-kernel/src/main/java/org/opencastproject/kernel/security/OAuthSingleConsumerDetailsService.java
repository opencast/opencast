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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth.common.OAuthException;
import org.springframework.security.oauth.common.signature.SharedConsumerSecret;
import org.springframework.security.oauth.provider.BaseConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetailsService;
import org.springframework.security.oauth.provider.ExtraTrustConsumerDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample OAuth consumer details service, hard coded to authenticate a consumer with the following information:
 * 
 * <li>key=consumerkey <li>name=consumername <li>secret=consumersecret
 * 
 * A UserDetailsService must be provided for delegating user lookup requests.
 * 
 * TODO: Replace the single, hard-coded consumer with database lookup from a separate service.
 * 
 */
public class OAuthSingleConsumerDetailsService implements ConsumerDetailsService, UserDetailsService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OAuthSingleConsumerDetailsService.class);

  /** The single hard-coded OAuth consumer. To be replaced later. */
  private ConsumerDetails consumer;

  /** The user details service to use as a delegate for user lookups */
  private UserDetailsService delegate;

  /**
   * Constructs this consumer detail service with a user details service to delegate user lookups.
   * 
   * @param delegate
   *          the user detail service to handle user lookups
   */
  public OAuthSingleConsumerDetailsService(UserDetailsService delegate) {
    this.delegate = delegate;
    consumer = createConsumerDetails("consumerkey", "consumername", "consumersecret");
  }

  private ExtraTrustConsumerDetails createConsumerDetails(String consumerKey, String consumerName, String consumerSecret) {
    SharedConsumerSecret secret = new SharedConsumerSecret(consumerSecret);
    BaseConsumerDetails bcd = new BaseConsumerDetails();
    bcd.setConsumerKey(consumerKey);
    bcd.setConsumerName(consumerName);
    bcd.setSignatureSecret(secret);
    List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    authorities.add(new GrantedAuthorityImpl("ROLE_OAUTH_USER"));
    bcd.setAuthorities(authorities);
    bcd.setRequiredToObtainAuthenticatedToken(false); // false for 2 legged OAuth
    return bcd;
  }

  @Override
  public ConsumerDetails loadConsumerByConsumerKey(String key) throws OAuthException {
    logger.debug("Request received to find consumer for consumerKey=[" + key + "]");
    if (!consumer.getConsumerKey().equals(key)) {
      logger.debug("Result: No consumer found for [" + key + "]");
      throw new OAuthException("No consumer found for key " + key);
    }
    logger.debug("Result: Found consumer [" + consumer.getConsumerName() + "]");
    return consumer;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
    return delegate.loadUserByUsername(username);
  }

}
