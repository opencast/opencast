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
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class AbstractUrlSigningProvider implements UrlSigningProvider, ManagedService {
  /** The prefix in the configuration file to define the id of the key. */
  public static final String ID_PREFIX = "id";
  /** The prefix in the configuration file to define the encryption key. */
  public static final String KEY_PREFIX = "key";
  /** The prefix in the configuration file to define the matching url. */
  public static final String URL_PREFIX = "url";

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
  private class KeyEntry {
    private final String keyId;
    private final String key;

    KeyEntry(String keyId, String key) {
      this.keyId = keyId;
      this.key = key;
    }

    public String getId() {
      return keyId;
    }

    public String getKey() {
      return key;
    }
  }

  /** The map to contain the list of keys, their ids and the urls they match. */
  private Map<String, KeyEntry> keys = new TreeMap<String, KeyEntry>();

  /**
   * Add a new key entry to the collection of keys.
   *
   * @param keyId
   *          The id of the key to add.
   * @param url
   *          The url that the matching will apply to.
   * @param key
   *          The encryption key to use for these urls.
   */
  public void addKeyEntry(String keyId, String url, String key) {
    if (keyId == null)
      throw new IllegalArgumentException("The key id prefix must not be null");
    if (url == null)
      throw new IllegalArgumentException("The url matcher prefix must not be null");
    if (key == null)
      throw new IllegalArgumentException("The key prefix must not be null");
    if (keys.containsKey(url))
      throw new IllegalStateException("Url matcher '" + url + "' already registered");
    keys.put(url, new KeyEntry(keyId, key));
  }

  /**
   * @return The current set of url beginnings this signing provider is looking for.
   */
  public Set<String> getUris() {
    return Collections.unmodifiableSet(keys.keySet());
  }

  /**
   * If available get a {@link KeyEntry} if there is a matching Url matcher.
   *
   * @param baseUrl
   *          The url to check against the possible matchers.
   * @return The {@link KeyEntry} if it is available.
   */
  private KeyEntry getKeyEntry(String baseUrl) {
    for (String uriMatcher : keys.keySet()) {
      if (baseUrl.startsWith(uriMatcher)) {
        return keys.get(uriMatcher);
      }
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    getLogger().info("Updating {}", this.toString());
    if (properties == null) {
      getLogger().warn("{} is unconfigured", this.toString());
      return;
    }

    // Clear the current set of keys
    keys.clear();

    String key = null;
    String keyId = null;
    String url = null;

    int i = 1;
    while (true) {
      // Create the configuration prefixes
      key = new StringBuilder(KEY_PREFIX).append(".").append(i).toString();
      keyId = new StringBuilder(ID_PREFIX).append(".").append(i).toString();
      url = new StringBuilder(URL_PREFIX).append(".").append(i).toString();
      getLogger().debug("Looking for configuration of {}, {}, and {}", new Object[] { key, keyId, url });
      // Read the key, keyId and url values
      String keyValue = StringUtils.trimToNull((String) properties.get(key));
      String keyIdValue = StringUtils.trimToNull((String) properties.get(keyId));
      String urlValue = StringUtils.trimToNull((String) properties.get(url));

      // Has the url signing provider been fully configured
      if (keyValue == null || keyIdValue == null || urlValue == null) {
        getLogger()
                .debug("Unable to configure key with id '{}' and url matcher '{}' because the id, key or url is missing. Stopping to look for new keys.",
                        keyIdValue, urlValue);
        break;
      }

      // Store the key
      try {
        addKeyEntry(keyIdValue, urlValue, keyValue);
        getLogger().info("{} will handle uris that start with '{}' with the key id '{}'",
                new Object[] { this.toString(), urlValue, keyIdValue });
      } catch (IllegalStateException e) {
        throw new ConfigurationException(urlValue, e.getMessage());
      }

      i++;
    }

    // Has the rewriter been fully configured
    if (keys.size() == 0) {
      getLogger().info("{} configured to not sign any urls.", this.toString());
      return;
    }

  }

  @Override
  public boolean accepts(String baseUrl) {
    try {
      new URI(baseUrl);
      for (String uriMatcher : keys.keySet()) {
        if (baseUrl.startsWith(uriMatcher)) {
          return true;
        }
      }
      return false;
    } catch (URISyntaxException e) {
      getLogger().debug("Unable to support url {} because: {}", baseUrl, ExceptionUtils.getStackTrace(e));
      return false;
    }
  }

  @Override
  public String sign(Policy policy) throws UrlSigningException {
    if (!accepts(policy.getBaseUrl())) {
      throw UrlSigningException.urlNotSupported();
    }
    // Get the key that matches this URI since there must be one that matches as the base url has been accepted.
    KeyEntry keyEntry = getKeyEntry(policy.getBaseUrl());

    policy.setResourceStrategy(getResourceStrategy());

    try {
      URI uri = new URI(policy.getBaseUrl());
      List<NameValuePair> queryStringParameters = new ArrayList<NameValuePair>();
      if (uri.getQuery() != null) {
        queryStringParameters = URLEncodedUtils.parse(new URI(policy.getBaseUrl()).getQuery(), StandardCharsets.UTF_8);
      }
      queryStringParameters.addAll(URLEncodedUtils.parse(
              ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyEntry.getId(), keyEntry.getKey()),
              StandardCharsets.UTF_8));
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), URLEncodedUtils.format(
              queryStringParameters, StandardCharsets.UTF_8), null).toString();
    } catch (Exception e) {
      getLogger().error("Unable to create signed URL because {}", ExceptionUtils.getStackTrace(e));
      throw new UrlSigningException(e);
    }
  }

}
