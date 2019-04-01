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

import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple

import com.mchange.v2.c3p0.ComboPooledDataSource

import org.osgi.service.component.ComponentContext

import java.beans.PropertyVetoException
import java.sql.Connection
import java.sql.SQLException
import java.util.Collections
import java.util.Date
import java.util.HashMap

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException
import javax.persistence.Query
import javax.persistence.TemporalType
import javax.persistence.spi.PersistenceProvider
import javax.sql.DataSource

/** Functions supporting persistence.  */

/**
 * Functions supporting persistence.
 */
object PersistenceUtil {

    val NO_PERSISTENCE_PROPS = Collections
            .unmodifiableMap(HashMap<String, Any>())

    /**
     * Create a new entity manager factory with the persistence unit name `emName`. A
     * [javax.persistence.spi.PersistenceProvider] named `persistence` has to be registered as an OSGi
     * service. If you want to configure the factory please also register a map containing all properties under the name
     * `persistenceProps`. See
     * [javax.persistence.spi.PersistenceProvider.createEntityManagerFactory] for more
     * information about config maps.
     *
     * @param emName
     * name of the persistence unit
     */
    fun newEntityManagerFactory(cc: ComponentContext, emName: String): EntityManagerFactory {
        val persistenceProvider = cc.locateService("persistence") as PersistenceProvider
        val persistenceProps: Map<*, *>
        val pp = cc.locateService("persistenceProps") as Map<*, *>
        persistenceProps = pp ?: emptyMap<Any, Any>()
        return persistenceProvider.createEntityManagerFactory(emName, persistenceProps)
    }

    /**
     * Create a new entity manager factory with the persistence unit name `emName`. A
     * [javax.persistence.spi.PersistenceProvider] named `persistence` has to be registered as an OSGi
     * service. See [javax.persistence.spi.PersistenceProvider.createEntityManagerFactory]
     * for more information about config maps.
     *
     * @param emName
     * name of the persistence unit
     * @param persistenceProps
     * config map for the creation of an EntityManagerFactory
     */
    fun newEntityManagerFactory(cc: ComponentContext, emName: String, persistenceProps: Map<*, *>): EntityManagerFactory {
        val persistenceProvider = cc.locateService("persistence") as PersistenceProvider
        return persistenceProvider.createEntityManagerFactory(emName, persistenceProps)
    }

    /** Create a new persistence environment. This method is the preferred way of creating a persitence environment.  */
    fun newPersistenceEnvironment(persistenceProvider: PersistenceProvider, emName: String,
                                  persistenceProps: Map<*, *>): PersistenceEnv {
        return newPersistenceEnvironment(persistenceProvider.createEntityManagerFactory(emName, persistenceProps))
    }

    /**
     * Shortcut for `newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))`.
     *
     * @see .newEntityManagerFactory
     */
    fun newPersistenceEnvironment(cc: ComponentContext, emName: String, persistenceProps: Map<*, *>): PersistenceEnv {
        return newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))
    }

    /**
     * Shortcut for `newPersistenceEnvironment(newEntityManagerFactory(cc, emName))`.
     *
     * @see .newEntityManagerFactory
     */
    fun newPersistenceEnvironment(cc: ComponentContext, emName: String): PersistenceEnv {
        return newPersistenceEnvironment(newEntityManagerFactory(cc, emName))
    }

    /** Create a new entity manager or return none, if the factory has already been closed.  */
    fun createEntityManager(emf: EntityManagerFactory): Option<EntityManager> {
        try {
            return some(emf.createEntityManager())
        } catch (ex: IllegalStateException) {
            // factory is already closed
            return none()
        }

    }

    /**
     * Equip a persistence environment with an exception handler.
     */
    fun <F> equip2(penv: PersistenceEnv, exHandler: Function<Exception, F>): PersistenceEnv2<F> {
        return object : PersistenceEnv2<F> {
            override fun <A> tx(transactional: Function<EntityManager, A>): Either<F, A> {
                try {
                    return Either.right(penv.tx(transactional))
                } catch (e: Exception) {
                    return Either.left(exHandler.apply(e))
                }

            }

            override fun close() {
                penv.close()
            }
        }
    }

    /**
     * Create a new, concurrently usable persistence environment which uses JPA local transactions.
     *
     *
     * Transaction propagation is supported on a per thread basis.
     *
     */
    @Deprecated("use {@link PersistenceEnvs#persistenceEnvironment(EntityManagerFactory)}")
    fun newPersistenceEnvironment(emf: EntityManagerFactory): PersistenceEnv {
        return PersistenceEnvs.persistenceEnvironment(emf)
    }

    fun closeQuietly(c: Connection?) {
        if (c != null) {
            try {
                c.close()
            } catch (ignore: SQLException) {
            }

        }
    }

    /**
     * Test if a connection to the given data source can be established.
     *
     * @return none, if the connection could be established
     */
    fun testConnection(ds: DataSource): Option<SQLException> {
        var connection: Connection? = null
        try {
            connection = ds.connection
            return none()
        } catch (e: SQLException) {
            return some(e)
        } finally {
            closeQuietly(connection)
        }
    }

    /**
     * Create a named query with a list of parameters. Values of type [Date] are recognized and set as a timestamp (
     * [TemporalType.TIMESTAMP].
     *
     */
    @Deprecated("use {@link Queries#named} query(EntityManager, String, Class, Object[])")
    fun createNamedQuery(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): Query {
        val q = em.createNamedQuery(queryName)
        for (p in params) {
            val value = p.b
            if (value is Date) {
                q.setParameter(p.a, p.b as Date, TemporalType.TIMESTAMP)
            } else {
                q.setParameter(p.a, p.b)
            }
        }
        return q
    }

    /**
     * Run an update (UPDATE or DELETE) query and ensure that at least one row got affected.
     *
     */
    @Deprecated("use {@link Queries#named} #update(EntityManager, String, Object[])")
    fun runUpdate(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): Boolean {
        return createNamedQuery(em, queryName, *params).executeUpdate() > 0
    }

    /**
     * Run a query (SELECT) that should return a single result.
     *
     */
    @Deprecated("use {@link Queries#named} #findSingle(EntityManager, String, Object[])")
    fun <A> runSingleResultQuery(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): Option<A> {
        try {
            return some(createNamedQuery(em, queryName, *params).singleResult as A)
        } catch (e: NoResultException) {
            return none()
        } catch (e: NonUniqueResultException) {
            return none()
        }

    }

    /**
     * Run a query that should return the first result of it.
     *
     */
    @Deprecated("use {@link Queries#named} findFirst(EntityManager, String, Object[])")
    fun <A> runFirstResultQuery(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): Option<A> {
        try {
            return some(createNamedQuery(em, queryName, *params).setMaxResults(1).singleResult as A)
        } catch (e: NoResultException) {
            return none()
        } catch (e: NonUniqueResultException) {
            return none()
        }

    }

    /**
     * Execute a `COUNT(x)` query.
     *
     */
    @Deprecated("use {@link Queries#named} count(EntityManager, String, Object[])")
    fun runCountQuery(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): Long {
        return (createNamedQuery(em, queryName, *params).singleResult as Number).toLong()
    }


    @Deprecated("use {@link Queries#find(Class, Object)} ")
    fun <A> findById(clazz: Class<A>, primaryKey: Any): Function<EntityManager, Option<A>> {
        return object : Function<EntityManager, Option<A>>() {
            override fun apply(em: EntityManager): Option<A> {
                return option(em.find(clazz, primaryKey))
            }
        }
    }

    /**
     * Find a single object.
     *
     * @param params
     * the query parameters
     * @param toA
     * map to the desired result object
     */
    @Deprecated("")
    fun <A, B> find(em: EntityManager, toA: Function<B, A>, queryName: String,
                    vararg params: Tuple<String, *>): Option<A> {
        return PersistenceUtil.runSingleResultQuery<B>(em, queryName, *params).map(toA)
    }

    /**
     * Find multiple objects.
     *
     */
    @Deprecated("use {@link Queries#named} findAll(EntityManager, String, Object[])")
    fun <A> findAll(em: EntityManager, queryName: String, vararg params: Tuple<String, *>): List<A> {
        return createNamedQuery(em, queryName, *params).resultList
    }

    /**
     * Find multiple objects with optional pagination.
     *
     */
    @Deprecated("use {@link Queries#named} findAll(EntityManager, String, Option, Option, Object[])")
    fun <A> findAll(em: EntityManager, queryName: String, offset: Option<Int>,
                    limit: Option<Int>, vararg params: Tuple<String, *>): List<A> {
        val q = createNamedQuery(em, queryName, *params)
        for (x in offset)
            q.firstResult = x!!
        for (x in limit)
            q.maxResults = x!!
        return q.resultList
    }

    /**
     * Find multiple objects.
     *
     * @param params
     * the query parameters
     * @param toA
     * map to the desired result object
     */
    @Deprecated("use {@link Queries#named} findAll(EntityManager, String, Object[]) instead")
    fun <A, B> findAll(em: EntityManager, toA: Function<B, A>, queryName: String,
                       vararg params: Tuple<String, *>): List<A> {
        return mlist(createNamedQuery(em, queryName, *params).resultList as List<B>).map(toA).value()
    }

    /**
     * Find multiple objects with optional pagination.
     *
     * @param params
     * the query parameters
     * @param toA
     * map to the desired result object
     */
    @Deprecated("use {@link Queries#named} findAll(EntityManager, String, Option, Option, Object[]) instead")
    fun <A, B> findAll(em: EntityManager, toA: Function<B, A>, offset: Option<Int>,
                       limit: Option<Int>, queryName: String, vararg params: Tuple<String, *>): List<A> {
        val q = createNamedQuery(em, queryName, *params)
        for (x in offset)
            q.firstResult = x!!
        for (x in limit)
            q.maxResults = x!!
        return mlist(q.resultList as List<B>).map(toA).value()
    }

    /**
     * Create function to persist object `a` using [EntityManager.persist].
     *
     */
    @Deprecated("use {@link Queries#persist(Object)}")
    fun <A> persist(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                em.persist(a)
                return a
            }
        }
    }

    /**
     * Create function to merge an object `a` with the persisten context of the given entity manage.
     *
     */
    @Deprecated("use {@link Queries#merge(Object)}")
    fun <A> merge(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                em.merge(a)
                return a
            }
        }
    }

    fun newEntityManagerFactory(emName: String, vendor: String, driver: String, url: String,
                                user: String, pwd: String, persistenceProps: Map<String, *>, pp: PersistenceProvider): EntityManagerFactory {
        // Set up the database
        val pooledDataSource = ComboPooledDataSource()
        try {
            pooledDataSource.driverClass = driver
        } catch (e: PropertyVetoException) {
            throw RuntimeException(e)
        }

        pooledDataSource.jdbcUrl = url
        pooledDataSource.user = user
        pooledDataSource.password = pwd

        // Set up the persistence properties
        val props = HashMap<String, Any>(persistenceProps)
        props["javax.persistence.nonJtaDataSource"] = pooledDataSource
        props["eclipselink.target-database"] = vendor

        return pp.createEntityManagerFactory(emName, props)
                ?: throw Error("Cannot create entity manager factory for persistence unit " + emName
                        + ". Maybe you misspelled the name of the persistence unit?")
    }

    /**
     * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     */
    fun newTestEntityManagerFactory(emName: String): EntityManagerFactory {
        val persistenceProperties = HashMap<String, String>()
        persistenceProperties["eclipselink.ddl-generation"] = "create-tables"
        persistenceProperties["eclipselink.ddl-generation.output-mode"] = "database"
        return newEntityManagerFactory(emName, "Auto", "org.h2.Driver", "jdbc:h2:./target/db" + System.currentTimeMillis(),
                "sa", "sa", persistenceProperties, testPersistenceProvider())
    }

    /** Create a new persistence provider for unit tests.  */
    fun testPersistenceProvider(): PersistenceProvider {
        return org.eclipse.persistence.jpa.PersistenceProvider()
    }

    /**
     * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
     * testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     */
    @Deprecated("use {@link PersistenceEnvs#testPersistenceEnv(String)}")
    fun newTestPersistenceEnv(emName: String): PersistenceEnv {
        return newPersistenceEnvironment(newTestEntityManagerFactory(emName))
    }
}
