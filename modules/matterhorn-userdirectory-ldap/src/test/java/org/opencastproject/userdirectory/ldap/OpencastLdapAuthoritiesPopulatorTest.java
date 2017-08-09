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
package org.opencastproject.userdirectory.ldap;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpencastLdapAuthoritiesPopulatorTest {

  private static final Logger logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulatorTest.class);

  private static final Set<String> DEFAULT_ATTRIBUTE_NAMES;
  private static final String DEFAULT_STR_ATTRIBUTE_NAMES;

  private static final String DEFAULT_PREFIX = "ROLE_";

  private static final String[] DEFAULT_EXTRA_ROLES;
  private static final String[] EMPTY_EXTRA_ROLES = new String[] {};

  private static final String USERNAME = "username";
  private static final String ORG_NAME = "my_organization_id";
  private static final int N_GROUP_ROLES = 3;
  private static final int N_LDAP_ATTRIBUTES = 3;
  private static final int N_EXTRA_ROLES = 2;

  static {
    HashSet<String> tempSet = new HashSet<String>();
    for (int i = 1; i <= N_LDAP_ATTRIBUTES; i++)
      tempSet.add(format("ldap_attribute_%d", i));
    DEFAULT_ATTRIBUTE_NAMES = Collections.unmodifiableSet(tempSet);
    DEFAULT_STR_ATTRIBUTE_NAMES = StringUtils.join(DEFAULT_ATTRIBUTE_NAMES, ", ");

    DEFAULT_EXTRA_ROLES = new String[N_EXTRA_ROLES];
    for (int i = 0; i < N_EXTRA_ROLES; i++)
      DEFAULT_EXTRA_ROLES[i] = format("extra_role_%d", i);
  }

  /** A map containing the set of LDAP arguments and their values (they keys can be multivalued) */
  private HashMap<String, String[]> mappings;

  private SecurityService securityService;
  private JpaGroupRoleProvider groupRoleProvider;
  private Organization org;
  private Group group;

  private static final String[] PREFIX_TESTS = new String[] { null, "", DEFAULT_PREFIX };
  private static final boolean[] UPPERCASE_TESTS = new boolean[] { true, false };
  private static final String[][] EXTRA_ROLES_TESTS = new String[][] { null, EMPTY_EXTRA_ROLES, DEFAULT_EXTRA_ROLES };
  private final JpaGroupRoleProvider[] groupRoleProviderTests = new JpaGroupRoleProvider[] { null, groupRoleProvider };

  @Before
  public void setUp() {

    mappings = new HashMap<String, String[]>();

    org = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(org.getId()).andReturn(ORG_NAME).anyTimes();

    Set<Role> groupRoles = new HashSet<Role>();
    for (int i = 1; i <= N_GROUP_ROLES; i++) {
      Role r = EasyMock.createNiceMock(Role.class);
      EasyMock.expect(r.getOrganization()).andReturn(org).anyTimes();
      EasyMock.expect(r.getName()).andReturn(format("group_role_%d", i)).anyTimes();
      EasyMock.replay(r);
      groupRoles.add(r);
    }
    group = EasyMock.createNiceMock(Group.class);
    EasyMock.expect(group.getRoles()).andReturn(groupRoles).anyTimes();

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();

    groupRoleProvider = EasyMock.createNiceMock(JpaGroupRoleProvider.class);
    EasyMock.expect(groupRoleProvider.getRolesForGroup(EasyMock.anyString())).andReturn(new ArrayList<Role>(groupRoles))
            .anyTimes();

    EasyMock.replay(org, group, securityService, groupRoleProvider);
  }

  @Test
  public void testNullAttributeNames() {
    try {
      new OpencastLdapAuthoritiesPopulator(null, DEFAULT_PREFIX, false, org, securityService, groupRoleProvider,
              DEFAULT_EXTRA_ROLES);
    } catch (NullPointerException e) {
      // OK
      return;
    }
    fail(format("A null \"attributeNames\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testEmptyAttributeNames() {
    try {
      new OpencastLdapAuthoritiesPopulator("", DEFAULT_PREFIX, false, org, securityService, groupRoleProvider,
              DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("An empty \"attributeNames\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNullOrganization() {
    try {
      new OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, false, null, securityService,
              groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (NullPointerException e) {
      // OK
      return;
    }
    fail(format("A null \"organization\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNullSecurityService() {
    try {
      new OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, false, org, null,
              groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (NullPointerException e) {
      // OK
      return;
    }
    fail(format("A null \"securityService\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testAttributeNotFound() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    // The attribute returns "null", i.e. does not exist in the LDAP user
    mappings.put("myAttribute", null);
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testEmptyAttributeArray() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    // The attribute returns an empty array
    mappings.put("myAttribute", new String[] {});
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testEmptySingleAttribute() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("myAttribute", new String[] { "" });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testMultivaluedAttributeSimpleRoles() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1 ", " value2 ", " value3 ", " value4 " });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testSingleAttributeMultipleRoles() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1, value2, value3 , value4 " });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testAttributesWithWhitespaces() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute" });
    mappings.put("attribute2",
            new String[] { "value_2_1 , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                    "normal_value , normalvalue2" });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testRolePrefix() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute" });
    mappings.put("attribute2",
            new String[] { "value_2_1 , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                    "normal_value , normalvalue2" });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Define prefixes
    String[] prefixes = new String[] { "normal_", "many_underscores____", "cApItAlS_", " WH\n\niTE\tspA cE ",
            " \n \t\t\n \r" };

    // Test several argument combinations
    for (String prefix : prefixes) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, org, securityService,
                    groupRoleProvider, extraRoles);
            doTest(populator, mappings, groupRoleProvider);
          }
        }
      }
    }
  }

  @Test
  public void testWrongOrganization() {
    OpencastLdapAuthoritiesPopulator populator;

    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1 ", " value2 ", " value3 ", " value4 " });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Prepare the alternative organization
    Organization otherOrg = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(otherOrg.getId()).andReturn("other_organization").anyTimes();
    EasyMock.replay(otherOrg);

    // Test several argument combinations
    for (String prefix : PREFIX_TESTS) {
      for (boolean upper : UPPERCASE_TESTS) {
        for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
          for (String[] extraRoles : EXTRA_ROLES_TESTS) {
            try {
              populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, upper, otherOrg, securityService,
                      groupRoleProvider, extraRoles);
              doTest(populator, mappings, groupRoleProvider);
              fail(format(
                      "Request came from a different organization (\"%s\") as the expected (\"%s\") but no exception was thrown",
                      otherOrg, this.org));
            } catch (SecurityException e) {
              // OK
            }
          }
        }
      }
    }
  }

  /**
   * Perform the test of an instance of the class OpencastLdapAuthoritiesPopulator.
   *
   * @param populator
   *          an instance of {@link OpencastLdapAuthoritiesPopulator} to test
   * @param mappings
   *          a {@link Map} containing the LDAP attribute name - value pairs, where the name is a {@link String} and the
   *          value an array of {@code String}s, possibly {@code null} or empty.
   */
  private void doTest(OpencastLdapAuthoritiesPopulator populator, Map<String, String[]> mappings,
          JpaGroupRoleProvider groupRoleProvider) {
    DirContextOperations dirContextMock = EasyMock.createNiceMock(DirContextOperations.class);

    // Populate the DirContextOperations class
    for (String attrName : mappings.keySet()) {
      EasyMock.expect(dirContextMock.getStringAttributes(attrName)).andReturn(mappings.get(attrName)).anyTimes();
    }
    EasyMock.replay(dirContextMock);

    // Prepare the expected result
    HashSet<GrantedAuthority> expectedResult = new HashSet<GrantedAuthority>();
    for (String attrName : populator.getAttributeNames()) {
      if (mappings.get(attrName) != null) {
        for (String attrValues : mappings.get(attrName)) {
          addRoles(expectedResult, populator.getRolePrefix(), populator.getConvertToUpperCase(), groupRoleProvider,
                  attrValues.split(","));
        }
      }
    }

    // Add the additional authorities
    addRoles(expectedResult, populator.getRolePrefix(), populator.getConvertToUpperCase(), groupRoleProvider,
            populator.getAdditionalAuthorities());

    // Check the response is correct
    checkResponse(populator.getGrantedAuthorities(dirContextMock, USERNAME), expectedResult);
  }

  private void addRoles(Set<GrantedAuthority> roles, String prefix, boolean toUpper, JpaGroupRoleProvider groupProvider,
          String... strRoles) {

    if (strRoles != null) {
      for (String strRole : strRoles) {
        String role = strRole.trim();
        if (!role.isEmpty()) {
          if (prefix == null) {
            prefix = "";
          }
          if (toUpper) {
            role = (prefix.trim() + role).replaceAll("[\\s_]+", "_").toUpperCase();
          } else {
            role = (prefix.trim() + role).replaceAll("[\\s_]+", "_");
          }
          logger.debug("Creating expected attribute '{}'", role);
          roles.add(new SimpleGrantedAuthority(role));

          if (groupProvider != null) {
            for (Role groupRole : groupRoleProvider.getRolesForGroup(role)) {
              roles.add(new SimpleGrantedAuthority(groupRole.getName()));
            }
          }
        }
      }
    }
  }

  /**
   * Compare the result returned by the method {@link OpencastLdapAuthoritiesPopulator#getGrantedAuthorities} and
   * {@link Collection} of {@link GrantedAuthority}'s containing the expected results.
   *
   * @param actual
   *          the actual output of the method; it should match the contents in {@code expected}
   * @param expected
   *          the expected output
   */
  private void checkResponse(Collection<? extends GrantedAuthority> actual,
          Collection<? extends GrantedAuthority> expected) {

    for (GrantedAuthority auth : actual) {
      assertTrue(format("Authorities populator returned unexpected authority: %s", auth), expected.contains(auth));
    }

    for (GrantedAuthority auth : expected) {
      assertTrue(format("Authorities populator did not return expected authority: %s", auth), actual.contains(auth));
    }
  }
}
