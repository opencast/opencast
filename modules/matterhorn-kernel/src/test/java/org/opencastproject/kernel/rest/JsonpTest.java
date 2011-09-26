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
package org.opencastproject.kernel.rest;

import org.junit.Assert;
import org.junit.Test;

public class JsonpTest {

  @Test
  public void testCallbackSafety() {
    // Some good ones
    Assert.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("GoodCallback").matches());
    Assert.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("GoodCallback1").matches());
    Assert.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("Good1Callback").matches());
    Assert.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("Object.GoodCallback").matches());

    // Some bad ones
    Assert.assertFalse(JsonpFilter.SAFE_PATTERN.matcher("alert(document.cookie)").matches());
  }
}
