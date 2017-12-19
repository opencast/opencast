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
package org.opencastproject.assetmanager.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.entwinemedia.fn.data.SetB;

import org.junit.Test;

import java.util.Set;
import java.util.UUID;

public class PropertyTest {
  @Test
  public void testEquality() throws Exception {
    for (int i = 0; i < 100; i++) {
      final String id = UUID.randomUUID().toString();
      final String ns = UUID.randomUUID().toString();
      final String n = UUID.randomUUID().toString();
      assertEquals(Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(true)),
                   Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(true)));
      assertNotEquals(Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(true)),
                      Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(false)));
      final Set<Property> p = SetB.IH.mk(
              Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(true)),
              Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(10L)));
      assertEquals(2, p.size());
      assertTrue(p.contains(Property.mk(PropertyId.mk(id, PropertyName.mk(ns, n)), Value.mk(true))));
    }
  }
}
