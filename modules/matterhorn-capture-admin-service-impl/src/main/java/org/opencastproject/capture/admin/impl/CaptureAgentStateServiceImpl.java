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

import static org.apache.commons.lang.StringUtils.isBlank;
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
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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

  /** The delimiter for the CA configuration cache */
  private static final String DELIMITER = ";==;";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** The persistence properties */
  protected Map<String, Object> persistenceProperties;

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

  /** A cache of CA properties, which lightens the load on the SQL server */
  private ConcurrentMap<String, Object> agentCache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

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
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
    recordings = new HashMap<String, Recording>();
  }

  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory(
            "org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl", persistenceProperties);

    // Setup the agent cache
    agentCache = new MapMaker().expireAfterWrite(1, TimeUnit.HOURS).makeComputingMap(new Function<String, Object>() {
      public Object apply(String id) {
        String[] key = id.split(DELIMITER);
        AgentImpl agent;
        try {
          agent = getAgent(key[0], key[1]);
        } catch (NotFoundException e) {
          return nullToken;
        }
        return agent == null ? nullToken : Tuple.tuple(agent.getState(), agent.getConfiguration());
      }
    });
  }

  public void deactivate() {
    agentCache.clear();
    if (emf != null)
      emf.close();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgent(java.lang.String)
   */
  @Override
  public Agent getAgent(String name) throws NotFoundException {
    return getAgent(name, securityService.getOrganization().getId());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#updateAgent(Agent)
   */
  @Override
  public void updateAgent(Agent agent) {
    updateAgentInDatabase((AgentImpl) agent);
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
  protected AgentImpl getAgent(String name, String org) throws NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      AgentImpl agent = getAgentEntity(name, org, em);
      if (agent == null)
        throw new NotFoundException();
      return agent;
    } finally {
      if (em != null)
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
   * @return the agent or <code>null</code> if no agent has been found
   */
  protected AgentImpl getAgentEntity(String name, String organization, EntityManager em) {
    try {
      Query q = em.createNamedQuery("Agent.get");
      q.setParameter("id", name);
      q.setParameter("org", organization);
      return (AgentImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentState(java.lang.String)
   */
  public String getAgentState(String agentName) throws NotFoundException {
    String orgId = securityService.getOrganization().getId();
    Tuple<String, Properties> agent = getAgentFromCache(agentName, orgId);
    return agent.getA();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentState(java.lang.String,
   *      java.lang.String)
   */
  public boolean setAgentState(String agentName, String state) {
    if (StringUtils.isBlank(agentName))
      throw new IllegalArgumentException("Unable to set agent state, agent name is blank or null.");
    if (StringUtils.isBlank(state))
      throw new IllegalArgumentException("Unable to set agent state, state is blank or null.");
    if (!KNOWN_STATES.contains(state))
      throw new IllegalArgumentException("Can not set agent to an invalid state: ".concat(state));

    logger.debug("Agent '{}' state set to '{}'", agentName, state);
    AgentImpl agent;
    String orgId = securityService.getOrganization().getId();
    try {
      String agentState = getAgentFromCache(agentName, orgId).getA();
      if (agentState.equals(state))
        return false;

      agent = (AgentImpl) getAgent(agentName);

      // the agent is known, so set the state
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      agent.setState(state);
    } catch (NotFoundException e) {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      logger.debug("Creating Agent {} with state {}.", agentName, state);
      agent = new AgentImpl(agentName, orgId, state, "", new Properties());
    }
    updateAgentInDatabase(agent);
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentUrl(String, String)
   */
  public boolean setAgentUrl(String agentName, String agentUrl) throws NotFoundException {
    Agent agent = getAgent(agentName);
    if (agent.getUrl().equals(agentUrl))
      return false;
    agent.setUrl(agentUrl);
    updateAgentInDatabase((AgentImpl) agent);
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeAgent(java.lang.String)
   */
  public void removeAgent(String agentName) throws NotFoundException {
    deleteAgentFromDatabase(agentName);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownAgents()
   */
  public Map<String, Agent> getKnownAgents() {
    EntityManager em = null;
    User user = securityService.getUser();
    Organization org = securityService.getOrganization();
    String orgAdmin = org.getAdminRole();
    String[] roles = user.getRoles();
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Agent.byOrganization");
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
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentCapabilities(java.lang.String)
   */
  public Properties getAgentCapabilities(String agentName) throws NotFoundException {
    return getAgent(agentName).getCapabilities();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentConfiguration(java.lang.String)
   */
  public Properties getAgentConfiguration(String agentName) throws NotFoundException {
    String orgId = securityService.getOrganization().getId();
    Tuple<String, Properties> agent = getAgentFromCache(agentName, orgId);
    return agent.getB();
  }

  @SuppressWarnings("unchecked")
  private Tuple<String, Properties> getAgentFromCache(String agentName, String orgId) throws NotFoundException {
    Object agent = agentCache.get(agentName.concat(DELIMITER).concat(orgId));
    if (agent == nullToken) {
      throw new NotFoundException();
    } else {
      return (Tuple<String, Properties>) agent;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentConfiguration
   */
  public boolean setAgentConfiguration(String agentName, Properties configuration) {
    if (StringUtils.isBlank(agentName))
      throw new IllegalArgumentException("Unable to set agent state, agent name is blank or null.");

    String orgId = securityService.getOrganization().getId();
    AgentImpl agent;
    try {
      Properties agentConfig = getAgentFromCache(agentName, orgId).getB();
      if (agentConfig.equals(configuration))
        return false;

      agent = (AgentImpl) getAgent(agentName);
      logger.debug("Setting Agent {}'s capabilities", agentName);
      agent.setConfiguration(configuration);
    } catch (NotFoundException e) {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      logger.debug("Creating Agent {} with state {}.", agentName, UNKNOWN);
      agent = new AgentImpl(agentName, orgId, UNKNOWN, "", configuration);
    }
    updateAgentInDatabase(agent);
    return true;
  }

  /**
   * Updates or adds an agent to the database.
   * 
   * @param agent
   *          The Agent you wish to modify or add in the database.
   */
  protected void updateAgentInDatabase(AgentImpl agent) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      AgentImpl existing = getAgentEntity(agent.getName(), agent.getOrganization(), em);
      if (existing == null) {
        em.persist(agent);
      } else {
        existing.setConfiguration(agent.getConfiguration());
        existing.setLastHeardFrom(agent.getLastHeardFrom());
        existing.setState(agent.getState());
        existing.setSchedulerRoles(agent.getSchedulerRoles());
        existing.setUrl(agent.getUrl());
        em.merge(existing);
      }
      tx.commit();
      agentCache.put(agent.getName().concat(DELIMITER).concat(agent.getOrganization()),
              Tuple.tuple(agent.getState(), agent.getConfiguration()));
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in updateAgent.");
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Removes an agent from the database.
   * 
   * @param agentName
   *          The name of the agent you wish to remove.
   */
  private void deleteAgentFromDatabase(String agentName) throws NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      String org = securityService.getOrganization().getId();
      Agent existing = getAgentEntity(agentName, org, em);
      if (existing == null)
        throw new NotFoundException();
      em.remove(existing);
      tx.commit();
      agentCache.remove(agentName.concat(DELIMITER).concat(org));
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in deleteAgent.");
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String id) throws NotFoundException {
    Recording req = recordings.get(id);
    // If that recording doesn't exist, return null
    if (req == null) {
      logger.debug("Recording {} does not exist in the system.", id);
      throw new NotFoundException();
    }

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
    if (StringUtils.isBlank(id))
      throw new IllegalArgumentException("id can not be null");
    if (StringUtils.isBlank(state))
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
      logger.info("Recording id '{}' is not a long, assuming an unscheduled capture", recordingId);
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

    // Does the workflow exist?
    if (workflowToUpdate == null) {
      logger.warn("The workflow '{}' cannot be updated because it does not exist", recordingId);
      return;
    }

    WorkflowState wfState = workflowToUpdate.getState();
    switch (workflowToUpdate.getState()) {
      case FAILED:
      case FAILING:
      case STOPPED:
      case SUCCEEDED:
        logger.debug("The workflow '{}' should not be updated because it is {}", recordingId, wfState.toString()
                .toLowerCase());
        return;
      default:
        break;

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
  public void removeRecording(String id) throws NotFoundException {
    logger.debug("Removing Recording {}.", id);
    Recording removed = recordings.remove(id);
    if (removed == null)
      throw new NotFoundException();
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
    if (isBlank(nameConfig))
      throw new ConfigurationException("id", "must be specified");

    nameConfig = nameConfig.trim();

    String urlConfig = (String) properties.get("url");
    if (isBlank(urlConfig))
      throw new ConfigurationException("url", "must be specified");
    urlConfig = urlConfig.trim();

    String orgConfig = (String) properties.get("organization");
    if (isBlank(orgConfig))
      throw new ConfigurationException("organization", "must be specified");
    orgConfig = orgConfig.trim();

    String schedulerRolesConfig = (String) properties.get("schedulerRoles");
    if (isBlank(schedulerRolesConfig))
      throw new ConfigurationException("schedulerRoles", "must be specified");
    String[] schedulerRoles = schedulerRolesConfig.trim().split(",");

    // If we don't already have a mapping for this PID, create one
    if (!pidMap.containsKey(pid)) {
      pidMap.put(pid, nameConfig);
    }

    AgentImpl agent;
    try {
      agent = getAgent(nameConfig, orgConfig);
      agent.setUrl(urlConfig);
      agent.setState(UNKNOWN);
    } catch (NotFoundException e) {
      agent = new AgentImpl(nameConfig, orgConfig, UNKNOWN, urlConfig, new Properties());
    }

    for (String role : schedulerRoles) {
      agent.schedulerRoles.add(role.trim());
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
      try {
        deleteAgentFromDatabase(agentId);
      } catch (NotFoundException e) {
        logger.warn("Unable to delete capture agent '{}'", agentId);
      }
    }
  }
}
