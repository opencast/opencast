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
package org.opencastproject.assetmanager.storage.impl.fs;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.PathSupport;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class FileSystemAssetStoreTest {
  private static final String XML_EXTENSTION = ".xml";

  private static final String TEST_ROOT_DIR_NAME = "test-archive";

  private static final String ORG_ID = "sampleOrgId";

  private static final String MP_ID = "sampleMediaPackageId";

  private static final String MP_ELEM_ID = "sampleMediaPackageElementId";

  private static final String FILE_NAME = "dublincore.xml";

  private static final VersionImpl VERSION_1 = new VersionImpl(1);

  private static final VersionImpl VERSION_2 = new VersionImpl(2);

  private File tmpRoot;

  private FileSystemAssetStore repo = new FileSystemAssetStore();

  private File sampleElemDir;

  @Before
  public void setUp() throws Exception {

    HttpEntity entity = EasyMock.createNiceMock(HttpEntity.class);
    EasyMock.expect(entity.getContent()).andReturn(getClass().getClassLoader().getResourceAsStream(FILE_NAME))
    .anyTimes();
    EasyMock.replay(entity);

    HttpResponse response = EasyMock.createNiceMock(HttpResponse.class);
    EasyMock.expect(response.getEntity()).andReturn(entity).anyTimes();
    EasyMock.replay(response);

    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    tmpRoot = FileSupport.getTempDirectory(TEST_ROOT_DIR_NAME);

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(FileSystemAssetStore.CONFIG_ARCHIVE_ROOT_DIR)).andReturn(
            tmpRoot.getPath());
    EasyMock.replay(bundleContext);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);

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
    repo.put(storagePath, Source.mk(getClass().getClassLoader().getResource(FILE_NAME).toURI()));

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, new Long(1).toString() }));
    assertTrue(file.exists());
    assertTrue(file.listFiles().length == 1);

    InputStream original = null;
    FileInputStream fileInput = null;
    try {
      fileInput = new FileInputStream(file.listFiles()[0]);
      original = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
      byte[] bytesFromRepo = IOUtils.toByteArray(fileInput);
      byte[] bytesFromClasspath = IOUtils.toByteArray(original);
      assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(original);
      IOUtils.closeQuietly(fileInput);
    }
  }

  @Test
  public void testCopy() throws Exception {
    StoragePath from = new StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID);
    StoragePath to = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    assertTrue(repo.copy(from, to));

    File srcFile = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString(),
            MP_ELEM_ID + XML_EXTENSTION }));
    File copyFile = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_1.toString(),
            MP_ELEM_ID + XML_EXTENSTION }));
    assertTrue(srcFile.exists());
    assertTrue(copyFile.exists());

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
    assertFalse(repo.copy(from, to));
  }

  @Test
  public void testGet() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID);
    Opt<InputStream> option = repo.get(storagePath);
    assertTrue(option.isSome());

    InputStream original = null;
    InputStream repo = option.get();
    try {
      original = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
      byte[] bytesFromRepo = IOUtils.toByteArray(repo);
      byte[] bytesFromClasspath = IOUtils.toByteArray(original);
      assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(original);
      IOUtils.closeQuietly(repo);
    }
  }

  @Test
  public void testGetBad() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    Opt<InputStream> option = repo.get(storagePath);
    assertFalse(option.isSome());
  }

  @Test
  public void testDeleteWithVersion() throws Exception {
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_2);
    assertTrue(repo.delete(versionSelector));
    assertFalse(sampleElemDir.exists());

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString() }));
    assertFalse(file.exists());

    file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID }));
    assertTrue(file.exists());
  }

  @Test
  public void testDeleteNoneVersion() throws Exception {
    DeletionSelector noneVersionSelector = DeletionSelector.deleteAll(ORG_ID, MP_ID);
    assertTrue(repo.delete(noneVersionSelector));
    assertFalse(sampleElemDir.exists());

    File file = new File(PathSupport.concat(new String[] { tmpRoot.toString(), ORG_ID, MP_ID }));
    assertFalse(file.exists());
  }

  @Test
  public void testDeleteWrongVersion() throws Exception {
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_1);
    repo.delete(versionSelector);
    assertTrue(sampleElemDir.exists());
  }
}
