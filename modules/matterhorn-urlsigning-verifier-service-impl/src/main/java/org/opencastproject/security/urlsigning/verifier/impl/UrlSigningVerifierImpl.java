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
package org.opencastproject.security.urlsigning.verifier.impl;

import org.opencastproject.security.urlsigning.verifier.UrlSigningVerifier;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

public class UrlSigningVerifierImpl implements UrlSigningVerifier, ManagedService {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(UrlSigningVerifierImpl.class);

  /** The prefix in the configuration file to define the id of the key. */
  public static final String ID_PREFIX = "id";
  /** The prefix in the configuration file to define the encryption key. */
  public static final String KEY_PREFIX = "key";

  private Properties keys = new Properties();

  @Override
  public ResourceRequest verify(String queryString, String clientIp, String baseUri) {
    return ResourceRequestUtil.resourceRequestFromQueryString(queryString, clientIp, baseUri, getKeys(), true);
  }

  @Override
  public ResourceRequest verify(String queryString, String clientIp, String baseUri, boolean strict) {
    return ResourceRequestUtil.resourceRequestFromQueryString(queryString, clientIp, baseUri, getKeys(), strict);
  }

  protected Properties getKeys() {
    return keys;
  }

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    logger.info("Updating UrlSigningVerifierImpl");

    // Clear the current set of keys
    keys.clear();

    if (properties == null) {
      logger.warn("UrlSigningVerifierImpl has no keys to verify urls");
      return;
    }

    String key = null;
    String keyId = null;

    int i = 1;
    while (true) {
      // Create the configuration prefixes
      key = new StringBuilder(KEY_PREFIX).append(".").append(i).toString();
      keyId = new StringBuilder(ID_PREFIX).append(".").append(i).toString();
      logger.debug("Looking for configuration of {}, {}", key, keyId);
      // Read the key and keyId
      String keyValue = StringUtils.trimToNull((String) properties.get(key));
      String keyIdValue = StringUtils.trimToNull((String) properties.get(keyId));

      // Has the url signing provider been fully configured
      if (keyValue == null || keyIdValue == null) {
        logger.debug(
                "Unable to configure key with id '{}' because the id or key is missing. Stopping to look for new keys.",
                keyIdValue);
        break;
      }

      keys.put(keyIdValue, keyValue);
      i++;
    }

    if (keys.size() == 0) {
      logger.info("UrlSigningVerifierImpl configured to not verify any urls.");
      return;
    }
    logger.info("Finished updating UrlSigningVerifierImpl");
  }
}
