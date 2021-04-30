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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpencastLdapAuthoritiesPopulatorTest {

  private static final Logger logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulatorTest.class);

  private static final Set<String> DEFAULT_ATTRIBUTE_NAMES;
  private static final Set<Role> DEFAULT_INTERNAL_ROLES;
  private static final String DEFAULT_STR_ATTRIBUTE_NAMES;

  private static final String DEFAULT_PREFIX = "ROLE_";

  private static final String[] DEFAULT_EXTRA_ROLES;

  private static final String[] DEFAULT_EXCLUDE_PREFIXES = new String[] { "other", "ldap" };

  private static final String DEFAULT_GROUP_CHECK_PREFIX = "";

  private static final HashMap<String, String[]> DEFAULT_ASSIGNMENT_ROLE_MAP = new HashMap();
  private static final HashMap<String, String[]> DEFAULT_ASSIGNMENT_GROUP_MAP = new HashMap();

  private static final String USERNAME = "username";
  private static final String ORG_NAME = "my_organization_id";
  private static final String MAPPED_ROLE_ATTR = "THIS_ROLE_WILL_BE_MAPPED";
  private static final String GROUP_ROLE = "THIS_IS_THE_GROUP_ROLE";
  private static final String GROUP_ROLE_PREFIX = "THIS_";
  private static final int N_GROUP_ROLES = 3;
  private static final int N_LDAP_ATTRIBUTES = 3;
  private static final int N_EXTRA_ROLES = 2;
  private static final int N_INTERNAL_ROLES = 3;

  static {
    HashSet<String> tempSet = new HashSet<>();
    for (int i = 1; i <= N_LDAP_ATTRIBUTES; i++)
      tempSet.add(format("ldap_attribute_%d", i));
    DEFAULT_ATTRIBUTE_NAMES = Collections.unmodifiableSet(tempSet);
    DEFAULT_STR_ATTRIBUTE_NAMES = StringUtils.join(DEFAULT_ATTRIBUTE_NAMES, ", ");

    HashSet<Role> roleSet = new HashSet<>();
    for (int i = 1; i <= N_INTERNAL_ROLES; i++) {
      Role r = EasyMock.createNiceMock(Role.class);
      EasyMock.expect(r.getName()).andReturn("internal_role_" + (i + 1)).anyTimes();
      EasyMock.replay(r);
      roleSet.add(r);
    }
    DEFAULT_INTERNAL_ROLES = Collections.unmodifiableSet(roleSet);

    DEFAULT_EXTRA_ROLES = new String[N_EXTRA_ROLES];
    for (int i = 0; i < N_EXTRA_ROLES; i++)
      DEFAULT_EXTRA_ROLES[i] = format("extra_role_%d", i);

    {
      ArrayList<Map<String, String[]>> tempGroupMapTests = new ArrayList();
      tempGroupMapTests.add(null);
      tempGroupMapTests.add(Collections.unmodifiableMap(DEFAULT_ASSIGNMENT_GROUP_MAP));

      HashMap<String, String[]> tempGroupMap = new HashMap();
      tempGroupMap.put("NOT_MAPPED_GROUP", new String[] { "NOT_MAPPED_GROUP" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put("NOT_MAPPED_GROUP", new String[] { GROUP_ROLE });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, null);
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, new String[] {});
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, new String[] { "MAPPED_GROUP1" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, new String[] { GROUP_ROLE });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put("VALUE1", new String[] { GROUP_ROLE });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put("VALUE2", new String[] { GROUP_ROLE });
      tempGroupMap.put("VALUE1", new String[] { "MAPPED_GROUP2" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put("NOT_MAPPED_GROUP", new String[] { "NOT_MAPPED_GROUP" });
      tempGroupMap.put(GROUP_ROLE, new String[] { "MAPPED_GROUP1" });
      tempGroupMap.put("VALUE1", new String[] { "MAPPED_GROUP2" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, new String[] { "MAPPED_GROUP1", "MAPPED_GROUP2" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      tempGroupMap = new HashMap();
      tempGroupMap.put(GROUP_ROLE, new String[] { "MAPPED_GROUP1", "MAPPED_GROUP2" });
      tempGroupMap.put("VALUE1", new String[] { "MAPPED_GROUP3" });
      tempGroupMapTests.add(Collections.unmodifiableMap(tempGroupMap));

      ASSIGNMENT_GROUP_MAP_TESTS = Collections.unmodifiableList(tempGroupMapTests);
    }

    {
      ArrayList<Map<String, String[]>> tempRoleMapTests = new ArrayList();
      tempRoleMapTests.add(null);
      tempRoleMapTests.add(Collections.unmodifiableMap(DEFAULT_ASSIGNMENT_ROLE_MAP));

      HashMap<String, String[]> tempRoleMap = new HashMap();
      tempRoleMap.put("NOT_MAPPED_ROLE", new String[] { "NOT_MAPPED_ROLE" });
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put(MAPPED_ROLE_ATTR, null);
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put(MAPPED_ROLE_ATTR, new String[] {});
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put(MAPPED_ROLE_ATTR, new String[] { "MAPPED_ROLE1" });
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put("NOT_MAPPED_ROLE", new String[] { "NOT_MAPPED_ROLE" });
      tempRoleMap.put(MAPPED_ROLE_ATTR, new String[] { "MAPPED_ROLE1" });
      tempRoleMap.put("VALUE1", new String[] { "MAPPED_ROLE2" });
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put(MAPPED_ROLE_ATTR, new String[] { "MAPPED_ROLE1", "MAPPED_ROLE2" });
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      tempRoleMap = new HashMap();
      tempRoleMap.put(MAPPED_ROLE_ATTR, new String[] { "MAPPED_ROLE1", "MAPPED_ROLE2" });
      tempRoleMap.put("VALUE1", new String[] { "MAPPED_ROLE3" });
      tempRoleMapTests.add(Collections.unmodifiableMap(tempRoleMap));

      ASSIGNMENT_ROLE_MAP_TESTS = Collections.unmodifiableList(tempRoleMapTests);
    }
  }

  /** A map containing the set of LDAP arguments and their values (they keys can be multivalued) */
  private HashMap<String, String[]> mappings;

  private SecurityService securityService;
  private JpaGroupRoleProvider groupRoleProvider;
  private Organization org;

  private static final String[] PREFIX_TESTS = new String[] { null, "", DEFAULT_PREFIX, "PREFIX_WHICH_DOES_NOT_EXIST", "extra_role_" };
  private static final String[] GROUP_CHECK_PREFIX_TESTS = new String[] { DEFAULT_GROUP_CHECK_PREFIX, "val", GROUP_ROLE_PREFIX };
  private static final boolean[] APPLY_ATTRIBUTES_AS_ROLES_TESTS = { true, false };
  private static final boolean[] APPLY_ATTRIBUTES_AS_GROUPS_TESTS = { true, false };
  private static final List<Map<String, String[]>> ASSIGNMENT_ROLE_MAP_TESTS;
  private static final List<Map<String, String[]>> ASSIGNMENT_GROUP_MAP_TESTS;
  private static final boolean[] UPPERCASE_TESTS = new boolean[] { true, false };
  private static final String[][] EXTRA_ROLES_TESTS = new String[][] { null, new String[] {}, DEFAULT_EXTRA_ROLES };
  private static final String[][] EXCLUDE_PREFIXES_TESTS = new String[][] {
      null,
      new String[] {},
      new String[] { "extra" },
      new String[] { "ldap" },
      DEFAULT_EXCLUDE_PREFIXES
  };
  private final JpaGroupRoleProvider[] groupRoleProviderTests = new JpaGroupRoleProvider[] { null, groupRoleProvider };

  @Before
  public void setUp() {

    mappings = new HashMap<>();

    org = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(org.getId()).andReturn(ORG_NAME).anyTimes();

    Set<Role> groupRoles = new HashSet<>();
    for (int i = 1; i <= N_GROUP_ROLES; i++) {
      Role r = EasyMock.createNiceMock(Role.class);
      EasyMock.expect(r.getOrganizationId()).andReturn(ORG_NAME).anyTimes();
      EasyMock.expect(r.getName()).andReturn(format("group_role_%d", i)).anyTimes();
      EasyMock.replay(r);
      groupRoles.add(r);
    }

    User mockUser = EasyMock.createNiceMock(User.class);
    EasyMock.expect(mockUser.getUsername()).andReturn(USERNAME).anyTimes();
    EasyMock.expect(mockUser.getRoles()).andReturn(DEFAULT_INTERNAL_ROLES).anyTimes();

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(mockUser).anyTimes();

    groupRoleProvider = EasyMock.createNiceMock(JpaGroupRoleProvider.class);
    EasyMock.expect(groupRoleProvider.getRolesForGroup(GROUP_ROLE)).andReturn(new ArrayList<>(groupRoles)).anyTimes();

    EasyMock.replay(org, securityService, groupRoleProvider, mockUser);
  }

  @Test
  public void testNullAttributeNames() {
    try {
      new OpencastLdapAuthoritiesPopulator(null, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, DEFAULT_GROUP_CHECK_PREFIX,
              true, true, DEFAULT_ASSIGNMENT_ROLE_MAP, DEFAULT_ASSIGNMENT_GROUP_MAP, false, org, securityService,
              groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("A null \"attributeNames\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }
  @Test
  public void testEmptyAttributeNames() {
    try {
      new OpencastLdapAuthoritiesPopulator("", DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, DEFAULT_GROUP_CHECK_PREFIX,
              true, true, DEFAULT_ASSIGNMENT_ROLE_MAP, DEFAULT_ASSIGNMENT_GROUP_MAP, false, org, securityService,
              groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("An empty \"attributeNames\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNullGroupCheckPrefix() {
    try {
      new OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, null,
              true, true, DEFAULT_ASSIGNMENT_ROLE_MAP, DEFAULT_ASSIGNMENT_GROUP_MAP, false, org, securityService,
              groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("An null \"groupCheckPrefix\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNullOrganization() {
    try {
      new OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES,
              DEFAULT_GROUP_CHECK_PREFIX, true, true, DEFAULT_ASSIGNMENT_ROLE_MAP, DEFAULT_ASSIGNMENT_GROUP_MAP, false,
              null, securityService, groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("A null \"organization\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNullSecurityService() {
    try {
      new OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES,
              DEFAULT_GROUP_CHECK_PREFIX, true, true, DEFAULT_ASSIGNMENT_ROLE_MAP, DEFAULT_ASSIGNMENT_GROUP_MAP, false,
              org, null, groupRoleProvider, DEFAULT_EXTRA_ROLES);
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("A null \"securityService\" constructor argument for %s did not raise an exception",
            OpencastLdapAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testAttributeNotFound() {
    // Prepare the mappings
    // The attribute returns "null", i.e. does not exist in the LDAP user
    mappings.put("myAttribute", null);
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testEmptyAttributeArray() {
    // Prepare the mappings
    // The attribute returns an empty array
    mappings.put("myAttribute", new String[] {});
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testEmptySingleAttribute() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { "" });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testMultivaluedAttributeSimpleRoles() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1 ", " value2 ", " value3 ", " value4 ", GROUP_ROLE, MAPPED_ROLE_ATTR });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testSingleAttributeMultipleRoles() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { format(" value1, value2, value3 , value4 , %s ", GROUP_ROLE) });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testAttributesWithWhitespaces() {
    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute" });
    mappings.put("attribute2",
            new String[] {
                    format("value_2_1 , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                            "normal_value , normalvalue2, %s", GROUP_ROLE) });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    testCombinations(attributes);
  }

  @Test
  public void testRolePrefix() {
    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute" });
    mappings.put("attribute2", new String[] { format("value_1 , exclude_value_1, value_2, exclude_value_2",
            "normal_value , normalvalue2 , %s", GROUP_ROLE) });
    String attributes = StringUtils.join(mappings.keySet(), ", ");

    // Define prefixes
    String[] prefixes = new String[] { "normal_" };

    String[][] excludePrefixesTest = new String[][] { null, new String[0], new String[] { "exclude" } };

    testCombinationsPrefixes(attributes, prefixes, excludePrefixesTest);
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
      for (String[] excludePrefixes : EXCLUDE_PREFIXES_TESTS) {
        for (Map<String, String[]> ldapAssignmentRoleMap : ASSIGNMENT_ROLE_MAP_TESTS) {
          for (boolean upper : UPPERCASE_TESTS) {
            for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
              for (String[] extraRoles : EXTRA_ROLES_TESTS) {
                try {
                  populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes,
                          DEFAULT_GROUP_CHECK_PREFIX, true, true, ldapAssignmentRoleMap, DEFAULT_ASSIGNMENT_GROUP_MAP,
                          upper, otherOrg, securityService,
                          groupRoleProvider, extraRoles);
                  doTest(populator, mappings, prefix, excludePrefixes, DEFAULT_GROUP_CHECK_PREFIX,
                          true, true, ldapAssignmentRoleMap, DEFAULT_ASSIGNMENT_GROUP_MAP, upper, extraRoles, groupRoleProvider);
                  fail(format(
                          "Request came from a different organization (\"%s\") as the expected (\"%s\") but no exception was thrown",
                          otherOrg, org));
                } catch (SecurityException e) {
                  // OK
                }
              }
            }
          }
        }
      }
    }
  }

  private void testCombinations(final String attributes) {
    /* testing all Combinations takes quite long
    testCombinationsAdv(attributes, PREFIX_TESTS, EXCLUDE_PREFIXES_TESTS, GROUP_CHECK_PREFIX_TESTS,
                    APPLY_ATTRIBUTES_AS_ROLES_TESTS, APPLY_ATTRIBUTES_AS_GROUPS_TESTS,
                    ASSIGNMENT_ROLE_MAP_TESTS, ASSIGNMENT_GROUP_MAP_TESTS, UPPERCASE_TESTS,
                    groupRoleProviderTests, EXTRA_ROLES_TESTS,
                    org);
    */
    // therefore we only test subsets
    List<Map<String, String[]>> defaultAssignmentRoleMapList = new ArrayList();
    defaultAssignmentRoleMapList.add(DEFAULT_ASSIGNMENT_ROLE_MAP);
    List<Map<String, String[]>> defaultAssignmentGroupMapList = new ArrayList();
    defaultAssignmentGroupMapList.add(DEFAULT_ASSIGNMENT_GROUP_MAP);
    testCombinationsAdv(attributes, PREFIX_TESTS, new String[][] { DEFAULT_EXCLUDE_PREFIXES },
                    GROUP_CHECK_PREFIX_TESTS,
                    APPLY_ATTRIBUTES_AS_ROLES_TESTS, APPLY_ATTRIBUTES_AS_GROUPS_TESTS,
                    defaultAssignmentRoleMapList,
                    defaultAssignmentGroupMapList,
                    UPPERCASE_TESTS,
                    groupRoleProviderTests, EXTRA_ROLES_TESTS,
                    org);
    testCombinationsAdv(attributes, PREFIX_TESTS, new String[][] { DEFAULT_EXCLUDE_PREFIXES },
                    new String[] { DEFAULT_GROUP_CHECK_PREFIX },
                    new boolean[] { true }, new boolean[] { true },
                    ASSIGNMENT_ROLE_MAP_TESTS,
                    ASSIGNMENT_GROUP_MAP_TESTS,
                    UPPERCASE_TESTS,
                    groupRoleProviderTests, EXTRA_ROLES_TESTS,
                    org);
    testCombinationsAdv(attributes, PREFIX_TESTS, EXCLUDE_PREFIXES_TESTS,
                    new String[] { DEFAULT_GROUP_CHECK_PREFIX },
                    new boolean[] { true }, new boolean[] { true },
                    defaultAssignmentRoleMapList,
                    defaultAssignmentGroupMapList,
                    UPPERCASE_TESTS,
                    groupRoleProviderTests, EXTRA_ROLES_TESTS,
                    org);
  }

  private void testCombinationsPrefixes(final String attributes, final String[] prefixTests, final String[][] excludePrefixesTests) {
    testCombinationsAdv(attributes, prefixTests, excludePrefixesTests, GROUP_CHECK_PREFIX_TESTS,
                    new boolean[] { true }, new boolean[] { true },
                    ASSIGNMENT_ROLE_MAP_TESTS, ASSIGNMENT_GROUP_MAP_TESTS, UPPERCASE_TESTS,
                    groupRoleProviderTests, EXTRA_ROLES_TESTS,
                    org);
  }

  private void testCombinationsAdv(final String attributes, final String[] prefixTests,
                  final String[][] excludePrefixesTests, final String[] groupCheckPrefixTests,
                  final boolean[] applyAttributesAsRolesTests, final boolean[] applyAttributesAsGroupsTests,
                  final List<Map<String, String[]>> assignmentRoleMapTests,
                  final List<Map<String, String[]>> assignmentGroupMapTests,
                  final boolean[] uppercaseTests, final JpaGroupRoleProvider[] groupRoleProviderTests,
                  final String[][] extraRolesTests,
                  final Organization pOrg) {
    OpencastLdapAuthoritiesPopulator populator;

    // Test several argument combinations
    for (String prefix : prefixTests) {
      for (String[] excludePrefixes : excludePrefixesTests) {
        for (String groupCheckPrefix : groupCheckPrefixTests) {
          for (boolean attrAsRoles : applyAttributesAsRolesTests) {
            for (boolean attrAsGroups : applyAttributesAsGroupsTests) {
              for (Map<String, String[]> ldapAssignmentRoleMap : assignmentRoleMapTests) {
                for (Map<String, String[]> ldapAssignmentGroupMap : assignmentGroupMapTests) {
                  for (boolean upper : uppercaseTests) {
                    for (JpaGroupRoleProvider groupRoleProvider : groupRoleProviderTests) {
                      for (String[] extraRoles : extraRolesTests) {
                        populator = new OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes,
                                groupCheckPrefix, attrAsRoles, attrAsGroups, ldapAssignmentRoleMap,
                                ldapAssignmentGroupMap, upper, pOrg, securityService,
                                groupRoleProvider, extraRoles);
                        doTest(populator, mappings, prefix, excludePrefixes, groupCheckPrefix,
                                attrAsRoles, attrAsGroups, ldapAssignmentRoleMap, ldapAssignmentGroupMap,
                                upper, extraRoles, groupRoleProvider);
                      }
                    }
                  }
                }
              }
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
  private void doTest(OpencastLdapAuthoritiesPopulator populator, Map<String, String[]> mappings, String rolePrefix,
          String[] excludePrefixes, String groupCheckPrefix, boolean attrAsRoles, boolean attrAsGroups,
          Map<String, String[]> ldapAssignmentRoleMap, Map<String, String[]> ldapAssignmentGroupMap,
          boolean toUppercase, String[] additionalAuthorities, JpaGroupRoleProvider groupRoleProvider) {
    DirContextOperations dirContextMock = EasyMock.createNiceMock(DirContextOperations.class);

    // Populate the DirContextOperations class
    for (String attrName : mappings.keySet()) {
      EasyMock.expect(dirContextMock.getStringAttributes(attrName)).andReturn(mappings.get(attrName)).anyTimes();
    }
    EasyMock.replay(dirContextMock);

    // Prepare the expected result
    HashSet<GrantedAuthority> expectedResult = new HashSet<>();
    for (String attrName : mappings.keySet()) {
      if (mappings.get(attrName) != null) {
        for (String attrValues : mappings.get(attrName)) {
          String[] attrValuesSplitted = attrValues.split(",");

          if (attrAsRoles) {
            // no group resolve for roles added this way (groupRoleProvider = null)
            addRoles(expectedResult, rolePrefix, excludePrefixes, toUppercase, null, org,
                    attrValuesSplitted);
            if (attrAsGroups) {
              String[] attrValuesFiltered = Arrays.stream(attrValuesSplitted)
                      .filter(x -> {
                        String filter = roleCleanUpperCase(x, toUppercase);
                        return filter.startsWith(groupCheckPrefix);
                      })
                      .toArray(String[]::new);
              addRoles(expectedResult, rolePrefix, excludePrefixes, toUppercase, groupRoleProvider, org,
                      attrValuesFiltered);
            }
          }

          String[] mappedRoles = Arrays.stream(attrValuesSplitted)
                  .map(x -> roleCleanUpperCase(x, toUppercase))
                  .map(x -> ldapAssignmentRoleMap != null ? ldapAssignmentRoleMap.get(x) : null)
                  .filter(x -> x != null)
                  .flatMap(x-> Arrays.stream(x))
                  .toArray(String[]::new);
          // no prefix for roles added this way (prefix = "")
          // no group resolve for roles added this way (groupRoleProvider = null)
          addRoles(expectedResult, "", excludePrefixes, toUppercase, null, org,
                  mappedRoles);
          String[] mappedGroups = Arrays.stream(attrValuesSplitted)
                  .map(x -> roleCleanUpperCase(x, toUppercase))
                  .map(x -> ldapAssignmentGroupMap != null ? ldapAssignmentGroupMap.get(x) : null)
                  .filter(x -> x != null)
                  .flatMap(x -> Arrays.stream(x))
                  .toArray(String[]::new);
          // no prefix for roles added this way (prefix = "")
          addRoles(expectedResult, "", excludePrefixes, toUppercase, groupRoleProvider, org,
                  mappedGroups);
        }
      }
    }

    // Add the internal roles
    for (Role role : DEFAULT_INTERNAL_ROLES) {
      expectedResult.add(new SimpleGrantedAuthority(role.getName()));
    }

    // Add the additional authorities
    // no prefix for roles added this way (prefix = "")
    // no group resolve for roles added this way (groupRoleProvider = null)
    addRoles(expectedResult, "", excludePrefixes, toUppercase, null, org, additionalAuthorities);
    String[] filteredAddiAuthorities = null;
    if (additionalAuthorities != null) {
      filteredAddiAuthorities = Arrays.stream(additionalAuthorities)
              .filter(x -> roleCleanUpperCase(x, toUppercase).startsWith(groupCheckPrefix))
              .toArray(String[]::new);
    }
    // no prefix for roles added this way (prefix = "")
    addRoles(expectedResult, "", excludePrefixes, toUppercase, groupRoleProvider, org, filteredAddiAuthorities);

    // Check the response is correct
    checkResponse(populator.getGrantedAuthorities(dirContextMock, USERNAME), expectedResult);
  }

  private String roleCleanUpperCase(String rawRole, boolean toUpper) {
    if (toUpper)
      return StringUtils.trimToEmpty(rawRole).replaceAll("[\\s_]+", "_").toUpperCase();
    else
      return StringUtils.trimToEmpty(rawRole).replaceAll("[\\s_]+", "_");
  }

  private void addRoles(Set<GrantedAuthority> roles, String thePrefix, String[] excludePrefixes, boolean toUpper,
          JpaGroupRoleProvider groupProvider, Organization org, String... strRoles) {

    /*
     * The whitespace around the roles and "thePrefix" is always trimmed.
     * The special characters and internal spaces in roles and "thePrefix" are always converted to underscores
     * The roles and prefix are always converted to uppercase when the 'toUpper' is true.
     * The (trimmed, possibly uppercased) prefix is appended if, and only if:
     * - The role does not match a group role, and
     * - The role does not start with any of the "excludePrefixes" provided
     */

    if (toUpper)
      thePrefix = StringUtils.trimToEmpty(thePrefix).toUpperCase();
    else
      thePrefix = StringUtils.trimToEmpty(thePrefix);

    if (strRoles != null) {
      for (String strRole : strRoles) {
        String role = roleCleanUpperCase(strRole, toUpper);

        if (!role.isEmpty()) {
          String prefix = thePrefix;

          if (groupProvider != null) {
            List<Role> groupRoles = groupRoleProvider.getRolesForGroup(role);
            if (!groupRoles.isEmpty()) {
              logger.debug("Found group role {} with the following roles:", role);
              for (Role groupRole : groupRoles) {
                logger.debug("\t* {}", groupRole);
                roles.add(new SimpleGrantedAuthority(groupRole.getName()));
              }
              prefix = "";
            }
          } else if (!thePrefix.isEmpty()) {
            if (excludePrefixes != null) {
              for (String excludePrefix : excludePrefixes) {
                String excPrefix;
                if (toUpper)
                  excPrefix = StringUtils.trimToEmpty(excludePrefix).toUpperCase();
                else
                  excPrefix = StringUtils.trimToEmpty(excludePrefix);
                if (role.startsWith(excPrefix)) {
                  prefix = "";
                  break;
                }
              }
            }
          }

          role = (prefix + role).replaceAll("[\\s_]+", "_");

          logger.debug("Adding expected authority '{}'", role);
          roles.add(new SimpleGrantedAuthority(role));

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
