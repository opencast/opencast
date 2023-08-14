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
package org.opencastproject.security.urlsigning.provider;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.urlsigning.common.Policy;

public interface UrlSigningProvider {

  /**
   * Returns true if the implementation accepts to sign the {@code baseUrl}.
   *
   * @param baseUrl
   *          The base URL of the resource that needs to be signed
   * @return True, if the provider accepts to sign the URL; false otherwise.
   */
  boolean accepts(String baseUrl);

  /**
   * Creates the necessary query string to sign a resource using the given {@link Policy}
   *
   * @param policy
   *          The {@link Policy} to sign.
   * @return A query string with the signed policy encoded ready for delivery
   * @throws UrlSigningException
   *           Thrown if unable to sign the url.
   */
  String sign(Policy policy) throws UrlSigningException;
}
