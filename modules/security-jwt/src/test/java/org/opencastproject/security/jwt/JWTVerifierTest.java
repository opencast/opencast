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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.List;

/**
 * Tests for {@link JWTVerifier}.
 */
public class JWTVerifierTest {

  private JWTGenerator generator;
  private JWKSetProvider validProvider;
  private JWKSetProvider invalidProvider;

  @Before
  public void setUp() throws NoSuchAlgorithmException, JOSEException {
    generator = JWTGenerator.getInstance();

    validProvider = createMock(JWKSetProvider.class);
    expect(validProvider.getAll())
        .andReturn(List.of(generator.getRsaJWK()))
        .atLeastOnce();
    replay(validProvider);

    invalidProvider = createMock(JWKSetProvider.class);
    expect(invalidProvider.getAll())
        .andReturn(List.of(generator.getInvalidRsaJWK()))
        .atLeastOnce();
    replay(invalidProvider);
  }

  @Test
  public void testVerifySymmetric() throws JOSEException, ParseException {
    // Valid JWT + valid claim constraints
    SignedJWT signedJWT = JWTVerifier.verify(
        generator.generateValidSymmetricJWT(),
        generator.getSecret(),
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), signedJWT.getJWTClaimsSet().getClaimAsString("username"));

    // Valid JWT + invalid claim constraints
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidSymmetricJWT(),
            generator.getSecret(),
            generator.generateInvalidClaimConstraints()
        )
    );

    // Valid JWT + invalid secret
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidSymmetricJWT(),
          "abc",
            generator.generateValidClaimConstraints()
        )
    );

    // Invalid JWT
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateExpiredSymmetricJWT(),
            generator.getSecret(),
            generator.generateValidClaimConstraints()
        )
    );
  }

  @Test
  public void testVerifyAsymmetric() throws Exception {
    SignedJWT signedJWT;

    // Valid JWT + valid claim constraints
    signedJWT = JWTVerifier.verify(
        generator.generateValidAsymmetricJWT(),
        validProvider,
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), signedJWT.getJWTClaimsSet().getClaimAsString("username"));

    // Valid JWT + invalid claim constraints
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidAsymmetricJWT(),
            validProvider,
            generator.generateInvalidClaimConstraints()
        )
    );

    // Valid JWT + invalid provider
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidAsymmetricJWT(),
            invalidProvider,
            generator.generateValidClaimConstraints()
        )
    );

    // Invalid JWT
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateExpiredAsymmetricJWT(),
            validProvider,
            generator.generateValidClaimConstraints()
        )
    );
  }

  /**
   * A JWT whose algorithm implies a different key type than the one offered by the JWK set must be
   * rejected as unverifiable rather than causing a {@link ClassCastException}.
   */
  @Test
  public void testVerifyAlgorithmKeyTypeMismatch() throws Exception {
    // EC-signed JWT against an OKP (Ed25519) JWK set, the combination reported in the issue
    JWKSetProvider okpProvider = createMock(JWKSetProvider.class);
    expect(okpProvider.getAll())
        .andReturn(List.of(generator.getOkpJWK().toPublicJWK()))
        .atLeastOnce();
    replay(okpProvider);

    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidEcJWT(),
            okpProvider,
            generator.generateValidClaimConstraints()
        )
    );

    // EC-signed JWT against an RSA JWK set
    assertThrows(
        JOSEException.class,
        () -> JWTVerifier.verify(
            generator.generateValidEcJWT(),
            validProvider,
            generator.generateValidClaimConstraints()
        )
    );

    // A JWK set mixing an unusable and a usable key must still verify via the usable one
    JWKSetProvider mixedProvider = createMock(JWKSetProvider.class);
    expect(mixedProvider.getAll())
        .andReturn(List.of(generator.getOkpJWK().toPublicJWK(), generator.getEcJWK().toPublicJWK()))
        .atLeastOnce();
    replay(mixedProvider);

    SignedJWT signedJWT = JWTVerifier.verify(
        generator.generateValidEcJWT(),
        mixedProvider,
        generator.generateValidClaimConstraints()
    );
    assertEquals(generator.getUsername(), signedJWT.getJWTClaimsSet().getClaimAsString("username"));
  }

}
