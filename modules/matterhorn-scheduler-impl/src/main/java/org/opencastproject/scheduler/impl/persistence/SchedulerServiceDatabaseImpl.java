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

import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.ReadableInstant;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link SchedulerServiceDatabase}.
 */
public class SchedulerServiceDatabaseImpl implements SchedulerServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** Dublin core service for serializing and deserializing Dublin cores */
  protected DublinCoreCatalogService dcService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for scheduler");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.scheduler.impl.persistence",
            persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set persistence properties.
   *
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set persistence provider.
   *
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * OSGi callback to set dublin core catalog service.
   *
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Serializes Dublin core catalog and returns it as String.
   *
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   * @return String presenting serialized dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /**
   * Serializes Properties and returns them as string.
   *
   * @param caProperties
   *          Properties to be serialized
   * @return String representation of properties
   * @throws IOException
   *           if serialization fails
   */
  private String serializeProperties(Properties caProperties) throws IOException {
    StringWriter writer = new StringWriter();
    caProperties.store(writer, "Capture Agent specific data");
    return writer.toString();
  }

  /**
   * Parses Dublin core stored as string.
   *
   * @param dcXML
   *          string representation of Dublin core
   * @return parsed {@link DublinCoreCatalog}
   * @throws IOException
   *           if parsing fails
   */
  private DublinCoreCatalog parseDublinCore(String dcXML) throws IOException {
    DublinCoreCatalog dc = dcService.load(IOUtils.toInputStream(dcXML, "UTF-8"));
    return dc;
  }

  /**
   * Parses Properties represented as String.
   *
   * @param serializedProperties
   * @return parsed Properties
   * @throws IOException
   *           if parsing fails
   */
  private Properties parseProperties(String serializedProperties) throws IOException {
    Properties caProperties = new Properties();
    caProperties.load(new StringReader(serializedProperties));
    return caProperties;
  }

  @Override
  public void deleteEvent(long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Event with ID " + eventId + " does not exist");
      }
      em.remove(entity);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      }
      logger.error("Could not delete series: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public int countEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Event.countAll");
    try {
      Number total = (Number) query.getSingleResult();
      return total.intValue();
    } catch (Exception e) {
      logger.error("Could not find the number of events.", e);
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public DublinCoreCatalog[] getAllEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    Query query = null;
    List<EventEntity> eventEntities = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Event.findAll");
      eventEntities = query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all events: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
    List<DublinCoreCatalog> eventList = new LinkedList<DublinCoreCatalog>();
    try {
      for (EventEntity entity : eventEntities) {
        DublinCoreCatalog dc = parseDublinCore(entity.dublinCoreXML);
        eventList.add(dc);
      }
    } catch (Exception e) {
      logger.error("Could not parse event entity: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    }
    return eventList.toArray(new DublinCoreCatalog[eventList.size()]);
  }

  @Override
  public Properties getEventMetadata(long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Could not found series with ID " + eventId);
      }
      Properties caProperties = null;
      if (entity.getCaptureAgentMetadata() != null) {
        caProperties = parseProperties(entity.getCaptureAgentMetadata());
      }
      return caProperties;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve CA properties for event '{}': {}", eventId, e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEvent(DublinCoreCatalog event) throws NotFoundException, SchedulerServiceDatabaseException {
    if (event == null) {
      throw new SchedulerServiceDatabaseException("Cannot update <null> event");
    }
    Long eventId = Long.parseLong(event.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    String dcXML;
    try {
      dcXML = serializeDublinCore(event);
    } catch (Exception e1) {
      logger.error("Could not serialize Dublin Core: {}", e1);
      throw new SchedulerServiceDatabaseException(e1);
    }
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Event with ID " + eventId + " does not exist.");
      }
      entity.setEventDublinCore(dcXML);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not store event: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }

  }

  @Override
  public void storeEvents(DublinCoreCatalog... events) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      for (DublinCoreCatalog event : events) {
        Long eventId = Long.parseLong(event.getFirst(DublinCore.PROPERTY_IDENTIFIER));
        String dcXML;
        try {
          dcXML = serializeDublinCore(event);
        } catch (Exception e1) {
          logger.error("Could not serialize Dublin Core: {}", e1);
          throw new SchedulerServiceDatabaseException(e1);
        }
        EventEntity entity = new EventEntity();
        entity.setEventId(eventId);
        entity.setEventDublinCore(dcXML);
        em.persist(entity);
      }
      tx.commit();
    } catch (SchedulerServiceDatabaseException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not store events: {}", e);
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventWithMetadata(long eventId, Properties caProperties) throws SchedulerServiceDatabaseException,
          NotFoundException {
    if (caProperties == null) {
      caProperties = new Properties();
    }
    String caSerialized;
    try {
      caSerialized = serializeProperties(caProperties);
    } catch (IOException e) {
      logger.error("Could not serialize properties: {}", e);
      throw new SchedulerServiceDatabaseException(e);
    }
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Event with ID: " + eventId + " does not exist");
      }
      entity.setCaptureAgentMetadata(caSerialized);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      logger.error("Event with ID '{}' does not exist", eventId);
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not store event metadata: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventAccessControlList(long eventId, AccessControlList accessControlList) throws NotFoundException,
          SchedulerServiceDatabaseException {
    String aclSerialized = null;
    try {
      if (accessControlList != null)
        aclSerialized = AccessControlParser.toJson(accessControlList);
    } catch (IOException e) {
      logger.error("Could not serialize access control list: {}", e);
      throw new SchedulerServiceDatabaseException(e);
    }

    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity event = em.find(EventEntity.class, eventId);
      if (event == null)
        throw new NotFoundException("Event " + eventId + " does not exist");

      event.setAccessControl(aclSerialized);
      em.merge(event);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not update the access control list for event '{}': {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventMediaPackageId(long eventId, String mediaPackageId) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity event = em.find(EventEntity.class, eventId);
      if (event == null)
        throw new NotFoundException("Event " + eventId + " does not exist");

      event.setMediaPackageId(mediaPackageId);
      em.merge(event);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not update the mediapackage for event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public AccessControlList getAccessControlList(long eventId) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = em.find(EventEntity.class, eventId);
      if (event == null)
        throw new NotFoundException("Event " + eventId + " does not exist");

      return event.getAccessControl() != null ? AccessControlParser.parseAcl(event.getAccessControl()) : null;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve mediapackage for event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public String getMediaPackageId(long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = em.find(EventEntity.class, eventId);
      if (event == null)
        throw new NotFoundException("Event " + eventId + " does not exist");

      return event.getMediaPackageId();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve mediapackage for event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Long getEventId(String mediaPackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = getEventEntityByMpId(mediaPackageId, em);
      if (event == null)
        throw new NotFoundException("Event with MP ID " + mediaPackageId + " does not exist");

      return event.getEventId();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve event id for event with mediapackage '{}': {}", mediaPackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean isOptOut(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = getEventEntityByMpId(mediapackageId, em);
      if (event == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      return event.isOptOut();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve review status for event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean isOptOut(Long eventid) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = em.find(EventEntity.class, eventid);
      if (event == null) {
        throw new NotFoundException("Event with ID " + eventid + " does not exist");
      }
      return event.isOptOut();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve review status for event with mediapackage '{}': {}", eventid,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventOptOutStatus(String mediapackageId, boolean optOut) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = getEventEntityByMpId(mediapackageId, em);
      if (entity == null) {
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");
      }
      entity.setOptOut(optOut);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not updated event opted out status: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventOptOutStatus(Long eventId, boolean optOut) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Event with ID " + eventId + " does not exist");
      }
      entity.setOptOut(optOut);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not updated event opted out status: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public ReviewStatus getReviewStatus(String mediapackageId) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = getEventEntityByMpId(mediapackageId, em);
      if (event == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      return event.getReviewStatus();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve review status for event with mediapackage '{}': {}", mediapackageId,
              e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Date getReviewDate(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = getEventEntityByMpId(mediapackageId, em);
      if (event == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      return event.getReviewDate();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve review date for event with mediapackage '{}': {}", mediapackageId,
              e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventReviewStatus(String mediapackageId, ReviewStatus reviewStatus, Date modificationDate)
          throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = getEventEntityByMpId(mediapackageId, em);
      if (entity == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      entity.setReviewStatus(reviewStatus);
      entity.setReviewDate(modificationDate);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not updated event review status: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean isBlacklisted(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = getEventEntityByMpId(mediapackageId, em);
      if (event == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      return event.isBlacklisted();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve blacklist status for event with mediapackage '{}': {}", mediapackageId,
              e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean isBlacklisted(Long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      EventEntity event = em.find(EventEntity.class, eventId);
      if (event == null) {
        throw new NotFoundException("Event with ID " + eventId + " does not exist");
      }
      return event.isBlacklisted();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve review status for event with mediapackage '{}': {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventBlacklistStatus(String mediapackageId, boolean blacklisted) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = getEventEntityByMpId(mediapackageId, em);
      if (entity == null)
        throw new NotFoundException("Event with MP ID " + mediapackageId + " does not exist");

      entity.setBlacklistStatus(blacklisted);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not updated event blacklist status: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateEventBlacklistStatus(Long eventId, boolean blacklisted) throws NotFoundException,
          SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EventEntity entity = em.find(EventEntity.class, eventId);
      if (entity == null) {
        throw new NotFoundException("Event with ID " + eventId + " does not exist");
      }
      entity.setBlacklistStatus(blacklisted);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not updated event blacklist status: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countConfirmed");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countQuarterConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countConfirmedByDateRange");
      setDateForQuarterQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countDailyConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countConfirmedByDateRange");
      setDateForDailyQuery(q);
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countTotalResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countRespones");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countUnconfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countUnconfirmed");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public long countOptedOutResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Event.countOptedOut");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets an event by its mediapackage ID.
   *
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param em
   *          an open entity manager
   * @return the event entity, or null if not found
   */
  protected EventEntity getEventEntityByMpId(String mediaPackageId, EntityManager em) {
    Query q = em.createNamedQuery("Event.findByMpId").setParameter("mpId", mediaPackageId);
    try {
      return (EventEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /** The beginnings of a quarter independent from the year. */
  private final List<Partial> quarterBeginnings = list(partial(DateTimeConstants.JANUARY, 1),
          partial(DateTimeConstants.APRIL, 1), partial(DateTimeConstants.JULY, 1),
          partial(DateTimeConstants.OCTOBER, 1));

  private DateTimeFieldType[] partialFields;

  /**
   * Add the correct start and end value for the given daily count query.
   * <p/>
   * Please note that the start instant is inclusive while the end instant is exclusive.
   *
   * @param query
   *          The query where the parameters have to be added
   * @return the same query instance
   */
  private Query setDateForDailyQuery(Query query) {
    final DateTime today = new DateTime().withTimeAtStartOfDay();
    return query.setParameter("start", today.toDate()).setParameter("end", today.plusDays(1).toDate());
  }

  /**
   * Add the correct start and end value for the given quarter count query
   * <p/>
   * Please note that the start instant is inclusive while the end instant is exclusive.
   *
   * @param query
   *          The query where the parameters have to be added
   * @return the same query instance
   */
  private Query setDateForQuarterQuery(Query query) {
    final DateTime today = new DateTime().withTimeAtStartOfDay();
    final Partial partialToday = partialize(today);
    final DateTime quarterBeginning = mlist(quarterBeginnings).foldl(quarterBeginnings.get(0),
            new Function2<Partial, Partial, Partial>() {
              @Override
              public Partial apply(Partial sum, Partial quarterBeginning) {
                return partialToday.isAfter(quarterBeginning) ? quarterBeginning : sum;
              }
            }).toDateTime(today);
    return query.setParameter("start", quarterBeginning.toDate()).setParameter("end",
            quarterBeginning.plusMonths(3).toDate());
  }

  /** Create a Partial from an Instant extracting month and day. */
  private Partial partialize(ReadableInstant instant) {
    return partial(instant.get(DateTimeFieldType.monthOfYear()), instant.get(DateTimeFieldType.dayOfMonth()));
  }

  private DateTimeFieldType[] getPartialFields() {
    if (partialFields == null) {
      partialFields = array(DateTimeFieldType.monthOfYear(), DateTimeFieldType.dayOfMonth());
    }
    return partialFields;
  }

  /** Create a Partial consisting of only month and day. */
  private Partial partial(int month, int dayOfMonth) {
    return new Partial(getPartialFields(), new int[] { month, dayOfMonth });
  }

}
