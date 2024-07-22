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
package org.opencastproject.lifecyclemanagement.impl.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.lifecyclemanagement.api.LifeCycleDatabaseException;
import org.opencastproject.lifecyclemanagement.api.LifeCycleDatabaseService;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyImpl;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Component(
    immediate = true,
    service = { LifeCycleDatabaseService.class },
    property = {
      "service.description=LifeCycle Database Service"
    })
public class LifeCycleDatabaseServiceImpl implements LifeCycleDatabaseService {
  /**
   * JPA persistence unit name
   */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.lifecyclemanagement";
  private static final Logger logger = LoggerFactory.getLogger(LifeCycleDatabaseServiceImpl.class);
  /**
   * Factory used to create {@link EntityManager}s for transactions
   */
  private EntityManagerFactory emf;

  private DBSessionFactory dbSessionFactory;
  private DBSession db;

  /**
   * The security service
   */
  protected SecurityService securityService;

  /**
   * OSGi DI
   */
  @Reference(target = "(osgi.unit.name=org.opencastproject.lifecyclemanagement.impl.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for lifecycle management");
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#getLifeCyclePolicy(String)
   */
  @Override
  public LifeCyclePolicy getLifeCyclePolicy(String id) throws NotFoundException, LifeCycleDatabaseException {
    return getLifeCyclePolicy(id, securityService.getOrganization().getId());
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#getLifeCyclePolicy(String, String)
   */
  @Override
  public LifeCyclePolicy getLifeCyclePolicy(String id, String orgId)
          throws NotFoundException, LifeCycleDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<LifeCyclePolicy> lifeCyclePolicy = getLifeCyclePolicyById(id, orgId).apply(em);
        // TODO: Should we just return the optional instead?
        if (lifeCyclePolicy.isEmpty()) {
          throw new NotFoundException("No lifecycle policy with id=" + id + " exists");
        }
        return lifeCyclePolicy.get();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve lifecycle policy with ID '{}'", id, e);
      throw new LifeCycleDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see LifeCycleDatabaseService#getLifeCyclePolicies(int, int, SortCriterion)
   */
  @Override
  public List<LifeCyclePolicy> getLifeCyclePolicies(int limit, int offset, SortCriterion sortCriterion)
          throws LifeCycleDatabaseException {

    try {
      return db.exec(em -> {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<LifeCyclePolicyImpl> criteriaQuery = criteriaBuilder.createQuery(LifeCyclePolicyImpl.class);
        Root<LifeCyclePolicyImpl> from = criteriaQuery.from(LifeCyclePolicyImpl.class);
        CriteriaQuery<LifeCyclePolicyImpl> select = criteriaQuery.select(from);
//            .where(criteriaBuilder.isNull(from.get("deletionDate")));

        if (sortCriterion.getOrder().equals(SortCriterion.Order.Ascending)) {
          criteriaQuery.orderBy(criteriaBuilder.asc(from.get(sortCriterion.getFieldName())));
        } else if (sortCriterion.getOrder().equals(SortCriterion.Order.Descending)) {
          criteriaQuery.orderBy(criteriaBuilder.desc(from.get(sortCriterion.getFieldName())));
        }

        TypedQuery<LifeCyclePolicyImpl> allQuery = em.createQuery(select);

        allQuery.setMaxResults(limit);
        allQuery.setFirstResult(offset);

        return (List<LifeCyclePolicy>)(List<?>)allQuery.getResultList();
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Error fetching policies from database", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#getLifeCyclePolicy(String, String)
   */
  @Override
  public List<LifeCyclePolicy> getActiveLifeCyclePolicies(String orgId)
          throws LifeCycleDatabaseException {
    try {
      return db.execTxChecked(em -> {
        var lifeCyclePolicies = namedQuery.findAll(
            "LifeCyclePolicy.findActive",
            LifeCyclePolicy.class,
            Pair.of("organizationId", orgId)
        ).apply(em);
        return lifeCyclePolicies;
      });
    } catch (Exception e) {
      logger.error("Could not find active lifecycle policies", e);
      throw new LifeCycleDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#updateLifeCyclePolicy(LifeCyclePolicy, String)
   */
  @Override
  public LifeCyclePolicy createLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException {
    try {
      if (policy.getId() != null) {
        throw new LifeCycleDatabaseException("Id must not be set when creating new policy");
      }
      return db.execTx(em -> {
        policy.setOrganization(securityService.getOrganization().getId());
        em.persist(policy);
        return policy;
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not create policy with ID '" + policy.getId() + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#updateLifeCyclePolicy(LifeCyclePolicy, String)
   */
  @Override
  public boolean updateLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<LifeCyclePolicy> fromDb = getLifeCyclePolicyById(policy.getId(), orgId).apply(em);
        if (fromDb.isEmpty()) {
          return false;
        } else {
          em.merge(policy);
          return true;
        }
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not update policy with ID '" + policy.getId() + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#deleteLifeCyclePolicy(LifeCyclePolicy, String)
   */
  @Override
  public boolean deleteLifeCyclePolicy(LifeCyclePolicy policy, String orgId) throws LifeCycleDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<LifeCyclePolicy> fromDb = getLifeCyclePolicyById(policy.getId(), orgId).apply(em);
        if (fromDb.isPresent()) {
          em.remove(fromDb.get());
          logger.debug("LifeCyclePolicy with id {} was deleted.", fromDb.get().getId());

          return true;
        }
        return false;
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not delete policy with ID '" + policy.getId() + "'", e);
    }
  }

  @Override
  public LifeCycleTask getLifeCycleTask(String id) throws NotFoundException, LifeCycleDatabaseException {
    try {
      return db.execTxChecked(em -> {
        String orgId = securityService.getOrganization().getId();
        Optional<LifeCycleTask> lifeCycleTask = getLifeCycleTaskById(id, orgId).apply(em);
        if (lifeCycleTask.isEmpty()) {
          throw new NotFoundException("No lifecycle task with id=" + id + " exists");
        }
        return lifeCycleTask.get();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve lifecycle task with ID '{}'", id, e);
      throw new LifeCycleDatabaseException(e);
    }
  }

  @Override
  public LifeCycleTask getLifeCycleTaskByTargetId(String targetId)
          throws NotFoundException, LifeCycleDatabaseException {
    try {
      return db.execTxChecked(em -> {
        String orgId = securityService.getOrganization().getId();
        Optional<LifeCycleTask> lifeCycleTask = namedQuery
            .findOpt(
                "LifeCycleTask.findByTargetId",
                LifeCycleTask.class,
                Pair.of("targetId", targetId),
                Pair.of("organizationId", orgId)
            ).apply(em);
        if (lifeCycleTask.isEmpty()) {
          throw new NotFoundException("No lifecycle task with targetId=" + targetId + " exists");
        }
        return lifeCycleTask.get();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve lifecycle task with targetId ID '{}'", targetId, e);
      throw new LifeCycleDatabaseException(e);
    }
  }

  @Override
  public List<LifeCycleTask> getLifeCycleTasksWithStatus(Status status, String orgId)
          throws LifeCycleDatabaseException {
    try {
      return db.execTxChecked(em -> {
        var lifeCycleTasks = namedQuery.findAll(
            "LifeCycleTask.withStatus",
            LifeCycleTask.class,
            Pair.of("status", status),
            Pair.of("organizationId", orgId)
        ).apply(em);
        return lifeCycleTasks;
      });
    } catch (Exception e) {
      logger.error("Could not find lifecycle tasks with status: ", status, e);
      throw new LifeCycleDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see LifeCycleDatabaseService#updateLifeCyclePolicy(LifeCyclePolicy, String)
   */
  @Override
  public LifeCycleTask createLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException {
    try {
      if (task.getId() != null) {
        throw new LifeCycleDatabaseException("Id must not be set when creating new task");
      }
      return db.execTx(em -> {
        task.setOrganization(securityService.getOrganization().getId());
        em.persist(task);
        return task;
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not create task with ID '" + task.getId() + "'", e);
    }
  }

  @Override
  public boolean updateLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<LifeCycleTask> fromDb = getLifeCycleTaskById(task.getId(), orgId).apply(em);
        if (fromDb.isEmpty()) {
          return false;
        } else {
          em.merge(task);
          return true;
        }
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not update task with ID '" + task.getId() + "'", e);
    }
  }

  @Override
  public boolean deleteLifeCycleTask(LifeCycleTask task, String orgId) throws LifeCycleDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<LifeCycleTask> fromDb = getLifeCycleTaskById(task.getId(), orgId).apply(em);
        if (fromDb.isPresent()) {
          em.remove(fromDb.get());
          logger.debug("LifeCycleTask with id {} was deleted.", fromDb.get().getId());
          return true;
        }
        return false;
      });
    } catch (Exception e) {
      throw new LifeCycleDatabaseException("Could not delete task with ID '" + task.getId() + "'", e);
    }
  }

  /**
   * Gets a potentially deleted series by its ID, using the current organizational context.
   *
   * @param id    the policy identifier
   * @param orgId the organisation identifier
   * @return the policy in an optional
   */
  protected Function<EntityManager, Optional<LifeCyclePolicy>> getLifeCyclePolicyById(String id, String orgId) {
    return namedQuery.findOpt("LifeCyclePolicy.findById", LifeCyclePolicy.class, Pair.of("id", id),
        Pair.of("organizationId", orgId));
  }

  protected Function<EntityManager, Optional<LifeCycleTask>> getLifeCycleTaskById(String id, String orgId) {
    return namedQuery.findOpt("LifeCycleTask.findById", LifeCycleTask.class, Pair.of("id", id),
        Pair.of("organizationId", orgId));
  }
}
