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
package org.opencastproject.util.data.functions;

import org.junit.Test;
import org.opencastproject.util.data.Either;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.functions.Eithers.flatten;

public class EithersTest {
  @Test
  public void testFlatten() throws Exception {
    {
      Either<String, Either<String, Integer>> e = left("error");
      assertEquals("error", e.left().value());
    }
    {
      Either<String, Either<String, Integer>> e = right(Either.<String, Integer>left("error"));
      assertTrue(flatten(e).isLeft());
      assertEquals("error", flatten(e).left().value());
    }
    {
      Either<String, Either<String, Integer>> e = right(Either.<String, Integer>right(1));
      assertTrue(flatten(e).isRight());
      assertEquals(new Integer(1), flatten(e).right().value());
    }
  }
}
