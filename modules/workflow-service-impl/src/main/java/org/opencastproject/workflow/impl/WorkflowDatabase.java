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
package org.opencastproject.workflow.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionReport;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationReport;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowStatisticsReport;
import org.opencastproject.workflow.impl.jpa.JpaWorkflow;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

public class WorkflowDatabase {

  /** Logging facility */
  private static final Log logger = new Log(LoggerFactory.getLogger(WorkflowDatabase.class));

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The workflow definition scanner */
  private WorkflowDefinitionScanner workflowDefinitionScanner;

  //TODO javadoc

  public WorkflowDatabase(EntityManagerFactory emf, SecurityService securityService, WorkflowDefinitionScanner
          workflowDefinitionScanner) {
    this.emf = emf;
    this.securityService = securityService;
    this.workflowDefinitionScanner = workflowDefinitionScanner;
  }

  long countWorkflows(WorkflowInstance.WorkflowState workflowState, String operation)
          throws UnauthorizedException {

    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
      throw new UnauthorizedException("Non-Admin has no right to count workflows");
    }

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.count");

      String state = null;
      if (workflowState != null) {
        state = workflowState.toString();
      }
      query.setParameter("state", state);
      query.setParameter("currentOperation", operation);

      // We want all available workflows for this organization
      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);

      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();

    } finally {
      if (em != null)
        em.close();
    }
  }

  boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.countActiveByMediaPackage");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("mediaPackageId", mediaPackageId);

      return ((Number) query.getSingleResult()).longValue() > 0;

    } finally {
      if (em != null)
        em.close();
    }
  }

  WorkflowInstance getWorkflowById(long id) throws WorkflowDatabaseException, NotFoundException,
          UnauthorizedException {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.byId");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("id", id);

      List<JpaWorkflow> results = (List<JpaWorkflow>) query.getResultList();
      if (results.size() > 0)
        return results.get(0).toWorkflow();
      throw new NotFoundException();

    } catch (MediaPackageException | WorkflowParsingException e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId) throws WorkflowDatabaseException {

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

      List<WorkflowInstance> workflowInstances = getWorkflowInstancesForQuery(query);
      return workflowInstances;

    } finally {
      if (em != null)
        em.close();
    }
  }


  List<WorkflowInstance> getWorkflowInstancesForCleanup(WorkflowInstance.WorkflowState state, Date dateCreated)
          throws WorkflowDatabaseException {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Workflow.toCleanup");

      String orgId = securityService.getOrganization().getId();
      query.setParameter("organizationId", orgId);
      query.setParameter("state", state.toString());
      query.setParameter("dateCreated", dateCreated);

      List<WorkflowInstance> workflowInstances = getWorkflowInstancesForQuery(query);
      return workflowInstances;

    } finally {
      if (em != null)
        em.close();
    }
  }



  List<WorkflowInstance> getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {

    //TODO check authorization

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String queryString = "SELECT w FROM Workflow w where w.organizationId = :organizationId";

      //TODO build query from WorkflowQuery dynamically

      Query databaseQuery = em.createQuery(queryString);

      String orgId = securityService.getOrganization().getId();
      databaseQuery.setParameter("organizationId", orgId);

      return getWorkflowInstancesForQuery(databaseQuery);

    } finally {
      if (em != null)
        em.close();
    }
  }

  List<WorkflowInstance> getWorkflowInstancesForAdministrativeRead(WorkflowQuery query) throws WorkflowDatabaseException,
          UnauthorizedException {

    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole()))
      throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String queryString = "SELECT w FROM Workflow w where w.organizationId = :organizationId";

      //TODO build query from WorkflowQuery dynamically

      Query databaseQuery = em.createQuery(queryString);

      String orgId = securityService.getOrganization().getId();
      databaseQuery.setParameter("organizationId", orgId);

      return getWorkflowInstancesForQuery(databaseQuery);

    } finally {
      if (em != null)
        em.close();
    }
  }

  WorkflowStatisticsReport getStatistics() throws UnauthorizedException {
    WorkflowStatisticsReport statsReport = new WorkflowStatisticsReport();

    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
      throw new UnauthorizedException("Non-Admin has no right to get workflow statistics");
    }
    String orgId = securityService.getOrganization().getId();

    EntityManager em = null;
    try {
      em = emf.createEntityManager();

      // count workflows by state
      Query query = em.createNamedQuery("Workflow.countByState");
      query.setParameter("organizationId", orgId);
      List<Object[]> results = query.getResultList();

      for (Object[] result : results) {
        String state = (String) result[0];
        long count = (Long) result[1];
        statsReport.set(WorkflowInstance.WorkflowState.valueOf(state), count);
      }

      // count by workflow definition and workflow state
      List<WorkflowDefinition> workflowDefinitions =
              new ArrayList(workflowDefinitionScanner.getWorkflowDefinitions().values());

      for (WorkflowDefinition workflowDefinition: workflowDefinitions) {
        WorkflowDefinitionReport definitionsReport = new WorkflowDefinitionReport(workflowDefinition.getId());

        Query query2 = em.createNamedQuery("Workflow.countByStateAndDefinition");
        query2.setParameter("template", workflowDefinition.getId());
        query2.setParameter("organizationId", orgId);
        List<Object[]> results2 = query2.getResultList();

        for (Object[] result : results2) {
          String state = (String) result[0];
          long count = (Long) result[1];
          definitionsReport.set(WorkflowInstance.WorkflowState.valueOf(state), count);
        }

        // count by current operation, workflow definition and workflow state
        List<WorkflowOperationDefinition> workflowOperationDefinitions = workflowDefinition.getOperations();

        for (WorkflowOperationDefinition workflowOperationDefinition: workflowOperationDefinitions) {
          WorkflowOperationReport operationsReport = new WorkflowOperationReport(workflowOperationDefinition.getId());

          Query query3 = em.createNamedQuery("Workflow.countByStateDefinitionAndOperation");
          query3.setParameter("template", workflowDefinition.getId());
          query3.setParameter("currentOperation", workflowOperationDefinition.getId());
          query3.setParameter("organizationId", orgId);
          List<Object[]> results3 = query3.getResultList();

          for (Object[] result : results3) {
            String state = (String) result[0];
            long count = (Long) result[1];
            operationsReport.set(WorkflowInstance.WorkflowState.valueOf(state), count);
          }
          definitionsReport.addOperationsReport(operationsReport);
        }
        statsReport.addDefinitionsReport(definitionsReport);
      }
      return statsReport;

    } finally {
      if (em != null)
        em.close();
    }
  }

  void removeFromDatabase(WorkflowInstance instance) throws WorkflowDatabaseException, NotFoundException {

    JpaWorkflow jpaWorkflow;
    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      jpaWorkflow = em.find(JpaWorkflow.class, instance.getId());
      if (jpaWorkflow == null) {
        logger.error("Workflow with id {} cannot be deleted: Not found.", instance.getId());
        tx.rollback();
        throw new NotFoundException("Workflow with ID '" + instance.getId() + "' not found");
      }

      em.remove(jpaWorkflow);
      tx.commit();
      logger.debug("Workflow with id {} was deleted.", instance.getId());
    } finally {
      if (em != null)
        em.close();
    }
  }

  void updateInDatabase(WorkflowInstance instance) throws WorkflowDatabaseException {

    JpaWorkflow jpaWorkflow;
    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      jpaWorkflow = JpaWorkflow.from(instance);
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaWorkflow fromDb = em.find(JpaWorkflow.class, instance.getId());
      if (fromDb == null) {
        em.persist(jpaWorkflow);
      } else {
        em.merge(jpaWorkflow);
      }
      tx.commit();
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } catch (WorkflowParsingException e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Callback for setting the entity manager factory.
   *
   * @param emf
   *          the entity manager factory to set
   */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback to set the workflow definition scanner
   *
   * @param scanner
   *          the workflow definition scanner
   */
  protected void addWorkflowDefinitionScanner(WorkflowDefinitionScanner scanner) {
    workflowDefinitionScanner = scanner;
  }

  private List<WorkflowInstance> getWorkflowInstancesForQuery(Query query) throws WorkflowDatabaseException {
    List<JpaWorkflow> results = query.getResultList();
    List<WorkflowInstance> workflows = new ArrayList<>(results.size());
    for (JpaWorkflow jpaWorkflow: results) {
      try {
        workflows.add(jpaWorkflow.toWorkflow());
      } catch (MediaPackageException | WorkflowParsingException e) {
        throw new WorkflowDatabaseException(e);
      }
    }
    return workflows;
  }
}
