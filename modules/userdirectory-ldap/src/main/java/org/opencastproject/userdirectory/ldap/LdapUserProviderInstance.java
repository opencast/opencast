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

package org.opencastproject.userdirectory.ldap;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A UserProvider that reads user roles from LDAP entries.
 */
@Component(
    immediate = true,
    configurationPid = "org.opencastproject.userdirectory.ldap",
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { UserProvider.class },
    property = {
        "service.description=Provides ldap user directory instances"
    }
)
public class LdapUserProviderInstance implements UserProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LdapUserProviderInstance.class);

  public static final String PROVIDER_NAME = "ldap";

  /** The key to look up the ldap search filter in the service configuration properties */
  private static final String SEARCH_FILTER_KEY = "org.opencastproject.userdirectory.ldap.searchfilter";

  /** The key to look up the ldap search base in the service configuration properties */
  private static final String SEARCH_BASE_KEY = "org.opencastproject.userdirectory.ldap.searchbase";

  /** The key to look up the ldap server URL in the service configuration properties */
  private static final String LDAP_URL_KEY = "org.opencastproject.userdirectory.ldap.url";

  /** The key to look up the role attributes in the service configuration properties */
  private static final String ROLE_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.roleattributes";

  /** The key to look up the users name attributes **/
  private static final String USER_NAME_ATTRIBUTES_KEY = "org.opencastproject.userdirectory.ldap.userattributes.name";

  /** The key to look up the users attribute to set its mail address**/
  private static final String USER_MAIL_ATTRIBUTE_KEY = "org.opencastproject.userdirectory.ldap.userattributes.mail";

  /** The key to look up the organization identifier in the service configuration properties */
  private static final String ORGANIZATION_KEY = "org.opencastproject.userdirectory.ldap.org";

  /** The key to look up the user DN to use for performing searches. */
  private static final String SEARCH_USER_DN = "org.opencastproject.userdirectory.ldap.userDn";

  /** The key to look up the password to use for performing searches */
  private static final String SEARCH_PASSWORD = "org.opencastproject.userdirectory.ldap.password";

  /** The key to look up the number of user records to cache */
  private static final String CACHE_SIZE = "org.opencastproject.userdirectory.ldap.cache.size";

  /** The key to look up the number of minutes to cache users */
  private static final String CACHE_EXPIRATION = "org.opencastproject.userdirectory.ldap.cache.expiration";

  /** The key to indicate a prefix that will be added to every role read from the LDAP */
  private static final String ROLE_PREFIX_KEY = "org.opencastproject.userdirectory.ldap.roleprefix";

  /**
   * The key to indicate a comma-separated list of prefixes.
   * The "role prefix" defined with the ROLE_PREFIX_KEY will not be prepended to the roles starting with any of these
   */
  private static final String EXCLUDE_PREFIXES_KEY = "org.opencastproject.userdirectory.ldap.exclude.prefixes";

  /**
   * The key to indicate a prefix,
   * which is used to check whether a roleattribute value shall be added as a group to the user
   */
  private static final String GROUP_CHECK_PREFIX_KEY = "org.opencastproject.userdirectory.ldap.groupcheckprefix";

  /** Specifies, whether the roleattributes should be added as a role */
  private static final String APPLY_ROLEATTRIBUTES_AS_ROLES_KEY
      = "org.opencastproject.userdirectory.ldap.roleattributes.applyasroles";

  /** Specifies, whether the roleattributes should be added as a group */
  private static final String APPLY_ROLEATTRIBUTES_AS_GROUPS_KEY
      = "org.opencastproject.userdirectory.ldap.roleattributes.applyasgroups";

  /** The prefix of the keys, which map a ldap attribute to opencast roles */
  private static final String ATTRIBUTE_MAPPING_KEY_PREFIX = "org.opencastproject.userdirectory.ldap.map.";

  /** The postfix of the attribute maps, which specifiy the value to map */
  private static final String ATTRIBUTE_MAPPING_KEY_POSTFIX_VALUE = "value";

  /** The postfix of the attribute maps, which map a ldap attribute to opencast roles */
  private static final String ATTRIBUTE_MAPPING_KEY_POSTFIX_ROLES = "roles";

  /** The postfix of the attribute maps, which map a ldap attribute to opencast groups */
  private static final String ATTRIBUTE_MAPPING_KEY_POSTFIX_GROUPS = "groups";

  /** The key to indicate whether or not the roles should be converted to uppercase */
  private static final String UPPERCASE_KEY = "org.opencastproject.userdirectory.ldap.uppercase";

  /** The key to indicate a unique identifier for each LDAP connection */
  private static final String INSTANCE_ID_KEY = "org.opencastproject.userdirectory.ldap.id";

  /** The key to indicate a comma-separated list of extra roles to add to the authenticated user */
  private static final String EXTRA_ROLES_KEY = "org.opencastproject.userdirectory.ldap.extra.roles";

  /** The key to setup an LDAP connection ID as an OSGI service property */
  private static final String INSTANCE_ID_SERVICE_PROPERTY_KEY = "instanceId";

  /** The key to setup the organization ID as an OSGI service property */
  private static final String ORGANIZATION_ID_SERVICE_PROPERTY_KEY = "orgId";

  /** The spring ldap userdetails service delegate */
  private LdapUserDetailsService delegate;

  /** The organization id */
  private Organization organization = null;

  /** Total number of requests made to load users */
  private AtomicLong requests = null;

  /** The number of requests made to ldap */
  private AtomicLong ldapLoads = null;

  /** A cache of users, which lightens the load on the LDAP server */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectory;

  /** The group role provider service */
  private JpaGroupRoleProvider groupRoleProvider;

  /** Opencast's security service */
  private SecurityService securityService;

  /** The registration of the authorities populator registered alongside this user provider */
  private ServiceRegistration authoritiesPopulatorRegistration;

  /** OSGi callback for setting the organization directory service. */
  @Reference
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /** OSGi callback for setting the role group service. */
  @Reference
  public void setGroupRoleProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  /** OSGi callback for setting the security service. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Retrieve configuration values and check for a proper value.
   *
   * @param properties
   *      Configuration map
   * @param key
   *      Configuration key to check for
   * @return
   *      The configuration value
   * @throws ConfigurationException
   *      Thrown if the configuration value is blank
   */
  private String getRequiredProperty(final Map<String, Object> properties, final String key)
          throws ConfigurationException {
    final String value = (String) properties.get(key);
    if (StringUtils.isBlank(value)) {
      throw new ConfigurationException(key, "missing configuration value");
    }
    return value;
  }

  // CHECKSTYLE:OFF
  @Activate
  @Modified
  public void updated(BundleContext bundleContext, Map<String, Object> properties) throws ConfigurationException {
    // CHECKSTYLE:ON

    // required settings
    String searchBase = getRequiredProperty(properties, SEARCH_BASE_KEY);
    String searchFilter = getRequiredProperty(properties, SEARCH_FILTER_KEY);
    String url = getRequiredProperty(properties, LDAP_URL_KEY);
    String instanceId = getRequiredProperty(properties, INSTANCE_ID_KEY);
    String roleAttributesGlob = getRequiredProperty(properties, ROLE_ATTRIBUTES_KEY);

    // optional settings
    String organizationId = (String) properties.get(ORGANIZATION_KEY);
    String userDn = (String) properties.get(SEARCH_USER_DN);
    String password = (String) properties.get(SEARCH_PASSWORD);
    String[] userAttributeName = StringUtils.split((String) properties.get(USER_NAME_ATTRIBUTES_KEY), ',');
    String userAttributeMail = (String) properties.get(USER_MAIL_ATTRIBUTE_KEY);

    // optional with default values
    String rolePrefix = Objects.toString(properties.get(ROLE_PREFIX_KEY), "ROLE_");
    String[] excludePrefixes = StringUtils.split((String) properties.get(EXCLUDE_PREFIXES_KEY), ",");
    String groupCheckPrefix = Objects.toString(properties.get(GROUP_CHECK_PREFIX_KEY), "ROLE_GROUP_");
    boolean convertToUppercase = BooleanUtils.toBoolean(Objects.toString(properties.get(UPPERCASE_KEY), "true"));
    int cacheSize = NumberUtils.toInt((String) properties.get(CACHE_SIZE), 1000);
    int cacheExpiration = NumberUtils.toInt((String) properties.get(CACHE_EXPIRATION), 5);
    boolean applyRoleattributesAsRoles = BooleanUtils.toBoolean(Objects.toString(
            properties.get(APPLY_ROLEATTRIBUTES_AS_ROLES_KEY), "true"));
    boolean applyRoleattributesAsGroups = BooleanUtils.toBoolean(Objects.toString(
            properties.get(APPLY_ROLEATTRIBUTES_AS_GROUPS_KEY), "true"));
    if (applyRoleattributesAsGroups && !applyRoleattributesAsRoles) {
      throw new ConfigurationException(APPLY_ROLEATTRIBUTES_AS_GROUPS_KEY,
              "'" + APPLY_ROLEATTRIBUTES_AS_ROLES_KEY + "' needs to be 'true' to enable this option");
    }

    // extra roles
    String[] extraRoles = StringUtils.split(Objects.toString(properties.get(EXTRA_ROLES_KEY), ""), ",");
    Set<String> extraRoleSet = new HashSet<>(Arrays.asList(extraRoles));
    extraRoleSet.addAll(Arrays.asList("ROLE_ANONYMOUS", "ROLE_USER"));
    extraRoles = extraRoleSet.toArray(new String[extraRoles.length]);

    // maps
    HashMap<String, HashMap<String, String>> ldapAssignmentMappingsPreparation = new HashMap();
    for (String key : properties.keySet()) {
      if (key.startsWith(ATTRIBUTE_MAPPING_KEY_PREFIX)) {
        final String[] postfix = key.substring(ATTRIBUTE_MAPPING_KEY_PREFIX.length()).split("\\.");

        if (postfix.length != 2) {
          throw new ConfigurationException(key,
                  "Invalid Configkey format, the following format is needed: "
                  + ATTRIBUTE_MAPPING_KEY_PREFIX + "<identifier>.<key>");
        }

        final String mappingIdentifier = postfix[0];
        final String mappingKey = postfix[1];

        HashMap keyValueMap = ldapAssignmentMappingsPreparation.getOrDefault(mappingIdentifier, new HashMap());

        keyValueMap.put(mappingKey, (String) properties.get(key));

        ldapAssignmentMappingsPreparation.put(mappingIdentifier, keyValueMap);
      }
    }
    HashMap<String, String[]> ldapAssignmentRoleMap = new HashMap();
    HashMap<String, String[]> ldapAssignmentGroupMap = new HashMap();
    for (HashMap.Entry<String, HashMap<String, String>> entry : ldapAssignmentMappingsPreparation.entrySet()) {
      HashMap<String, String> mappingConf = entry.getValue();
      String value = StringUtils.trimToNull(mappingConf.get(ATTRIBUTE_MAPPING_KEY_POSTFIX_VALUE));
      String roles = StringUtils.trimToNull(mappingConf.get(ATTRIBUTE_MAPPING_KEY_POSTFIX_ROLES));
      String groups = StringUtils.trimToNull(mappingConf.get(ATTRIBUTE_MAPPING_KEY_POSTFIX_GROUPS));

      if (value == null) {
        throw new ConfigurationException(ATTRIBUTE_MAPPING_KEY_PREFIX + entry.getKey() + ".*",
                "LDAP mapping incomplete, the key 'value' is needed");
      }
      if (roles == null && groups == null) {
        throw new ConfigurationException(ATTRIBUTE_MAPPING_KEY_PREFIX + entry.getKey() + ".*",
                "LDAP mapping incomplete, one of the keys 'roles' or 'groups' is needed");
      }

      if (convertToUppercase) {
        value = value.toUpperCase();
      }

      if (roles != null) {
        if (convertToUppercase) {
          roles = roles.toUpperCase();
        }
        ldapAssignmentRoleMap.put(value,
                ArrayUtils.addAll(
                        ldapAssignmentRoleMap.getOrDefault(value, new String[0]),
                        Arrays.stream(roles.split(","))
                                .map(r -> StringUtils.trimToNull(r))
                                .filter(r -> r != null)
                                .toArray(String[]::new)
                )
        );
      }

      if (groups != null) {
        if (convertToUppercase) {
          groups = groups.toUpperCase();
        }
        ldapAssignmentGroupMap.put(value,
                ArrayUtils.addAll(
                        ldapAssignmentGroupMap.getOrDefault(value, new String[0]),
                        Arrays.stream(groups.split(","))
                                .map(r -> StringUtils.trimToNull(r))
                                .filter(r -> r != null)
                                .toArray(String[]::new)
                )
        );
      }
    }

    // Defaults to first available organization
    Organization org;
    try {
      if (StringUtils.isNoneBlank(organizationId)) {
        org = orgDirectory.getOrganization(organizationId);
      } else {
        if (orgDirectory.getOrganizations().size() != 1) {
          throw new NotFoundException("Multiple organizations exist but none is specified");
        }
        org = orgDirectory.getOrganizations().get(0);
        organizationId = org.getId();
      }
    } catch (NotFoundException e) {
      throw new ConfigurationException(ORGANIZATION_KEY, "no organization with configured id", e);
    }

    // Remove the previous authorities populator registration, if any, before registering the new one
    if (authoritiesPopulatorRegistration != null) {
      authoritiesPopulatorRegistration.unregister();
      authoritiesPopulatorRegistration = null;
    }

    // Dictionary to include a property to identify this LDAP instance in the security.xml file
    Hashtable<String, String> dict = new Hashtable<>();
    dict.put(INSTANCE_ID_SERVICE_PROPERTY_KEY, instanceId);
    dict.put(ORGANIZATION_ID_SERVICE_PROPERTY_KEY, organizationId);

    OpencastLdapAuthoritiesPopulator authoritiesPopulator = new OpencastLdapAuthoritiesPopulator(roleAttributesGlob,
            rolePrefix, excludePrefixes, groupCheckPrefix, applyRoleattributesAsRoles, applyRoleattributesAsGroups,
            ldapAssignmentRoleMap, ldapAssignmentGroupMap, convertToUppercase, org, securityService,
            groupRoleProvider, extraRoles);

    // Also, register this instance as LdapAuthoritiesPopulator so that it can be used within the security.xml file
    authoritiesPopulatorRegistration =
        bundleContext.registerService(LdapAuthoritiesPopulator.class.getName(), authoritiesPopulator, dict);

    OpencastUserDetailsContextMapper mapper = null;
    if (userAttributeName != null && userAttributeMail != null) {
      mapper = new OpencastUserDetailsContextMapper(userAttributeName, userAttributeMail);
    }

    this.organization = org;
    logger.debug("Creating LdapUserProvider instance with organization=" + organizationId
            + ", to LDAP server at url:  " + url);

    DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
    if (StringUtils.isNotBlank(userDn)) {
      contextSource.setPassword(password);
      contextSource.setUserDn(userDn);
      // Required so that authentication will actually be used
      contextSource.setAnonymousReadOnly(false);
    } else {
      // No password set so try to connect anonymously.
      contextSource.setAnonymousReadOnly(true);
    }

    try {
      contextSource.afterPropertiesSet();
    } catch (Exception e) {
      throw new org.opencastproject.util.ConfigurationException("Unable to create a spring context source", e);
    }
    FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource);
    userSearch.setReturningAttributes(roleAttributesGlob.split(","));

    delegate = new LdapUserDetailsService(userSearch, authoritiesPopulator);

    if (mapper != null) {
      userSearch.setReturningAttributes(
          Stream.of(roleAttributesGlob.split(","), mapper.getAttributes())
              .flatMap(Stream::of)
              .collect(Collectors.toList()).toArray(new String[] { })
      );
      delegate.setUserDetailsMapper(mapper);
    }

    // Setup the caches
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String id) throws Exception {
                User user = loadUserFromLdap(id);
                return user == null ? nullToken : user;
              }
            });
  }

  @Deactivate
  public void deactivate() {
    if (authoritiesPopulatorRegistration != null) {
      authoritiesPopulatorRegistration.unregister();
      authoritiesPopulatorRegistration = null;
    }
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization.getId();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    logger.debug("LdapUserProvider is loading user " + userName);
    requests.incrementAndGet();
    try {
      // use #getUnchecked since the loader does not throw any checked exceptions
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        return null;
      } else {
        return (JaxbUser) user;
      }
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while loading user " + userName, e);
      return null;
    }
  }

  /**
   * Loads a user from LDAP.
   *
   * @param userName
   *          the username
   * @return the user
   */
  protected User loadUserFromLdap(String userName) {
    if (delegate == null || cache == null) {
      throw new IllegalStateException("The LDAP user detail service has not yet been configured");
    }
    ldapLoads.incrementAndGet();
    UserDetails userDetails = null;

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(LdapUserProviderInstance.class.getClassLoader());
      try {
        userDetails = delegate.loadUserByUsername(userName);
      } catch (UsernameNotFoundException e) {
        cache.put(userName, nullToken);
        return null;
      }
      JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

      Set<JaxbRole> roles = userDetails.getAuthorities()
          .stream()
          .map(a -> new JaxbRole(a.getAuthority(), jaxbOrganization))
          .collect(Collectors.toUnmodifiableSet());

      User user;
      if (userDetails instanceof OpencastUserDetails) {
        user = new JaxbUser(userDetails.getUsername(),null,
            ((OpencastUserDetails) userDetails).getName(),
            ((OpencastUserDetails) userDetails).getMail(), PROVIDER_NAME, jaxbOrganization, roles);
      } else {
        user = new JaxbUser(userDetails.getUsername(), PROVIDER_NAME, jaxbOrganization, roles);
      }

      cache.put(userName, user);
      return user;
    } finally {
      currentThread.setContextClassLoader(originalClassloader);
    }
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }
    // TODO implement a LDAP wildcard search
    // FIXME We return the current user, rather than an empty list, to make sure the current user's role is displayed in
    // the admin UI (MH-12526).
    User currentUser = securityService.getUser();
    if (loadUser(currentUser.getUsername()) != null) {
      List<User> retVal = new ArrayList<>();
      retVal.add(securityService.getUser());
      return retVal.iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public Iterator<User> getUsers() {
    // TODO implement LDAP get all users
    // FIXME We return the current user, rather than an empty list,
    // to make sure the current user's role is displayed in
    // the admin UI (MH-12526).
    User currentUser = securityService.getUser();
    if (loadUser(currentUser.getUsername()) != null) {
      List<User> retVal = new ArrayList<>();
      retVal.add(securityService.getUser());
      return retVal.iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public long countUsers() {
    // TODO implement LDAP count users
    // FIXME Because of MH-12526, we return conditionally 1 when the previous methods return the current user
    if (loadUser(securityService.getUser().getUsername()) != null) {
      return 1;
    }
    return 0;
  }

  @Override
  public void invalidate(String userName) {
    cache.invalidate(userName);
  }
}
