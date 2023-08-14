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

package org.opencastproject.feed.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EnclosureImplTest {

  private static EnclosureImpl instance;

  public EnclosureImplTest() {
  }

  @Before
    public void setUp() {
    instance = new EnclosureImpl(null, null, null, 2L);
  }

  @After
    public void tearDown() {
    instance = null;
  }

    /**
     * Test of SetterAndGetter for EnclousureImol
     */
  @Test
    public void testSetterAndGetter() {

    String result = "item";
    Assert.assertNull(instance.getFlavor());
    instance.setFlavor(result);
    Assert.assertEquals(instance.getFlavor(), result);

    Assert.assertNull(instance.getType());
    instance.setType(result);
    Assert.assertEquals(instance.getType(), result);

    Assert.assertNull(instance.getUrl());
    instance.setUrl(result);
    Assert.assertEquals(instance.getUrl(), result);

    Assert.assertEquals(instance.getLength(), 2L);
    instance.setLength(4L);
    Assert.assertEquals(instance.getLength(), 4L);
  }
}
