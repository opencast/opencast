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
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * This is a special implementation of the anonymous filter that prevents the filter from going with the anonymous user
 * if there is the potential for a real authentication coming later on.
 * <p>
 * The filter is needed in order to allow for security configurations where a url is open to the public but at the same
 * needs to support authorization to Opencast's {@link org.opencastproject.security.api.TrustedHttpClient}.
 */
public class TrustedAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

  /**
   * @see org.springframework.security.web.authentication.AnonymousAuthenticationFilter#applyAnonymousForThisRequest(javax
   *      .servlet.http.HttpServletRequest)
   */
  @Override
  @Deprecated
  protected boolean applyAnonymousForThisRequest(HttpServletRequest request) {
    return StringUtils.isBlank(request.getHeader(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER));
  }

}
