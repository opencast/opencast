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
package org.opencastproject.capture.admin.impl;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.opencastproject.capture.admin.api.AgentState.KNOWN_STATES;
import static org.opencastproject.capture.admin.api.AgentState.UNKNOWN;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * IMPL for the capture-admin service (MH-1336, MH-1394, MH-1457, MH-1475 and MH-1476).
 */
public class CaptureAgentStateServiceImpl implements CaptureAgentStateService, ManagedServiceFactory {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateServiceImpl.class);

  /** The name of the persistence unit for this class */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** The persistence properties */
  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** The workflow service */
  protected WorkflowService workflowService;

  /** The security service */
  protected SecurityService securityService;

  // TODO: Remove the in-memory recordings map, and use the database instead
  private HashMap<String, Recording> recordings;

  /** Maps the configuration PID to the agent ID, so agents can be updated via the configuration factory pattern */
  protected Map<String, String> pidMap = new ConcurrentHashMap<String, String>();

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * Sets the workflow service
   * 
   * @param workflowService
   *          the workflowService to set
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
    recordings = new HashMap<String, Recording>();
  }

  @SuppressWarnings("unchecked")
  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory(
            "org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl", persistenceProperties);
  }

  public void deactivate() {
    if (emf != null) {
      emf.close();
    }
  }

  /**
   * Gets an agent by name, using the current user's organizational context.
   * 
   * @param name
   *          the unique agent name
   * @return the agent
   */
  protected AgentImpl getAgent(String name) {
    EntityManager em = emf.createEntityManager();
    try {
      return getAgent(name, securityService.getOrganization().getId(), em);
    } finally {
      em.close();
    }
  }

  /**
   * Gets an agent by name and organization.
   * 
   * @param name
   *          the unique agent name
   * @param org
   *          the organization identifier
   * @return the agent
   */
  protected AgentImpl getAgent(String name, String org) {
    EntityManager em = emf.createEntityManager();
    try {
      return getAgent(name, org, em);
    } finally {
      em.close();
    }
  }

  /**
   * Gets an agent by name and organization, using an open entitymanager.
   * 
   * @param name
   *          the unique agent name
   * @param org
   *          the organization
   * @param em
   *          the entity manager
   * @return the agent
   */
  protected AgentImpl getAgent(String name, String organization, EntityManager em) {
    try {
      Query q = em.createQuery("select a from AgentImpl a where a.name = :id and a.organization = :org");
      q.setParameter("id", name);
      q.setParameter("org", organization);
      return (AgentImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } //TODO check if closing em helps here.
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentState(java.lang.String)
   */
  public Agent getAgentState(String agentName) {
    Agent agent = getAgent(agentName);
    // If that agent doesn't exist, return an unknown agent, else return the known agent
    if (agent == null) {
      logger.debug("Agent {} does not exist in the system.", agentName);
    } else {
      logger.debug("Agent {} found, returning state.", agentName);
    }
    return agent;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentState(java.lang.String,
   *      java.lang.String)
   */
  public int setAgentState(String agentName, String state) {

    // Checks the state is not null nor empty
    if (StringUtils.isBlank(state)) {
      logger.debug("Unable to set agent state, state is blank or null.");
      return BAD_PARAMETER;
    } else if (StringUtils.isBlank(agentName)) {
      logger.debug("Unable to set agent state, agent name is blank or null.");
      return BAD_PARAMETER;
    } else if (!KNOWN_STATES.contains(state)) {
      logger.warn("can not set agent to an invalid state: ", state);
      return BAD_PARAMETER;
    } else {
      logger.debug("Agent '{}' state set to '{}'", agentName, state);
    }

    AgentImpl agent = getAgent(agentName);
    if (agent == null) {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      logger.debug("Creating Agent {} with state {}.", agentName, state);
      Organization org = securityService.getOrganization();
      AgentImpl a = new AgentImpl(agentName, org.getId(), state, "", new Properties());
      updateAgentInDatabase(a);
    } else {
      // the agent is known, so set the state
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      agent.setState(state);
      updateAgentInDatabase(agent);
    }

    return OK;
  }

  public boolean setAgentUrl(String agentName, String agentUrl) {
    AgentImpl agent = getAgent(agentName);
    if (agent == null) {
      return false;
    } else {
      agent.setUrl(agentUrl);
      updateAgentInDatabase(agent);
    }
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeAgent(java.lang.String)
   */
  public int removeAgent(String agentName) {
    if (getAgent(agentName) == null) {
      return NO_SUCH_AGENT;
    } else {
      logger.debug("Removing Agent {}.", agentName);
      deleteAgentFromDatabase(agentName);
      return OK;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownAgents()
   */
  public Map<String, Agent> getKnownAgents() {
    EntityManager em = emf.createEntityManager();
    User user = securityService.getUser();
    Organization org = securityService.getOrganization();
    String orgAdmin = org.getAdminRole();
    String[] roles = user.getRoles();
    try {
      Query q = em.createQuery("SELECT a FROM AgentImpl a where a.organization = :org");
      q.setParameter("org", securityService.getOrganization().getId());

      // Filter the results in memory if this user is not an administrator
      List<AgentImpl> agents = q.getResultList();
      if (!user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) && !user.hasRole(orgAdmin)) {
        for (Iterator<AgentImpl> iter = agents.iterator(); iter.hasNext();) {
          AgentImpl agent = iter.next();
          Set<String> schedulerRoles = agent.getSchedulerRoles();
          // If there are no roles associated with this capture agent, it is available to anyone who can pass the
          // coarse-grained web layer security
          if (schedulerRoles == null || schedulerRoles.isEmpty()) {
            continue;
          }
          boolean hasSchedulerRole = false;
          for (String role : roles) {
            if (schedulerRoles.contains(role)) {
              hasSchedulerRole = true;
              break;
            }
          }
          if (!hasSchedulerRole) {
            iter.remove();
          }
        }
      }

      // Build the map that the API defines as agent name->agent
      Map<String, Agent> map = new TreeMap<String, Agent>();
      for (AgentImpl agent : agents) {
        map.put(agent.getName(), agent);
      }
      return map;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentCapabilities(java.lang.String)
   */
  public Properties getAgentCapabilities(String agentName) {

    Agent agent = getAgent(agentName);

    if (agent == null) {
      return null;
    } else {
      return agent.getCapabilities();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentConfiguration(java.lang.String)
   */
  public Properties getAgentConfiguration(String agentName) {
    Agent agent = getAgent(agentName);
    if (agent == null) {
      return null;
    } else {
      return agent.getConfiguration();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentConfiguration
   */
  public int setAgentConfiguration(String agentName, Properties configuration) {
    AgentImpl agent = getAgent(agentName);
    if (agent != null) {
      logger.debug("Setting Agent {}'s capabilities", agentName);
      agent.setConfiguration(configuration);
      updateAgentInDatabase(agent);
    } else {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      if (StringUtils.isBlank(agentName)) {
        logger.debug("Unable to set agent state, agent name is blank or null.");
        return BAD_PARAMETER;
      }
      logger.debug("Creating Agent {} with state {}.", agentName, UNKNOWN);
      Organization org = securityService.getOrganization();
      AgentImpl a = new AgentImpl(agentName, org.getId(), UNKNOWN, "", configuration);
      updateAgentInDatabase(a);
    }

    return OK;
  }

  /**
   * Updates or adds an agent to the database.
   * 
   * @param agent
   *          The Agent you wish to modify or add in the database.
   */
  protected void updateAgentInDatabase(AgentImpl agent) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();
      AgentImpl existing = getAgent(agent.getName(), agent.getOrganization(), em);
      if (existing == null) {
        em.persist(agent);
      } else {
        existing.setConfiguration(agent.getConfiguration());
        existing.setLastHeardFrom(agent.getLastHeardFrom());
        existing.setState(agent.getState());
        existing.setSchedulerRoles(agent.getSchedulerRoles());
        em.merge(existing);
      }
      tx.commit();
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in updateAgent.");
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Removes an agent from the database.
   * 
   * @param agentName
   *          The name of the agent you wish to remove.
   */
  private void deleteAgentFromDatabase(String agentName) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();
      Agent existing = getAgent(agentName, securityService.getOrganization().getId(), em);
      if (existing != null) {
        em.remove(existing);
      }
      tx.commit();
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in deleteAgent.");
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String id) {
    Recording req = recordings.get(id);
    // If that recording doesn't exist, return null
    if (req == null)
      logger.debug("Recording {} does not exist in the system.", id);
    else
      logger.debug("Recording {} found, returning state.", id);

    return req;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setRecordingState(java.lang.String,
   *      java.lang.String)
   * @throws IllegalArgumentException
   */
  public boolean setRecordingState(String id, String state) {
    if (state == null)
      throw new IllegalArgumentException("state can not be null");
    if (!RecordingState.KNOWN_STATES.contains(state)) {
      logger.warn("Invalid recording state: {}.", state);
      return false;
    }
    Recording req = recordings.get(id);
    if (req != null) {
      if (state.equals(req.getState())) {
        logger.debug("Recording state not changed");
        // Reset the state anyway so that the last-heard-from time is correct...
        req.setState(state);
        return true;
      } else {
        logger.debug("Setting Recording {} to state {}.", id, state);
        req.setState(state);
        if (!RecordingState.WORKFLOW_IGNORE_STATES.contains(state)) {
          updateWorkflow(id, state);
        }
        return true;
      }
    } else {
      if (StringUtils.isBlank(id)) {
        logger.debug("Unable to set recording state, recording name is blank or null.");
        return false;
      } else if (StringUtils.isBlank(state)) {
        logger.debug("Unable to set recording state, recording state is blank or null.");
        return false;
      }
      logger.debug("Creating Recording {} with state {}.", id, state);
      Recording r = new RecordingImpl(id, state);
      recordings.put(id, r);
      updateWorkflow(id, state);
      return true;
    }
  }

  /**
   * Resumes a workflow instance associated with this capture, if one exists.
   * 
   * @param recordingId
   *          the recording id, which is assumed to correspond to the scheduled event id
   * @param state
   *          the new state for this recording
   */
  protected void updateWorkflow(String recordingId, String state) {
    if (!RecordingState.CAPTURING.equals(state) && !RecordingState.UPLOADING.equals(state) && !state.endsWith("_error")) {
      logger.debug("Recording state updated to {}.  Not updating an associated workflow.", state);
      return;
    }

    WorkflowInstance workflowToUpdate = null;
    try {
      workflowToUpdate = workflowService.getWorkflowById(Long.parseLong(recordingId));
    } catch (NumberFormatException e) {
      logger.warn("Recording id '{}' is not a long, and is therefore not a valid workflow identifier", recordingId, e);
      return;
    } catch (WorkflowDatabaseException e) {
      logger.warn("Unable to update workflow for recording {}: {}", recordingId, e);
      return;
    } catch (NotFoundException e) {
      logger.warn("Unable to find a workflow with id='{}'", recordingId);
      return;
    } catch (UnauthorizedException e) {
      logger.warn("Can not update workflow: {}", e.getMessage());
    }
    try {
      if (state.endsWith("_error")) {
        workflowToUpdate.getCurrentOperation().setState(WorkflowOperationInstance.OperationState.FAILED);
        workflowToUpdate.setState(WorkflowState.FAILED);
        workflowService.update(workflowToUpdate);
        logger.info("Recording status changed to '{}', failing workflow '{}'", state, workflowToUpdate.getId());
      } else {
        workflowService.resume(workflowToUpdate.getId());
        logger.info("Recording status changed to '{}', resuming workflow '{}'", state, workflowToUpdate.getId());
      }
    } catch (Exception e) {
      logger.warn("Unable to update workflow {}: {}", workflowToUpdate.getId(), e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeRecording(java.lang.String)
   */
  public boolean removeRecording(String id) {
    logger.debug("Removing Recording {}.", id);
    return recordings.remove(id) != null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownRecordings()
   */
  public Map<String, Recording> getKnownRecordings() {
    return recordings;
  }

  public List<String> getKnownRecordingsIds() {
    LinkedList<String> ids = new LinkedList<String>();
    for (Entry<String, Recording> e : recordings.entrySet()) {
      ids.add(e.getValue().getID());
    }
    return ids;
  }

  // // ManagedServiceFactory Methods ////

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#getName()
   */
  @Override
  public String getName() {
    return "org.opencastproject.capture.agent";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
   */
  @Override
  public void updated(String pid, Dictionary properties) throws ConfigurationException {

    // Get the agent properties
    String nameConfig = (String) properties.get("id");
    if (isEmpty(nameConfig)) {
      throw new ConfigurationException("id", "must be specified");
    }
    nameConfig = nameConfig.trim();

    String urlConfig = (String) properties.get("url");
    if (isEmpty(urlConfig)) {
      throw new ConfigurationException("url", "must be specified");
    }
    urlConfig = urlConfig.trim();

    String orgConfig = (String) properties.get("organization");
    if (isEmpty(orgConfig)) {
      throw new ConfigurationException("organization", "must be specified");
    }
    orgConfig = orgConfig.trim();

    String schedulerRolesConfig = (String) properties.get("schedulerRoles");
    if (isEmpty(schedulerRolesConfig)) {
      throw new ConfigurationException("schedulerRoles", "must be specified");
    }
    schedulerRolesConfig = schedulerRolesConfig.trim();

    // If we don't already have a mapping for this PID, create one
    if (!pidMap.containsKey(pid)) {
      pidMap.put(pid, nameConfig);
    }

    AgentImpl agent = getAgent(nameConfig, orgConfig);
    if (agent == null) {
      agent = new AgentImpl(nameConfig, orgConfig, UNKNOWN, urlConfig, new Properties());
    } else {
      agent.url = urlConfig.trim();
      agent.organization = orgConfig.trim();
      agent.state = UNKNOWN;
      String[] schedulerRoles = schedulerRolesConfig.split(",");
      for (String role : schedulerRoles) {
        agent.schedulerRoles.add(role.trim());
      }
    }

    // Update the database
    logger.info("Roles '{}' may schedule '{}'", schedulerRolesConfig, agent.name);
    updateAgentInDatabase(agent);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
   */
  @Override
  public void deleted(String pid) {
    String agentId = pidMap.remove(pid);
    if (agentId == null) {
      logger.warn("{} was not a managed capture agent pid", pid);
    } else {
      deleteAgentFromDatabase(agentId);
    }
  }
}
