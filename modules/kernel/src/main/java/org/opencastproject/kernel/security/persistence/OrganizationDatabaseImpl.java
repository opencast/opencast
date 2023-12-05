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

package org.opencastproject.kernel.security.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implements {@link OrganizationDatabase}. Defines permanent storage for series.
 */
@Component(
  property = {
    "service.description=Organization Persistence"
  },
  immediate = true,
  service = { OrganizationDatabase.class }
)
public class OrganizationDatabaseImpl implements OrganizationDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationDatabaseImpl.class);

  static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.common)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for kernel");
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#deleteOrganization(java.lang.String)
   */
  @Override
  public void deleteOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException {
    try {
      db.execTxChecked(em -> {
        Optional<JpaOrganization> organization = getOrganizationEntityQuery(orgId).apply(em);
        if (organization.isEmpty()) {
          throw new NotFoundException("Organization " + orgId + " does not exist");
        }
        em.remove(organization.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete organization: {}", e.getMessage());
      throw new OrganizationDatabaseException(e);
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#storeOrganization(org.opencastproject.security.api.Organization)
   */
  @Override
  public void storeOrganization(Organization org) throws OrganizationDatabaseException {
    try {
      db.execTx(em -> {
        Optional<JpaOrganization> organizationEntity = getOrganizationEntityQuery(org.getId()).apply(em);
        if (organizationEntity.isEmpty()) {
          JpaOrganization organization = new JpaOrganization(org.getId(), org.getName(), org.getServers(),
              org.getAdminRole(), org.getAnonymousRole(), org.getProperties());
          em.persist(organization);
        } else {
          organizationEntity.get().setName(org.getName());
          organizationEntity.get().setAdminRole(org.getAdminRole());
          organizationEntity.get().setAnonymousRole(org.getAnonymousRole());
          for (Map.Entry<String, Integer> servers : org.getServers().entrySet()) {
            organizationEntity.get().addServer(servers.getKey(), servers.getValue());
          }
          organizationEntity.get().setServers(org.getServers());
          organizationEntity.get().setProperties(org.getProperties());
          em.merge(organizationEntity.get());
        }
      });
    } catch (Exception e) {
      logger.error("Could not update organization: {}", e.getMessage());
      throw new OrganizationDatabaseException(e);
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#getOrganization(java.lang.String)
   */
  @Override
  public Organization getOrganization(String id) throws NotFoundException, OrganizationDatabaseException {
    return db.exec(getOrganizationEntityQuery(id))
        .orElseThrow(NotFoundException::new);
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#countOrganizations()
   */
  @Override
  public int countOrganizations() throws OrganizationDatabaseException {
    try {
      return db.exec(namedQuery.find("Organization.getCount", Long.class)).intValue();
    } catch (Exception e) {
      logger.error("Could not find number of organizations.", e);
      throw new OrganizationDatabaseException(e);
    }
  }

  @Override
  public Organization getOrganizationByHost(String host, int port) throws OrganizationDatabaseException, NotFoundException {
    try {
      return db.exec(namedQuery.findOpt(
          "Organization.findByHost",
          JpaOrganization.class,
          Pair.of("serverName", host),
          Pair.of("port", port)
      )).orElseThrow(NotFoundException::new);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new OrganizationDatabaseException(e);
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#getOrganizations()
   */
  @Override
  public List<Organization> getOrganizations() throws OrganizationDatabaseException {
    try {
      return db.exec(namedQuery.findAll("Organization.findAll", Organization.class));
    } catch (Exception e) {
      throw new OrganizationDatabaseException(e);
    }
  }

  /**
   * @see org.opencastproject.kernel.security.persistence.OrganizationDatabase#containsOrganization(String)
   */
  @Override
  public boolean containsOrganization(String orgId) throws OrganizationDatabaseException {
    return db.exec(getOrganizationEntityQuery(orgId)).isPresent();
  }

  /**
   * Return the persisted organization entity by its id
   *
   * @param id
   *          the organization id
   * @return the organization or <code>null</code> if not found
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  private Function<EntityManager, Optional<JpaOrganization>> getOrganizationEntityQuery(String id) {
    return namedQuery.findOpt(
        "Organization.findById",
        JpaOrganization.class,
        Pair.of("id", id)
    );
  }
}
