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

package org.opencastproject.workspace.impl;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClient.RequestRunner;
import org.opencastproject.security.util.StandAloneTrustedHttpClientImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import com.entwinemedia.fn.Prelude;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

public class WorkspaceImplTest {

  private WorkspaceImpl workspace;

  private static final String workspaceRoot = "." + File.separator + "target" + File.separator
          + "junit-workspace-rootdir";
  private static final String repoRoot = "." + File.separator + "target" + File.separator + "junit-repo-rootdir";

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    workspace = new WorkspaceImpl(workspaceRoot, false);
    workspace.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    workspace.deactivate();
    FileUtils.deleteDirectory(new File(workspaceRoot));
    FileUtils.deleteDirectory(new File(repoRoot));
  }

  @Test
  public void testLongFilenames() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    File source = new File(
            "target/test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/opencast_header.gif");
    URL urlToSource = source.toURI().toURL();

    final TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    HttpEntity entity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(entity.getContent()).andReturn(new FileInputStream(source));
    StatusLine statusLine = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(HttpServletResponse.SC_OK);
    HttpResponse response = EasyMock.createNiceMock(HttpResponse.class);
    EasyMock.expect(response.getEntity()).andReturn(entity);
    EasyMock.expect(response.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.replay(response, entity, statusLine);
    EasyMock.expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andReturn(response);
    EasyMock.expect(httpClient.runner((HttpUriRequest) EasyMock.anyObject())).andAnswer(
            new IAnswer<TrustedHttpClient.RequestRunner<Object>>() {
              @Override
              public RequestRunner<Object> answer() throws Throwable {
                HttpUriRequest req = (HttpUriRequest) EasyMock.getCurrentArguments()[0];
                return StandAloneTrustedHttpClientImpl.runner(httpClient, req);
              }
            });
    EasyMock.replay(httpClient);
    workspace.setTrustedHttpClient(httpClient);
    Assert.assertTrue(urlToSource.toString().length() > 255);
    try {
      Assert.assertNotNull(workspace.get(urlToSource.toURI()));
    } catch (NotFoundException e) {
      // This happens on some machines, so we catch and handle it.
    }
  }

  // Calls to put() should put the file into the working file repository, but not in the local cache if there's a valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithFilesystemMapping() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(
            repo.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(
                    new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                            + "foo/bar/header.gif"));
    EasyMock.expect(
            repo.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(
            new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                    + "foo/bar/header.gif"));
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);

    workspace.setRepository(repo);

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/opencast_header.gif");
      Assert.assertNotNull(in);
      workspace.put("foo", "bar", "header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was not cached in the workspace (since there is a configured filesystem mapping)
    File file = new File(workspaceRoot, "http___localhost_8080_files_foo_bar_header.gif");
    Assert.assertFalse(file.exists());
  }

  // Calls to put() should put the file into the working file repository and the local cache if there is no valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithoutFilesystemMapping() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(
            repo.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(
                    new URI(UrlSupport.concat(new String[] { "http://localhost:8080", WorkingFileRepository.URI_PREFIX,
                            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif" })));
    EasyMock.expect(
            repo.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(
            new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                    + "foo/bar/header.gif"));
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/opencast_header.gif");
      Assert.assertNotNull(in);
      workspace.put("foo", "bar", "header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was cached in the workspace (since there is no configured filesystem mapping)
    File file = new File(PathSupport.concat(new String[] { workspaceRoot,
            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif" }));
    Assert.assertTrue(file.exists());
  }

  @Test
  public void testGetWorkspaceFileWithOutPort() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    File workspaceFile = workspace.toWorkspaceFile(new URI("http://foo.com/myaccount/videos/bar.mov"));
    File expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.toWorkspaceFile(new URI("http://foo.com:8080/myaccount/videos/bar.mov"));
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com_8080", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.toWorkspaceFile(new URI("http://localhost/files/collection/c1/bar.mov"));
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "collection", "c1", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

  }

  @Test
  public void testGetWorkspaceFileWithPort() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    File workspaceFile = workspace.toWorkspaceFile(new URI("http://foo.com/myaccount/videos/bar.mov"));
    File expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.toWorkspaceFile(new URI("http://foo.com:8080/myaccount/videos/bar.mov"));
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com_8080", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.toWorkspaceFile(new URI("http://localhost:8080/files/collection/c1/bar.mov"));
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "collection", "c1", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

  }

  @Test
  public void testGetNoFilename() throws Exception {
    final File expectedFile = testFolder.newFile("test.txt");
    FileUtils.write(expectedFile, "asdf");
    expectedFile.deleteOnExit();

    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    RequestRunner<Either<String, Option<File>>> requestRunner = new TrustedHttpClient.RequestRunner<Either<String, Option<File>>>() {
      @Override
      public Either<Exception, Either<String, Option<File>>> run(Function<HttpResponse, Either<String, Option<File>>> f) {
        Either<String, Option<File>> right = Either.right(Option.some(expectedFile));
        return Either.right(right);
      }
    };
    TrustedHttpClient trustedHttpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(trustedHttpClient.<Either<String, Option<File>>> runner(EasyMock.anyObject(HttpUriRequest.class)))
            .andReturn(requestRunner).anyTimes();
    EasyMock.replay(trustedHttpClient);
    workspace.setTrustedHttpClient(trustedHttpClient);

    File resultingFile = workspace.get(URI.create("http://foo.com/myaccount/videos/"));
    Assert.assertEquals(expectedFile, resultingFile);
  }

  @Test
  public void testCleanup() throws Exception {
    workspace.cleanup(-1);
    Assert.assertEquals(0L, workspace.getUsedSpace().get().longValue());

    File file = new File(PathSupport.concat(new String[] { workspaceRoot, "test", "c1", "bar.mov" }));
    FileUtils.write(file, "asdf");
    file.deleteOnExit();

    Assert.assertEquals(4L, workspace.getUsedSpace().get().longValue());

    workspace.cleanup(0);
    Assert.assertEquals(0L, workspace.getUsedSpace().get().longValue());

    FileUtils.write(file, "asdf");
    Assert.assertEquals(4L, workspace.getUsedSpace().get().longValue());

    workspace.cleanup(100);
    Assert.assertEquals(4L, workspace.getUsedSpace().get().longValue());

    Prelude.sleep(1100L);

    workspace.cleanup(1);
    Assert.assertEquals(0L, workspace.getUsedSpace().get().longValue());
  }

}
