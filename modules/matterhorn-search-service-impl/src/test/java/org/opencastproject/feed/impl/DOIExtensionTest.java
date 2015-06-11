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

public class DOIExtensionTest {

    private static DOIExtension instance;

    public DOIExtensionTest() {
    }

    @Before
    public void setUp() {
        instance = new DOIExtension();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test Setter and Getter for DOIExtension
     */
    @Test
    public void testSetterAndGetter() {
        instance.setAlsoPublishedAs("publish");
        Assert.assertEquals(instance.getAlsoPublishedAs(), "publish");

        instance.setCompiledBy("compile");
        Assert.assertEquals(instance.getCompiledBy(), "compile");

        instance.setDoi("doi");
        Assert.assertEquals(instance.getDoi(), "doi");

        instance.setIssueNumber("number");
        Assert.assertEquals(instance.getIssueNumber(), "number");

        instance.setMode("mode");
        Assert.assertEquals(instance.getMode(), "mode");

        instance.setRegistrationAgency("agency");
        Assert.assertEquals(instance.getRegistrationAgency(), "agency");

        instance.setStructuralType("type");
        Assert.assertEquals(instance.getStructuralType(), "type");

        Assert.assertEquals(instance.getUri(), "http://www.doi.org");

    }
}
