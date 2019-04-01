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

package org.opencastproject.authorization.xacml

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.SecurityService
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.HashSet

import de.schlichtherle.io.FileOutputStream

/**
 * Tests XACML features of the security service
 */
class XACMLSecurityTest {

    /** The username to use with the security service  */
    protected val currentUser = "me"

    /** The organization to use  */
    protected val organization: JaxbOrganization = DefaultOrganization()

    /** The roles to use with the security service  */
    protected val currentRoles: MutableSet<JaxbRole> = HashSet()

    // Override the behavior of the security service to use the current user and roles defined here
    protected var securityService: SecurityService? = null

    protected var authzService: XACMLAuthorizationService? = null

    @Rule
    var testFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        authzService = XACMLAuthorizationService()

        // Mock security service
        securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<User>(securityService!!.user).andAnswer { JaxbUser(currentUser, "test", organization, currentRoles) }.anyTimes()

        // Mock workspace
        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        val `in` = EasyMock.newCapture<InputStream>()
        val uri = EasyMock.newCapture<URI>()
        EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.capture(`in`))).andAnswer {
            val file = testFolder.newFile()
            val out = FileOutputStream(file)
            IOUtils.copyLarge(`in`.value, out)
            IOUtils.closeQuietly(out)
            IOUtils.closeQuietly(`in`.value)
            file.toURI()
        }.anyTimes()
        EasyMock.expect(workspace.get(EasyMock.capture(uri), EasyMock.captureBoolean(EasyMock.newCapture())))
                .andAnswer {
                    val dest = testFolder.newFile()
                    FileUtils.copyFile(File(uri.value), dest)
                    dest
                }.anyTimes()
        EasyMock.expect(workspace.read(EasyMock.capture(uri))).andAnswer { FileInputStream(uri.value.path) }.anyTimes()
        workspace.delete(EasyMock.anyObject(URI::class.java))
        EasyMock.expectLastCall<Any>().anyTimes()
        EasyMock.replay(securityService, workspace)
        authzService!!.setWorkspace(workspace)
        authzService!!.setSecurityService(securityService)
    }

    @Test
    @Throws(Exception::class)
    fun testSecurity() {

        // Create a mediapackage and some role/action tuples
        var mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()

        // Get default ACL
        var defaultAcl = authzService!!.getActiveAcl(mediapackage).a
        Assert.assertEquals(0, defaultAcl.entries!!.size.toLong())

        // Default with series
        mediapackage.series = "123"
        defaultAcl = authzService!!.getActiveAcl(mediapackage).a
        Assert.assertEquals(0, defaultAcl.entries!!.size.toLong())

        val aclSeries1 = AccessControlList()
        val entriesSeries1 = aclSeries1.entries
        entriesSeries1!!.add(AccessControlEntry("admin", "delete", true))
        entriesSeries1.add(AccessControlEntry("admin", "read", true))

        entriesSeries1.add(AccessControlEntry("student", "read", true))
        entriesSeries1.add(AccessControlEntry("student", "comment", true))

        entriesSeries1.add(AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "read", true))
        entriesSeries1.add(AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "comment", false))

        val aclSeries2 = AccessControlList()
        val entriesSeries2 = aclSeries2.entries
        entriesSeries2!!.add(AccessControlEntry("admin", "delete", true))
        entriesSeries2.add(AccessControlEntry("admin", "read", true))

        entriesSeries2.add(AccessControlEntry("student", "read", false))
        entriesSeries2.add(AccessControlEntry("student", "comment", false))

        entriesSeries2.add(AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "read", true))
        entriesSeries2.add(AccessControlEntry(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, "comment", false))

        val aclEpisode = AccessControlList()

        // Add the security policy to the mediapackage
        authzService!!.setAcl(mediapackage, AclScope.Series, aclSeries1)

        // Ensure that the permissions specified are respected by the security service
        currentRoles.clear()
        currentRoles.add(JaxbRole("admin", organization, ""))
        Assert.assertTrue(authzService!!.hasPermission(mediapackage, "delete"))
        Assert.assertTrue(authzService!!.hasPermission(mediapackage, "read"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "comment"))
        currentRoles.clear()
        currentRoles.add(JaxbRole("student", organization, ""))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "delete"))
        Assert.assertTrue(authzService!!.hasPermission(mediapackage, "read"))
        Assert.assertTrue(authzService!!.hasPermission(mediapackage, "comment"))
        currentRoles.clear()
        currentRoles.add(JaxbRole("admin", organization))

        mediapackage = authzService!!.setAcl(mediapackage, AclScope.Episode, aclEpisode).a
        Assert.assertEquals(AclScope.Merged, authzService!!.getActiveAcl(mediapackage).b)
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "delete"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "read"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "comment"))

        mediapackage = authzService!!.removeAcl(mediapackage, AclScope.Episode)

        val computedAcl = authzService!!.getActiveAcl(mediapackage).a
        Assert.assertEquals("ACLs are the same size?", entriesSeries1.size.toLong(), computedAcl.entries!!.size.toLong())
        Assert.assertTrue("ACLs contain the same ACEs?", computedAcl.entries!!.containsAll(entriesSeries1))

        authzService!!.setAcl(mediapackage, AclScope.Series, aclSeries2)

        currentRoles.clear()
        currentRoles.add(JaxbRole("student", organization))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "delete"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "read"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "comment"))

        currentRoles.clear()
        currentRoles.add(JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, organization, ""))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "delete"))
        Assert.assertTrue(authzService!!.hasPermission(mediapackage, "read"))
        Assert.assertFalse(authzService!!.hasPermission(mediapackage, "comment"))
    }

    companion object {

        /** The logger  */
        protected val logger = LoggerFactory.getLogger(XACMLSecurityTest::class.java)
    }
}
