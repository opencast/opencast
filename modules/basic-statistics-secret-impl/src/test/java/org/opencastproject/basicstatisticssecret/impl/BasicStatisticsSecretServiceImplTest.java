/*
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
package org.opencastproject.basicstatisticssecret.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Covers that the service starts up with a secret.
 */
public class BasicStatisticsSecretServiceImplTest {

  private BasicStatisticsSecretServiceImpl service;

  @Before
  public void setUp() {
    service = new BasicStatisticsSecretServiceImpl();
  }

  @Test
  public void getCurrentSecretReturnsA32ByteSecret() throws Exception {
    byte[] secret = service.getCurrentSecret();

    assertNotNull(secret);
    assertEquals(32, secret.length);
  }
}
