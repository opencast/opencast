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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;

import org.opencastproject.authorization.xacml.manager.impl.AclDb;
import org.opencastproject.util.osgi.SimpleServicePublisher;
import org.opencastproject.util.persistence.PersistenceEnv;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

import javax.persistence.EntityManagerFactory;

/** Publishes a {@link JpaAclDb}. */
public class OsgiJpaAclDbPublisher extends SimpleServicePublisher {

  private EntityManagerFactory emf;

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override public ServiceReg registerService(Dictionary properties, ComponentContext cc)
          throws ConfigurationException {
    PersistenceEnv penv = persistenceEnvironment(emf);
    final JpaAclDb aclDb = new JpaAclDb(penv);
    return ServiceReg.reg(registerService(cc, aclDb, AclDb.class, "JPA based ACL Provider"), close(penv));
  }

  @Override public boolean needConfig() {
    return false;
  }
}
