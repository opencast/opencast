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
package org.opencastproject.security.urlsigning.service;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;

import org.joda.time.DateTime;

public interface UrlSigningService {

  /**
   * Returns true if the signing service accepts to sign the {@code baseUrl}.
   *
   * @param baseUrl
   *          The base URL of the resource that needs to be signed
   * @return True, if the signing service accepts to sign the URL; false otherwise.
   */
  boolean accepts(String baseUrl);

  /**
   * Create a secure signature for a resource by adding the validUntilDuration to the current time and optionally adding
   * the validFromDuration to the current time to create the available and expiry dates for the signature.
   *
   * @param baseUrl
   *          The required url that refers to the resource.
   * @param validUntilDuration
   *          The required number of seconds from now that the resource will expire.
   * @param validFromDuration
   *          The number of seconds after now that the resource will become available; may be {@code null} (optional).
   * @param ipAddr
   *          The IP address of the client that is allowed to view the resource; may be {@code null} (optional).
   * @return A query string signature for this resource.
   * @throws UrlSigningException
   *           Thrown if unable to sign the resource.
   */
  String sign(String baseUrl, Long validUntilDuration, Long validFromDuration, String ipAddr)
          throws UrlSigningException;

  /**
   * Create a secure signature for a resource using DateTime objects.
   *
   * @param baseUrl
   *          The required url that refers to the resource.
   * @param validUntil
   *          The required date and time this signature will expire.
   * @param validFrom
   *          The date and time the resource will become available; may be {@code null} (optional).
   * @param ipAddr
   *          The IP address of the client that is allowed to view the resource; may be {@code null} (optional).
   * @return A query string signature for this resource.
   * @throws UrlSigningException
   *           Thrown if unable to sign the resource.
   */
  String sign(String baseUrl, DateTime validUntil, DateTime validFrom, String ipAddr) throws UrlSigningException;
}
