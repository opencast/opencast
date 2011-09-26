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

package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.identifier.Handle;
import org.opencastproject.mediapackage.identifier.HandleBuilder;
import org.opencastproject.mediapackage.identifier.HandleBuilderFactory;
import org.opencastproject.util.FileSupport;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileInputStream;

/**
 * Base class for media package tests.
 */
public abstract class AbstractMediaPackageTest {

  /** tmp directory */
  protected File tmpDir = null;

  /** The media package used to test */
  protected MediaPackage mediaPackage = null;

  /** The media package builder */
  protected MediaPackageBuilder mediaPackageBuilder = null;

  /** The handle builder */
  protected HandleBuilder handleBuilder = null;

  /** The media package identifier */
  protected Handle identifier = null;

  /** The test media packages root directory */
  protected File packageDir = null;

  /** The test media packages metadata directory */
  protected File metadataDir = null;

  /** The test media packages track directory */
  protected File trackDir = null;

  /** The test media packages attachment directory */
  protected File attachmentDir = null;

  /** The media package manifest */
  protected File manifestFile = null;

  /** The dublin core catalog */
  protected File dcFile = null;

  /** The series dublin core catalog */
  protected File dcSeriesFile = null;

  /** The mpeg-7 catalog */
  protected File mpeg7File = null;

  /** The cover */
  protected File coverFile = null;

  /** The vidoe track */
  protected File videoFile = null;

  /** The audio track */
  protected File audioFile = null;

  /** The zip file */
  protected File archiveFile = null;

  /** The unzipped media package */
  protected MediaPackage unzippedMediaPackage = null;

  /**
   * Creates everything that is needed to test a media package.
   * 
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    // Create a media package builder
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    mediaPackageBuilder = builderFactory.newMediaPackageBuilder();

    // Create a handle builder
    handleBuilder = HandleBuilderFactory.newInstance().newHandleBuilder();

    identifier = handleBuilder.createNew();

    // Get hold of the tmp directory
    tmpDir = FileSupport.getTempDirectory();

    // Create the media package directory
    packageDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    trackDir = new File(packageDir, "tracks");
    trackDir.mkdirs();
    metadataDir = new File(packageDir, "metadata");
    metadataDir.mkdirs();
    attachmentDir = new File(packageDir, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File baseDir = new File(MediaPackageBuilderTest.class.getResource("/").toURI());
    File manifestTestFile = new File(baseDir, "manifest.xml");
    File videoTestFile = new File(baseDir, "vonly.mov");
    File audioTestFile = new File(baseDir, "aonly.mov");
    File dcTestFile = new File(baseDir, "dublincore.xml");
    File dcSeriesTestFile = new File(baseDir, "series-dublincore.xml");
    File mpeg7TestFile = new File(baseDir, "mpeg-7.xml");
    File coverTestFile = new File(baseDir, "cover.png");

    // Copy files into place
    manifestFile = FileSupport.copy(manifestTestFile, new File(packageDir, MediaPackageElements.MANIFEST_FILENAME));
    videoFile = FileSupport.copy(videoTestFile, trackDir);
    audioFile = FileSupport.copy(audioTestFile, trackDir);
    dcFile = FileSupport.copy(dcTestFile, metadataDir);
    dcSeriesFile = FileSupport.copy(dcSeriesTestFile, metadataDir);
    mpeg7File = FileSupport.copy(mpeg7TestFile, metadataDir);
    coverFile = FileSupport.copy(coverTestFile, attachmentDir);

    // Create a media package
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(packageDir));
    mediaPackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));
  }

  /**
   * Cleans up after every test method.
   * 
   * @throws Exception
   */
  @After
  public void tearDown() throws Exception {
    if (packageDir.getParentFile().getName().equals(identifier.getNamingAuthority()))
      FileSupport.delete(packageDir.getParentFile(), true);
    else
      FileSupport.delete(packageDir, true);
  }

}
