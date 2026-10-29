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

package org.opencastproject.assetmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.naming.ConfigurationException;

public class AssetStoreDirectoryTest {

  private static final String ROOT_DIR = AssetStoreDirectory.CONFIG_ROOT_DIR;
  private static final String STORAGE_DIR = AssetStoreDirectory.CONFIG_STORAGE_DIR;
  private static final String PREFIX_LENGTH = AssetStoreDirectory.CONFIG_PREFIX_LENGTH_SUFFIX;

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Test
  public void testPrefix() {
    assertEquals("", AssetStoreDirectory.prefix("3f6e3c1a-8d0b-4b0e-9a64-0c2f0e8f7d21", 0));
    assertEquals("3", AssetStoreDirectory.prefix("3f6e3c1a-8d0b-4b0e-9a64-0c2f0e8f7d21", 1));
    assertEquals("3f", AssetStoreDirectory.prefix("3f6e3c1a-8d0b-4b0e-9a64-0c2f0e8f7d21", 2));
    assertEquals("le", AssetStoreDirectory.prefix("Lecture-2026", 2));
    assertEquals("__", AssetStoreDirectory.prefix("..abc", 2));
    assertEquals("a_b", AssetStoreDirectory.prefix("a:b", 3));
    assertEquals("x_", AssetStoreDirectory.prefix("x", 2));
    assertEquals("___", AssetStoreDirectory.prefix("", 3));
    // Non-ASCII characters are always replaced, even if Character.toLowerCase() would map them to ASCII
    assertEquals("__a", AssetStoreDirectory.prefix("İKa", 3));
    assertEquals("_", AssetStoreDirectory.prefix("ä", 1));
  }

  @Test
  public void testPrefixLengthLimit() {
    new AssetStoreDirectory("/mnt/archive", AssetStoreDirectory.MAX_PREFIX_LENGTH);
    assertThrows(IllegalArgumentException.class,
        () -> new AssetStoreDirectory("/mnt/archive", AssetStoreDirectory.MAX_PREFIX_LENGTH + 1));
    assertThrows(IllegalArgumentException.class, () -> AssetStoreDirectory.fromConfig(bundleContext(Map.of(
        ROOT_DIR, "/mnt/archive",
        ROOT_DIR + PREFIX_LENGTH, "300"))));
  }

  @Test
  public void testMediaPackageDir() {
    final Path root = tmpFolder.getRoot().toPath().toAbsolutePath();
    assertEquals(root.resolve("org").resolve("Abc-123"),
        new AssetStoreDirectory(root.toString(), 0).mediaPackageDir("org", "Abc-123"));
    assertEquals(root.resolve("org").resolve("a").resolve("Abc-123"),
        new AssetStoreDirectory(root.toString(), 1).mediaPackageDir("org", "Abc-123"));
    assertEquals(root.resolve("org").resolve("abc_").resolve("Abc-123"),
        new AssetStoreDirectory(root.toString(), 4).mediaPackageDir("org", "Abc-123"));
  }

  @Test
  public void testDeletionLimit() {
    final Path root = tmpFolder.getRoot().toPath().toAbsolutePath();
    assertEquals(root.resolve("org"), new AssetStoreDirectory(root.toString(), 0).deletionLimit("org", "abc"));
    assertEquals(root.resolve("org").resolve("ab"),
        new AssetStoreDirectory(root.toString(), 2).deletionLimit("org", "abc"));
  }

  @Test
  public void testNormalizedPath() {
    final File root = tmpFolder.getRoot();
    assertEquals(root.getAbsolutePath(), new AssetStoreDirectory(root.getPath() + "/./sub/..", 0).getPath());
    assertThrows(IllegalArgumentException.class, () -> new AssetStoreDirectory(root.getPath(), -1));
  }

  @Test
  public void testFromConfigDefaults() {
    assertNull(AssetStoreDirectory.fromConfig(bundleContext(Map.of())));

    final List<AssetStoreDirectory> directories = AssetStoreDirectory.fromConfig(
        bundleContext(Map.of(STORAGE_DIR, "/srv/opencast")));
    assertEquals(1, directories.size());
    assertEquals(new File("/srv/opencast/archive").getAbsolutePath(), directories.get(0).getPath());
    assertEquals(0, directories.get(0).getPrefixLength());
  }

  @Test
  public void testFromConfigMultipleDirectories() {
    final List<AssetStoreDirectory> directories = AssetStoreDirectory.fromConfig(bundleContext(Map.of(
        STORAGE_DIR, "/srv/opencast",
        ROOT_DIR, "/mnt/archive ",
        ROOT_DIR + PREFIX_LENGTH, "1",
        ROOT_DIR + ".1", "/mnt/archive-1",
        ROOT_DIR + ".2", "/mnt/archive-2",
        ROOT_DIR + ".2" + PREFIX_LENGTH, " 2 ",
        // not read because .3 is missing
        ROOT_DIR + ".4", "/mnt/archive-4")));
    assertEquals(3, directories.size());
    assertEquals(new AssetStoreDirectory("/mnt/archive", 1), directories.get(0));
    assertEquals(new AssetStoreDirectory("/mnt/archive-1", 0), directories.get(1));
    assertEquals(new AssetStoreDirectory("/mnt/archive-2", 2), directories.get(2));
  }

  @Test
  public void testFromConfigInvalidPrefixLength() {
    assertThrows(IllegalArgumentException.class, () -> AssetStoreDirectory.fromConfig(bundleContext(Map.of(
        ROOT_DIR, "/mnt/archive",
        ROOT_DIR + PREFIX_LENGTH, "two"))));
    assertThrows(IllegalArgumentException.class, () -> AssetStoreDirectory.fromConfig(bundleContext(Map.of(
        ROOT_DIR, "/mnt/archive",
        ROOT_DIR + ".1", "/mnt/archive-1",
        ROOT_DIR + ".1" + PREFIX_LENGTH, "-1"))));
  }

  @Test
  public void testMarkerIsWrittenForEmptyDirectory() throws Exception {
    final File root = tmpFolder.newFolder();
    final AssetStoreDirectory directory = new AssetStoreDirectory(root.getPath(), 2);
    assertEquals(Optional.empty(), directory.readMarker());

    directory.verifyOrWriteMarker();
    assertTrue(new File(root, AssetStoreDirectory.MARKER_FILE).isFile());
    assertEquals(Optional.of(2), directory.readMarker());
    // No temporary file is left behind
    assertEquals(1, root.list().length);

    // Verifying again with the same configuration is fine
    directory.verifyOrWriteMarker();
  }

  @Test
  public void testMarkerIgnoresMountPointDirectories() throws Exception {
    final File root = tmpFolder.newFolder();
    assertTrue(new File(root, "lost+found").mkdir());
    assertTrue(new File(root, ".snapshot").mkdir());
    assertTrue(new File(root, "some-file").createNewFile());

    new AssetStoreDirectory(root.getPath(), 2).verifyOrWriteMarker();
    assertEquals(Optional.of(2), new AssetStoreDirectory(root.getPath(), 2).readMarker());
  }

  @Test
  public void testMarkerMismatch() throws Exception {
    final File root = tmpFolder.newFolder();
    new AssetStoreDirectory(root.getPath(), 2).verifyOrWriteMarker();

    assertThrows(ConfigurationException.class, () -> new AssetStoreDirectory(root.getPath(), 3).verifyOrWriteMarker());
    assertThrows(ConfigurationException.class, () -> new AssetStoreDirectory(root.getPath(), 0).verifyOrWriteMarker());
  }

  @Test
  public void testMarkerForLegacyData() throws Exception {
    final File root = tmpFolder.newFolder();
    assertTrue(new File(root, "mh_default_org/some-media-package/0").mkdirs());

    // Existing data without a marker file is in the flat layout
    assertThrows(ConfigurationException.class, () -> new AssetStoreDirectory(root.getPath(), 2).verifyOrWriteMarker());
    assertFalse(new File(root, AssetStoreDirectory.MARKER_FILE).exists());

    new AssetStoreDirectory(root.getPath(), 0).verifyOrWriteMarker();
    assertEquals(Optional.of(0), new AssetStoreDirectory(root.getPath(), 0).readMarker());
  }

  @Test
  public void testInvalidMarker() throws Exception {
    final File root = tmpFolder.newFolder();
    final AssetStoreDirectory directory = new AssetStoreDirectory(root.getPath(), 0);

    Files.writeString(new File(root, AssetStoreDirectory.MARKER_FILE).toPath(), "");
    assertThrows(ConfigurationException.class, directory::readMarker);
    assertThrows(ConfigurationException.class, directory::verifyOrWriteMarker);

    Files.writeString(new File(root, AssetStoreDirectory.MARKER_FILE).toPath(), "prefix.length=many");
    assertThrows(ConfigurationException.class, directory::readMarker);
  }

  private static BundleContext bundleContext(Map<String, String> properties) {
    final BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(EasyMock.anyString()))
        .andAnswer(() -> properties.get((String) EasyMock.getCurrentArguments()[0])).anyTimes();
    EasyMock.replay(bundleContext);
    return bundleContext;
  }
}
