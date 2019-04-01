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

import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.opencastproject.util.UrlSupport.uri

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.PropertyId
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Value
import org.opencastproject.assetmanager.impl.AbstractAssetManager
import org.opencastproject.assetmanager.impl.HttpAssetProvider
import org.opencastproject.assetmanager.impl.persistence.Database
import org.opencastproject.assetmanager.impl.storage.AssetStore
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.persistencefn.PersistenceUtil
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt
import com.google.gson.Gson

import org.easymock.EasyMock
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.Version
import org.osgi.service.cm.Configuration
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.service.component.ComponentContext

import java.io.File
import java.net.URI
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.Hashtable

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

class SchedulerMigrationServiceTest {
    private val schedulerMigrationService = SchedulerMigrationService()
    private var assetManager: AssetManager? = null
    private val currentOrg = DefaultOrganization()
    private val emf = mkMigrationEntityManagerFactory()
    private val gson = Gson()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val orgDirService = createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        expect(orgDirService.getOrganization(anyString())).andReturn(currentOrg).anyTimes()
        expect(orgDirService.organizations).andReturn(listOf<Organization>(DefaultOrganization())).anyTimes()
        replay(orgDirService)

        schedulerMigrationService.setOrgDirectoryService(orgDirService)

        val securityService = createNiceMock<SecurityService>(SecurityService::class.java)
        expect(securityService.organization).andReturn(DefaultOrganization()).anyTimes()
        expect<User>(securityService.user).andReturn(JaxbUser()).anyTimes()
        replay(securityService)

        schedulerMigrationService.setSecurityService(securityService)
        schedulerMigrationService.setEntityManagerFactory(emf)

        assetManager = mkAssetManager()
        schedulerMigrationService.setAssetManager(assetManager)


        // fill assetmanager with testdata
        assetManager!!.takeSnapshot(SchedulerMigrationService.SNAPSHOT_OWNER, generateEvent(Opt.some("mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_LAST_HEARD_CONFIG), Value.mk(100)))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_STATE_CONFIG), Value.mk("state of mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.SOURCE_CONFIG), Value.mk("source of mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.PRESENTERS_CONFIG), Value.mk("presenter of mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.AGENT_CONFIG), Value.mk("agent of mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.START_DATE_CONFIG), Value.mk(Date(102))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.END_DATE_CONFIG), Value.mk(Date(103))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.LAST_MODIFIED_DATE), Value.mk(Date(104))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.CHECKSUM), Value.mk("checksum of mp1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.WORKFLOW_NAMESPACE, "workflow testproperty"), Value.mk("wf prop 1")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.CA_NAMESPACE, "agent testproperty"), Value.mk("ca prop 1")))

        assetManager!!.takeSnapshot(SchedulerMigrationService.SNAPSHOT_OWNER, generateEvent(Opt.some("mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_LAST_HEARD_CONFIG), Value.mk(200)))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_STATE_CONFIG), Value.mk("state of mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.SOURCE_CONFIG), Value.mk("source of mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.PRESENTERS_CONFIG), Value.mk("presenter of mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.AGENT_CONFIG), Value.mk("agent of mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.START_DATE_CONFIG), Value.mk(Date(202))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.END_DATE_CONFIG), Value.mk(Date(203))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.LAST_MODIFIED_DATE), Value.mk(Date(204))))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.CHECKSUM), Value.mk("checksum of mp2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.WORKFLOW_NAMESPACE, "workflow testproperty"), Value.mk("wf prop 2")))
        assetManager!!.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.CA_NAMESPACE, "agent testproperty"), Value.mk("ca prop 2")))
    }

    @Test
    @Throws(Exception::class)
    fun testSchedulerMigration() {
        val bundleContext = createNiceMock<BundleContext>(BundleContext::class.java)
        val bundle = createNiceMock<Bundle>(Bundle::class.java)
        val version = Version(7, 0, 0)
        val configurationAdmin = createNiceMock<ConfigurationAdmin>(ConfigurationAdmin::class.java)
        val configuration = createNiceMock<Configuration>(Configuration::class.java)
        val properties = Hashtable<String, Any>()
        properties["maintenance"] = true
        expect(bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("root").anyTimes()
        expect(bundleContext.bundle).andReturn(bundle).anyTimes()
        expect(bundleContext.getService<Any>(anyObject<ServiceReference<Any>>())).andReturn(configurationAdmin).anyTimes()
        expect(bundle.version).andReturn(version).anyTimes()
        expect(configurationAdmin.getConfiguration("org.opencastproject.scheduler.impl.SchedulerServiceImpl")).andReturn(configuration).anyTimes()
        expect<Dictionary<String, Any>>(configuration.properties).andReturn(properties).anyTimes()

        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bundleContext).anyTimes()

        EasyMock.replay(cc, bundleContext, bundle, configurationAdmin, configuration)

        schedulerMigrationService.activate(cc)

        val em = emf.createEntityManager()
        var entityOpt: Opt<ExtendedEventDto>
        var entity: ExtendedEventDto
        var map: Map<*, *>

        entityOpt = Opt.nul(em.find(ExtendedEventDto::class.java, EventIdPK("mp1", currentOrg.toString())))
        assertTrue(entityOpt.isSome)
        entity = entityOpt.get()
        assertEquals(100, entity.recordingLastHeard)
        assertEquals("state of mp1", entity.recordingState)
        assertEquals("source of mp1", entity.source)
        assertEquals("presenter of mp1", entity.presenters)
        assertEquals("agent of mp1", entity.captureAgentId)
        assertEquals(Date(102), entity.startDate)
        assertEquals(Date(103), entity.endDate)
        assertEquals(Date(104), entity.lastModifiedDate)
        assertEquals("checksum of mp1", entity.checksum)
        map = gson.fromJson(entity.captureAgentProperties, HashMap<*, *>::class.java)
        assertEquals("ca prop 1", map["agent testproperty"])
        map = gson.fromJson(entity.workflowProperties, HashMap<*, *>::class.java)
        assertEquals("wf prop 1", map["workflow testproperty"])

        entityOpt = Opt.nul(em.find(ExtendedEventDto::class.java, EventIdPK("mp2", currentOrg.toString())))
        assertTrue(entityOpt.isSome)
        entity = entityOpt.get()
        assertEquals(200, entity.recordingLastHeard)
        assertEquals("state of mp2", entity.recordingState)
        assertEquals("source of mp2", entity.source)
        assertEquals("presenter of mp2", entity.presenters)
        assertEquals("agent of mp2", entity.captureAgentId)
        assertEquals(Date(202), entity.startDate)
        assertEquals(Date(203), entity.endDate)
        assertEquals(Date(204), entity.lastModifiedDate)
        assertEquals("checksum of mp2", entity.checksum)
        map = gson.fromJson(entity.captureAgentProperties, HashMap<*, *>::class.java)
        assertEquals("ca prop 2", map["agent testproperty"])
        map = gson.fromJson(entity.workflowProperties, HashMap<*, *>::class.java)
        assertEquals("wf prop 2", map["workflow testproperty"])
    }

    private fun mkAssetManager(): AssetManager {
        val db = Database(mkAssetManagerEntityManagerFactory())
        val assetStore = mkAssetStore()
        return object : AbstractAssetManager() {

            override fun getHttpAssetProvider(): HttpAssetProvider {
                // identity provider
                return HttpAssetProvider { snapshot ->
                    AbstractAssetManager.rewriteUris(snapshot, object : Fn<MediaPackageElement, URI>() {
                        override fun apply(mpe: MediaPackageElement): URI {
                            val baseName = AbstractAssetManager.getFileNameFromUrn(mpe).getOr(mpe.elementType.toString())

                            // the returned uri must match the path of the {@link #getAsset} method
                            return uri(baseDir!!.toURI(),
                                    mpe.mediaPackage.identifier.toString(),
                                    mpe.identifier,
                                    baseName)
                        }
                    })
                }
            }

            override fun getDb(): Database {
                return db
            }

            override fun getWorkspace(): Workspace {
                return EasyMock.niceMock(Workspace::class.java)
            }

            override fun getLocalAssetStore(): AssetStore {
                return assetStore
            }

            override fun getCurrentOrgId(): String {
                return currentOrg.id
            }
        }
    }

    companion object {

        @ClassRule
        val testFolder = TemporaryFolder()

        private var baseDir: File? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setupClass() {
            baseDir = testFolder.newFolder()
        }

        private fun mkAssetStore(): AssetStore {
            val result = EasyMock.niceMock<AssetStore>(AssetStore::class.java)
            EasyMock.expect(result.storeType).andReturn("test_store").anyTimes()
            EasyMock.replay(result)
            return result
        }

        @Throws(MediaPackageException::class)
        private fun generateEvent(id: Opt<String>): MediaPackage {
            val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
            if (id.isSome)
                mp.identifier = IdImpl(id.get())
            return mp
        }

        private fun mkAssetManagerEntityManagerFactory(): EntityManagerFactory {
            return PersistenceUtil.mkTestEntityManagerFactory("org.opencastproject.assetmanager.impl", true)
        }

        private fun mkMigrationEntityManagerFactory(): EntityManagerFactory {
            return PersistenceUtil.mkTestEntityManagerFactory("org.opencastproject.migration", true)
        }
    }
}
