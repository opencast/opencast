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
package org.opencastproject.mediapackage.identifier;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests non-handle Identifiers
 *
 */
public class IdTest {

  @Test
  public void testIds() throws Exception {
    IdImpl id = new IdImpl("c4f91f2f-bfac-43db-b3b3-8de233dad462");
    Assert.assertEquals("c4f91f2f-bfac-43db-b3b3-8de233dad462", id.toString());
  }
}
