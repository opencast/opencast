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

import org.opencastproject.security.api.User

import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.util.Arrays

class LdapUserProviderTest {

    protected var service: LdapUserProviderInstance? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        /*
     * service = new LdapUserProviderInstance("sample_pid", new DefaultOrganization(), "ou=people,dc=berkeley,dc=edu",
     * "(uid={0})", "ldap://ldap.berkeley.edu", null, null, "berkeleyEduAffiliations,departmentNumber", null, 100, 10);
     */
    }

    @Ignore("Ignore this test by default, since it requires internet connectivity, and the user's details may change.")
    @Test
    @Throws(Exception::class)
    fun testLookup() {
        val user = service!!.loadUser("231693")
        Assert.assertNotNull(user)
        Assert.assertTrue(Arrays.asList<Set<Role>>(user!!.roles).contains("ROLE_URSET"))
        Assert.assertTrue(Arrays.asList<Set<Role>>(user.roles).contains("ROLE_EMPLOYEE-TYPE-STAFF"))
    }

    /*
   * At the time this test was written, the LDAP attributes for this user included the following: $ /usr/bin/ldapsearch
   * -LLL -x -H ldap://ldap.berkeley.edu -b 'ou=people,dc=berkeley,dc=edu' '(uid=231693)' dn:
   * uid=231693,ou=people,dc=berkeley,dc=edu berkeleyEduUnitCalNetDeptName: Ed Tech mail: jholtzman@berkeley.edu title:
   * Senior Software Developer postalAddress: 9 Dwinelle Hall$Berkeley, CA 94720-2535 postalCode: 94720-2535 st: CA l:
   * Berkeley street: 9 Dwinelle Hall telephoneNumber: +1 510 269-4829 displayName: Josh Holtzman cn: Holtzman, Josh
   * givenName: Josh sn: Holtzman berkeleyEduAffiliations: EMPLOYEE-TYPE-STAFF berkeleyEduModDate: 20110217155308Z
   * berkeleyEduDeptUnitHierarchyString: UCBKL-EVCP2-VPAPF-URMED-URSET departmentNumber: URSET labeledUri:
   * http://josh.media.berkeley.edu/ o: University of California, Berkeley berkeleyEduPrimaryDeptUnitHierarchyString:
   * UCBKL-EVCP2-VPAPF-URMED-URSET berkeleyEduUnitHRDeptName: ETS EducTechnology uid: 231693 ou: people objectClass: top
   * objectClass: person objectClass: organizationalperson objectClass: inetorgperson objectClass: berkeleyEduPerson
   * objectClass: eduPerson objectClass: ucEduPerson berkeleyEduPrimaryDeptUnit: URSET
   */
}
