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

package org.opencastproject.capture.admin.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.opencastproject.capture.admin.api.AgentState.KNOWN_STATES;
import static org.opencastproject.capture.admin.api.AgentState.UNKNOWN;
import static org.opencastproject.util.OsgiUtil.getOptContextProperty;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple3;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;

/**
 * IMPL for the capture-admin service (MH-1336, MH-1394, MH-1457, MH-1475 and MH-1476).
 */
public class CaptureAgentStateServiceImpl implements CaptureAgentStateService, ManagedServiceFactory {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateServiceImpl.class);

  /** The name of the persistence unit for this class */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl";

  /** The delimiter for the CA configuration cache */
  private static final String DELIMITER = ";==;";

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** The security service */
  protected SecurityService securityService;

  /** Maps the configuration PID to the agent ID, so agents can be updated via the configuration factory pattern */
  protected Map<String, String> pidMap = new ConcurrentHashMap<>();

  /** A cache of CA properties, which lightens the load on the SQL server */
  private LoadingCache<String, Object> agentCache = null;

  /** Configuration key for capture agent timeout in minutes before being marked offline */
  public static final String CAPTURE_AGENT_TIMEOUT_KEY = "org.opencastproject.capture.admin.timeout";

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
  }

  public void activate(ComponentContext cc) {

    // Set up the agent cache
    int timeoutInMinutes = 120;

    Option<String> timeout = getOptContextProperty(cc, CAPTURE_AGENT_TIMEOUT_KEY);

    if (timeout.isSome()) {
      try {
        timeoutInMinutes = Integer.parseInt(timeout.get());
      } catch (NumberFormatException e) {
        logger.warn("Invalid configuration for capture agent status timeout (minutes) ({}={})",
                CAPTURE_AGENT_TIMEOUT_KEY, timeout.get());
      }
    }

    setupAgentCache(timeoutInMinutes, TimeUnit.MINUTES);
    logger.info("Capture agent status timeout is {} minutes", timeoutInMinutes);
  }

  public void deactivate() {
    agentCache.invalidateAll();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgent(java.lang.String)
   */
  @Override
  public Agent getAgent(String name) throws NotFoundException {
    String org = securityService.getOrganization().getId();
    Agent agent = getAgent(name, org);
    return updateCachedLastHeardFrom(agent, org);
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
   * @param organization
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
   * Mix in the last-seen timestamp from the agent cache
   *
   * @param agent
   *          The Agent you wish to update
   * @param org
   *          the organization
   * @return the agent
   */
  protected Agent updateCachedLastHeardFrom(Agent agent, String org) {
    String agentKey = agent.getName().concat(DELIMITER).concat(org);
    Tuple3<String, Properties, Long> cachedAgent = (Tuple3) agentCache.getUnchecked(agentKey);
    if (cachedAgent != null) {
      agent.setLastHeardFrom(cachedAgent.getC());
    }
    return agent;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentState(java.lang.String)
   */
  @Override
  public String getAgentState(String agentName) throws NotFoundException {
    String orgId = securityService.getOrganization().getId();
    Tuple3<String, Properties, Long> agent = getAgentFromCache(agentName, orgId);
    return agent.getA();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentState(java.lang.String,
   *      java.lang.String)
   */
  @Override
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
      //Check the return code, if it's false then we don't need to update the DB, and we should also return false
      if (!updateAgentInCache(agentName, state, orgId)) {
        return false;
      }

      agent = (AgentImpl) getAgent(agentName);

      // the agent is known, so set the state
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      agent.setState(state);
      if (!AgentState.UNKNOWN.equals(state)) {
        agent.setLastHeardFrom(System.currentTimeMillis());
      }
    } catch (NotFoundException e) {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      logger.debug("Creating Agent {} with state {}.", agentName, state);
      agent = new AgentImpl(agentName, orgId, state, "", new Properties());
    }
    updateAgentInDatabase(agent);
    return true;
  }

  /**
   * Updates the agent cache, and tells you whether you need to update the database as well
   *
   * @param agentName
   *             The name of the agent in thecache
   * @param state
   *             The new state for the agent
   * @param orgId
   *             The organization the agent is a part of
   * @return
   *             True if the agent state database needs to be updated, false otherwise
   */
  private boolean updateAgentInCache(String agentName, String state, String orgId) {
    return updateAgentInCache(agentName, state, orgId, null);
  }

  /**
   * Updates the agent cache, and tells you whether you need to update the database as well
   *
   * @param agentName
   *             The name of the agent in thecache
   * @param state
   *             The new state for the agent
   * @param orgId
   *             The organization the agent is a part of
   * @param configuration
   *             The agent's configuration
   * @return
   *             True if the agent state database needs to be updated, false otherwise
   */
  private boolean updateAgentInCache(String agentName, String state, String orgId, Properties configuration) {
    try {
      String agentState = getAgentFromCache(agentName, orgId).getA();
      Properties config = getAgentConfiguration(agentName);
      if (configuration != null) {
        config = configuration;
      }
      if (!AgentState.UNKNOWN.equals(state)) {
        agentCache.put(agentName.concat(DELIMITER).concat(orgId),
            Tuple3.tuple3(state, config, Long.valueOf(System.currentTimeMillis())));
      } else {
        //If we're putting the agent into an unknown state we're assuming that we didn't get a check in
        // therefore we don't update the timestamp and persist to the DB
        agentCache.put(agentName.concat(DELIMITER).concat(orgId),
            Tuple3.tuple3(state, config, getAgentFromCache(agentName, orgId).getC()));
      }
      if (agentState.equals(state)) {
        return false;
      }
      return true;
    } catch (NotFoundException e) {
      agentCache.put(agentName.concat(DELIMITER).concat(orgId),
              Tuple3.tuple3(state, configuration, Long.valueOf(System.currentTimeMillis())));
      return true;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentUrl(String, String)
   */
  @Override
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
  @Override
  public void removeAgent(String agentName) throws NotFoundException {
    deleteAgentFromDatabase(agentName);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownAgents()
   */
  @Override
  public Map<String, Agent> getKnownAgents() {
    agentCache.cleanUp();
    EntityManager em = null;
    User user = securityService.getUser();
    Organization org = securityService.getOrganization();
    String orgAdmin = org.getAdminRole();
    Set<Role> roles = user.getRoles();
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
          for (Role role : roles) {
            if (schedulerRoles.contains(role.getName())) {
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
      Map<String, Agent> map = new TreeMap<>();
      for (AgentImpl agent : agents) {
        map.put(agent.getName(), updateCachedLastHeardFrom(agent, org.getId()));
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
  @Override
  public Properties getAgentCapabilities(String agentName) throws NotFoundException {
    return getAgent(agentName).getCapabilities();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentConfiguration(java.lang.String)
   */
  @Override
  public Properties getAgentConfiguration(String agentName) throws NotFoundException {
    String orgId = securityService.getOrganization().getId();
    Tuple3<String, Properties, Long> agent = getAgentFromCache(agentName, orgId);
    return agent.getB();
  }

  @SuppressWarnings("unchecked")
  private Tuple3<String, Properties, Long> getAgentFromCache(String agentName, String orgId) throws NotFoundException {
    Object agent = agentCache.getUnchecked(agentName.concat(DELIMITER).concat(orgId));
    if (agent == nullToken) {
      throw new NotFoundException();
    } else {
      return (Tuple3<String, Properties, Long>) agent;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentConfiguration
   */
  @Override
  public boolean setAgentConfiguration(String agentName, Properties configuration) {
    if (StringUtils.isBlank(agentName))
      throw new IllegalArgumentException("Unable to set agent state, agent name is blank or null.");

    String orgId = securityService.getOrganization().getId();
    AgentImpl agent;
    try {
      Properties agentConfig = getAgentFromCache(agentName, orgId).getB();
      if (agentConfig.equals(configuration)) {
        agentCache.put(agentName.concat(DELIMITER).concat(orgId),
                Tuple3.tuple3(getAgentState(agentName), agentConfig, Long.valueOf(System.currentTimeMillis())));
        return false;
      }

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
    updateAgentInDatabase(agent, true);
  }

  /**
   * Updates or adds an agent to the database.
   *
   * @param agent
   *          The Agent you wish to modify or add in the database.
   * @param updateFromCache
   *          True to update the last heard from timestamp from the agentCache, false to avoid this.
   *          Note that you should nearly always update the cache, this was added to avoid deadlocks when removing agents from the cache. 
   */
  private void updateAgentInDatabase(AgentImpl agent, boolean updateFromCache) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      AgentImpl existing = getAgentEntity(agent.getName(), agent.getOrganization(), em);

      // Update the last seen property from the agent cache
      if (existing != null && updateFromCache) {
        try {
          Tuple3<String, Properties, Long> cachedAgent = getAgentFromCache(existing.getName(),
                  existing.getOrganization());
          if (agent != null && cachedAgent != null) {
            agent.setLastHeardFrom(cachedAgent.getC());
          }
        } catch (NotFoundException e) {
          // That's fine
        }
      }

      if (existing == null) {
        em.persist(agent);
      } else {
        existing.setConfiguration(agent.getConfiguration());
        if (!AgentState.UNKNOWN.equals(agent.getState())) {
          existing.setLastHeardFrom(agent.getLastHeardFrom());
        }
        existing.setState(agent.getState());
        existing.setSchedulerRoles(agent.getSchedulerRoles());
        existing.setUrl(agent.getUrl());
        em.merge(existing);
      }
      tx.commit();
      if (updateFromCache) {
        updateAgentInCache(agent.getName(), agent.getState(), agent.getOrganization(), agent.getConfiguration());
      }
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
      agentCache.invalidate(agentName.concat(DELIMITER).concat(org));
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in deleteAgent.");
    } finally {
      if (em != null)
        em.close();
    }
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

  protected void setupAgentCache(int count, TimeUnit unit) {
    // Setup the agent cache
    RemovalListener<String, Object> removalListener = new RemovalListener<String, Object>() {
      private Set<String> ignoredStates = new LinkedHashSet<>(Arrays.asList(AgentState.UNKNOWN, AgentState.OFFLINE));
      @Override
      public void onRemoval(RemovalNotification<String, Object> removal) {
        if (RemovalCause.EXPIRED.equals(removal.getCause())) {
          String org = securityService.getOrganization().getId();
          try {
            String agentName = removal.getKey().split(DELIMITER)[0];
            AgentImpl agent = getAgent(agentName, org);
            if (!ignoredStates.contains(agent.getState())) {
              agent.setState(AgentState.OFFLINE);
              updateAgentInDatabase(agent, false);
            }
          } catch (NotFoundException e) {
            //Ignore this
            //It should not happen, and if it does we just don't update the non-existant agent in the DB
          }
        }
      }
    };
    agentCache = CacheBuilder.newBuilder().expireAfterWrite(count, unit).removalListener(removalListener).build(new CacheLoader<String, Object>() {
      @Override
      public Object load(String id) {
        String[] key = id.split(DELIMITER);
        AgentImpl agent;
        try {
          agent = getAgent(key[0], key[1]);
        } catch (NotFoundException e) {
          return nullToken;
        }
        return Tuple3.tuple3(agent.getState(), agent.getConfiguration(), agent.getLastHeardFrom());
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
   */
  @Override
  public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
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
