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
package org.opencastproject.mediapackage;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a {@link MediaPackageSerializer} that will rewrite urls of a Mediapackage.
 */
public class RedirectingMediaPackageSerializer implements MediaPackageSerializer, ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RedirectingMediaPackageSerializer.class);

  /** The redirect serializer should be invoked after the default serializer */
  public static final int RANKING = 100;

  /** Configuration option for the source prefix */
  public static final String OPT_SOURCE_PREFIX = "source";

  /** Configuration option for the destination prefix */
  public static final String OPT_DESINATION_PREFIX = "destination";

  /** Map containing source and destination prefixes */
  private final Map<URI, URI> redirects = new HashMap<URI, URI>();

  /**
   * Creates a new and unconfigured package serializer that will not be able to perform any redirecting.
   */
  public RedirectingMediaPackageSerializer() {
  }

  /**
   * Creates a new package serializer that enables rewriting of urls starting with <code>sourcePrefix</code> to strart
   * with <code>destintionPrefix</code>.
   *
   * @param sourcePrefix
   *          the original url prefix
   * @param destinationPrefix
   *          the new url prefix
   */
  public RedirectingMediaPackageSerializer(URI sourcePrefix, URI destinationPrefix) {
    addRedirect(sourcePrefix, destinationPrefix);
  }

  /**
   * Returns the current set of redirects.
   *
   * @return the redirects
   */
  public Map<URI, URI> getRedirections() {
    return Collections.unmodifiableMap(redirects);
  }

  /**
   * Adds a redirect to the set of configured redirections.
   *
   * @param sourcePrefix
   *          the source prefix
   * @param destinationPrefix
   *          the destination prefix
   * @throws IllegalArgumentException
   *           if <code>sourcePrefix</code> or <code>destinationPrefix</code> is <code>null</code>
   * @throws IllegalStateException
   *           if a redirect for <code>sourcePrefix</code> has already been configured
   */
  public void addRedirect(URI sourcePrefix, URI destinationPrefix) {
    if (sourcePrefix == null)
      throw new IllegalArgumentException("Source prefix must not be null");
    if (destinationPrefix == null)
      throw new IllegalArgumentException("Destination prefix must not be null");
    if (redirects.containsKey(sourcePrefix))
      throw new IllegalStateException("Source prefix '" + sourcePrefix + "' already registered");
    redirects.put(sourcePrefix, destinationPrefix);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageSerializer#encodeURI(URI)
   */
  @Override
  public URI encodeURI(URI uri) throws URISyntaxException {
    if (uri == null)
      throw new IllegalArgumentException("Argument uri is null");
    return rewrite(uri, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageSerializer#decodeURI(URI)
   */
  @Override
  public URI decodeURI(URI uri) throws URISyntaxException {
    if (uri == null)
      throw new IllegalArgumentException("Argument uri is null");
    return rewrite(uri, true);
  }

  /**
   * This method is rewriting the URI with regards to its prefix.
   *
   * @param uri
   *          the URI to rewrite
   * @param reverse
   *          whether to decode instead of encode the URI
   *
   * @return the rewritten URI
   * @throws URISyntaxException
   *           if the rewritten URI contains syntax errors
   */
  private URI rewrite(URI uri, boolean reverse) throws URISyntaxException {
    String path = uri.toString();
    List<String> variations = new ArrayList<String>();
    boolean changed = true;
    while (changed) {
      changed = false;

      // Make sure we are not getting into an endless loop
      if (variations.contains(path))
        throw new IllegalStateException("Rewriting of mediapackage element '" + uri + "' experienced an endless loop");
      variations.add(path);

      // Loop over all configured redirects
      for (Map.Entry<URI, URI> entry : redirects.entrySet()) {
        URI oldPrefix = (reverse) ? entry.getValue() : entry.getKey();
        URI newPrefix = (reverse) ? entry.getKey() : entry.getValue();

        // Does the URI match the source prefix?
        String sourcePrefixString = oldPrefix.toString();
        if (!path.startsWith(sourcePrefixString))
          continue;

        // Cut off the source prefix
        path = path.substring(sourcePrefixString.length());

        // Prepend the destination prefix
        path = new StringBuilder(newPrefix.toString()).append(path).toString();

        changed = true;
        break;
      }
    }
    return new URI(path);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      logger.warn("Mediapackage serializer is unconfigured");
      return;
    }

    // Clear the current set of redirects
    redirects.clear();

    String sourceKey = null;
    String destinationKey = null;

    int i = 1;
    while (true) {

      // Create the configuration keys
      sourceKey = new StringBuilder(OPT_SOURCE_PREFIX).append(".").append(i).toString();
      destinationKey = new StringBuilder(OPT_DESINATION_PREFIX).append(".").append(i).toString();

      logger.debug("Looking for configuration of {} and {}", sourceKey, destinationKey);

      // Read the source and destination prefixes
      String sourcePrefixOpt = StringUtils.trimToNull((String) properties.get(sourceKey));
      String destinationPrefixOpt = StringUtils.trimToNull((String) properties.get(destinationKey));

      // Has the rewriter been fully configured
      if (sourcePrefixOpt == null || destinationPrefixOpt == null) {
        logger.info("Mediapackage serializer configured to transparent mode");
        break;
      }

      URI sourcePrefix = null;
      URI destinationPrefix = null;

      try {
        sourcePrefix = new URI(sourcePrefixOpt);
      } catch (URISyntaxException e) {
        throw new ConfigurationException(sourceKey, e.getMessage());
      }

      // Read the source prefix
      try {
        destinationPrefix = new URI(destinationPrefixOpt);
      } catch (URISyntaxException e) {
        throw new ConfigurationException(destinationKey, e.getMessage());
      }

      // Store the redirect
      try {
        addRedirect(destinationPrefix, sourcePrefix);
        logger.info("Mediapackage serializer will rewrite element uris from starting with '{}' to start with '{}'", destinationPrefix, sourcePrefix);
      } catch (IllegalStateException e) {
        throw new ConfigurationException(sourceKey, e.getMessage());
      }

      i++;
    }

    // Has the rewriter been fully configured
    if (redirects.size() == 0) {
      logger.info("Mediapackage serializer configured to transparent mode");
      return;
    }

  }

  @Override
  public int getRanking() {
    return RANKING;
  }

}
