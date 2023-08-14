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

import static java.lang.String.format;
import static java.nio.file.Files.createLink;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility class, dealing with files. */
public final class FileSupport {

  /** Only files will be deleted, the directory structure remains untouched. */
  public static final int DELETE_FILES = 0;

  /** Delete everything including the root directory. */
  public static final int DELETE_ROOT = 1;

  /** Name of the java environment variable for the temp directory */
  private static final String IO_TMPDIR = "java.io.tmpdir";

  /** Work directory */
  private static File tmpDir = null;

  /** Logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(FileSupport.class);

  /** Disable construction of this utility class */
  private FileSupport() {
  }

  /**
   * Copies the specified file from <code>sourceLocation</code> to <code>targetLocation</code> and returns a reference
   * to the newly created file or directory.
   * <p>
   * If <code>targetLocation</code> is an existing directory, then the source file or directory will be copied into this
   * directory, otherwise the source file will be copied to the file identified by <code>targetLocation</code>.
   * <p>
   * Note that existing files and directories will be overwritten.
   * <p>
   * Also note that if <code>targetLocation</code> is a directory than the directory itself, not only its content is
   * copied.
   *
   * @param sourceLocation
   *          the source file or directory
   * @param targetLocation
   *          the directory to copy the source file or directory to
   * @return the created copy
   * @throws IOException
   *           if copying of the file or directory failed
   */
  public static File copy(File sourceLocation, File targetLocation) throws IOException {
    return copy(sourceLocation, targetLocation, true);
  }

  /**
   * Copies the specified <code>sourceLocation</code> to <code>targetLocation</code> and returns a reference to the
   * newly created file or directory.
   * <p>
   * If <code>targetLocation</code> is an existing directory, then the source file or directory will be copied into this
   * directory, otherwise the source file will be copied to the file identified by <code>targetLocation</code>.
   * <p>
   * If <code>overwrite</code> is set to <code>false</code>, this method throws an {@link IOException} if the target
   * file already exists.
   * <p>
   * Note that if <code>targetLocation</code> is a directory than the directory itself, not only its content is copied.
   *
   * @param sourceFile
   *          the source file or directory
   * @param targetFile
   *          the directory to copy the source file or directory to
   * @param overwrite
   *          <code>true</code> to overwrite existing files
   * @return the created copy
   * @throws IOException
   *           if copying of the file or directory failed
   */
  public static File copy(File sourceFile, File targetFile, boolean overwrite) throws IOException {

    // This variable is used when the channel copy files, and stores the maximum size of the file parts copied from
    // source to target
    final int chunk = 1024 * 1024 * 512; // 512 MB

    // This variable is used when the cannel copy fails completely, as the size of the memory buffer used to copy the
    // data from one stream to the other.
    final int bufferSize = 1024 * 1024; // 1 MB

    File dest = determineDestination(targetFile, sourceFile, overwrite);

    // We are copying a directory
    if (sourceFile.isDirectory()) {
      if (!dest.exists()) {
        dest.mkdirs();
      }
      File[] children = sourceFile.listFiles();
      for (File child : children) {
        copy(child, dest, overwrite);
      }
    }
    // We are copying a file
    else {
      // If dest is not an "absolute file", getParentFile may return null, even if there *is* a parent file.
      // That's why "getAbsoluteFile" is used here
      dest.getAbsoluteFile().getParentFile().mkdirs();
      if (dest.exists())
        delete(dest);

      FileChannel sourceChannel = null;
      FileChannel targetChannel = null;
      FileInputStream sourceStream = null;
      FileOutputStream targetStream = null;
      long size = 0;

      try {
        sourceStream = new FileInputStream(sourceFile);
        targetStream = new FileOutputStream(dest);
        try {
          sourceChannel = sourceStream.getChannel();
          targetChannel = targetStream.getChannel();
          size = targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException ioe) {
          logger.warn("Got IOException using Channels for copying.");
        } finally {
          // This has to be in "finally", because in 64-bit machines the channel copy may fail to copy the whole file
          // without causing a exception
          if ((sourceChannel != null) && (targetChannel != null) && (size < sourceFile.length())) {
            // Failing back to using FileChannels *but* with chunks and not altogether
            logger.info("Trying to copy the file in chunks using Channels");
            while (size < sourceFile.length())
              size += targetChannel.transferFrom(sourceChannel, size, chunk);
          }
        }
      } catch (IOException ioe) {
        if ((sourceStream != null) && (targetStream != null) && (size < sourceFile.length())) {
          logger.warn("Got IOException using Channels for copying in chunks. Trying to use stream copy instead...");
          int copied = 0;
          byte[] buffer = new byte[bufferSize];
          while ((copied = sourceStream.read(buffer, 0, buffer.length)) != -1)
            targetStream.write(buffer, 0, copied);
        } else
          throw ioe;
      } finally {
        if (sourceChannel != null)
          sourceChannel.close();
        if (sourceStream != null)
          sourceStream.close();
        if (targetChannel != null)
          targetChannel.close();
        if (targetStream != null)
          targetStream.close();
      }

      if (sourceFile.length() != dest.length()) {
        logger.warn("Source " + sourceFile + " and target " + dest + " do not have the same length");
        // TOOD: Why would this happen?
        // throw new IOException("Source " + sourceLocation + " and target " +
        // dest + " do not have the same length");
      }
    }
    return dest;
  }

  /**
   * Links the specified file or directory from <code>sourceLocation</code> to <code>targetLocation</code>. If
   * <code>targetLocation</code> does not exist, it will be created, if the target file already exists, an
   * {@link IOException} will be thrown.
   * <p>
   * If this fails (because linking is not supported on the current filesystem, then a copy is made.
   * </p>
   *
   * @param sourceLocation
   *          the source file or directory
   * @param targetLocation
   *          the targetLocation
   * @return the created link
   * @throws IOException
   *           if linking of the file or directory failed
   */
  public static File link(File sourceLocation, File targetLocation) throws IOException {
    return link(sourceLocation, targetLocation, false);
  }

  /**
   * Links the specified file or directory from <code>sourceLocation</code> to <code>targetLocation</code>. If
   * <code>targetLocation</code> does not exist, it will be created.
   * <p>
   * If this fails (because linking is not supported on the current filesystem, then a copy is made.
   * </p>
   * If <code>overwrite</code> is set to <code>false</code>, this method throws an {@link IOException} if the target
   * file already exists.
   *
   * @param sourceLocation
   *          the source file or directory
   * @param targetLocation
   *          the targetLocation
   * @param overwrite
   *          <code>true</code> to overwrite existing files
   * @return the created link
   * @throws IOException
   *           if linking of the file or directory failed
   */
  public static File link(final File sourceLocation, final File targetLocation, final boolean overwrite)
          throws IOException {
    final Path sourcePath = requireNonNull(sourceLocation).toPath();
    final Path targetPath = requireNonNull(targetLocation).toPath();

    if (exists(sourcePath)) {
      if (overwrite) {
        deleteIfExists(targetPath);
      } else {
        if (exists(targetPath)) {
          throw new IOException(format("There is already a file/directory at %s", targetPath));
        }
      }

      try {
        createLink(targetPath, sourcePath);
      } catch (UnsupportedOperationException e) {
        logger.debug("Copy file because creating hard-links is not supported by the current file system: {}",
                ExceptionUtils.getMessage(e));
        Files.copy(sourcePath, targetPath);
      } catch (IOException e) {
        logger.debug("Copy file because creating a hard-link at '{}' for existing file '{}' did not work:",
                targetPath, sourcePath, e);
        if (overwrite) {
          Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
        } else {
          Files.copy(sourcePath, targetPath);
        }
      }
    } else {
      throw new IOException(format("No file/directory found at %s", sourcePath));
    }

    return targetPath.toFile();
  }

  /**
   * Returns <code>true</code> if the operating system as well as the disk layout support creating a hard link from
   * <code>src</code> to <code>dest</code>. Note that this implementation requires two files rather than directories and
   * will overwrite any existing file that might already be present at the destination.
   *
   * @param sourceLocation
   *          the source file
   * @param targetLocation
   *          the target file
   * @return <code>true</code> if the link was created, <code>false</code> otherwhise
   */
  public static boolean supportsLinking(File sourceLocation, File targetLocation) {
    final Path sourcePath = requireNonNull(sourceLocation).toPath();
    final Path targetPath = requireNonNull(targetLocation).toPath();

    if (!exists(sourcePath))
      throw new IllegalArgumentException(format("Source %s does not exist", sourcePath));

    logger.debug("Creating hard link from {} to {}", sourcePath, targetPath);
    try {
      deleteIfExists(targetPath);
      createLink(targetPath, sourcePath);
    } catch (Exception e) {
      logger.debug("Unable to create a link from {} to {}: {}", sourcePath, targetPath, e);
      return false;
    }

    return true;
  }

  private static File determineDestination(File targetLocation, File sourceLocation, boolean overwrite)
          throws IOException {
    File dest = null;

    // Source location exists
    if (sourceLocation.exists()) {
      // Is the source file/directory readable
      if (sourceLocation.canRead()) {
        // If a directory...
        if (targetLocation.isDirectory()) {
          // Create a destination file within it, with the same name of the source target
          dest = new File(targetLocation, sourceLocation.getName());
        } else {
          // targetLocation is either a normal file or doesn't exist
          dest = targetLocation;
        }

        // Source and target locations can not be the same
        if (sourceLocation.equals(dest)) {
          throw new IOException("Source and target locations must be different");
        }

        // Search the first existing parent of the target file, to check if it can be written
        // getParentFile can return null even though there *is* a parent file, if the file is not absolute
        // That's the reason why getAbsoluteFile is used here
        for (File iter = dest.getAbsoluteFile(); iter != null; iter = iter.getParentFile()) {
          if (iter.exists()) {
            if (iter.canWrite()) {
              break;
            } else {
              throw new IOException("Destination " + dest + "cannot be written/modified");
            }
          }
        }

        // Check the target file can be overwritten
        if (dest.exists() && !dest.isDirectory() && !overwrite) {
          throw new IOException("Destination " + dest + " already exists");
        }

      } else {
        throw new IOException(sourceLocation + " cannot be read");
      }
    } else {
      throw new IOException("Source " + sourceLocation + " does not exist");
    }

    return dest;
  }

  /**
   * Delete all directories from <code>start</code> up to directory <code>limit</code> if they are empty. Directory
   * <code>limit</code> is <em>exclusive</em> and will not be deleted.
   *
   * @return true if the <em>complete</em> hierarchy has been deleted. false in any other case.
   */
  public static boolean deleteHierarchyIfEmpty(File limit, File start) {
    return limit.isDirectory()
            && start.isDirectory()
            && (isEqual(limit, start) || (isParent(limit, start) && start.list().length == 0 && start.delete() && deleteHierarchyIfEmpty(
                    limit, start.getParentFile())));
  }

  /** Compare two files by their canonical paths. */
  public static boolean isEqual(File a, File b) {
    try {
      return a.getCanonicalPath().equals(b.getCanonicalPath());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Check if <code>a</code> is a parent of <code>b</code>. This can only be the case if <code>a</code> is a directory
   * and a sub path of <code>b</code>. <code>isParent(a, a) == true</code>.
   */
  public static boolean isParent(File a, File b) {
    try {
      final String aCanonical = a.getCanonicalPath();
      final String bCanonical = b.getCanonicalPath();
      return (!aCanonical.equals(bCanonical) && bCanonical.startsWith(aCanonical));
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Deletes the specified file and returns <code>true</code> if the file was deleted.
   * <p>
   * If <code>f</code> is a directory, it will only be deleted if it doesn't contain any other files or directories. To
   * do a recursive delete, you may use {@link #delete(File, boolean)}.
   *
   * @param f
   *          the file or directory
   * @see #delete(File, boolean)
   */
  public static boolean delete(File f) throws IOException {
    return delete(f, false);
  }

  /**
   * Like {@link #delete(File)} but does not throw any IO exceptions.
   * In case of an IOException it will only be logged at warning level and the method returns false.
   */
  public static boolean deleteQuietly(File f) {
    return deleteQuietly(f, false);
  }

  /**
   * Like {@link #delete(File, boolean)} but does not throw any IO exceptions.
   * In case of an IOException it will only be logged at warning level and the method returns false.
   */
  public static boolean deleteQuietly(File f, boolean recurse) {
    try {
      return delete(f, recurse);
    } catch (IOException e) {
      logger.warn("Cannot delete " + f.getAbsolutePath() + " because of IOException"
                       + (e.getMessage() != null ? " " + e.getMessage() : ""));
      return false;
    }
  }

  /**
   * Deletes the specified file and returns <code>true</code> if the file was deleted.
   * <p>
   * In the case that <code>f</code> references a directory, it will only be deleted if it doesn't contain other files
   * or directories, unless <code>recurse</code> is set to <code>true</code>.
   * </p>
   *
   * @param f
   *          the file or directory
   * @param recurse
   *          <code>true</code> to do a recursive deletes for directories
   */
  public static boolean delete(File f, boolean recurse) throws IOException {
    if (f == null)
      return false;
    if (!f.exists())
      return false;
    if (f.isDirectory()) {
      String[] children = f.list();
      if (children == null) {
        throw new IOException("Cannot list content of directory " + f.getAbsolutePath());
      }
      if (children != null) {
        if (children.length > 0 && !recurse)
          return false;
        for (String child : children) {
          delete(new File(f, child), true);
        }
      } else {
        logger.debug("Unexpected null listing files in {}", f.getAbsolutePath());
      }
    }
    return f.delete();
  }

  /**
   * Deletes the content of directory <code>dir</code> and, if specified, the directory itself. If <code>dir</code> is a
   * normal file it will always be deleted.
   *
   * @return true everthing was deleted, false otherwise
   */
  public static boolean delete(File dir, int mode) {
    if (dir.isDirectory()) {
      boolean ok = delete(dir.listFiles(), mode != DELETE_FILES);
      if (mode == DELETE_ROOT) {
        ok &= dir.delete();
      }
      return ok;
    } else {
      return dir.delete();
    }
  }

  /**
   * Deletes the content of directory <code>dir</code> and, if specified, the directory itself. If <code>dir</code> is a
   * normal file it will be deleted always.
   */
  private static boolean delete(File[] files, boolean deleteDir) {
    boolean ok = true;
    for (File f : files) {
      if (f.isDirectory()) {
        delete(f.listFiles(), deleteDir);
        if (deleteDir) {
          ok &= f.delete();
        }
      } else {
        ok &= f.delete();
      }
    }
    return ok;
  }

  /**
   * Sets the webapp's temporary directory. Make sure that directory exists and has write permissions turned on.
   *
   * @param tmpDir
   *          the new temporary directory
   * @throws IllegalArgumentException
   *           if the file object doesn't represent a directory
   * @throws IllegalStateException
   *           if the directory is write protected
   */
  public static void setTempDirectory(File tmpDir) throws IllegalArgumentException, IllegalStateException {
    if (tmpDir == null || !tmpDir.isDirectory())
      throw new IllegalArgumentException(tmpDir + " is not a directory");
    if (!tmpDir.canWrite())
      throw new IllegalStateException(tmpDir + " is not writable");
    FileSupport.tmpDir = tmpDir;
  }

  /**
   * Returns the webapp's temporary work directory.
   *
   * @return the temp directory
   */
  public static File getTempDirectory() {
    if (tmpDir == null) {
      setTempDirectory(new File(System.getProperty(IO_TMPDIR)));
    }
    return tmpDir;
  }

  /**
   * Returns a directory <code>subdir</code> inside the webapp's temporary work directory.
   *
   * @param subdir
   *          name of the subdirectory
   * @return the ready to use temp directory
   */
  public static File getTempDirectory(String subdir) {
    File tmp = new File(getTempDirectory(), subdir);
    if (!tmp.exists())
      tmp.mkdirs();
    if (!tmp.isDirectory())
      throw new IllegalStateException(tmp + " is not a directory!");
    if (!tmp.canRead())
      throw new IllegalStateException("Temp directory " + tmp + " is not readable!");
    if (!tmp.canWrite())
      throw new IllegalStateException("Temp directory " + tmp + " is not writable!");
    return tmp;
  }

}
