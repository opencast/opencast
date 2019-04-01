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
package org.opencastproject.urlsigning.utils

import org.apache.commons.codec.binary.Hex

import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A utility class to hash plain text with the SHA-256 algorithm.
 */
object SHA256Util {
    /** The algorithm to use to encode the HMAC.  */
    private val ALGORITHM = "HmacSHA256"

    /**
     * Create a SHA 256 digest string from a string and a secret key.
     *
     * @param plainText
     * The plaintext string to hash.
     * @param secretKey
     * The key to use to create the hash.
     * @return Returns a hash of the plain text hashed with the secret key.
     * @throws NoSuchAlgorithmException
     * Thrown if the algorithm is not supported on this platform.
     * @throws InvalidKeyException
     * Thrown if the secret key is invalid.
     * @throws UnsupportedEncodingException
     * Thrown if unable to convert bytes into a hex string.
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, UnsupportedEncodingException::class)
    fun digest(plainText: String, secretKey: String): String {
        val key = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(key)
        val bytes = mac.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Convert raw bytes to Hex
        val hexBytes = Hex().encode(bytes)

        // Covert array of Hex bytes to a String
        return String(hexBytes, "UTF-8")
    }
}
