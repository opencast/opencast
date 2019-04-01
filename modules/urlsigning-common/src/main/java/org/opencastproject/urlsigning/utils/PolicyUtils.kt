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

import org.opencastproject.urlsigning.common.Policy

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.nio.charset.StandardCharsets
import java.util.TreeMap

/**
 * A Utility class to encode / decode Policy files from and to Base 64 and Json.
 */
object PolicyUtils {
    /** The JSON key for object that contains the date and ip conditions for the resource.  */
    private val CONDITION_KEY = "Condition"
    /** The JSON key for the date and time the resource will become available.  */
    private val DATE_GREATER_THAN_KEY = "DateGreaterThan"
    /** The JSON key for the date and time the resource will no longer be available.  */
    private val DATE_LESS_THAN_KEY = "DateLessThan"
    /** The JSON key for the IP address of the acceptable client.  */
    private val IP_ADDRESS_KEY = "IpAddress"
    /** The JSON key for the base url for the resource.  */
    private val RESOURCE_KEY = "Resource"
    /** The JSON key for the main object of the policy.  */
    private val STATEMENT_KEY = "Statement"

    /**
     * Encode a [String] into Base 64 encoding
     *
     * @param value
     * The [String] to encode into base 64 encoding
     * @return The [String] encoded into base 64.
     */
    fun base64Encode(value: String): String {
        return Base64.encodeBase64URLSafeString(value.toByteArray())
    }

    /**
     * Decode a [String] from Base 64 encoding
     *
     * @param value
     * The [String] to encode into Base 64
     * @return The [String] decoded from base 64.
     */
    fun base64Decode(value: String): String {
        return String(Base64.decodeBase64(value), StandardCharsets.UTF_8)
    }

    /**
     * Encode a byte[] into Base 64 encoding
     *
     * @param value
     * The byte[] to encode into base 64 encoding
     * @return The [String] encoded into base 64.
     */
    fun base64Encode(value: ByteArray): String {
        return String(Base64.encodeBase64URLSafe(value), StandardCharsets.UTF_8)
    }

    /**
     * Get a [Policy] from JSON data.
     *
     * @param policyJson
     * The [String] representation of the json.
     * @return A new [Policy] object populated from the JSON.
     */
    fun fromJson(policyJson: String): Policy {
        var jsonPolicy: JSONObject? = null
        val jsonParser = JSONParser()
        try {
            jsonPolicy = jsonParser.parse(policyJson) as JSONObject
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val statement = jsonPolicy!![STATEMENT_KEY] as JSONObject
        val resource = statement[RESOURCE_KEY].toString()
        val condition = statement[CONDITION_KEY] as JSONObject

        val lessThanString = condition[DATE_LESS_THAN_KEY].toString()
        val dateLessThan = DateTime(java.lang.Long.parseLong(lessThanString), DateTimeZone.UTC)

        val dateGreaterThan: DateTime?
        val greaterThanString = condition[DATE_GREATER_THAN_KEY]
        if (greaterThanString != null) {
            dateGreaterThan = DateTime(java.lang.Long.parseLong(greaterThanString.toString()), DateTimeZone.UTC)
        } else {
            dateGreaterThan = null
        }

        return Policy.mkPolicyValidFromWithIP(resource, dateLessThan, dateGreaterThan,
                condition[IP_ADDRESS_KEY] as String)
    }

    /**
     * Render a [Policy] into JSON.
     *
     * @param policy
     * The [Policy] to render into JSON.
     * @return The [JSONObject] representation of the [Policy].
     */
    fun toJson(policy: Policy): JSONObject {
        val policyJSON = JSONObject()

        val conditions = TreeMap<String, Any>()
        conditions[DATE_LESS_THAN_KEY] = policy.validUntil.millis
        if (policy.validFrom.isPresent) {
            conditions[DATE_GREATER_THAN_KEY] = policy.validFrom.get().millis
        }
        if (policy.clientIpAddress.isPresent) {
            conditions[IP_ADDRESS_KEY] = policy.clientIpAddress.get().hostAddress
        }
        val conditionsJSON = JSONObject()
        conditionsJSON.putAll(conditions)

        val statement = JSONObject()
        statement[RESOURCE_KEY] = policy.resource
        statement[CONDITION_KEY] = conditions

        policyJSON[STATEMENT_KEY] = statement

        return policyJSON
    }

    /**
     * Create a [Policy] in Json format and Base 64 encoded.
     *
     * @param encodedPolicy
     * The String representation of the [Policy] in Json format and encoded into Base 64
     * @return The [Policy] data
     */
    fun fromBase64EncodedPolicy(encodedPolicy: String): Policy {
        val decodedPolicyString = base64Decode(encodedPolicy)
        return fromJson(decodedPolicyString)
    }

    /**
     * Create a [Policy] in Json format and Base 64 encoded.
     *
     * @param policy
     * The String representation of the [Policy] in Json format and encoded into Base 64
     * @return The [Policy] data
     */
    fun toBase64EncodedPolicy(policy: Policy): String {
        return base64Encode(PolicyUtils.toJson(policy).toJSONString())
    }

    /**
     * Get an encrypted version of a [Policy] to use as a signature.
     *
     * @param policy
     * [Policy] that needs to be encrypted.
     * @param encryptionKey
     * The key to use to encrypt the [Policy].
     * @return An encrypted version of the [Policy] that is also Base64 encoded to make it safe to transmit as a
     * query parameter.
     * @throws Exception
     * Thrown if there is a problem encrypting or encoding the [Policy]
     */
    @Throws(Exception::class)
    fun getPolicySignature(policy: Policy, encryptionKey: String): String {
        return SHA256Util.digest(PolicyUtils.toJson(policy).toJSONString(), encryptionKey)
    }
}
