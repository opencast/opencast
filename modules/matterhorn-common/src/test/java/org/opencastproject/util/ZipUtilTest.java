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

package org.opencastproject.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* WARNING
 *
 * Instead of using a single static final file for testing the destination files, we are using
 * a file with a unique name for each test. The reason is that if the tests are run so quickly
 * the filesystem is unable to erase the files on time and a test can re-create the file before
 * it was physically deleted. If those files are suppossed to be different, a test may return
 * a false positive or negative. If the pathnames are different for each test, there's no
 * possible mistake.
 * For uniformity, all the tests declare internally it's own destination file/filename, even
 * though this may not be strictly necessary for all of them.
 *
 * ruben.perez
 */

public class ZipUtilTest {

  private static final String baseDirName = "zip_test_tmp";
  private static final String srcDirName = "src";
  private static final String nestedSrcDirName = "nested";
  private static final String srcFileName = "av.mov";
  private static final String nestedSrcFileName = "manifest.xml";
  private static final String destDirName = "dest";
  private static final String sampleZipName = "sampleZip.zip";
  private static final String dummieName = "notExists";
  private static final String over4GBFileName = "bigFish";

  private static final File baseDir = new File(System.getProperty("java.io.tmpdir"), baseDirName);
  private static final File srcDir = new File(baseDir, srcDirName);
  private static final File nestedSrcDir = new File(srcDir, nestedSrcDirName);
  private static final File srcFile = new File(srcDir, srcFileName);
  private static final File nestedSrcFile = new File(nestedSrcDir, nestedSrcFileName);
  private static final File destDir = new File(baseDir, destDirName);
  private static final File sampleZip = new File(baseDir, sampleZipName);
  private static final File dummieFile = new File(baseDir, dummieName);
  private static final File bigFile = new File(srcDir, over4GBFileName);

  public static final long bigFileSize = (long) 4.5 * 1024 * 1024 * 1024;

  /**
   * Added as part of the fix for MH-1809 WARNING: Changes in the files to zip would change the resulting zip size. If
   * such changes are made, change also this constant accordingly MH-2455: If files used in zip are checked out with
   * native line endings, windows file size will differ.
   */
  // Commented by ruben.perez -- not necessary
  // private static final long UNIX_ZIP_SIZE = 882172;

  private static final Logger logger = LoggerFactory.getLogger(ZipUtilTest.class);

  // Please adjust this value -cedriessen
  // Commented out by ruben.perez --not necessary
  // private static final long WINDOWS_ZIP_SIZE = 870533;

  @Before
  public void setUp() throws Exception {
    // Set up the source and destination directories
    Assert.assertTrue(baseDir.isDirectory() || baseDir.mkdirs());

    Assert.assertTrue(srcDir.isDirectory() || srcDir.mkdir());

    Assert.assertTrue(srcDir.isDirectory() || nestedSrcDir.mkdir());

    Assert.assertTrue(destDir.isDirectory() || destDir.mkdir());

    // Copy the source files from the classpath to the source dir
    FileUtils.copyURLToFile(this.getClass().getResource("/av.mov"), srcFile);
    FileUtils.copyURLToFile(this.getClass().getResource("/manifest.xml"), nestedSrcFile);
    FileUtils.copyURLToFile(this.getClass().getResource("/sampleZip.zip"), sampleZip);

  }

  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(baseDir);
  }

  /** Check the behavior with bad arguments for the zip signature String[], String */
  @Test
  public void badInputZipStrStr() throws Exception {

    File destFile = new File(destDir, "badInputStrStr.zip");

    try {

      // Null input filenames array, correct destination filename
      try {
        ZipUtil.zip((String[]) null, destFile.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when input String array is null");
        Assert.fail("Zip should fail when input String array is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input String array (String, String): OK");
      }

      // Null some of the input filenames, correct destination filename
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), null, nestedSrcFile.getCanonicalPath() },
                destFile.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input filename is null");
        Assert.fail("Zip should fail when any input filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (String, String): OK");
      }

      // Non-existing some of the input filenames, correct destination filename
      try {
        ZipUtil.zip(
                new String[] { srcFile.getCanonicalPath(), dummieFile.getCanonicalPath(),
                        nestedSrcFile.getCanonicalPath() }, destFile.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input filename does not exist");
        Assert.fail("Zip should fail when any input filename does not exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input filename (String, String): OK");
      }

      // Correct input filenames array, null destination filename
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() }, (String) null, true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination filename is null");
        Assert.fail("Zip should fail when destination filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination filename (String, String): OK");
      }

      // Correct input filenames array, empty destination filename
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() }, "", true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination filename is empty");
        Assert.fail("Zip should fail when destination filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty destination filename (String, String): OK");
      }

      // Correct input filenames, existing destination filename
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() },
                sampleZip.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination filename already exists");
        Assert.fail("Zip should fail when destination filename already exists");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting existing destination filename (String, String): OK");
      }

      // Correct input filenames, invalid name for the zip file
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() },
                dummieFile.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when the destination filename does not represent a zip file");
        Assert.fail("Zip should fail when the destination filename does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination filename not representing a valid zip file (String, String): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + " instead: " + e.getMessage());
    }

  }

  /** Check the behavior with bad arguments for the zip signature String[], File */
  @Test
  public void badInputZipStrFile() throws Exception {

    File destFile = new File(destDir, "badInputStrFile.zip");

    try {

      // Null input filenames array, correct destination file
      try {
        ZipUtil.zip((String[]) null, destFile, true, 0);
        logger.error("Zip should fail when input String array is null");
        Assert.fail("Zip should fail when input String array is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input File array (String, File): OK");
      }

      // Null some of the input filenames, correct destination file
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), null, nestedSrcFile.getCanonicalPath() }, destFile,
                true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input filename is null");
        Assert.fail("Zip should fail when any input filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (String, File): OK");
      }

      // Non-existing some of the input filenames, correct destination file
      try {
        ZipUtil.zip(
                new String[] { srcFile.getCanonicalPath(), dummieFile.getCanonicalPath(),
                        nestedSrcFile.getCanonicalPath() }, destFile, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input filename does not exist");
        Assert.fail("Zip should fail when any input filename does not exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input filename (String, File): OK");
      }

      // Correct input filenames array, null destination file
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() }, (File) null, true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination File is null");
        Assert.fail("Zip should fail when destination File is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination File (String, File): OK");
      }

      // Correct input filenames, existing destination file
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() }, sampleZip, true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination file already exists");
        Assert.fail("Zip should fail when destination file already exists");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting existing destination File (String, File): OK");
      }

      // Correct input filenames, invalid name for the zip file
      try {
        ZipUtil.zip(new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath() }, dummieFile, true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when the destination File does not represent a zip file");
        Assert.fail("Zip should fail when the destination File does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination File not representing a valid zip file (String, File): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + " instead: " + e.getMessage());
    }

  }

  /** Check the behavior before bad arguments for the zip signature File[], String */
  @Test
  public void badInputZipFileStr() throws Exception {

    File destFile = new File(destDir.getCanonicalPath(), "badInputFileStr.zip");

    try {

      // Null input File array, correct destination filename
      try {
        ZipUtil.zip((File[]) null, destFile.getCanonicalPath(), true, ZipUtil.DEFAULT_COMPRESSION);
        logger.error("Zip should fail when input File array is null");
        Assert.fail("Zip should fail when input File array is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input File array (File, String): OK");
      }

      // Null some of the input files, correct destination filename
      try {
        ZipUtil.zip(new File[] { srcFile, null, nestedSrcFile }, destFile.getCanonicalPath(), true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input file is null");
        Assert.fail("Zip should fail when any input file is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (File, String): OK");
      }

      // Non-existing some of the input files, correct destination filename
      try {
        ZipUtil.zip(new File[] { srcFile, dummieFile, nestedSrcFile }, destFile.getCanonicalPath(), true,
                ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input file does not exist");
        Assert.fail("Zip should fail when any input file does not exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input filename (File, String): OK");
      }

      // Correct input Files, null destination filename
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, (String) null, true, ZipUtil.DEFAULT_COMPRESSION);
        logger.error("Zip should fail when destination filename is null");
        Assert.fail("Zip should fail when destination filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination filename (File, String): OK");
      }

      // Correct input Files, empty destination filename
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, "", true, ZipUtil.DEFAULT_COMPRESSION);
        logger.error("Zip should fail when destination filename is empty");
        Assert.fail("Zip should fail when destination filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty destination filename (File, String): OK");
      }

      // Correct input filenames, existing destination filename
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, sampleZip.getCanonicalPath(), true,
                ZipUtil.DEFAULT_COMPRESSION);
        logger.error("Zip should fail when destination filename already exists");
        Assert.fail("Zip should fail when destination filename already exists");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting existing destination File (File, String): OK");
      }

      // Correct input Files, invalid name for the zip file
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, dummieFile.getCanonicalPath(), true,
                ZipUtil.DEFAULT_COMPRESSION);
        logger.error("Zip should fail when the destination filename does not represent a zip file");
        Assert.fail("Zip should fail when the destination filename does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination filename not representing a valid zip file (File, String): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + " instead: " + e.getMessage());
    }

  }

  /** Check the behavior before bad arguments for the signature File[], File */
  @Test
  public void badInputZipFileFile() throws Exception {

    File destFile = new File(destDir, "badInputFileFile.zip");

    try {

      // Null File array, correct destination File
      try {
        ZipUtil.zip((File[]) null, destFile, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when input File array is null");
        Assert.fail("Zip should fail when input File array is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input File array (File, File): OK");
      }

      // Null some of the input files, correct destination file
      try {
        ZipUtil.zip(new File[] { srcFile, null, nestedSrcFile }, destFile, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input file is null");
        Assert.fail("Zip should fail when any input file is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (File, File): OK");
      }

      // Non-existing some of the input files, correct destination file
      try {
        ZipUtil.zip(new File[] { srcFile, dummieFile, nestedSrcFile }, destFile, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when any input file does not exist");
        Assert.fail("Zip should fail when any input file does not exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input filename (File, File): OK");
      }

      // Correct input Files, null destination File
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, (File) null, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination File is null");
        Assert.fail("Zip should fail when destination File is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination File (File, File): OK");
      }

      // Correct input Files, existing destination File
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, sampleZip, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when destination File already exists");
        Assert.fail("Zip should fail when destination File already exists");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting existing destination File (File, File): OK");
      }

      // Invalid name for the zip file
      try {
        ZipUtil.zip(new File[] { srcFile, nestedSrcFile }, dummieFile, true, ZipUtil.NO_COMPRESSION);
        logger.error("Zip should fail when the destination File does not represent a zip file");
        Assert.fail("Zip should fail when the destination File does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination File not representing a valid zip file (File, File): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + " instead: " + e.getMessage());
    }

  }

  @Test
  public void zipNoRecStrStr() throws Exception {

    File destFile = new File(destDir, "noRecStrStr.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);

    File test = ZipUtil.zip(
            new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath(),
                    nestedSrcDir.getCanonicalPath() }, destFile.getCanonicalPath(), false, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Assert.assertTrue(names.contains(entry.getName()));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipNoRecStrFile() throws Exception {

    File destFile = new File(destDir, "noRecStrFile.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);

    File test = ZipUtil.zip(
            new String[] { srcFile.getCanonicalPath(), nestedSrcFile.getCanonicalPath(),
                    nestedSrcDir.getCanonicalPath() }, destFile, false, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Assert.assertTrue(names.contains(entry.getName()));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipNoRecFileStr() throws Exception {

    File destFile = new File(destDir, "noRecFileStr.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);

    File test = ZipUtil.zip(new File[] { srcFile, nestedSrcFile, nestedSrcDir }, destFile.getCanonicalPath(), false,
            ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Assert.assertTrue(names.contains(entry.getName()));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipNoRecFileFile() throws Exception {

    File destFile = new File(destDir, "noRecFileFile.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);

    File test = ZipUtil.zip(new File[] { srcFile, nestedSrcFile, nestedSrcDir }, destFile, false,
            ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Assert.assertTrue(names.contains(entry.getName()));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipRecStrStr() throws Exception {

    File destFile = new File(destDir, "recStrStr.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);

    String[] filenames = srcDir.list();
    for (int i = 0; i < filenames.length; i++)
      filenames[i] = srcDir.getCanonicalPath() + File.separator + filenames[i];

    File test = ZipUtil.zip(filenames, destFile.getCanonicalPath(), true, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        // The 'replace' method is used because the filesystem directory separator may not be the same as the Zip
        // files's
        Assert.assertTrue(names.contains(entry.getName().replace('/', File.separatorChar)));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipRecStrFile() throws Exception {

    File destFile = new File(destDir, "recStrFile.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);

    String[] filenames = srcDir.list();
    for (int i = 0; i < filenames.length; i++)
      filenames[i] = srcDir.getCanonicalPath() + File.separator + filenames[i];

    File test = ZipUtil.zip(filenames, destFile, true, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        // The 'replace' method is used because the filesystem directory separator may not be the same as the Zip
        // files's
        Assert.assertTrue(names.contains(entry.getName().replace('/', File.separatorChar)));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipRecFileStr() throws Exception {

    String destFilePath = destDir.getCanonicalPath() + File.separator + "recFileStr.zip";

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);

    File test = ZipUtil.zip(srcDir.listFiles(), destFilePath, true, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        // The 'replace' method is used because the filesystem directory separator may not be the same as the Zip
        // files's
        Assert.assertTrue(names.contains(entry.getName().replace('/', File.separatorChar)));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  @Test
  public void zipRecFileFile() throws Exception {

    File destFile = new File(destDir, "recFileFile.zip");

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);

    File test = ZipUtil.zip(srcDir.listFiles(), destFile, true, ZipUtil.NO_COMPRESSION);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3, zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        // The 'replace' method is used because the filesystem directory separator may not be the same as the Zip
        // files's
        Assert.assertTrue(names.contains(entry.getName().replace('/', File.separatorChar)));
      }
    } catch (AssertionError ae) {
      zip.close();
      throw ae;
    }

    zip.close();
  }

  /** Check the behavior with bad arguments for the unzip signature String, String */
  @Test
  public void badInputUnzipStrStr() throws Exception {

    File destFile = new File(destDir, "badInputStrStr");

    try {

      // Null input filename, correct destination filename
      try {
        ZipUtil.unzip((String) null, destFile.getCanonicalPath());
        logger.error("Unzip should fail when input filename is null");
        Assert.fail("Unzip should fail when input filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (String, String): OK");
      }

      // Empty input filename, correct destination filename
      try {
        ZipUtil.unzip("", destFile.getCanonicalPath());
        logger.error("Unzip should fail when input filename is empty");
        Assert.fail("Unzip should fail when input filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty input filename (String, String): OK");
      }

      // Correct input filename, null destination filename
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), (String) null);
        logger.error("Unzip should fail when destination filename is null");
        Assert.fail("Unzip should fail when destination filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination filename (String, String): OK");
      }

      // Correct input filename, empty destination filename
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), "");
        logger.error("Unzip should fail when destination filename is empty");
        Assert.fail("Unzip should fail when destination filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty destination filename (String, String): OK");
      }

      // Non-existing input filename, correct destination filename
      try {
        ZipUtil.unzip(dummieFile.getCanonicalPath(), destFile.getCanonicalPath());
        logger.error("Unzip should fail when the input filename doesn't exists");
        Assert.fail("Unzip should fail when the input filename doesn't exists");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting existing input filename (String, String): OK");
      }

      // Invalid input filename (using a regular file as input), correct destination filename
      try {
        ZipUtil.unzip(srcFile.getCanonicalPath(), destFile.getCanonicalPath());
        logger.error("Unzip should fail when the input filename does not represent a zip file");
        Assert.fail("Unzip should fail when the input filename does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting input filename not representing a valid zip file (String, String): OK");
      }

      // Correct input filename, invalid destination filename (some existing regular file rather than a dir)
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), srcFile.getCanonicalPath());
        logger.error("Unzip should fail when the destination filename does not represent a directory");
        Assert.fail("Unzip should fail when the destination filename does not represent a directory");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination filename not representing a directory (String, String): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }

  /** Check the behavior with bad arguments for the unzip signature String, File */
  @Test
  public void badInputUnzipStrFile() throws Exception {

    File destFile = new File(destDir, "badInputStrFile");

    try {

      // Null input filename, correct destination file
      try {
        ZipUtil.unzip((String) null, destFile);
        logger.error("Unzip should fail when input filename is null");
        Assert.fail("Unzip should fail when input filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input filename (String, File): OK");
      }

      // Empty input filename, correct destination file
      try {
        ZipUtil.unzip("", destFile);
        logger.error("Unzip should fail when input filename is empty");
        Assert.fail("Unzip should fail when input filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty input filename (String, File): OK");
      }

      // Non-existing input filename, correct destination file
      try {
        ZipUtil.unzip(dummieFile.getCanonicalPath(), destFile);
        logger.error("Unzip should fail when the input filename doesn't exists");
        Assert.fail("Unzip should fail when the input filename doesn't exists");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting existing input filename (String, File): OK");
      }

      // Correct input filename, null destination file
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), (File) null);
        logger.error("Unzip should fail when destination filename is null");
        Assert.fail("Unzip should fail when destination filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination filename (String, File): OK");
      }

      // Invalid input filename (using a regular file as input), correct destination file
      try {
        ZipUtil.unzip(srcFile.getCanonicalPath(), destFile);
        logger.error("Unzip should fail when the input filename does not represent a zip file");
        Assert.fail("Unzip should fail when the input filename does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting input filename not representing a valid zip file (String, File): OK");
      }

      // Correct input filename, invalid destination file (some existing regular file rather than a dir)
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), srcFile);
        logger.error("Unzip should fail when the destination File does not represent a directory");
        Assert.fail("Unzip should fail when the destination File does not represent a directory");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination File not representing a directory (String, File): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }

  /** Check the behavior with bad arguments for the unzip signature File, String */
  @Test
  public void badInputUnzipFileStr() throws Exception {

    File destFile = new File(destDir, "badInputFileStr");

    try {

      // Null input file, correct destination filename
      try {
        ZipUtil.unzip((File) null, destFile.getCanonicalPath());
        logger.error("Unzip should fail when input File is null");
        Assert.fail("Unzip should fail when input File is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input File (File, String): OK");
      }

      // Non-existing input file, correct destination filename
      try {
        ZipUtil.unzip(dummieFile, destFile.getCanonicalPath());
        logger.error("Unzip should fail when input File doesn't exist");
        Assert.fail("Unzip should fail when input File doesn't exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input File (File, String): OK");
      }

      // Correct input file, null destination filename
      try {
        ZipUtil.unzip(sampleZip, (String) null);
        logger.error("Unzip should fail when destination filename is null");
        Assert.fail("Unzip should fail when destination filename is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination filename (File, String): OK");
      }

      // Correct input file, empty destination filename
      try {
        ZipUtil.unzip(sampleZip.getCanonicalPath(), "");
        logger.error("Unzip should fail when destination filename is empty");
        Assert.fail("Unzip should fail when destination filename is empty");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting empty destination filename (File, String): OK");
      }

      // Invalid input filename (using a regular file as input), correct destination filename
      try {
        ZipUtil.unzip(srcFile, destFile.getCanonicalPath());
        logger.error("Unzip should fail when the input File does not represent a zip file");
        Assert.fail("Unzip should fail when the input File does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting input File not representing a valid zip file (File, String): OK");
      }

      // Correct input file, invalid destination filename (some existing regular file rather than a dir)
      try {
        ZipUtil.unzip(sampleZip, srcFile.getCanonicalPath());
        logger.error("Unzip should fail when the destination filename does not represent a directory");
        Assert.fail("Unzip should fail when the destination filename does not represent a directory");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination filename not representing a directory (File, String): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }

  /** Check the behavior with bad arguments for the unzip signature File, File */
  @Test
  public void badInputUnzipFileFile() throws Exception {

    File destFile = new File(destDir, "badInputFileFile");

    try {

      // Null input file, correct destination file
      try {
        ZipUtil.unzip((File) null, destFile);
        logger.error("Unzip should fail when input File is null");
        Assert.fail("Unzip should fail when input File is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null input File (File, File): OK");
      }

      // Non-existing input file, correct destination file
      try {
        ZipUtil.unzip(dummieFile, destFile);
        logger.error("Unzip should fail when input File doesn't exist");
        Assert.fail("Unzip should fail when input File doesn't exist");
      } catch (FileNotFoundException e) {
        logger.debug("Detecting non-existing input File (File, File): OK");
      }

      // Correct input filename, null destination filename
      try {
        ZipUtil.unzip(sampleZip, (File) null);
        logger.error("Unzip should fail when destination File is null");
        Assert.fail("Unzip should fail when destination File is null");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting null destination File (File, File): OK");
      }

      // Invalid input file (using a regular file as input), correct destination file
      try {
        ZipUtil.unzip(srcFile, destFile);
        logger.error("Unzip should fail when the input File does not represent a zip file");
        Assert.fail("Unzip should fail when the input File does not represent a zip file");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting input File not representing a valid zip file (File, File): OK");
      }

      // Correct input file, invalid destination file (some existing regular file rather than a dir)
      try {
        ZipUtil.unzip(sampleZip, srcFile);
        logger.error("Unzip should fail when the destination File does not represent a directory");
        Assert.fail("Unzip should fail when the destination File does not represent a directory");
      } catch (IllegalArgumentException e) {
        logger.debug("Detecting destination File not representing a directory (File, File): OK");
      }

    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }

  /**
   * Test unzipping
   */
  @Test
  public void testUnzip() throws Exception {

    ZipUtil.unzip(sampleZip, destDir);

    ZipFile test = new ZipFile(sampleZip);

    Enumeration<? extends ZipEntry> entries = test.entries();

    try {
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        Assert.assertTrue(new File(destDir, entry.getName()).exists());
      }
    } catch (AssertionError ae) {
      test.close();
      throw ae;
    }

    test.close();

  }

  /**
   * Test compression of >4GB file
   */
  @Test
  @Ignore
  public void testOver4GB() throws Exception {

    if (bigFile.exists() || bigFile.createNewFile()) {
      if (bigFile.getUsableSpace() >= bigFileSize) {

        FileOutputStream fos = new FileOutputStream(bigFile);
        byte[] buffer = new byte[1024];
        Random rdn = new Random();
        File destZip = new File(destDir, "bigZip.zip");

        for (long i = 0; i < bigFileSize; i += buffer.length) {
          rdn.nextBytes(buffer);
          fos.write(buffer);
        }

        fos.close();

        ZipUtil.zip(new File[] { bigFile }, destZip, ZipUtil.NO_COMPRESSION);

        Assert.assertTrue(destZip.exists());
        Assert.assertTrue(destZip.length() >= bigFileSize);
      } else {
        logger.warn("This test needs more than 4GB of disk free space: {}", bigFile.getUsableSpace());
        logger.warn("Skipping...");
      }
    } else
      logger.warn("Couldn't create the File descriptor");
  }

}
