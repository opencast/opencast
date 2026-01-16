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
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

/**
 * Helper class generating data for the tests.
 */
public final class JWTGenerator {

  private static JWTGenerator instance;

  // Symmetric Algorithm
  private final String secret = "t0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecret"
      + "t0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecrett0p$ecret";

  // Asymmetric Algorithm
  private final int keySize = 2048;
  private final RSAKey rsaJWK;

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

  private JWTGenerator() throws JOSEException {
    rsaJWK = new RSAKeyGenerator(keySize)
        .keyID("123")
        .generate();
  }

  public static JWTGenerator getInstance() throws NoSuchAlgorithmException, JOSEException {
    if (instance == null) {
      instance = new JWTGenerator();
    }
    return instance;
  }

  public String generateValidSymmetricJWT() throws JOSEException {
    return generateValidJWT(getSymmetricSigner(), getSymmetricAlgorithm(), 60 * 60 * 1000);
  }

  public String generateValidSymmetricJWT(int expiresInMillis) throws JOSEException {
    return generateValidJWT(getSymmetricSigner(), getSymmetricAlgorithm(), expiresInMillis);
  }

  public String generateValidAsymmetricJWT() throws JOSEException {
    return generateValidJWT(getAsymmetricSigner(), getAsymmetricAlgorithm(), 60 * 60 * 1000);
  }

  private String generateValidJWT(JWSSigner signer, JWSAlgorithm algorithm, int expiresInMillis) throws JOSEException {
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .audience(clientId)
        .claim(usernameKey, username)
        .claim(nameKey, name)
        .claim(emailKey, email)
        .claim(rolesKey, roles)
        .expirationTime(new Date(System.currentTimeMillis() + expiresInMillis))
        .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(algorithm), claimsSet);

    signedJWT.sign(signer);

    return signedJWT.serialize();
  }

  public String generateExpiredSymmetricJWT() throws JOSEException {
    return generateExpiredJWT(getSymmetricSigner(), getSymmetricAlgorithm());
  }

  public String generateExpiredAsymmetricJWT() throws JOSEException {
    return generateExpiredJWT(getAsymmetricSigner(), getAsymmetricAlgorithm());
  }

  private String generateExpiredJWT(JWSSigner signer, JWSAlgorithm algorithm) throws JOSEException {
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .audience(clientId)
        .claim(usernameKey, username)
        .claim(nameKey, name)
        .claim(emailKey, email)
        .claim(rolesKey, roles)
        .expirationTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000))
        .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(algorithm), claimsSet);

    signedJWT.sign(signer);

    return signedJWT.serialize();
  }

  public List<String> generateValidClaimConstraints() {
    return List.of(
        "['iss'] eq '" + issuer + "'",
        "(aud instanceof T(java.lang.String) ? aud == '" + clientId + "' : aud.contains('" + clientId + "'))"
    );
  }

  public List<String> generateInvalidClaimConstraints() {
    return List.of(
        "(aud instanceof T(java.lang.String) ? aud == 'xyz' : aud.contains('xyz'))"
    );
  }

  public String generateValidNonExpiringSymmetricJWT() throws JOSEException {
    return generateValidNonExpiringJWT(getSymmetricSigner(), getSymmetricAlgorithm());
  }

  private String generateValidNonExpiringJWT(JWSSigner signer, JWSAlgorithm algorithm) throws JOSEException {
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .audience(clientId)
        .claim(usernameKey, username)
        .claim(nameKey, name)
        .claim(emailKey, email)
        .claim(rolesKey, roles)
        .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(algorithm), claimsSet);

    signedJWT.sign(signer);

    return signedJWT.serialize();
  }

  public String getUsernameMapping() {
    return "['" + usernameKey + "']";
  }

  public String getNameMapping() {
    return "['" + nameKey + "']";
  }

  public String getEmailMapping() {
    return "['" + emailKey + "']";
  }

  public List<String> getRolesMappings() {
    return List.of(
        // Static Assignments
        "'ROLE_JWT_USER'",
        // Expressions
        "'ROLE_JWT_USER_' + ['" + usernameKey + "']",
        "['" + rolesKey + "'].contains('facultly@example.org') ? 'ROLE_GROUP_JWT_TRAINER' : null"
    );
  }

  public JWSAlgorithm getSymmetricAlgorithm() {
    return JWSAlgorithm.HS256;
  }

  public JWSAlgorithm getAsymmetricAlgorithm() {
    return JWSAlgorithm.RS512;
  }

  public JWSSigner getSymmetricSigner() throws KeyLengthException {
    return new MACSigner(secret);
  }

  public JWSSigner getAsymmetricSigner() throws JOSEException {
    return new RSASSASigner(rsaJWK);
  }

  public RSAKey getInvalidRsaJWK() throws JOSEException {
    return new RSAKeyGenerator(keySize)
        .keyID("ABC")
        .generate();
  }

  public String getSecret() {
    return secret;
  }

  public RSAKey getRsaJWK() {
    return rsaJWK;
  }

  public String getUsername() {
    return username;
  }

}
