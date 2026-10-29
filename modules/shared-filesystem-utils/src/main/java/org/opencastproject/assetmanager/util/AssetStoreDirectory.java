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

package org.opencastproject.assetmanager.util;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.naming.ConfigurationException;

/**
 * A root directory of the file system based asset store together with the directory layout used inside it.
 * <p>
 * Within a root directory, assets are stored as
 * <code>&lt;root&gt;/&lt;organization&gt;/[&lt;prefix&gt;/]&lt;mediapackage&gt;/&lt;version&gt;/&lt;element&gt;</code>.
 * The optional prefix directory is derived from the first characters of the media package identifier (see
 * {@link #prefix(String, int)}) and keeps the number of entries in the organization directory small. A prefix length
 * of zero means no prefix directory, which is the default.
 * <p>
 * This class is the single place where asset manager directories are read from the configuration and where paths
 * inside them are built, so that the asset store and the workspace agree on the layout.
 */
public final class AssetStoreDirectory {

  private static final Logger logger = LoggerFactory.getLogger(AssetStoreDirectory.class);

  /** Configuration key for the base asset manager directory */
  public static final String CONFIG_ROOT_DIR = "org.opencastproject.episode.rootdir";

  /** Configuration key for the general storage directory, used if no asset manager directory is configured */
  public static final String CONFIG_STORAGE_DIR = "org.opencastproject.storage.dir";

  /** Suffix of the configuration key defining the prefix length of a directory */
  public static final String CONFIG_PREFIX_LENGTH_SUFFIX = ".prefix.length";

  /** Name of the asset manager directory below the general storage directory */
  private static final String DEFAULT_STORE_DIRECTORY = "archive";

  /** Name of the marker file recording the layout of a root directory */
  public static final String MARKER_FILE = ".opencast-asset-store";

  /** Key of the prefix length within the marker file */
  private static final String MARKER_PREFIX_LENGTH = "prefix.length";

  /** Directory created by some file systems on fresh mount points */
  private static final String LOST_AND_FOUND = "lost+found";

  /** Largest sensible prefix length. Longer prefixes would exceed file name limits and are pointless anyway. */
  public static final int MAX_PREFIX_LENGTH = 32;

  private final Path path;
  private final int prefixLength;

  /**
   * Create a root directory.
   *
   * @param path
   *          the directory; relative paths are resolved against the working directory
   * @param prefixLength
   *          the number of characters of the media package identifier used for the prefix directory, zero for none
   */
  public AssetStoreDirectory(String path, int prefixLength) {
    if (prefixLength < 0 || prefixLength > MAX_PREFIX_LENGTH) {
      throw new IllegalArgumentException(
          "Prefix length must be between 0 and " + MAX_PREFIX_LENGTH + ": " + prefixLength);
    }
    this.path = Paths.get(path).toAbsolutePath().normalize();
    this.prefixLength = prefixLength;
  }

  /** The absolute path of this root directory */
  public String getPath() {
    return path.toString();
  }

  /** The number of characters of the media package identifier used for the prefix directory, zero for none */
  public int getPrefixLength() {
    return prefixLength;
  }

  /**
   * Derive the prefix directory name from a media package identifier.
   * <p>
   * Of the first <code>length</code> characters of the identifier, ASCII upper case letters are converted to lower
   * case, and every character which is not an ASCII letter or digit is replaced by an underscore. Identifiers shorter
   * than the prefix length are padded with underscores. This guarantees that the result is a safe directory name of
   * exactly <code>length</code> characters. The rule deliberately avoids the JDK's Unicode tables, since it must
   * never change: existing data depends on it.
   *
   * @param mpId
   *          the media package identifier
   * @param length
   *          the prefix length
   * @return the prefix, or an empty string if <code>length</code> is zero
   */
  public static String prefix(String mpId, int length) {
    final StringBuilder prefix = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      final char c = i < mpId.length() ? mpId.charAt(i) : '_';
      if (c >= 'A' && c <= 'Z') {
        prefix.append((char) (c + ('a' - 'A')));
      } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        prefix.append(c);
      } else {
        prefix.append('_');
      }
    }
    return prefix.toString();
  }

  /**
   * The directory containing all versions of a media package.
   *
   * @param orgId
   *          the organization identifier
   * @param mpId
   *          the media package identifier
   * @return the media package directory
   */
  public Path mediaPackageDir(String orgId, String mpId) {
    return deletionLimit(orgId, mpId).resolve(mpId);
  }

  /**
   * The directory up to which empty parent directories of a media package may be removed when deleting assets. This
   * is the prefix directory if prefixes are used and the organization directory otherwise. Prefix directories are
   * never removed since unrelated media packages may be written to them concurrently.
   *
   * @param orgId
   *          the organization identifier
   * @param mpId
   *          the media package identifier
   * @return the directory which must not be removed
   */
  public Path deletionLimit(String orgId, String mpId) {
    final Path orgDir = path.resolve(orgId);
    return prefixLength > 0 ? orgDir.resolve(prefix(mpId, prefixLength)) : orgDir;
  }

  /**
   * Read the asset manager directories from the configuration.
   * <p>
   * The base directory is configured by {@link #CONFIG_ROOT_DIR} and defaults to <code>archive</code> below
   * {@link #CONFIG_STORAGE_DIR}. Additional directories are configured by <code>{@value #CONFIG_ROOT_DIR}.N</code>
   * with <code>N</code> starting at one and incrementing without gaps. The prefix length of a directory is configured
   * by appending {@value #CONFIG_PREFIX_LENGTH_SUFFIX} to its key.
   *
   * @param bundleContext
   *          the bundle context providing the configuration
   * @return the configured directories with the base directory first, or null if no storage directory is configured
   * @throws IllegalArgumentException
   *           if a prefix length is not a non-negative integer
   */
  public static List<AssetStoreDirectory> fromConfig(BundleContext bundleContext) {
    String rootDirectory = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_ROOT_DIR));
    if (rootDirectory == null) {
      final String storageDir = StringUtils.trimToNull(bundleContext.getProperty(CONFIG_STORAGE_DIR));
      if (storageDir == null) {
        return null;
      }
      rootDirectory = Paths.get(storageDir, DEFAULT_STORE_DIRECTORY).toString();
    }

    final List<AssetStoreDirectory> directories = new ArrayList<>();
    directories.add(new AssetStoreDirectory(rootDirectory, prefixLength(bundleContext, CONFIG_ROOT_DIR)));

    for (int index = 1;; index++) {
      final String key = CONFIG_ROOT_DIR + "." + index;
      final String directory = StringUtils.trimToNull(bundleContext.getProperty(key));
      if (directory == null) {
        break;
      }
      directories.add(new AssetStoreDirectory(directory, prefixLength(bundleContext, key)));
    }
    return directories;
  }

  private static int prefixLength(BundleContext bundleContext, String directoryKey) {
    final String key = directoryKey + CONFIG_PREFIX_LENGTH_SUFFIX;
    final String value = StringUtils.trimToNull(bundleContext.getProperty(key));
    if (value == null) {
      return 0;
    }
    try {
      final int prefixLength = Integer.parseInt(value);
      if (prefixLength < 0 || prefixLength > MAX_PREFIX_LENGTH) {
        throw new IllegalArgumentException(key + " must be between 0 and " + MAX_PREFIX_LENGTH + ": " + value);
      }
      return prefixLength;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be an integer between 0 and " + MAX_PREFIX_LENGTH + ": "
          + value, e);
    }
  }

  /**
   * Make sure the layout recorded in the directory's marker file matches this configuration, writing the marker file
   * if the directory does not have one yet.
   * <p>
   * A directory without marker file is considered to hold data in the flat layout if it contains any subdirectory
   * other than <code>lost+found</code> or hidden directories. Such a directory can only be used with a prefix length
   * of zero. The marker file is written to a temporary file first and then linked into place, so that it appears
   * atomically with its full content and several nodes sharing the directory may start at the same time.
   *
   * @throws ConfigurationException
   *           if the marker file does not match the configured prefix length
   * @throws IOException
   *           if the marker file cannot be read or written
   */
  public void verifyOrWriteMarker() throws ConfigurationException, IOException {
    final Path marker = path.resolve(MARKER_FILE);
    if (!Files.exists(marker)) {
      if (prefixLength > 0 && hasData()) {
        throw new ConfigurationException(String.format(
            "Asset store directory %s already contains data but has no %s file. Existing data uses the flat layout, "
                + "so a prefix length of %d cannot be configured for it. Use a new directory instead.",
            path, MARKER_FILE, prefixLength));
      }
      final Properties properties = new Properties();
      properties.setProperty(MARKER_PREFIX_LENGTH, Integer.toString(prefixLength));
      final Path tmp = Files.createTempFile(path, MARKER_FILE + ".", ".tmp");
      try {
        try (OutputStream out = Files.newOutputStream(tmp)) {
          properties.store(out, "Layout of this Opencast asset store directory. Do not modify.");
        }
        Files.createLink(marker, tmp);
        logger.info("Recorded prefix length {} for asset store directory {}", prefixLength, path);
        return;
      } catch (FileAlreadyExistsException e) {
        logger.debug("Marker file {} was created concurrently", marker);
      } finally {
        Files.deleteIfExists(tmp);
      }
    }
    final int recorded = readMarker().orElseThrow(() -> new ConfigurationException(
        "Marker file " + marker + " was removed while it was being checked"));
    if (recorded != prefixLength) {
      throw new ConfigurationException(String.format(
          "Asset store directory %s was created with a prefix length of %d but %d is configured. "
              + "The prefix length of a directory must not be changed once it contains data.",
          path, recorded, prefixLength));
    }
  }

  /**
   * Read the prefix length recorded in the directory's marker file.
   *
   * @return the recorded prefix length, or none if the directory has no marker file
   * @throws ConfigurationException
   *           if the marker file exists but does not contain a valid prefix length
   * @throws IOException
   *           if the marker file cannot be read
   */
  public Optional<Integer> readMarker() throws ConfigurationException, IOException {
    final Path marker = path.resolve(MARKER_FILE);
    if (!Files.exists(marker)) {
      return Optional.empty();
    }
    final Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(marker)) {
      properties.load(in);
    }
    final String value = StringUtils.trimToNull(properties.getProperty(MARKER_PREFIX_LENGTH));
    try {
      if (value == null || Integer.parseInt(value) < 0) {
        throw invalidMarker(marker, value);
      }
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      throw invalidMarker(marker, value);
    }
  }

  private static ConfigurationException invalidMarker(Path marker, String value) {
    return new ConfigurationException(String.format(
        "Marker file %s contains an invalid prefix length: %s. If the directory contains no data, delete the file. "
            + "Otherwise, restore it with the prefix length the data was written with.",
        marker, value));
  }

  /** Whether the directory contains data, i.e. any subdirectory other than lost+found or hidden directories */
  private boolean hasData() throws IOException {
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
      for (Path entry : entries) {
        final String name = entry.getFileName().toString();
        if (Files.isDirectory(entry) && !LOST_AND_FOUND.equals(name) && !name.startsWith(".")) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AssetStoreDirectory)) {
      return false;
    }
    final AssetStoreDirectory other = (AssetStoreDirectory) o;
    return prefixLength == other.prefixLength && path.equals(other.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, prefixLength);
  }

  @Override
  public String toString() {
    return prefixLength > 0 ? path + " (prefix length " + prefixLength + ")" : path.toString();
  }
}
