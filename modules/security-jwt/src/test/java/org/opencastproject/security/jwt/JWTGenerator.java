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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

/**
 * Helper class generating data for the tests.
 */
public final class JWTGenerator {

  private static JWTGenerator instance;

  // Symmetric Algorithm
  private final String secret = "t0p$ecret";

  // Asymmetric Algorithm
  private final String asymmetricAlgorithm = "RSA";
  private final int keySize = 1024;
  private final RSAPublicKey publicKey;
  private final RSAPrivateKey privateKey;

  // Claims
  private final String issuer = "https://auth.example.org";
  private final String clientId = "client-id";
  private final String usernameKey = "username";
  private final String username = "john.doe";
  private final String nameKey = "name";
  private final String name = "John Doe";
  private final String emailKey = "email";
  private final String email = "john.doe@example.org";
  private final String rolesKey = "roles";
  private final List<String> roles = List.of("member@example.org", "facultly@example.org");

  private JWTGenerator() throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance(asymmetricAlgorithm);
    generator.initialize(keySize);
    KeyPair keyPair = generator.genKeyPair();
    publicKey = (RSAPublicKey) keyPair.getPublic();
    privateKey = (RSAPrivateKey) keyPair.getPrivate();
  }

  public static JWTGenerator getInstance() throws NoSuchAlgorithmException {
    if (instance == null) {
      instance = new JWTGenerator();
    }
    return instance;
  }

  public String generateValidSymmetricJWT() {
    return generateValidJWT(getSymmetricAlgorithm(), 60 * 60 * 1000);
  }

  public String generateValidSymmetricJWT(int expiresInMillis) {
    return generateValidJWT(getSymmetricAlgorithm(), expiresInMillis);
  }

  public String generateValidAsymmetricJWT() {
    return generateValidJWT(getAsymmetricAlgorithm(), 60 * 60 * 1000);
  }

  private String generateValidJWT(Algorithm algorithm, int expiresInMillis) {
    return JWT.create()
        .withIssuer(issuer)
        .withAudience(clientId)
        .withClaim(usernameKey, username)
        .withClaim(nameKey, name)
        .withClaim(emailKey, email)
        .withClaim(rolesKey, roles)
        .withExpiresAt(new Date(System.currentTimeMillis() + expiresInMillis))
        .sign(algorithm);
  }

  public String generateExpiredSymmetricJWT() {
    return generateExpiredJWT(getSymmetricAlgorithm());
  }

  public String generateExpiredAsymmetricJWT() {
    return generateExpiredJWT(getAsymmetricAlgorithm());
  }

  private String generateExpiredJWT(Algorithm algorithm) {
    return JWT.create()
        .withIssuer(issuer)
        .withAudience(clientId)
        .withClaim(usernameKey, username)
        .withClaim(nameKey, name)
        .withClaim(emailKey, email)
        .withClaim(rolesKey, roles)
        .withExpiresAt(new Date(System.currentTimeMillis() - 60 * 60 * 1000))
        .sign(algorithm);
  }

  public List<String> generateValidClaimConstraints() {
    return List.of(
        "['iss'].asString() eq '" + issuer + "'",
        "['aud'].asString() eq '" + clientId + "'"
    );
  }

  public List<String> generateInvalidClaimConstraints() {
    return List.of(
        "['aud'].asString() eq 'xyz'"
    );
  }

  public String generateValidNonExpiringSymmetricJWT() {
    return generateValidNonExpiringJWT(getSymmetricAlgorithm());
  }

  private String generateValidNonExpiringJWT(Algorithm algorithm) {
    return JWT.create()
        .withIssuer(issuer)
        .withAudience(clientId)
        .withClaim(usernameKey, username)
        .withClaim(nameKey, name)
        .withClaim(emailKey, email)
        .withClaim(rolesKey, roles)
        .sign(algorithm);
  }

  public String getUsernameMapping() {
    return "['" + usernameKey + "'].asString()";
  }

  public String getNameMapping() {
    return "['" + nameKey + "'].asString()";
  }

  public String getEmailMapping() {
    return "['" + emailKey + "'].asString()";
  }

  public List<String> getRolesMappings() {
    return List.of(
        // Static Assignments
        "'ROLE_JWT_USER'",
        // Expressions
        "'ROLE_JWT_USER_' + ['" + usernameKey + "'].asString()",
        "['" + rolesKey + "'].asList(T(String)).contains('facultly@example.org') ? 'ROLE_GROUP_JWT_TRAINER' : null"
    );
  }

  public Algorithm getSymmetricAlgorithm() {
    return Algorithm.HMAC256(secret);
  }

  public Algorithm getAsymmetricAlgorithm() {
    return Algorithm.RSA512(publicKey, privateKey);
  }

  public RSAPublicKey getInvalidPublicKey() throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance(asymmetricAlgorithm);
    generator.initialize(keySize);
    return (RSAPublicKey) generator.genKeyPair().getPublic();
  }

  public String getSecret() {
    return secret;
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  public String getUsername() {
    return username;
  }

}
