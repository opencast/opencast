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

package org.opencastproject.userdirectory.canvas;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component(
  property = {
    "service.description=Provides for Canvas users and roles"
  },
  immediate = true,
  service = {UserProvider.class, RoleProvider.class}
)
public class CanvasUserRoleProvider implements UserProvider, RoleProvider {

    private static final Logger logger = LoggerFactory.getLogger(CanvasUserRoleProvider.class);

    private static final String LTI_LEARNER_ROLE = "Learner";
    private static final String LTI_INSTRUCTOR_ROLE = "Instructor";
    private static final String PROVIDER_NAME = "canvas";

    /** The key to look up the organization identifier in the service configuration properties */
    private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.canvas.org";
    private static final String DEFAULT_ORGANIZATION_VALUE = "mh_default_org";

    /** The key to look up the URL of Canvas instance */
    private static final String CANVAS_URL_KEY = "org.opencastproject.userdirectory.canvas.url";
    /** The key to look up the token of the user to invoke RESTful service of Canvas */
    private static final String CANVAS_USER_TOKEN_KEY = "org.opencastproject.userdirectory.canvas.token";

    private static final String CACHE_SIZE_KEY = "org.opencastproject.userdirectory.canvas.cache.size";
    private static final Integer DEFAULT_CACHE_SIZE_VALUE = 1000;
    private static final String CACHE_EXPIRATION_KEY = "org.opencastproject.userdirectory.canvas.cache.expiration";
    private static final Integer DEFAULT_CACHE_EXPIRATION_VALUE = 60;

    /** The keys to look up which roles in Canvas should be considered as instructor roles */
    private static final String CANVAS_INSTRUCTOR_ROLES_KEY = "org.opencastproject.userdirectory.canvas.instructor.roles";
    private static final String DEFAULT_CANVAS_INSTRUCTOR_ROLES = "teacher,ta";
    /** The keys to look up which users should be ignored */
    private static final String IGNORED_USERNAMES_KEY = "org.opencastproject.userdirectory.canvas.ignored.usernames";
    private static final String DEFAULT_INGROED_USERNAMES = "admin,anonymous";

    private Organization org;
    private String url;
    private String token;
    private int cacheSize;
    private int cacheExpiration;
    private Set<String> instructorRoles;
    private Set<String> ignoredUsernames;
    private LoadingCache<String, Object> cache = null;
    private final Object nullToken = new Object();

    @Activate
    public void activate(ComponentContext cc) throws ConfigurationException {
        loadConfig(cc);
        logger.info("Activating CanvasUserRoleProvider(url={}, cacheSize={}, cacheExpiration={}, instructorRoles={}, ignoredUserNames={}",
                    url, cacheSize, cacheExpiration, instructorRoles, ignoredUsernames);
        cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Object>() {
                    @Override
                    public Object load(String id) {
                        User user = loadUserFromCanvas(id);
                        return user == null ? nullToken : user;
                    }
                });
    }

    private OrganizationDirectoryService orgDirectory;

    @Reference(name = "orgDirectory")
    public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
        this.orgDirectory = orgDirectory;
    }

    private SecurityService securityService;

    @Reference(name = "securityService")
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }


    private void loadConfig(ComponentContext cc) throws ConfigurationException {
        String orgStr = OsgiUtil.getComponentContextProperty(cc, ORGANIZATION_KEY, DEFAULT_ORGANIZATION_VALUE);
        try {
            org = orgDirectory.getOrganization(orgStr);
        } catch (NotFoundException e) {
            logger.warn("Organization {} not found!", orgStr);
            throw new ConfigurationException(ORGANIZATION_KEY, "not found");
        }
        url = OsgiUtil.getComponentContextProperty(cc, CANVAS_URL_KEY);
        if (url.endsWith("/")) {
            url = StringUtils.chop(url);
        }
        logger.debug("Canvas URL: {}", url);
        token = OsgiUtil.getComponentContextProperty(cc, CANVAS_USER_TOKEN_KEY);

        String cacheSizeStr = OsgiUtil.getComponentContextProperty(cc, CACHE_SIZE_KEY, DEFAULT_CACHE_SIZE_VALUE.toString());
        cacheSize = NumberUtils.toInt(cacheSizeStr);
        String cacheExpireStr = OsgiUtil.getComponentContextProperty(cc, CACHE_EXPIRATION_KEY, DEFAULT_CACHE_EXPIRATION_VALUE.toString());
        cacheExpiration = NumberUtils.toInt(cacheExpireStr);

        String rolesStr = OsgiUtil.getComponentContextProperty(cc, CANVAS_INSTRUCTOR_ROLES_KEY, DEFAULT_CANVAS_INSTRUCTOR_ROLES);
        instructorRoles = parsePropertyLineAsSet(rolesStr);
        logger.debug("Canvas instructor roles: {}", instructorRoles);

        String ignoredUsersStr = OsgiUtil.getComponentContextProperty(cc, IGNORED_USERNAMES_KEY, DEFAULT_INGROED_USERNAMES);
        ignoredUsernames = parsePropertyLineAsSet(ignoredUsersStr);
        logger.debug("Ignored users: {}", ignoredUsernames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Role> getRolesForUser(String userName) {
        logger.debug("getRolesForUser({})", userName);

        List<Role> roles = new ArrayList<>();

        if (ignoredUsernames.contains(userName)) {
            logger.debug("We don't answer for: {}", userName);
            return roles;
        }

        User user = loadUser(userName);
        if (user != null) {
            logger.debug("Returning cached rolset for {}", userName);
            return new ArrayList<>(user.getRoles());
        }
        logger.debug("Return empty roleset for {} - not found in Canvas", userName);
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
        logger.debug("findRoles(query={} offset={} limit={})", query, offset, limit);
        if (target == Role.Target.USER) {
            return Collections.emptyIterator();
        }
        boolean exact = true;
        if (query.endsWith("%")) {
            exact = false;
            query = StringUtils.chop(query);
        }

        if (query.isEmpty()) {
            return Collections.emptyIterator();
        }

        if (exact && !query.endsWith("_" + LTI_LEARNER_ROLE) && !query.endsWith("_" + LTI_INSTRUCTOR_ROLE)) {
            return Collections.emptyIterator();
        }

        List<Role> roles = new ArrayList<>();
        JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
        for (String siteRole: getCanvasSiteRolesByCurrentUser(query, exact)) {
            roles.add(new JaxbRole(siteRole, jaxbOrganization, "Canvas Site Role", Role.Type.EXTERNAL));
        }
        return roles.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<User> getUsers() {
        return Collections.emptyIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User loadUser(String userName) {
        logger.debug("loadUser({})", userName);
        Object user = cache.getUnchecked(userName);
        if (user == nullToken) {
            logger.debug("Returning null user from cache");
            return null;
        } else {
            logger.debug("Returning user {} from cache", userName);
            return (JaxbUser) user;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countUsers() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrganization() {
        return org.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<User> findUsers(String query, int offset, int limit) {
        if (query == null) {
            throw new IllegalArgumentException("Query must be set");
        }

        if (query.endsWith("%")) {
            query = query.substring(0, query.length() - 1);
        }
        if (query.isEmpty()) {
            return Collections.emptyIterator();
        }

        if (!verifyCanvasUser(query)) {
            return Collections.emptyIterator();
        }

        List<User> users = new ArrayList<>();
        JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
        JaxbUser queryUser = new JaxbUser(query, PROVIDER_NAME, jaxbOrganization, new HashSet<>());
        users.add(queryUser);
        return users.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate(String userName) {
        cache.invalidate(userName);
    }

    private User loadUserFromCanvas(String userName) {
        if (cache == null) {
            throw new IllegalArgumentException("The Canvas user detail service has not yet been configured");
        }

        if (ignoredUsernames.contains(userName)) {
            cache.put(userName, nullToken);
            logger.debug("We don't answer for: {}", userName);
            return null;
        }

        logger.debug("In loadUserFromCanvas, currently processing user: {}", userName);
        JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
        String[] canvasUserInfo = getCanvasUserInfo(userName);
        if (canvasUserInfo == null) {
            cache.put(userName, nullToken);
            return null;
        }
        String email = canvasUserInfo[0];
        String displayName = canvasUserInfo[1];

        List<String> canvasRoles = getRolesFromCanvas(userName);
        if (canvasRoles == null) {
            cache.put(userName, nullToken);
            return null;
        }

        logger.debug("Canvas roles for {}: {}", userName, canvasRoles);

        Set<JaxbRole> roles = new HashSet<>();
        boolean isInstructor = false;
        for (String roleStr: canvasRoles) {
            roles.add(new JaxbRole(roleStr, jaxbOrganization, "Canvas external role", Role.Type.EXTERNAL));
            if (roleStr.endsWith(LTI_INSTRUCTOR_ROLE)) {
                isInstructor = true;
            }
        }
        roles.add(new JaxbRole(Group.ROLE_PREFIX + "CANVAS", jaxbOrganization, "Canvas User",
            Role.Type.EXTERNAL_GROUP));
        if (isInstructor) {
            roles.add(new JaxbRole(Group.ROLE_PREFIX + "CANVAS_INSTRUCTOR", jaxbOrganization, "Canvas Instructor",
                Role.Type.EXTERNAL_GROUP));
        }
        logger.debug("Returning JaxbRoles: {}", roles);

        User user = new JaxbUser(userName, null, displayName, email, PROVIDER_NAME, jaxbOrganization, roles);
        cache.put(userName, user);
        logger.debug("Returning user {}", userName);
        return user;
    }

    private String[] getCanvasUserInfo(String userName) {
        String urlString = String.format("%s/api/v1/users/sis_login_id:%s", url, userName);
        try {
            JsonNode node = getRequestJson(urlString);

            String email = node.path("email").asText();
            String displayName = node.path("name").asText();
            return new String[]{email, displayName};
        } catch (IOException e) {
            logger.warn("Exception getting Canvas user information for user {} at {}: {}", userName, urlString, e);
        }
        return null;
    }

    private List<String> getRolesFromCanvas(String userName) {
        logger.debug("getRolesFromCanvas({})", userName);
        // Only list 'active' enrollments. That means, only courses in active terms will be used.
        String urlString =
                 String.format("%s/api/v1/users/sis_login_id:%s/courses.json?per_page=500&enrollment_state=active&state[]=unpublished&state[]=available",
                    url, userName);
        try {
            List<String> roleList = new ArrayList<>();
            JsonNode nodes = getRequestJson(urlString);
            for (JsonNode node: nodes) {
                String courseId = node.path("id").asText();
                JsonNode enrollmentNodes = node.path("enrollments");
                for (JsonNode enrollmentNode: enrollmentNodes) {
                    String canvasRole = enrollmentNode.path("type").asText();
                    String ltiRole = instructorRoles.contains(canvasRole) ? LTI_INSTRUCTOR_ROLE : LTI_LEARNER_ROLE;
                    String opencastRole = String.format("%s_%s", courseId, ltiRole);
                    roleList.add(opencastRole);
                }
            }
            return roleList;

        } catch (IOException e) {
            logger.warn("Exception getting site/role membership for Canvas user {} at {}: {}", userName, urlString, e);
        }
        return null;
    }

    private JsonNode getRequestJson(String urlString) throws IOException {
        Content content = Request.Get(urlString).addHeader("Authorization", "Bearer " + token)
            .execute().returnContent();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content.asStream());
    }

    private boolean verifyCanvasUser(String userName) {
        logger.debug("verifyCanvasUser({})", userName);
        String urlString =
          String.format("%s/api/v1/users/sis_login_id:%s", url, userName);
        try {
            getRequestJson(urlString);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private Set<String> parsePropertyLineAsSet(String configLine) {
        Set<String> set = new HashSet<>();
        String[] configs = configLine.split(",");
        for (String config: configs) {
            set.add(config.trim());
        }
        return set;
    }

    /**
     * Get all sites id taught by current user.
     * Only list available site roles for current user.
     */
    private List<String> getCanvasSiteRolesByCurrentUser(String query, boolean exact) {
        User user = securityService.getUser();
        if (exact) {
            Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
            if (roles.contains(query)) {
                return Collections.singletonList(query);
            } else {
                return Collections.emptyList();
            }
        }

        final String finalQuery = StringUtils.chop(query);

        return user.getRoles().stream()
            .map(Role::getName)
            .filter(roleName -> roleName.endsWith("_" + LTI_INSTRUCTOR_ROLE) || roleName.endsWith("_" + LTI_LEARNER_ROLE))
            .map(roleName -> StringUtils.substringBeforeLast(roleName, "_"))
            .distinct()
            .map(site -> Arrays.asList(site + "_" + LTI_LEARNER_ROLE, site + "_" + LTI_INSTRUCTOR_ROLE))
            .flatMap(siteRoles -> siteRoles.stream())
            .filter(roleName -> roleName.startsWith(finalQuery))
            .collect(Collectors.toList());
    }

}
