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

import org.opencastproject.security.api.Organization;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class AbstractUrlSigningProvider implements UrlSigningProvider, ManagedService {
  /** The prefix for key configuration keys */
  public static final String KEY_PROPERTY_PREFIX = "key";
  /** The attribute name in the configuration file to define the encryption key. */
  public static final String SECRET = "secret";
  /** The attribute name in the configuration file to define the matching url. */
  public static final String URL = "url";
  /** The attribute name in the configuration file to define the organization owning the key. */
  public static final String ORGANIZATION = "organization";

  /** Value indicating that the key can be used by any organization */
  public static final String ANY_ORGANIZATION = "*";

  /** The configuration key used for the exlusion list */
  public static final String EXCLUSION_PROPERTY_KEY = "exclude.url.pattern";

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
   * A class representing a URL signing key.
   */
  private static class Key {
    private String id = null;
    private String secret = null;
    private String organizationId = ANY_ORGANIZATION;

    Key(String id) {
      this.id = id;
    }

    boolean supports(String organizationId) {
      return this.organizationId.equals(ANY_ORGANIZATION) || this.organizationId.equals(organizationId);
    }
  }

  /** A mapping of URL prefixes to keys used to lookup keys for a given URL. */
  private TreeMap<String, Key> urls = new TreeMap<>();

  /** A regular expression pattern used to identify URLs that shall not be signed. Can be null */
  private Pattern exclusionPattern;

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
    return Collections.unmodifiableSet(urls.keySet());
  }

  /**
   * Get{@link Key} for a given URL.
   * This method supports multi-tenancy in means of only returning keys that can be used by the current
   * organization. In case the current organization cannot be determined, no key will be returned. 
   *
   * @param baseUrl
   *          The URL that needs to be signed.
   * @return The {@link Key} if it is available.
   */
  private Key getKey(String baseUrl) {
    /* Optimization: Use TreeMap.floorEntry that can retrieve the greatest URL equal to or greater than 'baseUrl'
       in O(log(n)). As we are trying to find an URL that is a prefix of 'baseUrl', candidate.getKey() either is
       that URL (needs to be checked!) or there is no such URL. */
    Map.Entry<String, Key> candidate = urls.floorEntry(baseUrl);
    if (candidate != null && baseUrl.startsWith(candidate.getKey())) {
      Key key = candidate.getValue();

      // Don't accept URLs without an organization context
      // (for example from the ServiceRegistry JobProducerHeartbeat)
      Organization organization = securityService.getOrganization();
      if (organization != null && key.supports(organization.getId())) {
        return key;
      }
    }
    return null;
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    getLogger().info("Updating {}", toString());
    if (properties == null) {
      getLogger().warn("{} is unconfigured", toString());
      return;
    }

    // Collect configuration in a new map so we don't partially override the old one in case of error
    TreeMap<String, Key> urls = new TreeMap<>();
    Pattern exclusionPattern = null;

    // Temporary list of key entries to simplify building up the keys
    Map<String, Key> keys = new HashMap<>();

    Enumeration<String> propertyKeys = properties.keys();
    while (propertyKeys.hasMoreElements()) {
      String propertyKey = propertyKeys.nextElement();

      if (propertyKey.startsWith(KEY_PROPERTY_PREFIX + ".")) {

        // We expected the parts [KEY_PROPERTY_PREFIX, id, attribute] or [KEY_PROPERTY_PREFIX, id, URL, name]
        String[] parts = Arrays.stream(propertyKey.split("\\.")).map(String::trim).toArray(String[]::new);
        if ((parts.length != 3) && !(parts.length == 4 && URL.equals(parts[2]))) {
          throw new ConfigurationException(propertyKey, "Wrong property key format");
        }

        String propertyValue = StringUtils.trimToNull(Objects.toString(properties.get(propertyKey), null));
        if (propertyValue == null) {
          throw new ConfigurationException(propertyKey, "Can't be null or empty");
        }

        String id = parts[1];
        Key currentKey = keys.computeIfAbsent(id, __ -> new Key(id));

        String attribute = parts[2];
        switch (attribute) {
          case ORGANIZATION:
            currentKey.organizationId = propertyValue;
            break;
          case URL:
            if (urls.keySet().stream().anyMatch(v -> propertyValue.startsWith(v) || (v.startsWith(propertyValue)))) {
              throw new ConfigurationException(propertyKey,
                      "There is already a key configuration for a URL with the prefix " + propertyValue);
            }
            /* We explicitely support multiple URLs that map to the same key */
            urls.put(propertyValue, currentKey);
            break;
          case SECRET:
            currentKey.secret = propertyValue;
            break;
          default:
            throw new ConfigurationException(propertyKey, "Unknown attribute " + attribute + " for key " + id);
        }
      } else if (EXCLUSION_PROPERTY_KEY.equals(propertyKey)) {
        String propertyValue = Objects.toString(properties.get(propertyKey), "");
        if (!StringUtils.isEmpty(propertyValue)) {
          exclusionPattern = Pattern.compile(propertyValue);
        }
        getLogger().debug("Exclusion pattern: {}", propertyValue);
      }
    }

    /* Validate key entries */
    for (Key key : keys.values()) {
      if (key.secret == null) {
        throw new ConfigurationException(key.id, "No secret set");
      }
    }

    // Has the rewriter been fully configured
    if (urls.size() == 0) {
      getLogger().info("{} configured to not sign any urls.", toString());
    }

    this.urls = urls;
    this.exclusionPattern = exclusionPattern;
  }

  private boolean isExcluded(String url) {
    boolean isExcluded = false;
    Pattern exclusionPattern = this.exclusionPattern;
    if (exclusionPattern != null) {
      Matcher matcher = exclusionPattern.matcher(url);
      isExcluded = matcher.matches();
    }
    return isExcluded;
  }

  private boolean isValid(String url) {
    try {
      new URI(url);
      return true;
    } catch (URISyntaxException e) {
      getLogger().debug("Unable to support url {} because", url, e);
      return false;
    }
  }

  @Override
  public boolean accepts(String baseUrl) {
    return isValid(baseUrl) && !isExcluded(baseUrl) && getKey(baseUrl) != null;
  }

  @Override
  public String sign(Policy policy) throws UrlSigningException {
    String url = policy.getBaseUrl();
    Key key = getKey(url);
    if (isExcluded(url) || key == null) {
      throw UrlSigningException.urlNotSupported();
    }

    policy.setResourceStrategy(getResourceStrategy());

    try {
      URI uri = new URI(url);
      List<NameValuePair> queryStringParameters = new ArrayList<>();
      if (uri.getQuery() != null) {
        queryStringParameters = URLEncodedUtils.parse(uri.getQuery(), StandardCharsets.UTF_8);
      }
      queryStringParameters.addAll(URLEncodedUtils.parse(
              ResourceRequestUtil.policyToResourceRequestQueryString(policy, key.id, key.secret),
              StandardCharsets.UTF_8));
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
              URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8), null).toString();
    } catch (Exception e) {
      getLogger().error("Unable to create signed URL because {}", ExceptionUtils.getStackTrace(e));
      throw new UrlSigningException(e);
    }
  }
}
