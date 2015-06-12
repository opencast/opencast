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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;

import org.opencastproject.authorization.xacml.manager.api.AclServiceNoReferenceException;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDb;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDbDuplicatedException;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDbException;
import org.opencastproject.authorization.xacml.manager.impl.TransitionResultImpl;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.spi.PersistenceProvider;

public final class OsgiJpaAclTransitionDb implements AclTransitionDb {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OsgiJpaAclTransitionDb.class);

  /** Persistence provider set by OSGi */
  private PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  private Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

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
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for ACL manager");
    final String emName = "org.opencastproject.authorization.xacml.manager";
    emf = persistenceProvider.createEntityManagerFactory(emName, persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  @Override
  public EpisodeACLTransition storeEpisodeAclTransition(Organization org, String episodeId, Date applicationDate,
          Option<Long> managedAclId, Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EpisodeAclTransitionEntity entity = new EpisodeAclTransitionEntity().update(episodeId, org.getId(),
              applicationDate, getManagedAcl(em, managedAclId, org), workflow);
      em.persist(entity);
      tx.commit();
      return entity;
    } catch (AclTransitionDbException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive())
        tx.rollback();
      boolean isConstraintViolation = Util.isConstraintViolationException(e);
      if (isConstraintViolation)
        throw new AclTransitionDbDuplicatedException();
      else {
        logger.error("Could not store the scheduled episode ACL: {}", e.getMessage());
        throw new AclTransitionDbException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public SeriesACLTransition storeSeriesAclTransition(Organization org, String seriesId, Date applicationDate,
          long managedAclId, boolean override, Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesAclTransitionEntity entity = new SeriesAclTransitionEntity().update(seriesId, org.getId(), applicationDate,
              getManagedAcl(em, some(managedAclId), org).get(), workflow, override);
      em.persist(entity);
      tx.commit();
      return entity;
    } catch (AclTransitionDbException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive())
        tx.rollback();
      boolean isContraintViolation = Util.isConstraintViolationException(e);
      if (isContraintViolation)
        throw new AclTransitionDbDuplicatedException();
      else {
        logger.error("Could not store the scheduled series ACL: {}", e.getMessage());
        throw new AclTransitionDbException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public EpisodeACLTransition updateEpisodeAclTransition(Organization org, long transitionId, Date applicationDate,
          Option<Long> managedAclId, Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EpisodeAclTransitionEntity entity = getEpisodeEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException("Episode transition " + transitionId + " not found!");
      entity.update(entity.getEpisodeId(), org.getId(), applicationDate, getManagedAcl(em, managedAclId, org), workflow);
      em.merge(entity);
      tx.commit();
      return entity;
    } catch (NotFoundException e) {
      throw e;
    } catch (AclTransitionDbException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive())
        tx.rollback();
      boolean isContraintViolation = Util.isConstraintViolationException(e);
      if (isContraintViolation)
        throw new AclTransitionDbDuplicatedException();
      else {
        logger.error("Could not update the scheduled episode ACL: {}", e.getMessage());
        throw new AclTransitionDbException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public SeriesACLTransition updateSeriesAclTransition(Organization org, long transitionId, Date applicationDate,
          long managedAclId, boolean override, Option<ConfiguredWorkflowRef> workflow) throws AclTransitionDbException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesAclTransitionEntity entity = getSeriesEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException("Series transition " + transitionId + " not found!");
      entity.update(entity.getSeriesId(), org.getId(), applicationDate, getManagedAcl(em, some(managedAclId), org)
              .get(), workflow, override);
      em.merge(entity);
      tx.commit();
      return entity;
    } catch (NotFoundException e) {
      throw e;
    } catch (AclTransitionDbException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive())
        tx.rollback();
      boolean isContraintViolation = Util.isConstraintViolationException(e);
      if (isContraintViolation)
        throw new AclTransitionDbDuplicatedException();
      else {
        logger.error("Could not update the scheduled series ACL: {}", e.getMessage());
        throw new AclTransitionDbException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteEpisodeAclTransition(Organization org, long transitionId) throws NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EpisodeAclTransitionEntity entity = getEpisodeEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException();
      em.remove(entity);
      tx.commit();
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteSeriesAclTransition(Organization org, long transitionId) throws NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesAclTransitionEntity entity = getSeriesEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException();
      em.remove(entity);
      tx.commit();
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<EpisodeACLTransition> getEpisodeAclTransitions(Organization org, String episodeId)
          throws AclTransitionDbException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return Misc.<EpisodeACLTransition> widen(getEpisodeEntities(episodeId, org.getId(), em));
    } catch (Exception e) {
      logger.warn("Error parsing episode ACL: {}", e);
      throw new AclTransitionDbException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public List<SeriesACLTransition> getSeriesAclTransitions(Organization org, String seriesId)
          throws AclTransitionDbException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return Misc.<SeriesACLTransition> widen(getSeriesEntities(seriesId, org.getId(), em));
    } catch (Exception e) {
      logger.warn("Error parsing episode ACL: {}", e);
      throw new AclTransitionDbException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public TransitionResult getByQuery(Organization org, TransitionQuery query) throws AclTransitionDbException {
    for (AclScope scope : query.getScope()) {
      switch (scope) {
        case Episode:
          return getEpisodeResult(query, org.getId());
        case Series:
          return getSeriesResult(query, org.getId());
        default:
          unexhaustiveMatch();
      }
    }
    // none
    return new TransitionResultImpl(getEpisodeResult(query, org.getId()).getEpisodeTransistions(), getSeriesResult(
            query, org.getId()).getSeriesTransistions());
  }

  @Override
  public SeriesACLTransition markSeriesTransitionAsCompleted(Organization org, long transitionId)
          throws AclTransitionDbException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesAclTransitionEntity entity = getSeriesEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException("Series transition " + transitionId + " not found!");
      entity.setDone(true);
      em.merge(entity);
      tx.commit();
      return entity;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update the scheduled series ACL: {}", e.getMessage());
      if (tx != null && tx.isActive())
        tx.rollback();
      throw new AclTransitionDbException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public EpisodeACLTransition markEpisodeTransitionAsCompleted(Organization org, long transitionId)
          throws AclTransitionDbException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      EpisodeAclTransitionEntity entity = getEpisodeEntity(transitionId, org.getId(), em);
      if (entity == null)
        throw new NotFoundException("Episode transition " + transitionId + " not found!");
      entity.setDone(true);
      em.merge(entity);
      tx.commit();
      return entity;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update the scheduled episode ACL: {}", e.getMessage());
      if (tx != null && tx.isActive())
        tx.rollback();
      throw new AclTransitionDbException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Search episode transitions with the given transition query and return it in a transition result
   *
   * @param query
   *          the transition query
   * @return the episode transition result
   * @throws AclTransitionDbException
   *           if exception occurs during reading/storing from the persistence layer
   */
  private TransitionResult getEpisodeResult(TransitionQuery query, String orgId) throws AclTransitionDbException {
    for (long transitionId : query.getTransitionId()) {
      EntityManager em = null;
      try {
        em = emf.createEntityManager();
        return new TransitionResultImpl(Misc.<EpisodeACLTransition> widen(option(
                getEpisodeEntity(transitionId, orgId, em)).list()), Collections.<SeriesACLTransition> nil());
      } catch (Exception e) {
        logger.warn("Error parsing episode ACL: {}", e);
        throw new AclTransitionDbException(e);
      } finally {
        if (em != null)
          em.close();
      }
    }

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<EpisodeAclTransitionEntity> q = cb.createQuery(EpisodeAclTransitionEntity.class);
      Root<EpisodeAclTransitionEntity> c = q.from(EpisodeAclTransitionEntity.class);
      q.select(c);
      // create predicates joined in an "and" expression
      final List<Predicate> predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(c.get("organizationId"), orgId));
      for (String p : query.getId())
        predicates.add(cb.equal(c.get("episodeId"), p));
      for (Boolean p : query.getDone()) {
        if (p)
          predicates.add(cb.isTrue(c.get("done").as(Boolean.class)));
        else
          predicates.add(cb.isFalse(c.get("done").as(Boolean.class)));
      }
      for (Long p : query.getAclId())
        predicates.add(cb.equal(c.get("managedAcl").get("id").as(Long.class), p));
      for (Date p : query.getAfter())
        predicates.add(cb.greaterThanOrEqualTo(c.get("applicationDate").as(Date.class), p));
      for (Date p : query.getBefore())
        predicates.add(cb.lessThanOrEqualTo(c.get("applicationDate").as(Date.class), p));
      q.where(cb.and(toArray(predicates)));

      q.orderBy(cb.asc(c.get("applicationDate")));

      TypedQuery<EpisodeAclTransitionEntity> typedQuery = em.createQuery(q);
      return new TransitionResultImpl(Misc.<EpisodeACLTransition> widen(typedQuery.getResultList()),
              Collections.<SeriesACLTransition> nil());
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Search series transitions with the given transition query and return it in a transition result
   *
   * @param query
   *          the transition query
   * @return the series transition result
   * @throws AclTransitionDbException
   *           if exception occurs during reading/storing from the persistence layer
   */
  private TransitionResult getSeriesResult(TransitionQuery query, String orgId) throws AclTransitionDbException {
    for (long transitionId : query.getTransitionId()) {
      EntityManager em = null;
      try {
        em = emf.createEntityManager();
        return new TransitionResultImpl(Collections.<EpisodeACLTransition> nil(),
                Misc.<SeriesACLTransition> widen(option(getSeriesEntity(transitionId, orgId, em)).list()));
      } catch (Exception e) {
        logger.warn("Error parsing episode ACL: {}", e);
        throw new AclTransitionDbException(e);
      } finally {
        if (em != null)
          em.close();
      }
    }

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<SeriesAclTransitionEntity> q = cb.createQuery(SeriesAclTransitionEntity.class);
      Root<SeriesAclTransitionEntity> c = q.from(SeriesAclTransitionEntity.class);
      q.select(c);
      // create predicates joined in an "and" expression
      final List<Predicate> predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(c.get("organizationId"), orgId));
      for (String p : query.getId())
        predicates.add(cb.equal(c.get("seriesId"), p));
      for (Boolean p : query.getDone()) {
        if (p)
          predicates.add(cb.isTrue(c.get("done").as(Boolean.class)));
        else
          predicates.add(cb.isFalse(c.get("done").as(Boolean.class)));
      }
      for (Long p : query.getAclId())
        predicates.add(cb.equal(c.get("managedAcl").get("id").as(Long.class), p));
      for (Date p : query.getAfter())
        predicates.add(cb.greaterThanOrEqualTo(c.get("applicationDate").as(Date.class), p));
      for (Date p : query.getBefore())
        predicates.add(cb.lessThanOrEqualTo(c.get("applicationDate").as(Date.class), p));
      q.where(cb.and(toArray(predicates)));

      q.orderBy(cb.asc(c.get("applicationDate")));

      TypedQuery<SeriesAclTransitionEntity> typedQuery = em.createQuery(q);
      return new TransitionResultImpl(Collections.<EpisodeACLTransition> nil(),
              Misc.<SeriesACLTransition> widen(typedQuery.getResultList()));
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets all series ACL entities by the series id as a list, using the current organizational context.
   *
   * @param seriesId
   *          the series id
   * @param em
   *          an open entity manager
   * @return the series ACL entity list
   */
  @SuppressWarnings("unchecked")
  private List<SeriesAclTransitionEntity> getSeriesEntities(String seriesId, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("SeriesAcl.findBySeriesId");
    q.setParameter("id", seriesId);
    q.setParameter("organizationId", orgId);
    return q.getResultList();
  }

  /**
   * Gets all episode ACL entities by the episode id as a list, using the current organizational context.
   *
   * @param episodeId
   *          the episode id
   * @param em
   *          an open entity manager
   * @return the episode ACL entity list
   */
  @SuppressWarnings("unchecked")
  private List<EpisodeAclTransitionEntity> getEpisodeEntities(String episodeId, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("EpisodeAcl.findByEpisodeId");
    q.setParameter("id", episodeId);
    q.setParameter("organizationId", orgId);
    return q.getResultList();
  }

  /**
   * Gets a series ACL entity by its ID, using the current organizational context.
   *
   * @param id
   *          the transition identifier
   * @param em
   *          an open entity manager
   * @return the series ACL entity, or null if not found
   */
  private SeriesAclTransitionEntity getSeriesEntity(long id, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("SeriesAcl.findByTransitionId");
    q.setParameter("id", id);
    q.setParameter("organizationId", orgId);
    try {
      return (SeriesAclTransitionEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Gets a episode ACL entity by its ID, using the current organizational context.
   *
   * @param id
   *          the transition identifier
   * @param em
   *          an open entity manager
   * @return the episode ACL entity, or null if not found
   */
  private EpisodeAclTransitionEntity getEpisodeEntity(long id, String orgId, EntityManager em) {
    Query q = em.createNamedQuery("EpisodeAcl.findByTransitionId");
    q.setParameter("id", id);
    q.setParameter("organizationId", orgId);
    try {
      return (EpisodeAclTransitionEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  private static Predicate[] toArray(List<Predicate> predicates) {
    return predicates.toArray(new Predicate[predicates.size()]);
  }

  /** Return none if <code>aclId</code> is none, throw NotFoundException if acl cannot be found. */
  private static Option<ManagedAclEntity> getManagedAcl(EntityManager em, Option<Long> aclId, Organization org)
          throws AclServiceNoReferenceException {
    for (Long id : aclId) {
      for (ManagedAclEntity e : ManagedAclEntity.findByIdAndOrg(org.getId(), id).apply(em)) {
        return some(e);
      }
      throw new AclServiceNoReferenceException("Cannot find ACL " + aclId);
    }
    return none();
  }
}
