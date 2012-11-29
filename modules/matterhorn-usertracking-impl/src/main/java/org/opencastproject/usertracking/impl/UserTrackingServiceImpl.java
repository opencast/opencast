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
package org.opencastproject.usertracking.impl;

import org.opencastproject.search.api.SearchService;
import org.opencastproject.usertracking.api.Footprint;
import org.opencastproject.usertracking.api.FootprintList;
import org.opencastproject.usertracking.api.Report;
import org.opencastproject.usertracking.api.ReportItem;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.endpoint.FootprintImpl;
import org.opencastproject.usertracking.endpoint.FootprintsListImpl;
import org.opencastproject.usertracking.endpoint.ReportImpl;
import org.opencastproject.usertracking.endpoint.ReportItemImpl;
import org.opencastproject.util.NotFoundException;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Collection;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implementation of org.opencastproject.usertracking.api.UserTrackingService
 * 
 * @see org.opencastproject.usertracking.api.UserTrackingService
 */
public class UserTrackingServiceImpl implements UserTrackingService, ManagedService {

  public static final String FOOTPRINT_KEY = "FOOTPRINT";

  private static final Logger logger = LoggerFactory.getLogger(UserTrackingServiceImpl.class);

  @SuppressWarnings("unused")
  private SearchService searchService;

  private boolean detailedTracking = false;

  /**
   * Sets the search service
   * 
   * @param searchService
   *          the search service to set
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  protected Map<String, Object> persistenceProperties;

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * The JPA provider
   */
  protected PersistenceProvider persistenceProvider;

  /**
   * Activation callback to be executed once all dependencies are set
   */
  public void activate() {
    logger.debug("activate()");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.usertracking", persistenceProperties);
  }

  /**
   * Deactivation callback
   */
  public void destroy() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @Override
  public void updated(Dictionary props) throws ConfigurationException {
    if (props == null) {
      logger.debug("Null properties in user tracking service, not doing detailed logging");
      return;
    }

    Object val = props.get("org.opencastproject.usertracking.detailedtrack");
    if (val != null && String.class.isInstance(val)) {
      detailedTracking = Boolean.valueOf((String) val);
    }
  }

  public int getViews(String mediapackageId) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("countSessionsOfMediapackage");
      q.setParameter("mediapackageId", mediapackageId);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public UserAction addUserFootprint(UserAction a) throws UserTrackingException {
    a.setType("FOOTPRINT");
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query q = em.createNamedQuery("findLastUserFootprintOfSession");
      q.setMaxResults(1);
      q.setParameter("sessionId", a.getSessionId());
      Collection<UserAction> userActions = q.getResultList();

      if (userActions.size() >= 1) {
        UserAction last = userActions.iterator().next();
        if (last.getMediapackageId().equals(a.getMediapackageId()) && last.getType().equals(a.getType())
                && last.getOutpoint() == a.getInpoint()) {
          last.setOutpoint(a.getOutpoint());
          a = last;
        } else {
          em.persist(a);
        }
      } else {
        em.persist(a);
      }
      tx.commit();
      return a;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new UserTrackingException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  public UserAction addUserTrackingEvent(UserAction a) throws UserTrackingException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.persist(a);
      tx.commit();
      return a;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new UserTrackingException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public UserActionList getUserActions(int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    result.setTotal(getTotal());
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActions");
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }

      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  private int getTotal() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotal");
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public UserActionList getUserActionsByType(String type, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    result.setTotal(getTotal(type));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByType");
      q.setParameter("type", type);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  private int getTotal(String type) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotalByType");
      q.setParameter("type", type);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  public UserActionList getUserActionsByTypeAndMediapackageId(String type, String mediapackageId, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    result.setTotal(getTotal(type, mediapackageId));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByTypeAndMediapackageId");
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      @SuppressWarnings("unchecked")
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public UserActionList getUserActionsByTypeAndDay(String type, String day, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    result.setTotal(getTotal(type, calBegin, calEnd));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByTypeAndIntervall");
      q.setParameter("type", type);
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  public UserActionList getUserActionsByTypeAndMediapackageIdByDate(String type, String mediapackageId, int offset,
          int limit) {
    UserActionList result = new UserActionListImpl();

    result.setTotal(getTotal(type, mediapackageId));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByMediaPackageAndTypeAscendingByDate");
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      @SuppressWarnings("unchecked")
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  public UserActionList getUserActionsByTypeAndMediapackageIdByDescendingDate(String type, String mediapackageId,
          int offset, int limit) {
    UserActionList result = new UserActionListImpl();
    result.setTotal(getTotal(type, mediapackageId));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByMediaPackageAndTypeDescendingByDate");
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      @SuppressWarnings("unchecked")
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public UserSummaryListImpl getUserSummaryByTypeAndMediaPackage(String type, String mediapackageId) {
    UserSummaryListImpl result = new UserSummaryListImpl();
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("userSummaryByMediapackageByType");
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      List<Object[]> users = q.getResultList();
      for (Object[] user : users) {
        UserSummaryImpl userSummary = new UserSummaryImpl();
        userSummary.ingest(user);
        result.add(userSummary);
      }
      return result;
    } catch (Exception e) { 
      logger.warn("Unable to return any results from mediapackage " + mediapackageId + " of type " + type + " because of ", e);
      return result;
    }
    finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  /*@SuppressWarnings("unchecked")
  public UserActionList getUserActionsByTypeAndDay(String type, String day, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    result.setTotal(getTotal(type, calBegin, calEnd));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      em.createQuery(arg0);
      Query q = em.createNamedQuery("findUserActionsByTypeAndIntervall");
      q.setParameter("type", type);
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }*/
  
  private int getTotal(String type, Calendar calBegin, Calendar calEnd) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotalByTypeAndIntervall");
      q.setParameter("type", type);
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  private int getTotal(String type, String mediapackageId) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotalByTypeAndMediapackageId");
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public UserActionList getUserActionsByDay(String day, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    result.setTotal(getTotal(calBegin, calEnd));
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByIntervall");
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<UserAction> userActions = q.getResultList();

      for (UserAction a : userActions) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  private int getTotal(Calendar calBegin, Calendar calEnd) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotalByIntervall");
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  private int getDistinctEpisodeIdTotal(Calendar calBegin, Calendar calEnd) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findDistinctEpisodeIdTotalByIntervall");
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  public Report getReport(int offset, int limit) {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("countSessionsGroupByMediapackage");
      q.setFirstResult(offset);
      q.setMaxResults(limit);

      @SuppressWarnings("unchecked")
      List<Object[]> result = q.getResultList();
      ReportItem item;

      for (Object[] a : result) {
        item = new ReportItemImpl();
        item.setEpisodeId((String) a[0]);
        item.setViews((Long) a[1]);
        item.setPlayed((Long) a[2]);
        report.add(item);
      }
      return report;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  public Report getReport(String from, String to, int offset, int limit) {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    Calendar calBegin = new GregorianCalendar();
    Calendar calEnd = new GregorianCalendar();
    
    // Expecting a date in the format 20121231 or 201212312359
    if (from.length() == 8 && to.length() == 8) {
    	int year = Integer.parseInt(from.substring(0, 4));
        int month = Integer.parseInt(from.substring(4, 6)) - 1;
        int date = Integer.parseInt(from.substring(6, 8));
        calBegin.set(year, month, date, 0, 0);

        year = Integer.parseInt(to.substring(0, 4));
        month = Integer.parseInt(to.substring(4, 6)) - 1;
        date = Integer.parseInt(to.substring(6, 8));
        calEnd.set(year, month, date, 23, 59);
    } else {
    	int year = Integer.parseInt(from.substring(0, 4));
        int month = Integer.parseInt(from.substring(4, 6)) - 1;
        int date = Integer.parseInt(from.substring(6, 8));
        int hour = Integer.parseInt(from.substring(8, 10));
        int minute = Integer.parseInt(from.substring(10, 12));
        calBegin.set(year, month, date, hour, minute);

        year = Integer.parseInt(to.substring(0, 4));
        month = Integer.parseInt(to.substring(4, 6)) - 1;
        date = Integer.parseInt(to.substring(6, 8));
        hour = Integer.parseInt(to.substring(8, 10));
        minute = Integer.parseInt(to.substring(10, 12));
        calEnd.set(year, month, date, hour, minute);
    }
    
    

    report.setTotal(getDistinctEpisodeIdTotal(calBegin, calEnd));

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("countSessionsGroupByMediapackageByIntervall");
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);

      @SuppressWarnings("unchecked")
      List<Object[]> result = q.getResultList();
      ReportItem item;

      for (Object[] a : result) {
        item = new ReportItemImpl();
        item.setEpisodeId((String) a[0]);
        item.setViews((Long) a[1]);
        item.setPlayed((Long) a[2]);
        report.add(item);
      }
      return report;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }
  
  public FootprintList getFootprints(String mediapackageId, String userId) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findUserActionsByTypeAndMediapackageIdOrderByOutpointDESC");
      q.setParameter("type", FOOTPRINT_KEY);
      q.setParameter("mediapackageId", mediapackageId);
      @SuppressWarnings("unchecked")
      Collection<UserAction> userActions = q.getResultList();

      int[] resultArray = new int[1];
      boolean first = true;

      for (UserAction a : userActions) {
        if (first) {
          // Get one more item than the known outpoint to append a footprint of 0 views at the end of the result set
          resultArray = new int[a.getOutpoint() + 1];
          first = false;
        }
        for (int i = a.getInpoint(); i < a.getOutpoint(); i++) {
          resultArray[i]++;
        }
      }
      FootprintList list = new FootprintsListImpl();
      int current = -1;
      int last = -1;
      int lastPositionAdded = -1;
      for (int i = 0; i < resultArray.length; i++) {
        current = resultArray[i];
        if (last != current) {
          Footprint footprint = new FootprintImpl();
          footprint.setPosition(i);
          footprint.setViews(current);
          list.add(footprint);
          lastPositionAdded = i;
        }
        last = current;
      }
      return list;
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.usertracking.api.UserTrackingService#getUserAction(java.lang.Long)
   */
  @Override
  public UserAction getUserAction(Long id) throws UserTrackingException, NotFoundException {
    EntityManager em = null;
    UserActionImpl result = null;
    try {
      em = emf.createEntityManager();
      result = em.find(UserActionImpl.class, id);
    } catch (Exception e) {
      throw new UserTrackingException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
    if (result == null) {
      throw new NotFoundException("No UserAction found with id='" + id + "'");
    } else {
      return result;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.usertracking.api.UserTrackingService#getUserTrackingEnabled()
   */
  @Override
  public boolean getUserTrackingEnabled() {
    return detailedTracking;
  }
}
