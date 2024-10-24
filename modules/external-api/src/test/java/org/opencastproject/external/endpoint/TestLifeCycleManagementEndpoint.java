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
package org.opencastproject.external.endpoint;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyAccessControlEntryImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Path;

@Path("")
@Ignore
public class TestLifeCycleManagementEndpoint extends LifeCycleManagementEndpoint {

  public TestLifeCycleManagementEndpoint() throws Exception {
    this.endpointBaseUrl = "https://api.opencast.org";

    String policyId = "42";
    String missingPolicyId = "4444";
    String unauthorizedPolicyId = "1";

    LifeCyclePolicyAccessControlEntryImpl accessControlEntry1 = createNiceMock(LifeCyclePolicyAccessControlEntryImpl.class);
    expect(accessControlEntry1.getId()).andReturn(0L);
    expect(accessControlEntry1.isAllow()).andReturn(true);
    expect(accessControlEntry1.getRole()).andReturn("ROLE_USER_BOB");
    expect(accessControlEntry1.getAction()).andReturn("read");
    replay(accessControlEntry1);

    LifeCyclePolicy policy = createNiceMock(LifeCyclePolicy.class);
    expect(policy.getId()).andReturn(policyId).anyTimes();
    expect(policy.getTitle()).andReturn("title").anyTimes();
    expect(policy.getTargetType()).andReturn(TargetType.EVENT).anyTimes();
    expect(policy.getAction()).andReturn(Action.START_WORKFLOW).anyTimes();
    expect(policy.getActionParameters()).andReturn("{ workflowId: noop }").anyTimes();
    expect(policy.getActionDate()).andReturn(new Date(1701361007521L)).anyTimes();
    expect(policy.getTiming()).andReturn(Timing.SPECIFIC_DATE).anyTimes();
    expect(policy.getTargetFilters()).andReturn(Maps.newHashMap()).anyTimes();
    expect(policy.getAccessControlEntries()).andReturn(Lists.newArrayList(accessControlEntry1)).anyTimes();
    replay(policy);

    List<LifeCyclePolicy> policies = new ArrayList<>();
    policies.add(policy);

    LifeCycleService service = createNiceMock(LifeCycleService.class);
    expect(service.getLifeCyclePolicyById(policyId)).andReturn(policy);
    expect(service.getLifeCyclePolicyById(null)).andThrow(new IllegalStateException());
    expect(service.getLifeCyclePolicies(100, 0, new SortCriterion("", SortCriterion.Order.None))).andReturn(policies);
    expect(service.createLifeCyclePolicy(anyObject(LifeCyclePolicy.class))).andReturn(policy);
    expect(service.updateLifeCyclePolicy(anyObject(LifeCyclePolicy.class))).andReturn(true);
    expect(service.deleteLifeCyclePolicy(policyId)).andReturn(true);

    expect(service.getLifeCyclePolicyById(missingPolicyId)).andThrow(new NotFoundException());
    expect(service.deleteLifeCyclePolicy(missingPolicyId)).andThrow(new NotFoundException());

    expect(service.getLifeCyclePolicyById(unauthorizedPolicyId)).andThrow(new UnauthorizedException(""));
    expect(service.deleteLifeCyclePolicy(unauthorizedPolicyId)).andThrow(new UnauthorizedException(""));

    replay(service);

    setLifeCycleService(service);
  }
}
