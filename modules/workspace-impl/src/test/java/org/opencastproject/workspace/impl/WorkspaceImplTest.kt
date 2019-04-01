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

package org.opencastproject.workspace.impl

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClient.RequestRunner
import org.opencastproject.security.util.StandAloneTrustedHttpClientImpl
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Option
import org.opencastproject.workingfilerepository.api.WorkingFileRepository

import com.entwinemedia.fn.Prelude

import org.apache.commons.io.FileUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.HttpUriRequest
import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

import javax.servlet.http.HttpServletResponse

class WorkspaceImplTest {

    private var workspace: WorkspaceImpl? = null

    @Rule
    var testFolder = TemporaryFolder()

    @Before
    fun setUp() {
        workspace = WorkspaceImpl(workspaceRoot, false)
        workspace!!.activate(null)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        workspace!!.deactivate()
        FileUtils.deleteDirectory(File(workspaceRoot))
        FileUtils.deleteDirectory(File(repoRoot))
    }

    @Test
    @Throws(Exception::class)
    fun testLongFilenames() {
        val repo = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost:8080/files")).anyTimes()
        EasyMock.replay(repo)
        workspace!!.setRepository(repo)

        val source = File(
                "target/test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/opencast_header.gif")
        val urlToSource = source.toURI().toURL()

        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization.id).andReturn("org1").anyTimes()
        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService, organization)
        workspace!!.setSecurityService(securityService)

        val httpClient = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        val entity = EasyMock.createNiceMock<HttpEntity>(HttpEntity::class.java)
        EasyMock.expect(entity.content).andReturn(FileInputStream(source))
        val statusLine = EasyMock.createNiceMock<StatusLine>(StatusLine::class.java)
        EasyMock.expect(statusLine.statusCode).andReturn(HttpServletResponse.SC_OK)
        val response = EasyMock.createNiceMock<HttpResponse>(HttpResponse::class.java)
        EasyMock.expect(response.entity).andReturn(entity)
        EasyMock.expect(response.statusLine).andReturn(statusLine).anyTimes()
        EasyMock.replay(response, entity, statusLine)
        EasyMock.expect(httpClient.execute(EasyMock.anyObject(HttpUriRequest::class.java))).andReturn(response)
        EasyMock.expect(httpClient.runner<Any>(EasyMock.anyObject(HttpUriRequest::class.java))).andAnswer {
            val req = EasyMock.getCurrentArguments()[0] as HttpUriRequest
            StandAloneTrustedHttpClientImpl.runner(httpClient, req)
        }
        EasyMock.replay(httpClient)
        workspace!!.setTrustedHttpClient(httpClient)
        Assert.assertTrue(urlToSource.toString().length > 255)
        try {
            Assert.assertNotNull(workspace!![urlToSource.toURI()])
        } catch (e: NotFoundException) {
            // This happens on some machines, so we catch and handle it.
        }

    }

    // Calls to put() should put the file into the working file repository, but not in the local cache if there's a valid
    // filesystem mapping present
    @Test
    @Throws(Exception::class)
    fun testPutCachingWithFilesystemMapping() {
        val repo = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(
                repo.getURI(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString()))
                .andReturn(
                        URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                                + "foo/bar/header.gif"))
        EasyMock.expect(
                repo.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                        EasyMock.anyObject(InputStream::class.java))).andReturn(
                URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                        + "foo/bar/header.gif"))
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost:8080/files")).anyTimes()
        EasyMock.replay(repo)

        workspace!!.setRepository(repo)

        // Put a stream into the workspace (and hence, the repository)
        javaClass.getResourceAsStream("/opencast_header.gif").use { `in` ->
            Assert.assertNotNull(`in`)
            workspace!!.put("foo", "bar", "header.gif", `in`)
        }

        // Ensure that the file was put into the working file repository
        EasyMock.verify(repo)

        // Ensure that the file was not cached in the workspace (since there is a configured filesystem mapping)
        val file = File(workspaceRoot, "http___localhost_8080_files_foo_bar_header.gif")
        Assert.assertFalse(file.exists())
    }

    // Calls to put() should put the file into the working file repository and the local cache if there is no valid
    // filesystem mapping present
    @Test
    @Throws(Exception::class)
    fun testPutCachingWithoutFilesystemMapping() {
        // First, mock up the collaborating working file repository
        val repo = EasyMock.createMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(
                repo.getURI(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString()))
                .andReturn(
                        URI(UrlSupport.concat("http://localhost:8080", WorkingFileRepository.URI_PREFIX,
                                WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif")))
        EasyMock.expect(
                repo.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                        EasyMock.anyObject(InputStream::class.java))).andReturn(
                URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                        + "foo/bar/header.gif"))
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost:8080/files")).anyTimes()
        EasyMock.replay(repo)
        workspace!!.setRepository(repo)

        // Put a stream into the workspace (and hence, the repository)
        javaClass.getResourceAsStream("/opencast_header.gif").use { `in` ->
            Assert.assertNotNull(`in`)
            workspace!!.put("foo", "bar", "header.gif", `in`)
        }

        // Ensure that the file was put into the working file repository
        EasyMock.verify(repo)

        // Ensure that the file was cached in the workspace (since there is no configured filesystem mapping)
        val file = File(PathSupport.concat(arrayOf(workspaceRoot, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif")))
        Assert.assertTrue(file.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testGetWorkspaceFileWithOutPort() {
        val repo = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost/files")).anyTimes()
        EasyMock.replay(repo)
        workspace!!.setRepository(repo)

        var workspaceFile = workspace!!.toWorkspaceFile(URI("http://foo.com/myaccount/videos/bar.mov"))
        var expected = File(PathSupport.concat(arrayOf(workspaceRoot, "http_foo.com", "myaccount", "videos", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

        workspaceFile = workspace!!.toWorkspaceFile(URI("http://foo.com:8080/myaccount/videos/bar.mov"))
        expected = File(PathSupport.concat(arrayOf(workspaceRoot, "http_foo.com_8080", "myaccount", "videos", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

        workspaceFile = workspace!!.toWorkspaceFile(URI("http://localhost/files/collection/c1/bar.mov"))
        expected = File(PathSupport.concat(arrayOf(workspaceRoot, "collection", "c1", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

    }

    @Test
    @Throws(Exception::class)
    fun testGetWorkspaceFileWithPort() {
        val repo = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost:8080/files")).anyTimes()
        EasyMock.replay(repo)
        workspace!!.setRepository(repo)

        var workspaceFile = workspace!!.toWorkspaceFile(URI("http://foo.com/myaccount/videos/bar.mov"))
        var expected = File(PathSupport.concat(arrayOf(workspaceRoot, "http_foo.com", "myaccount", "videos", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

        workspaceFile = workspace!!.toWorkspaceFile(URI("http://foo.com:8080/myaccount/videos/bar.mov"))
        expected = File(PathSupport.concat(arrayOf(workspaceRoot, "http_foo.com_8080", "myaccount", "videos", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

        workspaceFile = workspace!!.toWorkspaceFile(URI("http://localhost:8080/files/collection/c1/bar.mov"))
        expected = File(PathSupport.concat(arrayOf(workspaceRoot, "collection", "c1", "bar.mov")))
        Assert.assertEquals(expected.absolutePath, workspaceFile.absolutePath)

    }

    @Test
    @Throws(Exception::class)
    fun testGetNoFilename() {
        val expectedFile = testFolder.newFile("test.txt")
        FileUtils.write(expectedFile, "asdf", StandardCharsets.UTF_8)
        expectedFile.deleteOnExit()

        val repo = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(repo.baseUri).andReturn(URI("http://localhost:8080/files")).anyTimes()
        EasyMock.replay(repo)
        workspace!!.setRepository(repo)

        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization.id).andReturn("org1").anyTimes()
        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService, organization)
        workspace!!.setSecurityService(securityService)

        val requestRunner = { f ->
            val right = Either.right(Option.some(expectedFile))
            Either.right(right)
        }
        val trustedHttpClient = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        EasyMock.expect(trustedHttpClient.runner<Either<String, Option<File>>>(EasyMock.anyObject(HttpUriRequest::class.java)))
                .andReturn(requestRunner).anyTimes()
        EasyMock.replay(trustedHttpClient)
        workspace!!.setTrustedHttpClient(trustedHttpClient)

        val resultingFile = workspace!![URI.create("http://foo.com/myaccount/videos/")]
        Assert.assertEquals(expectedFile, resultingFile)
    }

    @Test
    @Throws(Exception::class)
    fun testCleanup() {
        workspace!!.cleanup(-1)
        Assert.assertEquals(0L, workspace!!.usedSpace.get())

        val file = File(PathSupport.concat(arrayOf(workspaceRoot, "test", "c1", "bar.mov")))
        FileUtils.write(file, "asdf", StandardCharsets.UTF_8)
        file.deleteOnExit()

        Assert.assertEquals(4L, workspace!!.usedSpace.get())

        workspace!!.cleanup(0)
        Assert.assertEquals(0L, workspace!!.usedSpace.get())

        FileUtils.write(file, "asdf", StandardCharsets.UTF_8)
        Assert.assertEquals(4L, workspace!!.usedSpace.get())

        workspace!!.cleanup(100)
        Assert.assertEquals(4L, workspace!!.usedSpace.get())

        Prelude.sleep(1100L)

        workspace!!.cleanup(1)
        Assert.assertEquals(0L, workspace!!.usedSpace.get())
    }

    companion object {

        private val workspaceRoot = ("." + File.separator + "target" + File.separator
                + "junit-workspace-rootdir")
        private val repoRoot = "." + File.separator + "target" + File.separator + "junit-repo-rootdir"
    }

}
