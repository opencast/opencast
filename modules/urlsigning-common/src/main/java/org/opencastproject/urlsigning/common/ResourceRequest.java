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
package org.opencastproject.urlsigning.common;

/**
 * Represents a request for a streaming resource whose signed url must be validated.
 */
public class ResourceRequest {
  public enum Status {
    BadRequest, Forbidden, Gone, Ok
  };

  /** The query string parameter key of the organization used to request resource. */
  public static final String ENCRYPTION_ID_KEY = "keyId";
  /** The query string key representing the conditions to allow the resource to be seen. */
  public static final String POLICY_KEY = "policy";
  /** The query string key representing the encrypted policy. */
  public static final String SIGNATURE_KEY = "signature";

  /** The policy encoded in Base64 from the query string value. */
  private String encodedPolicy;
  /** The query string value that is an id to use to retrieve the encryption key from. */
  private String encryptionKeyId;
  /** The policy to determine if this resource should be allowed. */
  private Policy policy;
  /** A textual reason for why a request was rejected. */
  private String rejectionReason = "";
  /** The encrypted policy used to verify this is a valid request. */
  private String signature;
  /** The status of whether this resource should be allowed to be shown. */
  private Status status = Status.Forbidden;

  public String getEncodedPolicy() {
    return encodedPolicy;
  }

  public void setEncodedPolicy(String encodedPolicy) {
    this.encodedPolicy = encodedPolicy;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public Policy getPolicy() {
    return policy;
  }

  public void setPolicy(Policy policy) {
    this.policy = policy;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
