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


package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.MediaPackageSupport.MergeMode;
import org.opencastproject.util.FileSupport;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure media package support works as expected.
 */
public class MediaPackageMergeTest {

  /** tmp directory */
  private File tmpDir = null;

  /** Media package directories */
  private File packageDir1 = null;
  private File packageDir2 = null;

  /** The media package builder */
  private MediaPackageBuilder mediaPackageBuilder = null;

  /** The source media package directory for merge tests */
  private MediaPackage sourcePackage = null;

  /** The target media package directory for merge tests */
  private MediaPackage targetPackage = null;

  @Before
  public void setUp() throws Exception {

    // Get hold of the tmp directory
    tmpDir = FileSupport.getTempDirectory();

    // Create a media package builder
    mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // Create source and target media package
    setUpSourceMediaPackage();
    setUpTargeMediaPackage();
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(packageDir1);
    FileUtils.deleteDirectory(packageDir2);
  }

  /**
   * Creates the source media package.
   *
   * @throws IOException
   * @throws MediaPackageException
   */
  private void setUpSourceMediaPackage() throws Exception {
    // Create the media package directory
    packageDir1 = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    File trackDir = new File(packageDir1, "tracks");
    trackDir.mkdirs();
    File metadataDir = new File(packageDir1, "metadata");
    metadataDir.mkdirs();
    File attachmentDir = new File(packageDir1, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File baseDir = new File(MediaPackageBuilderTest.class.getResource("/").toURI());
    File sourceManifestFile = new File(baseDir, "source-manifest.xml");
    File videoTestFile = new File(baseDir, "vonly.mov");
    File audioTestFile = new File(baseDir, "aonly.mov");
    File dcTestFile = new File(baseDir, "dublincore.xml");
    File dcSeriesTestFile = new File(baseDir, "series-dublincore.xml");
    File mpeg7TestFile = new File(baseDir, "mpeg-7.xml");
    File coverTestFile = new File(baseDir, "cover.png");

    // Copy files into place
    File manifestFile = new File(packageDir1, MediaPackageElements.MANIFEST_FILENAME);
    FileSupport.copy(sourceManifestFile, manifestFile);
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, new File(metadataDir, "series-dublincore-new.xml"));
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);
    FileSupport.copy(coverTestFile, new File(attachmentDir, "series-cover.png"));

    // Test the media package
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile()));
    sourcePackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));
    sourcePackage.verify();
  }

  /**
   * Creates the target media package.
   *
   * @throws IOException
   * @throws MediaPackageException
   * @throws URISyntaxException
   */
  private void setUpTargeMediaPackage() throws IOException, MediaPackageException, URISyntaxException {
    // Create the media package directory
    packageDir2 = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    File trackDir = new File(packageDir2, "tracks");
    trackDir.mkdirs();
    File metadataDir = new File(packageDir2, "metadata");
    metadataDir.mkdirs();
    File attachmentDir = new File(packageDir2, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File baseDir = new File(MediaPackageBuilderTest.class.getResource("/").toURI());
    File sourceManifestFile = new File(baseDir, "target-manifest.xml");
    File videoTestFile = new File(baseDir, "vonly.mov");
    File audioTestFile = new File(baseDir, "aonly.mov");
    File dcTestFile = new File(baseDir, "dublincore.xml");
    File dcSeriesTestFile = new File(baseDir, "series-dublincore.xml");
    File mpeg7TestFile = new File(baseDir, "mpeg-7.xml");
    File coverTestFile = new File(baseDir, "cover.png");

    // Copy files into place
    File manifestFile = new File(packageDir2, MediaPackageElements.MANIFEST_FILENAME);
    FileSupport.copy(sourceManifestFile, manifestFile);
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(audioTestFile, new File(trackDir, "aonly2.mov"));
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);

    // Test the media package
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile()));
    targetPackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));
    targetPackage.verify();
  }

  /**
   * Checks the media package for duplicate identifier and files.
   *
   * @param mediaPackage
   *          the media package to test
   * @throws MediaPackageException
   */
  private void testMediaPackageConsistency(MediaPackage mediaPackage) throws MediaPackageException {
    List<String> ids = new ArrayList<String>();
    List<URI> files = new ArrayList<URI>();
    for (MediaPackageElement e : mediaPackage.elements()) {
      if (ids.contains(e.getIdentifier()))
        throw new MediaPackageException("Duplicate id " + e.getIdentifier() + "' found");
      ids.add(e.getIdentifier());
      if (files.contains(e.getURI()))
        throw new MediaPackageException("Duplicate filename " + e.getURI() + "' found");
      files.add(e.getURI());
    }
  }

  @Test
  public void testMergeByMerging() {
    try {
      MediaPackage mediaPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Merge);
      assertTrue(mediaPackage.getTracks().length == 5);
      assertTrue(mediaPackage.getCatalogs().length == 6);
      assertTrue(mediaPackage.getAttachments().length == 3);
      testMediaPackageConsistency(mediaPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeByReplacing() {
    try {
      // Should be replaced
      MediaPackageElement track1 = targetPackage.getElementById("track-1");
      MediaPackageElement catalog1 = targetPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = targetPackage.getElementById("catalog-2");
      MediaPackageElement cover = targetPackage.getElementById("cover");

      // Should remain untouched
      MediaPackageElement track2 = targetPackage.getElementById("track-2");
      MediaPackageElement track4 = targetPackage.getElementById("track-4");
      MediaPackageElement catalog3 = targetPackage.getElementById("catalog-3");

      // Merge the media package
      MediaPackage mergedPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Replace);

      // Test number of elements
      assertEquals(mergedPackage.getTracks().length, 4);
      assertEquals(mergedPackage.getCatalogs().length, 4);
      assertEquals(mergedPackage.getAttachments().length, 2);

      // Test for replaced elements
      assertNotSame(track1, mergedPackage.getElementById("track-1"));
      assertNotSame(catalog1, mergedPackage.getElementById("catalog-1"));
      assertNotSame(catalog2, mergedPackage.getElementById("catalog-2"));
      assertNotSame(cover, mergedPackage.getElementById("cover"));

      // Test for untouched elements
      assertEquals(track2, mergedPackage.getElementById("track-2"));
      assertEquals(track4, mergedPackage.getElementById("track-4"));
      assertEquals(catalog3, mergedPackage.getElementById("catalog-3"));

      testMediaPackageConsistency(mergedPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeBySkipping() {
    try {
      // Should remain untouched
      MediaPackageElement track1 = targetPackage.getElementById("track-1");
      MediaPackageElement catalog1 = targetPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = targetPackage.getElementById("catalog-2");
      MediaPackageElement cover = targetPackage.getElementById("cover");

      // Merge the media package
      MediaPackage mergedPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Skip);

      // Test number of elements
      assertEquals(mergedPackage.getTracks().length, 4);
      assertEquals(mergedPackage.getCatalogs().length, 4);
      assertEquals(mergedPackage.getAttachments().length, 2);

      // Test for untouched elements
      assertEquals(track1, mergedPackage.getElementById("track-1"));
      assertEquals(catalog1, mergedPackage.getElementById("catalog-1"));
      assertEquals(catalog2, mergedPackage.getElementById("catalog-2"));
      assertEquals(cover, mergedPackage.getElementById("cover"));

      testMediaPackageConsistency(mergedPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeByFailing() {
    try {
      MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Fail);
      fail("Merging should have failed but didn't");
    } catch (MediaPackageException e) {
      // This is excpected
    }
  }

}
