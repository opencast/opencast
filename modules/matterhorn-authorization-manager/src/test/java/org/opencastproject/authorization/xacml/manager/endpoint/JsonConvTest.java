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
package org.opencastproject.authorization.xacml.manager.endpoint;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.authorization.xacml.manager.impl.TransitionResultImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.path.json.JsonPath.from;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.security.api.AccessControlUtil.acl;
import static org.opencastproject.security.api.AccessControlUtil.entry;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

public final class JsonConvTest {
  private static final ManagedAcl macl = new ManagedAcl() {
    @Override public Long getId() {
      return 1L;
    }

    @Override public String getOrganizationId() {
      return "default_org";
    }

    @Override public String getName() {
      return "Public";
    }

    @Override public AccessControlList getAcl() {
      return acl(entry("anonymous", "read", true));
    }
  };

  private static final Date now = new Date();

  private static final TransitionResult tresult = new TransitionResultImpl(
          Collections.<EpisodeACLTransition>list(new EpisodeACLTransition() {
            @Override public String getEpisodeId() {
              return "episode";
            }

            @Override public Option<ManagedAcl> getAccessControlList() {
              return some(macl);
            }

            @Override public boolean isDelete() {
              return getAccessControlList().isNone();
            }

            @Override public long getTransitionId() {
              return 1L;
            }

            @Override public String getOrganizationId() {
              return "org";
            }

            @Override public Date getApplicationDate() {
              return now;
            }

            @Override public Option<ConfiguredWorkflowRef> getWorkflow() {
              return none();
            }

            @Override public boolean isDone() {
              return false;
            }
          }),
          Collections.<SeriesACLTransition>list(new SeriesACLTransition() {
            @Override public String getSeriesId() {
              return "series";
            }

            @Override public ManagedAcl getAccessControlList() {
              return macl;
            }

            @Override public boolean isOverride() {
              return true;
            }

            @Override public long getTransitionId() {
              return 2L;
            }

            @Override public String getOrganizationId() {
              return "org";
            }

            @Override public Date getApplicationDate() {
              return now;
            }

            @Override public Option<ConfiguredWorkflowRef> getWorkflow() {
              return none();
            }

            @Override public boolean isDone() {
              return false;
            }
          })
  );

  @Test
  public void testManagedAclFull() {
    String json = JsonConv.full(macl).toJson();
    System.out.println(json);
    JsonPath jp = from(json);
    assertEquals(4, ((Map) jp.get()).size());
    assertEquals(1, jp.get("id"));
    assertEquals("default_org", jp.get("organizationId"));
    assertEquals("Public", jp.get("name"));
    assertEquals(1, ((List) jp.get("acl.ace")).size());
  }

  @Test
  public void testManagedAclDigest() {
    String json = JsonConv.digest(macl).toJson();
    JsonPath jp = from(json);
    System.out.println(json);
    assertEquals(2, ((Map) jp.get()).size());
    assertEquals(1, jp.get("id"));
    assertEquals("Public", jp.get("name"));
  }

  @Test
  public void testTransitionResultDigest() {
    String json = JsonConv.digest(tresult).toJson();
    System.out.println(json);
    JsonPath jp = from(json);
  }
}
