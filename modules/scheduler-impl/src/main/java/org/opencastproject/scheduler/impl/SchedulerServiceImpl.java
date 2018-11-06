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
package org.opencastproject.scheduler.impl;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.Opt.some;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.assetmanager.api.fn.Properties.getStringOpt;
import static org.opencastproject.scheduler.api.SchedulerService.ReviewStatus.UNSENT;
import static org.opencastproject.scheduler.impl.SchedulerUtil.calculateChecksum;
import static org.opencastproject.scheduler.impl.SchedulerUtil.episodeToMp;
import static org.opencastproject.scheduler.impl.SchedulerUtil.eventOrganizationFilter;
import static org.opencastproject.scheduler.impl.SchedulerUtil.filterByNamespace;
import static org.opencastproject.scheduler.impl.SchedulerUtil.isNotEpisodeDublinCore;
import static org.opencastproject.scheduler.impl.SchedulerUtil.recordToMp;
import static org.opencastproject.scheduler.impl.SchedulerUtil.toKey;
import static org.opencastproject.scheduler.impl.SchedulerUtil.toReviewStatus;
import static org.opencastproject.scheduler.impl.SchedulerUtil.toValue;
import static org.opencastproject.scheduler.impl.SchedulerUtil.uiAdapterToFlavor;
import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.Log.getHumanReadableTimeString;
import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.RequireUtil.requireTrue;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.ARecords;
import org.opencastproject.assetmanager.api.fn.Properties;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.PropertySchema;
import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.index.IndexProducer;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.ConflictHandler;
import org.opencastproject.scheduler.api.ConflictNotifier;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.RecordingImpl;
import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
import org.opencastproject.scheduler.api.Util;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link SchedulerService}.
 */
public class SchedulerServiceImpl extends AbstractIndexProducer implements SchedulerService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  /** The last modifed cache configuration key */
  private static final String CFG_KEY_LAST_MODIFED_CACHE_EXPIRE = "last_modified_cache_expire";

  /** The transaction cleanup offset configuration key */
  private static final String CFG_KEY_TRANSACTION_CLEANUP_OFFSET = "transaction_cleanup_offset";

  /** The default cache expire time in seconds */
  private static final int DEFAULT_CACHE_EXPIRE = 60;

  /** The Etag for an empty calendar */
  private static final String EMPTY_CALENDAR_ETAG = "mod0";

  /** The workflow configuration prefix */
  public static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";

  /** Namespace keys */
  private static final String WORKFLOW_NAMESPACE = SchedulerService.JOB_TYPE + ".workflow.configuration";
  private static final String CA_NAMESPACE = SchedulerService.JOB_TYPE + ".ca.configuration";

  private static final String SNAPSHOT_OWNER = SchedulerService.JOB_TYPE;

  /** Configuration keys */
  private static final String RECORDING_LAST_HEARD_CONFIG = "recording_last_heard";
  private static final String RECORDING_STATE_CONFIG = "recording_state";
  private static final String REVIEW_DATE_CONFIG = "review_date";
  private static final String REVIEW_STATUS_CONFIG = "review_status";
  private static final String SOURCE_CONFIG = "source";
  private static final String PRESENTERS_CONFIG = "presenters";
  private static final String AGENT_CONFIG = "agent";
  private static final String START_DATE_CONFIG = "start";
  private static final String END_DATE_CONFIG = "end";
  private static final String OPTOUT_CONFIG = "optout";
  private static final String VERSION = "version";
  private static final String LAST_MODIFIED_ORIGIN = "last_modified_origin";
  private static final String LAST_MODIFIED_DATE = "last_modified_date";
  private static final String LAST_CONFLICT = "last_conflict";
  private static final String CHECKSUM = "checksum";

  /** The last modified cache */
  protected Cache<String, String> lastModifiedCache = CacheBuilder.newBuilder()
          .expireAfterWrite(DEFAULT_CACHE_EXPIRE, TimeUnit.SECONDS).build();

  /** The transaction cleanup offset in millis */
  protected int transactionOffsetMillis = DateTimeConstants.MILLIS_PER_DAY * 10;

  /** The message broker sender service */
  private MessageSender messageSender;

  /** The message broker receiver service */
  private MessageReceiver messageReceiver;

  /** Persistent storage for events */
  private SchedulerServiceDatabase persistence;

  /** The series service */
  private SeriesService seriesService;

  /** The security service used to run the security context with. */
  private SecurityService securityService;

  /** The asset manager */
  private AssetManager assetManager;

  /** The workspace */
  private Workspace workspace;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The conflict handler */
  private ConflictHandler conflictHandler;

  /** The list of registered conflict notifiers */
  private List<ConflictNotifier> conflictNotifiers = new ArrayList<>();

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectoryService;

  /** The list of registered event catalog UI adapters */
  private List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<>();

  /** The system user name */
  private String systemUserName;

  /**
   * OSGi callback to set message sender.
   *
   * @param messageSender
   */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * OSGi callback to set message receiver.
   *
   * @param messageReceiver
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * OSGi callback to set Persistence Service.
   *
   * @param persistence
   */
  public void setPersistence(SchedulerServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * OSGi callback for setting Series Service.
   *
   * @param seriesService
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi callback to set security service.
   *
   * @param securityService
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the asset manager.
   *
   * @param assetManager
   */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * OSGi callback to set the workspace.
   *
   * @param workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi callback to set the authorization service.
   *
   * @param authorizationService
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * OSGi callback to set the conflict handler.
   *
   * @param conflictHandler
   */
  public void setConflictHandler(ConflictHandler conflictHandler) {
    this.conflictHandler = conflictHandler;
  }

  /** OSGi callback to add {@link ConflictNotifier} instance. */
  public void addConflictNotifier(ConflictNotifier conflictNotifier) {
    conflictNotifiers.add(conflictNotifier);
  }

  /** OSGi callback to remove {@link ConflictNotifier} instance. */
  public void removeConflictNotifier(ConflictNotifier conflictNotifier) {
    conflictNotifiers.remove(conflictNotifier);
  }

  /**
   * OSGi callback to set the organization directory service.
   *
   * @param orgDirectoryService
   */
  public void setOrgDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }

  /** OSGi callback to add {@link EventCatalogUIAdapter} instance. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link EventCatalogUIAdapter} instance. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /**
   * Activates Scheduler Service.
   *
   * @param cc
   *          ComponentContext
   * @throws Exception
   */
  public void activate(ComponentContext cc) throws Exception {
    super.activate();
    systemUserName = SecurityUtil.getSystemUserName(cc);
    logger.info("Activating Scheduler Service");
  }

   /** Callback from OSGi on service deactivation. */
  @Override
  public void deactivate() {
    super.deactivate();
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties != null) {
      Option<Integer> cacheExpireDuration = OsgiUtil.getOptCfg(properties, CFG_KEY_LAST_MODIFED_CACHE_EXPIRE)
              .bind(Strings.toInt);
      if (cacheExpireDuration.isSome()) {
        lastModifiedCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpireDuration.get(), TimeUnit.SECONDS)
                .build();
        logger.info("Set last modified cache to {}", getHumanReadableTimeString(cacheExpireDuration.get()));
      } else {
        logger.info("Set last modified cache to default {}", getHumanReadableTimeString(DEFAULT_CACHE_EXPIRE));
      }
      for (Integer offsetInSeconds : OsgiUtil.getOptCfg(properties, CFG_KEY_TRANSACTION_CLEANUP_OFFSET)
              .bind(Strings.toInt)) {
        transactionOffsetMillis = offsetInSeconds * DateTimeConstants.MILLIS_PER_SECOND;
      }
    }
    logger.info("Set transaction cleanup offset to {}",
            getHumanReadableTimeString(transactionOffsetMillis / DateTimeConstants.MILLIS_PER_SECOND));
  }

  @Override
  public void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
          MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
          Opt<Boolean> optOutStatus, Opt<String> schedulingSource, String modificationOrigin)
                  throws UnauthorizedException, SchedulerException {
    addEventInternal(startDateTime, endDateTime, captureAgentId, userIds, mediaPackage, wfProperties, caMetadata,
            modificationOrigin, optOutStatus, schedulingSource);
  }

  private void addEventInternal(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
          MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
          String modificationOrigin, Opt<Boolean> optOutStatus, Opt<String> schedulingSource)
                  throws SchedulerException {
    notNull(startDateTime, "startDateTime");
    notNull(endDateTime, "endDateTime");
    notEmpty(captureAgentId, "captureAgentId");
    notNull(userIds, "userIds");
    notNull(mediaPackage, "mediaPackage");
    notNull(wfProperties, "wfProperties");
    notNull(caMetadata, "caMetadata");
    notEmpty(modificationOrigin, "modificationOrigin");
    notNull(optOutStatus, "optOutStatus");
    notNull(schedulingSource, "schedulingSource");
    if (endDateTime.before(startDateTime))
      throw new IllegalArgumentException("The end date is before the start date");

    final String mediaPackageId = mediaPackage.getIdentifier().compact();

    try {
      AQueryBuilder query = assetManager.createQuery();
      AResult result = query.select(query.nothing())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId).and(query.version().isLatest())))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isSome()) {
        logger.warn("Mediapackage with id '{}' already exists!", mediaPackageId);
        throw new SchedulerConflictException("Mediapackage with id '" + mediaPackageId + "' already exists!");
      }

      Opt<String> seriesId = Opt.nul(StringUtils.trimToNull(mediaPackage.getSeries()));

      // Get opt out status
      boolean optOut = getOptOutStatus(seriesId, optOutStatus);

      // Check for conflicting events if not opted out
      if (!optOut) {
        List<MediaPackage> conflictingEvents = findConflictingEvents(captureAgentId, startDateTime, endDateTime);
        if (conflictingEvents.size() > 0) {
          logger.info("Unable to add event {}, conflicting events found: {}", mediaPackageId, conflictingEvents);
          throw new SchedulerConflictException(
                  "Unable to add event, conflicting events found for event " + mediaPackageId);
        }
      }

      // Load dublincore and acl for update
      Opt<DublinCoreCatalog> dublinCore = DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage);
      AccessControlList acl = authorizationService.getActiveAcl(mediaPackage).getA();

      // Get updated agent properties
      Map<String, String> finalCaProperties = getFinalAgentProperties(caMetadata, wfProperties, captureAgentId,
              seriesId, dublinCore);

      // Persist asset
      String checksum = calculateChecksum(workspace, getEventCatalogUIAdapterFlavors(), startDateTime, endDateTime,
                                          captureAgentId, userIds, mediaPackage, dublinCore, wfProperties, finalCaProperties, optOut,
                                          acl);
      persistEvent(mediaPackageId, modificationOrigin, checksum, Opt.some(startDateTime), Opt.some(endDateTime),
              Opt.some(captureAgentId), Opt.some(userIds), Opt.some(mediaPackage), Opt.some(wfProperties),
              Opt.some(finalCaProperties), Opt.some(optOut), schedulingSource);

      // Send updates
      sendUpdateAddEvent(mediaPackageId, some(acl), dublinCore, Opt.some(startDateTime),
              Opt.some(endDateTime), Opt.some(userIds), Opt.some(captureAgentId), Opt.some(finalCaProperties),
              Opt.some(optOut));

      // Update last modified
      touchLastEntry(captureAgentId);
    } catch (SchedulerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to create event with id '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }


  public Map<String, Period> addMultipleEvents(RRule rRule, Date start, Date end, Long duration, TimeZone tz,
          String captureAgentId, Set<String> userIds, MediaPackage templateMp, Map<String, String> wfProperties,
          Map<String, String> caMetadata, Opt<Boolean> optOut, Opt<String> schedulingSource, String modificationOrigin)
          throws UnauthorizedException, SchedulerConflictException, SchedulerException {
    List<Period> periods = calculatePeriods(rRule, start, end, duration, tz);
    return addMultipleEventInternal(periods, captureAgentId, userIds, templateMp, wfProperties, caMetadata,
            modificationOrigin, optOut, schedulingSource);
  }


  private Map<String, Period> addMultipleEventInternal(List<Period> periods, String captureAgentId,
          Set<String> userIds, MediaPackage templateMp, Map<String, String> wfProperties,
          Map<String, String> caMetadata, String modificationOrigin, Opt<Boolean> optOutStatus,
          Opt<String> schedulingSource) throws SchedulerException {
    notNull(periods, "periods");
    requireTrue(periods.size() > 0, "periods");
    notEmpty(captureAgentId, "captureAgentId");
    notNull(userIds, "userIds");
    notNull(templateMp, "mediaPackages");
    notNull(wfProperties, "wfProperties");
    notNull(caMetadata, "caMetadata");
    notEmpty(modificationOrigin, "modificationOrigin");
    notNull(optOutStatus, "optOutStatus");
    notNull(schedulingSource, "schedulingSource");

    Map<String, Period> scheduledEvents = new LinkedHashMap<>();

    try {
      LinkedList<Id> ids = new LinkedList<>();
      AQueryBuilder qb = assetManager.createQuery();
      Predicate p = null;
      //While we don't have a list of IDs equal to the number of periods
      while (ids.size() <= periods.size()) {
        //Create a list of IDs equal to the number of periods, along with a set of AM predicates
        while (ids.size() <= periods.size()) {
          Id id = new IdImpl(UUID.randomUUID().toString());
          ids.add(id);
          Predicate np = qb.mediaPackageId(id.compact());
          //Haha, p = np jokes with the AM query language. Ha. Haha. Ha.  (Sob...)
          if (null == p) {
            p = np;
          } else {
            p = p.or(np);
          }
        }
        //Select the list of ids which alread exist.  Hint: this needs to be zero
        AResult result = qb.select(qb.nothing()).where(withOrganization(qb).and(p).and(qb.version().isLatest())).run();
        //If there is conflict, clear the list and start over
        if (result.getTotalSize() > 0) {
          ids.clear();
        }
      }


      Opt<String> seriesId = Opt.nul(StringUtils.trimToNull(templateMp.getSeries()));
      // Get opt out status
      boolean optOut = getOptOutStatus(seriesId, optOutStatus);

      // Check for conflicting events if not opted out
      if (!optOut) {
        List<MediaPackage> conflictingEvents = findConflictingEvents(periods, captureAgentId, TimeZone.getDefault());
        if (conflictingEvents.size() > 0) {
          logger.info("Unable to add events, conflicting events found: {}", conflictingEvents);
          throw new SchedulerConflictException("Unable to add event, conflicting events found");
        }
      }

      //counter for index into the list of mediapackages
      int counter = 0;
      for (Period event : periods) {
        MediaPackage mediaPackage = (MediaPackage) templateMp.clone();
        Date startDate = new Date(event.getStart().getTime());
        Date endDate = new Date(event.getEnd().getTime());
        Id id = ids.get(counter);

        //Get, or make, the DC catalog
        DublinCoreCatalog dc;
        Opt<DublinCoreCatalog> dcOpt = DublinCoreUtil.loadEpisodeDublinCore(workspace,
                templateMp);
        if (dcOpt.isSome()) {
          dc = dcOpt.get();
          dc = (DublinCoreCatalog) dc.clone();
          // make sure to bind the OC_PROPERTY namespace
          dc.addBindings(XmlNamespaceContext
                  .mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX, DublinCores.OC_PROPERTY_NS_URI)));
        } else {
          dc = DublinCores.mkOpencastEpisode().getCatalog();
        }

        // Set the new media package identifier
        mediaPackage.setIdentifier(id);

        // Update dublincore title and temporal
        String newTitle = dc.getFirst(DublinCore.PROPERTY_TITLE) + String.format(" %0" + Integer.toString(periods.size()).length() + "d", ++counter);
        dc.set(DublinCore.PROPERTY_TITLE, newTitle);
        DublinCoreValue eventTime = EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDate, endDate),
                Precision.Second);
        dc.set(DublinCore.PROPERTY_TEMPORAL, eventTime);
        mediaPackage = updateDublincCoreCatalog(mediaPackage, dc);
        mediaPackage.setTitle(newTitle);

        String mediaPackageId = mediaPackage.getIdentifier().compact();
        //Converting from iCal4j DateTime objects to plain Date objects to prevent AMQ issues below
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(event.getStart());
        Date startDateTime = cal.getTime();
        cal.setTime(event.getEnd());
        Date endDateTime = cal.getTime();
        // Load dublincore and acl for update
        Opt<DublinCoreCatalog> dublinCore = DublinCoreUtil.loadEpisodeDublinCore(workspace, mediaPackage);
        AccessControlList acl = authorizationService.getActiveAcl(mediaPackage).getA();

        // Get updated agent properties
        Map<String, String> finalCaProperties = getFinalAgentProperties(caMetadata, wfProperties, captureAgentId,
                seriesId, dublinCore);

        // Persist asset
        String checksum = calculateChecksum(workspace, getEventCatalogUIAdapterFlavors(), startDateTime, endDateTime,
                captureAgentId, userIds, mediaPackage, dublinCore, wfProperties, finalCaProperties, optOut, acl);
        persistEvent(mediaPackageId, modificationOrigin, checksum, Opt.some(startDateTime), Opt.some(endDateTime), Opt.some(captureAgentId), Opt.some(userIds), Opt.some(mediaPackage), Opt.some(wfProperties),
                Opt.some(finalCaProperties), Opt.some(optOut), schedulingSource);

        // Send updates
        sendUpdateAddEvent(mediaPackageId, some(acl), dublinCore, Opt.some(startDateTime), Opt.some(endDateTime),
                Opt.some(userIds), Opt.some(captureAgentId), Opt.some(finalCaProperties), Opt.some(optOut));

        // Update last modified
        touchLastEntry(captureAgentId);
        scheduledEvents.put(mediaPackageId, event);
        for (MediaPackageElement mediaPackageElement : mediaPackage.getElements()) {
          try {
            workspace.delete(mediaPackage.getIdentifier().toString(), mediaPackageElement.getIdentifier());
          } catch (NotFoundException | IOException e) {
            logger.warn("Failed to delete media package element", e);
          }
        }
      }
      return scheduledEvents;
    } catch (SchedulerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to create events: {}", getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateEvent(final String mpId, Opt<Date> startDateTime, Opt<Date> endDateTime, Opt<String> captureAgentId,
          Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage, Opt<Map<String, String>> wfProperties,
          Opt<Map<String, String>> caMetadata, Opt<Opt<Boolean>> optOutOption, String modificationOrigin)
                  throws NotFoundException, UnauthorizedException, SchedulerException {
    updateEventInternal(mpId, modificationOrigin, startDateTime, endDateTime, captureAgentId, userIds, mediaPackage,
            wfProperties, caMetadata, optOutOption);
  }

  private void updateEventInternal(final String mpId, String modificationOrigin, Opt<Date> startDateTime,
          Opt<Date> endDateTime, Opt<String> captureAgentId, Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage,
          Opt<Map<String, String>> wfProperties, Opt<Map<String, String>> caMetadata, Opt<Opt<Boolean>> optOutOption
  ) throws NotFoundException, SchedulerException {
    notEmpty(mpId, "mpId");
    notEmpty(modificationOrigin, "modificationOrigin");
    notNull(startDateTime, "startDateTime");
    notNull(endDateTime, "endDateTime");
    notNull(captureAgentId, "captureAgentId");
    notNull(userIds, "userIds");
    notNull(mediaPackage, "mediaPackage");
    notNull(wfProperties, "wfProperties");
    notNull(caMetadata, "caMetadata");
    notNull(optOutOption, "optOutStatus");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      ASelectQuery select = query
              .select(query.snapshot(), p.start().target(), p.end().target(),
                      query.propertiesOf(WORKFLOW_NAMESPACE, CA_NAMESPACE), p.agent().target(), p.source().target(),
                      p.checksum().target(), p.optOut().target(), p.presenters().target())
              .where(withOrganization(query).and(query.mediaPackageId(mpId).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace()))));
      Opt<ARecord> optEvent = select.run().getRecords().head();
      if (optEvent.isNone())
        throw new NotFoundException("No event found while updating event " + mpId);

      ARecord record = optEvent.get();
      if (record.getSnapshot().isNone())
        throw new NotFoundException("No mediapackage found while updating event " + mpId);

      Opt<DublinCoreCatalog> dublinCoreOpt = loadEpisodeDublinCoreFromAsset(record.getSnapshot().get());
      if (dublinCoreOpt.isNone())
        throw new NotFoundException("No dublincore found while updating event " + mpId);

      verifyActive(mpId, record);

      Date start = record.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
      Date end = record.getProperties().apply(Properties.getDate(END_DATE_CONFIG));

      if ((startDateTime.isSome() || endDateTime.isSome()) && endDateTime.getOr(end).before(startDateTime.getOr(start)))
        throw new SchedulerException("The end date is before the start date");

      String agentId = record.getProperties().apply(Properties.getString(AGENT_CONFIG));
      Opt<String> seriesId = Opt.nul(record.getSnapshot().get().getMediaPackage().getSeries());
      boolean oldOptOut = record.getProperties().apply(Properties.getBoolean(OPTOUT_CONFIG));

      // Get opt out status
      Opt<Boolean> optOut = Opt.none();
      for (Opt<Boolean> optOutToUpdate : optOutOption) {
        optOut = Opt.some(getOptOutStatus(seriesId, optOutToUpdate));
      }

      // Set to opted out
      boolean isNewOptOut = optOut.isSome() && optOut.get();

      // Changed to ready for recording
      boolean readyForRecording = optOut.isSome() && !optOut.get();

      // Has a conflict related property be changed?
      boolean propertyChanged = captureAgentId.isSome() || startDateTime.isSome() || endDateTime.isSome();

      // Check for conflicting events
      if (!isNewOptOut && (readyForRecording || (propertyChanged && !oldOptOut))) {
        List<MediaPackage> conflictingEvents = $(findConflictingEvents(captureAgentId.getOr(agentId),
                startDateTime.getOr(start), endDateTime.getOr(end))).filter(new Fn<MediaPackage, Boolean>() {
                    @Override
                    public Boolean apply(MediaPackage mp) {
                    return !mpId.equals(mp.getIdentifier().compact());
                  }
                  }).toList();
        if (conflictingEvents.size() > 0) {
          logger.info("Unable to update event {}, conflicting events found: {}", mpId, conflictingEvents);
          throw new SchedulerConflictException("Unable to update event, conflicting events found for event " + mpId);
        }
      }

      Set<String> presenters = getPresenters(record.getProperties().apply(getStringOpt(PRESENTERS_CONFIG)).getOr(""));
      Map<String, String> wfProps = record.getProperties().filter(filterByNamespace._2(WORKFLOW_NAMESPACE)).group(toKey,
              toValue);
      Map<String, String> caProperties = record.getProperties().filter(filterByNamespace._2(CA_NAMESPACE)).group(toKey,
              toValue);

      boolean propertiesChanged = false;
      boolean dublinCoreChanged = false;

      // Get workflow properties
      for (Map<String, String> wfPropsToUpdate : wfProperties) {
        propertiesChanged = true;
        wfProps = wfPropsToUpdate;
      }

      // Get capture agent properties
      for (Map<String, String> caMetadataToUpdate : caMetadata) {
        propertiesChanged = true;
        caProperties = caMetadataToUpdate;
      }

      if (captureAgentId.isSome())
        propertiesChanged = true;

      Opt<AccessControlList> acl = Opt.none();
      Opt<DublinCoreCatalog> dublinCore = Opt.none();
      Opt<AccessControlList> aclOld = loadEpisodeAclFromAsset(record.getSnapshot().get());
      for (MediaPackage mpToUpdate : mediaPackage) {
        // Check for series change
        if (ne(record.getSnapshot().get().getMediaPackage().getSeries(), mpToUpdate.getSeries())) {
          propertiesChanged = true;
          seriesId = Opt.nul(mpToUpdate.getSeries());
        }

        // Check for ACL change and send update
        AccessControlList aclNew = authorizationService.getActiveAcl(mpToUpdate).getA();
        if (aclOld.isNone() || !AccessControlUtil.equals(aclNew, aclOld.get())) {
          acl = some(aclNew);
        }

        // Check for dublin core change and send update
        Opt<DublinCoreCatalog> dublinCoreNew = DublinCoreUtil.loadEpisodeDublinCore(workspace, mpToUpdate);
        if (dublinCoreNew.isSome() && !DublinCoreUtil.equals(dublinCoreOpt.get(), dublinCoreNew.get())) {
          dublinCoreChanged = true;
          propertiesChanged = true;
          dublinCore = dublinCoreNew;
        }
      }

      Opt<Map<String, String>> finalCaProperties = Opt.none();
      if (propertiesChanged) {
        finalCaProperties = Opt.some(getFinalAgentProperties(caProperties, wfProps, captureAgentId.getOr(agentId),
                                                             seriesId, some(dublinCore.getOr(dublinCoreOpt.get()))));
      }

      String checksum = calculateChecksum(workspace, getEventCatalogUIAdapterFlavors(), startDateTime.getOr(start),
              endDateTime.getOr(end), captureAgentId.getOr(agentId), userIds.getOr(presenters),
              mediaPackage.getOr(record.getSnapshot().get().getMediaPackage()),
              some(dublinCore.getOr(dublinCoreOpt.get())), wfProperties.getOr(wfProps),
              finalCaProperties.getOr(caProperties), optOut.getOr(oldOptOut), acl.getOr(new AccessControlList()));

      String oldChecksum = record.getProperties().apply(Properties.getString(CHECKSUM));
      if (checksum.equals(oldChecksum)) {
        logger.debug("Updated event {} has same checksum, ignore update", mpId);
        return;
      }

      // Update asset
      persistEvent(mpId, modificationOrigin, checksum, startDateTime, endDateTime, captureAgentId, userIds,
              mediaPackage, wfProperties, finalCaProperties, optOut, Opt.<String> none());

      // Send updates
      sendUpdateAddEvent(mpId, acl, dublinCore, startDateTime, endDateTime, userIds, Opt.some(agentId),
              finalCaProperties, optOut);
      // Update last modified
      if (propertiesChanged || dublinCoreChanged || optOutOption.isSome() || startDateTime.isSome()
              || endDateTime.isSome()) {
        touchLastEntry(agentId);
        for (String agent : captureAgentId) {
          touchLastEntry(agent);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (SchedulerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to update event with id '{}': {}", mpId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  private Opt<AccessControlList> loadEpisodeAclFromAsset(Snapshot snapshot) {
    Option<MediaPackageElement> acl = mlist(snapshot.getMediaPackage().getElements())
            .filter(MediaPackageSupport.Filters.isEpisodeAcl).headOpt();
    if (acl.isNone())
      return Opt.none();

    Opt<Asset> asset = assetManager.getAsset(snapshot.getVersion(),
            snapshot.getMediaPackage().getIdentifier().compact(), acl.get().getIdentifier());
    if (asset.isNone())
      return Opt.none();

    if (Availability.OFFLINE.equals(asset.get().getAvailability()))
      return Opt.none();

    InputStream inputStream = null;
    try {
      inputStream = asset.get().getInputStream();
      return Opt.some(XACMLUtils.parseXacml(inputStream));
    } catch (Exception e) {
      logger.warn("Unable to parse access control list: {}", getStackTrace(e));
      return Opt.none();
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  private Opt<DublinCoreCatalog> loadEpisodeDublinCoreFromAsset(Snapshot snapshot) {
    Option<MediaPackageElement> dcCatalog = mlist(snapshot.getMediaPackage().getElements())
            .filter(MediaPackageSupport.Filters.isEpisodeDublinCore).headOpt();
    if (dcCatalog.isNone())
      return Opt.none();

    Opt<Asset> asset = assetManager.getAsset(snapshot.getVersion(),
            snapshot.getMediaPackage().getIdentifier().compact(), dcCatalog.get().getIdentifier());
    if (asset.isNone())
      return Opt.none();

    if (Availability.OFFLINE.equals(asset.get().getAvailability()))
      return Opt.none();

    InputStream inputStream = null;
    try {
      inputStream = asset.get().getInputStream();
      return Opt.some(DublinCores.read(inputStream));
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /**
   * Verifies if existing event is found and has not already ended
   *
   * @param record
   *          the record
   * @throws SchedulerException
   *           if event has ended or is not found
   */
  private void verifyActive(String eventId, ARecord record) throws SchedulerException {
    Date start = record.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
    Date end = record.getProperties().apply(Properties.getDate(END_DATE_CONFIG));
    if (start == null || end == null) {
      throw new IllegalArgumentException("Start and/or end date for event ID " + eventId + " is not set");
    }
    // TODO: Assumption of no TimeZone adjustment because catalog temporal is local to server
    if (new Date().after(end)) {
      logger.info("Event ID {} has already ended as its end time was {} and current time is {}", new Object[] { eventId,
              DateTimeSupport.toUTC(end.getTime()), DateTimeSupport.toUTC(new Date().getTime()) });
      throw new SchedulerException("Event ID " + eventId + " has already ended at "
              + DateTimeSupport.toUTC(end.getTime()) + " and now is " + DateTimeSupport.toUTC(new Date().getTime()));
    }
  }

  @Override
  public synchronized void removeEvent(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      // Check if there are properties only from scheduler
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(p.agent().target(), p.source().target())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();
      Opt<ARecord> recordOpt = result.getRecords().head();
      long deletedProperties = 0;
      if (recordOpt.isSome()) {
        String agentId = recordOpt.get().getProperties().apply(Properties.getString(AGENT_CONFIG));

        // Delete all properties
        deletedProperties = query.delete(SNAPSHOT_OWNER, query.propertiesOf(p.namespace(), WORKFLOW_NAMESPACE, CA_NAMESPACE))
                .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)))
                .name("delete all properties")
                .willRemoveWholeMediaPackage(true)
                .run();

        if (StringUtils.isNotEmpty(agentId))
          touchLastEntry(agentId);
      }

      // Delete scheduler snapshot
      long deletedSnapshots = query.delete(SNAPSHOT_OWNER, query.snapshot())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)))
              .name("delete episode")
              .willRemoveWholeMediaPackage(true)
              .run();
      if (deletedProperties + deletedSnapshots == 0)
        throw new NotFoundException();

      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.delete(mediaPackageId));

    } catch (NotFoundException | SchedulerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not remove event '{}' from persistent storage: {}", mediaPackageId, e);
      throw new SchedulerException(e);
    }
  }

  @Override
  public MediaPackage getMediaPackage(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      Opt<MediaPackage> mp = getEventMediaPackage(mediaPackageId);
      if (mp.isNone())
        throw new NotFoundException();

      return mp.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get mediapackage of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public DublinCoreCatalog getDublinCore(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.snapshot())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId))
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      Opt<DublinCoreCatalog> dublinCore = loadEpisodeDublinCoreFromAsset(record.get().getSnapshot().get());
      if (dublinCore.isNone())
        throw new NotFoundException("No dublincore catalog found " + mediaPackageId);

      return dublinCore.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get dublin core catalog of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public TechnicalMetadata getTechnicalMetadata(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query
              .select(p.optOut().target(), p.agent().target(), p.start().target(), p.end().target(),
                      p.presenters().target(), p.recordingStatus().target(), p.recordingLastHeard().target(),
                      query.propertiesOf(CA_NAMESPACE), query.propertiesOf(WORKFLOW_NAMESPACE))
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();
      final Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      return getTechnicalMetadata(record.get(), p);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get technical metadata of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public AccessControlList getAccessControlList(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.snapshot())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId))
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      Opt<AccessControlList> acl = loadEpisodeAclFromAsset(record.get().getSnapshot().get());
      if (acl.isNone())
        return null;

      return acl.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get access control list of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public Map<String, String> getWorkflowConfig(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.propertiesOf(WORKFLOW_NAMESPACE))
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();

      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      return record.get().getProperties().group(toKey, toValue);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get workflow configuration of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public Map<String, String> getCaptureAgentConfiguration(String mediaPackageId)
          throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.propertiesOf(CA_NAMESPACE))
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId)).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace())))
              .run();

      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      return record.get().getProperties().group(toKey, toValue);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get capture agent contiguration of event '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public boolean isOptOut(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      Opt<Property> optOut = query
              .select(p.optOut().target()).where(withOrganization(query).and(query.mediaPackageId(mediaPackageId))
                      .and(query.version().isLatest()).and(p.optOut().exists()))
              .run().getRecords().bind(ARecords.getProperties).head();
      if (optOut.isNone())
        throw new NotFoundException();

      return optOut.get().getValue().get(Value.BOOLEAN);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get opt out status of event with mediapackage '{}': {}", mediaPackageId,
              getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  /*
   * FIXME: This query is unsafe since it may fetch many many rows. It would be better to do conflict checking directly
   * in the database instead of fetching all scheduled events for the capture agent and then doing the checks in memory.
   * Unfortunately, the current database schema is not suitable to do this in an efficient manner. Especially the generic
   * concept of 'properties' would lead to many nested queries.
   */
  private ARecord[] getScheduledEvents(Opt<String> captureAgentId) {
    AQueryBuilder query = assetManager.createQuery();
    Props p = new Props(query);
    Predicate predicate = withOrganization(query).and(withOwner(query)).and(query.hasPropertiesOf(p.namespace()));
    for (String agentId : captureAgentId) {
      predicate = predicate.and(p.agent().eq(agentId));
    }
    return query.select(query.snapshot(), p.start().target(), p.end().target()).where(predicate).run().getRecords().toList().toArray(new ARecord[0]);
  }

  @Override
  public List<MediaPackage> search(Opt<String> captureAgentId, Opt<Date> startsFrom, Opt<Date> startsTo,
          Opt<Date> endFrom, Opt<Date> endTo) throws SchedulerException {
    final ARecord[] alreadyScheduledEvents = getScheduledEvents(captureAgentId);
    return searchInternal(startsFrom, startsTo, endFrom, endTo, alreadyScheduledEvents);
  }

  /*
   * Returns a list of events which start and/or end within a given date range.  So you could search for things which
   * start on Tuesday between 0900 and 1000, and end between 1500 and 1600, and you could get an event which started at
   * Epoch and ended at 1559 on Tuesday.  This is *NOT* appropriate for conflict checking, and does not check for any
   * edge cases.  Use checkScheduleConflicts instead.
   */
  private List<MediaPackage> searchInternal(Opt<Date> startsFrom, Opt<Date> startsTo,
                                            Opt<Date> endFrom, Opt<Date> endTo, ARecord[] records) {
    final List<ARecord> result = new ArrayList<>();
    for (final ARecord r : records) {
      final Date start = r.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
      final Date end = r.getProperties().apply(Properties.getDate(END_DATE_CONFIG));
      if (startsFrom.isSome() && start.before(startsFrom.get())
              || startsTo.isSome() && start.after(startsTo.get())
              || endFrom.isSome() && end.before(endFrom.get())
              || endTo.isSome() && end.after(endTo.get())) {
        continue;
      }
      result.add(r);
    }
    result.sort(new Comparator<ARecord>() {
      @Override
      public int compare(ARecord o1, ARecord o2) {
        Date start1 = o1.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
        Date start2 = o2.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
        return start1.compareTo(start2);
      }
    });
    return Stream.mk(result).bind(recordToMp).toList();
  }

  /*
   * Returns a list of conflicting mediapackages.  This method checks for containment, starts-during, and ends-during.
   */
  private List<MediaPackage> checkScheduleConflicts(Date checkStart, Date checkEnd, ARecord[] records) {
    final List<ARecord> result = new ArrayList<>();
    for (final ARecord r : records) {
      final Date start = r.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
      final Date end = r.getProperties().apply(Properties.getDate(END_DATE_CONFIG));
      /*
      If the potential event starts during event r OR
      If the potential event ends during event r OR
      If the potential event begins before, and ends after event r (ie, containment) OR
      If the potential event begins or ends within the minimum separation distance of event r
      */
      if (Util.schedulingIntervalsOverlap(checkStart, checkEnd, start, end)) {
        result.add(r);
      }
    }
    result.sort(new Comparator<ARecord>() {
      @Override
      public int compare(ARecord o1, ARecord o2) {
        Date start1 = o1.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
        Date start2 = o2.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
        return start1.compareTo(start2);
      }
    });
    return Stream.mk(result).bind(recordToMp).toList();
  }

  @Override
  public List<MediaPackage> findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
      throws SchedulerException {
    try {
      Set<String> eventIds = persistence.getEvents(captureDeviceID, startDate, endDate, Util.EVENT_MINIMUM_SEPARATION_MILLISECONDS);
      final AQueryBuilder query = assetManager.createQuery();
      final Predicate predicate = withOrganization(query)
          .and(withOwner(query))
          .and(query.mediaPackageIds(eventIds.toArray(new String[0])));
      final Props p = new Props(query);
      final List<ARecord> records = query.select(query.snapshot(), p.agent().target(), p.start().target(), p.end().target(),
          query.propertiesOf(CA_NAMESPACE)).where(predicate).run().getRecords().toList();
      return Stream.mk(records).sort(
          new Comparator<ARecord>() {
            @Override
            public int compare(ARecord o1, ARecord o2) {
              Date start1 = o1.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
              Date start2 = o2.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
              return start1.compareTo(start2);
            }
          }
      ).bind(recordToMp).toList();
    } catch (Exception e) {
      throw new SchedulerException(e);
    }
  }

  @Override
  public List<MediaPackage> findConflictingEvents(String captureAgentId, RRule rrule, Date start, Date end,
          long duration, TimeZone tz) throws SchedulerException {
    notEmpty(captureAgentId, "captureAgentId");
    notNull(rrule, "rrule");
    notNull(start, "start");
    notNull(end, "end");
    notNull(tz, "timeZone");

    final List<Period> periods = calculatePeriods(rrule, start, end, duration, tz);
    return findConflictingEvents(periods, captureAgentId, tz);
  }

    private List<MediaPackage> findConflictingEvents(List<Period> periods, String captureAgentId, TimeZone tz)
          throws SchedulerException {
    notEmpty(captureAgentId, "captureAgentId");
    notNull(periods, "periods");
    requireTrue(periods.size() > 0, "periods");

    try {
      final ARecord[] alreadyScheduledEvents = getScheduledEvents(Opt.some(captureAgentId));
      final TimeZone utc = TimeZone.getTimeZone("utc");

      Set<MediaPackage> events = new HashSet<>();

      for (Period event : periods) {
        TimeZone.setDefault(utc);
        final Date startDate = event.getStart();
        final Date endDate = event.getEnd();

        events.addAll(findConflictingEvents(startDate, endDate, alreadyScheduledEvents));
      }

      TimeZone.setDefault(null);
      return new ArrayList<>(events);
    } catch (Exception e) {
      logger.error("Failed to search for conflicting events: {}", getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public List<Period> calculatePeriods(RRule rrule, Date start, Date end, long duration, TimeZone tz) {
    return Util.calculatePeriods(start, end, duration, rrule, tz);
  }

  @Override
  public String getCalendar(Opt<String> captureAgentId, Opt<String> seriesId, Opt<Date> cutoff)
          throws SchedulerException {
    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      Predicate predicate = withOrganization(query).and(withOwner(query)).and(query.hasPropertiesOf(p.namespace())).and(p.optOut().eq(false))
              .and(p.end().ge(DateTime.now().minusHours(1).toDate()));
      for (String agentId : captureAgentId) {
        predicate = predicate.and(p.agent().eq(agentId));
      }
      for (String series : seriesId) {
        predicate = predicate.and(query.seriesId().eq(series));
      }
      for (Date d : cutoff) {
        predicate = predicate.and(p.start().le(d));
      }
      ASelectQuery select = query.select(query.snapshot(), p.agent().target(), p.start().target(), p.end().target(),
              query.propertiesOf(CA_NAMESPACE)).where(predicate);
      Stream<ARecord> records = select.run().getRecords();

      CalendarGenerator cal = new CalendarGenerator(seriesService);
      for (ARecord record : records) {
        Opt<MediaPackage> optMp = record.getSnapshot().map(episodeToMp);

        // If the event media package is empty, skip the event
        if (optMp.isNone()) {
          logger.warn("Mediapackage for event '{}' can't be found, event is not recorded", record.getMediaPackageId());
          continue;
        }

        Opt<DublinCoreCatalog> catalogOpt = loadEpisodeDublinCoreFromAsset(record.getSnapshot().get());
        if (catalogOpt.isNone()) {
          logger.warn("No episode catalog available, skipping!");
          continue;
        }

        Map<String, String> caMetadata = record.getProperties().filter(filterByNamespace._2(CA_NAMESPACE)).group(toKey,
                toValue);

        // If the even properties are empty, skip the event
        if (caMetadata.isEmpty()) {
          logger.warn("Properties for event '{}' can't be found, event is not recorded", record.getMediaPackageId());
          continue;
        }

        String agentId = record.getProperties().apply(Properties.getString(AGENT_CONFIG));
        Date start = record.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
        Date end = record.getProperties().apply(Properties.getDate(END_DATE_CONFIG));
        Date lastModified = record.getSnapshot().get().getArchivalDate();

        // Add the entry to the calendar, skip it with a warning if adding fails
        try {
          cal.addEvent(optMp.get(), catalogOpt.get(), agentId, start, end, lastModified, toPropertyString(caMetadata));
        } catch (Exception e) {
          logger.warn("Error adding event '{}' to calendar, event is not recorded", record.getMediaPackageId(), e);
        }
      }

      // Only validate calendars with events. Without any events, the iCalendar won't validate
      if (cal.getCalendar().getComponents().size() > 0) {
        try {
          cal.getCalendar().validate();
        } catch (ValidationException e) {
          logger.warn("Recording calendar could not be validated (returning it anyways): {}", getStackTrace(e));
        }
      }

      return cal.getCalendar().toString();

    } catch (Exception e) {
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateReviewStatus(String mediaPackageId, ReviewStatus reviewStatus)
          throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");
    notNull(reviewStatus, "reviewStatus");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.nothing())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace()))))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      Date now = new Date();
      assetManager.setProperty(p.reviewDate().mk(mediaPackageId, now));
      assetManager.setProperty(p.reviewStatus().mk(mediaPackageId, reviewStatus.toString()));
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateReviewStatus(mediaPackageId, reviewStatus, now));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to update review status of event with mediapackage '{}': {}", mediaPackageId,
              getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public ReviewStatus getReviewStatus(String mediaPackageId) throws NotFoundException, SchedulerException {
    notEmpty(mediaPackageId, "mediaPackageId");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(p.reviewStatus().target())
              .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace()))))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      return record.get().getProperties().apply(getStringOpt(REVIEW_STATUS_CONFIG)).map(toReviewStatus).getOr(UNSENT);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get review status of event with mediapackage '{}': {}", mediaPackageId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public String getScheduleLastModified(String captureAgentId) throws SchedulerException {
    notEmpty(captureAgentId, "captureAgentId");

    try {
      String lastModified = lastModifiedCache.getIfPresent(captureAgentId);
      if (lastModified != null)
        return lastModified;

      populateLastModifiedCache();

      lastModified = lastModifiedCache.getIfPresent(captureAgentId);

      // If still null set the empty calendar ETag
      if (lastModified == null) {
        lastModified = EMPTY_CALENDAR_ETAG;
        lastModifiedCache.put(captureAgentId, lastModified);
      }
      return lastModified;
    } catch (Exception e) {
      logger.error("Failed to get last modified of agent with id '{}': {}", captureAgentId, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void removeScheduledRecordingsBeforeBuffer(long buffer) throws SchedulerException {
    DateTime end = new DateTime(DateTimeZone.UTC).minus(buffer * 1000);

    logger.info("Starting to look for scheduled recordings that have finished before {}.",
            DateTimeSupport.toUTC(end.getMillis()));

    List<MediaPackage> finishedEvents;
    try {
      finishedEvents = search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.some(end.toDate()));
      logger.debug("Found {} events from search.", finishedEvents.size());
    } catch (SchedulerException e) {
      logger.error("Unable to search for finished events: {}", getStackTrace(e));
      throw new SchedulerException(e);
    }

    int recordingsRemoved = 0;
    for (MediaPackage event : finishedEvents) {
      String eventId = event.getIdentifier().compact();
      try {
        removeEvent(eventId);
        logger.debug("Sucessfully removed scheduled event with id " + eventId);
        recordingsRemoved++;
      } catch (NotFoundException e) {
        logger.debug("Skipping event with id {} because it is not found", eventId);
      } catch (Exception e) {
        logger.warn("Unable to delete event with id '{}': {}", eventId, getStackTrace(e));
      }
    }

    logger.info("Found {} to remove that ended before {}.", recordingsRemoved, DateTimeSupport.toUTC(end.getMillis()));
  }

  @Override
  public boolean updateRecordingState(String id, String state) throws NotFoundException, SchedulerException {
    notEmpty(id, "id");
    notEmpty(state, "state");

    if (!RecordingState.KNOWN_STATES.contains(state)) {
      logger.warn("Invalid recording state: {}.", state);
      return false;
    }

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(p.recordingStatus().target(), p.recordingLastHeard().target())
              .where(withOrganization(query).and(query.mediaPackageId(id).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace()))))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      Opt<String> recordingState = record.get().getProperties().apply(Properties.getStringOpt(RECORDING_STATE_CONFIG));
      Opt<Long> lastHeard = record.get().getProperties().apply(Properties.getLongOpt(RECORDING_LAST_HEARD_CONFIG));

      if (recordingState.isSome() && lastHeard.isSome()) {
        Recording r = new RecordingImpl(id, recordingState.get(), lastHeard.get());
        if (state.equals(r.getState())) {
          logger.debug("Recording state not changed");
          // Reset the state anyway so that the last-heard-from time is correct...
          r.setState(state);
        } else {
          logger.debug("Setting Recording {} to state {}.", id, state);
          r.setState(state);
          sendRecordingUpdate(r);
        }
        assetManager.setProperty(p.recordingStatus().mk(id, r.getState()));
        assetManager.setProperty(p.recordingLastHeard().mk(id, r.getLastCheckinTime()));
        return true;
      } else {
        Recording r = new RecordingImpl(id, state);
        assetManager.setProperty(p.recordingStatus().mk(id, r.getState()));
        assetManager.setProperty(p.recordingLastHeard().mk(id, r.getLastCheckinTime()));
        sendRecordingUpdate(r);
        return true;
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to update recording status of event with mediapackage '{}': {}", id, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public Recording getRecordingState(String id) throws NotFoundException, SchedulerException {
    notEmpty(id, "id");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(p.recordingStatus().target(), p.recordingLastHeard().target())
              .where(withOrganization(query).and(query.mediaPackageId(id)).and(query.version().isLatest())
                      .and(query.hasPropertiesOf(p.namespace())).and(p.recordingStatus().exists())
                      .and(p.recordingLastHeard().exists()))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone() || record.get().getProperties().isEmpty())
        throw new NotFoundException();

      String recordingState = record.get().getProperties().apply(Properties.getString(RECORDING_STATE_CONFIG));
      Long lastHeard = record.get().getProperties().apply(Properties.getLong(RECORDING_LAST_HEARD_CONFIG));
      return new RecordingImpl(id, recordingState, lastHeard);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to get recording status of event with mediapackage '{}': {}", id, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void removeRecording(String id) throws NotFoundException, SchedulerException {
    notEmpty(id, "id");

    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(query.nothing()).where(withOrganization(query)
              .and(query.mediaPackageId(id).and(query.version().isLatest())).and(query.hasPropertiesOf(p.namespace())))
              .run();
      Opt<ARecord> record = result.getRecords().head();
      if (record.isNone())
        throw new NotFoundException();

      query = assetManager.createQuery();
      p = new Props(query);
      final Predicate predicate = withOrganization(query).and(query.mediaPackageId(id));
      query.delete(SNAPSHOT_OWNER, p.recordingStatus().target()).where(predicate).run();
      query.delete(SNAPSHOT_OWNER, p.recordingLastHeard().target()).where(predicate).run();

      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.deleteRecordingState(id));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to delete recording status of event with mediapackage '{}': {}", id, getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public Map<String, Recording> getKnownRecordings() throws SchedulerException {
    try {
      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query.select(p.recordingStatus().target(), p.recordingLastHeard().target())
              .where(withOrganization(query).and(query.version().isLatest()).and(query.hasPropertiesOf(p.namespace()))
                      .and(p.recordingStatus().exists()).and(p.recordingLastHeard().exists()))
              .run();
      Map<String, Recording> recordings = new HashMap<>();
      for (ARecord record : result.getRecords()) {
        String recordingState = record.getProperties().apply(Properties.getString(RECORDING_STATE_CONFIG));
        Long lastHeard = record.getProperties().apply(Properties.getLong(RECORDING_LAST_HEARD_CONFIG));
        recordings.put(record.getMediaPackageId(),
                new RecordingImpl(record.getMediaPackageId(), recordingState, lastHeard));
      }
      return recordings;
    } catch (Exception e) {
      logger.error("Failed to get known recording states: {}", getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  private synchronized void persistEvent(final String mpId, final String modificationOrigin, final String checksum,
          final Opt<Date> startDateTime, final Opt<Date> endDateTime, final Opt<String> captureAgentId,
          final Opt<Set<String>> userIds, final Opt<MediaPackage> mediaPackage,
          final Opt<Map<String, String>> wfProperties, final Opt<Map<String, String>> caProperties,
          final Opt<Boolean> optOut, final Opt<String> schedulingSource) throws SchedulerServiceDatabaseException {
    AQueryBuilder query = assetManager.createQuery();
    final Props p = new Props(query);
    final String workflowNamespace = WORKFLOW_NAMESPACE;
    final String caNamespace = CA_NAMESPACE;

    // Store scheduled mediapackage
    for (MediaPackage mpToUpdate : mediaPackage) {
      Snapshot snapshot = assetManager.takeSnapshot(SNAPSHOT_OWNER, mpToUpdate);
      assetManager.setProperty(p.version().mk(mpId, snapshot.getVersion()));
    }
    // Store scheduled start date
    for (Date start : startDateTime) {
      assetManager.setProperty(p.start().mk(mpId, start));
    }
    // Store scheduled end date
    for (Date end : endDateTime) {
      assetManager.setProperty(p.end().mk(mpId, end));
    }
    // Store capture agent id
    for (String agent : captureAgentId) {
      assetManager.setProperty(p.agent().mk(mpId, agent));
    }
    // Store the user identifiers
    for (Set<String> users : userIds) {
      assetManager.setProperty(p.presenters().mk(mpId, StringUtils.join(users, ",")));
    }
    // Store opt out status
    for (Boolean optOutToUpdate : optOut) {
      assetManager.setProperty(p.optOut().mk(mpId, optOutToUpdate));
    }

    // Store capture agent properties
    for (Map<String, String> props : caProperties) {
      Properties.removeProperties(assetManager, SNAPSHOT_OWNER, securityService.getOrganization().getId(), mpId,
              caNamespace);
      for (Entry<String, String> entry : props.entrySet()) {
        assetManager
                .setProperty(Property.mk(PropertyId.mk(mpId, caNamespace, entry.getKey()), Value.mk(entry.getValue())));
      }
    }

    // Store workflow properties
    for (Map<String, String> props : wfProperties) {
      Properties.removeProperties(assetManager, SNAPSHOT_OWNER, securityService.getOrganization().getId(), mpId,
              workflowNamespace);
      for (Entry<String, String> entry : props.entrySet()) {
        assetManager.setProperty(
                Property.mk(PropertyId.mk(mpId, workflowNamespace, entry.getKey()), Value.mk(entry.getValue())));
      }
    }

    // Store scheduling source
    for (String source : schedulingSource) {
      assetManager.setProperty(p.source().mk(mpId, source));
    }

    // Store last modified source/date and checksum
    assetManager.setProperty(p.lastModifiedOrigin().mk(mpId, modificationOrigin));
    assetManager.setProperty(p.lastModifiedDate().mk(mpId, new Date()));
    assetManager.setProperty(p.checksum().mk(mpId, checksum));

    // Remove last conflicts
    final AQueryBuilder q = assetManager.createQuery();
    q.delete(SNAPSHOT_OWNER, new Props(q).lastConflict().target())
            .where(withOrganization(q).and(q.mediaPackageId(mpId))).run();
  }

  private void sendUpdateAddEvent(String mpId, Opt<AccessControlList> acl, Opt<DublinCoreCatalog> dublinCore,
          Opt<Date> startTime, Opt<Date> endTime, Opt<Set<String>> presenters, Opt<String> agentId,
          Opt<Map<String, String>> properties, Opt<Boolean> optOut) {
    if (acl.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateAcl(mpId, acl.get()));
    }
    if (dublinCore.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateCatalog(mpId, dublinCore.get()));
    }
    if (startTime.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateStart(mpId, startTime.get()));
    }
    if (endTime.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateEnd(mpId, endTime.get()));
    }
    if (presenters.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updatePresenters(mpId, presenters.get()));
    }
    if (agentId.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateAgent(mpId, agentId.get()));
    }
    if (properties.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateProperties(mpId, properties.get()));
    }
    if (optOut.isSome()) {
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateOptOut(mpId, optOut.get()));
    }
  }

  private boolean getOptOutStatus(Opt<String> seriesId, Opt<Boolean> optOutStatus)
          throws NotFoundException, SeriesException {
    if (optOutStatus.isSome()) {
      return optOutStatus.get();
    } else if (seriesId.isSome()) {
      return seriesService.isOptOut(seriesId.get());
    } else {
      return false;
    }
  }

  private Map<String, String> getFinalAgentProperties(Map<String, String> caMetadata, Map<String, String> wfProperties,
          String captureAgentId, Opt<String> seriesId, Opt<DublinCoreCatalog> dublinCore) {
    Map<String, String> properties = new HashMap<>();
    for (Entry<String, String> entry : caMetadata.entrySet()) {
      if (entry.getKey().startsWith(WORKFLOW_CONFIG_PREFIX))
        continue;
      properties.put(entry.getKey(), entry.getValue());
    }
    for (Entry<String, String> entry : wfProperties.entrySet()) {
      properties.put(WORKFLOW_CONFIG_PREFIX.concat(entry.getKey()), entry.getValue());
    }
    if (dublinCore.isSome()) {
      properties.put("event.title", dublinCore.get().getFirst(DublinCore.PROPERTY_TITLE));
    }
    if (seriesId.isSome()) {
      properties.put("event.series", seriesId.get());
    }
    properties.put("event.location", captureAgentId);
    return properties;
  }

  private void touchLastEntry(String captureAgentId) throws SchedulerException {
    // touch last entry
    try {
      logger.debug("Marking calendar feed for {} as modified", captureAgentId);
      persistence.touchLastEntry(captureAgentId);
      populateLastModifiedCache();
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update last modified entry of agent '{}': {}", captureAgentId, getStackTrace(e));
    }
  }

  private void populateLastModifiedCache() throws SchedulerException {
    try {
      Map<String, Date> lastModifiedDates = persistence.getLastModifiedDates();
      for (Entry<String, Date> entry : lastModifiedDates.entrySet()) {
        Date lastModifiedDate = entry.getValue() != null ? entry.getValue() : new Date();
        lastModifiedCache.put(entry.getKey(), generateLastModifiedHash(lastModifiedDate));
      }
    } catch (Exception e) {
      logger.error("Failed to retrieve last modified for CA: {}", getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  private String generateLastModifiedHash(Date lastModifiedDate) {
    return "mod" + Long.toString(lastModifiedDate.getTime());
  }

  private String toPropertyString(Map<String, String> properties) {
    StringBuilder wfPropertiesString = new StringBuilder();
    for (Map.Entry<String, String> entry : properties.entrySet())
      wfPropertiesString.append(entry.getKey() + "=" + entry.getValue() + "\n");
    return wfPropertiesString.toString();
  }

  private Opt<MediaPackage> getEventMediaPackage(String mediaPackageId) {
    AQueryBuilder query = assetManager.createQuery();
    Props p = new Props(query);
    AResult result = query.select(query.snapshot())
            .where(withOrganization(query).and(query.mediaPackageId(mediaPackageId))
                    .and(query.hasPropertiesOf(p.namespace())))
            .run();
    Opt<ARecord> record = result.getRecords().head();
    if (record.isNone())
      return Opt.none();

    return record.bind(recordToMp);
  }

  /**
   *
   * @param mp
   *          the mediapackage to update
   * @param dc
   *          the dublincore metadata to use to update the mediapackage
   * @return the updated mediapackage
   * @throws IOException
   *           Thrown if an IO error occurred adding the dc catalog file
   * @throws MediaPackageException
   *           Thrown if an error occurred updating the mediapackage or the mediapackage does not contain a catalog
   */
  private MediaPackage updateDublincCoreCatalog(MediaPackage mp, DublinCoreCatalog dc)
          throws IOException, MediaPackageException {
    try (InputStream inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8")) {
      // Update dublincore catalog
      Catalog[] catalogs = mp.getCatalogs(MediaPackageElements.EPISODE);
      if (catalogs.length > 0) {
        Catalog catalog = catalogs[0];
        URI uri = workspace.put(mp.getIdentifier().toString(), catalog.getIdentifier(), "dublincore.xml", inputStream);
        catalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.setChecksum(null);
      } else {
        throw new MediaPackageException("Unable to find catalog");
      }
    }
    return mp;
  }

  private TechnicalMetadata getTechnicalMetadata(ARecord record, Props p) {
    final String agentId = record.getProperties().apply(Properties.getString(p.agent().name()));
    final boolean optOut = record.getProperties().apply(Properties.getBoolean(p.optOut().name()));
    final Date start = record.getProperties().apply(Properties.getDate(p.start().name()));
    final Date end = record.getProperties().apply(Properties.getDate(p.end().name()));
    final Set<String> presenters = getPresenters(
        record.getProperties().apply(getStringOpt(PRESENTERS_CONFIG)).getOr(""));
    final Opt<String> recordingStatus = record.getProperties()
            .apply(Properties.getStringOpt(p.recordingStatus().name()));
    final Opt<Long> lastHeard = record.getProperties().apply(Properties.getLongOpt(p.recordingLastHeard().name()));
    final Map<String, String> caMetadata = record.getProperties().filter(filterByNamespace._2(CA_NAMESPACE)).group(toKey,
            toValue);
    final Map<String, String> wfProperties = record.getProperties().filter(filterByNamespace._2(WORKFLOW_NAMESPACE))
            .group(toKey, toValue);

    Recording recording = null;
    if (recordingStatus.isSome() && lastHeard.isSome())
      recording = new RecordingImpl(record.getMediaPackageId(), recordingStatus.get(), lastHeard.get());

    return new TechnicalMetadataImpl(record.getMediaPackageId(), agentId, start, end, optOut, presenters, wfProperties,
            caMetadata, Opt.nul(recording));
  }

  private Predicate withOrganization(AQueryBuilder query) {
    return query.organizationId().eq(securityService.getOrganization().getId());
  }

  private Predicate withOwner(AQueryBuilder query) {
    return query.owner().eq(SNAPSHOT_OWNER);
  }

  private Set<String> getPresenters(String presentersString) {
    return new HashSet<>(Arrays.asList(StringUtils.split(presentersString, ",")));
  }

  private void sendRecordingUpdate(Recording recording) {
    if (RecordingState.UNKNOWN.equals(recording.getState()))
      return;

    messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue, SchedulerItem
            .updateRecordingStatus(recording.getID(), recording.getState(), recording.getLastCheckinTime()));
  }

  /**
   * @return A {@link List} of {@link MediaPackageElementFlavor} that provide the extended metadata to the front end.
   */
  private List<MediaPackageElementFlavor> getEventCatalogUIAdapterFlavors() {
    String organization = securityService.getOrganization().getId();
    return Stream.$(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).map(uiAdapterToFlavor)
            .filter(isNotEpisodeDublinCore).toList();
  }

  private static class Props extends PropertySchema {

    Props(AQueryBuilder q) {
      super(q, SchedulerService.JOB_TYPE);
    }

    protected Props(AQueryBuilder q, String namespace) {
      super(q, namespace);
    }

    public PropertyField<String> source() {
      return stringProp(SOURCE_CONFIG);
    }

    public PropertyField<String> recordingStatus() {
      return stringProp(RECORDING_STATE_CONFIG);
    }

    public PropertyField<Long> recordingLastHeard() {
      return longProp(RECORDING_LAST_HEARD_CONFIG);
    }

    public PropertyField<String> reviewStatus() {
      return stringProp(REVIEW_STATUS_CONFIG);
    }

    public PropertyField<Date> reviewDate() {
      return dateProp(REVIEW_DATE_CONFIG);
    }

    public PropertyField<String> presenters() {
      return stringProp(PRESENTERS_CONFIG);
    }

    public PropertyField<String> agent() {
      return stringProp(AGENT_CONFIG);
    }

    public PropertyField<Date> start() {
      return dateProp(START_DATE_CONFIG);
    }

    public PropertyField<Date> end() {
      return dateProp(END_DATE_CONFIG);
    }

    public PropertyField<Boolean> optOut() {
      return booleanProp(OPTOUT_CONFIG);
    }

    public PropertyField<Version> version() {
      return versionProp(VERSION);
    }

    public PropertyField<String> lastModifiedOrigin() {
      return stringProp(LAST_MODIFIED_ORIGIN);
    }

    public PropertyField<String> lastConflict() {
      return stringProp(LAST_CONFLICT);
    }

    public PropertyField<Date> lastModifiedDate() {
      return dateProp(LAST_MODIFIED_DATE);
    }

    public PropertyField<String> checksum() {
      return stringProp(CHECKSUM);
    }

  }

  @Override
  public void repopulate(final String indexName) {
    notEmpty(indexName, "indexName");

    final String destinationId = SchedulerItem.SCHEDULER_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    final Organization organization = new DefaultOrganization();
    final User user = SecurityUtil.createSystemUser(systemUserName, organization);
    SecurityUtil.runAs(securityService, organization, user, () -> {
      int current = 0;

      AQueryBuilder query = assetManager.createQuery();
      Props p = new Props(query);
      AResult result = query
              .select(query.snapshot(), p.agent().target(), p.start().target(), p.end().target(),
                      p.optOut().target(), p.presenters().target(), p.reviewDate().target(),
                      p.reviewStatus().target(), p.recordingStatus().target(),
                      p.recordingLastHeard().target(), query.propertiesOf(CA_NAMESPACE))
              .where(query.hasPropertiesOf(p.namespace()))
              .run();
      final int total = (int) Math.min(result.getSize(), Integer.MAX_VALUE);
      logger.info(
              "Re-populating '{}' index with scheduled events. There are {} scheduled events to add to the index.",
              indexName, total);
      final int responseInterval = (total < 100) ? 1 : (total / 100);
      try {
        for (ARecord record : result.getRecords()) {
          current++;
          if (record.getSnapshot().isNone()) {
            logger.warn("Doesn't found a media package for an scheuled event {}", record.getMediaPackageId());
            continue;
          }
          try {
            Snapshot snapshot = record.getSnapshot().get();
            Organization org = orgDirectoryService.getOrganization(snapshot.getOrganizationId());
            User orgSystemUser = SecurityUtil.createSystemUser(systemUserName, org);
            securityService.setOrganization(org);
            securityService.setUser(orgSystemUser);

            String agentId = record.getProperties().apply(Properties.getString(AGENT_CONFIG));
            boolean optOut = record.getProperties().apply(Properties.getBoolean(OPTOUT_CONFIG));
            Date start = record.getProperties().apply(Properties.getDate(START_DATE_CONFIG));
            Date end = record.getProperties().apply(Properties.getDate(END_DATE_CONFIG));
            Set<String> presenters = getPresenters(
                record.getProperties().apply(getStringOpt(PRESENTERS_CONFIG)).getOr(""));
            Map<String, String> caMetadata = record.getProperties().filter(filterByNamespace._2(CA_NAMESPACE))
                    .group(toKey, toValue);
            ReviewStatus reviewStatus = record.getProperties()
                .apply(getStringOpt(REVIEW_STATUS_CONFIG)).map(toReviewStatus).getOr(UNSENT);
            Date reviewDate = record.getProperties().apply(Properties.getDateOpt(REVIEW_DATE_CONFIG)).orNull();
            Opt<String> recordingStatus = record.getProperties()
                    .apply(Properties.getStringOpt(RECORDING_STATE_CONFIG));
            Opt<Long> lastHeard = record.getProperties()
                    .apply(Properties.getLongOpt(RECORDING_LAST_HEARD_CONFIG));

            Opt<AccessControlList> acl = loadEpisodeAclFromAsset(snapshot);
            Opt<DublinCoreCatalog> dublinCore = loadEpisodeDublinCoreFromAsset(snapshot);

            sendUpdateAddEvent(record.getMediaPackageId(), acl, dublinCore, Opt.some(start), Opt.some(end),
                    Opt.some(presenters), Opt.some(agentId), Opt.some(caMetadata), Opt.some(optOut));

            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateBlacklist(record.getMediaPackageId(), false));
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateReviewStatus(record.getMediaPackageId(), reviewStatus, reviewDate));
            if (recordingStatus.isSome() && lastHeard.isSome())
              sendRecordingUpdate(
                      new RecordingImpl(record.getMediaPackageId(), recordingStatus.get(), lastHeard.get()));
          } finally {
            securityService.setOrganization(organization);
            securityService.setUser(user);
          }
          if (((current % responseInterval) == 0) || (current == total)) {
            messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                    IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Scheduler, total, current));
          }
        }
      } catch (Exception e) {
        logger.warn("Unable to index scheduled instances:", e);
        throw new ServiceException(e.getMessage());
      }
    });

    SecurityUtil.runAs(securityService, organization, user, () -> {
      messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
              IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Scheduler));
    });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Scheduler;
  }

  @Override
  public String getClassName() {
    return SchedulerServiceImpl.class.getName();
  }

  @Override
  public MessageSender getMessageSender() {
    return messageSender;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  public String getSystemUserName() {
    return systemUserName;
  }

}
