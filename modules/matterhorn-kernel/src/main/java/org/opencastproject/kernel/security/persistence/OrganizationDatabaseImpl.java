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

package org.opencastproject.kernel.security.persistence;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link OrganizationDatabase}. Defines permanent storage for series.
 */
public class OrganizationDatabaseImpl implements OrganizationDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The security service */
  protected SecurityService securityService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for kernel");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.kernel", persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

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
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#deleteOrganization(java.lang.String)
   */
  @Override
  public void deleteOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaOrganization organization = getOrganizationEntity(orgId, em);
      if (organization == null)
        throw new NotFoundException("Organization " + orgId + " does not exist");

      em.remove(organization);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete organization: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new OrganizationDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#storeOrganization(org.opencastproject.security.api.Organization)
   */
  @Override
  public void storeOrganization(Organization org) throws OrganizationDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaOrganization organizationEntity = getOrganizationEntity(org.getId(), em);
      if (organizationEntity == null) {
        JpaOrganization organization = new JpaOrganization(org.getId(), org.getName(), org.getServers(),
                org.getAdminRole(), org.getAnonymousRole(), org.getProperties());
        em.persist(organization);
      } else {
        organizationEntity.setName(org.getName());
        organizationEntity.setAdminRole(org.getAdminRole());
        organizationEntity.setAnonymousRole(org.getAnonymousRole());
        for (Map.Entry<String, Integer> servers : org.getServers().entrySet()) {
          organizationEntity.addServer(servers.getKey(), servers.getValue());
        }
        organizationEntity.setServers(org.getServers());
        organizationEntity.setProperties(org.getProperties());
        em.merge(organizationEntity);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not update organization: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new OrganizationDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#getOrganization(java.lang.String)
   */
  @Override
  public Organization getOrganization(String id) throws NotFoundException, OrganizationDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      JpaOrganization entity = getOrganizationEntity(id, em);
      if (entity == null)
        throw new NotFoundException();
      return entity;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#countOrganizations()
   */
  @Override
  public int countOrganizations() throws OrganizationDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Organization.getCount");
      Long total = (Long) query.getSingleResult();
      return total.intValue();
    } catch (Exception e) {
      logger.error("Could not find number of organizations.", e);
      throw new OrganizationDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Organization getOrganizationByHost(String host, int port) throws OrganizationDatabaseException,
          NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Organization.findByHost");
      q.setParameter("serverName", host);
      q.setParameter("port", port);
      return (JpaOrganization) q.getSingleResult();
    } catch (NoResultException e) {
      throw new NotFoundException();
    } catch (Exception e) {
      throw new OrganizationDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#getOrganizations()
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<Organization> getOrganizations() throws OrganizationDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Organization.findAll");
      return q.getResultList();
    } catch (Exception e) {
      throw new OrganizationDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#containsOrganization(String)
   */
  @Override
  public boolean containsOrganization(String orgId) throws OrganizationDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      JpaOrganization organization = getOrganizationEntity(orgId, em);
      return organization != null ? true : false;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Return the persisted organization entity by its id
   *
   * @param id
   *          the organization id
   * @param em
   *          an open entity manager
   * @return the organization or <code>null</code> if not found
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  private JpaOrganization getOrganizationEntity(String id, EntityManager em) throws OrganizationDatabaseException {
    Query q = em.createNamedQuery("Organization.findById");
    q.setParameter("id", id);
    try {
      return (JpaOrganization) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new OrganizationDatabaseException(e);
    }
  }
}
