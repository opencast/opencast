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
package org.opencastproject.security.urlsigning.provider.impl;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider;
import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceStrategy;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractUrlSigningProvider implements UrlSigningProvider, ManagedService {
  /** The prefix for key entry configuration keys */
  public static final String KEY_ENTRY_PREFIX = "key";
  /** The postfix in the configuration file to define the encryption key. */
  public static final String SECRET = "secret";
  /** The postfix in the configuration file to define the matching url. */
  public static final String URL = "url";
  /** The postfix in the configuration file to define the organization owning the key. */
  public static final String ORGANIZATION = "organization";

  /** Value indicating that the key can be used by any organization */
  public static final String ANY_ORGANIZATION = "*";

  /** The security service */
  protected SecurityService securityService;

  /**
   * @return The method that an implementation class will convert base urls to resource urls.
   */
  public abstract ResourceStrategy getResourceStrategy();

  /**
   * @return The logger to use for this signing provider.
   */
  public abstract Logger getLogger();

  /**
   * A class to contain the necessary key entries for url signing.
   */
  private static class KeyEntry {
    private String key = null;
    private String url = null;
    private String organization = ANY_ORGANIZATION;
  }

  /** The map to contain the list of keys, their ids and the urls they match. */
  private Map<String, KeyEntry> keys = new HashMap<>();

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @return The current set of url beginnings this signing provider is looking for.
   */
  public Set<String> getUris() {
    return keys.values().stream()
            .map(keyEntry -> keyEntry.url)
            .collect(Collectors.collectingAndThen(
                    Collectors.toSet(),
                    Collections::unmodifiableSet));
  }

  /**
   * If available get a {@link KeyEntry} if there is a matching Url matcher.
   *
   * @param baseUrl
   *          The url to check against the possible matchers.
   * @return The {@link KeyEntry} if it is available.
   */
  private Optional<Map.Entry<String, KeyEntry>> getKeyEntry(String baseUrl) {
    return keys.entrySet().stream()
            .filter(entry -> baseUrl.startsWith(entry.getValue().url))
            .findAny();
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    getLogger().info("Updating {}", toString());
    if (properties == null) {
      getLogger().warn("{} is unconfigured", toString());
      return;
    }

    // Collect configuration in a new map so we don't partially override the old one in case of error
    Map<String, KeyEntry> keys = new HashMap<>();

    Enumeration<String> propertyKeys = properties.keys();
    while (propertyKeys.hasMoreElements()) {
      String propertyKey = propertyKeys.nextElement();

      String keyEntryProperty = StringUtils.removeStart(propertyKey, KEY_ENTRY_PREFIX + ".");
      if (keyEntryProperty == propertyKey) continue;

      String[] parts = Arrays.stream(keyEntryProperty.split("\\."))
              .map(String::trim)
              .toArray(String[]::new);
      if (parts.length != 2) {
        throw new ConfigurationException(propertyKey, "wrong property key format");
      }

      String id = parts[0];
      KeyEntry currentKeyEntry = keys.computeIfAbsent(id, __ -> new KeyEntry());

      String attribute = parts[1];
      String propertyValue = StringUtils.trimToNull(Objects.toString(properties.get(propertyKey), null));
      if (propertyValue == null) {
        throw new ConfigurationException(propertyKey, "can't be null or empty");
      }
      switch (attribute) {
        case ORGANIZATION:
          currentKeyEntry.organization = propertyValue;
          break;
        case URL:
          if (keys.values().stream()
                  .map(keyEntry -> keyEntry.url)
                  .filter(Objects::nonNull)
                  .anyMatch(url -> propertyValue.startsWith(url) || (url != null && url.startsWith(propertyValue)))) {
            throw new ConfigurationException(propertyKey,
                    "there is already a key configuration for a URL with the prefix " + propertyValue);
          }
          currentKeyEntry.url = propertyValue;
          break;
        case SECRET:
          currentKeyEntry.key = propertyValue;
          break;
        default:
          throw new ConfigurationException(propertyKey, "unknown attribute " + attribute + " for key " + id);
      }
    }

    keys = keys.entrySet().stream()
            .filter(entry -> entry.getValue().key != null && entry.getValue().url != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Has the rewriter been fully configured
    if (keys.size() == 0) {
      getLogger().info("{} configured to not sign any urls.", toString());
    }

    this.keys = keys;
  }

  @Override
  public boolean accepts(String baseUrl) {
    try {
      new URI(baseUrl);
    } catch (URISyntaxException e) {
      getLogger().debug("Unable to support url {} because", baseUrl, e);
      return false;
    }

    // Don't accept URLs without an organization context
    // (for example from the ServiceRegistry JobProducerHeartbeat)
    if (securityService.getOrganization() == null)
      return false;

    String orgId = securityService.getOrganization().getId();

    Optional<Map.Entry<String, KeyEntry>> keyEntry = getKeyEntry(baseUrl);
    return keyEntry
            .map(entry -> entry.getValue().organization)
            .map(organization -> organization.equals(ANY_ORGANIZATION) || organization.equals(orgId))
            .orElse(false);
  }

  @Override
  public String sign(Policy policy) throws UrlSigningException {
    if (!accepts(policy.getBaseUrl())) {
      throw UrlSigningException.urlNotSupported();
    }

    policy.setResourceStrategy(getResourceStrategy());

    try {
      // Get the key that matches this URI since there must be one that matches as the base url has been accepted.
      Map.Entry<String, KeyEntry> keyEntry = getKeyEntry(policy.getBaseUrl()).get();
      URI uri = new URI(policy.getBaseUrl());
      List<NameValuePair> queryStringParameters = new ArrayList<>();
      if (uri.getQuery() != null) {
        queryStringParameters = URLEncodedUtils.parse(new URI(policy.getBaseUrl()).getQuery(), StandardCharsets.UTF_8);
      }
      queryStringParameters.addAll(URLEncodedUtils.parse(
              ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyEntry.getKey(), keyEntry.getValue().key),
              StandardCharsets.UTF_8));
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
              URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8), null).toString();
    } catch (Exception e) {
      getLogger().error("Unable to create signed URL because {}", ExceptionUtils.getStackTrace(e));
      throw new UrlSigningException(e);
    }
  }
}
