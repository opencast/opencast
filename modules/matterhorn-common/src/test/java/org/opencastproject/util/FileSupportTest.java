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

package org.opencastproject.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.FileSupport.deleteHierarchyIfEmpty;
import static org.opencastproject.util.FileSupport.isParent;
import static org.opencastproject.util.PathSupport.path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Test class for {@link FileSupport}
 */
public class FileSupportTest {
  private File fileToLink;
  private File linkLocation;
  private File fileSupportTestsDirectory;
  private File fileSupportTestsDestinationDirectory;

  @Before
  public void setUp() throws IOException {
    fileSupportTestsDirectory = new File(System.getProperty("java.io.tmpdir"), "fileSupportTestsDirectory");
    fileSupportTestsDestinationDirectory = new File(System.getProperty("java.io.tmpdir"),
            "fileSupportTestsDestinationDirectory");
    fileToLink = new File(fileSupportTestsDirectory.getAbsolutePath(), "file-to-link");
    linkLocation = new File(fileSupportTestsDirectory.getAbsolutePath(), "link-location");
    // Create test directory
    FileUtils.forceMkdir(fileSupportTestsDirectory);
    assertTrue("Can't read from test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canRead());
    assertTrue("Can't write to test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canWrite());
    // Create file that we could link.
    FileUtils.touch(fileToLink);
    assertTrue("Can't read from file directory " + fileToLink.getAbsolutePath(), fileToLink.canRead());
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(fileSupportTestsDirectory);
    FileUtils.deleteQuietly(fileSupportTestsDestinationDirectory);
    fileToLink = null;
    linkLocation = null;
    fileSupportTestsDirectory = null;
    fileSupportTestsDestinationDirectory = null;
  }

  @Test
  public void supportsLinkingReturnsTrueOnAppropriateFile() {
    assertTrue(FileSupport.supportsLinking(fileToLink, linkLocation));
  }

  @Test(expected = IllegalArgumentException.class)
  public void supportsLinkingReturnsFalseOnMissingFile() {
    FileSupport.supportsLinking(linkLocation, fileToLink);
  }

  @Test
  public void linkTestWithoutForce() throws IOException {
    assertNotNull(FileSupport.link(fileToLink, linkLocation, false));
    assertTrue(linkLocation.exists());
  }

  @Test
  public void linkTestWithForce() throws IOException {
    assertNotNull(FileSupport.link(fileToLink, linkLocation, true));
    assertTrue(linkLocation.exists());
  }

  @Test(expected = IOException.class)
  public void missingLinkTestFailsWithoutForce() throws Exception {
    FileSupport.link(linkLocation, fileToLink, false);
  }

  @Test(expected = IOException.class)
  public void missingLinkTestFailsWithForce() throws Exception {
    FileSupport.link(linkLocation, fileToLink, true);
  }

  @Test
  public void testIsParent() throws Exception {
    final File a = new File(path("one", "two", "three"));
    final File b = new File(path("one", "two"));
    final File c = new File(path("one", "..", "one", ".", "two", "."));
    final File d = new File(path("two", "three"));
    assertTrue(isParent(b, a));
    assertTrue(isParent(c, a));
    assertFalse(isParent(d, a));
    assertFalse(isParent(d, c));
    assertFalse(isParent(a, b));
    assertFalse(isParent(a, a));
  }

  @Test
  public void testDeleteHierarchyIfEmpty() throws Exception {
    final File a = File.createTempFile("test", ".tmp");
    a.deleteOnExit();
    final File tmpDir = a.getParentFile();
    assertFalse(deleteHierarchyIfEmpty(tmpDir, a));
    assertFalse(deleteHierarchyIfEmpty(a, a));
    // three nested sub dirs
    final File subDir1 = createDirWithRandomNameIn(tmpDir);
    final File subDir2 = createDirWithRandomNameIn(subDir1);
    final File subDir3 = createDirWithRandomNameIn(subDir2);
    final File subDir1File = createFileWithRandomNameIn(subDir1);
    final File subDir3File = createFileWithRandomNameIn(subDir3);
    //
    assertFalse("file in sub dir 3 prevents deletion", deleteHierarchyIfEmpty(subDir1, subDir3));
    assertTrue(subDir3.exists());
    assertTrue(subDir3File.exists());
    assertTrue(subDir3File.delete());
    assertTrue("sub dir 3 and sub dir 2 are empty", deleteHierarchyIfEmpty(subDir1, subDir3));
    assertFalse(subDir3.exists());
    assertFalse(subDir2.exists());
    assertTrue("sub dir 1 has not been deleted", subDir1.exists());
    assertTrue(subDir2.mkdirs());
    assertTrue(subDir3.mkdirs());
    assertTrue(subDir1File.delete());
    assertTrue("all sub dirs are empty", deleteHierarchyIfEmpty(tmpDir, subDir3));
    assertFalse(subDir3.exists());
    assertFalse(subDir2.exists());
    assertFalse(subDir1.exists());
    assertTrue(tmpDir.exists());
  }

  private File createDirWithRandomNameIn(File parent) {
    final File dir = new File(parent, UUID.randomUUID().toString());
    dir.deleteOnExit();
    assertTrue(dir.mkdirs());
    return dir;
  }

  private File createFileWithRandomNameIn(File parent) throws IOException {
    final File file = new File(parent, UUID.randomUUID().toString());
    file.deleteOnExit();
    assertTrue(file.createNewFile());
    return file;
  }
}
