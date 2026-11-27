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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import javax.ws.rs.Path;

public class RestUtilTest {

  @Path("/annotated")
  private static class AnnotatedEndpoint {
  }

  private static class InheritingEndpoint extends AnnotatedEndpoint {
  }

  @Path("/overriding")
  private static class OverridingEndpoint extends AnnotatedEndpoint {
  }

  private static class NotAnnotated {
  }

  @Test
  public void testServicePathFromAnnotation() {
    assertEquals("/annotated", RestUtil.getServicePath(AnnotatedEndpoint.class));
  }

  @Test
  public void testServicePathFromSuperclass() {
    assertEquals("/annotated", RestUtil.getServicePath(InheritingEndpoint.class));
  }

  @Test
  public void testServicePathOfSubclassWins() {
    assertEquals("/overriding", RestUtil.getServicePath(OverridingEndpoint.class));
  }

  @Test
  public void testServicePathWithoutAnnotation() {
    assertThrows(IllegalArgumentException.class, () -> RestUtil.getServicePath(NotAnnotated.class));
  }

}
