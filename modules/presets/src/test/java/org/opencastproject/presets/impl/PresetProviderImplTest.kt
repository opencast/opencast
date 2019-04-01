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

package org.opencastproject.presets.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.fail

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.util.NotFoundException

import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test
import java.util.TreeMap

class PresetProviderImplTest {
    private var organization: Organization? = null
    private var presetProviderImpl: PresetProviderImpl? = null
    private var seriesService: SeriesService? = null

    @Before
    @Throws(SeriesException::class, NotFoundException::class, UnauthorizedException::class)
    fun setUp() {
        val properties = TreeMap<String, String>()
        properties[ORG_PROPERTY_NAME] = ORG_PROPERTY_VALUE
        organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization!!.properties).andReturn(properties).anyTimes()
        EasyMock.replay(organization!!)

        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization)
        EasyMock.replay(securityService)

        seriesService = EasyMock.createMock<SeriesService>(SeriesService::class.java)
        EasyMock.expect(seriesService!!.getSeriesProperty(SERIES_ID, SERIES_PROPERTY_NAME)).andReturn(SERIES_PROPERTY_VALUE).anyTimes()
        EasyMock.expect(seriesService!!.getSeriesProperty(SERIES_ID, ORG_PROPERTY_NAME)).andThrow(NotFoundException()).anyTimes()
        EasyMock.expect(seriesService!!.getSeriesProperty(SERIES_ID, NOT_FOUND_NAME)).andThrow(NotFoundException()).anyTimes()
        EasyMock.replay(seriesService!!)

        presetProviderImpl = PresetProviderImpl()
        presetProviderImpl!!.setSeriesService(seriesService)
        presetProviderImpl!!.setSecurityService(securityService)
    }

    @Test
    @Throws(NotFoundException::class)
    fun propertyInSeriesNullOrg() {
        val securityServiceWithoutOrg = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityServiceWithoutOrg.organization).andReturn(null)
        EasyMock.replay(securityServiceWithoutOrg)
        presetProviderImpl!!.setSecurityService(securityServiceWithoutOrg)
        assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl!!.getProperty(SERIES_ID, SERIES_PROPERTY_NAME))
    }

    @Test
    @Throws(NotFoundException::class)
    fun propertyInSeries() {
        assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl!!.getProperty(SERIES_ID, SERIES_PROPERTY_NAME))
    }

    @Test
    @Throws(NotFoundException::class)
    fun propertyInOrganization() {
        assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl!!.getProperty(SERIES_ID, ORG_PROPERTY_NAME))
    }

    @Test
    @Throws(NotFoundException::class)
    fun propertyInOrganizationNullSeries() {
        assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl!!.getProperty(null, ORG_PROPERTY_NAME))
    }

    @Test
    @Throws(NotFoundException::class)
    fun propertyInOrganizationEmptySeries() {
        assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl!!.getProperty("", ORG_PROPERTY_NAME))
    }

    @Test
    fun propertyIsNotSet() {
        try {
            assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl!!.getProperty(SERIES_ID, NOT_FOUND_NAME))
            fail()
        } catch (e: NotFoundException) {
            // This test expects a not found exception.
        }

    }

    companion object {
        private val SERIES_ID = "series_id"
        private val SERIES_PROPERTY_NAME = "SeriesOrgPropertyName"
        private val SERIES_PROPERTY_VALUE = "SeriesOrgPropertyValue"
        private val ORG_PROPERTY_NAME = "OrgPropertyName"
        private val ORG_PROPERTY_VALUE = "OrgPropertyValue"
        private val NOT_FOUND_NAME = "NotFoundName"
    }
}
