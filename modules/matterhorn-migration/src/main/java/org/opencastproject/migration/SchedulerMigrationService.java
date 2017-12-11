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
import static com.entwinemedia.fn.data.Opt.none;
import static java.lang.String.format;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.scheduler.api.SchedulerService.SchedulerTransaction;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Equality;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;

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
    SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(cc, org), new Effect0() {
      @Override
      protected void run() {
        // check if migration is needed
        try {
          int size = schedulerService.search(none(), none(), none(), none(), none()).size();
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
      }
    });
    logger.info("Finished migrating scheduled events");
  }

  private void migrateScheduledEvents() throws SQLException {
    SchedulerTransaction tx = null;
    ResultSet result = null;
    Statement stm = null;
    try (Connection connection = dataSource.getConnection()) {
      logger.info("Scheduler transaction | start");
      tx = schedulerService.createTransaction("opencast");
      stm = connection.createStatement();
      result = stm.executeQuery("SELECT id, access_control, blacklisted, capture_agent_metadata, dublin_core, mediapackage_id, opt_out, review_date, review_status FROM mh_scheduled_event");
      List<Event> events = transform(result);
      for (Event event : events) {
        // Outdated events have to be removed just before schedule time.
        // Filtering in advance is dangerous because the whole process lasts very long
        if (!isOutdated(event)) {
          schedule(tx, event);
          logger.info("Migrated event '{}'", event.mediaPackageId);
        } else {
          logger.info("Ignoring outdated event '{}'", event.mediaPackageId);
        }
      }
      tx.commit();

      // Update review status
      for (Event event : events) {
        if (!isOutdated(event)) {
          schedulerService.updateReviewStatus(event.mediaPackageId, event.reviewStatus);
        }
      }
      logger.info("Scheduler transaction | end");
    } catch (Exception e) {
      final String stackTrace = ExceptionUtils.getStackTrace(e);
      logger.error(format("Scheduler transaction | error\n%s", stackTrace));
      if (tx != null) {
        logger.error("Scheduler transaction | rollback transaction");
        try {
          tx.rollback();
        } catch (Exception e2) {
          final String stackTrace2 = ExceptionUtils.getStackTrace(e2);
          logger.error(format("Scheduler transaction | error doing rollback\n%s", stackTrace2));
        }
      }
    } finally {
      if (result != null)
        result.close();
      if (stm != null)
        stm.close();
    }
  }

  void schedule(SchedulerTransaction tx, Event event) {
    final Map<String, String> wfProperties = Collections.emptyMap();
    final Map<String, String> caMetadata = PropertiesUtil.toMap(event.captureAgentProperites);
    final MediaPackage mp = mkMediaPackage();
    mp.setIdentifier(new IdImpl(event.mediaPackageId));
    // create the catalog
    final DublinCoreCatalog dc = event.dublinCore;
    mp.setSeries(dc.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    // and make them available for download in the workspace
    dc.setURI(storeInWs(event.mediaPackageId, dc.getIdentifier(), "dc-episode.xml", inputStream(dc)));
    // add them to the media package
    mp.add(dc);
    // add acl to the media package
    for (AccessControlList acl : event.accessControlList) {
      authorizationService.setAcl(mp, AclScope.Episode, acl);
    }
    //
    // add to scheduler service
    Tuple<Date, Date> schedulingDate = getSchedulingDate(dc);
    String caId = dc.getFirst(DublinCore.PROPERTY_SPATIAL);
    try {
      tx.addEvent(schedulingDate.getA(), schedulingDate.getB(), caId, Collections.<String> emptySet(), mp, wfProperties,
              caMetadata, Opt.some(event.optOut));
    } catch (UnauthorizedException e) {
      logger.error("Not authorized to schedule an event", e);
      chuck(e);
    } catch (SchedulerException e) {
      logger.warn("Not able to schedule event.", e);
      chuck(e);
    } catch (NotFoundException e) {
      logger.error("Transaction disappeared");
      chuck(e);
    }
  }

  /**
   * Transform the event result set into {@link Event}s.
   */
  List<Event> transform(ResultSet resultSet) {
    try {
      List<Event> events = new ArrayList<>();
      while (resultSet.next()) {
        DublinCoreCatalog dc = readDublinCoreSilent(resultSet.getString(5));
        dc.setIdentifier(UUID.randomUUID().toString());
        dc.setFlavor(MediaPackageElements.EPISODE);
        Properties properties = parseProperties(resultSet.getString(4));
        AccessControlList acl = resultSet.getString(2) != null
                ? AccessControlParser.parseAclSilent(resultSet.getString(2)) : null;
        events.add(new Event(resultSet.getLong(1), resultSet.getString(6), dc, properties, acl, resultSet.getBoolean(7),
                ReviewStatus.valueOf(resultSet.getString(9)), resultSet.getDate(8)));
      }
      return events;
    } catch (Exception e) {
      return chuck(e);
    }
  }

  Properties parseProperties(String serializedProperties) {
    try {
      Properties caProperties = new Properties();
      caProperties.load(new StringReader(serializedProperties));
      return caProperties;
    } catch (IOException e) {
      return chuck(e);
    }
  }

  MediaPackage mkMediaPackage() {
    try {
      return mpbf.newMediaPackageBuilder().createNew();
    } catch (MediaPackageException e) {
      return chuck(e);
    }
  }

  DublinCoreCatalog readDublinCoreSilent(String serializedForm) {
    try {
      return DublinCoreXmlFormat.read(serializedForm);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      return chuck(e);
    }
  }

  InputStream inputStream(DublinCoreCatalog dc) {
    try {
      return IOUtils.toInputStream(dc.toXmlString());
    } catch (IOException e) {
      return chuck(e);
    }
  }

  /** Serialize a DublinCore catalog to a byte array. Use UTF-8 charset. */
  byte[] serialize(DublinCoreCatalog dc) {
    try {
      return dc.toXmlString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      return chuck(e);
    }
  }

  URI storeInWs(String mpId, String mpeId, String fileName, InputStream data) {
    try {
      return workspace.put(mpId, mpeId, fileName, data);
    } catch (IOException e) {
      chuck(e);
    }
    return null;
  }

  Tuple<Date, Date> getSchedulingDate(DublinCoreCatalog dc) {
    String eventId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(dc.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasStart()) {
      logger.error("Couldn't get startdate from event {}!", eventId);
    }
    if (!period.hasEnd()) {
      logger.error("Couldn't get enddate from event {}!", eventId);
    }
    return Tuple.tuple(period.getStart(), period.getEnd());
  }

  boolean isOutdated(Event event) {
    Tuple<Date, Date> schedulingDate = getSchedulingDate(event.dublinCore);
    Interval interval = new Interval(new DateTime(schedulingDate.getA()), new DateTime(schedulingDate.getB()));
    return interval.containsNow() || interval.isBeforeNow();
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
