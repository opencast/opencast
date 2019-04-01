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

package org.opencastproject.adminui.index

import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.index.service.impl.index.event.EventSearchQuery
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.impl.TestUtils
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.User

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File

@Ignore
class AdminUIEventSearchQueryTest {

    private var wrongOrgAdminUser: User? = null
    private var totalAdmin: JaxbUser? = null
    private var noAccessUser: JaxbUser? = null
    private var readAccessUser: JaxbUser? = null
    private var writeAccessUser: JaxbUser? = null
    private var wrongRolesUser: JaxbUser? = null

    private var rightOrg: JaxbOrganization? = null
    private var wrongOrg: JaxbOrganization? = null

    /**
     * Does the cleanup after each test.
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        idx!!.clear()
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        rightOrg = JaxbOrganization(RIGHT_ORG)
        wrongOrg = JaxbOrganization(WRONG_ORG)

        wrongOrgAdminUser = JaxbUser("Wrong Org User", "Provider", wrongOrg!!, JaxbRole(ORG_OWNER, wrongOrg!!))
        totalAdmin = JaxbUser("Total Admin User", "Provider", rightOrg!!, JaxbRole(TOTAL_ADMIN, rightOrg!!))
        noAccessUser = JaxbUser("No Access User", "Provider", rightOrg!!, JaxbRole(VIEWER, rightOrg!!))
        wrongRolesUser = JaxbUser("Wrong Role User", "Provider", rightOrg!!, JaxbRole("Wrong:Role", rightOrg!!))
        readAccessUser = JaxbUser("Read Access User", "Provider", rightOrg!!, JaxbRole(EDITOR, rightOrg!!))
        writeAccessUser = JaxbUser("Write Access User", "Provider", rightOrg!!, JaxbRole(PRODUCER, rightOrg!!))

        populateIndex()
    }

    @Test
    @Throws(SearchIndexException::class)
    fun aclLimited() {
        idx!!.getByQuery(EventSearchQuery(WRONG_ORG, wrongOrgAdminUser!!))
    }

    /**
     * Adds sample pages to the search index and returns the number of documents added.
     */
    @Throws(Exception::class)
    private fun populateIndex() {
        for (i in 0..9) {
            val event = Event(Integer.toString(i), rightOrg!!.id)
            idx!!.addOrUpdate(event)
        }
    }

    companion object {
        private val RIGHT_ORG = "rightOrg"
        private val WRONG_ORG = "wrongOrg"
        private val TOTAL_ADMIN = "matterhorn:admin"
        private val ORG_OWNER = "matterhorn:owner"
        private val PRODUCER = "matterhorn:producer"
        private val EDITOR = "matterhorn:editor"
        private val VIEWER = "matterhorn:viewer"

        /** The search index  */
        // protected static AdminUISearchIndexStub idx = null;
        private var idx: AdminUISearchIndex? = null

        /** The name of the index  */
        private val indexName = "adminui"

        @ClassRule
        var testFolder = TemporaryFolder()

        @BeforeClass
        @Throws(Exception::class)
        fun setupClass() {
            TestUtils.startTesting()
            val idxRoot = testFolder.newFolder()
            AdminUIElasticsearchUtils.createIndexConfigurationAt(idxRoot, indexName)
            idx = AdminUISearchIndex()
            idx!!.activate(null)
        }
    }
}
