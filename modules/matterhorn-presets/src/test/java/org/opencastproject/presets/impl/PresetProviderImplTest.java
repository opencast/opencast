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

package org.opencastproject.presets.impl;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

public class PresetProviderImplTest {
  private static final String SERIES_ID = "series_id";
  private static final String SERIES_PROPERTY_NAME = "SeriesOrgPropertyName";
  private static final String SERIES_PROPERTY_VALUE = "SeriesOrgPropertyValue";
  private static final String ORG_PROPERTY_NAME = "OrgPropertyName";
  private static final String ORG_PROPERTY_VALUE = "OrgPropertyValue";
  private static final String NOT_FOUND_NAME = "NotFoundName";
  private Organization organization;
  private PresetProviderImpl presetProviderImpl;
  private SeriesService seriesService;

  @Before
  public void setUp() throws SeriesException, NotFoundException, UnauthorizedException {
    Map<String, String> properties = new TreeMap<String, String>();
    properties.put(ORG_PROPERTY_NAME, ORG_PROPERTY_VALUE);
    organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getProperties()).andReturn(properties).anyTimes();
    EasyMock.replay(organization);

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.replay(securityService);

    seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeriesProperty(SERIES_ID, SERIES_PROPERTY_NAME)).andReturn(SERIES_PROPERTY_VALUE).anyTimes();
    EasyMock.expect(seriesService.getSeriesProperty(SERIES_ID, ORG_PROPERTY_NAME)).andThrow(new NotFoundException()).anyTimes();
    EasyMock.expect(seriesService.getSeriesProperty(SERIES_ID, NOT_FOUND_NAME)).andThrow(new NotFoundException()).anyTimes();
    EasyMock.replay(seriesService);

    presetProviderImpl = new PresetProviderImpl();
    presetProviderImpl.setSeriesService(seriesService);
    presetProviderImpl.setSecurityService(securityService);
  }

  @Test
  public void propertyInSeriesNullOrg() throws NotFoundException {
    SecurityService securityServiceWithoutOrg = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityServiceWithoutOrg.getOrganization()).andReturn(null);
    EasyMock.replay(securityServiceWithoutOrg);
    presetProviderImpl.setSecurityService(securityServiceWithoutOrg);
    assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl.getProperty(SERIES_ID, SERIES_PROPERTY_NAME));
  }

  @Test
  public void propertyInSeries() throws NotFoundException {
    assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl.getProperty(SERIES_ID, SERIES_PROPERTY_NAME));
  }

  @Test
  public void propertyInOrganization() throws NotFoundException {
    assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl.getProperty(SERIES_ID, ORG_PROPERTY_NAME));
  }

  @Test
  public void propertyInOrganizationNullSeries() throws NotFoundException {
    assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl.getProperty(null, ORG_PROPERTY_NAME));
  }

  @Test
  public void propertyInOrganizationEmptySeries() throws NotFoundException {
    assertEquals(ORG_PROPERTY_VALUE, presetProviderImpl.getProperty("", ORG_PROPERTY_NAME));
  }

  @Test
  public void propertyIsNotSet() {
    try {
      assertEquals(SERIES_PROPERTY_VALUE, presetProviderImpl.getProperty(SERIES_ID, NOT_FOUND_NAME));
      fail();
    } catch (NotFoundException e) {
      // This test expects a not found exception.
    }
  }
}
