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

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

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

  @Override
  public void touchLastEntry(String agentId) throws SchedulerServiceDatabaseException {
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
      logger.error("Could not updated last modifed date of agent {} status: {}", agentId, getStackTrace(e));
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
      logger.error("Could not retrieve last modified date for agent with id '{}': {}", agentId, getStackTrace(e));
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
      logger.error("Could not retrieve last modified dates: {}", getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
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
      Opt<ExtendedEventDto> entity = getExtendedEventDto(mediapackageId, em);
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
      logger.error("Could not delete extended event: {}", getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public int countEvents() throws SchedulerServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("ExtendedEvent.countAll");
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

  /**
   * Gets an extended event by its ID, using the current organizational context.
   *
   * @param id
   *          the mediapackage identifier
   * @param em
   *          an open entity manager
   * @return the extended entity option
   */
  private Opt<ExtendedEventDto> getExtendedEventDto(String id, EntityManager em) {
    String orgId = securityService.getOrganization().getId();
    return Opt.nul(em.find(ExtendedEventDto.class, new EventIdPK(id, orgId)));
  }
}
