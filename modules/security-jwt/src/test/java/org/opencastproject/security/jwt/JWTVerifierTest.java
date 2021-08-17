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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Tests for {@link JWTVerifier}.
 */
public class JWTVerifierTest {

  private JWTGenerator generator;
  private GuavaCachedUrlJwkProvider validProvider;
  private GuavaCachedUrlJwkProvider invalidProvider;
  private GuavaCachedUrlJwkProvider rotatingProvider;

  @Before
  public void setUp() throws NoSuchAlgorithmException, JwkException {
    generator = JWTGenerator.getInstance();

    validProvider = createMock(GuavaCachedUrlJwkProvider.class);
    expect(validProvider.getAlgorithms(anyObject(), anyBoolean()))
        .andReturn(List.of(Algorithm.RSA512(generator.getPublicKey(), null)))
        .atLeastOnce();
    replay(validProvider);

    invalidProvider = createMock(GuavaCachedUrlJwkProvider.class);
    expect(invalidProvider.getAlgorithms(anyObject(), anyBoolean()))
        .andReturn(List.of(Algorithm.RSA512(generator.getInvalidPublicKey(), null)))
        .atLeastOnce();
    replay(invalidProvider);

    rotatingProvider = createMock(GuavaCachedUrlJwkProvider.class);
    expect(rotatingProvider.getAlgorithms(anyObject(), anyBoolean()))
        .andReturn(List.of(Algorithm.RSA512(generator.getInvalidPublicKey(), null)))
        .once()
        .andReturn(List.of(Algorithm.RSA512(generator.getPublicKey(), null)))
        .once();
    replay(rotatingProvider);
  }

  @Test
  public void testVerifySymmetric() {
    Exception exception;

    // Valid JWT + valid claim constraints
    DecodedJWT decodedJWT = JWTVerifier.verify(
        generator.generateValidSymmetricJWT(),
        generator.getSecret(),
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), decodedJWT.getClaim("username").asString());

    // Valid JWT + invalid claim constraints
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateValidSymmetricJWT(),
            generator.getSecret(),
            generator.generateInvalidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The claims did not fulfill constraint"));

    // Valid JWT + invalid secret
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateValidSymmetricJWT(),
          "abc",
            generator.generateValidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The Token's Signature resulted invalid"));

    // Invalid JWT
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateExpiredSymmetricJWT(),
            generator.getSecret(),
            generator.generateValidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The Token has expired on"));
  }

  @Test
  public void testVerifyAsymmetric() throws Exception {
    Exception exception;
    DecodedJWT decodedJWT;

    // Valid JWT + valid claim constraints
    decodedJWT = JWTVerifier.verify(
        generator.generateValidAsymmetricJWT(),
        validProvider,
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), decodedJWT.getClaim("username").asString());

    // Valid JWT + invalid claim constraints
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateValidAsymmetricJWT(),
            validProvider,
            generator.generateInvalidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The claims did not fulfill constraint"));

    // Valid JWT + invalid provider
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateValidAsymmetricJWT(),
            invalidProvider,
            generator.generateValidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The Token's Signature resulted invalid"));

    // Invalid JWT
    exception = assertThrows(
        JWTVerificationException.class,
        () -> JWTVerifier.verify(
            generator.generateExpiredAsymmetricJWT(),
            validProvider,
            generator.generateValidClaimConstraints()
        )
    );
    assertTrue(exception.getMessage().startsWith("The Token has expired on"));

    // Simulate key rotation
    decodedJWT = JWTVerifier.verify(
        generator.generateValidAsymmetricJWT(),
        rotatingProvider,
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), decodedJWT.getClaim("username").asString());
  }

}
