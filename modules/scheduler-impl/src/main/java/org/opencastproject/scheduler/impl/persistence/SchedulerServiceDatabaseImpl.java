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

import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * Implements {@link SchedulerServiceDatabase}.
 */
public class SchedulerServiceDatabaseImpl implements SchedulerServiceDatabase {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.scheduler.impl.persistence";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceDatabaseImpl.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  /** The security service */
  private SecurityService securityService;

  private static final Gson gson = new Gson();

  /** OSGi DI */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /** OSGi DI */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for scheduler");
  }

  /*
   * We need to synchronize this method because JPA doesn't support thread-safe atomic upserts.
   */
  @Override
  public synchronized void touchLastEntry(String agentId) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
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
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Date getLastModified(String agentId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      LastModifiedDto entity = em.find(LastModifiedDto.class, agentId);
      if (entity == null)
        throw new NotFoundException("Agent with ID " + agentId + " does not exist");

      return entity.getLastModifiedDate();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Date> getLastModifiedDates() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("LastModified.findAll");
      List<LastModifiedDto> resultList = q.getResultList();
      Map<String, Date> dates = new HashMap<String, Date>();
      for (LastModifiedDto dto : resultList) {
        dates.put(dto.getCaptureAgentId(), dto.getLastModifiedDate());
      }
      return dates;
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void storeEvent(String mediapackageId, String organizationId, Opt<String> captureAgentId, Opt<Date> start,
          Opt<Date> end, Opt<String> source, Opt<String> recordingState, Opt<Long> recordingLastHeard,
          Opt<String> presenters, Opt<Date> lastModifiedDate, Opt<String> checksum, Opt<Map<String,
          String>> workflowProperties, Opt<Map<String, String>> caProperties
  ) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Opt<ExtendedEventDto> entityOpt = getExtendedEventDto(mediapackageId, organizationId, em);
      ExtendedEventDto entity = entityOpt.getOr(new ExtendedEventDto());
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

      if (entityOpt.isNone()) {
        em.persist(entity);
      } else {
        em.merge(entity);
      }
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getEvents(String captureAgentId, Date start, Date end, int separationMillis) throws SchedulerServiceDatabaseException {
    final Date extendedStart = Date.from(start.toInstant().minusMillis(separationMillis));
    final Date extendedEnd = Date.from(end.toInstant().plusMillis(separationMillis));
    final EntityManager em = emf.createEntityManager();
    final Query query = em.createNamedQuery("ExtendedEvent.findEvents")
        .setParameter("org", securityService.getOrganization().getId())
        .setParameter("ca", captureAgentId)
        .setParameter("start", extendedStart)
        .setParameter("end", extendedEnd);
    try {
      return query.getResultList();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ExtendedEventDto> search(
      Opt<String> captureAgentId,
      Opt<Date> optStartsFrom,
      Opt<Date> optStartsTo,
      Opt<Date> optEndFrom,
      Opt<Date> optEndTo,
      Opt<Integer> limit) throws SchedulerServiceDatabaseException {
    final EntityManager em = emf.createEntityManager();
    final Date startsFrom = optStartsFrom.getOr(new Date(0));
    // A better value would be a Date initialized with Long.MAX_VALUE, but that leads to the DB (at least MySQL)
    // returning zero results.
    final Date farIntoTheFuture = DateTime.now().plusYears(30).toDate();
    final Date startsTo = optStartsTo.getOr(farIntoTheFuture);
    final Date endFrom = optEndFrom.getOr(new Date(0));
    final Date endTo = optEndTo.getOr(farIntoTheFuture);

    final Query query;
    if (captureAgentId.isSome()) {
      query = em.createNamedQuery("ExtendedEvent.searchEventsCA")
          .setParameter("org", securityService.getOrganization().getId())
          .setParameter("ca", captureAgentId.get())
          .setParameter("startFrom", startsFrom)
          .setParameter("startTo", startsTo)
          .setParameter("endFrom", endFrom)
          .setParameter("endTo", endTo);
    } else {
      query = em.createNamedQuery("ExtendedEvent.searchEvents")
          .setParameter("org", securityService.getOrganization().getId())
          .setParameter("startFrom", startsFrom)
          .setParameter("startTo", startsTo)
          .setParameter("endFrom", endFrom)
          .setParameter("endTo", endTo);
    }
    try {
      if (limit.isSome()) {
        return query.setMaxResults(limit.get()).getResultList();
      }
      return query.getResultList();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  public List<ExtendedEventDto> getKnownRecordings() throws SchedulerServiceDatabaseException {
    final EntityManager em = emf.createEntityManager();
    final Query query = em.createNamedQuery("ExtendedEvent.knownRecordings")
        .setParameter("org", securityService.getOrganization().getId());
    try {
      return query.getResultList();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  public void deleteEvent(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      final String orgId = securityService.getOrganization().getId();
      Opt<ExtendedEventDto> entity = getExtendedEventDto(mediapackageId, orgId, em);
      if (entity.isNone()) {
        throw new NotFoundException("Event with ID " + mediapackageId + " does not exist");
      }
      em.remove(entity.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<ExtendedEventDto> getEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    final String organization = securityService.getOrganization().getId();
    try {
      em = emf.createEntityManager();
      TypedQuery<ExtendedEventDto> query = em.createNamedQuery("ExtendedEvent.findAll", ExtendedEventDto.class)
                      .setParameter("org", organization);
      return query.getResultList();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Opt<ExtendedEventDto> getEvent(String mediapackageId, String orgId) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getExtendedEventDto(mediapackageId, orgId, em);
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
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
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      final String orgId = securityService.getOrganization().getId();
      Opt<ExtendedEventDto> entity = getExtendedEventDto(mediapackageId, orgId, em);
      if (entity.isNone()) {
        throw new NotFoundException("Event with ID " + mediapackageId + " does not exist");
      }
      entity.get().setRecordingState(null);
      entity.get().setRecordingLastHeard(null);
      em.merge(entity.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public int countEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("ExtendedEvent.countAll");
    try {
      Number total = (Number) query.getSingleResult();
      return total.intValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  private Opt<ExtendedEventDto> getExtendedEventDto(String id, String orgId, EntityManager em) {
    return Opt.nul(em.find(ExtendedEventDto.class, new EventIdPK(id, orgId)));
  }
}
