/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.feed.impl;

import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;



public class DublinCoreExtensionTest {

    private static DublinCoreExtension instance;

    public DublinCoreExtensionTest() {
    }

    @Before
    public void setUp() {
        instance = new DublinCoreExtension();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getUri method, of class DublinCoreExtension.
     */
    @Test
    public void testSetterAndGetter() {
        String expResult = "http://purl.org/dc/elements/1.1/";
        String result = instance.getUri();
        assertEquals(expResult, result);

        assertNull(instance.getContributor());
        instance.setContributor(result);
        assertEquals(instance.getContributor(), result);

        instance.setCoverage(result);
        assertEquals(instance.getCoverage(), result);

        assertNull(instance.getCreator());
        instance.setCreator(result);
        assertEquals(instance.getCreator(), result);

        Date date = new Date(21091981);
        instance.setDate(date);
        assertEquals(instance.getDate(), date);

        instance.setDescription(result);
        assertEquals(instance.getDescription(), result);

        instance.setFormat(result);
        assertEquals(instance.getFormat(), result);

        instance.setIdentifier(result);
        assertEquals(instance.getIdentifier(), result);

        instance.setLanguage(result);
        assertEquals(instance.getLanguage(), result);

        assertNull(instance.getPublisher());
        instance.setPublisher(result);
        assertEquals(instance.getPublisher(), result);

        instance.setRelation(result);
        assertEquals(instance.getRelation(), result);

        instance.setRights(result);
        assertEquals(instance.getRights(), result);

        instance.setSource(result);
        assertEquals(instance.getSource(), result);

        instance.setTitle(result);
        assertEquals(instance.getTitle(), result);
        assertEquals(instance.geTitles(), result);

        instance.setType(result);
        assertEquals(instance.getType(), result);

    }

    /**
     * Tests for the List Properties ans Subclass Subject
     */
    @Test
    public void testLists() {
        String result = "item";
        instance.addContributor(result);
        assertEquals(instance.getContributors().get(0), result);

        instance.addPublisher(result);
        assertEquals(instance.getPublishers().get(0), result);

        instance.addCreator(result);
        assertEquals(instance.getCreators().get(0), result);

        instance.addSubject("uri", result);
        assertEquals(instance.getSubjects().get(0).getValue(), result);
        assertEquals(instance.getSubjects().get(0).getTaxonomyUri(), "uri");
    }
}
