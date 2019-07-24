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

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Properties;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.OsgiUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * This class provides index and DB migrations to Opencast.
 */
public class SchedulerMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerMigrationService.class);

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.migration";

  /** Configuration keys */
  static final String SCHEDULER_NAMESPACE = "org.opencastproject.scheduler";
  static final String SNAPSHOT_OWNER = SCHEDULER_NAMESPACE;
  static final String WORKFLOW_NAMESPACE = SCHEDULER_NAMESPACE + ".workflow.configuration";
  static final String CA_NAMESPACE = SCHEDULER_NAMESPACE + ".ca.configuration";
  static final String RECORDING_LAST_HEARD_CONFIG = "recording_last_heard";
  static final String RECORDING_STATE_CONFIG = "recording_state";
  static final String SOURCE_CONFIG = "source";
  static final String PRESENTERS_CONFIG = "presenters";
  static final String AGENT_CONFIG = "agent";
  static final String START_DATE_CONFIG = "start";
  static final String END_DATE_CONFIG = "end";
  static final String OPTOUT_CONFIG = "optout";
  static final String VERSION = "version";
  static final String LAST_MODIFIED_DATE = "last_modified_date";
  static final String LAST_CONFLICT = "last_conflict";
  static final String CHECKSUM = "checksum";

  /** The security service */
  private SecurityService securityService;

  /** The asset manager */
  private AssetManager assetManager;

  private OrganizationDirectoryService orgDirectoryService;

  private EntityManagerFactory emf;

  private final Gson gson = new Gson();

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  public void setOrgDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }

  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  public void activate(final ComponentContext cc) throws IOException {
    int ocVersion = cc.getBundleContext().getBundle().getVersion().getMajor();
    if (ocVersion > 7) {
      logger.info("Scheduler migration can only be run when upgrading from opencast 6.x to 7.x. Skipping.");
      return;
    }
    final ServiceReference<ConfigurationAdmin> svcReference = cc.getBundleContext().getServiceReference(ConfigurationAdmin.class);
    final Dictionary<String, Object> props = cc.getBundleContext().getService(svcReference)
        .getConfiguration("org.opencastproject.scheduler.impl.SchedulerServiceImpl").getProperties();
    final boolean maintenance = props != null && OsgiUtil.getOptCfgAsBoolean(props, "maintenance").getOrElse(false);
    if (!maintenance) {
      logger.info("Scheduler is not in maintenance mode. Skipping migration.");
      return;
    }
    logger.info("Start migrating scheduled events");
    final String systemUserName = SecurityUtil.getSystemUserName(cc);
    for (final Organization org : orgDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, org, SecurityUtil.createSystemUser(systemUserName, org), () -> {
        try {
          migrateScheduledEvents();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
    logger.info("Finished migrating scheduled events. You can now disable maintenance mode of scheduler and restart opencast.");
  }

  private void migrateScheduledEvents() throws Exception {
    // migrate all events for current organization
    final String org = securityService.getOrganization().getId();
    logger.info("Migrating scheduled events for organization {}", org);
    final Stream<ARecord> allEvents = getScheduledEvents();
    int count = 0;
    for (final ARecord record : allEvents) {
      migrateProperties(record);
      count++;
    }
    logger.info("Migrated {} scheduled events for organization {}.", count, org);
  }

  private Stream<ARecord> getScheduledEvents() {
    final AQueryBuilder query = assetManager.createQuery();
    // query filter for organization could be helpful to split up big migrations
    final Predicate predicate = withOrganization(query).and(withVersion(query)).and(withProperties(query));
    // select necessary properties when assembling query
    return query.select(query.propertiesOf(SCHEDULER_NAMESPACE, WORKFLOW_NAMESPACE, CA_NAMESPACE))
        .where(predicate).run().getRecords();
  }

  private Predicate withOrganization(AQueryBuilder query) {
    return query.organizationId().eq(securityService.getOrganization().getId());
  }

  private Predicate withVersion(AQueryBuilder query) {
    return query.version().isLatest();
  }

  private Predicate withProperties(AQueryBuilder query) {
    return query.hasPropertiesOf(SCHEDULER_NAMESPACE);
  }

  private Opt<ExtendedEventDto> getExtendedEventDto(String id, String orgId, EntityManager em) {
    return Opt.nul(em.find(ExtendedEventDto.class, new EventIdPK(id, orgId)));
  }

  private void migrateProperties(ARecord event) throws Exception {
    final String orgID = securityService.getOrganization().getId();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      final Opt<ExtendedEventDto> entityOpt = getExtendedEventDto(event.getMediaPackageId(), orgID, em);
      if (entityOpt.isSome()) {
        logger.warn("Migration for event {} of organization {} seems to be done already. Migrating again.",
            event.getMediaPackageId(), orgID);
      }
      // Store all properties in extended events database
      final ExtendedEventDto entity = entityOpt.getOr(new ExtendedEventDto());
      entity.setMediaPackageId(event.getMediaPackageId());
      entity.setOrganization(orgID);
      final Opt<String> agent = event.getProperties().apply(Properties.getStringOpt(AGENT_CONFIG));
      if (agent.isSome()) {
        entity.setCaptureAgentId(agent.get());
      }
      final Opt<String> checksum = event.getProperties().apply(Properties.getStringOpt(CHECKSUM));
      if (checksum.isSome()) {
        entity.setChecksum(checksum.get());
      }
      final Opt<Date> endDate = event.getProperties().apply(Properties.getDateOpt(END_DATE_CONFIG));
      if (endDate.isSome()) {
        entity.setEndDate(endDate.get());
      }
      final Opt<Date> lastModifiedDate = event.getProperties().apply(Properties.getDateOpt(LAST_MODIFIED_DATE));
      if (lastModifiedDate.isSome()) {
        entity.setLastModifiedDate(lastModifiedDate.get());
      }
      final Opt<String> presenters = event.getProperties().apply(Properties.getStringOpt(PRESENTERS_CONFIG));
      if (presenters.isSome()) {
        entity.setPresenters(presenters.get());
      }
      final Opt<Long> recLastHeard = event.getProperties().apply(Properties.getLongOpt(RECORDING_LAST_HEARD_CONFIG));
      if (recLastHeard.isSome()) {
        entity.setRecordingLastHeard(recLastHeard.get());
      }
      final Opt<String> recState = event.getProperties().apply(Properties.getStringOpt(RECORDING_STATE_CONFIG));
      if (recState.isSome()) {
        entity.setRecordingState(recState.get());
      }
      final Opt<String> source = event.getProperties().apply(Properties.getStringOpt(SOURCE_CONFIG));
      if (source.isSome()) {
        entity.setSource(source.get());
      }
      final Opt<Date> startDate = event.getProperties().apply(Properties.getDateOpt(START_DATE_CONFIG));
      if (startDate.isSome()) {
        entity.setStartDate(startDate.get());
      }
      entity.setCaptureAgentProperties(gson.toJson(event.getProperties().filter(Properties.byNamespace(CA_NAMESPACE))
          .group(toKey, toValue)));
      entity.setWorkflowProperties(gson.toJson(event.getProperties().filter(Properties.byNamespace(WORKFLOW_NAMESPACE))
          .group(toKey, toValue)));
      if (entityOpt.isSome()) {
        em.merge(entity);
      } else {
        em.persist(entity);
      }
      tx.commit();
      try {
        // Remove obsolete asset manager properties
        int deleted = 0;
        for (String namespace: Arrays.asList(SCHEDULER_NAMESPACE, CA_NAMESPACE, WORKFLOW_NAMESPACE)) {
          deleted += assetManager.deleteProperties(event.getMediaPackageId(), namespace);
        }
        logger.debug("Deleted {} migrated properties", deleted);
      } catch (Exception e) {
        logger.error("Could not delete obsolete properties for event {}", event.getMediaPackageId());
      }
    } catch (Exception e) {
      logger.error("Could not store extended event: ", e);
      if (tx != null) {
        tx.rollback();
      }
      throw (e);
    } finally {
      if (em != null)
        em.close();
    }
  }


  private static final Fn<Boolean, String> decomposeBooleanValue = new Fn<Boolean, String>() {
    @Override
    public String apply(Boolean b) {
      return b.toString();
    }
  };

  private static final Fn<Long, String> decomposeLongValue = new Fn<Long, String>() {
    @Override
    public String apply(Long l) {
      return l.toString();
    }
  };

  private static final Fn<Date, String> decomposeDateValue = new Fn<Date, String>() {
    @Override
    public String apply(Date d) {
      return DateTimeSupport.toUTC(d.getTime());
    }
  };

  private static final Fn<String, String> decomposeStringValue = new Fn<String, String>() {
    @Override
    public String apply(String s) {
      return s;
    }
  };

  private static final Fn<Version, String> decomposeVersionValue = new Fn<Version, String>() {
    @Override
    public String apply(Version v) {
      return v.toString();
    }
  };

  private static final Fn<Property, String> toKey = new Fn<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getId().getName();
    }
  };

  private static final Fn<Property, String> toValue = new Fn<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getValue().decompose(decomposeStringValue, decomposeDateValue, decomposeLongValue,
          decomposeBooleanValue, decomposeVersionValue);
    }
  };

}
