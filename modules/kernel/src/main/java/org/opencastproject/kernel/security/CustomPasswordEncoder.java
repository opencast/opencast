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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder using bcrypt for password hashing
 * Breaking change: no more backwards support for very old md5 based passwords.
 */
public class CustomPasswordEncoder implements PasswordEncoder {
  private Logger logger = LoggerFactory.getLogger(CustomPasswordEncoder.class);
  private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /**
   * Encode the raw password for storage using bcrypt.
   * @param rawPassword raw password to encrypt/hash
   * @return hashed password
   */
  @Override
  public String encode(CharSequence rawPassword) {
    return encoder.encode(rawPassword);
  }

  /**
   * Verify the encoded password obtained from storage matches the submitted raw
   * password after it too is encoded. Returns true if the passwords match, false if
   * they do not. The stored password itself is never decoded.
   *
   * @param rawPassword the raw password to encode and match
   * @param encodedPassword the encoded password from storage to compare with
   * @return true if the raw password, after encoding, matches the encoded password from storage
   */
  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    // Test old deprecated MD5 encoded hash
    if (encodedPassword.length() == 32) {
      logger.warn("User has old 32 char MD5 password, hash {}. Forcing user to reset password to use BCrypt.",
              encodedPassword);
      // Force user to set a new more secure password by returning false
      return false;
    }
    // Test BCrypt encoded hash
    logger.debug("Verifying bcrypt hash {}", encodedPassword);
    try {
      return StringUtils.startsWith(encodedPassword, "$") && encoder.matches(rawPassword, encodedPassword);
    } catch (IllegalArgumentException e) {
      logger.debug("bcrypt hash verification failed", e);
    }
    return false;
  }

}
