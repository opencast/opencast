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

package org.opencastproject.util.persistencefn;

import static com.entwinemedia.fn.Prelude.chuck;
import static org.opencastproject.util.persistencefn.PersistenceUtil.mkEntityManager;
import static org.opencastproject.util.persistencefn.PersistenceUtil.mkTestEntityManagerFactory;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * Persistence environment factory.
 */
public final class PersistenceEnvs {

  private PersistenceEnvs() {
  }

  private interface Transactional {
    /** Run a function in a transactional context. */
    <A> A tx(Fn<EntityManager, A> transactional);
  }

  private static final ThreadLocal<Opt<Transactional>> emStore = new ThreadLocal<Opt<Transactional>>() {
    @Override protected Opt<Transactional> initialValue() {
      return Opt.none();
    }
  };

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p>
   * Transaction propagation is supported on a per thread basis.
   */
  public static PersistenceEnv mk(final EntityManagerFactory emf) {
    final Transactional startTx = new Transactional() {
      @Override
      public <A> A tx(Fn<EntityManager, A> transactional) {
        for (final EntityManager em : mkEntityManager(emf)) {
          final EntityTransaction tx = em.getTransaction();
          try {
            tx.begin();
            emStore.set(Opt.<Transactional>some(new Transactional() {
              @Override public <A> A tx(Fn<EntityManager, A> transactional) {
                return transactional.apply(em);
              }
            }));
            A ret = transactional.apply(em);
            tx.commit();
            return ret;
          } catch (Exception e) {
            if (tx.isActive()) {
              tx.rollback();
            }
            // propagate exception
            return chuck(e);
          } finally {
            if (em.isOpen())
              em.close();
            emStore.remove();
          }
        }
        return chuck(new IllegalStateException("EntityManager is already closed"));
      }
    };
    return new PersistenceEnv() {
      Transactional currentTx() {
        return emStore.get().getOr(startTx);
      }

      @Override public <A> A tx(Fn<EntityManager, A> transactional) {
        return currentTx().tx(transactional);
      }

      @Override public void close() {
        emf.close();
      }
    };
  }

  /**
   * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
   * testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static PersistenceEnv mkTestEnv(String emName) {
    return mk(mkTestEntityManagerFactory(emName));
  }

  /**
   * Create a persistence environment for unit tests configured by the following system properties.
   * <ul>
   * <li>-Dtest-database-url, JDBC URL, defaults to H2 in-memory database</li>
   * <li>-Dtest-database-user, defaults to 'matterhorn'</li>
   * <li>-Dtest-database-password, defaults to 'matterhorn'</li>
   * <li>-Dsql-logging=[true|false], defaults to 'false', turns on SQL logging to the console</li>
   * <li>-Dkeep-database=[true|false], defaults to 'false', keep an existing database or recreate at startup. Not used with H2.</li>
   * </ul>
   * Currently only MySQL is recognized.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static PersistenceEnv mkTestEnvFromSystemProperties(String emName) {
    return mk(PersistenceUtil.mkTestEntityManagerFactoryFromSystemProperties(emName));
  }
}
