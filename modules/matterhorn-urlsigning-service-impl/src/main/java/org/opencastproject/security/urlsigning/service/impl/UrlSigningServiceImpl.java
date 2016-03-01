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
package org.opencastproject.security.urlsigning.service.impl;

import static java.util.Objects.requireNonNull;
import static org.opencastproject.security.urlsigning.exception.UrlSigningException.urlNotSupported;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.urlsigning.common.Policy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UrlSigningServiceImpl implements UrlSigningService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(UrlSigningServiceImpl.class);

  /** List of registered signing providers */
  private final List<UrlSigningProvider> signingProviders = new CopyOnWriteArrayList<>();

  /** OSGi callback for registering {@link UrlSigningProvider} */
  void registerSigningProvider(final UrlSigningProvider provider) {
    signingProviders.add(provider);
    logger.info("{} registered", provider);
  }

  /** OSGi callback for unregistering {@link UrlSigningProvider} */
  void unregisterSigningProvider(final UrlSigningProvider provider) {
    signingProviders.remove(provider);
    logger.info("{} unregistered", provider);
  }

  @Override
  public boolean accepts(String baseUrl) {
    for (final UrlSigningProvider provider : signingProviders) {
      if (provider.accepts(baseUrl)) {
        logger.debug("{} accepted to sign base URL '{}'", provider, baseUrl);
        return true;
      }
    }

    logger.debug("No provider accepted to sign the URL '{}'", baseUrl);
    return false;
  }

  @Override
  public String sign(final String baseUrl, final Long validUntilDuration, final Long validFromDuration,
          final String ipAddr) throws UrlSigningException {
    requireNonNull(validUntilDuration);
    DateTime validUntil = new DateTime(DateTimeZone.UTC).plus(validUntilDuration * DateTimeConstants.MILLIS_PER_SECOND);
    DateTime validFrom = validFromDuration == null ? null : new DateTime(DateTimeZone.UTC).plus(validFromDuration
            * DateTimeConstants.MILLIS_PER_SECOND);
    return sign(baseUrl, validUntil, validFrom, ipAddr);
  }

  @Override
  public String sign(final String baseUrl, final DateTime validUntil, final DateTime validFrom, final String ipAddr)
          throws UrlSigningException {
    requireNonNull(baseUrl);
    requireNonNull(validUntil);

    final Policy policy = Policy.mkPolicyValidFromWithIP(baseUrl, validUntil, validFrom, ipAddr);

    for (final UrlSigningProvider provider : signingProviders) {
      if (provider.accepts(baseUrl)) {
        logger.debug("{} accepted to sign base URL '{}'", provider, baseUrl);
        return provider.sign(policy);
      }
    }

    logger.warn("No signing provider accepted to sign URL '{}'", baseUrl);
    throw urlNotSupported();
  }

}
