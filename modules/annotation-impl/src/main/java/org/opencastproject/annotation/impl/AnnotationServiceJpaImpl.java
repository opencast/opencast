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

package org.opencastproject.annotation.impl;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.annotation.api.Annotation;
import org.opencastproject.annotation.api.AnnotationList;
import org.opencastproject.annotation.api.AnnotationService;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 * JPA-based implementation of the {@link AnnotationService}
 */
@Component(
    immediate = true,
    service = AnnotationService.class,
    property = {
        "service.description=Annotation Service"
    }
)
public class AnnotationServiceJpaImpl implements AnnotationService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.annotation";

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** Opencast's security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.annotation)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Activate
  public void activate() {
    db = dbSessionFactory.createSession(emf);
  }

  @Deactivate
  public void deactivate() {
    db.close();
  }

  /**
   * Sets the opencast security service
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public Annotation addAnnotation(Annotation a) {
    // set the User ID on the annotation
    a.setUserId(securityService.getUser().getUsername());
    return db.execTx(namedQuery.persist(a));
  }

  public boolean removeAnnotation(Annotation a) {
    try {
      db.execTxChecked(em -> {
        // first merge then remove element
        em.remove(em.merge(a));
      });
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public Annotation changeAnnotation(Annotation a) throws NotFoundException {
    long id = a.getAnnotationId();
    return db.execTx(em -> {
      Query q = em.createNamedQuery("updateAnnotation");
      q.setParameter("value", a.getValue());
      q.setParameter("annotationId", id);
      int no = q.executeUpdate();

      AnnotationImpl b = null;
      if (no == 1) {
        b = em.find(AnnotationImpl.class, id);
      }
      return b;
    });
  }

  public Annotation getAnnotation(long id) throws NotFoundException {
    AnnotationImpl a = db.exec(namedQuery.findById(AnnotationImpl.class, id));
    if (a == null) {
      throw new NotFoundException("Annotation '" + id + "' not found");
    }
    return a;
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotations(int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery().apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      Query q = em.createNamedQuery("findAnnotations");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  public AnnotationList getAnnotationsByTypeAndMediapackageId(String type, String mediapackageId, int offset,
      int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, mediapackageId).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      Query q = em.createNamedQuery("findAnnotationsByTypeAndMediapackageId");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("type", type);
      q.setParameter("mediapackageId", mediapackageId);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      @SuppressWarnings("unchecked")
      Collection<Annotation> annotations = q.getResultList();

      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  public AnnotationList getAnnotationsByMediapackageId(String mediapackageId, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    db.exec(em -> {
      result.setTotal(getTotalByMediapackageIDQuery(mediapackageId).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      Query q = em.createNamedQuery("findAnnotationsByMediapackageId");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("mediapackageId", mediapackageId);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      @SuppressWarnings("unchecked")
      Collection<Annotation> annotations = q.getResultList();

      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByTypeAndDay(String type, String day, int offset, int limit) {
    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    AnnotationListImpl result = new AnnotationListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type, calBegin, calEnd).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      Query q = em.createNamedQuery("findAnnotationsByTypeAndIntervall");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("type", type);
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByDay(String day, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

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

      Query q = em.createNamedQuery("findAnnotationsByIntervall");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
      q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByType(String type, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    db.exec(em -> {
      result.setTotal(getTotalQuery(type).apply(em));
      result.setOffset(offset);
      result.setLimit(limit);

      Query q = em.createNamedQuery("findAnnotationsByType");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("type", type);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
    });

    return result;
  }

  private Function<EntityManager, Integer> getTotalQuery() {
    return namedQuery.find(
        "findTotal",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername())
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalQuery(String type) {
    return namedQuery.find(
        "findTotalByType",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername()),
        Pair.of("type", type)
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalQuery(String type, String mediapackageId) {
    return namedQuery.find(
        "findTotalByTypeAndMediapackageId",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername()),
        Pair.of("type", type),
        Pair.of("mediapackageId", mediapackageId)
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalByMediapackageIDQuery(String mediapackageId) {
    return namedQuery.find(
        "findTotalByMediapackageId",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername()),
        Pair.of("mediapackageId", mediapackageId)
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalQuery(String type, Calendar calBegin, Calendar calEnd) {
    return namedQuery.find(
        "findTotalByTypeAndIntervall",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername()),
        Pair.of("type", type),
        Pair.of("begin", calBegin),
        Pair.of("end", calEnd)
    ).andThen(Long::intValue);
  }

  private Function<EntityManager, Integer> getTotalQuery(Calendar calBegin, Calendar calEnd) {
    return namedQuery.find(
        "findTotalByIntervall",
        Long.class,
        Pair.of("userId", securityService.getUser().getUsername()),
        Pair.of("begin", calBegin),
        Pair.of("end", calEnd)
    ).andThen(Long::intValue);
  }
}
