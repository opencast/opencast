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
package org.opencastproject.urlsigning.utils;

import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.common.ResourceRequest.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A utility class to transform ResourceRequests into query strings and back.
 */
public final class ResourceRequestUtil {
  private static final Logger logger = LoggerFactory.getLogger(ResourceRequestUtil.class);

  private static final DateTimeFormatter humanReadableFormat = DateTimeFormat.forPattern("yyyy-MM-dd kk:mm:ss Z").withZoneUTC();

  private ResourceRequestUtil() {
  }

  /**
   * Get a list of all of the query string parameters and their values.
   *
   * @param queryString
   *          The query string to process.
   * @return A {@link List} of {@link NameValuePair} representing the query string parameters
   */
  protected static List<NameValuePair> parseQueryString(String queryString) {
    if (StringUtils.isBlank(queryString)) {
      return new ArrayList<NameValuePair>();
    }
    return URLEncodedUtils.parse(queryString.replaceFirst("\\?", ""), StandardCharsets.UTF_8);
  }

  /**
   * Get all of the necessary query string parameters.
   *
   * @param queryParameters
   *          The query string parameters.
   * @return True if all of the mandatory query string parameters were provided.
   */
  private static boolean getQueryStringParameters(ResourceRequest resourceRequest, List<NameValuePair> queryParameters) {
    for (NameValuePair nameValuePair : queryParameters) {
      if (ResourceRequest.ENCRYPTION_ID_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getEncryptionKeyId())) {
          resourceRequest.setEncryptionKeyId(nameValuePair.getValue());
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          resourceRequest.setRejectionReason(
                  String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.ENCRYPTION_ID_KEY));
          return false;
        }
      }
      if (ResourceRequest.POLICY_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getEncodedPolicy())) {
          resourceRequest.setEncodedPolicy(nameValuePair.getValue());
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          resourceRequest.setRejectionReason(
                  String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.POLICY_KEY));
          return false;
        }
      }
      if (ResourceRequest.SIGNATURE_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getSignature())) {
          resourceRequest.setSignature(nameValuePair.getValue());
          resourceRequest.setRejectionReason(
                  String.format("The mandatory '%s' query string parameter had an empty value.", ResourceRequest.SIGNATURE_KEY));
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          return false;
        }
      }
    }

    if (StringUtils.isBlank(resourceRequest.getEncodedPolicy())) {
      resourceRequest.setStatus(Status.BadRequest);
      resourceRequest.setRejectionReason(String.format("The mandatory '%s' query string parameter was missing.",
              ResourceRequest.POLICY_KEY));
      return false;
    } else if (StringUtils.isBlank(resourceRequest.getEncryptionKeyId())) {
      resourceRequest.setStatus(Status.BadRequest);
      resourceRequest.setRejectionReason(String.format("The mandatory '%s' query string parameter was missing.",
              ResourceRequest.ENCRYPTION_ID_KEY));
      return false;
    } else if (StringUtils.isBlank(resourceRequest.getSignature())) {
      resourceRequest.setStatus(Status.BadRequest);
      resourceRequest.setRejectionReason(String.format("The mandatory '%s' query string parameter was missing.",
              ResourceRequest.SIGNATURE_KEY));
      return false;
    }

    return true;
  }

  /**
   * Determine if the policy matches the encrypted signature.
   *
   * @param policy
   *          The policy to compare to the encrypted signature.
   * @param signature
   *          The encrypted policy that was sent.
   * @param encryptionKey
   *          The encryption key to use to encrypt the policy.
   * @return If the policy encrypted matches the signature.
   */
  protected static boolean policyMatchesSignature(Policy policy, String signature, String encryptionKey) {
    try {
      String encryptedPolicy = PolicyUtils.getPolicySignature(policy, encryptionKey);
      return signature.equals(encryptedPolicy);
    } catch (Exception e) {
      logger.warn("Unable to encrypt policy because", e);
      return false;
    }
  }

  /**
   * Transform a {@link Policy} into a {@link ResourceRequest} query string.
   *
   * @param policy
   *          The {@link Policy} to use in the {@link ResourceRequest}
   * @param encryptionKeyId
   *          The id of the encryption key.
   * @param encryptionKey
   *          The actual encryption key.
   * @return A query string created from the policy.
   * @throws Exception
   *           Thrown if there is a problem encoding or encrypting the policy.
   */
  public static String policyToResourceRequestQueryString(Policy policy, String encryptionKeyId, String encryptionKey)
          throws Exception {
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.setEncodedPolicy(PolicyUtils.toBase64EncodedPolicy(policy));
    resourceRequest.setEncryptionKeyId(encryptionKeyId);
    resourceRequest.setSignature(PolicyUtils.getPolicySignature(policy, encryptionKey));
    return resourceRequestToQueryString(resourceRequest);
  }

  /**
   * Transform a {@link ResourceRequest} into a query string.
   *
   * @param resourceRequest
   *          The {@link ResourceRequest} to transform.
   * @return The query string version of the {@link ResourceRequest}
   */
  public static String resourceRequestToQueryString(ResourceRequest resourceRequest) {
    List<NameValuePair> queryStringParameters = new ArrayList<NameValuePair>();
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.POLICY_KEY, resourceRequest.getEncodedPolicy()));
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.ENCRYPTION_ID_KEY, resourceRequest
            .getEncryptionKeyId()));
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.SIGNATURE_KEY, resourceRequest.getSignature()));
    return URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8);
  }

  /**
   * @param queryString
   *          The query string for this request to determine its validity.
   * @param clientIp
   *          The IP of the client requesting the resource.
   * @param resourceUri
   *          The base uri for the resource.
   * @param encryptionKeys
   *          The available encryption key ids and their keys.
   * @param strict
   *          If false it will only compare the path to the resource instead of the entire URL including scheme,
   *          hostname, port etc.
   * @return ResourceRequest
   */
  public static ResourceRequest resourceRequestFromQueryString(String queryString, String clientIp, String resourceUri,
          Properties encryptionKeys, boolean strict) {
    ResourceRequest resourceRequest = new ResourceRequest();
    List<NameValuePair> queryParameters = parseQueryString(queryString);

    if (!getQueryStringParameters(resourceRequest, queryParameters)) {
      return resourceRequest;
    }

    // Get the encryption key by its id.
    String encryptionKey = encryptionKeys.getProperty(resourceRequest.getEncryptionKeyId());
    if (StringUtils.isBlank(encryptionKey)) {
      resourceRequest.setStatus(Status.Forbidden);
      resourceRequest
              .setRejectionReason(String.format("Forbidden because unable to find an encryption key with ID '%s'.",
                      resourceRequest.getEncryptionKeyId()));
      return resourceRequest;
    }

    // Get Policy
    Policy policy = PolicyUtils.fromBase64EncodedPolicy(resourceRequest.getEncodedPolicy());
    resourceRequest.setPolicy(policy);
    // Check to make sure that the Policy & Signature match when encrypted using the private key. If they don't match
    // return a Forbidden 403.
    if (!policyMatchesSignature(policy, resourceRequest.getSignature(), encryptionKey)) {
      resourceRequest.setStatus(Status.Forbidden);
      try {
        String policySignature = PolicyUtils.getPolicySignature(policy, encryptionKey);
        resourceRequest
                .setRejectionReason(String
                        .format("Forbidden because policy and signature do not match. Policy: '%s' created Signature from this policy '%s' and query string Signature: '%s'.",
                                PolicyUtils.toJson(resourceRequest.getPolicy()).toJSONString(), policySignature,
                                resourceRequest.getSignature()));
      } catch (Exception e) {
        resourceRequest
                .setRejectionReason(String
                        .format("Forbidden because policy and signature do not match. Policy: '%s' and query string Signature: '%s'. Unable to sign policy because: %s",
                                PolicyUtils.toJson(resourceRequest.getPolicy()).toJSONString(),
                                resourceRequest.getSignature(), ExceptionUtils.getStackTrace(e)));
      }
      return resourceRequest;
    }
    // If the IP address is specified, check it against the requestor's ip, if it doesn't match return a Forbidden 403.
    if (policy.getClientIpAddress().isPresent()
            && !policy.getClientIpAddress().get().getHostAddress().equalsIgnoreCase(clientIp)) {
      resourceRequest.setStatus(Status.Forbidden);
      resourceRequest.setRejectionReason(String.format(
              "Forbidden because client trying to access the resource '%s' doesn't match the policy client '%s'",
              clientIp, resourceRequest.getPolicy().getClientIpAddress()));
      return resourceRequest;
    }

    // If the resource value in the policy doesn't match the requested resource return a Forbidden 403.
    if (strict && !policy.getResource().equals(resourceUri)) {
      resourceRequest.setStatus(Status.Forbidden);
      resourceRequest.setRejectionReason(String.format(
              "Forbidden because resource trying to be accessed '%s' doesn't match policy resource '%s'", resourceUri,
              resourceRequest.getPolicy().getBaseUrl()));
      return resourceRequest;
    } else if (!strict) {
      try {
        String requestedPath = new URI(resourceUri).getPath();
        String policyPath = new URI(policy.getResource()).getPath();
        if (!policyPath.endsWith(requestedPath)) {
          resourceRequest.setStatus(Status.Forbidden);
          resourceRequest.setRejectionReason(String.format(
                  "Forbidden because resource trying to be accessed '%s' doesn't match policy resource '%s'", resourceUri,
                  resourceRequest.getPolicy().getBaseUrl()));
          return resourceRequest;
        }
      } catch (URISyntaxException e) {
        resourceRequest.setStatus(Status.Forbidden);
        resourceRequest
            .setRejectionReason(String
                .format("Forbidden because either the policy or requested URI cannot be parsed. Policy Path: '%s' and Request Path: '%s'. Unable to sign policy because: %s",
                        policy.getResource(),
                        resourceUri, ExceptionUtils.getStackTrace(e)));
        return resourceRequest;
      }
    }

    // Check the dates of the policy to make sure that it is still valid. If it is no longer valid give an Gone return
    // value of 410.
    if (new DateTime(DateTimeZone.UTC).isAfter(policy.getValidUntil().getMillis())) {
      resourceRequest.setStatus(Status.Gone);
      resourceRequest.setRejectionReason(
              String.format("The resource is gone because now '%s' is after the expiry time of '%s'",
                      humanReadableFormat.print(new DateTime(DateTimeZone.UTC)),
                      humanReadableFormat.print(new DateTime(policy.getValidUntil().getMillis(), DateTimeZone.UTC))));
      return resourceRequest;
    }
    if (policy.getValidFrom().isPresent()
            && new DateTime(DateTimeZone.UTC).isBefore(policy.getValidFrom().get().getMillis())) {
      resourceRequest.setStatus(Status.Gone);
      resourceRequest.setRejectionReason(
              String.format("The resource is gone because now '%s' is before the available time of ",
                      humanReadableFormat.print(new DateTime(DateTimeZone.UTC)),
                      humanReadableFormat.print(policy.getValidFrom().get())));
      return resourceRequest;
    }
    // If all of the above conditions pass, then allow the video to be played.
    resourceRequest.setStatus(Status.Ok);
    return resourceRequest;
  }

  /**
   * Check to see if a {@link URI} has not been signed already.
   *
   * @param uri
   *          The {@link URI} to check to see if it was not signed.
   * @return True if not signed, false if signed.
   */
  public static boolean isNotSigned(URI uri) {
    return !isSigned(uri);
  }

  /**
   * Check to see if a {@link URI} has been signed already.
   *
   * @param uri
   *          The {@link URI} to check to see if it was signed.
   * @return True if signed, false if not.
   */
  public static boolean isSigned(URI uri) {
    List<NameValuePair> queryStringParameters = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8.toString());
    boolean hasKeyId = false;
    boolean hasPolicy = false;
    boolean hasSignature = false;
    for (NameValuePair parameter : queryStringParameters) {
      if (parameter.getName().equals(ResourceRequest.ENCRYPTION_ID_KEY)) {
        hasKeyId = true;
      } else if (parameter.getName().equals(ResourceRequest.POLICY_KEY)) {
        hasPolicy = true;
      } else if (parameter.getName().equals(ResourceRequest.SIGNATURE_KEY)) {
        hasSignature = true;
      }
    }
    return hasKeyId && hasPolicy && hasSignature;
  }
}
