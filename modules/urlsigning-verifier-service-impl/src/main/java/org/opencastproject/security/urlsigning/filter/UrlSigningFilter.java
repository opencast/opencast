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

package org.opencastproject.security.urlsigning.filter;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.verifier.UrlSigningVerifier;
import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UrlSigningFilter implements Filter, ManagedService {
  /** The prefix in the configuration file to define the regex that will match a url path. */
  public static final String URL_REGEX_PREFIX = "url.regex";
  /** The property in the configuration file to enable or disable this filter. */
  public static final String ENABLE_FILTER_CONFIG_KEY = "enabled";

  /** The property in the configuration file to enable or disable strict checking of the resource. */
  public static final String STRICT_FILTER_CONFIG_KEY = "strict";

  private static final Logger logger = LoggerFactory.getLogger(UrlSigningFilter.class);

  private UrlSigningVerifier urlSigningVerifier;

  private List<String> urlRegularExpressions = new LinkedList<>();

  private boolean enabled = true;

  private boolean strict = true;

  /** OSGi DI */
  public void setUrlSigningVerifier(UrlSigningVerifier urlSigningVerifier) {
    this.urlSigningVerifier = urlSigningVerifier;
  }

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    if (!enabled) {
      chain.doFilter(request, response);
      return;
    }

    if (urlRegularExpressions.size() == 0) {
      logger.debug("There are no regular expressions configured to protect endpoints, skipping filter.");
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (!("GET".equalsIgnoreCase(httpRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpRequest.getMethod()))) {
      logger.debug("The request '{}' is not a GET or HEAD request so skipping the filter.",
              httpRequest.getRequestURL());
      chain.doFilter(request, response);
      return;
    }

    boolean matches = false;
    for (String urlRegularExpression : urlRegularExpressions) {
      Pattern p = Pattern.compile(urlRegularExpression);
      Matcher m = p.matcher(httpRequest.getRequestURL());
      if (m.matches()) {
        matches = true;
        break;
      }
    }

    if (!matches) {
      logger.debug("The request '{}' doesn't match any of the configured regular expressions so skipping the filter.",
              httpRequest.getRequestURL());
      chain.doFilter(request, response);
      return;
    }

    ResourceRequest resourceRequest;
    try {
      resourceRequest = urlSigningVerifier.verify(httpRequest.getQueryString(), httpRequest.getRemoteAddr(),
              httpRequest.getRequestURL().toString(), strict);

      if (resourceRequest == null) {
        logger.error("Unable to process httpRequest '{}' because we got a null object as the verification.",
                httpRequest.getRequestURL());
        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to process http request because we got a null object as the verification.");
        return;
      }

      switch (resourceRequest.getStatus()) {
        case Ok:
          logger.trace("The request '{}' matched a regular expression path and was accepted as a properly signed url.",
                  httpRequest.getRequestURL());
          chain.doFilter(httpRequest, response);
          return;
        case BadRequest:
          logger.debug(
                  "Unable to process httpRequest '{}' because it was rejected as a Bad Request, usually a problem with query string: {}",
                  httpRequest.getRequestURL(), resourceRequest.getRejectionReason());
          httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
          return;
        case Forbidden:
          logger.debug(
                  "Unable to process httpRequest '{}' because is was rejected as Forbidden, usually a problem with making policy matching the signature: {}",
                  httpRequest.getRequestURL(), resourceRequest.getRejectionReason());
          httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        case Gone:
          logger.debug("Unable to process httpRequest '{}' because is was rejected as Gone: {}",
                  httpRequest.getRequestURL(), resourceRequest.getRejectionReason());
          httpResponse.sendError(HttpServletResponse.SC_GONE);
          return;
        default:
          logger.error(
                  "Unable to process httpRequest '{}' because is was rejected as status {} which is not a status we should be handling here. This must be due to a code change and is a bug.: {}",
                  httpRequest.getRequestURL(), resourceRequest.getStatus(), resourceRequest.getRejectionReason());
          httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return;

      }
    } catch (UrlSigningException e) {
      logger.error("Unable to verify request for '{}' with query string '{}' from host '{}' because:",
              httpRequest.getRequestURL(), httpRequest.getQueryString(), httpRequest.getRemoteAddr(), e);
      httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              String.format("%s is unable to verify request for '%s' with query string '%s' from host '%s' because: %s",
                      getName(), httpRequest.getRequestURL(), httpRequest.getQueryString(), httpRequest.getRemoteAddr(),
                      ExceptionUtils.getStackTrace(e)));
      return;
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void destroy() {
  }

  private String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    logger.info("Updating UrlSigningFilter");

    Option<String> enableFilterConfig = OsgiUtil.getOptCfg(properties, ENABLE_FILTER_CONFIG_KEY);
    if (enableFilterConfig.isSome()) {
      enabled = Boolean.parseBoolean(enableFilterConfig.get());
      if (enabled) {
        logger.info("The UrlSigningFilter is configured to be enabled.");
      } else {
        logger.info("The UrlSigningFilter is configured to be disabled.");
      }
    } else {
      enabled = true;
      logger.info(
              "The UrlSigningFilter is enabled by default. Use the '{}' property in its properties file to enable or disable it.",
              ENABLE_FILTER_CONFIG_KEY);
    }

    Option<String> strictFilterConfig = OsgiUtil.getOptCfg(properties, STRICT_FILTER_CONFIG_KEY);
    if (strictFilterConfig.isSome()) {
      strict = Boolean.parseBoolean(strictFilterConfig.get());
      if (strict) {
        logger.info("The UrlSigningFilter is configured to use strict checking of resource URLs.");
      } else {
        logger.info("The UrlSigningFilter is configured to not use strict checking of resource URLs.");
      }
    } else {
      strict = true;
      logger.info(
              "The UrlSigningFilter is using strict checking of resource URLs by default. Use the '{}' property in its properties file to enable or disable it.",
              STRICT_FILTER_CONFIG_KEY);
    }

    // Clear the current set of keys
    urlRegularExpressions.clear();

    if (properties == null) {
      logger.warn("UrlSigningFilter has no paths to match");
      return;
    }

    Enumeration<String> propertyKeys = properties.keys();
    while (propertyKeys.hasMoreElements()) {
      String propertyKey = propertyKeys.nextElement();
      if (!propertyKey.startsWith(URL_REGEX_PREFIX)) {
        continue;
      }

      String urlRegularExpression = StringUtils.trimToNull((String) properties.get(propertyKey));
      logger.debug("Looking for configuration of {} and found '{}'", propertyKey, urlRegularExpression);
      // Has the url signing provider been fully configured
      if (urlRegularExpression == null) {
        logger.debug(
                "Unable to configure url regular expression with id '{}' because it is missing. Stopping to look for new keys.",
                propertyKey);
        break;
      }

      urlRegularExpressions.add(urlRegularExpression);
    }

    if (urlRegularExpressions.size() == 0) {
      logger.info("UrlSigningFilter configured to not verify any urls.");
      return;
    }
    logger.info("Finished updating UrlSigningFilter");
  }

}
