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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
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
  public WorkflowInstance getWorkflow(long workflowId) throws NotFoundException, WorkflowServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      WorkflowInstance entity = getWorkflowInstance(workflowId, em);
      if (entity == null) {
        throw new NotFoundException("No workflow with id=" + workflowId + " exists");
      }
//      // Ensure this user is allowed to read this series
//      String accessControlXml = entity.getAccessControl();
//      if (accessControlXml != null) {
//        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
//        User currentUser = securityService.getUser();
//        Organization currentOrg = securityService.getOrganization();
//        // There are several reasons a user may need to load a series: to read content, to edit it, or add content
//        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.READ.toString())
//                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg,
//                Permissions.Action.CONTRIBUTE.toString())
//                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
//          throw new UnauthorizedException(currentUser + " is not authorized to see series " + seriesId);
//        }
//      }
      return entity;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve workflow with ID '{}'", workflowId, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new WorkflowServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * Gets a series by its ID, using the current organizational context.
   *
   * @param id
   *          the series identifier
   * @param em
   *          an open entity manager
   * @return the series entity, or null if not found or if the series is deleted.
   */
  protected WorkflowInstance getWorkflowInstance(long id, EntityManager em) {
    String orgId = securityService.getOrganization().getId();
    Query q = em.createNamedQuery("Workflow.workflowById").setParameter("workflowId", id).setParameter("organizationId", orgId);
    try {
      return (WorkflowInstance) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public boolean mediaPackageHasActiveWorkflows(String mediaPackageId)  {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      Query query = em.createNamedQuery("Workflow.countActiveByMediaPackage");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);
      query.setParameter("stateInstantiated", WorkflowInstance.WorkflowState.INSTANTIATED);
      query.setParameter("statePaused", WorkflowInstance.WorkflowState.PAUSED);
      query.setParameter("stateRunning", WorkflowInstance.WorkflowState.RUNNING);
      return ((Number) query.getSingleResult()).longValue() > 0;
    } catch (Exception e) {
      logger.error("DB: ", e);
      return false;
    } finally {
      if (em != null)
        em.close();
    }
  }

  public void removeFromDatabase(WorkflowInstance instance) {

    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      em.remove(instance);
      tx.commit();
      logger.debug("Workflow with id {} was deleted.", instance.getId());
    } finally {
      if (em != null)
        em.close();
    }
  }

  public List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId) {

    //TODO check authorization
    //either user is local or global admin
    //or user has read right on mp
    //get mp from assetmanager, current workflow (or scheduler?) to check acl

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.byMediaPackage");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);

      List<WorkflowInstance> workflowInstances = query.getResultList();
//      List<WorkflowInstance> workflowInstances = getWorkflowInstancesForQuery(query);
      return workflowInstances;

    } finally {
      if (em != null)
        em.close();
    }
  }

  public List<WorkflowInstance> getRunningWorkflowInstancesByMediaPackage(String mediaPackageId) {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.byMediaPackageAndOneOfThreeStates");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);
      query.setParameter("stateOne", WorkflowInstance.WorkflowState.RUNNING);
      query.setParameter("stateTwo", WorkflowInstance.WorkflowState.PAUSED);
      query.setParameter("stateThree", WorkflowInstance.WorkflowState.FAILING);

      return query.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  public void updateInDatabase(WorkflowInstance instance) {

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
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

}
