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

package org.opencastproject.security.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

/**
 * Class used for caching JWTs.
 */
public class CachedJWT {

  /** JWT's signature. */
  private final String signature;

  /** JWT's 'exp' claim. */
  private final Date expiresAt;

  /** Username extracted from the JWT. */
  private final String username;

  /**
   * Creates a cached JWT form a decoded JWT and a username.
   *
   * @param jwt The decoded JWT.
   * @param username The username extracted from the JWT.
   */
  public CachedJWT(DecodedJWT jwt, String username) {
    this.signature = jwt.getSignature();
    this.expiresAt = jwt.getExpiresAt();
    this.username = username;
  }

  /**
   * Returns <code>true</code> if the cached JWT expired, false otherwise.
   *
   * @return Boolean indicating whether the cached JWT has expired.
   */
  public boolean hasExpired() {
    return this.expiresAt != null && !this.expiresAt.after(new Date());
  }

  /**
   * Getter for the signature.
   *
   * @return The signature.
   */
  public String getSignature() {
    return signature;
  }

  /**
   * Getter for the expiry date.
   *
   * @return The expiry date.
   */
  public Date getExpiresAt() {
    return expiresAt;
  }

  /**
   * Getter for the username.
   *
   * @return The username.
   */
  public String getUsername() {
    return username;
  }

}
