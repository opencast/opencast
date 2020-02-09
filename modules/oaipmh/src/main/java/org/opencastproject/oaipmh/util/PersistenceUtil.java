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


package org.opencastproject.oaipmh.util;

import org.opencastproject.util.data.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * Functions supporting persistence.
 */
public final class PersistenceUtil {

  private PersistenceUtil() {
  }

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p>
   * Please note that calling {@link PersistenceEnv#tx(Function)} always creates a <em>new</em> transaction. Transaction
   * propagation is <em>not</em> supported.
   */
  public static PersistenceEnv newPersistenceEnvironment(final EntityManagerFactory emf) {
    return new PersistenceEnv() {
      @Override
      public <A> A tx(Function<EntityManager, A> transactional) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
          em = emf.createEntityManager();
          tx = em.getTransaction();
          tx.begin();
          A ret = transactional.apply(em);
          tx.commit();
          return ret;
        } catch (RuntimeException e) {
          if (tx.isActive()) {
            tx.rollback();
          }
          // propagate exception
          throw (e);
        } finally {
          if (em != null)
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
