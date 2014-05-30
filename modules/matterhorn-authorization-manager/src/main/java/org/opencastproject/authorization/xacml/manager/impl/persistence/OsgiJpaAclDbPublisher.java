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
package org.opencastproject.authorization.xacml.manager.impl.persistence;

import org.opencastproject.authorization.xacml.manager.impl.AclDb;
import org.opencastproject.util.osgi.SimpleServicePublisher;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import javax.persistence.spi.PersistenceProvider;
import java.util.Dictionary;
import java.util.Map;

import static org.opencastproject.util.persistence.PersistenceUtil.newPersistenceEnvironment;

/** Publishes a {@link JpaAclDb}. */
public class OsgiJpaAclDbPublisher extends SimpleServicePublisher {
  /** Persistence properties used to create {@link javax.persistence.EntityManagerFactory} */
  private Map<String, Object> persistenceProperties;

  /** Persistence provider set by OSGi */
  private PersistenceProvider persistenceProvider;

  /** OSGi callback to set persistence properties. */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** OSGi callback to set persistence provider. */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @Override public ServiceReg registerService(Dictionary properties, ComponentContext cc) throws ConfigurationException {
    PersistenceEnv penv = newPersistenceEnvironment(persistenceProvider, "org.opencastproject.authorization.xacml.manager", persistenceProperties);
    final JpaAclDb aclDb = new JpaAclDb(penv);
    return ServiceReg.reg(registerService(cc, aclDb, AclDb.class, "JPA based ACL Provider"), close(penv));
  }

  @Override public boolean needConfig() {
    return false;
  }
}
