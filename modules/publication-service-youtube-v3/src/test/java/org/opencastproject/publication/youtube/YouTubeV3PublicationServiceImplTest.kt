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

package org.opencastproject.publication.youtube

import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.createMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.expectLastCall
import org.easymock.EasyMock.replay

import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.publication.youtube.auth.ClientCredentials
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.HostRegistration
import org.opencastproject.serviceregistry.api.HostRegistrationInMemory
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workspace.api.Workspace

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.Video

import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.IOException
import java.util.Dictionary
import java.util.LinkedList
import java.util.Properties

class YouTubeV3PublicationServiceImplTest {

    private var service: YouTubeV3PublicationServiceImpl? = null
    private var youTubeService: YouTubeAPIVersion3Service? = null
    private var orgDirectory: OrganizationDirectoryService? = null
    private var security: SecurityService? = null
    private var registry: ServiceRegistry? = null
    private var userDirectoryService: UserDirectoryService? = null
    private var workspace: Workspace? = null

    @Rule
    var testFolder = TemporaryFolder()

    private// maxFieldLength is optional so we skip
    val serviceProperties: Dictionary<*, *>
        @Throws(IOException::class)
        get() {
            val p = Properties()
            YouTubeUtils.put(p, YouTubeKey.credentialDatastore, "credentialDatastore")
            YouTubeUtils.put(p, YouTubeKey.scopes, "foo")
            val absolutePath = UnitTestUtils.getMockClientSecretsFile("clientId",
                    testFolder.newFile("client-secrets-youtube-v3.json")).absolutePath
            YouTubeUtils.put(p, YouTubeKey.clientSecretsV3, absolutePath)
            YouTubeUtils.put(p, YouTubeKey.dataStore, "dataStore")
            YouTubeUtils.put(p, YouTubeKey.keywords, "foo")
            YouTubeUtils.put(p, YouTubeKey.defaultPlaylist, "foo")
            YouTubeUtils.put(p, YouTubeKey.makeVideosPrivate, "true")
            return p
        }

    @Before
    @Throws(Exception::class)
    fun before() {
        youTubeService = createMock<YouTubeAPIVersion3Service>(YouTubeAPIVersion3Service::class.java)
        youTubeService!!.initialize(anyObject(ClientCredentials::class.java))
        expectLastCall<Any>()
        orgDirectory = createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        security = createMock<SecurityService>(SecurityService::class.java)
        registry = createMock<ServiceRegistry>(ServiceRegistry::class.java)
        val hosts = LinkedList<HostRegistration>()
        val host = HostRegistrationInMemory("localhost", "localhost", 1.0f, 1, 1024L)
        hosts.add(host)
        expect(registry!!.hostRegistrations).andReturn(hosts).anyTimes()
        userDirectoryService = createMock<UserDirectoryService>(UserDirectoryService::class.java)
        workspace = createMock<Workspace>(Workspace::class.java)
        //
        service = YouTubeV3PublicationServiceImpl(youTubeService)
        service!!.organizationDirectoryService = orgDirectory
        service!!.securityService = security
        service!!.serviceRegistry = registry
        service!!.userDirectoryService = userDirectoryService
        service!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testPublishNewPlaylist() {
        val baseDir = File(this.javaClass.getResource("/mediapackage").toURI())
        val xml = FileUtils.readFileToString(File(baseDir, "manifest.xml"))
        val mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(xml)
        //
        expect(youTubeService!!.getMyPlaylistByTitle(mediaPackage.title)).andReturn(null).once()
        expect(youTubeService!!.createPlaylist(mediaPackage.seriesTitle, null, mediaPackage.series))
                .andReturn(Playlist()).once()
        expect(youTubeService!!.addVideoToMyChannel(anyObject(VideoUpload::class.java))).andReturn(Video()).once()
        expect(youTubeService!!.addPlaylistItem(anyObject(String::class.java), anyObject(String::class.java)))
                .andReturn(PlaylistItem()).once()

        expect<Job>(registry!!.createJob(anyObject(String::class.java), anyObject(String::class.java), anyObject<List>(List<*>::class.java),
                anyObject(Float::class.java))).andReturn(JobImpl()).once()
        replay(youTubeService, orgDirectory, security, registry, userDirectoryService, workspace)
        service!!.updated(serviceProperties)
        service!!.publish(mediaPackage, mediaPackage.tracks[0])
    }

}
