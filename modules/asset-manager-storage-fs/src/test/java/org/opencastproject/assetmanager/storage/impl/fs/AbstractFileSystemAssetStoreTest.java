/*
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
package org.opencastproject.assetmanager.storage.impl.fs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.util.AssetStoreDirectory;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class AbstractFileSystemAssetStoreTest {
  private static final String XML_EXTENSTION = ".xml";

  private static final String ORG_ID = "sampleOrgId";

  private static final String MP_ID = "sampleMediaPackageId";

  private static final String PREFIXED_MP_ID = "prefixedMediaPackageId";

  private static final int PREFIX_LENGTH = 2;

  private static final String MP_ELEM_ID = "sampleMediaPackageElementId";

  private static final String FILE_NAME = "dublincore.xml";

  private static final VersionImpl VERSION_1 = new VersionImpl(1);

  private static final VersionImpl VERSION_2 = new VersionImpl(2);

  private File tmpRoot;
  private File tmpRoot2;

  private OsgiFileSystemAssetStore repo;

  private File sampleElemDir;

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder tmpFolder2 = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    final File asset = IoSupport.classPathResourceAsFile("/" + FILE_NAME).get();
    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(asset);
    EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andAnswer(() -> {
      File tmp = tmpFolder.newFile();
      FileUtils.copyFile(asset, tmp);
      return tmp;
    }).anyTimes();
    EasyMock.replay(workspace);

    tmpRoot = tmpFolder.newFolder();
    tmpRoot2 = tmpFolder2.newFolder();

    repo = new OsgiFileSystemAssetStore() {
      @Override protected Workspace getWorkspace() {
        return workspace;
      }
    };
    // The first root uses the flat layout, the second one prefix directories
    setRootDirectories(new AssetStoreDirectory(tmpRoot.getAbsolutePath(), 0),
        new AssetStoreDirectory(tmpRoot2.getAbsolutePath(), PREFIX_LENGTH));

    sampleElemDir = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString()).toFile();
    FileUtils.forceMkdir(sampleElemDir);

    FileUtils.copyFile(asset, new File(sampleElemDir, MP_ELEM_ID + XML_EXTENSTION));
  }

  private void setRootDirectories(AssetStoreDirectory... directories) throws Exception {
    Field rootDirectories = OsgiFileSystemAssetStore.class.getDeclaredField("rootDirectories");
    rootDirectories.setAccessible(true);
    rootDirectories.set(repo, new ArrayList<>(Arrays.asList(directories)));
    repo.setupCache();
  }

  /** The directory of a media package in the prefixed root */
  private File prefixedMpDir(String mpId) {
    return Paths.get(tmpRoot2.toString(), ORG_ID, AssetStoreDirectory.prefix(mpId, PREFIX_LENGTH), mpId).toFile();
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(tmpRoot);
  }

  @Test
  public void testPutIntoPrefixedRoot() throws Exception {
    // New media packages go to the root with the most usable space. With only the prefixed root configured, the
    // choice does not depend on the free space of the temporary folders.
    setRootDirectories(new AssetStoreDirectory(tmpRoot2.getAbsolutePath(), PREFIX_LENGTH));

    StoragePath storagePath = new StoragePath(ORG_ID, PREFIXED_MP_ID, VERSION_1, MP_ELEM_ID);
    repo.put(storagePath, Source.mk(getClass().getClassLoader().getResource(FILE_NAME).toURI()));

    File versionDir = new File(prefixedMpDir(PREFIXED_MP_ID), VERSION_1.toString());
    assertEquals("pr", prefixedMpDir(PREFIXED_MP_ID).getParentFile().getName());
    assertTrue(versionDir + " should be a directory", versionDir.isDirectory());
    assertEquals(1, versionDir.listFiles().length);
    assertTrue(repo.contains(storagePath));
    assertTrue(repo.get(storagePath).isPresent());

    // Further versions are stored in the same root even if a flat root is searched first
    setRootDirectories(new AssetStoreDirectory(tmpRoot.getAbsolutePath(), 0),
        new AssetStoreDirectory(tmpRoot2.getAbsolutePath(), PREFIX_LENGTH));
    StoragePath version2 = new StoragePath(ORG_ID, PREFIXED_MP_ID, VERSION_2, MP_ELEM_ID);
    assertTrue(repo.copy(storagePath, version2));
    assertTrue(new File(prefixedMpDir(PREFIXED_MP_ID), VERSION_2.toString()).isDirectory());
    assertFalse(Paths.get(tmpRoot.toString(), ORG_ID, PREFIXED_MP_ID).toFile().exists());
  }

  @Test
  public void testDeleteFromPrefixedRoot() throws Exception {
    File versionDir = new File(prefixedMpDir(PREFIXED_MP_ID), VERSION_1.toString());
    FileUtils.forceMkdir(versionDir);
    FileUtils.copyFile(IoSupport.classPathResourceAsFile("/" + FILE_NAME).get(),
        new File(versionDir, MP_ELEM_ID + XML_EXTENSTION));
    StoragePath storagePath = new StoragePath(ORG_ID, PREFIXED_MP_ID, VERSION_1, MP_ELEM_ID);
    assertTrue(repo.contains(storagePath));

    assertTrue(repo.delete(DeletionSelector.delete(ORG_ID, PREFIXED_MP_ID, VERSION_1)));
    assertFalse(repo.contains(storagePath));
    assertFalse(prefixedMpDir(PREFIXED_MP_ID).exists());
    // The prefix directory is kept
    assertTrue(prefixedMpDir(PREFIXED_MP_ID).getParentFile().isDirectory());
  }

  @Test
  public void testUnknownMediaPackage() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, "unknown", VERSION_1, MP_ELEM_ID);
    assertFalse(repo.contains(storagePath));
    assertFalse(repo.get(storagePath).isPresent());
    assertFalse(repo.copy(storagePath, new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID)));
    assertFalse(repo.delete(DeletionSelector.deleteAll(ORG_ID, "unknown")));
  }

  @Test
  public void testPut() throws Exception {
    StoragePath storagePath = new StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID);
    repo.put(storagePath, Source.mk(getClass().getClassLoader().getResource(FILE_NAME).toURI()));

    File file = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID, VERSION_1.toString()).toFile();
    assertTrue(file + " should be a directory", file.isDirectory());
    assertTrue(file.listFiles().length == 1);

    InputStream original = null;
    FileInputStream fileInput = null;
    try {
      fileInput = new FileInputStream(file.listFiles()[0]);
      original = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
      byte[] bytesFromRepo = IOUtils.toByteArray(fileInput);
      byte[] bytesFromClasspath = IOUtils.toByteArray(original);
      assertArrayEquals(bytesFromClasspath, bytesFromRepo);
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

    File srcFile = Paths.get(
        tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString(), MP_ELEM_ID + XML_EXTENSTION
    ).toFile();
    File copyFile = Paths.get(
        tmpRoot.toString(), ORG_ID, MP_ID, VERSION_1.toString(), MP_ELEM_ID + XML_EXTENSTION
    ).toFile();
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
    Optional<InputStream> option = repo.get(storagePath);
    assertTrue(option.isPresent());

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
    Optional<InputStream> option = repo.get(storagePath);
    assertFalse(option.isPresent());
  }

  @Test
  public void testDeleteWithVersion() throws Exception {
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_2);
    assertTrue(repo.delete(versionSelector));
    assertFalse(sampleElemDir.exists());

    File file = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID, VERSION_2.toString()).toFile();
    assertFalse(file.exists());

    file = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID).toFile();
    assertFalse(file.exists());

    file = Paths.get(tmpRoot.toString(), ORG_ID).toFile();
    assertTrue(file.exists());
  }

  @Test
  public void testDeleteNoneVersion() throws Exception {
    DeletionSelector noneVersionSelector = DeletionSelector.deleteAll(ORG_ID, MP_ID);
    assertTrue(repo.delete(noneVersionSelector));
    assertFalse(sampleElemDir.exists());

    File file = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID).toFile();
    assertFalse(file.exists());
  }

  /**
   * The root directory has to be resolved before the deletion, not after. Deleting the media package directory removes
   * exactly what a repeated lookup would search for, so that lookup may legitimately return null and must not be used
   * unchecked.
   */
  @Test
  public void testDeleteNoneVersionWithStaleRootDirectoryCache() throws Exception {
    final OsgiFileSystemAssetStore store = new OsgiFileSystemAssetStore() {
      @Override protected Workspace getWorkspace() {
        return null;
      }

      @Override protected AssetStoreDirectory getRootDirectory(String orgId, String mpId) {
        final AssetStoreDirectory rootDirectory = super.getRootDirectory(orgId, mpId);
        // drop the cached entry so that every further lookup is resolved from disk again, just like it would be
        // after the cache entry expired
        onDeleteMediaPackage(orgId, mpId);
        return rootDirectory;
      }
    };
    Field rootDirectories = OsgiFileSystemAssetStore.class.getDeclaredField("rootDirectories");
    rootDirectories.setAccessible(true);
    rootDirectories.set(store, new ArrayList<>(Arrays.asList(
        new AssetStoreDirectory(tmpRoot.getAbsolutePath(), 0),
        new AssetStoreDirectory(tmpRoot2.getAbsolutePath(), PREFIX_LENGTH))));
    store.setupCache();

    assertTrue(store.delete(DeletionSelector.deleteAll(ORG_ID, MP_ID)));
    assertFalse(sampleElemDir.exists());

    File file = Paths.get(tmpRoot.toString(), ORG_ID, MP_ID).toFile();
    assertFalse(file.exists());
  }

  @Test
  public void testDeleteWrongVersion() throws Exception {
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_1);
    repo.delete(versionSelector);
    assertTrue(sampleElemDir.exists());
  }

  /**
   * A media package identifier that is the current or parent directory reference or the empty
   * string resolves the root directory lookup successfully (it collapses into an existing
   * parent directory). The deletion must still be rejected before any file is touched.
   */
  @Test
  public void testDeleteWithUnsafeMediaPackageIdIsRejected() throws Exception {
    for (String badId : new String[] { "", ".", ".." }) {
      try {
        repo.delete(DeletionSelector.delete(ORG_ID, badId, VERSION_2));
        Assert.fail("Expected the media package ID '" + badId + "' to be rejected");
      } catch (AssetStoreException e) {
        // intended
      }
    }
    assertTrue("The stored data must be intact", sampleElemDir.exists());
    assertEquals(1, sampleElemDir.listFiles().length);
  }

  /**
   * A media package identifier containing a path separator can address an existing directory
   * of another media package (here: version 2 of the sample media package, as if it was a
   * media package of its own). The deletion must be rejected.
   */
  @Test
  public void testDeleteWithSeparatorInMediaPackageIdIsRejected() throws Exception {
    for (String badId : new String[] { MP_ID + "/" + VERSION_2, MP_ID + "\\" + VERSION_2, "x/../" + MP_ID }) {
      try {
        repo.delete(DeletionSelector.deleteAll(ORG_ID, badId));
        Assert.fail("Expected the media package ID '" + badId + "' to be rejected");
      } catch (AssetStoreException e) {
        // intended
      }
    }
    assertTrue("The stored data must be intact", sampleElemDir.exists());
    assertEquals(1, sampleElemDir.listFiles().length);
  }

  /**
   * Organization, media package and element identifiers are joined into the storage path, so
   * each of them must be rejected if it is unsafe as a path component, both for writing and
   * for reading.
   */
  @Test
  public void testUnsafeStoragePathComponentsAreRejected() throws Exception {
    final Source source = Source.mk(getClass().getClassLoader().getResource(FILE_NAME).toURI());
    for (String bad : new String[] { "", ".", "..", "a/b", "a\\b", "../" + MP_ID }) {
      final StoragePath[] paths = {
          new StoragePath(bad, MP_ID, VERSION_2, MP_ELEM_ID),
          new StoragePath(ORG_ID, bad, VERSION_2, MP_ELEM_ID),
          new StoragePath(ORG_ID, MP_ID, VERSION_2, bad) };
      for (StoragePath path : paths) {
        try {
          repo.put(path, source);
          Assert.fail("Expected put to reject the storage path " + path);
        } catch (AssetStoreException e) {
          // intended
        }
        try {
          repo.get(path);
          Assert.fail("Expected get to reject the storage path " + path);
        } catch (AssetStoreException e) {
          // intended
        }
      }
    }
    assertEquals("Nothing must have been written next to the sample element", 1, sampleElemDir.listFiles().length);
    assertEquals("Nothing must have been written to the store root", 1, tmpRoot.listFiles().length);
  }
}
