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
package org.opencastproject.scheduler.conflict.notifier.email

import org.opencastproject.kernel.mail.SmtpService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter
import org.opencastproject.scheduler.api.ConflictResolution.Strategy
import org.opencastproject.scheduler.api.ConflictingEvent
import org.opencastproject.scheduler.api.SchedulerEvent
import org.opencastproject.scheduler.api.TechnicalMetadata
import org.opencastproject.scheduler.api.TechnicalMetadataImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.SecurityService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.IoSupport
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext

import java.net.URI
import java.util.ArrayList
import java.util.Date
import java.util.Dictionary
import java.util.HashMap
import java.util.HashSet
import java.util.Hashtable

import javax.mail.MessagingException
import javax.mail.internet.MimeMessage

class EmailSchedulerConflictNotifierTest {

    private var conflictNotifier: EmailSchedulerConflictNotifier? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val properties = Hashtable<String, String>()
        properties["to"] = "test@test.com"
        properties["subject"] = "Test email scheduler conflict"
        properties["template"] = "Dear Administrator,\n\nthe following recording schedules are conflicting with existing ones:\n\n\${recordings}\nBye!"

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<User>(securityService.user).andReturn(JaxbUser("admin", "provider", DefaultOrganization(),
                JaxbRole("admin", DefaultOrganization(), "test"))).anyTimes()
        EasyMock.expect<Organization>(securityService.organization).andReturn(DefaultOrganization()).anyTimes()
        EasyMock.replay(securityService)

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect<File>(workspace.get(EasyMock.anyObject(URI::class.java)))
                .andReturn(IoSupport.classPathResourceAsFile("/dublincore.xml").get()).anyTimes()
        EasyMock.replay(workspace)

        val episodeAdapter = EasyMock.createMock<EventCatalogUIAdapter>(EventCatalogUIAdapter::class.java)
        EasyMock.expect(episodeAdapter.flavor).andReturn(MediaPackageElementFlavor("dublincore", "episode"))
                .anyTimes()
        EasyMock.expect(episodeAdapter.organization).andReturn(DefaultOrganization().id).anyTimes()
        EasyMock.replay(episodeAdapter)

        val extendedAdapter = EasyMock.createMock<EventCatalogUIAdapter>(EventCatalogUIAdapter::class.java)
        EasyMock.expect(extendedAdapter.flavor).andReturn(MediaPackageElementFlavor("extended", "episode"))
                .anyTimes()
        EasyMock.expect(extendedAdapter.organization).andReturn(DefaultOrganization().id).anyTimes()
        EasyMock.replay(extendedAdapter)

        val bundleContext = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY))
                .andReturn("http://localhost:8080").anyTimes()
        EasyMock.replay(bundleContext)

        val componentContext = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(componentContext.bundleContext).andReturn(bundleContext).anyTimes()
        EasyMock.expect(componentContext.properties).andReturn(Hashtable()).anyTimes()
        EasyMock.replay(componentContext)

        conflictNotifier = EmailSchedulerConflictNotifier()
        conflictNotifier!!.setSecurityService(securityService)
        conflictNotifier!!.setWorkspace(workspace)
        conflictNotifier!!.addCatalogUIAdapter(episodeAdapter)
        conflictNotifier!!.addCatalogUIAdapter(extendedAdapter)
        conflictNotifier!!.activate(componentContext)
        conflictNotifier!!.updated(properties)
    }

    @Test
    @Throws(Exception::class)
    fun testEmailSchedulerConflict() {
        val userIds = HashSet<String>()
        userIds.add("user1")
        userIds.add("user2")

        val caProperties = HashMap<String, String>()
        caProperties["test"] = "true"
        caProperties["clear"] = "all"

        val wfProperties = HashMap<String, String>()
        wfProperties["test"] = "false"
        wfProperties["skip"] = "true"

        val mpId = "1234"
        val technicalMetadata = TechnicalMetadataImpl(mpId, "demo", Date(),
                Date(Date().time + 10 * 60 * 1000), userIds, wfProperties, caProperties, null)
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.identifier = IdImpl(mpId)
        mp.add(DublinCores.mkOpencastEpisode().catalog)
        val extendedEvent = DublinCores.mkStandard()
        extendedEvent.flavor = MediaPackageElementFlavor("extended", "episode")
        mp.add(extendedEvent)

        val schedulerEvent = EasyMock.createNiceMock<SchedulerEvent>(SchedulerEvent::class.java)
        EasyMock.expect(schedulerEvent.technicalMetadata).andReturn(technicalMetadata).anyTimes()
        EasyMock.expect(schedulerEvent.mediaPackage).andReturn(mp).anyTimes()
        EasyMock.expect(schedulerEvent.eventId).andReturn(mpId).anyTimes()
        EasyMock.expect(schedulerEvent.version).andReturn("2").anyTimes()
        EasyMock.replay(schedulerEvent)

        val conflictingEvent = EasyMock.createNiceMock<ConflictingEvent>(ConflictingEvent::class.java)
        EasyMock.expect(conflictingEvent.oldEvent).andReturn(schedulerEvent).anyTimes()
        EasyMock.expect(conflictingEvent.newEvent).andReturn(schedulerEvent).anyTimes()
        EasyMock.expect(conflictingEvent.conflictStrategy).andReturn(Strategy.NEW).anyTimes()
        EasyMock.replay(conflictingEvent)

        val conflicts = ArrayList<ConflictingEvent>()
        conflicts.add(conflictingEvent)

        val counter = arrayOfNulls<Int>(1)
        counter[0] = 0

        val smtpService = object : SmtpService() {
            @Throws(MessagingException::class)
            override fun send(message: MimeMessage) {
                counter[0]++
            }
        }

        conflictNotifier!!.setSmtpService(smtpService)
        conflictNotifier!!.notifyConflicts(conflicts)

        Assert.assertEquals(1, counter[0].toInt().toLong())
    }

}
