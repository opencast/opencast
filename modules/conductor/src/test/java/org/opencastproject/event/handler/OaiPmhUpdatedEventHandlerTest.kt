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
package org.opencastproject.event.handler

import org.easymock.EasyMock.anyBoolean
import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.capture
import org.easymock.EasyMock.expect
import org.junit.Assert.assertEquals

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.CatalogImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase
import org.opencastproject.oaipmh.persistence.Query
import org.opencastproject.oaipmh.persistence.SearchResult
import org.opencastproject.oaipmh.persistence.SearchResultItem
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.workspace.api.Workspace

import org.easymock.Capture
import org.easymock.EasyMockRule
import org.easymock.EasyMockSupport
import org.easymock.Mock
import org.easymock.MockType
import org.easymock.TestSubject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import java.net.URI
import java.util.Collections
import java.util.Date
import java.util.Hashtable

/**
 * This class contains some tests for the [OaiPmhUpdatedEventHandler].
 */
class OaiPmhUpdatedEventHandlerTest : EasyMockSupport() {

    @Rule
    var rule = EasyMockRule(this)

    @TestSubject
    private val cut = OaiPmhUpdatedEventHandler()

    // Dependencies of class under test
    @Mock
    private val securityServiceMock: SecurityService? = null
    @Mock
    private val workspace: Workspace? = null
    @Mock
    private val oaiPmhDatabaseMock: OaiPmhDatabase? = null
    @Mock
    private val oaiPmhPublicationService: OaiPmhPublicationService? = null

    private var adminUserCapture: Capture<User>? = null
    private var queryCapture: Capture<Query>? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        cut.systemAccount = SYSTEM_ACCOUNT
        val props = Hashtable<String, String>()
        props[OaiPmhUpdatedEventHandler.CFG_PROPAGATE_EPISODE] = "true"
        props[OaiPmhUpdatedEventHandler.CFG_FLAVORS] = "dublincore/*,security/*"
        props[OaiPmhUpdatedEventHandler.CFG_TAGS] = "archive"
        cut.updated(props)

        expect<InputStream>(workspace!!.read(anyObject())).andAnswer { javaClass.getResourceAsStream("/episode.xml") }.anyTimes()
    }

    /**
     * Tests "normal" behavior, where the media package contains at least one element with the given flavor and tags
     */
    @Test
    @Throws(Exception::class)
    fun testHandleEvent() {
        val episodeCatalog = CatalogImpl.newInstance()
        episodeCatalog.setURI(URI.create("/episode.xml"))
        episodeCatalog.flavor = MediaPackageElementFlavor.parseFlavor("dublincore/episode")
        episodeCatalog.addTag("archive")
        val updatedMp = createMediaPackage(episodeCatalog)

        // these are the interactions we expect with the security service
        mockSecurityService()

        // these are the interactions we expect for the OAI-PMH database
        mockOaiPmhDatabase()

        val mpCapture = Capture.newInstance<MediaPackage>()
        val repositoryCapture = Capture.newInstance<String>()
        val flavorsCapture = Capture.newInstance<Set<String>>()
        val tagsCapture = Capture.newInstance<Set<*>>()
        expect(oaiPmhPublicationService!!.updateMetadata(capture(mpCapture), capture(repositoryCapture),
                capture(flavorsCapture), capture<Set>(tagsCapture), anyBoolean()))
                .andAnswer { mock(Job::class.java) }.times(1)

        replayAll()

        cut.handleEvent(createSnapshot(updatedMp))

        assertEquals(updatedMp.identifier.compact(), mpCapture.value.identifier.compact())
        assertEquals(OAIPMH_REPOSITORY, repositoryCapture.value)
        Assert.assertNotNull(flavorsCapture.value)
        Assert.assertTrue(flavorsCapture.value.contains("dublincore/*"))
        Assert.assertTrue(flavorsCapture.value.contains("security/*"))
        Assert.assertTrue(tagsCapture.value.contains("archive"))
    }

    /**
     * Tests if publishing to OAI-PMH is skipped, if the episode is not known by OAI-PMH.
     */
    @Test
    @Throws(Exception::class)
    fun testEpisodeNotKnownByOaiPmh() {
        val episodeCatalog = CatalogImpl.newInstance()
        episodeCatalog.setURI(URI.create("/episode.xml"))
        episodeCatalog.flavor = MediaPackageElementFlavor.parseFlavor("dublincore/episode")
        episodeCatalog.addTag("archive")
        val updatedMp = createMediaPackage(episodeCatalog)

        // these are the interactions we expect with the security service
        mockSecurityService()

        // mock the OAI-PMH database
        val searchResultMock = mock<SearchResult, SearchResult>(MockType.NICE, SearchResult::class.java)
        expect(searchResultMock.items).andReturn(Collections.EMPTY_LIST).anyTimes()
        queryCapture = Capture.newInstance()
        expect(oaiPmhDatabaseMock!!.search(capture(queryCapture))).andReturn(searchResultMock)

        replayAll()

        cut.handleEvent(createSnapshot(updatedMp))

        // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
        verifyAll()
    }

    /**
     * The media package does not contains elements with the configured tag, the publication should be skipped
     */
    @Test
    @Throws(Exception::class)
    fun testNoElementsWithGivenFlavorAndTags() {
        val episodeCatalog = CatalogImpl.newInstance()
        episodeCatalog.setURI(URI.create("/episode.xml"))
        episodeCatalog.flavor = MediaPackageElementFlavor.parseFlavor("dublincore/episode")
        // the episode catalog isn't tagged with archive
        val updatedMp = createMediaPackage()

        // these are the interactions we expect with the security service
        mockSecurityService()

        replayAll()

        cut.handleEvent(createSnapshot(updatedMp))

        // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
        verifyAll()
    }

    /**
     * The media package does not contains any elements, the publication should be skipped
     */
    @Test
    @Throws(Exception::class)
    fun testNoElementsForPublishing() {
        val updatedMp = createMediaPackage()

        // these are the interactions we expect with the security service
        mockSecurityService()

        replayAll()

        cut.handleEvent(createSnapshot(updatedMp))

        // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
        verifyAll()
    }

    @Throws(Exception::class)
    private fun createSnapshot(mediaPackage: MediaPackage): AssetManagerItem.TakeSnapshot {
        val acl = AccessControlList()
        return AssetManagerItem.add(workspace!!, mediaPackage, acl, 0L, Date())
    }

    @Throws(MediaPackageException::class)
    private fun createMediaPackage(vararg elements: MediaPackageElement): MediaPackage {
        val result = MP_BUILDER.createNew()
        result.title = MEDIA_PACKAGE_TITLE
        for (mpe in elements) {
            result.add(mpe)
        }
        return result
    }

    private fun mockSecurityService() {
        val organization = DefaultOrganization()
        val user = mock<User, User>(User::class.java)
        expect(user.organization).andReturn(organization).anyTimes()
        adminUserCapture = Capture.newInstance()
        expect(securityServiceMock!!.user).andReturn(user)
        expect(securityServiceMock.organization).andReturn(organization)
        securityServiceMock.user = capture(adminUserCapture)
        securityServiceMock.user = user
        securityServiceMock.organization = organization
    }

    private fun mockOaiPmhDatabase() {
        val searchResultMock = mock<SearchResult, SearchResult>(MockType.NICE, SearchResult::class.java)
        val searchResultItemMock = mock<SearchResultItem, SearchResultItem>(MockType.NICE, SearchResultItem::class.java)
        expect(searchResultMock.items).andReturn(listOf(searchResultItemMock)).anyTimes()
        expect(searchResultItemMock.repository).andReturn(OAIPMH_REPOSITORY).anyTimes()
        queryCapture = Capture.newInstance()
        expect(oaiPmhDatabaseMock!!.search(capture(queryCapture))).andReturn(searchResultMock)
    }

    companion object {

        private val MEDIA_PACKAGE_TITLE = "test.mediapackage.title"
        private val OAIPMH_REPOSITORY = "test.oaipmh.repository"
        private val SYSTEM_ACCOUNT = "opencast_system_account"
        private val MP_BUILDER = MediaPackageBuilderFactory.newInstance()
                .newMediaPackageBuilder()
    }
}
