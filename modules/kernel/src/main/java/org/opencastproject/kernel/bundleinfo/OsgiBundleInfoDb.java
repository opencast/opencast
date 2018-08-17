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
package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;

import org.opencastproject.util.persistence.PersistenceEnv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;

/** OSGi bound bundle info database. */
public class OsgiBundleInfoDb extends AbstractBundleInfoDb {
  private static final Logger logger = LoggerFactory.getLogger(OsgiBundleInfoDb.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.kernel";

  private EntityManagerFactory emf;
  private PersistenceEnv penv;

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override protected PersistenceEnv getPersistenceEnv() {
    return penv;
  }

  /** OSGi callback */
  public void activate() {
    penv = persistenceEnvironment(emf);
  }

  public void deactivate() {
    logger.info("Closing persistence environment");
    penv.close();
  }
}
