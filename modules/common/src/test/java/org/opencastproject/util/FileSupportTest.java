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


package org.opencastproject.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencastproject.util.FileSupport.deleteHierarchyIfEmpty;
import static org.opencastproject.util.FileSupport.isParent;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Test class for {@link FileSupport}
 */
public class FileSupportTest {
  private File fileToLink;
  private File linkLocation;
  private File fileSupportTestsDirectory;
  private File fileSupportTestsDestinationDirectory;

  @TempDir
  private File testFolder;

  @BeforeEach
  public void setUp() throws IOException {
    fileSupportTestsDirectory = Files.createTempDirectory(testFolder.toPath(), "junit").toFile();
    fileSupportTestsDestinationDirectory = Files.createTempDirectory(testFolder.toPath(), "junit").toFile();
    fileToLink = new File(fileSupportTestsDirectory.getAbsolutePath(), "file-to-link");
    linkLocation = new File(fileSupportTestsDirectory.getAbsolutePath(), "link-location");
    // Create test directory
    FileUtils.forceMkdir(fileSupportTestsDirectory);
    assertTrue(fileSupportTestsDirectory.canRead(),
            "Can't read from test directory " + fileSupportTestsDirectory.getAbsolutePath());
    assertTrue(fileSupportTestsDirectory.canWrite(),
            "Can't write to test directory " + fileSupportTestsDirectory.getAbsolutePath());
    // Create file that we could link.
    FileUtils.touch(fileToLink);
    assertTrue(fileToLink.canRead(), "Can't read from file directory " + fileToLink.getAbsolutePath());
  }

  @AfterEach
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

  @Test
  public void supportsLinkingReturnsFalseOnMissingFile() {
    assertThrows(IllegalArgumentException.class, () ->
        FileSupport.supportsLinking(linkLocation, fileToLink));
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

  @Test
  public void missingLinkTestFailsWithoutForce() throws Exception {
    assertThrows(IOException.class, () ->
        FileSupport.link(linkLocation, fileToLink, false));
  }

  @Test
  public void missingLinkTestFailsWithForce() throws Exception {
    assertThrows(IOException.class, () ->
        FileSupport.link(linkLocation, fileToLink, true));
  }

  @Test
  public void testIsParent() throws Exception {
    final File a = Paths.get("one", "two", "three").toFile();
    final File b = Paths.get("one", "two").toFile();
    final File c = Paths.get("one", "..", "one", ".", "two", ".").toFile();
    final File d = Paths.get("two", "three").toFile();
    assertTrue(isParent(b, a));
    assertTrue(isParent(c, a));
    assertFalse(isParent(d, a));
    assertFalse(isParent(d, c));
    assertFalse(isParent(a, b));
    assertFalse(isParent(a, a));
  }

  @Test
  public void testDeleteHierarchyIfEmpty() throws Exception {
    final File a = newFile(testFolder, "test.tmp");
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
    assertFalse(deleteHierarchyIfEmpty(subDir1, subDir3), "file in sub dir 3 prevents deletion");
    assertTrue(subDir3.exists());
    assertTrue(subDir3File.exists());
    assertTrue(subDir3File.delete());
    assertTrue(deleteHierarchyIfEmpty(subDir1, subDir3), "sub dir 3 and sub dir 2 are empty");
    assertFalse(subDir3.exists());
    assertFalse(subDir2.exists());
    assertTrue(subDir1.exists(), "sub dir 1 has not been deleted");
    assertTrue(subDir2.mkdirs());
    assertTrue(subDir3.mkdirs());
    assertTrue(subDir1File.delete());
    assertTrue(deleteHierarchyIfEmpty(tmpDir, subDir3), "all sub dirs are empty");
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

  private static File newFile(File parent, String child) throws IOException {
    File result = new File(parent, child);
    result.createNewFile();
    return result;
  }
}
