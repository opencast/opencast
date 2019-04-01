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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util.persistence

import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.functions.Misc.chuck
import org.opencastproject.util.persistence.PersistenceUtil.createEntityManager
import org.opencastproject.util.persistence.PersistenceUtil.newEntityManagerFactory
import org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.osgi.service.component.ComponentContext

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.spi.PersistenceProvider

/** Persistence environment factory.  */
object PersistenceEnvs {

    private val emStore = object : ThreadLocal<Option<Transactional>>() {
        override fun initialValue(): Option<Transactional> {
            return none()
        }
    }

    private interface Transactional {
        /** Run a function in a transactional context.  */
        fun <A> tx(transactional: Function<EntityManager, A>): A
    }

    /**
     * Create a new, concurrently usable persistence environment which uses JPA local transactions.
     *
     *
     * Transaction propagation is supported on a per thread basis.
     */
    fun persistenceEnvironment(emf: EntityManagerFactory): PersistenceEnv {
        val startTx = object : Transactional {
            override fun <A> tx(transactional: Function<EntityManager, A>): A {
                for (em in createEntityManager(emf)) {
                    val tx = em.transaction
                    try {
                        tx.begin()
                        emStore.set(Option.some(object : Transactional {
                            override fun <A> tx(transactional: Function<EntityManager, A>): A {
                                return transactional.apply(em)
                            }
                        }))
                        val ret = transactional.apply(em)
                        tx.commit()
                        return ret
                    } catch (e: Exception) {
                        if (tx.isActive) {
                            tx.rollback()
                        }
                        // propagate exception
                        return chuck(e)
                    } finally {
                        if (em.isOpen)
                            em.close()
                        emStore.remove()
                    }
                }
                return chuck(IllegalStateException("EntityManager is already closed"))
            }
        }
        return object : PersistenceEnv() {
            internal fun currentTx(): Transactional {
                return emStore.get().getOrElse(startTx)
            }

            override fun <A> tx(transactional: Function<EntityManager, A>): A {
                return currentTx().tx(transactional)
            }

            override fun close() {
                emf.close()
            }
        }
    }

    /**
     * Shortcut for `persistenceEnvironment(newEntityManagerFactory(cc, emName))`.
     *
     * @see PersistenceUtil.newEntityManagerFactory
     */
    fun persistenceEnvironment(cc: ComponentContext, emName: String): PersistenceEnv {
        return persistenceEnvironment(newEntityManagerFactory(cc, emName))
    }

    /**
     * Shortcut for `newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))`.
     *
     * @see PersistenceUtil.newEntityManagerFactory
     */
    fun persistenceEnvironment(cc: ComponentContext, emName: String, persistenceProps: Map<*, *>): PersistenceEnv {
        return persistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))
    }

    /** Create a new persistence environment. This method is the preferred way of creating a persistence environment.  */
    fun persistenceEnvironment(persistenceProvider: PersistenceProvider, emName: String,
                               persistenceProps: Map<*, *>): PersistenceEnv {
        return persistenceEnvironment(persistenceProvider.createEntityManagerFactory(emName, persistenceProps))
    }

    /**
     * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
     * testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     */
    fun testPersistenceEnv(emName: String): PersistenceEnv {
        return persistenceEnvironment(newTestEntityManagerFactory(emName))
    }
}
