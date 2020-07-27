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
package org.opencastproject.security.aai;

import org.opencastproject.security.aai.api.AttributeMapper;
import org.opencastproject.security.api.GroupProvider;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUserReference;
import org.opencastproject.security.shibboleth.ShibbolethLoginHandler;
import org.opencastproject.userdirectory.api.AAIRoleProvider;
import org.opencastproject.userdirectory.api.UserReferenceProvider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Dynamic login with Shibboleth data through SpEL mappings
 */
public class DynamicLoginHandler implements ShibbolethLoginHandler, AAIRoleProvider, GroupProvider, InitializingBean {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DynamicLoginHandler.class);

  /** The user reference provider */
  private UserReferenceProvider userReferenceProvider = null;

  /** The security service */
  private SecurityService securityService = null;

  /** The security service */
  private AttributeMapper attributeMapper = null;

  public DynamicLoginHandler() {
  }

  /**
   * Handle a new user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  @Override
  public void newUserLogin(String id, HttpServletRequest request) {
    String name = extractName(request);
    String email = extractEmail(request);
    Date loginDate = new Date();
    JpaOrganization organization = fromOrganization(securityService.getOrganization());

    // Compile the list of roles
    Set<JpaRole> roles = extractRoles(id, request);

    // Create the user reference
    JpaUserReference userReference = new JpaUserReference(id, name, email, MECH_SHIBBOLETH, loginDate, organization,
            roles);

    logger.debug("Shibboleth user '{}' logged in for the first time", id);
    userReferenceProvider.addUserReference(userReference, MECH_SHIBBOLETH);
  }

  /**
   * Handle an existing user login.
   *
   * @param id
   *          The identity of the user, ideally the Shibboleth persistent unique identifier
   * @param request
   *          The request, for accessing any other Shibboleth variables
   */
  @Override
  public void existingUserLogin(String id, HttpServletRequest request) {
    Organization organization = securityService.getOrganization();

    // Load the user reference
    JpaUserReference userReference = userReferenceProvider.findUserReference(id, organization.getId());
    if (userReference == null) {
      //Triggers creation of user reference
      //Possible problem: if there is user (not reference) with that id, we will get conflicts
      throw new UsernameNotFoundException("User reference '" + id + "' was not found");
    }

    // Update the reference
    userReference.setName(extractName(request));
    userReference.setEmail(extractEmail(request));
    userReference.setLastLogin(new Date());
    Set<JpaRole> roles = extractRoles(id, request);
    userReference.setRoles(roles);

    logger.debug("Shibboleth user '{}' logged in", id);
    userReferenceProvider.updateUserReference(userReference);
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the user reference provider.
   *
   * @param userReferenceProvider
   *          the user reference provider
   */
  public void setUserReferenceProvider(UserReferenceProvider userReferenceProvider) {
    this.userReferenceProvider = userReferenceProvider;
  }

  /**
   * Extracts the name from the request.
   *
   * @param request
   *          the request
   * @return the name
   */
  private String extractName(HttpServletRequest request) {
    String displayName = extractDisplayName(request);
    if (StringUtils.isNotBlank(displayName)) {
      return displayName;
    }
    return null;
  }

  /**
   * Extracts the e-mail from the request.
   *
   * @param request
   *          the request
   * @return the e-mail address
   */
  private String extractEmail(HttpServletRequest request) {
    List<String> mailAdresses = attributeMapper.getMappedAttributes(request, "mail");

    if (mailAdresses.size() == 0) {
      return null;
    }

    String mailValue = mailAdresses.get(0);
    String mail = StringUtils.isBlank(mailValue) ? ""
            : new String(mailValue.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    return mail;
  }

  /**
   * Extracts the e-mail from the request.
   *
   * @param request
   *          the request
   * @return the e-mail address
   */
  private String extractDisplayName(HttpServletRequest request) {
    List<String> displayNames = attributeMapper.getMappedAttributes(request, "displayName");

    if (displayNames.size() == 0) {
      return null;
    }

    String displayNameValue = displayNames.get(0);
    String displayName = StringUtils.isBlank(displayNameValue) ? ""
              : new String(displayNameValue.getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);
    return displayName;
  }

  /**
   * Extracts the roles from the request.
   *
   * @param request
   *          the request
   * @return the roles
   */
  private Set<JpaRole> extractRoles(String id, HttpServletRequest request) {
    List<String> aaiRoles = attributeMapper.getMappedAttributes(request, "roles");
    JpaOrganization organization = fromOrganization(securityService.getOrganization());
    Set<JpaRole> roles = new HashSet<JpaRole>();
    if (aaiRoles != null) {
      for (String aaiRole : aaiRoles) {
        roles.add(new JpaRole(aaiRole, organization));
      }
    }

    return roles;
  }

  /**
   * Creates a JpaOrganization from an organization
   *
   * @param org
   *          the organization
   */
  private JpaOrganization fromOrganization(Organization org) {
    if (org instanceof JpaOrganization) {
      return (JpaOrganization) org;
    }

    return new JpaOrganization(org.getId(), org.getName(), org.getServers(), org.getAdminRole(),
        org.getAnonymousRole(), org.getProperties());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.api.AAIRoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    JaxbOrganization organization = JaxbOrganization.fromOrganization(securityService.getOrganization());
    HashSet<Role> roles = new HashSet<Role>();
    roles.add(new JaxbRole(organization.getAnonymousRole(), organization));
    roles.addAll(securityService.getUser().getRoles());
    return roles.iterator();
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
      ArrayList<Role> roles = new ArrayList<Role>();
      User user = userReferenceProvider.loadUser(userName);
      if (user != null)
        roles.addAll(user.getRoles());
      return roles;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query))
        foundRoles.add(role);
    }
    return offsetLimitCollection(offset, limit, foundRoles).iterator();

  }

  private <T> HashSet<T> offsetLimitCollection(int offset, int limit, HashSet<T> entries) {
    HashSet<T> result = new HashSet<T>();
    int i = 0;
    for (T entry : entries) {
      if (limit != 0 && result.size() >= limit)
        break;
      if (i >= offset)
        result.add(entry);
      i++;
    }
    return result;
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    this.userReferenceProvider.setRoleProvider(this);
  }

  public AttributeMapper getAttributeMapper() {
    return attributeMapper;
  }

  public void setAttributeMapper(AttributeMapper attributeMapper) {
    this.attributeMapper = attributeMapper;
  }

  @Override
  public List<Role> getRolesForGroup(String groupName) {
    return null;
  }

}
