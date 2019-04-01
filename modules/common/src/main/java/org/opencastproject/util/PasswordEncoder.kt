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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util

import org.apache.commons.codec.digest.DigestUtils

/**
 * A password encoder that md5 hashes a password with a salt.
 */
object PasswordEncoder {

    /**
     * Encode a clear text password.
     *
     * @param clearText
     * the password
     * @param salt
     * the salt. See http://en.wikipedia.org/wiki/Salt_%28cryptography%29
     * @return the encoded password
     * @throws IllegalArgumentException
     * if clearText or salt are null
     */
    @Throws(IllegalArgumentException::class)
    fun encode(clearText: String?, salt: Any?): String {
        if (clearText == null || salt == null)
            throw IllegalArgumentException("clearText and salt must not be null")
        return DigestUtils.md5Hex("$clearText{$salt}")
    }

}
/**
 * Private constructor to disallow construction of this utility class.
 */
