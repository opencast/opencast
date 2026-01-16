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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * JWK provider that fetches and caches jwk sets
 */
public class JWKSetProvider {

  /** Logging facility. */
  private static final Logger logger = LoggerFactory.getLogger(JWKSetProvider.class);

  private JWKSource<SecurityContext> jwkSource;
  private JWKSelector selector;

  /**
   * Creates a new cached provider from a JWKs URL.
   *
   * @param jwksUrl The URL where JWKs are published.
   * @param ttl time-to-live in milliseconds
   * @param refreshTimeout in milliseconds
   */
  public JWKSetProvider(String jwksUrl, long ttl, long refreshTimeout) {
    URL url = urlFromString(jwksUrl);

    selector = new JWKSelector(
        new JWKMatcher.Builder()
            .build());

    jwkSource = JWKSourceBuilder.create(url)
        .cache(ttl, refreshTimeout)
        .retrying(true)
        .build();
  }

  /**
   * Converts a URL string into a {@link URL}.
   *
   * @param url The URL string.
   * @return The {@link URL}.
   */
  private static URL urlFromString(String url) {
    checkArgument(!isNullOrEmpty(url), "A URL is required");
    try {
      final URI uri = new URI(url).normalize();
      return uri.toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalArgumentException("Invalid JWKS URI", e);
    }
  }

  /**
   * Getter for all JWKs.
   *
   * @return The JWKs.
   */
  public List<JWK> getAll() {
    try {
      return jwkSource.get(selector, null);
    } catch (KeySourceException e) {
      logger.error("Error while loading from JWKS cache: " + e.getMessage());
      return Collections.emptyList();
    }
  }
}
