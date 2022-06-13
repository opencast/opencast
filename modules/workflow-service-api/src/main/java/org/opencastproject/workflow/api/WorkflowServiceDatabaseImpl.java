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

package org.opencastproject.workflow.api;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Implements {@link WorkflowServiceDatabase}. Defines permanent storage for workflow.
 */
@Component(
        property = {
                "service.description=Workflow Service Database"
        },
        immediate = true,
        service = { WorkflowServiceDatabase.class }
)
public class WorkflowServiceDatabaseImpl implements WorkflowServiceDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceDatabaseImpl.class);

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.workflow.api";

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The security service */
  protected SecurityService securityService;


  /** OSGi DI */
  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.workflow.api)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for workflow");
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflow(long)
   */
  @Override
  public WorkflowInstance getWorkflow(long workflowId) throws NotFoundException, WorkflowDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("Workflow.workflowById");
      q.setParameter("workflowId", workflowId);
      q.setParameter("organizationId", orgId);

      return (WorkflowInstance) q.getSingleResult();
    } catch (NoResultException e) {
      throw new NotFoundException("No workflow with id=" + workflowId + " exists");
    } catch (Exception e) {
      logger.error("Could not retrieve workflow with ID '{}'", workflowId, e);
      throw new WorkflowDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstances(int limit, int offset)
   */
  public List<WorkflowInstance> getWorkflowInstances(int limit, int offset) throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      var query = em.createNamedQuery("Workflow.findAll", WorkflowInstance.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setMaxResults(limit);
      query.setFirstResult(offset);
      logger.debug("Requesting workflows using query: {}", query);
      return query.getResultList();
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Error fetching workflows from database", e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstancesForCleanup(WorkflowInstance.WorkflowState state, Date dateCreated)
   */
  public List<WorkflowInstance> getWorkflowInstancesForCleanup(WorkflowInstance.WorkflowState state, Date dateCreated)
          throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      var query = em.createNamedQuery("Workflow.toCleanup", WorkflowInstance.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("state", state);
      query.setParameter("dateCreated", dateCreated);

      return query.getResultList();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#countWorkflows(WorkflowInstance.WorkflowState state)
   */
  public long countWorkflows(WorkflowInstance.WorkflowState state) throws WorkflowDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      var query = em.createNamedQuery("Workflow.getCount", Long.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("state", state);

      return query.getSingleResult();
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Could not find number of workflows.", e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowIndexData(int limit, int offset)
   */
  public List<WorkflowIndexData> getWorkflowIndexData(int limit, int offset) throws WorkflowDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      var nativeQuery = em.createNamedQuery("WorkflowIndexData.getAll", WorkflowIndexData.class);
      nativeQuery.setMaxResults(limit);
      nativeQuery.setFirstResult(offset);
      return nativeQuery.getResultList();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

    /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#countMediaPackages()
   */
  public int countMediaPackages() throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      var query = em.createNamedQuery("Workflow.countLatest", Long.class);
      logger.debug("Counting latest workflows using query: {}", query);
      final Number countResult = query.getSingleResult();
      return countResult.intValue();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstancesByMediaPackage(String mediaPackageId)
   */
  public List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId)
          throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      var query = em.createNamedQuery("Workflow.byMediaPackage", WorkflowInstance.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);

      return query.getResultList();
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Failed to get workflows from database", e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getRunningWorkflowInstancesByMediaPackage(String mediaPackageId)
   */
  public List<WorkflowInstance> getRunningWorkflowInstancesByMediaPackage(String mediaPackageId)
          throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      var query = em.createNamedQuery("Workflow.byMediaPackageAndActive", WorkflowInstance.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);
      query.setParameter("stateInstantiated", WorkflowInstance.WorkflowState.INSTANTIATED);
      query.setParameter("stateRunning", WorkflowInstance.WorkflowState.RUNNING);
      query.setParameter("statePaused", WorkflowInstance.WorkflowState.PAUSED);
      query.setParameter("stateFailing", WorkflowInstance.WorkflowState.FAILING);

      return query.getResultList();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#mediaPackageHasActiveWorkflows(String mediaPackageId)
   */
  public boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException {

    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      var query = em.createNamedQuery("Workflow.countActiveByMediaPackage", Long.class);

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);
      query.setParameter("stateInstantiated", WorkflowInstance.WorkflowState.INSTANTIATED);
      query.setParameter("stateRunning", WorkflowInstance.WorkflowState.RUNNING);
      query.setParameter("statePaused", WorkflowInstance.WorkflowState.PAUSED);
      query.setParameter("stateFailing", WorkflowInstance.WorkflowState.FAILING);
      return query.getSingleResult() > 0;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#updateInDatabase(WorkflowInstance instance)
   */
  public void updateInDatabase(WorkflowInstance instance) throws WorkflowDatabaseException {

    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      WorkflowInstance fromDb = em.find(WorkflowInstance.class, instance.getId());
      if (fromDb == null) {
        em.persist(instance);
      } else {
        em.merge(instance);
      }
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new WorkflowDatabaseException("Could not update workflow with ID '" + instance.getId() + "'", e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#removeFromDatabase(WorkflowInstance instance)
   */
  public void removeFromDatabase(WorkflowInstance instance) throws WorkflowDatabaseException {

    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      WorkflowInstance fromDb = em.find(WorkflowInstance.class, instance.getId());
      if (fromDb == null) {
        // Already removed
      } else {
        instance = em.merge(instance);
        em.remove(instance);
      }
      tx.commit();
      logger.debug("Workflow with id {} was deleted.", instance.getId());
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new WorkflowDatabaseException("Could not delete workflow with ID '" + instance.getId() + "'", e);
    } finally {
      if (em != null)
        em.close();
    }
  }
}
