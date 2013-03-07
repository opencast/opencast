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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * Utility class, dealing with files.
 */
public final class FileSupport {

  /** Only files will be deleted, the directory structure remains untouched. */
  public static final int DELETE_FILES = 0;

  /** Delete everything including the root directory. */
  public static final int DELETE_ROOT = 1;

  /** Delete all content but keep the root directory. */
  public static final int DELETE_CONTENT = 2;

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
   * Moves the specified file or directory from <code>sourceLocation</code> to <code>targetDirectory</code>. If
   * <code>targetDirectory</code> does not exist, it will be created.
   * 
   * @param sourceLocation
   *          the source file or directory
   * @param targetDirectory
   *          the target directory
   * @return the moved file
   */
  public static File move(File sourceLocation, File targetDirectory) throws IOException {
    if (!targetDirectory.isDirectory())
      throw new IllegalArgumentException("Target location must be a directory");

    if (!targetDirectory.exists())
      targetDirectory.mkdirs();

    File targetFile = new File(targetDirectory, sourceLocation.getName());
    if (sourceLocation.renameTo(targetFile))
      return targetFile;

    // Rename doesn't work, so use copy / delete instead
    copy(sourceLocation, targetDirectory);
    delete(sourceLocation, true);
    return targetFile;
  }

  /**
   * Copies the specified file from <code>sourceLocation</code> to <code>targetLocation</code> and returns a reference
   * to the newly created file or directory.
   * <p/>
   * If <code>targetLocation</code> is an existing directory, then the source file or directory will be copied into this
   * directory, otherwise the source file will be copied to the file identified by <code>targetLocation</code>.
   * <p/>
   * Note that existing files and directories will be overwritten.
   * <p/>
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
   * <p/>
   * If <code>targetLocation</code> is an existing directory, then the source file or directory will be copied into this
   * directory, otherwise the source file will be copied to the file identified by <code>targetLocation</code>.
   * <p/>
   * If <code>overwrite</code> is set to <code>false</code>, this method throws an {@link IOException} if the target
   * file already exists.
   * <p/>
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

    // This variable is used when the channel copy files, and stores the maximum size of the file parts copied from source to target
    final int chunk = 1024 * 1024 * 512; // 512 MB
    
    // This variable is used when the cannel copy fails completely, as the size of the memory buffer used to copy the data from one stream to the other.
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
          // This has to be in "finally", because in 64-bit machines the channel copy may fail to copy the whole file without causing a exception
          if ((sourceChannel != null) && (targetChannel != null) && (size < sourceFile.length()))
            // Failing back to using FileChannels *but* with chunks and not altogether
            logger.info("Trying to copy the file in chunks using Channels");
          if (size != sourceFile.length()) {
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
   * Copies recursively the <em>content</em> of the specified <code>sourceDirectory</code> to
   * <code>targetDirectory</code>.
   * <p/>
   * If <code>overwrite</code> is set to <code>false</code>, this method throws an {@link IOException} if the target
   * file already exists.
   * 
   * @param sourceDirectory
   *          the source directory
   * @param targetDirectory
   *          the target directory to copy the content of the source directory to
   * @param overwrite
   *          <code>true</code> to overwrite existing files
   * @throws IOException
   *           if copying fails
   */
  public static void copyContent(File sourceDirectory, File targetDirectory, boolean overwrite) throws IOException {
    if (sourceDirectory == null)
      throw new IllegalArgumentException("Source directory must not by null");
    if (!sourceDirectory.isDirectory())
      throw new IllegalArgumentException(sourceDirectory.getAbsolutePath() + " is not a directory");
    if (targetDirectory == null)
      throw new IllegalArgumentException("Target directory must not by null");
    if (!targetDirectory.isDirectory())
      throw new IllegalArgumentException(targetDirectory.getAbsolutePath() + " is not a directory");

    for (File content : sourceDirectory.listFiles()) {
      copy(content, targetDirectory, overwrite);
    }
  }

  /**
   * Copies recursively the <em>content</em> of the specified <code>sourceDirectory</code> to
   * <code>targetDirectory</code>.
   * <p/>
   * Note that existing files and directories will be overwritten.
   * 
   * @param sourceDirectory
   *          the source directory
   * @param targetDirectory
   *          the target directory to copy the content of the source directory to
   * @throws IOException
   *           if copying fails
   */
  public static void copyContent(File sourceDirectory, File targetDirectory) throws IOException {
    copyContent(sourceDirectory, targetDirectory, false);
  }

  /**
   * Links recursively the <em>content</em> of the specified <code>sourceDirectory</code> to
   * <code>targetDirectory</code>.
   * <p/>
   * If <code>overwrite</code> is set to <code>false</code>, this method throws an {@link IOException} if the target
   * file already exists.
   * 
   * @param sourceDirectory
   *          the source directory
   * @param targetDirectory
   *          the target directory to link the content of the source directory to
   * @param overwrite
   *          <code>true</code> to overwrite existing files
   * @throws IOException
   *           if copying fails
   */
  public static void linkContent(File sourceDirectory, File targetDirectory, boolean overwrite) throws IOException {
    if (sourceDirectory == null)
      throw new IllegalArgumentException("Source directory must not by null");
    if (!sourceDirectory.isDirectory())
      throw new IllegalArgumentException(sourceDirectory.getAbsolutePath() + " is not a directory");
    if (targetDirectory == null)
      throw new IllegalArgumentException("Target directory must not by null");
    if (targetDirectory.exists() && !targetDirectory.isDirectory())
      throw new IllegalArgumentException(targetDirectory.getAbsolutePath() + " is not a directory");

    // Create the target directory if it doesn't exist yet
    if (!targetDirectory.exists()) {
      FileUtils.forceMkdir(targetDirectory);
    }

    logger.trace("Linking files in " + sourceDirectory + " to " + targetDirectory);
    Process p = null;
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    StringBuffer error = new StringBuffer();
    try {
      p = createLinkDirectoryProcess(sourceDirectory, targetDirectory, overwrite);
      stdout = new StreamHelper(p.getInputStream());
      stderr = new LinkErrorStreamHelper(p.getErrorStream(), error);
      p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      // Find does not return with an error if -exec fails
      if (p.exitValue() != 0 || error.length() > 0) {
        logger.debug("Unable to link files from " + sourceDirectory + " to " + targetDirectory + ": " + error);
        copyContent(sourceDirectory, targetDirectory, overwrite);
      }
    } catch (InterruptedException e) {
      throw new IOException("Interrupted while creating links from " + sourceDirectory + " to " + targetDirectory
              + ": " + e.getMessage());
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }

    // Link nested directories
    File[] children = sourceDirectory.listFiles();
    for (int i = 0; i < children.length; i++) {
      if (children[i].isDirectory())
        link(children[i], targetDirectory, overwrite);
    }
    // return targetDirectory;
  }

  /**
   * Links recursively the <em>content</em> of the specified <code>sourceDirectory</code> to
   * <code>targetDirectory</code>.
   * <p/>
   * Note that existing files and directories will be overwritten.
   * 
   * @param sourceDirectory
   *          the source directory
   * @param targetDirectory
   *          the target directory to link the content of the source directory to
   * @throws IOException
   *           if copying fails
   */
  public static void linkContent(File sourceDirectory, File targetDirectory) throws IOException {
    linkContent(sourceDirectory, targetDirectory, false);
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
  public static File link(File sourceLocation, File targetLocation, boolean overwrite) throws IOException {
    if (sourceLocation == null)
      throw new IllegalArgumentException("Source location must not by null");
    if (targetLocation == null)
      throw new IllegalArgumentException("Target location must not by null");
    
    File dest = determineDestination(targetLocation, sourceLocation, overwrite);

    // Special treatment for directories as sources
    if (sourceLocation.isDirectory()) {
      if (!dest.exists()) {
        dest.mkdir();
      }
      logger.trace("Linking files in " + sourceLocation + " to " + dest);
      Process p = null;
      StreamHelper stdout = null;
      StreamHelper stderr = null;
      StringBuffer error = new StringBuffer();
      try {
        p = createLinkDirectoryProcess(sourceLocation, dest, overwrite);
        stdout = new StreamHelper(p.getInputStream());
        stderr = new LinkErrorStreamHelper(p.getErrorStream(), error);
        p.waitFor();
        stdout.stopReading();
        stderr.stopReading();
        // Find does not return with an error if -exec fails
        if (p.exitValue() != 0 || error.length() > 0) {
          logger.debug("Unable to link files from " + sourceLocation + " to " + dest + ": " + error);
          copy(sourceLocation, dest, overwrite);
        }
      } catch (InterruptedException e) {
        throw new IOException("Interrupted while creating links from " + sourceLocation + " to " + dest + ": "
                + e.getMessage());
      } finally {
        IoSupport.closeQuietly(stdout);
        IoSupport.closeQuietly(stderr);
        IoSupport.closeQuietly(p);
      }

      // Link nested directories
      File[] children = sourceLocation.listFiles();
      for (int i = 0; i < children.length; i++) {
        if (children[i].isDirectory())
          link(children[i], dest, overwrite);
      }
    }

    // Normal file
    else {
      logger.trace("Creating link from " + sourceLocation + " to " + dest);
      Process p = null;
      StreamHelper stdout = null;
      StreamHelper stderr = null;
      StringBuffer error = new StringBuffer();
      try {
        p = createLinkFileProcess(sourceLocation, dest, overwrite);
        stdout = new StreamHelper(p.getInputStream());
        stderr = new LinkErrorStreamHelper(p.getErrorStream(), error);
        p.waitFor();
        stdout.stopReading();
        stderr.stopReading();
        // Find does not return with an error if -exec fails
        if (p.exitValue() != 0 || error.length() > 0) {
          logger.debug("Unable to create a link from " + sourceLocation + " to " + dest + ": " + error);
          copy(sourceLocation, dest, overwrite);
        }
        if (sourceLocation.length() != dest.length()) {
          logger.warn("Source " + sourceLocation + " and target " + dest + " do not have the same length");
          // TOOD: Why would this happen?
          // throw new IOException("Source " + sourceLocation + " and target " +
          // dest + " do not have the same length");
        }
      } catch (InterruptedException e) {
        throw new IOException("Interrupted while creating a link from " + sourceLocation + " to " + dest + ": " + error);
      } finally {
        IoSupport.closeQuietly(stdout);
        IoSupport.closeQuietly(stderr);
        IoSupport.closeQuietly(p);
      }
    }
    return dest;
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
   * @throws IOException
   *           if linking of the file failed
   */
  public static boolean supportsLinking(File sourceLocation, File targetLocation) {
    if (sourceLocation == null)
      throw new IllegalArgumentException("Source location must not by null");
    if (targetLocation == null)
      throw new IllegalArgumentException("Target location must not by null");
    if (!sourceLocation.exists())
      throw new IllegalArgumentException("Source " + sourceLocation + " does not exist");

    logger.trace("Creating link from " + sourceLocation + " to " + targetLocation);
    Process p = null;
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    StringBuffer error = new StringBuffer();
    try {
      p = createLinkFileProcess(sourceLocation, targetLocation, true);
      stdout = new StreamHelper(p.getInputStream());
      stderr = new LinkErrorStreamHelper(p.getErrorStream(), error);
      p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      // Find does not return with an error if -exec fails
      if (p.exitValue() != 0 || error.length() > 0) {
        logger.debug("Unable to create a link from " + sourceLocation + " to " + targetLocation + ": " + error);
        return false;
      }
      if (sourceLocation.length() != targetLocation.length()) {
        logger.warn("Source " + sourceLocation + " and target " + targetLocation + " do not have the same length");
        // TOOD: Why would this happen?
        // throw new IOException("Source " + sourceLocation + " and target " +
        // dest + " do not have the same length");
      }
    } catch (Exception e) {
      logger.debug("Unable to create a link from " + sourceLocation + " to " + targetLocation + ": " + error.toString());
      return false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
    return true;
  }

  /**
   * @param sourceLocation The location of the file you want to link. 
   * @param targetLocation The location and name to place the link. 
   * @param overwrite Whether to overwrite a link if it exists. 
   * @return Returns a process that should link the two 
   * @throws IOException
   */
  private static Process createLinkFileProcess(File sourceLocation, File targetLocation, boolean overwrite) throws IOException {
    Process p;
    if (!System.getProperty("os.name").startsWith("Windows")) {
      if (overwrite) {
        p = new ProcessBuilder("ln", "-f", sourceLocation.getAbsolutePath(), targetLocation.getAbsolutePath()).start();
      } else {
        p = new ProcessBuilder("ln", sourceLocation.getAbsolutePath(), targetLocation.getAbsolutePath()).start();
      }
    } else {
      /** 
       * Handle the windows special case by using mklink instead of ln. mklink is also a command in the cmd.exe command
       * shell, not a separate application so we need to run a command shell with the /C switch to be able to use the
       * utility. There also is no force in windows. **/
      p = new ProcessBuilder("cmd", "/C", "mklink", "/H", targetLocation.getAbsolutePath(), sourceLocation.getAbsolutePath()).start();
    }
    return p;
  }

  private static Process createLinkDirectoryProcess(File sourceDirectory, File targetDirectory, boolean overwrite)
  throws IOException {
    Process p;
    if (!System.getProperty("os.name").startsWith("Windows")) {
      if (overwrite) {
        p = new ProcessBuilder("find", sourceDirectory.getAbsolutePath(), "-maxdepth", "1", "-type", "f", "-follow",
                "-exec", "ln", "-fF", "{}", targetDirectory.getAbsolutePath() + File.separator, ";").start();
      } else {
        p = new ProcessBuilder("find", sourceDirectory.getAbsolutePath(), "-maxdepth", "1", "-type", "f", "-follow",
                "-exec", "ln", "{}", targetDirectory.getAbsolutePath() + File.separator, ";").start();
      }
    } else {
      /** 
       * Handle the windows special case by using mklink instead of ln. mklink is also a command in the cmd.exe command
       * shell, not a separate application so we need to run a command shell with the /C switch to be able to use the
       * utility. There also is no force in windows. **/
      p = new ProcessBuilder("cmd", "/C", "mklink", "/J", sourceDirectory.getAbsolutePath(), targetDirectory.getAbsolutePath()).start();
    }
    return p;
  }

  private static File determineDestination(File targetLocation, File sourceLocation, boolean overwrite)
  throws IOException {
    File dest = null;

    // Source location exists
    if (sourceLocation.exists()) {
      // Is the source file/directory readable
      if (sourceLocation.canRead()) {
        // If a directory...
        if (targetLocation.isDirectory())
          // Create a destination file within it, with the same name of the source target
          dest = new File(targetLocation, sourceLocation.getName());
        else 
          // targetLocation is either a normal file or doesn't exist
          dest = targetLocation;
        
        // Source and target locations can not be the same
        if (sourceLocation.equals(dest))
          throw new IOException("Source and target locations must be different");
        
        // Search the first existing parent of the target file, to check if it can be written
        // getParentFile can return null even though there *is* a parent file, if the file is not absolute
        // That's the reason why getAbsoluteFile is used here
        for (File iter = dest.getAbsoluteFile(); iter != null; iter = iter.getParentFile())
          if (iter.exists()) {
            if (iter.canWrite())
              break;
            else  
              throw new IOException("Destination " + dest + "cannot be written/modified");
          }
        
        // Check the target file can be overwritten
        if (dest.exists() && !dest.isDirectory() && !overwrite)
          throw new IOException("Destination " + dest + " already exists");
        
      } else
        throw new IOException(sourceLocation + " cannot be read");
    } else
      throw new IOException("Source " + sourceLocation + " does not exist");
    
    return dest;
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
  public static boolean delete(File f) {
    return delete(f, false);
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
  public static boolean delete(File f, boolean recurse) {
    if (!f.exists())
      return false;
    if (f.isDirectory()) {
      String[] children = f.list();
      if (children.length > 0 && !recurse)
        return false;
      for (String child : children) {
        delete(new File(f, child), true);
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

  /**
   * Returns true if both canonical file paths are equal.
   * 
   * @param a
   *          the first file or null
   * @param b
   *          the second file or null
   */
  public static boolean equals(File a, File b) {
    try {
      return a != null && b != null && a.getCanonicalPath().equals(b.getCanonicalPath());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Special implementation of the stream helper that will swallow some of the videosegmenter's output erroneously
   * written to stderr.
   */
  private static class LinkErrorStreamHelper extends StreamHelper {

    /**
     * Creates a new stream helper that will swallow some well known error messages while linking.
     * 
     * @param stream
     *          the content stream
     * @param output
     *          the output buffer
     */
    public LinkErrorStreamHelper(InputStream stream, StringBuffer output) {
      super(stream, output);
    }

    /**
     * @see org.opencastproject.util.StreamHelper#log(java.lang.String)
     */
    protected void log(String output) {
      if (output.endsWith("Invalid cross-device link"))
        return;
      else
        super.log(output);
    }
  }

}
