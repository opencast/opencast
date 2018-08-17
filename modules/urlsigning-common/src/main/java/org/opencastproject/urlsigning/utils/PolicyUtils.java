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

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Utility class to encode / decode Policy files from and to Base 64 and Json.
 */
public final class PolicyUtils {
  /** The JSON key for object that contains the date and ip conditions for the resource. */
  private static final String CONDITION_KEY = "Condition";
  /** The JSON key for the date and time the resource will become available. */
  private static final String DATE_GREATER_THAN_KEY = "DateGreaterThan";
  /** The JSON key for the date and time the resource will no longer be available. */
  private static final String DATE_LESS_THAN_KEY = "DateLessThan";
  /** The JSON key for the IP address of the acceptable client. */
  private static final String IP_ADDRESS_KEY = "IpAddress";
  /** The JSON key for the base url for the resource. */
  private static final String RESOURCE_KEY = "Resource";
  /** The JSON key for the main object of the policy. */
  private static final String STATEMENT_KEY = "Statement";

  private PolicyUtils() {

  }

  /**
   * Encode a {@link String} into Base 64 encoding
   *
   * @param value
   *          The {@link String} to encode into base 64 encoding
   * @return The {@link String} encoded into base 64.
   */
  public static String base64Encode(String value) {
    return Base64.encodeBase64URLSafeString(value.getBytes());
  }

  /**
   * Decode a {@link String} from Base 64 encoding
   *
   * @param value
   *          The {@link String} to encode into Base 64
   * @return The {@link String} decoded from base 64.
   */
  public static String base64Decode(String value) {
    return new String(Base64.decodeBase64(value), StandardCharsets.UTF_8);
  }

  /**
   * Encode a byte[] into Base 64 encoding
   *
   * @param value
   *          The byte[] to encode into base 64 encoding
   * @return The {@link String} encoded into base 64.
   */
  public static String base64Encode(byte[] value) {
    return new String(Base64.encodeBase64URLSafe(value), StandardCharsets.UTF_8);
  }

  /**
   * Get a {@link Policy} from JSON data.
   *
   * @param policyJson
   *          The {@link String} representation of the json.
   * @return A new {@link Policy} object populated from the JSON.
   */
  public static Policy fromJson(String policyJson) {
    JSONObject jsonPolicy = null;
    JSONParser jsonParser = new JSONParser();
    try {
      jsonPolicy = (JSONObject) jsonParser.parse(policyJson);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    JSONObject statement = (JSONObject) jsonPolicy.get(STATEMENT_KEY);
    String resource = statement.get(RESOURCE_KEY).toString();
    JSONObject condition = (JSONObject) statement.get(CONDITION_KEY);

    final String lessThanString = condition.get(DATE_LESS_THAN_KEY).toString();
    final DateTime dateLessThan = new DateTime(Long.parseLong(lessThanString), DateTimeZone.UTC);

    final DateTime dateGreaterThan;
    Object greaterThanString = condition.get(DATE_GREATER_THAN_KEY);
    if (greaterThanString != null) {
      dateGreaterThan = new DateTime(Long.parseLong(greaterThanString.toString()), DateTimeZone.UTC);
    } else {
      dateGreaterThan = null;
    }

    return Policy.mkPolicyValidFromWithIP(resource, dateLessThan, dateGreaterThan,
            (String) condition.get(IP_ADDRESS_KEY));
  }

  /**
   * Render a {@link Policy} into JSON.
   *
   * @param policy
   *          The {@link Policy} to render into JSON.
   * @return The {@link JSONObject} representation of the {@link Policy}.
   */
  @SuppressWarnings("unchecked")
  public static JSONObject toJson(Policy policy) {
    JSONObject policyJSON = new JSONObject();

    Map<String, Object> conditions = new TreeMap<String, Object>();
    conditions.put(DATE_LESS_THAN_KEY, new Long(policy.getValidUntil().getMillis()));
    if (policy.getValidFrom().isPresent()) {
      conditions.put(DATE_GREATER_THAN_KEY, new Long(policy.getValidFrom().get().getMillis()));
    }
    if (policy.getClientIpAddress().isPresent()) {
      conditions.put(IP_ADDRESS_KEY, policy.getClientIpAddress().get().getHostAddress());
    }
    JSONObject conditionsJSON = new JSONObject();
    conditionsJSON.putAll(conditions);

    JSONObject statement = new JSONObject();
    statement.put(RESOURCE_KEY, policy.getResource());
    statement.put(CONDITION_KEY, conditions);

    policyJSON.put(STATEMENT_KEY, statement);

    return policyJSON;
  }

  /**
   * Create a {@link Policy} in Json format and Base 64 encoded.
   *
   * @param encodedPolicy
   *          The String representation of the {@link Policy} in Json format and encoded into Base 64
   * @return The {@link Policy} data
   */
  public static Policy fromBase64EncodedPolicy(String encodedPolicy) {
    String decodedPolicyString = base64Decode(encodedPolicy);
    return fromJson(decodedPolicyString);
  }

  /**
   * Create a {@link Policy} in Json format and Base 64 encoded.
   *
   * @param policy
   *          The String representation of the {@link Policy} in Json format and encoded into Base 64
   * @return The {@link Policy} data
   */
  public static String toBase64EncodedPolicy(Policy policy) {
    return base64Encode(PolicyUtils.toJson(policy).toJSONString());
  }

  /**
   * Get an encrypted version of a {@link Policy} to use as a signature.
   *
   * @param policy
   *          {@link Policy} that needs to be encrypted.
   * @param encryptionKey
   *          The key to use to encrypt the {@link Policy}.
   * @return An encrypted version of the {@link Policy} that is also Base64 encoded to make it safe to transmit as a
   *         query parameter.
   * @throws Exception
   *           Thrown if there is a problem encrypting or encoding the {@link Policy}
   */
  public static String getPolicySignature(Policy policy, String encryptionKey) throws Exception {
    return SHA256Util.digest(PolicyUtils.toJson(policy).toJSONString(), encryptionKey);
  }
}
