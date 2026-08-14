/*
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.nio.charset.StandardCharsets;

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
   * Get a {@link Policy} from JSON data.
   *
   * @param policyJson
   *          The {@link String} representation of the json.
   * @return A new {@link Policy} object populated from the JSON.
   */
  public static Policy fromJson(String policyJson) {
    JsonObject jsonPolicy = JsonParser.parseString(policyJson).getAsJsonObject();

    JsonObject statement = jsonPolicy.getAsJsonObject(STATEMENT_KEY);
    String resource = statement.get(RESOURCE_KEY).getAsString();
    JsonObject condition = statement.getAsJsonObject(CONDITION_KEY);

    final String lessThanString = condition.get(DATE_LESS_THAN_KEY).getAsString();
    final DateTime dateLessThan = new DateTime(Long.parseLong(lessThanString), DateTimeZone.UTC);

    final DateTime dateGreaterThan;
    if (condition.has(DATE_GREATER_THAN_KEY)) {
      dateGreaterThan = new DateTime(Long.parseLong(condition.get(DATE_GREATER_THAN_KEY).getAsString()),
              DateTimeZone.UTC);
    } else {
      dateGreaterThan = null;
    }

    final String ipAddress = condition.has(IP_ADDRESS_KEY) ? condition.get(IP_ADDRESS_KEY).getAsString() : null;

    return Policy.mkPolicyValidFromWithIP(resource, dateLessThan, dateGreaterThan, ipAddress);
  }

  /**
   * Render a {@link Policy} into JSON.
   * <p>
   * The exact bytes of this output are part of Opencast's contract with anything that has ever signed a URL:
   * {@link #getPolicySignature(Policy, String)} takes a digest of this string, and
   * <code>ResourceRequestUtil</code> verifies a request by re-rendering the decoded policy and comparing digests.
   * Re-ordering a key or changing how a character is escaped therefore invalidates every signed URL already in
   * circulation, and breaks third-party signers that reproduce this format.
   * <p>
   * That is why this is written out by hand rather than handed to a JSON library: the format is a fixed contract, so
   * it should not be able to drift when a library is upgraded or replaced. <code>PolicyUtilsTest</code> pins the
   * result.
   *
   * @param policy
   *          The {@link Policy} to render into JSON.
   * @return The JSON representation of the {@link Policy}.
   */
  public static String toJson(Policy policy) {
    StringBuilder json = new StringBuilder(128);

    json.append("{\"").append(STATEMENT_KEY).append("\":{\"").append(CONDITION_KEY).append("\":{");
    if (policy.getValidFrom().isPresent()) {
      json.append('"').append(DATE_GREATER_THAN_KEY).append("\":")
          .append(policy.getValidFrom().get().getMillis()).append(',');
    }
    json.append('"').append(DATE_LESS_THAN_KEY).append("\":").append(policy.getValidUntil().getMillis());
    if (policy.getClientIpAddress().isPresent()) {
      json.append(",\"").append(IP_ADDRESS_KEY).append("\":\"")
          .append(escape(policy.getClientIpAddress().get().getHostAddress())).append('"');
    }
    json.append("},\"").append(RESOURCE_KEY).append("\":\"").append(escape(policy.getResource())).append("\"}}");

    return json.toString();
  }

  /**
   * Escape a string for inclusion in the policy JSON.
   * <p>
   * This deliberately reproduces the escaping Opencast has emitted since the signing format was introduced, which
   * includes escaping the forward slash and the U+2000-U+20FF block. Both are optional per the JSON specification,
   * but they are part of the signed bytes, so they cannot be dropped. See {@link #toJson(Policy)}.
   *
   * @param value
   *          The string to escape.
   * @return The escaped string, without surrounding quotes.
   */
  private static String escape(String value) {
    StringBuilder escaped = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '"':
          escaped.append("\\\"");
          break;
        case '\\':
          escaped.append("\\\\");
          break;
        case '/':
          escaped.append("\\/");
          break;
        case '\b':
          escaped.append("\\b");
          break;
        case '\f':
          escaped.append("\\f");
          break;
        case '\n':
          escaped.append("\\n");
          break;
        case '\r':
          escaped.append("\\r");
          break;
        case '\t':
          escaped.append("\\t");
          break;
        default:
          if (ch <= 0x1F || (ch >= 0x7F && ch <= 0x9F) || (ch >= 0x2000 && ch <= 0x20FF)) {
            escaped.append(String.format("\\u%04X", (int) ch));
          } else {
            escaped.append(ch);
          }
          break;
      }
    }
    return escaped.toString();
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
    return base64Encode(PolicyUtils.toJson(policy));
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
    return SHA256Util.digest(PolicyUtils.toJson(policy), encryptionKey);
  }
}
