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

package org.opencastproject.userdirectory.moodle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Role
import org.opencastproject.security.api.User

import org.apache.commons.collections4.IteratorUtils
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.net.URI

class MoodleUserProviderTest {
    private var moodleProvider: MoodleUserProviderInstance? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        moodleProvider = MoodleUserProviderInstance("sample_pid",
                MoodleWebServiceImpl(URI("http://moodle/webservice/rest/server.php"), "myToken"),
                DefaultOrganization(), "^[0-9]+$", "^[0-9a-zA-Z_]+$", "^[0-9]+$", true, 100, 10)
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testLoadUser() {
        var user = moodleProvider!!.loadUser("testdozent22")
        assertNotNull(user)

        // Generic group role added for all Moodle users
        assertTrue(hasRole(user!!.roles, "ROLE_GROUP_MOODLE"))

        // Test role specific to user datest on test Moodle instances
        assertTrue(hasRole(user.roles, "6928_Learner"))
        assertTrue(hasRole(user.roles, "10765_Instructor"))

        user = moodleProvider!!.loadUser("nobody")
        assertNull(user)
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testFindUser() {
        // User exists
        assertEquals(1, IteratorUtils.toList(moodleProvider!!.findUsers("testdozent22", 0, 1)).size.toLong())

        // User exists but fails regexp pattern (minimum 6 characters)
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findUsers("admin", 0, 1)).size.toLong())

        // User doesn't exist
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findUsers("nobody", 0, 1)).size.toLong())
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testFindRoles() {
        // Site exists
        assertEquals(2, IteratorUtils.toList(moodleProvider!!.findRoles("10765%", Role.Target.ACL, 0, 2)).size.toLong())
        assertEquals(1, IteratorUtils.toList(moodleProvider!!.findRoles("6928_Learner", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(1, IteratorUtils.toList(moodleProvider!!.findRoles("6928_Learner%", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(1, IteratorUtils.toList(moodleProvider!!.findRoles("10765_Instructor", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(1, IteratorUtils.toList(moodleProvider!!.findRoles("10765_Instructor%", Role.Target.ACL, 0, 1)).size.toLong())

        // Site fails pattern
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findRoles("!gateway%", Role.Target.ACL, 0, 2)).size.toLong())

        // Site or role does not exist
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findRoles("6928__Learner", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findRoles("10765__Instructor", Role.Target.ACL, 0, 1)).size.toLong())
        assertEquals(0, IteratorUtils.toList(moodleProvider!!.findRoles("10765_", Role.Target.ACL, 0, 1)).size.toLong())
    }

    private fun hasRole(roles: Set<Role>, roleName: String): Boolean {
        for (role in roles)
            if (roleName == role.name)
                return true

        return false
    }
}
