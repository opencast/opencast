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
package org.opencastproject.urlsigning.common

import java.util.Objects.requireNonNull

import com.google.common.base.Optional
import com.google.common.net.InetAddresses

import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime

import java.net.InetAddress

/**
 * Represents a policy for a signed resource that looks like
 *
 */
class Policy
/**
 * Create a new Policy.
 *
 * @param baseUrl
 * The base url that points to the resource that is being made available.
 * @param validUntil
 * The date and time the resource is available until.
 * @param validFrom
 * An optional date and time the resource will first become available.
 * @param clientIpAddress
 * An optional client IP address to restrict the viewing of the resource to.
 * @param resourceStrategy
 * The strategy for getting the resource from the policy.
 */
private constructor(
        /** The base URL for the resource being requested.  */
        /**
         * @return Get the url to the resource that is being signed with this policy.
         */
        val baseUrl: String,
        /** The date and time when the resource expires.  */
        /**
         * @return Get the date this resource is valid until.
         */
        val validUntil: DateTime, validFrom: DateTime, clientIpAddress: String) {

    /** The date and time when the resource will become available.  */
    /**
     * @return Get the optional date and time this resource will become available.
     */
    val validFrom: Optional<DateTime>

    /** An optional client IP address that made the original request.  */
    /**
     * @return Get the optional ip address of the client that this resource will be restricted to.
     */
    val clientIpAddress: Optional<InetAddress>

    /** The required strategy to convert a base url to the resource url.  */
    private var resourceStrategy: ResourceStrategy = BasicResourceStrategyImpl()

    /**
     * @return Get the url for the resource in this [Policy].
     */
    val resource: String
        get() = resourceStrategy.getResource(baseUrl)

    init {
        requireNonNull(baseUrl)
        requireNonNull(validUntil)
        this.validFrom = Optional.fromNullable(validFrom)
        if (StringUtils.isNotBlank(clientIpAddress)) {
            this.clientIpAddress = Optional.of(InetAddresses.forString(clientIpAddress))
        } else {
            this.clientIpAddress = Optional.absent()
        }
    }

    /**
     * Set a new [ResourceStrategy] to transform the base url to a resource url.
     *
     * @param resourceStrategy
     * The resource strategy to apply to transform the base url.
     */
    fun setResourceStrategy(resourceStrategy: ResourceStrategy) {
        this.resourceStrategy = resourceStrategy
    }

    companion object {

        /**
         * Create a [Policy] with only the required properties.
         *
         * @param baseUrl
         * The url to the resource that will be signed.
         * @param validUntil
         * The date and time the resource will be available until
         * @return A new [Policy] with the parameters set.
         */
        fun mkSimplePolicy(baseUrl: String, validUntil: DateTime): Policy {
            return Policy(baseUrl, validUntil, null, null)
        }

        /**
         * Create a [Policy] with a date and time the resource will become available.
         *
         * @param baseUrl
         * The url to the resource being signed.
         * @param validUntil
         * The date and time the resource is available until.
         * @param validFrom
         * The date and time the resource will become available.
         * @return A new [Policy] for limiting access to the resource.
         */
        fun mkPolicyValidFrom(baseUrl: String, validUntil: DateTime, validFrom: DateTime): Policy {
            return Policy(baseUrl, validUntil, validFrom, null)
        }

        /**
         * Create a [Policy] with the only ip address that will be allowed to view the resource.
         *
         * @param baseUrl
         * The url to the resource being signed.
         * @param validUntil
         * The date the resource will be available until.
         * @param ipAddress
         * The ip of the client that will be allowed to view the resource.
         * @return A new [Policy] for limiting access to the resource.
         */
        fun mkPolicyValidWithIP(baseUrl: String, validUntil: DateTime, ipAddress: String): Policy {
            return Policy(baseUrl, validUntil, null, ipAddress)
        }

        /**
         * Create a [Policy] with both a date and time the resource will become available and a client ip address to
         * restrict it to.
         *
         * @param baseUrl
         * The url to the resource that is being signed.
         * @param validUntil
         * The date and time the resource will be available until.
         * @param validFrom
         * The date and time the resource will become available.
         * @param ipAddress
         * The ip of the client that will be allowed to view the resource.
         * @return A new [Policy] for limiting access to the resource.
         */
        fun mkPolicyValidFromWithIP(baseUrl: String, validUntil: DateTime, validFrom: DateTime,
                                    ipAddress: String): Policy {
            return Policy(baseUrl, validUntil, validFrom, ipAddress)
        }
    }

}
