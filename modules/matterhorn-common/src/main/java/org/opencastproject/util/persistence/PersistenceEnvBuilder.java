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
package org.opencastproject.util.persistence;

import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Lazy;

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

import static org.opencastproject.util.data.Collections.map;

/**
 * Builder for persistence environments.
 * Useful in OSGi bound services where required properties are injected by the OSGi environment.
 */
public final class PersistenceEnvBuilder {
  private Map<String, Object> persistenceProperties = map();
  private PersistenceProvider persistenceProvider;
  private String persistenceUnit;
  private Lazy<PersistenceEnv> penv = Lazy.lazy(new Function0<PersistenceEnv>() {
    @Override public PersistenceEnv apply() {
      if (persistenceProvider == null) {
        throw new IllegalStateException("Persistence provider has not been set yet");
      }
      if (persistenceUnit == null) {
        throw new IllegalStateException("Persistence unit has not been set yet");
      }
      return PersistenceEnvs.persistenceEnvironment(persistenceProvider, persistenceUnit, persistenceProperties);
    }
  });

  public PersistenceEnvBuilder() {
  }

  public PersistenceEnvBuilder(String persistenceUnit) {
    this.persistenceUnit = persistenceUnit;
  }

  /** Set the mandatory name of the persistence unit. */
  public void setPersistenceUnit(String name) {
    this.persistenceUnit = name;
  }

  /** Set the optional persistence properties. */
  public void setPersistenceProperties(Map<String, Object> properties) {
    this.persistenceProperties = properties;
  }

  /** Set the mandatory persistence provider. */
  public void setPersistenceProvider(PersistenceProvider provider) {
    this.persistenceProvider = provider;
  }

  /** Builds the persistence env. Always returns the same environment so it may be safely called multiple times. */
  public PersistenceEnv buildOrGet() {
    return penv.value();
  }
}
