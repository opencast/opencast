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
import static org.opencastproject.util.persistencefn.PersistenceUtil.mkEntityManagerFactory;
import static org.opencastproject.util.persistencefn.PersistenceUtil.mkTestEntityManagerFactory;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.osgi.service.component.ComponentContext;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

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
                return transactional.ap(em);
              }
            }));
            A ret = transactional.ap(em);
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
        return emStore.get().or(startTx);
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
   * Shortcut for <code>persistenceEnvironment(newEntityManagerFactory(cc, emName))</code>.
   *
   * @see org.opencastproject.util.persistence.PersistenceUtil#newEntityManagerFactory(org.osgi.service.component.ComponentContext, String)
   */
  public static PersistenceEnv mk(ComponentContext cc, String emName) {
    return mk(mkEntityManagerFactory(cc, emName));
  }

  /**
   * Shortcut for <code>newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))</code>.
   *
   * @see org.opencastproject.util.persistence.PersistenceUtil#newEntityManagerFactory(org.osgi.service.component.ComponentContext, String, java.util.Map)
   */
  public static PersistenceEnv mk(ComponentContext cc, String emName, Map persistenceProps) {
    return mk(mkEntityManagerFactory(cc, emName, persistenceProps));
  }

  /** Create a new persistence environment. This method is the preferred way of creating a persistence environment. */
  public static PersistenceEnv mk(PersistenceProvider persistenceProvider, String emName, Map persistenceProps) {
    return mk(persistenceProvider.createEntityManagerFactory(emName, persistenceProps));
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
   * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
   * testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   * @param withSqlLogging
   *         turn on EclipseLink SQL logging
   */
  public static PersistenceEnv mkTestEnv(String emName, boolean withSqlLogging) {
    return mk(mkTestEntityManagerFactory(emName, withSqlLogging));
  }
}
