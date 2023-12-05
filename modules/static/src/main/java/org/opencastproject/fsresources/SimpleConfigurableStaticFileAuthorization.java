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

package org.opencastproject.fsresources;

import org.opencastproject.security.api.StaticFileAuthorization;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple static file authorization service which allows access to a configured set of patterns.
 */
@Component(
    property = {
        "service.description=Simple Configurable StaticFileAuthorization",
    },
    immediate = true,
    service = StaticFileAuthorization.class
)
public class SimpleConfigurableStaticFileAuthorization implements StaticFileAuthorization {

  private static final Logger logger = LoggerFactory.getLogger(SimpleConfigurableStaticFileAuthorization.class);

  private static final Pattern ALLOW_PATTERN = Pattern.compile("^allow\\.([^.]+)\\.pattern$");

  private List<Pattern> patterns = Collections.emptyList();


  @Activate
  public void activate(ComponentContext cc) {
    List<Pattern> newPattern = new ArrayList<>();
    if (cc != null) {
      Enumeration<String> keys = cc.getProperties().keys();
      while (keys.hasMoreElements()) {
        final String key = keys.nextElement();
        Matcher matcher = ALLOW_PATTERN.matcher(key);
        if (matcher.matches()) {
          final String patternString = Objects.toString(cc.getProperties().get(key));
          newPattern.add(Pattern.compile(patternString));
          logger.debug("Always allowing access to {}", patternString);
        }
      }
    }
    patterns = newPattern;
    logger.info("Started authentication handler for {}", patterns);
  }

  @Override
  public List<Pattern> getProtectedUrlPattern() {
    return patterns;
  }

  @Override
  public boolean verifyUrlAccess(final String path) {
    return true;
  }
}
