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
package org.opencastproject.migration

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.api.fn.Properties
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.ARecord
import org.opencastproject.assetmanager.api.query.Predicate
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.OsgiUtil

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.google.gson.Gson

import org.osgi.framework.ServiceReference
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.Date
import java.util.Dictionary

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction

/**
 * This class provides index and DB migrations to Opencast.
 */
class SchedulerMigrationService {

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The asset manager  */
    private var assetManager: AssetManager? = null

    private var orgDirectoryService: OrganizationDirectoryService? = null

    private var emf: EntityManagerFactory? = null

    private val gson = Gson()

    private// query filter for organization could be helpful to split up big migrations
    // select necessary properties when assembling query
    val scheduledEvents: Stream<ARecord>
        get() {
            val query = assetManager!!.createQuery()
            val predicate = withOrganization(query).and(withVersion(query)).and(withOwner(query))
                    .and(withProperties(query))
            return query.select(query.propertiesOf(SCHEDULER_NAMESPACE, WORKFLOW_NAMESPACE, CA_NAMESPACE))
                    .where(predicate).run().records
        }

    /** OSGi DI callback.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    fun setOrgDirectoryService(orgDirectoryService: OrganizationDirectoryService) {
        this.orgDirectoryService = orgDirectoryService
    }

    fun setEntityManagerFactory(emf: EntityManagerFactory) {
        this.emf = emf
    }

    @Throws(IOException::class)
    fun activate(cc: ComponentContext) {
        val ocVersion = cc.bundleContext.bundle.version.major
        if (ocVersion > 7) {
            logger.info("Scheduler migration can only be run when upgrading from opencast 6.x to 7.x. Skipping.")
            return
        }
        val svcReference = cc.bundleContext.getServiceReference(ConfigurationAdmin::class.java)
        val props = cc.bundleContext.getService(svcReference)
                .getConfiguration("org.opencastproject.scheduler.impl.SchedulerServiceImpl").properties
        val maintenance = props != null && OsgiUtil.getOptCfgAsBoolean(props, "maintenance").getOrElse(false)
        if (!maintenance) {
            logger.info("Scheduler is not in maintenance mode. Skipping migration.")
            return
        }
        logger.info("Start migrating scheduled events")
        val systemUserName = SecurityUtil.getSystemUserName(cc)
        for (org in orgDirectoryService!!.organizations) {
            SecurityUtil.runAs(securityService!!, org, SecurityUtil.createSystemUser(systemUserName, org), {
                try {
                    migrateScheduledEvents()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            })
        }
        logger.info("Finished migrating scheduled events. You can now disable maintenance mode of scheduler and restart opencast.")
    }

    @Throws(Exception::class)
    private fun migrateScheduledEvents() {
        // migrate all events for current organization
        val org = securityService!!.organization.id
        logger.info("Migrating scheduled events for organization {}", org)
        val allEvents = scheduledEvents
        var count = 0
        for (record in allEvents) {
            migrateProperties(record)
            count++
        }
        logger.info("Migrated {} scheduled events for organization {}.", count, org)
    }

    private fun withOrganization(query: AQueryBuilder): Predicate {
        return query.organizationId().eq(securityService!!.organization.id)
    }

    private fun withOwner(query: AQueryBuilder): Predicate {
        return query.owner().eq(SNAPSHOT_OWNER)
    }

    private fun withVersion(query: AQueryBuilder): Predicate {
        return query.version().isLatest
    }

    private fun withProperties(query: AQueryBuilder): Predicate {
        return query.hasPropertiesOf(SCHEDULER_NAMESPACE)
    }

    private fun getExtendedEventDto(id: String, orgId: String, em: EntityManager): Opt<ExtendedEventDto> {
        return Opt.nul(em.find(ExtendedEventDto::class.java, EventIdPK(id, orgId)))
    }

    @Throws(Exception::class)
    private fun migrateProperties(event: ARecord) {
        val orgID = securityService!!.organization.id
        var em: EntityManager? = null
        var tx: EntityTransaction? = null
        try {
            em = emf!!.createEntityManager()
            tx = em!!.transaction
            tx!!.begin()
            val entityOpt = getExtendedEventDto(event.mediaPackageId, orgID, em)
            if (entityOpt.isSome) {
                logger.warn("Migration for event {} of organization {} seems to be done already. Migrating again.",
                        event.mediaPackageId, orgID)
            }
            // Store all properties in extended events database
            val entity = entityOpt.getOr(ExtendedEventDto())
            entity.mediaPackageId = event.mediaPackageId
            entity.organization = orgID
            val agent = event.properties.apply(Properties.getStringOpt(AGENT_CONFIG))
            if (agent.isSome) {
                entity.captureAgentId = agent.get()
            }
            val checksum = event.properties.apply(Properties.getStringOpt(CHECKSUM))
            if (checksum.isSome) {
                entity.checksum = checksum.get()
            }
            val endDate = event.properties.apply(Properties.getDateOpt(END_DATE_CONFIG))
            if (endDate.isSome) {
                entity.endDate = endDate.get()
            }
            val lastModifiedDate = event.properties.apply(Properties.getDateOpt(LAST_MODIFIED_DATE))
            if (lastModifiedDate.isSome) {
                entity.lastModifiedDate = lastModifiedDate.get()
            }
            val presenters = event.properties.apply(Properties.getStringOpt(PRESENTERS_CONFIG))
            if (presenters.isSome) {
                entity.presenters = presenters.get()
            }
            val recLastHeard = event.properties.apply(Properties.getLongOpt(RECORDING_LAST_HEARD_CONFIG))
            if (recLastHeard.isSome) {
                entity.recordingLastHeard = recLastHeard.get()
            }
            val recState = event.properties.apply(Properties.getStringOpt(RECORDING_STATE_CONFIG))
            if (recState.isSome) {
                entity.recordingState = recState.get()
            }
            val source = event.properties.apply(Properties.getStringOpt(SOURCE_CONFIG))
            if (source.isSome) {
                entity.source = source.get()
            }
            val startDate = event.properties.apply(Properties.getDateOpt(START_DATE_CONFIG))
            if (startDate.isSome) {
                entity.startDate = startDate.get()
            }
            entity.captureAgentProperties = gson.toJson(event.properties.filter(Properties.byNamespace(CA_NAMESPACE))
                    .group(toKey, toValue))
            entity.workflowProperties = gson.toJson(event.properties.filter(Properties.byNamespace(WORKFLOW_NAMESPACE))
                    .group(toKey, toValue))
            if (entityOpt.isSome) {
                em.merge(entity)
            } else {
                em.persist(entity)
            }
            tx.commit()
            try {
                // Remove obsolete asset manager properties
                val query = assetManager!!.createQuery()
                val deleted = query.delete(SNAPSHOT_OWNER, query.propertiesOf(SCHEDULER_NAMESPACE, CA_NAMESPACE, WORKFLOW_NAMESPACE))
                        .where(query.mediaPackageId(event.mediaPackageId).and(withOrganization(query))).run()
                logger.debug("Deleted {} migrated properties", deleted)
            } catch (e: Exception) {
                logger.error("Could not delete obsolete properties for event {}", event.mediaPackageId)
            }

        } catch (e: Exception) {
            logger.error("Could not store extended event: ", e)
            tx?.rollback()
            throw e
        } finally {
            em?.close()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SchedulerMigrationService::class.java)

        /** JPA persistence unit name  */
        val PERSISTENCE_UNIT = "org.opencastproject.migration"

        /** Configuration keys  */
        internal val SCHEDULER_NAMESPACE = "org.opencastproject.scheduler"
        internal val SNAPSHOT_OWNER = SCHEDULER_NAMESPACE
        internal val WORKFLOW_NAMESPACE = "$SCHEDULER_NAMESPACE.workflow.configuration"
        internal val CA_NAMESPACE = "$SCHEDULER_NAMESPACE.ca.configuration"
        internal val RECORDING_LAST_HEARD_CONFIG = "recording_last_heard"
        internal val RECORDING_STATE_CONFIG = "recording_state"
        internal val SOURCE_CONFIG = "source"
        internal val PRESENTERS_CONFIG = "presenters"
        internal val AGENT_CONFIG = "agent"
        internal val START_DATE_CONFIG = "start"
        internal val END_DATE_CONFIG = "end"
        internal val OPTOUT_CONFIG = "optout"
        internal val VERSION = "version"
        internal val LAST_MODIFIED_DATE = "last_modified_date"
        internal val LAST_CONFLICT = "last_conflict"
        internal val CHECKSUM = "checksum"


        private val decomposeBooleanValue = object : Fn<Boolean, String>() {
            override fun apply(b: Boolean): String {
                return b.toString()
            }
        }

        private val decomposeLongValue = object : Fn<Long, String>() {
            override fun apply(l: Long): String {
                return l.toString()
            }
        }

        private val decomposeDateValue = object : Fn<Date, String>() {
            override fun apply(d: Date): String {
                return DateTimeSupport.toUTC(d.time)
            }
        }

        private val decomposeStringValue = object : Fn<String, String>() {
            override fun apply(s: String): String {
                return s
            }
        }

        private val decomposeVersionValue = object : Fn<Version, String>() {
            override fun apply(v: Version): String {
                return v.toString()
            }
        }

        private val toKey = object : Fn<Property, String>() {
            override fun apply(property: Property): String {
                return property.id.name
            }
        }

        private val toValue = object : Fn<Property, String>() {
            override fun apply(property: Property): String {
                return property.value.decompose(decomposeStringValue, decomposeDateValue, decomposeLongValue,
                        decomposeBooleanValue, decomposeVersionValue)
            }
        }
    }

}
