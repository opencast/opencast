/*
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

package org.opencastproject.usertracking.impl;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.usertracking.api.Footprint;
import org.opencastproject.usertracking.api.FootprintList;
import org.opencastproject.usertracking.api.Report;
import org.opencastproject.usertracking.api.ReportItem;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;
import org.opencastproject.usertracking.api.UserSession;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.endpoint.FootprintImpl;
import org.opencastproject.usertracking.endpoint.FootprintsListImpl;
import org.opencastproject.usertracking.endpoint.ReportImpl;
import org.opencastproject.usertracking.endpoint.ReportItemImpl;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

/**
 * Implementation of org.opencastproject.usertracking.api.UserTrackingService
 *
 * @see org.opencastproject.usertracking.api.UserTrackingService
 */
@Component(
    immediate = true,
    service = { UserTrackingService.class,ManagedService.class },
    property = {
        "service.description=User Tracking Service",
        "service.pid=org.opencastproject.usertracking.impl.UserTrackingServiceImpl"
    }
)
public class UserTrackingServiceImpl implements UserTrackingService, ManagedService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.usertracking";

  public static final String FOOTPRINT_KEY = "FOOTPRINT";

  public static final String DETAILED_TRACKING = "org.opencastproject.usertracking.detailedtrack";
  public static final String IP_LOGGING = "org.opencastproject.usertracking.log.ip";
  public static final String USER_LOGGING = "org.opencastproject.usertracking.log.user";
  public static final String SESSION_LOGGING = "org.opencastproject.usertracking.log.session";

  private static final Logger logger = LoggerFactory.getLogger(UserTrackingServiceImpl.class);

  private boolean detailedTracking = false;
  private boolean logIp = true;
  private boolean logUser = true;
  private boolean logSession = true;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.usertracking)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * Activation callback to be executed once all dependencies are set
   */
  @Activate
  public void activate() {
    logger.debug("activate()");
    db = dbSessionFactory.createSession(emf);
  }

  @Override
  public void updated(Dictionary props) throws ConfigurationException {
    if (props == null) {
      logger.debug("Null properties in user tracking service, not doing detailed logging");
      return;
    }

    Object val = props.get(DETAILED_TRACKING);
    if (val != null && String.class.isInstance(val)) {
      detailedTracking = Boolean.valueOf((String) val);
    }
    val = props.get(IP_LOGGING);
    if (val != null && String.class.isInstance(val)) {
      logIp = Boolean.valueOf((String) val);
    }
    val = props.get(USER_LOGGING);
    if (val != null && String.class.isInstance(val)) {
      logUser = Boolean.valueOf((String) val);
    }
    val = props.get(SESSION_LOGGING);
    if (val != null && String.class.isInstance(val)) {
      logSession = Boolean.valueOf((String) val);
    }

  }

  public int getViews(String mediapackageId) {
    return db.exec(namedQuery.find(
        "countSessionsOfMediapackage",
        Long.class,
        Pair.of("mediapackageId", mediapackageId)
    )).intValue();
  }

  public UserAction addUserFootprint(UserAction action, UserSession session) throws UserTrackingException {
    action.setType(FOOTPRINT_KEY);
    if (!logIp) session.setUserIp("-omitted-");
    if (!logUser) session.setUserId("-omitted-");
    if (!logSession) session.setSessionId("-omitted-");

    try {
      return db.execTx(em -> {
        UserSession userSession = populateSession(em, session);
        List<UserAction> userActions = em
            .createNamedQuery("findLastUserFootprintOfSession", UserAction.class)
            .setParameter("session", userSession)
            .setMaxResults(1)
            .getResultList();

        // no actions
        if (userActions.isEmpty()) {
          action.setSession(userSession);
          em.persist(action);
          return action;
        }

        // found last action
        UserAction lastAction = userActions.iterator().next();
        if (lastAction.getMediapackageId().equals(action.getMediapackageId())
            && lastAction.getType().equals(action.getType())
            && lastAction.getOutpoint() == action.getInpoint()) {
          // we are assuming in this case that the sessions match and are unchanged (IP wise, for example)
          action.setId(lastAction.getId());
          lastAction.setOutpoint(action.getOutpoint());
          em.persist(lastAction);
          return lastAction;
        }

        // last action does not match current action
        action.setSession(userSession);
        em.persist(action);
        return action;
      });
    } catch (Exception e) {
      throw new UserTrackingException(e);
    }
  }

  public UserAction addUserTrackingEvent(UserAction a, UserSession session) throws UserTrackingException {
    if (!logIp) session.setUserIp("-omitted-");
    if (!logUser) session.setUserId("-omitted-");
    if (!logSession) session.setSessionId("-omitted-");

    try {
      return db.execTx(em -> {
        UserSession userSession = populateSession(em, session);
        a.setSession(userSession);
        em.persist(a);
        return a;
      });
    } catch (Exception e) {
      throw new UserTrackingException(e);
    }
  }

  private synchronized UserSession populateSession(EntityManager em, UserSession session) {
    // assumption: this code is only called inside a DB transaction
    //             => transaction retries are handled outside this method
    try {
      // Try and find the session. If not found, persist it
      return namedQuery.find(
          "findUserSessionBySessionId",
          UserSession.class,
          Pair.of("sessionId", session.getSessionId())
      ).apply(em);
    } catch (NoResultException n) {
      em.persist(session);
      // Commit the session object so that it's immediately found by other threads
      EntityTransaction tx = em.getTransaction();
      tx.commit();
      tx.begin(); // start a new transaction to continue after session population
    }
    return session;
  }

  public UserActionList getUserActions(int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery().apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActions", UserAction.class)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  private Function<EntityManager, Integer> getTotalQuery() {
    return namedQuery.find("findTotal", Long.class)
        .andThen(Long::intValue);
  }

  public UserActionList getUserActionsByType(String type, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByType", UserAction.class)
          .setParameter("type", type)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  private Function<EntityManager, Integer> getTotalQuery(String type) {
    return namedQuery.find(
        "findTotalByType",
        Long.class,
        Pair.of("type", type)
    ).andThen(Long::intValue);
  }

  public UserActionList getUserActionsByTypeAndMediapackageId(String type, String mediapackageId, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, mediapackageId).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByTypeAndMediapackageId", UserAction.class)
          .setParameter("type", type)
          .setParameter("mediapackageId", mediapackageId)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  public UserActionList getUserActionsByTypeAndDay(String type, String day, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, calBegin, calEnd).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByTypeAndIntervall", UserAction.class)
          .setParameter("type", type)
          .setParameter("begin", calBegin, TemporalType.TIMESTAMP)
          .setParameter("end", calEnd, TemporalType.TIMESTAMP)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  public UserActionList getUserActionsByTypeAndMediapackageIdByDate(String type, String mediapackageId, int offset,
          int limit) {
    UserActionList result = new UserActionListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, mediapackageId).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByMediaPackageAndTypeAscendingByDate", UserAction.class)
          .setParameter("type", type)
          .setParameter("mediapackageId", mediapackageId)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  public UserActionList getUserActionsByTypeAndMediapackageIdByDescendingDate(String type, String mediapackageId,
          int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, mediapackageId).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByMediaPackageAndTypeDescendingByDate", UserAction.class)
          .setParameter("type", type)
          .setParameter("mediapackageId", mediapackageId)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  private Function<EntityManager, Integer> getTotalQuery(String type, Calendar calBegin, Calendar calEnd) {
    return namedQuery.find(
        "findTotalByTypeAndIntervall",
        Long.class,
        Pair.of("type", type),
        Pair.of("begin", calBegin),
        Pair.of("end", calEnd)
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalQuery(String type, String mediapackageId) {
    return namedQuery.find(
        "findTotalByTypeAndMediapackageId",
        Long.class,
        Pair.of("type", type),
        Pair.of("mediapackageId", mediapackageId)
    ).andThen(Long::intValue);
  }

  public UserActionList getUserActionsByDay(String day, int offset, int limit) {
    UserActionList result = new UserActionListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    db.exec(em -> {
      result.setTotal(getTotalQuery(calBegin, calEnd).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      TypedQuery<UserAction> q = em
          .createNamedQuery("findUserActionsByIntervall", UserAction.class)
          .setParameter("begin", calBegin, TemporalType.TIMESTAMP)
          .setParameter("end", calEnd, TemporalType.TIMESTAMP)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }
      q.getResultList().forEach(result::add);
    });

    return result;
  }

  private Function<EntityManager, Integer> getTotalQuery(Calendar calBegin, Calendar calEnd) {
    return namedQuery.find(
        "findTotalByIntervall",
        Long.class,
        Pair.of("begin", calBegin),
        Pair.of("end", calEnd)
    ).andThen(Long::intValue);
  }

  public Report getReport(int offset, int limit) {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    db.exec(em -> {
      TypedQuery<Object[]> q = em
          .createNamedQuery("countSessionsGroupByMediapackage", Object[].class)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }

      q.getResultList().forEach(row -> {
        ReportItem item = new ReportItemImpl();
        item.setEpisodeId((String) row[0]);
        item.setViews((Long) row[1]);
        item.setPlayed((Long) row[2]);
        report.add(item);
      });
    });

    return report;
  }

  public Report getReport(String from, String to, int offset, int limit) throws ParseException {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    Calendar calBegin = new GregorianCalendar();
    Calendar calEnd = new GregorianCalendar();
    SimpleDateFormat complex = new SimpleDateFormat("yyyyMMddhhmm");
    SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");

    // Try to parse the from calendar
    try {
      calBegin.setTime(complex.parse(from));
    } catch (ParseException e) {
      calBegin.setTime(simple.parse(from));
    }

    // Try to parse the to calendar
    try {
      calEnd.setTime(complex.parse(to));
    } catch (ParseException e) {
      calEnd.setTime(simple.parse(to));
    }

    db.exec(em -> {
      TypedQuery<Object[]> q = em
          .createNamedQuery("countSessionsGroupByMediapackageByIntervall", Object[].class)
          .setParameter("begin", calBegin, TemporalType.TIMESTAMP)
          .setParameter("end", calEnd, TemporalType.TIMESTAMP)
          .setFirstResult(offset);
      if (limit > 0) {
        q.setMaxResults(limit);
      }

      q.getResultList().forEach(row -> {
        ReportItem item = new ReportItemImpl();
        item.setEpisodeId((String) row[0]);
        item.setViews((Long) row[1]);
        item.setPlayed((Long) row[2]);
        report.add(item);
      });
    });

    return report;
  }

  public FootprintList getFootprints(String mediapackageId, String userId) {
    List<UserAction> userActions = db.exec(em -> {
      TypedQuery<UserAction> q;
      if (!logUser || StringUtils.trimToNull(userId) == null) {
        q = em.createNamedQuery("findUserActionsByTypeAndMediapackageIdOrderByOutpointDESC", UserAction.class);
      } else {
        q = em.createNamedQuery("findUserActionsByTypeAndMediapackageIdByUserOrderByOutpointDESC",
                UserAction.class)
            .setParameter("userid", userId);
      }
      q.setParameter("type", FOOTPRINT_KEY);
      q.setParameter("mediapackageId", mediapackageId);
      return q.getResultList();
    });

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
    for (int i = 0; i < resultArray.length; i++) {
      current = resultArray[i];
      if (last != current) {
        Footprint footprint = new FootprintImpl();
        footprint.setPosition(i);
        footprint.setViews(current);
        list.add(footprint);
      }
      last = current;
    }
    return list;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.usertracking.api.UserTrackingService#getUserAction(java.lang.Long)
   */
  @Override
  public UserAction getUserAction(Long id) throws UserTrackingException, NotFoundException {
    try {
      return db.exec(namedQuery.findByIdOpt(UserActionImpl.class, id)).orElseThrow(NoResultException::new);
    } catch (NoResultException e) {
      throw new NotFoundException("No UserAction found with id='" + id + "'");
    } catch (Exception e) {
      throw new UserTrackingException(e);
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
