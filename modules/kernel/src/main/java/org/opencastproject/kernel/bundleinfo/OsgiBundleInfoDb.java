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

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;

/** OSGi bound bundle info database. */
@Component(
    immediate = true,
    service = BundleInfoDb.class
)
public class OsgiBundleInfoDb extends AbstractBundleInfoDb {
  private static final Logger logger = LoggerFactory.getLogger(OsgiBundleInfoDb.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.kernel";

  private DBSessionFactory dbSessionFactory;
  private EntityManagerFactory emf;
  private DBSession db;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.kernel)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Override
  protected DBSession getDBSession() {
    return db;
  }

  /** OSGi callback */
  @Activate
  public void activate() {
    db = dbSessionFactory.createSession(emf);
  }

  public void deactivate() {
    logger.info("Closing persistence environment");
    db.close();
  }
}
