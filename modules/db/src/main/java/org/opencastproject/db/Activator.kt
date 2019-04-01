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

package org.opencastproject.db

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.mchange.v2.c3p0.DataSources

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.util.Hashtable

import javax.sql.DataSource

/** Registers [DataSource].  */
class Activator : BundleActivator {

    private var rootDir: String? = null
    private var datasourceRegistration: ServiceRegistration<*>? = null
    private var pooledDataSource: ComboPooledDataSource? = null

    @Throws(Exception::class)
    override fun start(bundleContext: BundleContext) {
        // Use the configured storage directory
        rootDir = bundleContext.getProperty("org.opencastproject.storage.dir") + File.separator + "db"

        // Register the data source, defaulting to an embedded H2 database if DB configurations are not specified
        val jdbcDriver = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.driver"),
                "org.h2.Driver")
        val jdbcUrl = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.url"),
                "jdbc:h2:" + rootDir!!)
        val jdbcUser = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.user"), "sa")
        val jdbcPass = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pass"), "sa")

        val maxPoolSize = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.size"))
        val minPoolSize = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pool.min.size"))
        val acquireIncrement = getConfigProperty(
                bundleContext.getProperty("org.opencastproject.db.jdbc.pool.acquire.increment"))
        val maxStatements = getConfigProperty(
                bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.statements"))
        val loginTimeout = getConfigProperty(
                bundleContext.getProperty("org.opencastproject.db.jdbc.pool.login.timeout"))
        val maxIdleTime = getConfigProperty(
                bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.idle.time"))
        val maxConnectionAge = getConfigProperty(
                bundleContext.getProperty("org.opencastproject.db.jdbc.pool.max.connection.age"))

        pooledDataSource = ComboPooledDataSource()
        pooledDataSource!!.driverClass = jdbcDriver
        pooledDataSource!!.jdbcUrl = jdbcUrl
        pooledDataSource!!.user = jdbcUser
        pooledDataSource!!.password = jdbcPass
        if (minPoolSize != null)
            pooledDataSource!!.minPoolSize = minPoolSize
        if (maxPoolSize != null)
            pooledDataSource!!.maxPoolSize = maxPoolSize
        if (acquireIncrement != null)
            pooledDataSource!!.acquireIncrement = acquireIncrement
        if (maxStatements != null)
            pooledDataSource!!.maxStatements = maxStatements
        if (loginTimeout != null)
            pooledDataSource!!.loginTimeout = loginTimeout

        // maxIdleTime should not be zero, otherwise the connection pool will hold on to stale connections
        // that have been closed by the database.
        if (maxIdleTime != null)
            pooledDataSource!!.maxIdleTime = maxIdleTime
        else if (pooledDataSource!!.maxIdleTime == 0) {
            logger.debug("Setting database connection pool max.idle.time to default of {}", DEFAULT_MAX_IDLE_TIME)
            pooledDataSource!!.maxIdleTime = DEFAULT_MAX_IDLE_TIME
        }

        if (maxConnectionAge != null)
            pooledDataSource!!.maxConnectionAge = maxConnectionAge

        var connection: Connection? = null
        try {
            logger.info("Testing connectivity to database at {}", jdbcUrl)
            connection = pooledDataSource!!.connection
            val dsProps = Hashtable<String, String>()
            dsProps["osgi.jndi.service.name"] = "jdbc/opencast"
            datasourceRegistration = bundleContext.registerService(DataSource::class.java.name, pooledDataSource, dsProps)
        } catch (e: SQLException) {
            logger.error("Connection attempt to {} failed", jdbcUrl)
            logger.error("Exception: ", e)
            throw e
        } finally {
            connection?.close()
        }

        logger.info("Database connection pool established at {}", jdbcUrl)
        logger.info("Database connection pool parameters: max.size={}, min.size={}, max.idle.time={}",
                pooledDataSource!!.maxPoolSize, pooledDataSource!!.minPoolSize, pooledDataSource!!.maxIdleTime)
    }

    @Throws(Exception::class)
    override fun stop(context: BundleContext) {
        logger.info("Shutting down database")
        if (datasourceRegistration != null)
            datasourceRegistration!!.unregister()
        logger.info("Shutting down connection pool")
        DataSources.destroy(pooledDataSource)
    }

    private fun getConfigProperty(config: String?, defaultValue: String): String {
        return config ?: defaultValue
    }

    private fun getConfigProperty(config: String?): Int? {
        return if (config == null) null else Integer.parseInt(config)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(Activator::class.java)

        /** The default max idle time for the connection pool  */
        private val DEFAULT_MAX_IDLE_TIME = 3600
    }

}
