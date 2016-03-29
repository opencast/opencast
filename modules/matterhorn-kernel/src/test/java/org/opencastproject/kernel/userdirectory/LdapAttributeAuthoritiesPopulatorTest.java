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
package org.opencastproject.kernel.userdirectory;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LdapAttributeAuthoritiesPopulatorTest {

  private static final Logger logger = LoggerFactory.getLogger(LdapAttributeAuthoritiesPopulatorTest.class);

  private static final String USERNAME = "username";
  private HashMap<String, String[]> mappings;

  @Before
  public void setUp() {
    mappings = new HashMap<String, String[]>();
  }

  @Test
  public void testNullConstructorArgument() {
    try {
      new LdapAttributeAuthoritiesPopulator(null);
    } catch (NullPointerException e) {
      // OK
      return;
    }
    fail(format("A null constructor argument for %s did not raise an exception",
            LdapAttributeAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testEmptyConstructorArgument() {
    try {
      new LdapAttributeAuthoritiesPopulator(mappings.keySet());
    } catch (IllegalArgumentException e) {
      // OK
      return;
    }
    fail(format("A null constructor argument for %s did not raise an exception",
            LdapAttributeAuthoritiesPopulator.class.getName()));
  }

  @Test
  public void testNonEmptyConstructorArgument() {
    // Add a test entry to 'mappings'
    mappings.put("test", null);
    mappings.put("test2", null);
    mappings.put("test3", null);

    try {
      // Create test object
      LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());
      // Check the attribute names have been correctly set
      assertEquals("Unexpected value of the \"attributeNames\" parameter", mappings.keySet(),
              populator.getAttributeNames());
    } catch (Throwable t) {
      fail(format("Constructor shall not fail for a non-null, non-empty argument. Received unexpected exception %s: %s",
              t.getClass().getName(), t.getMessage()));
    }
  }

  @Test
  public void testSetConvertToUpperCase() {
    // Add a test entry to 'mappings'
    mappings.put("test", null);

    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    populator.setConvertToUpperCase(true);
    assertTrue("Unexpected value of the property \"convertToUpperCase\": getter or setter failed",
            populator.getConvertToUpperCase());
    populator.setConvertToUpperCase(false);
    assertFalse("Unexpected value of the property \"convertToUpperCase\": getter or setter failed",
            populator.getConvertToUpperCase());
  }

  @Test
  public void testSetRolePrefix() {
    // Add a test entry to 'mappings'
    mappings.put("test", null);

    // Sample prefixes
    final String nullPrefix = null;
    final String emptyPrefix = "";
    final String normalPrefix = "testprefix_";
    final String whitespacePrefix = "  testPrefiX_with_\twhitespaces";

    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    populator.setRolePrefix(nullPrefix);
    assertEquals("Setting a null rolePrefix failed: it should yield an empty string", populator.getRolePrefix(),
            emptyPrefix);

    populator.setRolePrefix(normalPrefix);
    assertEquals("Setting a null rolePrefix failed: prefixes do not match", populator.getRolePrefix(), normalPrefix);

    populator.setRolePrefix(whitespacePrefix);
    assertEquals("Setting a rolePrefix with whitespaces failed: prefixes do not match", populator.getRolePrefix(),
            whitespacePrefix);

    populator.setRolePrefix("");
    assertEquals("Setting an empty rolePrefix failed: prefixes do not match", populator.getRolePrefix(), "");
  }

  @Test
  public void testSetAdditionalAuthorities() {
    // Add a test entry to 'mappings'
    mappings.put("test", new String[] { "auth1, auth2", "auth3, auth4" });

    // Create a test set of additional authorities
    Set<String> additional = new HashSet<String>();
    additional.add("additional_auth1");
    additional.add("additional_auth2");
    additional.add("additional_auth3");

    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    populator.setAdditionalAuthorities(null);
    assertTrue("Setting a null set of additional authorities failed: it should yield an empty set",
            populator.getAdditionalAuthorities().isEmpty());
    doTest(populator, mappings);
    populator.setAdditionalAuthorities(additional);
    assertEquals("Setting additional authorities failed: sets do not match", populator.getAdditionalAuthorities(),
            additional);
    doTest(populator, mappings);
    populator.setAdditionalAuthorities(new HashSet<String>());
    assertEquals("Setting an empty set of additional authorities failed: additional authorities are still not empty",
            populator.getAdditionalAuthorities(), Collections.emptySet());
    doTest(populator, mappings);
  }

  @Test
  public void testAttributeNotFound() {
    // Prepare the mappings
    mappings.put("myAttribute", null);

    // Prepare the expectations
    // In this case, we expect an empty collection, so nothing to do

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);
  }

  @Test
  public void testEmptyAttributeArray() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] {});

    // Prepare the expectations
    // In this case, we expect an empty collection, so nothing to do

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);
  }

  @Test
  public void testEmptySingleAttribute() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { "" });

    // Prepare the expectations
    // In this case, we expect an empty collection, so nothing to do

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);
  }

  @Test
  public void testMultivaluedAttributeSimpleRoles() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1 ", "  value2  ", "   value3   ", "    value4    " });

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);

    // Do the same test with uppercase turned off
    populator.setConvertToUpperCase(false);
    doTest(populator, mappings);

  }

  @Test
  public void testSingleAttributeMultipleRoles() {
    // Prepare the mappings
    mappings.put("myAttribute", new String[] { " value1,   value2,  value3 ,  value4   " });

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);

    // Do the same test with uppercase turned off
    populator.setConvertToUpperCase(false);
    doTest(populator, mappings);

  }

  @Test
  public void testAttributesWithWhitespaces() {
    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", "  \n \t", "  \nthis\tis  an attribute" });
    mappings.put("attribute2",
            new String[] { "value_2_1   , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                    "normal_value , normalvalue2" });

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    doTest(populator, mappings);

    // Do the same test with uppercase turned off
    populator.setConvertToUpperCase(false);
    doTest(populator, mappings);
  }

  @Test
  public void testRolePrefix() {
    // Prepare the mappings
    mappings.put("attribute1", new String[] { " ", "\n", "\t", "\r", "  \n \t", "  \nthis\tis  an attribute" });
    mappings.put("attribute2",
            new String[] { "value_2_1   , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                    "normal_value , normalvalue2" });

    // Define prefixes
    String[] prefixes = new String[] { "normal_", "many_underscores____", "cApItAlS_", "  WH\n\niTE\tspA  cE   ",
            "   \n \t\t\n    \r" };

    // Create a test class
    LdapAttributeAuthoritiesPopulator populator = new LdapAttributeAuthoritiesPopulator(mappings.keySet());

    for (String prefix : prefixes) {
      populator.setRolePrefix(prefix);
      for (boolean b : new boolean[] { true, false }) {
        populator.setConvertToUpperCase(b);
        doTest(populator, mappings);
      }
    }

  }

  /**
   * Perform the test of an instance of the class LdapAttributeAuthoritiesPopulator.
   *
   * @param populator
   *          an instance of {@link LdapAttributeAuthoritiesPopulator} to test
   * @param mappings
   *          a {@link Map} containing the LDAP attribute name - value pairs, where the name is a {@link String} and the
   *          value an array of {@code String}s, possibly {@code null} or empty.
   * @param expected
   *          a {@link Collection} containing the expected result produced by the {@code populator}
   */
  private void doTest(LdapAttributeAuthoritiesPopulator populator, Map<String, String[]> mappings) {
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
          addRoles(expectedResult, Arrays.asList(attrValues.split(",")), populator.getRolePrefix(),
                  populator.getConvertToUpperCase());
        }
      }
    }

    // Add the additional authorities
    addRoles(expectedResult, populator.getAdditionalAuthorities(), populator.getRolePrefix(),
            populator.getConvertToUpperCase());

    // Check the response is correct
    checkResponse(populator.getGrantedAuthorities(dirContextMock, USERNAME), expectedResult);
  }

  private void addRoles(Set<GrantedAuthority> roles, Collection<? extends String> roleStrings, String prefix,
          boolean toUpper) {
    for (String roleString : roleStrings) {
      String role = roleString.trim();
      if (!role.isEmpty()) {
        if (toUpper) {
          role = (prefix.trim() + role).replaceAll("[\\s_]+", "_").toUpperCase();
        } else {
          role = (prefix.trim() + role).replaceAll("[\\s_]+", "_");
        }
        logger.debug("Creating expected attribute '{}'", role);
        roles.add(new SimpleGrantedAuthority(role));
      }
    }

  }

  /**
   * Compare the result returned by the method {@link LdapAttributeAuthoritiesPopulator#getGrantedAuthorities} and
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
