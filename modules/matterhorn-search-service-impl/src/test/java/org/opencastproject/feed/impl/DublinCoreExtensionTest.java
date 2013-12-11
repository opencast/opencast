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
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


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

        Assert.assertNull(instance.getContributor());
        instance.setContributor(result);
        Assert.assertEquals(instance.getContributor(), result);

        instance.setCoverage(result);
        Assert.assertEquals(instance.getCoverage(), result);

        Assert.assertNull(instance.getCreator());
        instance.setCreator(result);
        Assert.assertEquals(instance.getCreator(), result);

        Date date = new Date(21091981);
        instance.setDate(date);
        Assert.assertEquals(instance.getDate(), date);

        instance.setDescription(result);
        Assert.assertEquals(instance.getDescription(), result);

        instance.setFormat(result);
        Assert.assertEquals(instance.getFormat(), result);

        instance.setIdentifier(result);
        Assert.assertEquals(instance.getIdentifier(), result);

        instance.setLanguage(result);
        Assert.assertEquals(instance.getLanguage(), result);

        Assert.assertNull(instance.getPublisher());
        instance.setPublisher(result);
        Assert.assertEquals(instance.getPublisher(), result);

        instance.setRelation(result);
        Assert.assertEquals(instance.getRelation(), result);

        instance.setRights(result);
        Assert.assertEquals(instance.getRights(), result);

        instance.setSource(result);
        Assert.assertEquals(instance.getSource(), result);

        instance.setTitle(result);
        Assert.assertEquals(instance.getTitle(), result);
        Assert.assertEquals(instance.geTitles(), result);

        instance.setType(result);
        Assert.assertEquals(instance.getType(), result);

    }
    
    /**
     * Tests for the List Properties ans Subclass Subject
     */
    @Test
    public void testLists() {
        String result = "item";
        instance.addContributor(result);
        Assert.assertEquals(instance.getContributors().get(0), result);

        instance.addPublisher(result);
        Assert.assertEquals(instance.getPublishers().get(0), result);

        instance.addCreator(result);
        Assert.assertEquals(instance.getCreators().get(0), result);

        instance.addSubject("uri", result);
        Assert.assertEquals(instance.getSubjects().get(0).getValue(), result);
        Assert.assertEquals(instance.getSubjects().get(0).getTaxonomyUri(), "uri");
    }
}
