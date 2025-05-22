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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUserReference;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.userdirectory.api.UserReferenceProvider;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Dynamic login handler for JWTs.
 */
public class DynamicLoginHandler implements InitializingBean, JWTLoginHandler {

  /** Logging facility. */
  private static final Logger logger = LoggerFactory.getLogger(DynamicLoginHandler.class);

  /** Spring security's user details manager. */
  private UserDetailsService userDetailsService = null;

  /** User directory service. */
  private UserDirectoryService userDirectoryService = null;

  /** User reference provider. */
  private UserReferenceProvider userReferenceProvider = null;

  /** Security service. */
  private SecurityService securityService = null;

  /** JWKS URL to use for JWT validation (asymmetric algorithms). */
  private String jwksUrl = null;

  /** Number of minutes fetched JWKs will be cached. */
  private int jwksCacheExpiresIn = 60 * 24;

  /** Secret to use for JWT validation (symmetric algorithms). */
  private String secret = null;

  /** Allowed algorithms with which a valid JWT may be signed ('alg' claim). */
  private List<String> expectedAlgorithms = null;

  /** Constraints that the claims of a valid JWT must fulfill. */
  private List<String> claimConstraints = null;

  /** Mapping used to extract the username from the JWT. */
  private String usernameMapping = null;

  /** Mapping used to extract the name from the JWT. */
  private String nameMapping = null;

  /** Mapping used to extract the email from the JWT. */
  private String emailMapping = null;

  /** Whether the built-in schema role mapping should be used. */
  private boolean ocStandardRoleMappings = true;

  /** Mapping used to extract roles from the JWT. */
  private List<String> roleMappings = null;

  /** Mapping used to extract roles from the JWT. */
  private GuavaCachedUrlJwkProvider jwkProvider;

  /** Size of the JWT cache. */
  private int jwtCacheSize = 500;

  /** Number of minutes validated JWTs will be cached before re-validating them. */
  private int jwtCacheExpiresIn = 60;

  /** Cache for validated JWTs. */
  private Cache<String, CachedJWT> cache;

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(userDetailsService, "A UserDetailsService must be set");
    Assert.notNull(userDirectoryService, "A UserDirectoryService must be set");
    Assert.notNull(userReferenceProvider, "A UserReferenceProvider must be set");
    Assert.notNull(securityService, "A SecurityService must be set");
    Assert.isTrue(StringUtils.isNotBlank(jwksUrl) ^ StringUtils.isNotBlank(secret),
        "Either a JWKS URL or a secret must be set");
    Assert.notEmpty(expectedAlgorithms, "Expected algorithms must be set");
    Assert.notEmpty(claimConstraints, "Claim constraints must be set");
    Assert.notNull(usernameMapping, "User name mapping must be set");
    Assert.notNull(nameMapping, "Name mapping must be set");
    Assert.notNull(emailMapping, "Email mapping must be set");
    Assert.isTrue(roleMappings != null || ocStandardRoleMappings,
            "Role mappings must be set if ocStandardRoleMappings is false");

    if (jwksUrl != null) {
      jwkProvider = new GuavaCachedUrlJwkProvider(jwksUrl, jwksCacheExpiresIn, TimeUnit.MINUTES);
    }
    userReferenceProvider.setRoleProvider(new JWTRoleProvider(securityService, userReferenceProvider));
    cache = CacheBuilder.newBuilder()
        .maximumSize(jwtCacheSize)
        .expireAfterWrite(jwtCacheExpiresIn, TimeUnit.MINUTES)
        .build();
  }

  @Override
  public String handleToken(String token) {
    try {
      String signature = extractSignature(token);
      CachedJWT cachedJwt = cache.getIfPresent(signature);

      if (cachedJwt == null) {
        // JWT hasn't been cached before, so validate all claims
        DecodedJWT jwt = decodeAndValidate(token);
        String username = extractUsername(jwt);

        try {
          if (userDetailsService.loadUserByUsername(username) != null) {
            existingUserLogin(username, jwt);
          }
        } catch (UsernameNotFoundException e) {
          newUserLogin(username, jwt);
        }

        userDirectoryService.invalidate(username);
        cache.put(jwt.getSignature(), new CachedJWT(jwt, username));
        return username;
      } else {
        // JWT has been cached before, so only check if it has expired
        if (cachedJwt.hasExpired()) {
          cache.invalidate(signature);
          throw new JWTVerificationException("JWT token is not valid anymore");
        }
        logger.debug("Using decoded and validated JWT from cache");
        return cachedJwt.getUsername();
      }
    } catch (JWTVerificationException | JwkException exception) {
      logger.debug(exception.getMessage());
    }

    return null;
  }

  /**
   * Decodes and validates a JWT.
   *
   * @param token The JWT string.
   * @return The decoded JWT.
   * @throws JwkException If the JWT fails to be validated.
   */
  private DecodedJWT decodeAndValidate(String token) throws JwkException {
    DecodedJWT jwt;

    if (jwksUrl != null) {
      jwt = JWTVerifier.verify(token, jwkProvider, claimConstraints);
    } else {
      jwt = JWTVerifier.verify(token, secret, claimConstraints);
    }

    if (!expectedAlgorithms.contains(jwt.getAlgorithm())) {
      throw new JWTVerificationException(
          "JWT token was signed with an unexpected algorithm '" + jwt.getAlgorithm() + "'"
      );
    }

    return jwt;
  }

  /**
   * Extracts the signature from a JWT.
   *
   * @param token The JWT string.
   * @return The JWT's signature.
   */
  private String extractSignature(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new JWTDecodeException("Given token is not in a valid JWT format");
    }
    return parts[2];
  }

  /**
   * Extracts the username from a decoded and validated JWT.
   *
   * @param jwt The decoded JWT.
   * @return The username.
   */
  private String extractUsername(DecodedJWT jwt) {
    String username = evaluateMapping(jwt, usernameMapping, false);
    Assert.isTrue(StringUtils.isNotBlank(username), "Extracted username is blank");
    return username;
  }

  /**
   * Extracts the name from a decoded and validated JWT.
   *
   * @param jwt The decoded JWT.
   * @return The name.
   */
  private String extractName(DecodedJWT jwt) {
    String name = evaluateMapping(jwt, nameMapping, true);
    Assert.isTrue(StringUtils.isNotBlank(name), "Extracted name is blank");
    return name;
  }

  /**
   * Extracts the email from a decoded and validated JWT.
   *
   * @param jwt The decoded JWT.
   * @return The email.
   */
  private String extractEmail(DecodedJWT jwt) {
    String email = evaluateMapping(jwt, emailMapping, true);
    Assert.isTrue(StringUtils.isNotBlank(email), "Extracted email is blank");
    return email;
  }

  /**
   * Extracts the roles from a decoded and validated JWT.
   *
   * @param jwt The decoded JWT.
   * @return The roles.
   */
  private Set<JpaRole> extractRoles(DecodedJWT jwt) {
    JpaOrganization organization = fromOrganization(securityService.getOrganization());
    Set<JpaRole> roles = new HashSet<>();
    Consumer<String> addRole = (String role) -> {
      if (StringUtils.isNotBlank(role)) {
        roles.add(new JpaRole(role, organization));
      }
    };

    // Evaluate the standard role mapping if specified
    if (ocStandardRoleMappings) {
      // Read `role` claim
      try {
        var rolesClaim = jwt.getClaim("roles");
        if (rolesClaim != null && !rolesClaim.isNull()) {
          for (String r : rolesClaim.asArray(String.class)) {
            addRole.accept(r);
          }
        }
      } catch (JWTDecodeException e) {
        logger.debug("claim 'roles' is not an array of strings, ignoring");
      }

      // Read `oc` claim
      try {
        var ocClaim = jwt.getClaim("oc");
        if (ocClaim != null && !ocClaim.isNull()) {
          for (var entry : ocClaim.asMap().entrySet()) {
            var key = entry.getKey();
            var parts = key.split(":", 2);
            if (parts.length != 2) {
              logger.debug("key in 'oc' claim does not start with 'x:' -> ignoring");
              continue;
            }
            var type = parts[0];
            var id = parts[1];

            try {
              for (var actionObj : (List<?>) entry.getValue()) {
                var action = (String) actionObj;
                if (action.isBlank()) {
                  continue;
                }

                if (type.equals("e")) {
                  addRole.accept(SecurityUtil.getEpisodeRoleId(id, action));
                } else {
                  logger.debug("in 'oc' claim: granting access to item type '{}' is not yet supported", type);
                }
              }
            } catch (ClassCastException e) {
              logger.debug("value in 'oc' claim is not a string array -> ignoring");
              continue;
            }
          }
        }
      } catch (JWTDecodeException e) {
        logger.debug("claim 'oc' is not an array of strings, ignoring");
      }
    }

    for (String mapping : (roleMappings == null ? new ArrayList<String>() : roleMappings)) {
      ExpressionParser parser = new SpelExpressionParser();
      Expression exp = parser.parseExpression(mapping);
      Object value = exp.getValue(jwt.getClaims());
      if (value != null) {
        // We allow the expression to either return a string directly, or a list/array of strings.
        if (value instanceof String) {
          addRole.accept((String) value);
        } else if (value.getClass().isArray()) {
          for (var role : (Object[]) value) {
            addRole.accept((String) role);
          }
        } else {
          for (var role : (List<?>) value) {
            addRole.accept((String) role);
          }
        }
      }
    }
    Assert.notEmpty(roles, "No roles could be extracted");
    return roles;
  }

  /**
   * Evaluates a mapping given in SpEL on a decoded JWT.
   *
   * @param jwt The decoded JWT.
   * @param mapping The mapping.
   * @param ensureEncoding Whether to ensure UTF_8 encoding.
   *
   * @return The string evaluated from the mapping.
   */
  private String evaluateMapping(DecodedJWT jwt, String mapping, boolean ensureEncoding) {
    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression(mapping);
    String value = exp.getValue(jwt.getClaims(), String.class);
    if (ensureEncoding) {
      value = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
    return value;
  }

  /**
   * Handles a new user login.
   *
   * @param username The username.
   * @param jwt The decoded JWT.
   */
  public void newUserLogin(String username, DecodedJWT jwt) {
    // Create a new user reference
    JpaUserReference userReference = new JpaUserReference(username, extractName(jwt), extractEmail(jwt), MECH_JWT,
        new Date(), fromOrganization(securityService.getOrganization()), extractRoles(jwt));

    logger.debug("JWT user '{}' logged in for the first time", username);
    userReferenceProvider.addUserReference(userReference, MECH_JWT);
  }

  /**
   * Handles an existing user login.
   *
   * @param username The username.
   * @param jwt The decoded JWT.
   */
  public void existingUserLogin(String username, DecodedJWT jwt) {
    Organization organization = securityService.getOrganization();

    // Load the user reference
    JpaUserReference userReference = userReferenceProvider.findUserReference(username, organization.getId());
    if (userReference == null) {
      throw new UsernameNotFoundException("User reference '" + username + "' was not found");
    }

    // Update the reference
    userReference.setName(extractName(jwt));
    userReference.setEmail(extractEmail(jwt));
    userReference.setLastLogin(new Date());
    userReference.setRoles(extractRoles(jwt));

    logger.debug("JWT user '{}' logged in", username);
    userReferenceProvider.updateUserReference(userReference);
  }

  /**
   * Converts a {@link Organization} object into a {@link JpaOrganization} object.
   *
   * @param org The {@link Organization} object.
   * @return The corresponding {@link JpaOrganization} object.
   */
  private JpaOrganization fromOrganization(Organization org) {
    if (org instanceof JpaOrganization) {
      return (JpaOrganization) org;
    }

    return new JpaOrganization(org.getId(), org.getName(), org.getServers(), org.getAdminRole(), org.getAnonymousRole(),
        org.getProperties());
  }

  /**
   * Setter for the user details service.
   *
   * @param userDetailsService The user details service.
   */
  @Reference
  public void setUserDetailsService(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  /**
   * Setter for the user directory service.
   *
   * @param userDirectoryService The user directory service.
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Setter for the security service.
   *
   * @param securityService The security service.
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Setter for the user reference provider.
   *
   * @param userReferenceProvider The user reference provider.
   */
  @Reference
  public void setUserReferenceProvider(UserReferenceProvider userReferenceProvider) {
    this.userReferenceProvider = userReferenceProvider;
  }

  /**
   * Setter for the JWKS URL.
   *
   * @param jwksUrl The JWKS URL.
   */
  public void setJwksUrl(String jwksUrl) {
    this.jwksUrl = jwksUrl;
  }

  /**
   * Setter for the JWKS cache expiration.
   *
   * @param jwksCacheExpiresIn The number of minutes after which a cached JWKS expires.
   */
  public void setJwksCacheExpiresIn(int jwksCacheExpiresIn) {
    this.jwksCacheExpiresIn = jwksCacheExpiresIn;
  }

  /**
   * Setter for the secret used for JWT validation.
   *
   * @param secret The secret.
   */
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Setter for the expected algorithms.
   *
   * @param expectedAlgorithms The expected algorithms.
   */
  public void setExpectedAlgorithms(List<String> expectedAlgorithms) {
    this.expectedAlgorithms = expectedAlgorithms;
  }

  /**
   * Setter for the claim constraints.
   *
   * @param claimConstraints The claim constraints.
   */
  public void setClaimConstraints(List<String> claimConstraints) {
    this.claimConstraints = claimConstraints;
  }

  /**
   * Setter for the username mapping.
   * @param usernameMapping The username mapping.
   */
  public void setUsernameMapping(String usernameMapping) {
    this.usernameMapping = usernameMapping;
  }

  /**
   * Setter for the name mapping.
   *
   * @param nameMapping The name mapping.
   */
  public void setNameMapping(String nameMapping) {
    this.nameMapping = nameMapping;
  }

  /**
   * Setter for the email mapping.
   * @param emailMapping The email mapping.
   */
  public void setEmailMapping(String emailMapping) {
    this.emailMapping = emailMapping;
  }

  public void setOcStandardRoleMappings(boolean ocStandardRoleMappings) {
    this.ocStandardRoleMappings = ocStandardRoleMappings;
  }

  /**
   * Setter for the role mappings.
   *
   * @param roleMappings The role mappings.
   */
  public void setRoleMappings(List<String> roleMappings) {
    this.roleMappings = roleMappings;
  }

  /**
   * Setter for the JWT cache size.
   *
   * @param jwtCacheSize The JWT cache size.
   */
  public void setJwtCacheSize(int jwtCacheSize) {
    this.jwtCacheSize = jwtCacheSize;
  }

  /**
   * Setter for the JWT cache expiration.
   *
   * @param jwtCacheExpiresIn The number of minutes after which a cached JWT expires.
   */
  public void setJwtCacheExpiresIn(int jwtCacheExpiresIn) {
    this.jwtCacheExpiresIn = jwtCacheExpiresIn;
  }

}
