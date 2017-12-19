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
package org.opencastproject.security.urlsigning;

import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;

/**
 * Implementation of a {@link MediaPackageSerializer} that will securely sign urls of a Mediapackage.
 */
public class SigningMediaPackageSerializer implements MediaPackageSerializer, ManagedService {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SigningMediaPackageSerializer.class);

  /** Security service to use for the client's IP address */
  private SecurityService securityService;

  /** URL Signing Service for Securing Content. */
  private UrlSigningService urlSigningService;

  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;

  /** Signing of the URL should probably be something of the last things to do */
  public static final int RANKING = -1000;

  /**
   * Creates a new and unconfigured package serializer that will not be able to perform any redirecting.
   */
  public SigningMediaPackageSerializer() {
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for UrlSigningService */
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  /** OSGi callback if properties file is present */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
            this.getClass().getSimpleName());
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
    return uri;
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
    return sign(uri);
  }

  @Override
  public int getRanking() {
    return RANKING;
  }

  @Override
  public String toString() {
    return "URL Signing MediaPackage Serializer";
  }

  /**
   * This method is signing the URI with a policy to expire it.
   *
   * @param uri
   *          the URI to sign
   *
   * @return the signed URI
   * @throws URISyntaxException
   *           if the input URI contains syntax errors
   */
  private URI sign(URI uri) throws URISyntaxException {
    String path = uri.toString();
    if (urlSigningService != null && urlSigningService.accepts(path)) {
      try {
        String clientIP = null;
        if (signWithClientIP) {
          clientIP = securityService.getUserIP();
        }
        path = urlSigningService.sign(path, expireSeconds, null, clientIP);
      } catch (UrlSigningException e) {
        logger.debug("Unable to sign url '" + path + "' so not adding a signed query string.");
      }
    }
    return new URI(path);
  }

  protected Long getExpirationSeconds() {
    return expireSeconds;
  }

}
