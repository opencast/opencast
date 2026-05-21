/*
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * A OAuth consumer details service with multiple consumers. UserDetailsService is used for delegating user
 * lookup requests.
 */
@Component(
    immediate = true,
        service = { OAuthConsumerDetailsService.class },
    property = {
        "service.description=OAuth consumer details service"
    }
)
public class OAuthConsumerDetailsService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OAuthConsumerDetailsService.class);

  /** The prefix of the key to look up a consumer name. */
  private static final String CONSUMER_NAME_PREFIX = "oauth.consumer.name.";

  /** The prefix of the key to look up a consumer key. */
  private static final String CONSUMER_KEY_PREFIX = "oauth.consumer.key.";

  /** The prefix of the key to look up a consumer secret. */
  private static final String CONSUMER_SECRET_PREFIX = "oauth.consumer.secret.";

  /** A map associating consumer keys to secrets. */
  private Map<String, String> consumers = new HashMap<>();

  @Activate
  public void activate(ComponentContext cc) throws ConfigurationException {
    logger.debug("Updating OAuthConsumerDetailsService");

    consumers.clear();

    Dictionary<String, Object> properties = cc.getProperties();

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
                "Unable to configure OAuth consumer with name'{}' because the name, "
                        + "key or secret is missing. Stopping to look for new consumers.",
                consumerName);
        break;
      }

      consumers.put(consumerKey, consumerSecret);
    }
  }

  public String getConsumerSecret(String consumerKey) {
    return consumers.get(consumerKey);
  }

}
