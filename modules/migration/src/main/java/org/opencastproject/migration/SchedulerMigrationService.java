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

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Prelude.chuck;

import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Equality;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * This class provides index and DB migrations to Opencast.
 */
public class SchedulerMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerMigrationService.class);

  private static final MediaPackageBuilderFactory mpbf = MediaPackageBuilderFactory.newInstance();

  public static final String CFG_ORGANIZATION = "org.opencastproject.migration.organization";

  /** The security service */
  private SecurityService securityService;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The scheduler service */
  private SchedulerService schedulerService;

  /** The workspace */
  private Workspace workspace;

  /** The data source */
  private DataSource dataSource;

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
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /** OSGi DI callback. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi DI callback. */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void activate(final ComponentContext cc) throws ConfigurationException, SQLException {
    logger.info("Start migrating scheduled events");

    // read config
    final String orgId = StringUtils.trimToNull((String) cc.getBundleContext().getProperty(CFG_ORGANIZATION));

    if (StringUtils.isBlank(orgId)) {
      logger.debug("No organization set for migration. Aborting.");
      return;
    }

    // create security context
    final Organization org;
    try {
      org = organizationDirectoryService.getOrganization(orgId);
    } catch (NotFoundException e) {
      throw new ConfigurationException(CFG_ORGANIZATION, String.format("Could not find organization '%s'", orgId), e);
    }
    SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(cc, org), () -> {
      // check if migration is needed
      try {
        int size = schedulerService.getEventCount();
        if (size > 0) {
          logger.info("There are already '{}' existing scheduled events, skip scheduler migration!", size);
          return;
        }
      } catch (UnauthorizedException | SchedulerException e) {
        logger.error("Unable to read existing scheduled events, skip scheduler migration!", e);
      }

      try {
        migrateScheduledEvents();
      } catch (SQLException e) {
        chuck(e);
      }
    });
    logger.info("Finished migrating scheduled events");
  }

  private void migrateScheduledEvents() throws SQLException {

  }

  // CHECKSTYLE:OFF
  static final class Event {
    final long eventId;
    final String mediaPackageId;
    final DublinCoreCatalog dublinCore;
    final Properties captureAgentProperites;
    final Opt<AccessControlList> accessControlList;
    final boolean optOut;
    final ReviewStatus reviewStatus;
    final Date reviewDate;

    public Event(long eventId, String mediaPackageId, DublinCoreCatalog dublinCore, Properties captureAgentProperites,
            AccessControlList accessControlList, boolean optOut, ReviewStatus reviewStatus, Date reviewDate) {
      this.eventId = eventId;
      this.mediaPackageId = mediaPackageId;
      this.dublinCore = dublinCore;
      this.captureAgentProperites = captureAgentProperites;
      this.accessControlList = Opt.nul(accessControlList);
      this.optOut = optOut;
      this.reviewStatus = reviewStatus;
      this.reviewDate = reviewDate;
    }

    @Override
    public int hashCode() {
      return Equality.hash(eventId, mediaPackageId, dublinCore, captureAgentProperites, accessControlList, optOut,
              reviewStatus, reviewDate);
    }

    @Override
    public boolean equals(Object that) {
      return (this == that) || (that instanceof Event && eqFields((Event) that));
    }

    public boolean canEqual(Object that) {
      return that instanceof Event;
    }

    private boolean eqFields(Event that) {
      return that.canEqual(this) && eq(eventId, that.eventId) && eq(mediaPackageId, that.mediaPackageId)
              && eq(DublinCoreUtil.calculateChecksum(dublinCore), DublinCoreUtil.calculateChecksum(that.dublinCore))
              && eq(captureAgentProperites, that.captureAgentProperites)
              && AccessControlUtil.equals(accessControlList.orNull(), that.accessControlList.orNull())
              && eq(optOut, that.optOut) && eq(reviewStatus, that.reviewStatus) && eq(reviewDate, that.reviewDate);
    }

  }
  // CHECKSTYLE:ON

}
