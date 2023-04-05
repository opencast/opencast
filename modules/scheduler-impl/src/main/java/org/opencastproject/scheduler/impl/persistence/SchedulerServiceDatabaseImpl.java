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

package org.opencastproject.scheduler.impl.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

/**
 * Implements {@link SchedulerServiceDatabase}.
 */
@Component(
    immediate = true,
    service = SchedulerServiceDatabase.class
)
public class SchedulerServiceDatabaseImpl implements SchedulerServiceDatabase {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.scheduler.impl.persistence";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceDatabaseImpl.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  private DBSessionFactory dbSessionFactory;

  private DBSession db;

  /** The security service */
  private SecurityService securityService;

  private static final Gson gson = new Gson();

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.scheduler.impl.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /** OSGi DI */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for scheduler");
    db = dbSessionFactory.createSession(emf);
  }

  /*
   * We need to synchronize this method because JPA doesn't support thread-safe atomic upserts.
   */
  @Override
  public synchronized void touchLastEntry(String agentId) throws SchedulerServiceDatabaseException {
    try {
      db.execTx(em -> {
        LastModifiedDto entity = em.find(LastModifiedDto.class, agentId);
        if (entity == null) {
          entity = new LastModifiedDto();
          entity.setCaptureAgentId(agentId);
          entity.setLastModifiedDate(new Date());
          em.persist(entity);
        } else {
          entity.setLastModifiedDate(new Date());
          em.merge(entity);
        }
      });
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public Date getLastModified(String agentId) throws NotFoundException, SchedulerServiceDatabaseException {
    try {
      return db.exec(namedQuery.findByIdOpt(LastModifiedDto.class, agentId))
          .map(LastModifiedDto::getLastModifiedDate)
          .orElseThrow(() -> new NotFoundException("Agent with ID " + agentId + " does not exist"));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public Map<String, Date> getLastModifiedDates() throws SchedulerServiceDatabaseException {
    try {
      return db.exec(namedQuery.findAll("LastModified.findAll", LastModifiedDto.class)).stream()
          .collect(Collectors.toMap(
              LastModifiedDto::getCaptureAgentId,
              LastModifiedDto::getLastModifiedDate
          ));
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public void storeEvent(String mediapackageId, String organizationId, Opt<String> captureAgentId, Opt<Date> start,
          Opt<Date> end, Opt<String> source, Opt<String> recordingState, Opt<Long> recordingLastHeard,
          Opt<String> presenters, Opt<Date> lastModifiedDate, Opt<String> checksum, Opt<Map<String,
          String>> workflowProperties, Opt<Map<String, String>> caProperties
  ) throws SchedulerServiceDatabaseException {
    try {
      db.execTxChecked(em -> {
        Optional<ExtendedEventDto> entityOpt = getExtendedEventDtoQuery(mediapackageId, organizationId).apply(em);
        ExtendedEventDto entity = entityOpt.orElse(new ExtendedEventDto());
        entity.setMediaPackageId(mediapackageId);
        entity.setOrganization(organizationId);
        if (captureAgentId.isSome()) {
          entity.setCaptureAgentId(captureAgentId.get());
        }
        if (start.isSome()) {
          entity.setStartDate(start.get());
        }
        if (end.isSome()) {
          entity.setEndDate(end.get());
        }
        if (source.isSome()) {
          entity.setSource(source.get());
        }
        if (recordingState.isSome()) {
          entity.setRecordingState(recordingState.get());
        }
        if (recordingLastHeard.isSome()) {
          entity.setRecordingLastHeard(recordingLastHeard.get());
        }
        if (presenters.isSome()) {
          entity.setPresenters(presenters.get());
        }
        if (lastModifiedDate.isSome()) {
          entity.setLastModifiedDate(lastModifiedDate.get());
        }
        if (checksum.isSome()) {
          entity.setChecksum(checksum.get());
        }
        if (workflowProperties.isSome()) {
          entity.setWorkflowProperties(gson.toJson(workflowProperties.get()));
        }
        if (caProperties.isSome()) {
          entity.setCaptureAgentProperties(gson.toJson(caProperties.get()));
        }

        if (entityOpt.isEmpty()) {
          em.persist(entity);
        } else {
          em.merge(entity);
        }
      });
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public List<String> getEvents(String captureAgentId, Date start, Date end, int separationMillis)
      throws SchedulerServiceDatabaseException {
    final Date extendedStart = Date.from(start.toInstant().minusMillis(separationMillis));
    final Date extendedEnd = Date.from(end.toInstant().plusMillis(separationMillis));
    try {
      return db.exec(namedQuery.findAll(
          "ExtendedEvent.findEvents",
          String.class,
          Pair.of("org", securityService.getOrganization().getId()),
          Pair.of("ca", captureAgentId),
          Pair.of("start", extendedStart),
          Pair.of("end", extendedEnd)
      ));
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public List<ExtendedEventDto> search(
      Opt<String> captureAgentId,
      Opt<Date> optStartsFrom,
      Opt<Date> optStartsTo,
      Opt<Date> optEndFrom,
      Opt<Date> optEndTo,
      Opt<Integer> limit) throws SchedulerServiceDatabaseException {
    final Date startsFrom = optStartsFrom.getOr(new Date(0));
    // A better value would be a Date initialized with Long.MAX_VALUE, but that leads to the DB (at least MySQL)
    // returning zero results.
    final Date farIntoTheFuture = DateTime.now().plusYears(30).toDate();
    final Date startsTo = optStartsTo.getOr(farIntoTheFuture);
    final Date endFrom = optEndFrom.getOr(new Date(0));
    final Date endTo = optEndTo.getOr(farIntoTheFuture);
    try {
      return db.exec(em -> {
        final TypedQuery<ExtendedEventDto> query;
        if (captureAgentId.isSome()) {
          query = em.createNamedQuery("ExtendedEvent.searchEventsCA", ExtendedEventDto.class)
              .setParameter("org", securityService.getOrganization().getId())
              .setParameter("ca", captureAgentId.get())
              .setParameter("startFrom", startsFrom)
              .setParameter("startTo", startsTo)
              .setParameter("endFrom", endFrom)
              .setParameter("endTo", endTo);
        } else {
          query = em.createNamedQuery("ExtendedEvent.searchEvents", ExtendedEventDto.class)
              .setParameter("org", securityService.getOrganization().getId())
              .setParameter("startFrom", startsFrom)
              .setParameter("startTo", startsTo)
              .setParameter("endFrom", endFrom)
              .setParameter("endTo", endTo);
        }
        if (limit.isSome()) {
          return query.setMaxResults(limit.get()).getResultList();
        }
        return query.getResultList();
      });
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public List<ExtendedEventDto> getKnownRecordings() throws SchedulerServiceDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "ExtendedEvent.knownRecordings",
          ExtendedEventDto.class,
          Pair.of("org", securityService.getOrganization().getId())
      ));
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public void deleteEvent(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    try {
      db.execTxChecked(em -> {
        final String orgId = securityService.getOrganization().getId();
        Optional<ExtendedEventDto> entity = getExtendedEventDtoQuery(mediapackageId, orgId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Event with ID " + mediapackageId + " does not exist");
        }
        em.remove(entity.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public List<ExtendedEventDto> getEvents() throws SchedulerServiceDatabaseException {
    final String organization = securityService.getOrganization().getId();
    try {
      return db.exec(namedQuery.findAll(
          "ExtendedEvent.findAll",
          ExtendedEventDto.class,
          Pair.of("org", organization)
      ));
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public Opt<ExtendedEventDto> getEvent(String mediapackageId, String orgId)
      throws SchedulerServiceDatabaseException {
    try {
      return db.exec(getExtendedEventDtoQuery(mediapackageId, orgId))
          .map(Opt::some)
          .orElse(Opt.none());
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public Opt<ExtendedEventDto> getEvent(String mediapackageId) throws SchedulerServiceDatabaseException {
    try {
      final String orgId = securityService.getOrganization().getId();
      return getEvent(mediapackageId, orgId);
    } catch (SchedulerServiceDatabaseException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public void resetRecordingState(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    try {
      db.execTxChecked(em -> {
        final String orgId = securityService.getOrganization().getId();
        Optional<ExtendedEventDto> entity = getExtendedEventDtoQuery(mediapackageId, orgId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Event with ID " + mediapackageId + " does not exist");
        }
        entity.get().setRecordingState(null);
        entity.get().setRecordingLastHeard(null);
        em.merge(entity.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  @Override
  public int countEvents() throws SchedulerServiceDatabaseException {
    try {
      return db.exec(namedQuery.find("ExtendedEvent.countAll", Number.class)).intValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  private Function<EntityManager, Optional<ExtendedEventDto>> getExtendedEventDtoQuery(String id, String orgId) {
    return namedQuery.findByIdOpt(ExtendedEventDto.class, new EventIdPK(id, orgId));
  }
}
