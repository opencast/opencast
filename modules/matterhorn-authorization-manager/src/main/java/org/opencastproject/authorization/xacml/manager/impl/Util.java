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

package org.opencastproject.authorization.xacml.manager.impl;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.workflow.api.ConfiguredWorkflowRef.workflow;

import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import org.json.simple.JSONValue;

import java.util.Map;

/**
 * General helper functions.
 */
public final class Util {
  private Util() {
  }

  /**
   * @return none if workflowId is none
   */
  public static Option<ConfiguredWorkflowRef> createConfiguredWorkflowRef(Option<String> workflowId,
          final Option<String> jsonParams) {
    for (String wid : workflowId) {
      for (String p : jsonParams) {
        return some(workflow(wid, (Map<String, String>) JSONValue.parse(p)));
      }
      return some(workflow(wid));
    }
    return none();
  }

  /**
   * @return (workflowId, workflowParamJson)
   */
  public static Tuple<Option<String>, Option<String>> splitConfiguredWorkflowRef(Option<ConfiguredWorkflowRef> workflow) {
    for (ConfiguredWorkflowRef a : workflow) {
      return tuple(some(a.getWorkflowId()), some(JSONValue.toJSONString(a.getParameters())));
    }
    return tuple(Option.<String> none(), Option.<String> none());
  }

  public static final Function<ManagedAcl, AccessControlList> toAcl = new Function<ManagedAcl, AccessControlList>() {
    @Override
    public AccessControlList apply(ManagedAcl managedAcl) {
      return managedAcl.getAcl();
    }
  };

  public static final Function<ResultItem, MediaPackage> getArchiveMp = new Function<ResultItem, MediaPackage>() {
    @Override
    public MediaPackage apply(ResultItem item) {
      return item.getMediaPackage();
    }
  };

  public static Function<Long, Option<ManagedAcl>> getManagedAcl(final AclService aclService) {
    return new Function<Long, Option<ManagedAcl>>() {
      @Override
      public Option<ManagedAcl> apply(Long aclId) {
        return aclService.getAcl(aclId);
      }
    };
  }
}
