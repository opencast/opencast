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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.assetmanager.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

import org.junit.Test

import java.util.UUID

class PropertyIdTest {
    @Test
    @Throws(Exception::class)
    fun testEquality() {
        for (i in 0..99) {
            val id = UUID.randomUUID().toString()
            val ns = UUID.randomUUID().toString()
            val p = UUID.randomUUID().toString()
            assertEquals(PropertyId.mk(id, PropertyName.mk(ns, p)), PropertyId.mk(id, PropertyName.mk(ns, p)))
            assertEquals(PropertyId.mk(id, PropertyName.mk(ns, p)), PropertyId.mk(id, ns, p))
            assertEquals(PropertyId.mk(id, ns, p), PropertyId.mk(id, PropertyName.mk(ns, p)))
            assertNotEquals(PropertyId.mk("id", PropertyName.mk("ns", "p")), PropertyId.mk(id, ns, p))
        }
    }
}
