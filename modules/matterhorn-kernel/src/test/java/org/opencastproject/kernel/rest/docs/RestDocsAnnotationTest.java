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

package org.opencastproject.kernel.rest.docs;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * This test class tests the functionality of @RestQuery annotation type.
 */
public class RestDocsAnnotationTest {
  @Test
  public void testRestQueryDocs() {
    Method testMethod;
    try {
      testMethod = TestServletSample.class.getMethod("methodA");
      if (testMethod != null) {
        RestQuery annotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        
        Assert.assertEquals("Starts a capture using the default devices as appropriate.", annotation.description());      
        Assert.assertEquals("A list of capture agent things", annotation.returnDescription());

        Assert.assertTrue(annotation.pathParameters().length == 1);
        Assert.assertEquals("location", annotation.pathParameters()[0].name());
        Assert.assertEquals("The room of the capture agent", annotation.pathParameters()[0].description());
        Assert.assertFalse(annotation.pathParameters()[0].isRequired());
        
        Assert.assertTrue(annotation.restParameters().length == 1);
        Assert.assertEquals("id", annotation.restParameters()[0].name());
        Assert.assertEquals("The ID of the capture to start", annotation.restParameters()[0].description());
        Assert.assertTrue(annotation.restParameters()[0].isRequired());        
        
        Assert.assertTrue(annotation.reponses().length == 2);

        Assert.assertEquals(200, annotation.reponses()[0].responseCode());
        Assert.assertEquals("When the capture started correctly", annotation.reponses()[0].description());
        
        Assert.assertEquals(400, annotation.reponses()[1].responseCode());
        Assert.assertEquals("When there are no media devices", annotation.reponses()[1].description());
      }
    } catch (SecurityException e) {
      Assert.fail();
    } catch (NoSuchMethodException e) {
      Assert.fail();
    }
  }

  /**
   * This sample class simulates a annotated REST service class.
   */
  private class TestServletSample {
   
    @SuppressWarnings("unused")
    @RestQuery(
            name = "something",
            description = "Starts a capture using the default devices as appropriate.",
            returnDescription = "A list of capture agent things",
            pathParameters = { @RestParameter(name = "location", description = "The room of the capture agent", isRequired = false, type = Type.STRING, defaultValue = "") },
            restParameters = { @RestParameter(name = "id", description = "The ID of the capture to start", isRequired = true, type = Type.STRING, defaultValue = "") },
            reponses = { @RestResponse(responseCode = 200, description = "When the capture started correctly"),
                         @RestResponse(responseCode = 400, description = "When there are no media devices") }
            )            
    public int methodA()
    {
      return 0;
    }

  }
}