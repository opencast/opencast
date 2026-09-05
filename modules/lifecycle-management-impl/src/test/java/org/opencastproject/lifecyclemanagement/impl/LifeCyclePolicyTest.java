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
package org.opencastproject.lifecyclemanagement.impl;

import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LifeCyclePolicyTest {

  @Test
  public void testPolicy() throws Exception {
    LifeCyclePolicy policy = new LifeCyclePolicyImpl();
    policy.setTitle("title");
    Assert.assertEquals(policy.getTitle(), "title");
  }

  @Test
  public void testPlaylistEntries() throws Exception {
    LifeCyclePolicy policy = new LifeCyclePolicyImpl();

    LifeCyclePolicyAccessControlEntry policyEntry =
        new LifeCyclePolicyAccessControlEntryImpl(true, "ROLE_USER_BOB", "READ");
    LifeCyclePolicyAccessControlEntry policyEntry2 =
        new LifeCyclePolicyAccessControlEntryImpl(true, "ROLE_USER_BOB", "WRITE");
    List<LifeCyclePolicyAccessControlEntry> entries = new ArrayList<>();
    entries.add(policyEntry);
    entries.add(policyEntry2);

    policy.setAccessControlEntries(entries);
    Assert.assertEquals(2, policy.getAccessControlEntries().size());

    List<LifeCyclePolicyAccessControlEntry> policyEntries = policy.getAccessControlEntries();
    Assert.assertEquals(policyEntries.get(0).getRole(), "ROLE_USER_BOB");
  }
}
