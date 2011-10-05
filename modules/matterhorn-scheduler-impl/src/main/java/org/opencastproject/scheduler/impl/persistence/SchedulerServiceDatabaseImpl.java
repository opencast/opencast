/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.scheduler.impl.persistence;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceDatabase#deleteEvent(java.lang.String)
   */
  @Override
  public void deleteEvent(long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
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
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceDatabase#getAllEvents()
   */
  @SuppressWarnings("unchecked")
  @Override
  public DublinCoreCatalog[] getAllEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createQuery("SELECT e FROM EventEntity e");
    List<EventEntity> eventEntities = null;
    try {
      eventEntities = (List<EventEntity>) query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all events: {}", e.getMessage());
      throw new SchedulerServiceDatabaseException(e);
    } finally {
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

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceDatabase#getEventMetadata(long)
   */
  @Override
  public Properties getEventMetadata(long eventId) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
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
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.scheduler.impl.SchedulerServiceDatabase#updateEvent(org.opencastproject.metadata.dublincore
   * .DublinCoreCatalog)
   */
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
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
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
      em.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceDatabase#storeEvents(java.util.List)
   */
  @Override
  public void storeEvents(DublinCoreCatalog... events) throws SchedulerServiceDatabaseException {

    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
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
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceDatabase#updateEventWithMetadata(java.lang.String,
   * java.util.Properties)
   */
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
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
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
      em.close();
    }
  }
}
