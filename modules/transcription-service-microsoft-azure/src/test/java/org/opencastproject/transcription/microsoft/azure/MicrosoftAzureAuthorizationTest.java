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
package org.opencastproject.transcription.microsoft.azure;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MicrosoftAzureAuthorizationTest {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureAuthorizationTest.class);

  private MicrosoftAzureAuthorization azureAuthorization;

  @Before
  public void setUp() throws MicrosoftAzureStorageClientException {
    String azureStorageAccountName = "azurestorageaccount";
    String azureAccountAccessKey = "mysecretaccesskey";
    azureAuthorization = new MicrosoftAzureAuthorization(azureStorageAccountName, azureAccountAccessKey);
  }

  @Test
  public void generateAccountSASToken() {
    Calendar c = Calendar.getInstance();
    c.set(2023,Calendar.JANUARY,15,12,0,0);
    c.setTimeZone(TimeZone.getTimeZone("GMT+1"));
    Date start = c.getTime();
    c.set(2023,Calendar.JANUARY,16,12,0,0);
    Date end = c.getTime();
    String sasToken = azureAuthorization.generateAccountSASToken("r", "c", start, end, null, null);
    Assert.assertNotNull(sasToken);
    Assert.assertTrue(sasToken.contains("sp=r"));
    Assert.assertTrue(sasToken.contains("srt=c"));
    Assert.assertTrue(sasToken.contains("st=2023-01-15T11:00:00Z"));
    Assert.assertTrue(sasToken.contains("se=2023-01-16T11:00:00Z"));
    Assert.assertTrue(sasToken.contains("spr=https"));
    Assert.assertTrue(sasToken.contains("sig=1oAO19BF8zsHJ7xKO1AzVtgSacN8xgqM6dFBZoQMZUM%3D"));
  }
}
