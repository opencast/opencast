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

package org.opencastproject.kernel.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonpTest {

  @Test
  public void testCallbackSafety() {
    // Some good ones
    Assertions.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("GoodCallback").matches());
    Assertions.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("GoodCallback1").matches());
    Assertions.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("Good1Callback").matches());
    Assertions.assertTrue(JsonpFilter.SAFE_PATTERN.matcher("Object.GoodCallback").matches());

    // Some bad ones
    Assertions.assertFalse(JsonpFilter.SAFE_PATTERN.matcher("alert(document.cookie)").matches());
  }
}
