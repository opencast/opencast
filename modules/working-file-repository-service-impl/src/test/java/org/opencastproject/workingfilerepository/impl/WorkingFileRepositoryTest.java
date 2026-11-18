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


package org.opencastproject.workingfilerepository.impl;

import static org.junit.jupiter.api.Assertions.fail;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WorkingFileRepositoryTest {

  private String mediaPackageID = "working-file-test-media-package-1";
  private String mediaPackageElementID = "working-file-test-element-1";
  private String collectionId = "collection-1";
  private String filename = "file.gif";
  private WorkingFileRepositoryImpl repo = new WorkingFileRepositoryImpl();

  @BeforeEach
  public void setUp() throws Exception {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn("org1").anyTimes();
    Map<String, String> orgProps = new HashMap<String, String>();
    orgProps.put(OpencastConstants.WFR_URL_ORG_PROPERTY, UrlSupport.DEFAULT_BASE_URL);
    EasyMock.expect(organization.getProperties()).andReturn(orgProps).anyTimes();
    EasyMock.replay(organization);

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    repo.setSecurityService(securityService);
    repo.rootDirectory = "target" + File.separator + "repotest";
    repo.serverUrl = UrlSupport.DEFAULT_BASE_URL;
    repo.servicePath = WorkingFileRepositoryImpl.URI_PREFIX;
    repo.createRootDirectory();

    // Put an image file into the repository using the mediapackage / element storage
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(mediaPackageID, mediaPackageElementID, "opencast_header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Repeat the put
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(mediaPackageID, mediaPackageElementID, "opencast_header.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Put an image file into the repository into a collection
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.putInCollection(collectionId, filename, in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    FileUtils.forceDelete(new File(repo.rootDirectory));
  }

  @Test
  public void testPut() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = null;
    InputStream headerIn = null;
    try {
      fromRepo = repo.get(mediaPackageID, mediaPackageElementID);
      headerIn = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
      byte[] bytesFromClasspath = IOUtils.toByteArray(headerIn);
      Assertions.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(fromRepo);
      IOUtils.closeQuietly(headerIn);
    }
  }

  @Test
  public void testDelete() throws Exception {
    // Delete the file and ensure that we can no longer get() it
    repo.delete(mediaPackageID, mediaPackageElementID);
    try {
      Assertions.assertTrue(repo.get(mediaPackageID, mediaPackageElementID) == null);
      fail("File " + mediaPackageID + "/" + mediaPackageElementID + " was not deleted");
    } catch (NotFoundException e) {
      // This is intended
    }
  }

  @Test
  public void testPutBadId() throws Exception {
    // Try adding a file with a bad ID
    String badId = "../etc";
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(badId, mediaPackageElementID, "opencast_header.gif", in);
      Assertions.fail();
    } catch (Exception e) {
      // This is intended
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testGetBadId() throws Exception {
    String badId = "../etc";
    try {
      repo.get(badId, mediaPackageElementID);
      Assertions.fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testPutIntoCollection() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = null;
    InputStream headerIn = null;
    try {
      fromRepo = repo.getFromCollection(collectionId, filename);
      byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
      headerIn = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      byte[] bytesFromClasspath = IOUtils.toByteArray(headerIn);
      Assertions.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(fromRepo);
      IOUtils.closeQuietly(headerIn);
    }
  }

  @Test
  public void testCollectionSize() throws Exception {
    Assertions.assertEquals(1, repo.getCollectionSize(collectionId));
  }

  @Test
  public void testCopy() throws Exception {
    String newFileName = "newfile.gif";
    byte[] bytesFromCollection = null;
    InputStream in = null;
    try {
      in = repo.getFromCollection(collectionId, filename);
      bytesFromCollection = IOUtils.toByteArray(in);
      IOUtils.closeQuietly(in);
      repo.copyTo(collectionId, filename, "copied-mediapackage", "copied-element", newFileName);
      in = repo.get("copied-mediapackage", "copied-element");
      byte[] bytesFromCopy = IOUtils.toByteArray(in);
      Assertions.assertTrue(Arrays.equals(bytesFromCollection, bytesFromCopy));
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testMove() throws Exception {
    String newFileName = "newfile.gif";
    InputStream in = null;
    try {
      in = repo.getFromCollection(collectionId, filename);
      byte[] bytesFromCollection = IOUtils.toByteArray(in);
      IOUtils.closeQuietly(in);
      repo.moveTo(collectionId, filename, "moved-mediapackage", "moved-element", newFileName);
      in = repo.get("moved-mediapackage", "moved-element");
      byte[] bytesFromMove = IOUtils.toByteArray(in);
      Assertions.assertTrue(Arrays.equals(bytesFromCollection, bytesFromMove));
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testCleanupOldFilesFromCollectionNothingToDelete() throws Exception {
    // Cleanup files older than 1 day, nothing should be deleted
    boolean result = repo.cleanupOldFilesFromCollection(collectionId, 1);
    Assertions.assertTrue(result);
    InputStream in = null;
    try {
      in = repo.getFromCollection(collectionId, filename);
      Assertions.assertNotNull(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testCleanupOldFilesFromCollectionSomethingToDelete() throws Exception {
    // Cleanup files older than 0 days, file should be deleted
    boolean result = repo.cleanupOldFilesFromCollection(collectionId, 0);
    Assertions.assertTrue(result);
    try {
      Assertions.assertTrue(repo.getFromCollection(collectionId, filename) == null);
    } catch (NotFoundException e) {
      // This is intended
    }
  }

  @Test
  public void testCleanupOldFilesFromNonExistentCollection() throws Exception {
    boolean result = repo.cleanupOldFilesFromCollection("UNKNOWN", 0);
    Assertions.assertFalse(result);
  }

  @Test
  public void testCleanupOldFilesFromMediaPackageNothingToDelete() throws Exception {
    // Cleanup files older than 1 day, nothing should be deleted
    boolean result = repo.cleanupOldFilesFromMediaPackage(1);
    Assertions.assertTrue(result);
    File file = null;
    file = repo.getFile(mediaPackageID, mediaPackageElementID);
    Assertions.assertNotNull(file);
  }

  @Test
  public void testCleanupOldFilesFromMediaPackageSomethingToDelete() throws Exception {
    // Cleanup files older than 1 day, something should be deleted
    boolean result = repo.cleanupOldFilesFromMediaPackage(0);
    Assertions.assertTrue(result);
    File file = null;
    try {
      file = repo.getFile(mediaPackageID, mediaPackageElementID);
      Assertions.fail();
    } catch (NotFoundException e) {
      Assertions.assertNull(file);
    }
  }

  /**
   * The identifier rule applies to all operations: the empty identifier, the current and parent
   * directory references and path separators must be rejected before any file system access
   */
  @Test
  public void testUnsafeIdsAreRejected() throws Exception {
    String[] unsafeIds = new String[] { "", ".", "..", "a/b", "a\\b", "../etc" };
    for (String badId : unsafeIds) {
      assertIllegalArgument(() -> {
        InputStream in = null;
        try {
          in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
          repo.put(badId, mediaPackageElementID, "file.gif", in);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }, "put with unsafe media package ID '" + badId + "'");
      assertIllegalArgument(() -> {
        InputStream in = null;
        try {
          in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
          repo.put(mediaPackageID, badId, "file.gif", in);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }, "put with unsafe element ID '" + badId + "'");
      assertIllegalArgument(() -> repo.get(badId, mediaPackageElementID),
              "get with unsafe media package ID '" + badId + "'");
      assertIllegalArgument(() -> repo.get(mediaPackageID, badId), "get with unsafe element ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.delete(badId, mediaPackageElementID), "delete with unsafe media package ID '" + badId + "'");
      assertIllegalArgument(() -> repo.delete(mediaPackageID, badId), "delete with unsafe element ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.copyTo(collectionId, filename, badId, mediaPackageElementID, "file.gif"),
              "copyTo with unsafe media package ID '" + badId + "'");
      assertIllegalArgument(() -> repo.copyTo(collectionId, filename, mediaPackageID, badId, "file.gif"),
              "copyTo with unsafe element ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.moveTo(collectionId, filename, badId, mediaPackageElementID, "file.gif"),
              "moveTo with unsafe media package ID '" + badId + "'");
      assertIllegalArgument(() -> repo.moveTo(collectionId, filename, mediaPackageID, badId, "file.gif"),
              "moveTo with unsafe element ID '" + badId + "'");
    }
  }

  /**
   * The collection identifier is joined into the collection path, so the read-only collection
   * operations must reject unsafe identifiers as well, not only the ones that write to it
   */
  @Test
  public void testUnsafeCollectionIdsAreRejected() throws Exception {
    String[] unsafeIds = new String[] { "", ".", "..", "a/b", "a\\b", "../etc" };
    for (String badId : unsafeIds) {
      assertIllegalArgument(
              () -> repo.getCollectionSize(badId), "getCollectionSize with unsafe collection ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.getCollectionContents(badId),
              "getCollectionContents with unsafe collection ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.cleanupOldFilesFromCollection(badId, 3),
              "cleanupOldFilesFromCollection with unsafe collection ID '" + badId + "'");
      assertIllegalArgument(
              () -> repo.getFromCollection(badId, filename),
              "getFromCollection with unsafe collection ID '" + badId + "'");
    }
  }

  /**
   * A {@code ..} sequence inside an identifier component is a harmless literal file system name
   * and must stay usable
   */
  @Test
  public void testIdWithDoubleDotSequenceIsAccepted() throws Exception {
    String mpId = "mediapackage..1";
    String elemId = "element..1";
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(mpId, elemId, "file.gif", in);
    } finally {
      IOUtils.closeQuietly(in);
    }
    InputStream fromRepo = null;
    InputStream headerIn = null;
    try {
      fromRepo = repo.get(mpId, elemId);
      headerIn = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      Assertions.assertTrue(Arrays.equals(IOUtils.toByteArray(headerIn), IOUtils.toByteArray(fromRepo)));
    } finally {
      IOUtils.closeQuietly(fromRepo);
      IOUtils.closeQuietly(headerIn);
    }
  }

  /**
   * An incomplete element (a payload without its MD5 checksum, e.g. left behind by a previously
   * failed move) may already exist at the destination. A failed move must not delete it
   */
  @Test
  public void testMoveToKeepsPreExistingDestinationOnFailure() throws Exception {
    String toMpId = "orphan-mediapackage";
    String toElemId = "orphan-element";
    File destDir = Paths
            .get(repo.rootDirectory, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, toMpId, toElemId).toFile();
    FileUtils.forceMkdir(destDir);
    File dest = new File(destDir, repo.toSafeName(filename));
    FileUtils.writeStringToFile(dest, "orphan-payload", StandardCharsets.UTF_8);

    try {
      repo.moveTo(collectionId, filename, toMpId, toElemId, filename);
      fail("Moving over a pre-existing destination must fail");
    } catch (IllegalStateException e) {
      // intended: the destination file already exists
    }

    // The pre-existing payload must survive the failed move untouched
    Assertions.assertTrue(dest.exists(), "Pre-existing destination must survive a failed move");
    Assertions.assertEquals("orphan-payload", FileUtils.readFileToString(dest, StandardCharsets.UTF_8));
    // and so must the source in the collection
    InputStream in = null;
    try {
      in = repo.getFromCollection(collectionId, filename);
      Assertions.assertNotNull(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static void assertIllegalArgument(ThrowingRunnable runnable, String description) {
    try {
      runnable.run();
      fail(description + " must be rejected");
    } catch (IllegalArgumentException e) {
      // intended
    } catch (Exception e) {
      fail(description + " was rejected with " + e + " instead of IllegalArgumentException");
    }
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

}
