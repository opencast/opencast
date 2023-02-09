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

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
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
import javax.persistence.NoResultException;

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

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.workflow.api)")
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
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflow(long)
   */
  @Override
  public WorkflowInstance getWorkflow(long workflowId) throws NotFoundException, WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.find(
          "Workflow.workflowById",
          WorkflowInstance.class,
          Pair.of("workflowId", workflowId),
          Pair.of("organizationId", securityService.getOrganization().getId())
      ));
    } catch (NoResultException e) {
      throw new NotFoundException("No workflow with id=" + workflowId + " exists");
    } catch (Exception e) {
      logger.error("Could not retrieve workflow with ID '{}'", workflowId, e);
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstances(int limit, int offset)
   */
  public List<WorkflowInstance> getWorkflowInstances(int limit, int offset) throws WorkflowDatabaseException {
    try {
      return db.exec(em -> {
        var query = em
            .createNamedQuery("Workflow.findAll", WorkflowInstance.class)
            .setParameter("organizationId", securityService.getOrganization().getId())
            .setMaxResults(limit)
            .setFirstResult(offset);

        logger.debug("Requesting workflows using query: {}", query);
        return query.getResultList();
      });
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Error fetching workflows from database", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstancesForCleanup(WorkflowInstance.WorkflowState state, Date dateCreated)
   */
  public List<WorkflowInstance> getWorkflowInstancesForCleanup(WorkflowInstance.WorkflowState state, Date dateCreated)
          throws WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "Workflow.toCleanup",
          WorkflowInstance.class,
          Pair.of("organizationId", securityService.getOrganization().getId()),
          Pair.of("state", state),
          Pair.of("dateCreated", dateCreated)
      ));
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#countWorkflows(WorkflowInstance.WorkflowState state)
   */
  public long countWorkflows(WorkflowInstance.WorkflowState state) throws WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.find(
          "Workflow.getCount",
          Long.class,
          Pair.of("organizationId", securityService.getOrganization().getId()),
          Pair.of("state", state)
      ));
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Could not find number of workflows.", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowIndexData(int limit, int offset)
   */
  public List<WorkflowIndexData> getWorkflowIndexData(int limit, int offset) throws WorkflowDatabaseException {
    try {
      return db.exec(em -> {
        return em
            .createNamedQuery("WorkflowIndexData.getAll", WorkflowIndexData.class)
            .setMaxResults(limit)
            .setFirstResult(offset)
            .getResultList();
      });
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#countMediaPackages()
   */
  public int countMediaPackages() throws WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.find("Workflow.countLatest", Number.class)).intValue();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getWorkflowInstancesByMediaPackage(String mediaPackageId)
   */
  public List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId)
          throws WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "Workflow.byMediaPackage",
          WorkflowInstance.class,
          Pair.of("organizationId", securityService.getOrganization().getId()),
          Pair.of("mediaPackageId", mediaPackageId)
      ));
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Failed to get workflows from database", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#getRunningWorkflowInstancesByMediaPackage(String mediaPackageId)
   */
  public List<WorkflowInstance> getRunningWorkflowInstancesByMediaPackage(String mediaPackageId)
          throws WorkflowDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "Workflow.byMediaPackageAndActive",
          WorkflowInstance.class,
          Pair.of("organizationId", securityService.getOrganization().getId()),
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("stateInstantiated", WorkflowInstance.WorkflowState.INSTANTIATED),
          Pair.of("stateRunning", WorkflowInstance.WorkflowState.RUNNING),
          Pair.of("statePaused", WorkflowInstance.WorkflowState.PAUSED),
          Pair.of("stateFailing", WorkflowInstance.WorkflowState.FAILING)
      ));
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#mediaPackageHasActiveWorkflows(String mediaPackageId)
   */
  public boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException {
    try {
      long count = db.exec(namedQuery.find(
          "Workflow.countActiveByMediaPackage",
          Long.class,
          Pair.of("organizationId", securityService.getOrganization().getId()),
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("stateInstantiated", WorkflowInstance.WorkflowState.INSTANTIATED),
          Pair.of("stateRunning", WorkflowInstance.WorkflowState.RUNNING),
          Pair.of("statePaused", WorkflowInstance.WorkflowState.PAUSED),
          Pair.of("stateFailing", WorkflowInstance.WorkflowState.FAILING)
      ));
      return count > 0;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#updateInDatabase(WorkflowInstance instance)
   */
  public void updateInDatabase(WorkflowInstance instance) throws WorkflowDatabaseException {
    try {
      db.execTx(em -> {
        WorkflowInstance fromDb = em.find(WorkflowInstance.class, instance.getId());
        if (fromDb == null) {
          em.persist(instance);
        } else {
          em.merge(instance);
        }
      });
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Could not update workflow with ID '" + instance.getId() + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowServiceDatabase#removeFromDatabase(WorkflowInstance instance)
   */
  public void removeFromDatabase(WorkflowInstance instance) throws WorkflowDatabaseException {
    try {
      db.execTx(em -> {
        WorkflowInstance fromDb = em.find(WorkflowInstance.class, instance.getId());
        if (fromDb != null) {
          fromDb = em.merge(instance);
          em.remove(fromDb);
        }
      });
      logger.debug("Workflow with id {} was deleted.", instance.getId());
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Could not delete workflow with ID '" + instance.getId() + "'", e);
    }
  }
}
