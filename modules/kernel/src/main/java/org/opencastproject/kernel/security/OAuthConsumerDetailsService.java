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

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth.common.OAuthException;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.provider.BaseConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetailsService;
import org.springframework.security.oauth.provider.ExtraTrustConsumerDetails;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A OAuth consumer details service with multiple consumers. UserDetailsService is used for delegating user
 * lookup requests.
 */
public class OAuthConsumerDetailsService implements ConsumerDetailsService, UserDetailsService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OAuthConsumerDetailsService.class);

  /** The prefix of the key to look up a consumer name. */
  private static final String CONSUMER_NAME_PREFIX = "oauth.consumer.name.";

  /** The prefix of the key to look up a consumer key. */
  private static final String CONSUMER_KEY_PREFIX = "oauth.consumer.key.";

  /** The prefix of the key to look up a consumer secret. */
  private static final String CONSUMER_SECRET_PREFIX = "oauth.consumer.secret.";

  /** The user details service to use as a delegate for user lookups */
  private UserDetailsService delegate;

  /** A map associating consumer keys to OAuth consumers. */
  private Map<String, ConsumerDetails> consumers = new HashMap<>();

  /**
   * OSGi DI
   */
  public void setDelegate(UserDetailsService delegate) {
    this.delegate = delegate;
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    logger.debug("Updating OAuthConsumerDetailsService");

    consumers.clear();

    if (properties == null) {
      logger.warn("OAuthConsumerDetailsService has no configured OAuth consumers");
      return;
    }

    for (int i = 1; true; i++) {
      logger.debug("Looking for configuration of {}", CONSUMER_NAME_PREFIX + i);
      String consumerName = StringUtils.trimToNull((String) properties.get(CONSUMER_NAME_PREFIX + i));
      String consumerKey = StringUtils.trimToNull((String) properties.get(CONSUMER_KEY_PREFIX + i));
      String consumerSecret = StringUtils.trimToNull((String) properties.get(CONSUMER_SECRET_PREFIX + i));

      // Has the consumer been fully configured
      if (consumerName == null || consumerKey == null || consumerSecret == null) {
        logger.debug(
                "Unable to configure OAuth consumer with name'{}' because the name, key or secret is missing. Stopping to look for new consumers.",
                consumerName);
        break;
      }

      consumers.put(consumerKey, createConsumerDetails(consumerName, consumerKey, consumerSecret));
    }
  }

  /**
   * Creates a spring security consumer details object, suitable to achieve two-legged OAuth.
   *
   * @param consumerName
   *          the consumer name
   * @param consumerKey
   *          the consumer key
   * @param consumerSecret
   *          the consumer secret
   * @return the consumer details
   */
  private ExtraTrustConsumerDetails createConsumerDetails(String consumerName, String consumerKey,
          String consumerSecret) {
    SharedConsumerSecretImpl secret = new SharedConsumerSecretImpl(consumerSecret);
    BaseConsumerDetails bcd = new BaseConsumerDetails();
    bcd.setConsumerKey(consumerKey);
    bcd.setConsumerName(consumerName);
    bcd.setSignatureSecret(secret);
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_OAUTH_USER"));
    bcd.setAuthorities(authorities);
    bcd.setRequiredToObtainAuthenticatedToken(false); // false for 2 legged OAuth
    return bcd;
  }

  @Override
  public ConsumerDetails loadConsumerByConsumerKey(String key) throws OAuthException {
    logger.debug("Request received to find consumer for consumerKey=[" + key + "]");
    ConsumerDetails consumer = consumers.get(key);
    if (consumer == null) {
      logger.debug("Result: No consumer found for [" + key + "]");
      throw new OAuthException("No consumer found for key " + key);
    }
    logger.debug("Result: Found consumer [" + consumer.getConsumerName() + "]");
    return consumer;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return delegate.loadUserByUsername(username);
  }
}
