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

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

/**
 * Definition of a contract for the use of a {@link PersistenceEnv} in an OSGi environment.
 * <p/>
 * Use in conjunction with {@link PersistenceEnvBuilder}.
 */
public interface OsgiPersistenceEnvUser {
  /** OSGi callback to set persistence properties. */
  void setPersistenceProperties(Map<String, Object> persistenceProperties);

  /** OSGi callback to set persistence provider. */
  void setPersistenceProvider(PersistenceProvider persistenceProvider);

  /**
   * Return the persistence environment.
   * <p/>
   * Create the persistence environment in the activate method like so:
   * <pre>
   *   penv = PersistenceEnvs.persistenceEnvironment(persistenceProvider, "my.persistence.context", persistenceProperties);
   * </pre>
   * Or better use the {@link PersistenceEnvBuilder}.
   */
  PersistenceEnv getPenv();

  /** Close the persistence environment {@link PersistenceEnv#close()}. Call from the deactivate method. */
  void closePenv();
}
