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

package org.opencastproject.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.SignedJWT;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helper class to verify JWTs.
 */
public final class JWTVerifier {

  /** Logging facility. */
  private static final Logger logger = LoggerFactory.getLogger(JWTVerifier.class);

  private JWTVerifier() { }

  /**
   * Verifies a given JWT string with a given JWK provider and given claim constraints.
   *
   * @param token The JWT string.
   * @param retriever The JWK provider.
   * @param claimConstraints The claim constraints.
   * @return The decoded and verified JWT.
   * @throws JOSEException If the JWT cannot be verified successfully.
   * @throws java.text.ParseException If some part of the JWT cannot be parsed successfully.
   */
  public static SignedJWT verify(String token, JWKSetProvider retriever, List<String> claimConstraints)
          throws JOSEException, java.text.ParseException {
    Assert.notNull(token, "A token must be set");
    Assert.notNull(retriever, "A JWKS retriever must be set");

    SignedJWT jwt = SignedJWT.parse(token);
    JWSAlgorithm alg = jwt.getHeader().getAlgorithm();

    List<JWK> jwkSet = retriever.getAll();

    List<JWSVerifier> verifiers = new ArrayList<>();
    if (alg.equals(JWSAlgorithm.RS256) || alg.equals(JWSAlgorithm.RS384) || alg.equals(JWSAlgorithm.RS512)) {
      for (JWK jwk : matching(jwkSet, KeyType.RSA, alg)) {
        verifiers.add(new RSASSAVerifier(jwk.toRSAKey()));
      }
      return verify(jwt, claimConstraints, verifiers.toArray(new JWSVerifier[0]));
    } else if (alg.equals(JWSAlgorithm.ES256) || alg.equals(JWSAlgorithm.ES256K) || alg.equals(JWSAlgorithm.ES384)
        || alg.equals(JWSAlgorithm.ES512)) {
      for (JWK jwk : matching(jwkSet, KeyType.EC, alg)) {
        verifiers.add(new ECDSAVerifier(jwk.toECKey()));
      }
      return verify(jwt, claimConstraints, verifiers.toArray(new JWSVerifier[0]));
    } else if (alg.equals(JWSAlgorithm.EdDSA) || alg.equals(JWSAlgorithm.Ed25519)) {
      for (JWK jwk : matching(jwkSet, KeyType.OKP, alg)) {
        verifiers.add(new Ed25519Verifier(jwk.toPublicJWK().toOctetKeyPair()));
      }
      return verify(jwt, claimConstraints, verifiers.toArray(new JWSVerifier[0]));
    } else {
      throw new IllegalArgumentException("Unsupported algorithm '" + alg + "'");
    }
  }

  /**
   * Filters a JWK set down to the keys of a given key type.
   * <p>
   * The algorithm is taken from the JWT header and is therefore attacker controlled, while the key
   * type is a property of the configured JWK set. Converting a key to the type implied by the
   * algorithm without checking would throw a {@link ClassCastException}, so keys of a different
   * type are skipped here and simply do not yield a verifier.
   *
   * @param jwkSet The JWK set.
   * @param keyType The key type required by the JWT's algorithm.
   * @param alg The JWT's algorithm, for logging only.
   * @return The keys of the requested type.
   */
  private static List<JWK> matching(List<JWK> jwkSet, KeyType keyType, JWSAlgorithm alg) {
    List<JWK> matching = new ArrayList<>();
    for (JWK jwk : jwkSet) {
      if (keyType.equals(jwk.getKeyType())) {
        matching.add(jwk);
      } else {
        logger.debug("Skipping JWK '{}' of type '{}', algorithm '{}' requires a key of type '{}'",
            jwk.getKeyID(), jwk.getKeyType(), alg, keyType);
      }
    }
    if (matching.isEmpty()) {
      logger.warn("The JWK set contains no key of type '{}' as required by the JWT's algorithm '{}'", keyType, alg);
    }
    return matching;
  }

  /**
   * Verifies a given JWT string with a secret and given claim constraints.
   *
   * @param token The JWT string.
   * @param secret The secret.
   * @param claimConstraints The claim constraints.
   * @return The decoded and verified JWT.
   * @throws JOSEException If the JWT cannot be verified successfully.
   */
  public static SignedJWT verify(String token, String secret, List<String> claimConstraints)
          throws JOSEException, java.text.ParseException {
    Assert.notNull(token, "A token must be set");
    Assert.isTrue(StringUtils.isNotBlank(secret), "A secret must be set");

    SignedJWT jwt = SignedJWT.parse(token);
    JWSAlgorithm alg = jwt.getHeader().getAlgorithm();

    if (alg.equals(JWSAlgorithm.HS256) || alg.equals(JWSAlgorithm.HS384) || alg.equals(JWSAlgorithm.HS512)) {
      return verify(jwt, claimConstraints, new MACVerifier(secret));
    } else {
      throw new IllegalArgumentException("Unsupported algorithm '" + alg + "'");
    }
  }

  public static SignedJWT verify(SignedJWT jwt, List<String> claimConstraints, JWSVerifier... verifiers)
          throws JOSEException {
    Assert.notNull(jwt, "A decoded JWT must be set");
    Assert.notNull(claimConstraints, "Claim constraints must be set");
    Assert.notNull(verifiers, "Verifiers must be set");

    boolean verified = false;
    Exception lastException = new JOSEException("JWT could not be verified");
    for (JWSVerifier verifier : verifiers) {
      try {
        // General verification
        if (!jwt.verify(verifier)) {
          throw new JOSEException("JWT could not be verified");
        }

        // Expiration date verification
        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
        if (expirationTime != null && !new Date().before(expirationTime)) {
          throw new JOSEException("JWT is expired");
        }

        // Claim constraints verification
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.addPropertyAccessor(new MapAccessor());
        for (String constraint : claimConstraints) {
          Expression exp = parser.parseExpression(constraint);
          if (!exp.getValue(ctx, jwt.getJWTClaimsSet().getClaims(), Boolean.class)) {
            throw new JOSEException("The claims did not fulfill constraint '" + constraint + "'");
          }
        }

        // Verification was successful if no exception has been thrown
        verified = true;
        break;
      } catch (JOSEException | ParseException e) {
        // Ignore for now and try next algorithm
        lastException = e;
      }
    }

    // If verification was not successful until here, throw last known exception
    if (!verified) {
      throw new JOSEException(lastException.getMessage());
    }

    return jwt;
  }
}
