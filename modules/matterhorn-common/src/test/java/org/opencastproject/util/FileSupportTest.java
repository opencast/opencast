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
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.FileSupport.deleteHierarchyIfEmpty;
import static org.opencastproject.util.FileSupport.isParent;
import static org.opencastproject.util.PathSupport.path;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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
    Assert.assertTrue("Can't read from test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canRead());
    Assert.assertTrue("Can't write to test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canWrite());
    // Create file that we could link.
    FileUtils.touch(fileToLink);
    Assert.assertTrue("Can't read from file directory " + fileToLink.getAbsolutePath(), fileToLink.canRead());
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
    Assert.assertTrue(FileSupport.supportsLinking(fileToLink, linkLocation));
  }

  @Test
  public void supportsLinkingReturnsFalseOnMissingFile() {
    try {
      FileSupport.supportsLinking(linkLocation, fileToLink);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // If exception is thrown then this test has succeeded.
    }
  }

  @Test
  public void linkContentTestWithoutForce() throws IOException {
    FileSupport.linkContent(fileSupportTestsDirectory, fileSupportTestsDestinationDirectory, false);
  }

  @Test
  public void linkContentTestWithForce() throws IOException {
    FileUtils.forceMkdir(fileSupportTestsDestinationDirectory);
    FileSupport.linkContent(fileSupportTestsDirectory, fileSupportTestsDestinationDirectory, true);
  }

  @Test
  public void linkTestWithoutForce() throws IOException {
    Assert.assertNotNull(FileSupport.link(fileToLink, linkLocation, false));
  }

  @Test
  public void linkTestWithForce() throws IOException {
    Assert.assertNotNull(FileSupport.link(fileToLink, linkLocation, true));
  }

  @Test
  public void missingLinkTestFailsWithoutForce() {
    try {
      Assert.assertNull(FileSupport.link(linkLocation, fileToLink, false));
      Assert.fail();
    } catch (IOException e) {
      // Test should have IOException.
    }
  }

  @Test
  public void missingLinkTestFailsWithForce() {
    try {
      Assert.assertNull(FileSupport.link(linkLocation, fileToLink, true));
      Assert.fail();
    } catch (IOException e) {
      // Test should have IOException.
    }
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
