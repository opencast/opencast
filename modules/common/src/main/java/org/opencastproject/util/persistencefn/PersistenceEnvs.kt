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

package org.opencastproject.util.persistencefn

import com.entwinemedia.fn.Prelude.chuck
import org.opencastproject.util.persistencefn.PersistenceUtil.mkEntityManager
import org.opencastproject.util.persistencefn.PersistenceUtil.mkEntityManagerFactory
import org.opencastproject.util.persistencefn.PersistenceUtil.mkTestEntityManagerFactory

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.spi.PersistenceProvider

/**
 * Persistence environment factory.
 */
object PersistenceEnvs {
    private val logger = LoggerFactory.getLogger(PersistenceEnvs::class.java!!)

    private val emStore = object : ThreadLocal<Opt<Transactional>>() {
        override fun initialValue(): Opt<Transactional> {
            return Opt.none()
        }
    }

    private interface Transactional {
        /** Run a function in a transactional context.  */
        fun <A> tx(transactional: Fn<EntityManager, A>): A
    }

    /**
     * Create a new, concurrently usable persistence environment which uses JPA local transactions.
     *
     *
     * Transaction propagation is supported on a per thread basis.
     */
    fun mk(emf: EntityManagerFactory): PersistenceEnv {
        val startTx = object : Transactional {
            override fun <A> tx(transactional: Fn<EntityManager, A>): A {
                for (em in mkEntityManager(emf)) {
                    val tx = em.transaction
                    try {
                        tx.begin()
                        emStore.set(Opt.some(object : Transactional {
                            override fun <A> tx(transactional: Fn<EntityManager, A>): A {
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
                return emStore.get().getOr(startTx)
            }

            override fun <A> tx(transactional: Fn<EntityManager, A>): A {
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
     * @see org.opencastproject.util.persistence.PersistenceUtil.newEntityManagerFactory
     */
    fun mk(cc: ComponentContext, emName: String): PersistenceEnv {
        return mk(mkEntityManagerFactory(cc, emName))
    }

    /**
     * Shortcut for `newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))`.
     *
     * @see org.opencastproject.util.persistence.PersistenceUtil.newEntityManagerFactory
     */
    fun mk(cc: ComponentContext, emName: String, persistenceProps: Map<*, *>): PersistenceEnv {
        return mk(mkEntityManagerFactory(cc, emName, persistenceProps))
    }

    /** Create a new persistence environment. This method is the preferred way of creating a persistence environment.  */
    fun mk(persistenceProvider: PersistenceProvider, emName: String, persistenceProps: Map<*, *>): PersistenceEnv {
        return mk(persistenceProvider.createEntityManagerFactory(emName, persistenceProps))
    }

    /**
     * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
     * testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     */
    fun mkTestEnv(emName: String): PersistenceEnv {
        return mk(mkTestEntityManagerFactory(emName))
    }

    /**
     * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
     * testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     * @param withSqlLogging
     * turn on EclipseLink SQL logging
     */
    fun mkTestEnv(emName: String, withSqlLogging: Boolean): PersistenceEnv {
        return mk(mkTestEntityManagerFactory(emName, withSqlLogging))
    }

    /**
     * Create a persistence environment for unit tests configured by the following system properties.
     *
     *  * -Dtest-database-url, JDBC URL, defaults to H2 in-memory database
     *  * -Dtest-database-user, defaults to 'matterhorn'
     *  * -Dtest-database-password, defaults to 'matterhorn'
     *  * -Dsql-logging=[true|false], defaults to 'false', turns on SQL logging to the console
     *  * -Dkeep-database=[true|false], defaults to 'false', keep an existing database or recreate at startup. Not used with H2.
     *
     * Currently only MySQL is recognized.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     */
    fun mkTestEnvFromSystemProperties(emName: String): PersistenceEnv {
        return mk(PersistenceUtil.mkTestEntityManagerFactoryFromSystemProperties(emName))
    }
}
