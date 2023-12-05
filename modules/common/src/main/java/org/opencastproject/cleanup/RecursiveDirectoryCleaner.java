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
package org.opencastproject.cleanup;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

public final class RecursiveDirectoryCleaner implements FileVisitor<Path> {
  private final TemporalAmount days;
  private final Path startingDirectory;
  private static final Logger logger = LoggerFactory.getLogger(RecursiveDirectoryCleaner.class);

  private RecursiveDirectoryCleaner(Path startingDirectory, TemporalAmount days) {
    this.startingDirectory = startingDirectory;
    this.days = days;
  }

  private boolean isEmptyDirectory(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
        return !directory.iterator().hasNext();
      }
    }
    return false;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
    Instant daysBefore = ZonedDateTime.now().toInstant().minus(days);
    if (attrs.lastModifiedTime().toInstant().isBefore(daysBefore)) {
      logger.debug("Deleting file {}", path);
      FileUtils.deleteQuietly(path.toFile());
    } else {
      logger.trace("Keeping file {}", path);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
    if (!(e instanceof NoSuchFileException)) {
      logger.warn("Visiting file {} failed", path, e);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    if (dir.equals(startingDirectory)) {
      logger.info("Cleanup finished @{}", dir);
    } else if (isEmptyDirectory(dir)) {
      logger.debug("Deleting directory {}", dir);
      FileUtils.deleteQuietly(dir.toFile());
    } else {
      logger.trace("Keeping directory {}", dir);
    }
    return FileVisitResult.CONTINUE;
  }

  public static boolean cleanDirectory(Path startingDir, Duration duration) {
    if (!Files.exists(startingDir)) {
      logger.warn("Directory {} to cleanup is not existing", startingDir);
      return false;
    }

    if (Files.isSymbolicLink(startingDir)) {
      logger.warn("Directory {} is a symlink. Trying to resolve it.", startingDir);
      try {
        startingDir = Path.of(startingDir.toFile().getCanonicalPath());
      } catch (IOException ioException) {
        logger.error("Couldn't resolve symlink {}", startingDir, ioException);
        return false;
      }
      logger.warn("New cleanup directory after symlink was resolved: {}", startingDir);
    }

    if (!Files.isDirectory(startingDir)) {
      logger.warn("Configuration for directory cleanup invalid. {} is a file", startingDir);
      return false;
    }

    RecursiveDirectoryCleaner fileFilter = new RecursiveDirectoryCleaner(startingDir, duration);
    logger.info("Starting cleanup for directory {} for entries older than {} days", startingDir, duration.toDays());
    try {
      Files.walkFileTree(startingDir, fileFilter);
      return true;
    } catch (IOException e) {
      logger.error("Cleanup of directory {} failed", startingDir);
      return false;
    }
  }
}
