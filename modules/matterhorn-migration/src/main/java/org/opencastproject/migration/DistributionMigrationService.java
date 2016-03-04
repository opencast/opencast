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
package org.opencastproject.migration;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.opencastproject.util.OsgiUtil.getOptContextProperty;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.data.Option.none;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.impl.SearchServiceImpl;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides migration index and DB migrations to Matterhorn.
 */
public class DistributionMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(DistributionMigrationService.class);

  /** The security service */
  private SecurityService securityService;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The search service */
  private SearchServiceImpl searchService;

  /** The search database service */
  private SearchServiceDatabase searchServiceDatabase;

  /** The archive */
  private Archive<?> archive;

  /** HttpMediaPackagheElementProvider */
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;

  /** The component context */
  private ComponentContext cc;

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI callback. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi DI callback. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI callback. */
  public void setSearchService(SearchServiceImpl searchService) {
    this.searchService = searchService;
  }

  /** OSGi DI callback. */
  public void setSearchServiceDatabase(SearchServiceDatabase searchServiceDatabase) {
    this.searchServiceDatabase = searchServiceDatabase;
  }

  /** OSGi DI callback. */
  public void setArchive(Archive<?> archive) {
    this.archive = archive;
  }

  /** OSGi DI callback. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  public void activate(final ComponentContext cc) {
    this.cc = cc;
    logger.info("Start migration distribution artifacts to tenants");

    Opt<String> downloadDirectoryPath = getOptContextProperty(cc, "org.opencastproject.download.directory").toOpt();
    Opt<String> downloadUrl = getOptContextProperty(cc, "org.opencastproject.download.url").toOpt();
    if (downloadDirectoryPath.isSome() && downloadUrl.isSome()) {
      migrateDistributionDirectory(downloadDirectoryPath.get(), downloadUrl.get(), "download", false);
    } else {
      logger.info("No download distribution directory {} and/or URL {} found to migrate, skip it!",
              downloadDirectoryPath, downloadUrl);
    }

    Opt<String> streamingDirectoryPath = getOptContextProperty(cc, "org.opencastproject.streaming.directory").toOpt();
    Opt<String> streamingUrl = getOptContextProperty(cc, "org.opencastproject.streaming.url").toOpt();
    if (streamingDirectoryPath.isSome() && streamingUrl.isSome()) {
      migrateDistributionDirectory(streamingDirectoryPath.get(), streamingUrl.get(), "streaming", true);
    } else {
      logger.info("No streaming distribution directory and/or URL found to migrate, skip it!", streamingDirectoryPath,
              streamingUrl);
    }

    logger.info("Finished migration distribution artifacts to tenants");
  }

  private void migrateDistributionDirectory(final String distributionDirectoryPath, final String distributionUrl,
          String serviceName, final boolean isStreaming) {
    try {
      final File distributionDirectory = new File(distributionDirectoryPath);

      // Check for existing organization directories
      final List<Path> tenantPaths = new ArrayList<>();
      final List<Organization> organizations = new ArrayList<>();
      for (Organization org : organizationDirectoryService.getOrganizations()) {
        Path orgPath = new File(path(distributionDirectory.getAbsolutePath(), org.getId())).toPath();
        if (Files.exists(orgPath)) {
          logger.info("Migrating '{}' distribution artifacts on tenant '{}' already done, skip migration!", serviceName,
                  org);
          return;
        } else {
          // Create tenant directory according to organization directory
          tenantPaths.add(Files.createDirectory(orgPath));
          organizations.add(org);
          logger.info("Found '{}' distribution artifacts for tenant '{}'!", serviceName, org);
        }
      }

      // move files
      Map<Organization, Set<String>> mediapackages = moveFiles(serviceName, distributionDirectory, tenantPaths,
              organizations);

      adjustDistributedSearchURL(serviceName, mediapackages, distributionUrl, isStreaming);
    } catch (Exception e) {
      logger.error("Unable to migrate {} distribution artifacts, aborting migration!", serviceName);
    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
    }
  }

  private Map<Organization, Set<String>> moveFiles(String serviceName, final File distributionDirectory,
          final List<Path> tenantPaths, final List<Organization> organizations) throws IOException {
    logger.info("Start moving {} distribution files to new location", serviceName);

    final Integer[] errors = new Integer[1];
    errors[0] = 0;
    final Integer[] sucess = new Integer[1];
    sucess[0] = 0;

    // Loop over all mps in channel directories (filter out tenant dirs)
    final Map<Organization, Set<String>> mediapackages = new HashMap<>();
    Files.walkFileTree(distributionDirectory.toPath(), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        for (Path orgPath : tenantPaths) {
          if (Files.isSameFile(dir, orgPath))
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
        // Parse mediapackage identifier
        final String channelPath = file.toFile().getAbsolutePath()
                .substring(distributionDirectory.getAbsolutePath().length() + 1);
        String[] splitUrl = channelPath.split("/");
        if (splitUrl.length < 2) {
          logger.info("Skip migrating {}", file);
          return FileVisitResult.CONTINUE;
        }

        final String mpId = splitUrl[1];

        // Loop over all organizations and try to look up mediapackage in archive
        for (Organization org : organizations) {
          // if only one tenant, skip look up
          if (organizations.size() != 1 && !lookUpArchive(mpId, org))
            continue;

          Set<String> ids = mediapackages.get(org);
          if (ids == null)
            ids = new HashSet<>();

          ids.add(mpId);
          mediapackages.put(org, ids);

          File newPath = new File(path(distributionDirectory.getAbsolutePath(), org.getId(), channelPath))
                  .getParentFile();

          logger.info("Try to move {} to new location {}", file, newPath);

          try {
            FileUtils.forceMkdir(newPath);
            FileSupport.move(file.toFile(), newPath);
            FileSupport.deleteHierarchyIfEmpty(distributionDirectory, file.toFile().getParentFile());
            sucess[0]++;
            logger.info("Successfully moved file {}", file);
          } catch (IOException e) {
            errors[0]++;
            logger.error("Unable to move file {} to new location {}: {}",
                    new Object[] { file, newPath, getMessage(e) });
          }
          return FileVisitResult.CONTINUE;
        }
        logger.warn("No matching organization found for file {}, skip migration it", file);
        return FileVisitResult.CONTINUE;
      }
    });

    logger.info(
            "Finished moving {} distribution files to new location. {} files moved. {} files couldn't be moved. Check logs for errror",
            new Object[] { serviceName, sucess[0], errors[0] });
    return mediapackages;
  }

  private Boolean lookUpArchive(final String mpId, final Organization org) {
    return SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(cc, org), new Function0<Boolean>() {
      @Override
      public Boolean apply() {
        final ResultSet result = archive.findForAdministrativeRead(getQuery(mpId, org),
                httpMediaPackageElementProvider.getUriRewriter());
        if (result.size() > 0) {
          return true;
        }
        return false;
      }

      private Query getQuery(final String mpId, final Organization org) {
        return new Query() {
          @Override
          public boolean isOnlyLastVersion() {
            return false;
          }

          @Override
          public boolean isIncludeDeleted() {
            return false;
          }

          @Override
          public Option<String> getSeriesId() {
            return none();
          }

          @Override
          public Option<String> getOrganizationId() {
            return Option.some(org.getId());
          }

          @Override
          public Option<Integer> getOffset() {
            return none();
          }

          @Override
          public Option<String> getMediaPackageId() {
            return Option.some(mpId);
          }

          @Override
          public Option<Integer> getLimit() {
            return none();
          }

          @Override
          public Option<Date> getDeletedBefore() {
            return none();
          }

          @Override
          public Option<Date> getDeletedAfter() {
            return none();
          }

          @Override
          public Option<Date> getArchivedBefore() {
            return none();
          }

          @Override
          public Option<Date> getArchivedAfter() {
            return none();
          }
        };
      }
    });
  }

  private void adjustDistributedSearchURL(final String serviceName, final Map<Organization, Set<String>> mps,
          final String distributionUrl, final boolean isStreaming) {
    // Adjust distribution URLs in engage mediapackage
    for (final Organization org : mps.keySet()) {
      SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(cc, org), new Effect0() {
        @Override
        protected void run() {
          int sucess = 0;
          int errors = 0;
          int total = mps.get(org).size();
          logger.info("Start adjusting {} distribution URL's on search service for tenant {}", serviceName, org);
          logger.info("{} mediapackages to adjust...", total);
          int i = 0;
          for (String mpId : mps.get(org)) {
            try {
              SearchResult result = searchService.getForAdministrativeRead(new SearchQuery().withId(mpId));
              for (SearchResultItem item : result.getItems()) {
                MediaPackage mediaPackage = item.getMediaPackage();
                boolean mediapackageChanged = false;

                // migration distribution URL's!
                for (MediaPackageElement e : mediaPackage.getElements()) {
                  String uri = e.getURI().toString();

                  logger.debug("Looking to migrate '{}'", uri);

                  if (uri.indexOf(org.getId()) > 0) {
                    logger.debug("Mediapackage element {} has already been migrated", uri);
                    continue;
                  }

                  if (uri.startsWith(distributionUrl)) {
                    String path = uri.substring(distributionUrl.length());
                    URI newUri = URI.create(UrlSupport.concat(distributionUrl, org.getId(), path));
                    if (isStreaming) {
                      String[] tag = StringUtils.split(path, ":");
                      if (tag.length > 1) {
                        newUri = URI.create(UrlSupport.concat(distributionUrl, tag[0] + ":" + org.getId(),
                                path.substring(tag[0].length() + 1)));
                      }
                    }
                    e.setURI(newUri);
                    mediapackageChanged = true;
                  }
                }

                if (!mediapackageChanged) {
                  sucess++;
                  logger.info("Mediapackage {} ({}/{}) has already been migrated", new Object[] { mpId, i++, total });
                  continue;
                }

                // Write mediapackage to DB and update the index
                AccessControlList acl = authorizationService.getActiveAcl(mediaPackage).getA();
                searchService.getSolrIndexManager().add(mediaPackage, acl, item.getModified());
                searchServiceDatabase.storeMediaPackage(mediaPackage, acl, item.getModified());
                sucess++;
                logger.info("Successfully migrated {} ({}/{})", new Object[] { mpId, i++, total });
              }
            } catch (Exception e) {
              errors++;
              logger.info("Unable to migrate {} ({}/{}): {}", new Object[] { mpId, i++, total, getMessage(e) });
            }
          }
          logger.info(
                  "Finished adjusting {} distribution URL's on search service for tenant {}. {} entries migrated. {} entries couldn't be migrated. Check logs for errror",
                  new Object[] { serviceName, org, sucess, errors });
        }
      });
    }
  }

}
