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

package org.opencastproject.userdirectory.sakai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.User

import org.apache.commons.collections4.IteratorUtils
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.util.HashSet

class SakaiUserProviderTest {

    protected var sakaiProvider: SakaiUserProviderInstance? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {

        val instructorRoles = HashSet<String>()
        instructorRoles.add("Site owner")
        instructorRoles.add("Instructor")
        instructorRoles.add("maintain")

        sakaiProvider = SakaiUserProviderInstance("sample_pid", DefaultOrganization(), "https://qa11-mysql.nightly.sakaiproject.org",
                "admin", "admin", "^[a-zA-Z0-9-]+$", "^[0-9a-zA-Z]{6,}$", instructorRoles, 100, 10)
    }

    private fun hasRole(roles: Set<Role>, roleName: String): Boolean {
        for (role in roles) {
            if (roleName == role.name)
                return true
        }

        return false
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testLoadUser() {
        val user = sakaiProvider!!.loadUser("datest")
        assertNotNull(user)

        // Generic group role added for all Sakai users
        assertTrue(hasRole(user!!.roles, "ROLE_GROUP_SAKAI"))

        // Test role specific to user datest on test Sakai instances
        assertTrue(hasRole(user.roles, "DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor"))
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testFindUser() {

        // User exists
        assertEquals(1, IteratorUtils.toList(sakaiProvider!!.findUsers("datest", 0, 1)).size.toLong())

        // User exists but fails regexp pattern (minimum 6 characters)
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findUsers("admin", 0, 1)).size.toLong())

        // User doesn't exist
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findUsers("nobody", 0, 1)).size.toLong())
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testFindRoles() {

        // Site exists
        assertEquals(2, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426%", Role.Target.ACL, 0, 2)).size.toLong())
        assertEquals(1, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Learner", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(1, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(1, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor%", Role.Target.ACL, 0, 1)).size.toLong())

        // Site fails pattern
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findRoles("!gateway%", Role.Target.ACL, 0, 2)).size.toLong())

        // Site or role does not exist
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findRoles("unknown%", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findRoles("unknown", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426__Learner", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(0, IteratorUtils.toList(sakaiProvider!!.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_", Role.Target.ACL, 0, 1)).size.toLong())

    }
}
