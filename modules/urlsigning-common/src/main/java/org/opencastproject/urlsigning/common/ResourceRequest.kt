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

/**
 * Represents a request for a streaming resource whose signed url must be validated.
 */
class ResourceRequest {

    /** The policy encoded in Base64 from the query string value.  */
    var encodedPolicy: String? = null
    /** The query string value that is an id to use to retrieve the encryption key from.  */
    var encryptionKeyId: String? = null
    /** The policy to determine if this resource should be allowed.  */
    var policy: Policy? = null
    /** A textual reason for why a request was rejected.  */
    var rejectionReason = ""
    /** The encrypted policy used to verify this is a valid request.  */
    var signature: String? = null
    /** The status of whether this resource should be allowed to be shown.  */
    var status = Status.Forbidden

    enum class Status {
        BadRequest, Forbidden, Gone, Ok
    }

    companion object {

        /** The query string parameter key of the organization used to request resource.  */
        val ENCRYPTION_ID_KEY = "keyId"
        /** The query string key representing the conditions to allow the resource to be seen.  */
        val POLICY_KEY = "policy"
        /** The query string key representing the encrypted policy.  */
        val SIGNATURE_KEY = "signature"
    }
}
