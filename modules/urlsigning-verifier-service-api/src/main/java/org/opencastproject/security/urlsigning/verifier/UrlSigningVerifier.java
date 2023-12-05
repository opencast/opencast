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
package org.opencastproject.security.urlsigning.verifier;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.urlsigning.common.ResourceRequest;

public interface UrlSigningVerifier {

  /**
   * Verify whether a request is valid.
   *
   * @param queryString
   *          The query string of the request that should include the key id, policy and signature.
   * @param clientIp
   *          The optional client ip of the machine making the request.
   * @param baseUrl
   *          The location of the resource being requested
   * @return A {@link ResourceRequest} object with the status of it being a valid request.
   */
  ResourceRequest verify(String queryString, String clientIp, String baseUrl) throws UrlSigningException;

  /**
   * Verify whether a request is valid.
   *
   * @param queryString
   *          The query string of the request that should include the key id, policy and signature.
   * @param clientIp
   *          The optional client ip of the machine making the request.
   * @param baseUrl
   *          The location of the resource being requested
   * @param strict
   *          Whether the full resource URI should be compared or only the path to the resource
   * @return A {@link ResourceRequest} object with the status of it being a valid request.
   */
  ResourceRequest verify(String queryString, String clientIp, String baseUrl, boolean strict)
          throws UrlSigningException;
}
