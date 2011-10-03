/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * A password encoder that md5 hashes a password with a salt.
 */
public final class PasswordEncoder {

  /**
   * Private constructor to disallow construction of this utility class.
   */
  private PasswordEncoder() {
  }

  /**
   * Encode a clear text password.
   * 
   * @param clearText
   *          the password
   * @param salt
   *          the salt. See {@link http://en.wikipedia.org/wiki/Salt_%28cryptography%29}
   * @return the encoded password
   * @throws IllegalArgumentException
   *           if clearText or salt are null
   */
  public static String encode(String clearText, Object salt) throws IllegalArgumentException {
    if (clearText == null || salt == null)
      throw new IllegalArgumentException("clearText and salt must not be null");
    return DigestUtils.md5Hex(clearText + "{" + salt.toString() + "}");
  }

}
