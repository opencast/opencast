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

package org.opencastproject.assetmanager.auth;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.StaticFileAuthorization;
import org.opencastproject.security.api.User;

import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * A simple static file authorization service which allows access to a configured set of patterns.
 */
@Component(
    property = {
        "service.description=AssetManager based StaticFileAuthorization",
    },
    immediate = true,
    service = StaticFileAuthorization.class
)
public class AssetManagerStaticFileAuthorization implements StaticFileAuthorization {

  private static final Logger logger = LoggerFactory.getLogger(AssetManagerStaticFileAuthorization.class);

  protected EntityManagerFactory entityManagerFactory;
  private SecurityService securityService;

  private Pattern staticFilePattern = Pattern.compile("^/([^/]+)/(?:api|internal)/([^/]+)/.*$");

  // Settings for role filter
  private boolean includeAPIRoles = false;
  private boolean includeCARoles = false;
  private boolean includeUIRoles = false;

  @Reference
  public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    List<Pattern> newPattern = new ArrayList<>();
    Dictionary<String, Object> properties = cc != null ? cc.getProperties() : new Hashtable<>();
    staticFilePattern = Pattern.compile(Objects.toString(
        properties.get("pattern"),
        "^/([^/]+)/(?:api|internal)/([^/]+)/.*$"));
    includeAPIRoles = BooleanUtils.toBoolean(Objects.toString(properties.get("evaluate.roles.api"), null));
    includeCARoles = BooleanUtils.toBoolean(Objects.toString(properties.get("evaluate.roles.ca"), null));
    includeUIRoles = BooleanUtils.toBoolean(Objects.toString(properties.get("evaluate.roles.ui"), null));
    logger.info("Started authentication handler for {}", staticFilePattern);
  }

  @Override
  public List<Pattern> getProtectedUrlPattern() {
    return Collections.singletonList(staticFilePattern);
  }

  @Override
  public boolean verifyUrlAccess(final String path) {
    // Always allow access for admin
    final User user = securityService.getUser();
    if (user.hasRole(GLOBAL_ADMIN_ROLE)) {
      logger.debug("Allow access for admin `{}`", user);
      return true;
    }

    // Check pattern
    final Matcher m = staticFilePattern.matcher(path);
    if (!m.matches()) {
      logger.debug("Path does not match pattern. Preventing access.");
      return false;
    }

    // Check organization
    final String organizationId = m.group(1);
    if (!securityService.getOrganization().getId().equals(organizationId)) {
      logger.debug("The user's organization does not match. Preventing access.");
      return false;
    }

    if (user.getRoles().size() == 0) {
      logger.debug("User has no roles allowing access.");
      return false;
    }

    // Check role access
    final List<String> roles = user.getRoles().parallelStream()
        .map(Role::getName)
        .filter(roleFilter)
        .map((role) -> role + " | read")
        .collect(Collectors.toList());  // ["ROLE_XY | read", ...]

    StringBuilder properties = new StringBuilder("property_name = ?");
    for (int i = 1; i < roles.size(); i++) {
      properties.append(" or property_name = ?");
    }
    String sql = "select count(1) from oc_assets_properties "
        + "where val_bool = 1 "
        + "and namespace = ? "
        + "and mediapackage_id = ? "
        + "and (" + properties + ")";
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    Query q = entityManager.createNativeQuery(sql);
    q.setParameter(1, "org.opencastproject.assetmanager.security");
    q.setParameter(2, m.group(2));
    for (int i = 0; i < roles.size(); i++) {
      q.setParameter(i + 3, roles.get(i));
    }
    try {
      return ((Long) q.getSingleResult()) > 0;
    } catch (PersistenceException e) {
      Throwable parent = e.getCause();
      if (parent instanceof RuntimeException) {
        parent = parent.getCause();
        if (parent instanceof SQLSyntaxErrorException) {
          // We may get a SyntaxException if the Table does not yet exist
          // This also means that there are no access rules allowing access
          logger.info("Denying access to static file {}. {}", path, parent.getMessage());
          return false;
        }
      }
      throw e;
    }
  }

  /**
   * Filter for removing user interface roles from access control
   */
  private final java.util.function.Predicate<String> roleFilter = (name) -> (
      includeAPIRoles || !name.startsWith("ROLE_API_"))
      && (includeCARoles  || !name.startsWith("ROLE_CAPTURE_AGENT_"))
      && (includeUIRoles  || !name.startsWith("ROLE_UI_"));
}
