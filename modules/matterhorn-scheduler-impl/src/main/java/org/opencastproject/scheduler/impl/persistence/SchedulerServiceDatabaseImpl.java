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

import static com.entwinemedia.fn.Stream.$;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.data.Arrays.array;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.scheduler.api.Blacklist;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabase;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.ReadableInstant;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
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
  public String getTransactionId(String source) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("Transaction.findBySource").setParameter("source", source).setParameter("org",
              orgId);
      TransactionDto dto = (TransactionDto) q.getSingleResult();
      return dto.getId();
    } catch (NoResultException e) {
      throw new NotFoundException("Transaction with source " + source + " does not exist");
    } catch (Exception e) {
      logger.error("Could not retrieve id for transaction with source '{}': {}", source, getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public String getTransactionSource(String id) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Opt<TransactionDto> entity = getTransactionDto(id, em);
      if (entity.isNone())
        throw new NotFoundException("Transaction with ID " + id + " does not exist");

      return entity.get().getSource();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve source for transaction with id '{}': {}", id, getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Date getTransactionLastModified(String id) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Opt<TransactionDto> entity = getTransactionDto(id, em);
      if (entity.isNone())
        throw new NotFoundException("Transaction with ID " + id + " does not exist");

      return entity.get().getLastModifiedDate();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve last modified date for transaction with id '{}': {}", id, getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getTransactions() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("Transaction.findAll").setParameter("org", orgId);
      List<TransactionDto> resultList = q.getResultList();
      return $(resultList).map(new Fn<TransactionDto, String>() {
        @Override
        public String apply(TransactionDto trx) {
          return trx.getId();
        }
      }).toList();
    } catch (Exception e) {
      logger.error("Could not retrieve transactions: {}", getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean hasTransaction(String source) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("Transaction.findBySource").setParameter("source", source).setParameter("org",
              orgId);
      q.getSingleResult();
      return true;
    } catch (NoResultException e) {
      return false;
    } catch (Exception e) {
      logger.error("Could not retrieve transaction with source '{}': {}", source, getStackTrace(e));
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void storeTransaction(String id, String source) throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Opt<TransactionDto> entityOpt = getTransactionDto(id, em);
      TransactionDto entity = entityOpt.getOr(new TransactionDto());
      if (entityOpt.isNone()) {
        entity.setId(id);
        entity.setOrganization(securityService.getOrganization().getId());
        entity.setSource(source);
        entity.setLastModifiedDate(new Date());
        em.persist(entity);
      } else {
        entity.setLastModifiedDate(new Date());
        em.merge(entity);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not store transaction: {}", getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteTransaction(String id) throws NotFoundException, SchedulerServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Opt<TransactionDto> entity = getTransactionDto(id, em);
      if (entity.isNone())
        throw new NotFoundException("Transaction with ID " + id + " does not exist");

      em.remove(entity.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete transaction with id '{}': {}", id, getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public boolean isBlacklisted(List<String> presenters, Date start, Date end) throws SchedulerServiceDatabaseException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isBlacklisted(String agentId, Date start, Date end) throws SchedulerServiceDatabaseException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<Tuple<String, Boolean>> updateBlacklist(Blacklist blacklist) throws SchedulerServiceDatabaseException {
    // TODO Auto-generated method stub
    // TODO updates last modified state
    return new ArrayList<Tuple<String, Boolean>>();
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

  public long countConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("ExtendedEvent.countConfirmed");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public long countQuarterConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("ExtendedEvent.countConfirmedByDateRange");
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

  public long countDailyConfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("ExtendedEvent.countConfirmedByDateRange");
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

  public long countTotalResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("ExtendedEvent.countRespones");
      Number countResult = (Number) q.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public long countUnconfirmedResponses() throws SchedulerServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("ExtendedEvent.countUnconfirmed");
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
   * Gets a transaction by its ID, using the current organizational context.
   *
   * @param id
   *          the transaction identifier
   * @param em
   *          an open entity manager
   * @return the transaction entity option
   */
  private Opt<TransactionDto> getTransactionDto(String id, EntityManager em) {
    String orgId = securityService.getOrganization().getId();
    return Opt.nul(em.find(TransactionDto.class, new EventIdPK(id, orgId)));
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
    final DateTime quarterBeginning = mlist(quarterBeginnings)
            .foldl(quarterBeginnings.get(0), new Function2<Partial, Partial, Partial>() {
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
