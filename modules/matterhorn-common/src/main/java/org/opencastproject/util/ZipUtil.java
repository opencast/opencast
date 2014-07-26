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

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.ArchiveException;
import de.schlichtherle.io.ArchiveWarningException;
import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.schlichtherle.io.archive.zip.ZipDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;
import java.util.zip.Deflater;

/*
 * WARNING:
 * Some archivers, such as file-roller in Ubuntu, seem not to be able to uncompress zip archives containg files with special characters.
 * The pure zip standard uses the CP437 encoding which CAN'T represent special characters, but applications have implemented their own methods
 * to overcome this inconvenience. TrueZip also. However, it seems file-roller (and probably others) doesn't "understand" how to restore the
 * original filenames and shows strange (and un-readable) characters in the unzipped filenames
 * However, the inverse process (unzipping zip archives made with file-roller and containing files with special characters) seems to work fine
 *
 * N.B. By "special characters" I mean those not present in the original ASCII specification, such as accents, special letters, etc
 *
 * ruben.perez
 */

/**
 * Provides static methods for compressing and extracting zip files using zip64 extensions when necessary.
 */
public final class ZipUtil {

  private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

  public static final int BEST_SPEED = Deflater.BEST_SPEED;
  public static final int BEST_COMPRESSION = Deflater.BEST_COMPRESSION;
  public static final int DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION;
  public static final int NO_COMPRESSION = Deflater.NO_COMPRESSION;

  /** Disable construction of this utility class */
  private ZipUtil() {
  }

  /**
   * Utility class to ease the process of umounting a zip file
   *
   * @param zipFile
   *          The file to umount
   * @throws IOException
   *           If some problem occurs on unmounting
   */
  private static void umount(File zipFile) throws IOException {
    try {
      File.umount(zipFile);
    } catch (ArchiveWarningException awe) {
      logger.warn("Umounting {} threw the following warning: {}", zipFile.getCanonicalPath(), awe.getMessage());
    } catch (ArchiveException ae) {
      logger.error("Unable to umount zip file: {}", zipFile.getCanonicalPath());
      throw new IOException("Unable to umount zip file: " + zipFile.getCanonicalPath(), ae);
    }
  }

  /***********************************************************************************/
  /* SERVICE CLASSES - The two following classes are the ones actually doing the job */
  /***********************************************************************************/

  /**
   * Compresses source files into a zip archive
   *
   * @param files
   *          A {@link java.io.File} array with the files to include in the root of the archive
   * @param destination
   *          A {@link java.io.File} descriptor to the location where the zip file should be created
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, java.io.File destination, boolean recursive, int level)
          throws IOException {

    if (sourceFiles == null) {
      logger.error("The array with files to zip cannot be null");
      throw new IllegalArgumentException("The array with files to zip cannot be null");
    }

    if (sourceFiles.length <= 0) {
      logger.error("The array with files to zip cannot be empty");
      throw new IllegalArgumentException("The array with files to zip cannot be empty");
    }

    if (destination == null) {
      logger.error("The destination file cannot be null");
      throw new IllegalArgumentException("The destination file cannot be null");
    }

    if (destination.exists()) {
      logger.error("The destination file {} already exists", destination.getCanonicalPath());
      throw new IllegalArgumentException("The destination file already exists");
    }

    if (level < -1) {
      logger.warn("Compression level cannot be less than 0 (or -1 for default)");
      logger.warn("Reverting to default...");
      level = -1;
    } else if (level > 9) {
      logger.warn("Compression level cannot be greater than 9");
      logger.warn("Reverting to default...");
      level = -1;
    }

    // Limits the compression support to ZIP only and sets the compression level
    ZipDriver zd = new ZipDriver(level);
    ArchiveDetector ad = new DefaultArchiveDetector(ArchiveDetector.NULL, "zip", zd);
    File zipFile;
    try {
      zipFile = new File(destination.getCanonicalFile(), ad);
    } catch (IOException ioe) {
      logger.error("Unable to create the zip file: {}", destination.getAbsolutePath());
      throw new IOException("Unable to create the zip file: {}" + destination.getAbsolutePath(), ioe);
    }

    try {
      if (!zipFile.isArchive()) {
        logger.error("The destination file does not represent a valid zip archive (.zip extension is required)");
        zipFile.deleteAll();
        throw new IllegalArgumentException(
                "The destination file does not represent a valid zip archive (.zip extension is required)");
      }

      if (!zipFile.mkdirs())
        throw new IOException("Couldn't create the destination file");

      for (java.io.File f : sourceFiles) {

        if (f == null) {
          logger.error("Null inputfile in array");
          zipFile.deleteAll();
          throw new IllegalArgumentException("Null inputfile in array");
        }

        logger.debug("Attempting to zip file {}...", f.getAbsolutePath());

        // TrueZip manual says that (archiveC|copy)All(From|To) methods work with either directories or regular files
        // Therefore, one could do zipFile.archiveCopyAllFrom(f), where f is a regular file, and it would work. Well, it
        // DOESN'T
        // This is why we have to tell if a file is a regular file or a directory BEFORE copying it with the appropriate
        // method
        boolean success = false;
        if (f.exists()) {
          if (!f.isDirectory() || recursive) {
            success = new File(zipFile, f.getName()).copyAllFrom(f);
            if (success)
              logger.debug("File {} zipped successfuly", f.getAbsolutePath());
            else {
              logger.error("File {} not zipped", f.getAbsolutePath());
              zipFile.deleteAll();
              throw new IOException("Failed to zip one of the input files: " + f.getAbsolutePath());
            }
          }
        } else {
          logger.error("Input file {} doesn't exist", f.getAbsolutePath());
          zipFile.deleteAll();
          throw new FileNotFoundException("One of the input files does not exist: " + f.getAbsolutePath());
        }
      }
    } catch (IOException e) {
      throw e;
    } finally {
      umount(zipFile);
    }

    return destination;
  }

  /**
   * Extracts a zip file to a directory.
   *
   * @param zipFile
   *          A {@link String} with the path to the source zip archive
   * @param destination
   *          A {@link String} with the location where the zip archive will be extracted. If this destination directory
   *          does not exist, it will be created.
   * @throws IOException
   *           if the zip file cannot be read, the destination directory cannot be created or the extraction is not
   *           successful
   */
  public static void unzip(java.io.File zipFile, java.io.File destination) throws IOException {

    boolean success;

    if (zipFile == null) {
      logger.error("The zip file cannot be null");
      throw new IllegalArgumentException("The zip file must be set");
    }

    if (!zipFile.exists()) {
      logger.error("The zip file does not exist: {}", zipFile.getCanonicalPath());
      throw new FileNotFoundException("The zip file does not exist: " + zipFile.getCanonicalPath());
    }

    if (destination == null) {
      logger.error("The destination file cannot be null");
      throw new IllegalArgumentException("Destination file cannot be null");
    }

    // FIXME Commented out for 3rd party compatibility. See comment in the zip method above -ruben.perez
    // File f = new File(zipFile.getCanonicalFile(), new DefaultArchiveDetector(ArchiveDetector.NULL, "zip", new
    // ZipDriver("utf-8")));
    File f;
    try {
      f = new File(zipFile.getCanonicalFile());
    } catch (IOException ioe) {
      logger.error("Unable to create the zip file: {}", destination.getAbsolutePath());
      throw new IOException("Unable to create the zip file: {}" + destination.getAbsolutePath(), ioe);
    }

    try {
      if (f.isArchive() && f.isDirectory()) {
        if (destination.exists()) {
          if (!destination.isDirectory()) {
            logger.error("Destination file must be a directory");
            throw new IllegalArgumentException("Destination file must be a directory");
          }
        }

        try {
          destination.mkdirs();
        } catch (SecurityException e) {
          logger.error("Cannot create destination directory: {}", e.getMessage());
          throw new IOException("Cannot create destination directory", e);
        }

        success = f.copyAllTo(destination);

        if (success)
          logger.debug("File {} unzipped successfully", zipFile.getCanonicalPath());
        else {
          logger.warn("File {} was not correctly unzipped", zipFile.getCanonicalPath());
          throw new IOException("File " + zipFile.getCanonicalPath() + " was not correctly unzipped");
        }
      } else {
        logger.error("The input file is not a valid zip file");
        throw new IllegalArgumentException("The input file is not a valid zip file");
      }
    } catch (IOException e) {
      throw (e);
    } finally {
      umount(f);
    }

  }

  /************************************************************************************* */
  /* "ALIASES" - For different types of input, but actually calling the previous methods */
  /***************************************************************************************/

  /**
   * Compresses source files into a zip archive (no recursive, default compression)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, String destination) throws IOException {
    return zip(sourceFiles, destination, false, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive (no recursive)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, String destination, int level) throws IOException {
    return zip(sourceFiles, destination, false, level);
  }

  /**
   * Compresses source files into a zip archive (default compression)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param recursive
   *          A {@link boolean} indicating whether or not recursively zipping nested directories
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, String destination, boolean recursive) throws IOException {
    return zip(sourceFiles, destination, recursive, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, String destination, boolean recursive, int level)
          throws IOException {

    if (sourceFiles == null) {
      logger.error("The input String array cannot be null");
      throw new IllegalArgumentException("The input String array cannot be null");
    }

    if (destination == null) {
      logger.error("Destination file cannot be null");
      throw new IllegalArgumentException("Destination file cannot be null");
    }

    if ("".equals(destination)) {
      logger.error("Destination file name must be set");
      throw new IllegalArgumentException("Destination file name must be set");
    }

    Vector<java.io.File> files = new Vector<java.io.File>();
    for (String name : sourceFiles) {
      if (name == null) {
        logger.error("One of the input file names is null");
        throw new IllegalArgumentException("One of the input file names is null");
      } else if ("".equals(name)) {
        logger.error("One of the input file names is blank");
        throw new IllegalArgumentException("One of the input file names is blank");
      }
      files.add(new java.io.File(name));
    }

    return zip(files.toArray(new java.io.File[files.size()]), new java.io.File(destination), recursive, level);

  }

  /**
   * Compresses source files into a zip archive (no recursive, default compression)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, java.io.File destination) throws IOException {
    return zip(sourceFiles, destination, false, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive (no recursive)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, java.io.File destination, int level) throws IOException {
    return zip(sourceFiles, destination, false, level);
  }

  /**
   * Compresses source files into a zip archive (default compression)
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, java.io.File destination, boolean recursive) throws IOException {
    return zip(sourceFiles, destination, recursive, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive
   *
   * @param sourceFiles
   *          A {@link String} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(String[] sourceFiles, java.io.File destination, boolean recursive, int level)
          throws IOException {

    if (sourceFiles == null) {
      logger.error("The input String array cannot be null");
      throw new IllegalArgumentException("The input String array cannot be null");
    }

    Vector<java.io.File> files = new Vector<java.io.File>();
    for (String name : sourceFiles) {
      if (name == null) {
        logger.error("One of the input file names is null");
        throw new IllegalArgumentException("One of the input file names is null");
      } else if ("".equals(name)) {
        logger.error("One of the input file names is blank");
        throw new IllegalArgumentException("One of the input file names is blank");
      }
      files.add(new java.io.File(name));
    }

    return zip(files.toArray(new java.io.File[files.size()]), destination, recursive, level);

  }

  /**
   * Compresses source files into a zip archive (no recursive, default compression)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination) throws IOException {
    return zip(sourceFiles, destination, false, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive (no recursive)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination, int level) throws IOException {
    return zip(sourceFiles, destination, false, level);
  }

  /**
   * Compresses source files into a zip archive (default compression)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination, boolean recursive) throws IOException {
    return zip(sourceFiles, destination, recursive, DEFAULT_COMPRESSION);

  }

  /**
   * Compresses source files into a zip archive
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link String} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination, boolean recursive, int level)
          throws IOException {

    if (destination == null) {
      logger.error("Destination file cannot be null");
      throw new IllegalArgumentException("Destination file cannot be null");
    }

    if ("".equals(destination)) {
      logger.error("Destination file name must be set");
      throw new IllegalArgumentException("Destination file name must be set");
    }

    return zip(sourceFiles, new java.io.File(destination), recursive, level);

  }

  /**
   * Compresses source files into a zip archive (no recursive, default compression)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, java.io.File destination) throws IOException {
    return zip(sourceFiles, destination, false, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive (default compression)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @param recursive
   *          Indicate whether or not recursively zipping nested directories
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, java.io.File destination, boolean recursive)
          throws IOException {
    return zip(sourceFiles, destination, recursive, DEFAULT_COMPRESSION);
  }

  /**
   * Compresses source files into a zip archive (no recursive)
   *
   * @param sourceFiles
   *          A {@link java.io.File} array with the file names to be included in the root of the archive
   * @param destination
   *          A {@link java.io.File} with the path name of the resulting zip file
   * @param level
   *          The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
   * @return A {@link java.io.File} descriptor of the zip archive file
   * @throws IOException
   *           If the zip file can not be created, or the input files names can not be correctly parsed
   */
  public static java.io.File zip(java.io.File[] sourceFiles, java.io.File destination, int level) throws IOException {
    return zip(sourceFiles, destination, false, level);
  }

  /**
   * Extracts a zip file to a directory.
   *
   * @param zipFile
   *          A {@link String} with the path to the source zip archive
   * @param destination
   *          A {@link String} with the location where the zip archive will be extracted. If this destination directory
   *          does not exist, it will be created.
   * @throws IOException
   *           if the zip file cannot be read, the destination directory cannot be created or the extraction is not
   *           successful
   */
  public static void unzip(String zipFile, String destination) throws IOException {

    if (zipFile == null) {
      logger.error("Input filename cannot be null");
      throw new IllegalArgumentException("Input filename cannot be null");
    }

    if ("".equals(zipFile)) {
      logger.error("Input filename cannot be empty");
      throw new IllegalArgumentException("Input filename cannot be empty");
    }

    if (destination == null) {
      logger.error("Output filename cannot be null");
      throw new IllegalArgumentException("Output filename cannot be null");
    }

    if ("".equals(destination)) {
      logger.error("Output filename cannot be empty");
      throw new IllegalArgumentException("Output filename cannot be empty");
    }

    unzip(new java.io.File(zipFile), new java.io.File(destination));

  }

  /**
   * Extracts a zip file to a directory.
   *
   * @param zipFile
   *          A {@link java.io.File} with the path to the source zip archive
   * @param destination
   *          A {@link String} with the location where the zip archive will be extracted.
   * @throws IOException
   *           if the zip file cannot be read, the destination directory cannot be created or the extraction is not
   *           successful
   */
  public static void unzip(java.io.File zipFile, String destination) throws IOException {

    if (destination == null) {
      logger.error("Output filename cannot be null");
      throw new IllegalArgumentException("Output filename cannot be null");
    }

    if ("".equals(destination)) {
      logger.error("Output filename cannot be empty");
      throw new IllegalArgumentException("Output filename cannot be empty");
    }

    unzip(zipFile, new java.io.File(destination));

  }

  /**
   * Extracts a zip file to a directory.
   *
   * @param zipFile
   *          A {@link String} with the path to the source zip archive
   * @param destination
   *          A {@link java.io.File} with the location where the zip archive will be extracted.
   * @throws IOException
   *           if the zip file cannot be read, the destination directory cannot be created or the extraction is not
   *           successful
   */
  public static void unzip(String zipFile, java.io.File destination) throws IOException {

    if (zipFile == null) {
      logger.error("Input filename cannot be null");
      throw new IllegalArgumentException("Input filename cannot be null");
    }

    if ("".equals(zipFile)) {
      logger.error("Input filename cannot be empty");
      throw new IllegalArgumentException("Input filename cannot be empty");
    }

    unzip(new java.io.File(zipFile), destination);

  }

}
