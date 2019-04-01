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
import org.opencastproject.urlsigning.common.ResourceRequest
import org.opencastproject.urlsigning.common.ResourceRequest.Status

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Properties

/**
 * A utility class to transform ResourceRequests into query strings and back.
 */
object ResourceRequestUtil {
    private val logger = LoggerFactory.getLogger(ResourceRequestUtil::class.java)

    private val humanReadableFormat = DateTimeFormat.forPattern("yyyy-MM-dd kk:mm:ss Z").withZoneUTC()

    /**
     * Get a list of all of the query string parameters and their values.
     *
     * @param queryString
     * The query string to process.
     * @return A [List] of [NameValuePair] representing the query string parameters
     */
    internal fun parseQueryString(queryString: String): List<NameValuePair> {
        return if (StringUtils.isBlank(queryString)) {
            ArrayList()
        } else URLEncodedUtils.parse(queryString.replaceFirst("\\?".toRegex(), ""), StandardCharsets.UTF_8)
    }

    /**
     * Get all of the necessary query string parameters.
     *
     * @param queryParameters
     * The query string parameters.
     * @return True if all of the mandatory query string parameters were provided.
     */
    private fun getQueryStringParameters(resourceRequest: ResourceRequest, queryParameters: List<NameValuePair>): Boolean {
        for (nameValuePair in queryParameters) {
            if (ResourceRequest.ENCRYPTION_ID_KEY == nameValuePair.name) {
                if (StringUtils.isBlank(resourceRequest.encryptionKeyId)) {
                    resourceRequest.encryptionKeyId = nameValuePair.value
                } else {
                    resourceRequest.status = Status.BadRequest
                    resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.ENCRYPTION_ID_KEY)
                    return false
                }
            }
            if (ResourceRequest.POLICY_KEY == nameValuePair.name) {
                if (StringUtils.isBlank(resourceRequest.encodedPolicy)) {
                    resourceRequest.encodedPolicy = nameValuePair.value
                } else {
                    resourceRequest.status = Status.BadRequest
                    resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.POLICY_KEY)
                    return false
                }
            }
            if (ResourceRequest.SIGNATURE_KEY == nameValuePair.name) {
                if (StringUtils.isBlank(resourceRequest.signature)) {
                    resourceRequest.signature = nameValuePair.value
                    resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.SIGNATURE_KEY)
                } else {
                    resourceRequest.status = Status.BadRequest
                    return false
                }
            }
        }

        if (StringUtils.isBlank(resourceRequest.encodedPolicy)) {
            resourceRequest.status = Status.BadRequest
            resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter was missing.",
                    ResourceRequest.POLICY_KEY)
            return false
        } else if (StringUtils.isBlank(resourceRequest.encryptionKeyId)) {
            resourceRequest.status = Status.BadRequest
            resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter was missing.",
                    ResourceRequest.ENCRYPTION_ID_KEY)
            return false
        } else if (StringUtils.isBlank(resourceRequest.signature)) {
            resourceRequest.status = Status.BadRequest
            resourceRequest.rejectionReason = String.format("The mandatory '%s' query string parameter was missing.",
                    ResourceRequest.SIGNATURE_KEY)
            return false
        }

        return true
    }

    /**
     * Determine if the policy matches the encrypted signature.
     *
     * @param policy
     * The policy to compare to the encrypted signature.
     * @param signature
     * The encrypted policy that was sent.
     * @param encryptionKey
     * The encryption key to use to encrypt the policy.
     * @return If the policy encrypted matches the signature.
     */
    internal fun policyMatchesSignature(policy: Policy, signature: String?, encryptionKey: String): Boolean {
        try {
            val encryptedPolicy = PolicyUtils.getPolicySignature(policy, encryptionKey)
            return signature == encryptedPolicy
        } catch (e: Exception) {
            logger.warn("Unable to encrypt policy because {}", ExceptionUtils.getStackTrace(e))
            return false
        }

    }

    /**
     * Create a [ResourceRequest] from the necessary data encoded policy, encryptionKeyId and signature.
     *
     * @param encodedPolicy
     * The policy Base64 encoded.
     * @param encryptionKeyId
     * The id of the encryption key used.
     * @param signature
     * The policy encrypted using the key attached to the encryptionKeyId
     * @return A new [ResourceRequest] filled with the parameter data.
     */
    fun createResourceRequest(encodedPolicy: String, encryptionKeyId: String, signature: String): ResourceRequest {
        val resourceRequest = ResourceRequest()
        resourceRequest.encodedPolicy = encodedPolicy
        resourceRequest.encryptionKeyId = encryptionKeyId
        resourceRequest.signature = signature
        return resourceRequest
    }

    /**
     * Transform a [Policy] into a [ResourceRequest] query string.
     *
     * @param policy
     * The [Policy] to use in the [ResourceRequest]
     * @param encryptionKeyId
     * The id of the encryption key.
     * @param encryptionKey
     * The actual encryption key.
     * @return A query string created from the policy.
     * @throws Exception
     * Thrown if there is a problem encoding or encrypting the policy.
     */
    @Throws(Exception::class)
    fun policyToResourceRequestQueryString(policy: Policy, encryptionKeyId: String, encryptionKey: String): String {
        val resourceRequest = ResourceRequest()
        resourceRequest.encodedPolicy = PolicyUtils.toBase64EncodedPolicy(policy)
        resourceRequest.encryptionKeyId = encryptionKeyId
        resourceRequest.signature = PolicyUtils.getPolicySignature(policy, encryptionKey)
        return resourceRequestToQueryString(resourceRequest)
    }

    /**
     * Transform a [ResourceRequest] into a query string.
     *
     * @param resourceRequest
     * The [ResourceRequest] to transform.
     * @return The query string version of the [ResourceRequest]
     */
    fun resourceRequestToQueryString(resourceRequest: ResourceRequest): String {
        val queryStringParameters = ArrayList<NameValuePair>()
        queryStringParameters.add(BasicNameValuePair(ResourceRequest.POLICY_KEY, resourceRequest.encodedPolicy))
        queryStringParameters.add(BasicNameValuePair(ResourceRequest.ENCRYPTION_ID_KEY, resourceRequest
                .encryptionKeyId))
        queryStringParameters.add(BasicNameValuePair(ResourceRequest.SIGNATURE_KEY, resourceRequest.signature))
        return URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8)
    }

    /**
     * @param queryString
     * The query string for this request to determine its validity.
     * @param clientIp
     * The IP of the client requesting the resource.
     * @param resourceUri
     * The base uri for the resource.
     * @param encryptionKeys
     * The available encryption key ids and their keys.
     * @param strict
     * If false it will only compare the path to the resource instead of the entire URL including scheme,
     * hostname, port etc.
     * @return ResourceRequest
     */
    fun resourceRequestFromQueryString(queryString: String, clientIp: String, resourceUri: String,
                                       encryptionKeys: Properties, strict: Boolean): ResourceRequest {
        val resourceRequest = ResourceRequest()
        val queryParameters = parseQueryString(queryString)

        if (!getQueryStringParameters(resourceRequest, queryParameters)) {
            return resourceRequest
        }

        // Get the encryption key by its id.
        val encryptionKey = encryptionKeys.getProperty(resourceRequest.encryptionKeyId!!)
        if (StringUtils.isBlank(encryptionKey)) {
            resourceRequest.status = Status.Forbidden
            resourceRequest
                    .rejectionReason = String.format("Forbidden because unable to find an encryption key with ID '%s'.",
                    resourceRequest.encryptionKeyId)
            return resourceRequest
        }

        // Get Policy
        val policy = PolicyUtils.fromBase64EncodedPolicy(resourceRequest.encodedPolicy)
        resourceRequest.policy = policy
        // Check to make sure that the Policy & Signature match when encrypted using the private key. If they don't match
        // return a Forbidden 403.
        if (!policyMatchesSignature(policy, resourceRequest.signature, encryptionKey)) {
            resourceRequest.status = Status.Forbidden
            try {
                val policySignature = PolicyUtils.getPolicySignature(policy, encryptionKey)
                resourceRequest
                        .rejectionReason = String
                        .format("Forbidden because policy and signature do not match. Policy: '%s' created Signature from this policy '%s' and query string Signature: '%s'.",
                                PolicyUtils.toJson(resourceRequest.policy!!).toJSONString(), policySignature,
                                resourceRequest.signature)
            } catch (e: Exception) {
                resourceRequest
                        .rejectionReason = String
                        .format("Forbidden because policy and signature do not match. Policy: '%s' and query string Signature: '%s'. Unable to sign policy because: %s",
                                PolicyUtils.toJson(resourceRequest.policy!!).toJSONString(),
                                resourceRequest.signature, ExceptionUtils.getStackTrace(e))
            }

            return resourceRequest
        }
        // If the IP address is specified, check it against the requestor's ip, if it doesn't match return a Forbidden 403.
        if (policy.clientIpAddress.isPresent && !policy.clientIpAddress.get().hostAddress.equals(clientIp, ignoreCase = true)) {
            resourceRequest.status = Status.Forbidden
            resourceRequest.rejectionReason = String.format(
                    "Forbidden because client trying to access the resource '%s' doesn't match the policy client '%s'",
                    clientIp, resourceRequest.policy!!.clientIpAddress)
            return resourceRequest
        }

        // If the resource value in the policy doesn't match the requested resource return a Forbidden 403.
        if (strict && policy.resource != resourceUri) {
            resourceRequest.status = Status.Forbidden
            resourceRequest.rejectionReason = String.format(
                    "Forbidden because resource trying to be accessed '%s' doesn't match policy resource '%s'", resourceUri,
                    resourceRequest.policy!!.baseUrl)
            return resourceRequest
        } else if (!strict) {
            try {
                val requestedPath = URI(resourceUri).path
                val policyPath = URI(policy.resource).path
                if (!policyPath.endsWith(requestedPath)) {
                    resourceRequest.status = Status.Forbidden
                    resourceRequest.rejectionReason = String.format(
                            "Forbidden because resource trying to be accessed '%s' doesn't match policy resource '%s'", resourceUri,
                            resourceRequest.policy!!.baseUrl)
                    return resourceRequest
                }
            } catch (e: URISyntaxException) {
                resourceRequest.status = Status.Forbidden
                resourceRequest
                        .rejectionReason = String
                        .format("Forbidden because either the policy or requested URI cannot be parsed. Policy Path: '%s' and Request Path: '%s'. Unable to sign policy because: %s",
                                policy.resource,
                                resourceUri, ExceptionUtils.getStackTrace(e))
                return resourceRequest
            }

        }

        // Check the dates of the policy to make sure that it is still valid. If it is no longer valid give an Gone return
        // value of 410.
        if (DateTime(DateTimeZone.UTC).isAfter(policy.validUntil.millis)) {
            resourceRequest.status = Status.Gone
            resourceRequest.rejectionReason = String.format("The resource is gone because now '%s' is after the expiry time of '%s'",
                    humanReadableFormat.print(DateTime(DateTimeZone.UTC)),
                    humanReadableFormat.print(DateTime(policy.validUntil.millis, DateTimeZone.UTC)))
            return resourceRequest
        }
        if (policy.validFrom.isPresent && DateTime(DateTimeZone.UTC).isBefore(policy.validFrom.get().millis)) {
            resourceRequest.status = Status.Gone
            resourceRequest.rejectionReason = String.format("The resource is gone because now '%s' is before the available time of ",
                    humanReadableFormat.print(DateTime(DateTimeZone.UTC)),
                    humanReadableFormat.print(policy.validFrom.get()))
            return resourceRequest
        }
        // If all of the above conditions pass, then allow the video to be played.
        resourceRequest.status = Status.Ok
        return resourceRequest
    }

    /**
     * Check to see if a [URI] has not been signed already.
     *
     * @param uri
     * The [URI] to check to see if it was not signed.
     * @return True if not signed, false if signed.
     */
    fun isNotSigned(uri: URI): Boolean {
        return !isSigned(uri)
    }

    /**
     * Check to see if a [URI] has been signed already.
     *
     * @param uri
     * The [URI] to check to see if it was signed.
     * @return True if signed, false if not.
     */
    fun isSigned(uri: URI): Boolean {
        val queryStringParameters = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8.toString())
        var hasKeyId = false
        var hasPolicy = false
        var hasSignature = false
        for (parameter in queryStringParameters) {
            if (parameter.name == ResourceRequest.ENCRYPTION_ID_KEY) {
                hasKeyId = true
            } else if (parameter.name == ResourceRequest.POLICY_KEY) {
                hasPolicy = true
            } else if (parameter.name == ResourceRequest.SIGNATURE_KEY) {
                hasSignature = true
            }
        }
        return hasKeyId && hasPolicy && hasSignature
    }
}
