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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class CategoryImplTest {

    private static CategoryImpl instance;
    @Before
    public void setUp() {
    instance  = new CategoryImpl("name", "uri");

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getName method, of class CategoryImpl.
     */
    @Test
    public void testGetName() {
        String expResult = "name";
        String result = instance.getName();
        assertEquals(expResult, result);

    }

    /**
     * Test of getTaxonomyUri method, of class CategoryImpl.
     */
    @Test
    public void testGetTaxonomyUri() {
        String expResult = "uri";
        String result = instance.getTaxonomyUri();
        assertEquals(expResult, result);
    }

    /**
     * Test of setName method, of class CategoryImpl.
     */
    @Test
    public void testSetName() {
        String name = "name";
        instance.setName(name);
        assertEquals(instance.getName(),name);
    }

    /**
     * Test of setTaxonomyUri method, of class CategoryImpl.
     */
    @Test
    public void testSetTaxonomyUri() {
        String taxonomyUri = "uri";
        instance.setTaxonomyUri(taxonomyUri);
        assertEquals(instance.getTaxonomyUri(), taxonomyUri);
    }
}
