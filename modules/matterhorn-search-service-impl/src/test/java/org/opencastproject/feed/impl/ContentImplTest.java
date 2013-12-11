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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.feed.api.Content;

public class ContentImplTest {

    private static ContentImpl instance;

    public ContentImplTest() {
    }

    @Before
    public void setUp() {
        instance = new ContentImpl("value");
    }

    @After
    public void tearDown() {
        instance = null;
    }

    @Test
    public void contentImplTest() {
        ContentImpl content = new ContentImpl("value");
        Assert.assertEquals(content.value, "value");

    }

    /**
     * Test for Setters and Getters ContentImpl
     */
    @Test
    public void setterAndGetter() {

        Content content = new ContentImpl("value");
        instance.setMode(content.getMode());
        Assert.assertEquals(instance.getMode(), content.getMode());

        instance.setType("type");
        Assert.assertEquals(instance.getType(), "type");

        instance.setValue("value");
        Assert.assertEquals(instance.getValue(), "value");



    }
}
