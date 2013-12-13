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
package org.opencastproject.annotation.impl;

import org.opencastproject.annotation.api.Annotation;
import org.opencastproject.annotation.api.AnnotationList;
import org.opencastproject.annotation.api.AnnotationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceProvider;

/**
 * JPA-based implementation of the {@link AnnotationService}
 */
public class AnnotationServiceJpaImpl implements AnnotationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AnnotationRestService.class);

  /** The persistence unit properties */
  protected Map<String, Object> persistenceProperties;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * The JPA provider
   */
  protected PersistenceProvider persistenceProvider;

  /** Matterhorn's security service */
  protected SecurityService securityService;

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * Sets the matterhorn security service
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGI declarative services callback for component activation
   * 
   * @param componentContext
   *          the OSGI declarative services component context
   */
  protected void activate(ComponentContext componentContext) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.annotation", persistenceProperties);
  }

  protected void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  private int getTotal() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findTotal");
      q.setParameter("userId", securityService.getUser().getUsername());
      return ((Long) q.getSingleResult()).intValue();
    } finally {
      if (em != null)
        em.close();
    }
  }

  public Annotation addAnnotation(Annotation a) {
    // set the User ID on the annotation
    a.setUserId(securityService.getUser().getUsername());
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.persist(a);
      tx.commit();
      return a;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  public boolean removeAnnotation(Annotation a) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // first merge then remove element
      em.remove(em.merge(a));
      tx.commit();
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      em.close();
    }
  }

  public Annotation changeAnnotation(Annotation a) throws NotFoundException {
    EntityTransaction tx = null;
    EntityManager em = null;
    AnnotationImpl b = null;
    long id = a.getAnnotationId();
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query q = em.createNamedQuery("updateAnnotation");
      q.setParameter("value", a.getValue());
      q.setParameter("annotationId", id);
      int no = q.executeUpdate();
      if (no == 1) {
        b = em.find(AnnotationImpl.class, id);
      }
      tx.commit();
      return b;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      em.close();
    }
  }

  public Annotation getAnnotation(long id) throws NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      AnnotationImpl a = em.find(AnnotationImpl.class, id);
      if (a == null) {
        throw new NotFoundException("Annotation '" + id + "' not found");
      } else {
        return a;
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotations(int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    result.setTotal(getTotal());
    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("findAnnotations");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null)
        em.close();
    }
  }

  public AnnotationList getAnnotationsByTypeAndMediapackageId(String type, String mediapackageId, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      result.setTotal(getTotal(type, mediapackageId, em));
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

      return result;
    } finally {
      if (em != null)
        em.close();
    }
  }

  public AnnotationList getAnnotationsByMediapackageId(String mediapackageId, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      result.setTotal(getTotalByMediapackageID(mediapackageId, em));
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

      return result;
    } finally {
      em.close();
    }
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
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      result.setTotal(getTotal(type, calBegin, calEnd, em));
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
      return result;
    } finally {
      if (em != null)
        em.close();
    }

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

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      result.setTotal(getTotal(calBegin, calEnd, em));
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
      return result;
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByType(String type, int offset, int limit) {
    AnnotationListImpl result = new AnnotationListImpl();

    result.setOffset(offset);
    result.setLimit(limit);

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      result.setTotal(getTotal(type, em));
      Query q = em.createNamedQuery("findAnnotationsByType");
      q.setParameter("userId", securityService.getUser().getUsername());
      q.setParameter("type", type);
      q.setFirstResult(offset);
      q.setMaxResults(limit);
      Collection<Annotation> annotations = q.getResultList();
      for (Annotation a : annotations) {
        result.add(a);
      }
      return result;
    } finally {
      if (em != null)
        em.close();
    }
  }

  private int getTotal(String type, EntityManager em) {
    Query q = em.createNamedQuery("findTotalByType");
    q.setParameter("userId", securityService.getUser().getUsername());
    q.setParameter("type", type);
    return ((Long) q.getSingleResult()).intValue();
  }

  private int getTotal(String type, String mediapackageId, EntityManager em) {
    Query q = em.createNamedQuery("findTotalByTypeAndMediapackageId");
    q.setParameter("userId", securityService.getUser().getUsername());
    q.setParameter("type", type);
    q.setParameter("mediapackageId", mediapackageId);
    return ((Long) q.getSingleResult()).intValue();
  }

  private int getTotalByMediapackageID(String mediapackageId, EntityManager em) {
    Query q = em.createNamedQuery("findTotalByMediapackageId");
    q.setParameter("userId", securityService.getUser().getUsername());
    q.setParameter("mediapackageId", mediapackageId);
    return ((Long) q.getSingleResult()).intValue();
  }

  private int getTotal(String type, Calendar calBegin, Calendar calEnd, EntityManager em) {
    Query q = em.createNamedQuery("findTotalByTypeAndIntervall");
    q.setParameter("userId", securityService.getUser().getUsername());
    q.setParameter("type", type);
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    return ((Long) q.getSingleResult()).intValue();
  }

  private int getTotal(Calendar calBegin, Calendar calEnd, EntityManager em) {
    Query q = em.createNamedQuery("findTotalByIntervall");
    q.setParameter("userId", securityService.getUser().getUsername());
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    return ((Long) q.getSingleResult()).intValue();
  }
}
