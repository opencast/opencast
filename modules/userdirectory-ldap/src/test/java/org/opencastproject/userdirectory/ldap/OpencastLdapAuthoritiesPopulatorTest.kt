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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.userdirectory.ldap

import java.lang.String.format
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.userdirectory.JpaGroupRoleProvider

import org.apache.commons.lang3.StringUtils
import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

class OpencastLdapAuthoritiesPopulatorTest {

    /** A map containing the set of LDAP arguments and their values (they keys can be multivalued)  */
    private var mappings: HashMap<String, Array<String>>? = null

    private var securityService: SecurityService? = null
    private var groupRoleProvider: JpaGroupRoleProvider? = null
    private var org: Organization? = null
    private val groupRoleProviderTests = arrayOf<JpaGroupRoleProvider>(null, groupRoleProvider)

    @Before
    fun setUp() {

        mappings = HashMap()

        org = EasyMock.createNiceMock<Organization>(Organization::class.java)
        EasyMock.expect(org!!.id).andReturn(ORG_NAME).anyTimes()

        val groupRoles = HashSet<Role>()
        for (i in 1..N_GROUP_ROLES) {
            val r = EasyMock.createNiceMock<Role>(Role::class.java)
            EasyMock.expect(r.organizationId).andReturn(ORG_NAME).anyTimes()
            EasyMock.expect(r.name).andReturn(format("group_role_%d", i)).anyTimes()
            EasyMock.replay(r)
            groupRoles.add(r)
        }

        val mockUser = EasyMock.createNiceMock<User>(User::class.java)
        EasyMock.expect(mockUser.username).andReturn(USERNAME).anyTimes()
        EasyMock.expect(mockUser.roles).andReturn(DEFAULT_INTERNAL_ROLES).anyTimes()

        securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService!!.organization).andReturn(org).anyTimes()
        EasyMock.expect(securityService!!.user).andReturn(mockUser).anyTimes()

        groupRoleProvider = EasyMock.createNiceMock<JpaGroupRoleProvider>(JpaGroupRoleProvider::class.java)
        EasyMock.expect(groupRoleProvider!!.getRolesForGroup(GROUP_ROLE)).andReturn(ArrayList(groupRoles)).anyTimes()

        EasyMock.replay(org, securityService, groupRoleProvider, mockUser)
    }

    @Test
    fun testNullAttributeNames() {
        try {
            OpencastLdapAuthoritiesPopulator(null, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, false, org, securityService,
                    groupRoleProvider, *DEFAULT_EXTRA_ROLES)
        } catch (e: IllegalArgumentException) {
            // OK
            return
        }

        fail(format("A null \"attributeNames\" constructor argument for %s did not raise an exception",
                OpencastLdapAuthoritiesPopulator::class.java.name))
    }

    @Test
    fun testEmptyAttributeNames() {
        try {
            OpencastLdapAuthoritiesPopulator("", DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, false, org, securityService,
                    groupRoleProvider, *DEFAULT_EXTRA_ROLES)
        } catch (e: IllegalArgumentException) {
            // OK
            return
        }

        fail(format("An empty \"attributeNames\" constructor argument for %s did not raise an exception",
                OpencastLdapAuthoritiesPopulator::class.java.name))
    }

    @Test
    fun testNullOrganization() {
        try {
            OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, false, null, securityService, groupRoleProvider, *DEFAULT_EXTRA_ROLES)
        } catch (e: IllegalArgumentException) {
            // OK
            return
        }

        fail(format("A null \"organization\" constructor argument for %s did not raise an exception",
                OpencastLdapAuthoritiesPopulator::class.java.name))
    }

    @Test
    fun testNullSecurityService() {
        try {
            OpencastLdapAuthoritiesPopulator(DEFAULT_STR_ATTRIBUTE_NAMES, DEFAULT_PREFIX, DEFAULT_EXCLUDE_PREFIXES, false,
                    org, null, groupRoleProvider, *DEFAULT_EXTRA_ROLES)
        } catch (e: IllegalArgumentException) {
            // OK
            return
        }

        fail(format("A null \"securityService\" constructor argument for %s did not raise an exception",
                OpencastLdapAuthoritiesPopulator::class.java.name))
    }

    @Test
    fun testAttributeNotFound() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        // The attribute returns "null", i.e. does not exist in the LDAP user
        mappings!!["myAttribute"] = null
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testEmptyAttributeArray() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        // The attribute returns an empty array
        mappings!!["myAttribute"] = arrayOf()
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testEmptySingleAttribute() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["myAttribute"] = arrayOf("")
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testMultivaluedAttributeSimpleRoles() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["myAttribute"] = arrayOf(" value1 ", " value2 ", " value3 ", " value4 ", GROUP_ROLE)
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testSingleAttributeMultipleRoles() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["myAttribute"] = arrayOf(format(" value1, value2, value3 , value4 , %s ", GROUP_ROLE))
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testAttributesWithWhitespaces() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["attribute1"] = arrayOf(" ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute")
        mappings!!["attribute2"] = arrayOf(format("value_2_1 , value\nwith\n\n multiple\t whitespaces, value____with several_underscores",
                "normal_value , normalvalue2, %s", GROUP_ROLE))
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testRolePrefix() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["attribute1"] = arrayOf(" ", "\n", "\t", "\r", " \n \t", " \nthis\tis an attribute")
        mappings!!["attribute2"] = arrayOf(format("value_1 , exclude_value_1, value_2, exclude_value_2",
                "normal_value , normalvalue2 , %s", GROUP_ROLE))
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Define prefixes
        val prefixes = arrayOf("normal_")

        val excludePrefixesTest = arrayOf<Array<String>>(null, arrayOfNulls(0), arrayOf("exclude"))

        // Test several argument combinations
        for (prefix in prefixes) {
            for (excludePrefixes in excludePrefixesTest) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, org,
                                    securityService, groupRoleProvider, *extraRoles)
                            doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testWrongOrganization() {
        var populator: OpencastLdapAuthoritiesPopulator

        // Prepare the mappings
        mappings!!["myAttribute"] = arrayOf(" value1 ", " value2 ", " value3 ", " value4 ")
        val attributes = StringUtils.join(mappings!!.keys, ", ")

        // Prepare the alternative organization
        val otherOrg = EasyMock.createNiceMock<Organization>(Organization::class.java)
        EasyMock.expect(otherOrg.id).andReturn("other_organization").anyTimes()
        EasyMock.replay(otherOrg)

        // Test several argument combinations
        for (prefix in PREFIX_TESTS) {
            for (excludePrefixes in EXCLUDE_PREFIXES_TESTS) {
                for (upper in UPPERCASE_TESTS) {
                    for (groupRoleProvider in groupRoleProviderTests) {
                        for (extraRoles in EXTRA_ROLES_TESTS) {
                            try {
                                populator = OpencastLdapAuthoritiesPopulator(attributes, prefix, excludePrefixes, upper, otherOrg,
                                        securityService, groupRoleProvider, *extraRoles)
                                doTest(populator, mappings!!, prefix, excludePrefixes, upper, extraRoles, groupRoleProvider)
                                fail(format(
                                        "Request came from a different organization (\"%s\") as the expected (\"%s\") but no exception was thrown",
                                        otherOrg, org))
                            } catch (e: SecurityException) {
                                // OK
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
     * an instance of [OpencastLdapAuthoritiesPopulator] to test
     * @param mappings
     * a [Map] containing the LDAP attribute name - value pairs, where the name is a [String] and the
     * value an array of `String`s, possibly `null` or empty.
     */
    private fun doTest(populator: OpencastLdapAuthoritiesPopulator, mappings: Map<String, Array<String>>, rolePrefix: String,
                       excludePrefixes: Array<String>, toUppercase: Boolean, additionalAuthorities: Array<String>,
                       groupRoleProvider: JpaGroupRoleProvider) {
        val dirContextMock = EasyMock.createNiceMock<DirContextOperations>(DirContextOperations::class.java)

        // Populate the DirContextOperations class
        for (attrName in mappings.keys) {
            EasyMock.expect(dirContextMock.getStringAttributes(attrName)).andReturn(mappings[attrName]).anyTimes()
        }
        EasyMock.replay(dirContextMock)

        // Prepare the expected result
        val expectedResult = HashSet<GrantedAuthority>()
        for (attrName in mappings.keys) {
            if (mappings[attrName] != null) {
                for (attrValues in mappings[attrName]) {
                    addRoles(expectedResult, rolePrefix, excludePrefixes, toUppercase, groupRoleProvider, org,
                            *attrValues.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
                }
            }
        }

        // Add the internal roles
        for (role in DEFAULT_INTERNAL_ROLES) {
            expectedResult.add(SimpleGrantedAuthority(role.name))
        }

        // Add the additional authorities
        addRoles(expectedResult, rolePrefix, excludePrefixes, toUppercase, groupRoleProvider, org, *additionalAuthorities)

        // Check the response is correct
        checkResponse(populator.getGrantedAuthorities(dirContextMock, USERNAME), expectedResult)
    }

    private fun addRoles(roles: MutableSet<GrantedAuthority>, thePrefix: String, excludePrefixes: Array<String>?, toUpper: Boolean,
                         groupProvider: JpaGroupRoleProvider?, org: Organization?, vararg strRoles: String) {
        var thePrefix = thePrefix

        /*
     * The whitespace around the roles and "thePrefix" is always trimmed.
     * The special characters and internal spaces in roles and "thePrefix" are always converted to underscores
     * The roles and prefix are always converted to uppercase when the 'toUpper' is true.
     * The (trimmed, possibly uppercased) prefix is appended if, and only if:
     * - The role does not match a group role, and
     * - The role does not start with any of the "excludePrefixes" provided
     */

        if (toUpper)
            thePrefix = StringUtils.trimToEmpty(thePrefix).toUpperCase()
        else
            thePrefix = StringUtils.trimToEmpty(thePrefix)

        if (strRoles != null) {
            for (strRole in strRoles) {
                var role: String
                if (toUpper)
                    role = StringUtils.trimToEmpty(strRole).replace("[\\s_]+".toRegex(), "_").toUpperCase()
                else
                    role = StringUtils.trimToEmpty(strRole).replace("[\\s_]+".toRegex(), "_")

                if (!role.isEmpty()) {
                    var prefix = thePrefix

                    if (groupProvider != null) {
                        val groupRoles = groupRoleProvider!!.getRolesForGroup(role)
                        if (!groupRoles.isEmpty()) {
                            logger.debug("Found group role {} with the following roles:", role)
                            for (groupRole in groupRoles) {
                                logger.debug("\t* {}", groupRole)
                                roles.add(SimpleGrantedAuthority(groupRole.name))
                            }
                            prefix = ""
                        }
                    } else if (!thePrefix.isEmpty()) {
                        if (excludePrefixes != null) {
                            for (excludePrefix in excludePrefixes) {
                                val excPrefix: String
                                if (toUpper)
                                    excPrefix = StringUtils.trimToEmpty(excludePrefix).toUpperCase()
                                else
                                    excPrefix = StringUtils.trimToEmpty(excludePrefix)
                                if (role.startsWith(excPrefix)) {
                                    prefix = ""
                                    break
                                }
                            }
                        }
                    }

                    role = (prefix + role).replace("[\\s_]+".toRegex(), "_")

                    logger.debug("Adding expected authority '{}'", role)
                    roles.add(SimpleGrantedAuthority(role))

                }
            }
        }
    }

    /**
     * Compare the result returned by the method [OpencastLdapAuthoritiesPopulator.getGrantedAuthorities] and
     * [Collection] of [GrantedAuthority]'s containing the expected results.
     *
     * @param actual
     * the actual output of the method; it should match the contents in `expected`
     * @param expected
     * the expected output
     */
    private fun checkResponse(actual: Collection<GrantedAuthority>,
                              expected: Collection<GrantedAuthority>) {

        for (auth in actual) {
            assertTrue(format("Authorities populator returned unexpected authority: %s", auth), expected.contains(auth))
        }

        for (auth in expected) {
            assertTrue(format("Authorities populator did not return expected authority: %s", auth), actual.contains(auth))
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(OpencastLdapAuthoritiesPopulatorTest::class.java)

        private val DEFAULT_ATTRIBUTE_NAMES: Set<String>
        private val DEFAULT_INTERNAL_ROLES: Set<Role>
        private val DEFAULT_STR_ATTRIBUTE_NAMES: String

        private val DEFAULT_PREFIX = "ROLE_"

        private val DEFAULT_EXTRA_ROLES: Array<String>

        private val DEFAULT_EXCLUDE_PREFIXES = arrayOf("other", "ldap")

        private val USERNAME = "username"
        private val ORG_NAME = "my_organization_id"
        private val GROUP_ROLE = "THIS_IS_THE_GROUP_ROLE"
        private val N_GROUP_ROLES = 3
        private val N_LDAP_ATTRIBUTES = 3
        private val N_EXTRA_ROLES = 2
        private val N_INTERNAL_ROLES = 3

        init {
            val tempSet = HashSet<String>()
            for (i in 1..N_LDAP_ATTRIBUTES)
                tempSet.add(format("ldap_attribute_%d", i))
            DEFAULT_ATTRIBUTE_NAMES = Collections.unmodifiableSet(tempSet)
            DEFAULT_STR_ATTRIBUTE_NAMES = StringUtils.join(DEFAULT_ATTRIBUTE_NAMES, ", ")

            val roleSet = HashSet<Role>()
            for (i in 1..N_INTERNAL_ROLES) {
                val r = EasyMock.createNiceMock<Role>(Role::class.java)
                EasyMock.expect(r.name).andReturn("internal_role_" + (i + 1)).anyTimes()
                EasyMock.replay(r)
                roleSet.add(r)
            }
            DEFAULT_INTERNAL_ROLES = Collections.unmodifiableSet(roleSet)

            DEFAULT_EXTRA_ROLES = arrayOfNulls(N_EXTRA_ROLES)
            for (i in 0 until N_EXTRA_ROLES)
                DEFAULT_EXTRA_ROLES[i] = format("extra_role_%d", i)
        }

        private val PREFIX_TESTS = arrayOf<String>(null, "", DEFAULT_PREFIX)
        private val UPPERCASE_TESTS = booleanArrayOf(true, false)
        private val EXTRA_ROLES_TESTS = arrayOf<Array<String>>(null, arrayOf(), DEFAULT_EXTRA_ROLES)
        private val EXCLUDE_PREFIXES_TESTS = arrayOf<Array<String>>(null, arrayOf(), arrayOf("extra"), arrayOf("ldap"), DEFAULT_EXCLUDE_PREFIXES)
    }
}
