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
package org.opencastproject.kernel.bundleinfo;

import org.opencastproject.util.persistence.PersistenceEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;

/** OSGi bound bundle info database. */
public class OsgiBundleInfoDb extends AbstractBundleInfoDb {
  private static final Logger logger = LoggerFactory.getLogger(OsgiBundleInfoDb.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.kernel";

  private PersistenceEnv penv;
  private Map<String, Object> persistenceProperties;
  private PersistenceProvider persistenceProvider;

  @Override protected PersistenceEnv getPersistenceEnv() {
    return penv;
  }

  /** OSGi callback */
  public void activate() {
    penv = persistenceEnvironment(persistenceProvider,
                                  PERSISTENCE_UNIT,
                                  persistenceProperties);
  }

  public void deactivate() {
    logger.info("Closing persistence environment");
    penv.close();
  }

  /** OSGi DI */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** OSGi DI */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
}
