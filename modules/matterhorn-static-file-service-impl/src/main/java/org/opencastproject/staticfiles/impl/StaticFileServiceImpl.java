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

package org.opencastproject.staticfiles.impl;

import static java.lang.String.format;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.staticfiles.jmx.UploadStatistics;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.ProgressInputStream;
import org.opencastproject.util.jmx.JmxUtil;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;

/**
 * Stores and retrieves static file resources.
 */
public class StaticFileServiceImpl implements StaticFileService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StaticFileServiceImpl.class);

  /** The key to find the root directory for the static file service in the OSGi properties. */
  public static final String STATICFILES_ROOT_DIRECTORY_KEY = "org.opencastproject.staticfiles.rootdir";

  /** The JMX business object for uploaded statistics */
  private UploadStatistics staticFileStatistics = new UploadStatistics();

  /** The JMX bean object instance */
  private ObjectInstance registerMXBean;

  // OSGi service references
  private SecurityService securityService = null;
  private OrganizationDirectoryService orgDirectory = null;

  /** The root directory for storing static files. */
  private String rootDirPath;

  private PurgeTemporaryStorageService purgeService;

  /**
   * OSGI callback for activating this component
   *
   * @param cc
   *          the osgi component context
   */
  public void activate(ComponentContext cc) {
    logger.info("Upload Static Resource Service started.");
    registerMXBean = JmxUtil.registerMXBean(staticFileStatistics, "UploadStatistics");
    rootDirPath = OsgiUtil.getContextProperty(cc, STATICFILES_ROOT_DIRECTORY_KEY);

    final File rootFile = new File(rootDirPath);
    if (!rootFile.exists()) {
      try {
        FileUtils.forceMkdir(rootFile);
      } catch (IOException e) {
        throw new ComponentException(String.format("%s does not exists and could not be created",
                rootFile.getAbsolutePath()));
      }
    }
    if (!rootFile.canRead())
      throw new ComponentException(String.format("Cannot read from %s", rootFile.getAbsolutePath()));

    purgeService = new PurgeTemporaryStorageService();
    purgeService.addListener(new Listener() {
      @Override
      public void failed(State from, Throwable failure) {
        logger.warn("Temporary storage purging service failed: {}", getStackTrace(failure));
      }
    }, MoreExecutors.sameThreadExecutor());
    purgeService.startAsync();
    logger.info("Purging of temporary storage section scheduled");
  }

  /**
   * Callback from OSGi on service deactivation.
   */
  public void deactivate() {
    JmxUtil.unregisterMXBean(registerMXBean);

    purgeService.stopAsync();
    purgeService = null;
  }

  /** OSGi DI */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI */
  public void setOrganizationDirectoryService(OrganizationDirectoryService directoryService) {
    this.orgDirectory = directoryService;
  }

  @Override
  public String storeFile(String filename, InputStream inputStream) throws IOException {
    notNull(filename, "filename");
    notNull(inputStream, "inputStream");
    final String uuid = UUID.randomUUID().toString();
    final String org = securityService.getOrganization().getId();

    Path file = getTemporaryStorageDir(org).resolve(Paths.get(uuid, filename));
    try (ProgressInputStream progressInputStream = new ProgressInputStream(inputStream)) {
      progressInputStream.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          long totalNumBytesRead = (Long) evt.getNewValue();
          long oldTotalNumBytesRead = (Long) evt.getOldValue();
          staticFileStatistics.add(totalNumBytesRead - oldTotalNumBytesRead);
        }
      });

      Files.createDirectories(file.getParent());
      Files.copy(progressInputStream, file);
    } catch (IOException e) {
      logger.error("Unable to save file '{}' to {} because: {}",
              new Object[] { filename, file, ExceptionUtils.getStackTrace(e) });
      throw e;
    }

    return uuid;
  }

  @Override
  public InputStream getFile(final String uuid) throws NotFoundException, IOException {
    if (StringUtils.isBlank(uuid))
      throw new IllegalArgumentException("The uuid must not be blank");

    final String org = securityService.getOrganization().getId();

    return Files.newInputStream(getFile(org, uuid));
  }

  @Override
  public void persistFile(final String uuid) throws NotFoundException, IOException {
    final String org = securityService.getOrganization().getId();
    try (DirectoryStream<Path> folders = Files.newDirectoryStream(getTemporaryStorageDir(org),
            getDirsEqualsUuidFilter(uuid))) {
      for (Path folder : folders) {
        Files.move(folder, getDurableStorageDir(org).resolve(folder.getFileName()));
      }
    }
  }

  @Override
  public void deleteFile(String uuid) throws NotFoundException, IOException {
    final String org = securityService.getOrganization().getId();
    Path file = getFile(org, uuid);
    Files.deleteIfExists(file);
  }

  @Override
  public String getFileName(String uuid) throws NotFoundException {
    final String org = securityService.getOrganization().getId();
    try {
      Path file = getFile(org, uuid);
      return file.getFileName().toString();
    } catch (IOException e) {
      logger.warn("Error while reading file: {}", getStackTrace(e));
      throw new NotFoundException(e);
    }
  }

  /**
   * Returns a {@link DirectoryStream.Filter} to filter the entries of a directory and only return items which filename
   * starts with the UUID.
   *
   * @param uuid
   *          The UUID to filter by
   * @return the filter
   */
  private static DirectoryStream.Filter<Path> getDirsEqualsUuidFilter(final String uuid) {
    return new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        return Files.isDirectory(entry) && entry.getFileName().toString().equals(uuid);
      }
    };
  };

  /**
   * Returns the temporary storage directory for an organization.
   *
   * @param org
   *          The organization
   * @return Path to the temporary storage directory
   */
  private Path getTemporaryStorageDir(final String org) {
    return Paths.get(rootDirPath, org, "temp");
  }

  private Path getDurableStorageDir(final String org) {
    return Paths.get(rootDirPath, org);
  }

  private Path getFile(final String org, final String uuid) throws NotFoundException, IOException {
    // First check if the file is part of the durable storage section
    try (DirectoryStream<Path> dirs = Files
            .newDirectoryStream(getDurableStorageDir(org), getDirsEqualsUuidFilter(uuid))) {
      for (Path dir : dirs) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
          for (Path file : files) {
            return file;
          }
        }
      }
    }

    // Second check if the file is part of the temporary storage section
    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(getTemporaryStorageDir(org),
            getDirsEqualsUuidFilter(uuid))) {
      for (Path dir : dirs) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
          for (Path file : files) {
            return file;
          }
        }
      }
    }

    throw new NotFoundException(format("No file with UUID '%s' found.", uuid));
  }

  /**
   * Deletes all files found in the temporary storage section of an organization.
   *
   * @param org
   *          The organization identifier
   * @throws IOException
   *           if there was an error while deleting the files.
   */
  void purgeTemporaryStorageSection(final String org, final long lifetime) throws IOException {
    logger.info("Purge temporary storage section of organization '{}'", org);
    final Path temporaryStorageDir = getTemporaryStorageDir(org);
    if (Files.exists(temporaryStorageDir)) {
      try (DirectoryStream<Path> tempFilesStream = Files.newDirectoryStream(temporaryStorageDir,
              new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path path) throws IOException {
                  return (Files.getLastModifiedTime(path).toMillis() < (new Date()).getTime() - lifetime);
                }
              })) {
        for (Path file : tempFilesStream) {
          FileUtils.deleteQuietly(file.toFile());
        }
      }
    }
  }

  /**
   * Deletes all files found in the temporary storage section of all known organizations.
   *
   * @throws IOException
   *           if there was an error while deleting the files.
   */
  void purgeTemporaryStorageSection() throws IOException {
    logger.info("Start purging temporary storage section of all known organizations");
    for (Organization org : orgDirectory.getOrganizations()) {
      purgeTemporaryStorageSection(org.getId(), TimeUnit.DAYS.toMillis(1));
    }
  }

  /** Scheduled service for purging temporary storage sections. */
  private class PurgeTemporaryStorageService extends AbstractScheduledService {

    @Override
    protected void runOneIteration() throws Exception {
      StaticFileServiceImpl.this.purgeTemporaryStorageSection();
    }

    @Override
    protected Scheduler scheduler() {
      return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.HOURS);
    }

  }

}
