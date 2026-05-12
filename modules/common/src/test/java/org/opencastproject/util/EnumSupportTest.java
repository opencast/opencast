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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opencastproject.util.EnumSupport.parseEnum;

import org.junit.Test;

import java.util.Optional;

public class EnumSupportTest {
  public enum Mushroom {
    flyAgaric, chanterelle, boletus, kingOysterMushroom, deathCap
  }
  @Test
  public void testParseEnum() throws Exception {
    assertEquals(Optional.of(Mushroom.chanterelle), parseEnum(Mushroom.flyAgaric).apply("chanterelle"));
    assertEquals(Optional.of(Mushroom.deathCap), parseEnum(Mushroom.flyAgaric).apply("deathCap"));
    assertNotEquals(Optional.of(Mushroom.kingOysterMushroom), parseEnum(Mushroom.flyAgaric).apply("flyAgaric"));
    assertEquals(Optional.empty(), parseEnum(Mushroom.flyAgaric).apply("pilz"));
  }
}
