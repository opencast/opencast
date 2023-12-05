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
package org.opencastproject.security.urlsigning.provider.impl;

import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.WowzaResourceStrategyImpl;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.urlsigning.common.Policy;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.Hashtable;

public class WowzaUrlSigningProviderTest {
  private static final String KEY_ID = "wowza";
  private static final String URL_VALUE = "http://192.168.1.1:1935";
  private static final String SECRET_VALUE = "myTokenPrefix@mySharedSecret";
  private static final String ORGANIZATION_ID = "mh_default_org";

  private static WowzaUrlSigningProvider signer;
  private static Dictionary<String, String> properties;

  @BeforeClass
  public static void setUpClass() throws Exception {
    JaxbOrganization organization = EasyMock.createNiceMock(JaxbOrganization.class);
    EasyMock.expect(organization.getId()).andReturn(ORGANIZATION_ID).anyTimes();
    EasyMock.replay(organization);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    signer = new WowzaUrlSigningProvider();
    signer.setSecurityService(securityService);

    properties = new Hashtable<>();
  }

  @Before
  public void setUp() throws Exception {
    properties = new Hashtable<>();
    signer.updated(properties);
  }

  @Test
  public void testSign() throws UrlSigningException, ConfigurationException {
    properties.put(String.join(".", AbstractUrlSigningProvider.KEY_PROPERTY_PREFIX,
        KEY_ID, AbstractUrlSigningProvider.SECRET), SECRET_VALUE);
    properties.put(String.join(".", AbstractUrlSigningProvider.KEY_PROPERTY_PREFIX,
        KEY_ID, AbstractUrlSigningProvider.URL), URL_VALUE);

    signer.updated(properties);

    String ip = "192.168.1.2";
    String encryptionKeyId = "myTokenPrefix";
    String encryptionKey = "mySharedSecret";
    String startTime = "1395230";
    String endTime = "1500000";
    String predefinedHash = "E4mSDMQXutPnj6ApllhzFoONFDQVzhAdV39Q9I9TGsU=";
    String predefinedUri = "http://192.168.1.1:1935/vod/sample.mp4/playlist.m3u8?"
            + encryptionKeyId + "endtime=" + endTime
            + "&" + encryptionKeyId + "starttime=" + startTime
            + "&" + encryptionKeyId + "CustomParameter=abcdef"
            + "&" + encryptionKeyId + "hash=" + predefinedHash;

    Policy policy = Policy.mkPolicyValidFromWithIP(
            "http://192.168.1.1:1935/vod/sample.mp4/playlist.m3u8?CustomParameter=abcdef",
            new DateTime(Long.parseLong(endTime) * 1000), new DateTime(Long.parseLong(startTime) * 1000), ip);
    policy.setResourceStrategy(new WowzaResourceStrategyImpl());

    String uri = signer.sign(policy);

    assertTrue(uri.equals(predefinedUri));
  }
}
