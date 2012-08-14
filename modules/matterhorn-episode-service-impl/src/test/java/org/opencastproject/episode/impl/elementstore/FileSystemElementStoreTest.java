/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.episode.impl.elementstore;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.StoragePath;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.opencastproject.episode.impl.elementstore.Source.source;

public class FileSystemElementStoreTest {

  private static final String XML_EXTENSTION = ".xml";

  private static final String TEST_ROOT_DIR_NAME = "test-archive";

  private static final String ORG_ID = "sampleOrgId";

  private static final String MP_ID = "sampleMediaPackageId";

  private static final String MP_ELEM_ID = "sampleMediaPackageElementId";

  private static final String FILE_NAME = "dublincore.xml";

  private static final Version VERSION_1 = new Version(1);

  private static final Version VERSION_2 = new Version(2);

  private File tmpRoot;

  private FileSystemElementStore repo = new FileSystemElementStore();

  private File sampleElemDir;

  @Before
  public void setUp() throws Exception {
    HttpEntity entity = createNiceMock(HttpEntity.class);
    expect(entity.getContent()).andReturn(getClass().getClassLoader().getResourceAsStream(FILE_NAME)).anyTimes();
    replay(entity);

    HttpResponse response = createNiceMock(HttpResponse.class);
    expect(response.getEntity()).andReturn(entity).anyTimes();
    replay(response);

    TrustedHttpClient httpClient = createNiceMock(TrustedHttpClient.class);
    expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andReturn(response).anyTimes();
    replay(httpClient);

    tmpRoot = FileSupport.getTempDirectory(TEST_ROOT_DIR_NAME);

    BundleContext bundleContext = createNiceMock(BundleContext.class);
    expect(bundleContext.getProperty(FileSystemElementStore.CONFIG_ARCHIVE_ROOT_DIR)).andReturn(tmpRoot.getPath());
    replay(bundleContext);

    ComponentContext cc = createNiceMock(ComponentContext.class);
    expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    replay(cc);

    repo.setHttpClient(httpClient);
    repo.activate(cc);

    sampleElemDir = new File(
            PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString() }));
    FileUtils.forceMkdir(sampleElemDir);

    // Put an image file into the repository using the mediapackage / element storage
    URL resource = getClass().getClassLoader().getResource(FILE_NAME);
    FileUtils.copyURLToFile(resource, new File(sampleElemDir, MP_ELEM_ID + XML_EXTENSTION));
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(tmpRoot);
  }

  @Test
  public void testPut() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    repo.put(storagePath, source(getClass().getClassLoader().getResource(FILE_NAME).toURI()));

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, new Long(1).toString() }));
    Assert.assertTrue(file.exists());
    Assert.assertTrue(file.listFiles().length == 1);

    InputStream original = null;
    FileInputStream fileInput = null;
    try {
      fileInput = new FileInputStream(file.listFiles()[0]);
      original = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
      byte[] bytesFromRepo = IOUtils.toByteArray(fileInput);
      byte[] bytesFromClasspath = IOUtils.toByteArray(original);
      Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(original);
      IOUtils.closeQuietly(fileInput);
    }
  }

  @Test
  public void testCopy() throws Exception {
    StoragePath from = new StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID);
    StoragePath to = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    Assert.assertTrue(repo.copy(from, to));

    File srcFile = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString(),
            MP_ELEM_ID + XML_EXTENSTION }));
    File copyFile = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_1.toString(),
            MP_ELEM_ID + XML_EXTENSTION }));
    Assert.assertTrue(srcFile.exists());
    Assert.assertTrue(copyFile.exists());

    FileInputStream srcIn = null;
    FileInputStream copyIn = null;
    try {
      srcIn = new FileInputStream(srcFile);
      copyIn = new FileInputStream(copyFile);
      byte[] bytesOriginal = IOUtils.toByteArray(srcIn);
      byte[] bytesCopy = IOUtils.toByteArray(copyIn);
      Assert.assertEquals(bytesCopy.length, bytesOriginal.length);
    } finally {
      IOUtils.closeQuietly(srcIn);
      IOUtils.closeQuietly(copyIn);
    }
  }

  @Test
  public void testCopyBad() throws Exception {
    StoragePath from = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    StoragePath to = new StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID);
    Assert.assertFalse(repo.copy(from, to));
  }

  @Test
  public void testGet() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID);
    Option<InputStream> option = repo.get(storagePath);
    Assert.assertTrue(option.isSome());

    InputStream original = null;
    InputStream repo = option.get();
    try {
      original = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
      byte[] bytesFromRepo = IOUtils.toByteArray(repo);
      byte[] bytesFromClasspath = IOUtils.toByteArray(original);
      Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(original);
      IOUtils.closeQuietly(repo);
    }
  }

  @Test
  public void testGetBad() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    Option<InputStream> option = repo.get(storagePath);
    Assert.assertFalse(option.isSome());
  }

  @Test
  public void testDeleteWithVersion() throws Exception {
    DeletionSelector versionSelector = new DeletionSelector(ORG_ID, MP_ID, Option.some(VERSION_2));
    Assert.assertTrue(repo.delete(versionSelector));
    Assert.assertFalse(sampleElemDir.exists());

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString() }));
    Assert.assertFalse(file.exists());

    file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID }));
    Assert.assertTrue(file.exists());
  }

  @Test
  public void testDeleteNoneVersion() throws Exception {
    DeletionSelector noneVersionSelector = new DeletionSelector(ORG_ID, MP_ID, Option.<Version> none());
    Assert.assertTrue(repo.delete(noneVersionSelector));
    Assert.assertFalse(sampleElemDir.exists());

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID }));
    Assert.assertFalse(file.exists());
  }

  @Test
  public void testDeleteWrongVersion() throws Exception {
    DeletionSelector versionSelector = new DeletionSelector(ORG_ID, MP_ID, Option.some(VERSION_1));
    repo.delete(versionSelector);
    Assert.assertTrue(sampleElemDir.exists());
  }
}
