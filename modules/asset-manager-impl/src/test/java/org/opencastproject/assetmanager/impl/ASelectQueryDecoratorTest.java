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
package org.opencastproject.assetmanager.impl;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.assetmanager.api.query.ASelectQuery;

import org.junit.Test;

public class ASelectQueryDecoratorTest extends AbstractAssetManagerTestBase {
  /**
   * Test that returned queries are also decorated.
   */
  @Test
  public void testDecoration() throws Exception {
    final ASelectQuery delegate = q.select();
    final ASelectQuery s = new ASelectQueryDecorator(delegate);
    assertThat(s.where(q.always()), instanceOf(ASelectQueryDecorator.class));
    assertThat(s.orderBy(q.organizationId().asc()), instanceOf(ASelectQueryDecorator.class));
    assertThat(s.page(0, 1), instanceOf(ASelectQueryDecorator.class));
    assertNotEquals(delegate.getClass(), s.getClass());
  }
}
