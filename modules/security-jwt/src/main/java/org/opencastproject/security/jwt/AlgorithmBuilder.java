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

package org.opencastproject.security.jwt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Helper class to build {@link Algorithm} objects.
 */
public final class AlgorithmBuilder {

  // Asymmetric algorithms
  public static final String RS256 = "RS256";
  public static final String RS384 = "RS384";
  public static final String RS512 = "RS512";
  public static final String ES256 = "ES256";
  public static final String ES256K = "ES256K";
  public static final String ES384 = "ES384";
  public static final String ES512 = "ES512";

  // Symmetric algorithms
  public static final String HS256 = "HS256";
  public static final String HS384 = "HS384";
  public static final String HS512 = "HS512";

  private AlgorithmBuilder() { }

  /**
   * Build algorithm object from a JSON Web Key.
   *
   * @param jwk The JSON Web Key.
   * @return The corresponding algorithm.
   * @throws JwkException If the algorithm cannot be constructed.
   */
  public static Algorithm buildAlgorithm(Jwk jwk) throws JwkException {
    return buildAlgorithm(jwk.getAlgorithm(), jwk.getPublicKey());
  }

  /**
   * Build algorithm object from the 'alg' claim of a JWT and a public key.
   *
   * @param alg The 'alg' claim.
   * @param publicKey The public key.
   * @return The corresponding algorithm.
   */
  public static Algorithm buildAlgorithm(String alg, PublicKey publicKey) {
    checkArgument(!isNullOrEmpty(alg), "A algorithm is required");
    checkArgument(publicKey != null, "A public key is required");

    switch (alg) {
      case RS256:
        return Algorithm.RSA256((RSAPublicKey) publicKey, null);
      case RS384:
        return Algorithm.RSA384((RSAPublicKey) publicKey, null);
      case RS512:
        return Algorithm.RSA512((RSAPublicKey) publicKey, null);
      case ES256:
        return Algorithm.ECDSA256((ECPublicKey) publicKey, null);
      case ES256K:
        return Algorithm.ECDSA256K((ECPublicKey) publicKey, null);
      case ES384:
        return Algorithm.ECDSA384((ECPublicKey) publicKey, null);
      case ES512:
        return Algorithm.ECDSA512((ECPublicKey) publicKey, null);
      default:
        throw new IllegalArgumentException("Unsupported algorithm '" + alg + "'");
    }
  }

  /**
   * Build algorithm object from a decoded JWT and a secret.
   *
   * @param jwt The decoded JWT.
   * @param secret The secret.
   * @return The corresponding algorithm.
   */
  public static Algorithm buildAlgorithm(DecodedJWT jwt, String secret) {
    return buildAlgorithm(jwt.getAlgorithm(), secret);
  }

  /**
   * Build algorithm object from the 'alg' claim of a JWT and a secret.
   *
   * @param alg The 'alg' claim.
   * @param secret The secret.
   * @return The corresponding algorithm.
   */
  public static Algorithm buildAlgorithm(String alg, String secret) {
    checkArgument(!isNullOrEmpty(alg), "A algorithm is required");
    checkArgument(secret != null, "A secret is required");

    switch (alg) {
      case HS256:
        return Algorithm.HMAC256(secret);
      case HS384:
        return Algorithm.HMAC384(secret);
      case HS512:
        return Algorithm.HMAC512(secret);
      default:
        throw new IllegalArgumentException("Unsupported algorithm '" + alg + "'");
    }
  }

}
