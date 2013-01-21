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
package org.opencastproject.episode.impl.persistence;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

/** Implements {@link EpisodeServiceDatabase}. Defines permanent storage for series. */
public class OsgiEpisodeServiceDatabase extends AbstractEpisodeServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OsgiEpisodeServiceDatabase.class);

  /** Persistence provider set by OSGi */
  private PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link javax.persistence.EntityManagerFactory} */
  private Map<String, Object> persistenceProperties;

  /** The security service */
  private SecurityService securityService;
  private PersistenceEnv penv;

  @Override protected PersistenceEnv getPenv() {
    return penv;
  }

  @Override protected SecurityService getSecurityService() {
    return securityService;
  }

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for episodes");
    penv = PersistenceUtil.newPersistenceEnvironment(persistenceProvider, "org.opencastproject.episode.impl.persistence", persistenceProperties);
  }

  /** OSGi callback. Closes entity manager factory. */
  public void deactivate(ComponentContext cc) {
    penv.close();
  }

  /** OSGi callback to set persistence properties. */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** OSGi callback to set persistence provider. */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /** OSGi callback to set the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
