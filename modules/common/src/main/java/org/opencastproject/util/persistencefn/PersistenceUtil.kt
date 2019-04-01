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

import com.entwinemedia.fn.Equality.eq
import com.entwinemedia.fn.Stream.`$`
import java.lang.String.format

import org.opencastproject.util.data.Collections

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.ProductBuilder
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Products
import com.mchange.v2.c3p0.ComboPooledDataSource

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.beans.PropertyVetoException
import java.net.URI
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.util.HashMap

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.spi.PersistenceProvider
import javax.sql.DataSource

/**
 * Functions supporting persistence.
 */
object PersistenceUtil {
    private val logger = LoggerFactory.getLogger(PersistenceUtil::class.java!!)

    val NO_PERSISTENCE_PROPS = emptyMap<String, Any>()

    /* ------------------------------------------------------------------------------------------------------------------ */

    //
    // test entity manager
    //

    private val P = com.entwinemedia.fn.Products.E
    private val TEST_ENTITY_MANAGER_PROPS = `$`<P2<String, String>>(
            P.p2("eclipselink.ddl-generation", "create-tables"),
            P.p2("eclipselink.ddl-generation.output-mode", "database"))
            .group(Products.p2_1<String>(), Products.p2_2<String>())
    private val TEST_ENTITY_MANAGER_LOGGING_PROPS = `$`<P2<String, String>>(
            P.p2("eclipselink.logging.level.sql", "FINE"),
            P.p2("eclipselink.logging.parameters", "true"))
            .group(Products.p2_1<String>(), Products.p2_2<String>())

    /** [.getDatabaseMetadata] as a function.  */
    val getDatabaseMetadata: Fn<EntityManager, Opt<DatabaseMetaData>> = object : Fn<EntityManager, Opt<DatabaseMetaData>>() {
        override fun apply(em: EntityManager): Opt<DatabaseMetaData> {
            return getDatabaseMetadata(em)
        }
    }

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
    fun mkEntityManagerFactory(cc: ComponentContext, emName: String): EntityManagerFactory {
        val persistenceProvider = cc.locateService("persistence") as PersistenceProvider
        val pp = cc.locateService("persistenceProps") as Map<*, *>
        val persistenceProps = pp ?: NO_PERSISTENCE_PROPS
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
    fun mkEntityManagerFactory(cc: ComponentContext, emName: String, persistenceProps: Map<*, *>): EntityManagerFactory {
        val persistenceProvider = cc.locateService("persistence") as PersistenceProvider
        return persistenceProvider.createEntityManagerFactory(emName, persistenceProps)
    }

    fun mkEntityManagerFactory(
            emName: String,
            vendor: String,
            driver: String,
            url: String,
            user: String,
            pwd: String,
            persistenceProps: Map<String, *>,
            pp: PersistenceProvider): EntityManagerFactory {
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

    /** Create a new entity manager or return none, if the factory has already been closed.  */
    fun mkEntityManager(emf: EntityManagerFactory): Opt<EntityManager> {
        try {
            return Opt.some(emf.createEntityManager())
        } catch (ex: IllegalStateException) {
            // factory is already closed
            return Opt.none()
        }

    }

    /**
     * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     * @param withSqlLogging
     * turn on EclipseLink SQL logging
     */
    @JvmOverloads
    fun mkTestEntityManagerFactory(emName: String, withSqlLogging: Boolean = false): EntityManagerFactory {
        return mkEntityManagerFactory(
                emName,
                "Auto",
                "org.h2.Driver",
                "jdbc:h2:./target/db" + System.currentTimeMillis(),
                "sa",
                "sa",
                if (withSqlLogging)
                    Collections.merge(TEST_ENTITY_MANAGER_PROPS, TEST_ENTITY_MANAGER_LOGGING_PROPS)
                else
                    TEST_ENTITY_MANAGER_PROPS,
                mkTestPersistenceProvider())
    }

    /**
     * Create an entity manager for unit tests configured by the following system properties.
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
    fun mkTestEntityManagerFactoryFromSystemProperties(emName: String): EntityManagerFactory {
        val dbUrl = Opt.nul(System.getProperty("test-database-url"))
        val withSqlLogging = java.lang.Boolean.getBoolean("sql-logging")
        val keepDatabase = java.lang.Boolean.getBoolean("keep-database")
        val dbUser = Opt.nul(System.getProperty("test-database-user")).getOr("matterhorn")
        val dbPwd = Opt.nul(System.getProperty("test-database-password")).getOr("matterhorn")
        for (url in dbUrl) {
            try {
                val dbType = URI(URI(url).schemeSpecificPart).scheme
                if (eq("mysql", dbType)) {
                    logger.info(format("Use MySQL database\n"
                            + " test-database-url=%s\n"
                            + " test-database-user=%s\n"
                            + " test-database-password=%s\n"
                            + " sql-logging=%s\n"
                            + " keep-database=%s",
                            url, dbUser, dbPwd, withSqlLogging, keepDatabase))
                    return mkMySqlTestEntityManagerFactory(emName, url, dbUser, dbPwd, withSqlLogging, keepDatabase)
                }
            } catch (ignore: Exception) {
            }

        }
        logger.info(format("Use H2 database\n sql-logging=%s", withSqlLogging))
        return mkTestEntityManagerFactory(emName, withSqlLogging)
    }

    /**
     * Create a new entity manager factory using a MySQL database for testing purposes.
     *
     * @param emName
     * name of the persistence unit (see META-INF/persistence.xml)
     * @param jdbcUri
     * the JDBC URI of the database, e.g. jdbc:mysql://localhost/test_database
     * @param user
     * the database user
     * @param pwd
     * the user's password
     * @param withSqlLogging
     * turn on SQL logging
     * @param keepDatabase
     * recreate or keep the database at startup
     */
    fun mkMySqlTestEntityManagerFactory(
            emName: String,
            jdbcUri: String,
            user: String,
            pwd: String,
            withSqlLogging: Boolean,
            keepDatabase: Boolean): EntityManagerFactory {
        val testEntityManagerProps = HashMap<String, String>()
        testEntityManagerProps["eclipselink.ddl-generation"] = if (keepDatabase) "none" else "drop-and-create-tables"
        testEntityManagerProps["eclipselink.ddl-generation.output-mode"] = "database"
        if (withSqlLogging) {
            testEntityManagerProps.putAll(PersistenceUtil.TEST_ENTITY_MANAGER_LOGGING_PROPS)
        }
        return PersistenceUtil.mkEntityManagerFactory(
                emName,
                "MySql",
                "com.mysql.jdbc.Driver",
                jdbcUri,
                user,
                pwd,
                testEntityManagerProps,
                PersistenceUtil.mkTestPersistenceProvider())
    }

    //
    //
    //

    /** Create a new persistence provider for unit tests.  */
    fun mkTestPersistenceProvider(): PersistenceProvider {
        return org.eclipse.persistence.jpa.PersistenceProvider()
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
    fun testConnection(ds: DataSource): Opt<SQLException> {
        try {
            ds.connection.use { tryConnect -> return Opt.none() }
        } catch (e: SQLException) {
            return Opt.some(e)
        }

    }

    /**
     * Try to get the database metadata from an entity manager. Return none, if the metadata
     * cannot be retrieved, e.g. because the entity manager does not expose them.
     */
    fun getDatabaseMetadata(em: EntityManager): Opt<DatabaseMetaData> {
        try {
            val c = em.unwrap<Connection>(Connection::class.java)
            return Opt.some(c.metaData)
        } catch (e: Exception) {
            return Opt.none()
        }

    }

    /**
     * Get the database vendor from some metadata.
     *
     * @return [DatabaseVendor.UNKNOWN] in case the database is not known or an arbitrary error occurs
     */
    fun getVendor(m: DatabaseMetaData): DatabaseVendor {
        try {
            when (m.databaseProductName) {
                "H2" -> return DatabaseVendor.H2
                "MySQL" -> return DatabaseVendor.MYSQL
                "Postgres" -> return DatabaseVendor.POSTGRES
                else -> return DatabaseVendor.UNKNOWN
            }
        } catch (e: SQLException) {
            return DatabaseVendor.UNKNOWN
        }

    }

    enum class DatabaseVendor {
        H2, MYSQL, POSTGRES, UNKNOWN
    }
}
/**
 * Create a new entity manager factory backed by an in-memory H2 database for testing purposes.
 * If you want to turn on SQL logging see [.mkTestEntityManagerFactory]
 *
 * @param emName
 * name of the persistence unit (see META-INF/persistence.xml)
 */
