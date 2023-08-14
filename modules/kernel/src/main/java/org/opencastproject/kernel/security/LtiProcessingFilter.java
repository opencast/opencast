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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth.provider.filter.ProtectedResourceProcessingFilter;

/**
 * Overrides the default behavior of the OAuth ProtectedResourceProcessingFilter to keep the authentication returned by
 * <code>LtiLaunchAuthenticationHandler#createAuthentication(
 *  javax.servlet.http.HttpServletRequest,
 *  org.springframework.security.oauth.provider.ConsumerAuthentication,
 *  org.springframework.security.oauth.provider.token.OAuthAccessProviderToken)}
 * </code>
 */
public class LtiProcessingFilter extends ProtectedResourceProcessingFilter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LtiProcessingFilter.class);

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.oauth.provider.filter.OAuthProviderProcessingFilter#resetPreviousAuthentication(org.springframework.security.core.Authentication)
   */
  @Override
  protected void resetPreviousAuthentication(Authentication previousAuthentication) {
    logger.debug("Skip resetting the authentication");
  }
}
