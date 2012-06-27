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

package org.opencastproject.oaipmh.util;

import org.opencastproject.util.data.Function;
import org.osgi.service.component.ComponentContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;
import java.util.Collections;
import java.util.Map;

/**
 * Functions supporting persistence.
 */
public final class PersistenceUtil {

  private PersistenceUtil() {
  }

  /**
   * Create a new entity manager factory with the persistence unit name <code>emName</code>.
   * A {@link PersistenceProvider} named <code>persistence</code> has to be registered as an OSGi service.
   * If you want to configure the factory please also register a map containing all properties under
   * the name <code>persistenceProps</code>. See {@link PersistenceProvider#createEntityManagerFactory(String, Map)}
   * for more information about config maps.
   */
  public static EntityManagerFactory newEntityManagerFactory(ComponentContext cc, String emName) {
    PersistenceProvider persistenceProvider = (PersistenceProvider) cc.locateService("persistence");
    final Map persistenceProps;
    Map pp = (Map) cc.locateService("persistenceProps");
    persistenceProps = pp != null ? pp : Collections.emptyMap();
    return persistenceProvider.createEntityManagerFactory(emName, persistenceProps);
  }

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p/>
   * Please note that calling {@link PersistenceEnv#tx(Function)} always creates a <em>new</em> transaction.
   * Transaction propagation is <em>not</em> supported.
   */
  public static PersistenceEnv newPersistenceEnvironment(final EntityManagerFactory emf) {
    return new PersistenceEnv() {
      @Override
      public <A> A tx(Function<EntityManager, A> transactional) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
          tx.begin();
          A ret = transactional.apply(em);
          tx.commit();
          return ret;
        } catch (RuntimeException e) {
          if (tx.isActive()) {
            tx.rollback();
          }
          // propagate exception
          throw(e);
        } finally {
          em.close();
        }
      }

      @Override
      public void close() {
        emf.close();
      }
    };
  }
}
