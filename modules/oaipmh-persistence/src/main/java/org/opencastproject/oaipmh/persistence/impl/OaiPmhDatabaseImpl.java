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
package org.opencastproject.oaipmh.persistence.impl;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;

/** Implements {@link org.opencastproject.oaipmh.persistence.OaiPmhDatabase}. Defines permanent storage for OAI-PMH. */
public class OaiPmhDatabaseImpl extends AbstractOaiPmhDatabase {

  public static final String PERSISTENCE_UNIT_NAME = "org.opencastproject.oaipmh";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhDatabaseImpl.class);

  /** Factory used to create {@link javax.persistence.EntityManager}s for transactions */
  private EntityManagerFactory emf;

  private SecurityService securityService;

  /** The workspace */
  private Workspace workspace;

  @Override
  public EntityManagerFactory getEmf() {
    return emf;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for OAI-PMH");
  }

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
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
   * OSGi callback to set the workspace.
   *
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
