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

package org.opencastproject.index.service.util;

import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.authorization.xacml.manager.api.ACLTransition;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class around access information like ACL.
 */
public final class AccessInformationUtil {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AccessInformationUtil.class);

  private AccessInformationUtil() {
  }

  /**
   * Serializes a {@link ManagedAcl} as {@link JSONObject}. The JSON structure will look like this
   *
   * <pre>
   * {
   *   "id": 56739,
   *   "name": "ACL name"
   * }
   * </pre>
   *
   * @param managedAcl
   *          the ACL to serialize
   * @return the ACL as JSON object
   * @throws IllegalArgumentException
   *           if the <code>managedAcl</code> parameter is null
   */
  public static JSONObject serializeManagedAcl(ManagedAcl managedAcl) {
    if (managedAcl == null)
      throw new IllegalArgumentException("The parameter managedAcl must not be null");

    JSONObject systemAclJson = new JSONObject();

    try {
      systemAclJson.put("id", managedAcl.getId());
      systemAclJson.put("name", managedAcl.getName());
    } catch (JSONException e) {
      // This should never happen, because the key is never null
      logger.error("An unexpected error occured: {}", e);
    }

    return systemAclJson;
  }

  /**
   * Serialize a {@link ACLTransition} as {@link JSONObject}. The JSON structure will look like this
   *
   * <pre>
   * {
   *   "id": 37494,
   *   "application_date": "2014-05-15T11:12:13Z",
   *   "workflow_id": "custom_workflow",
   *   "done": true,
   * }
   * </pre>
   *
   * @param trans
   *          the transition to serialize
   * @return the transition as JSON object
   * @throws IllegalArgumentException
   *           if the <code>trans</code> parameter is null
   */
  public static JSONObject serializeACLTransition(ACLTransition trans) {
    if (trans == null)
      throw new IllegalArgumentException("The parameter trans must not be null");

    JSONObject transJson = new JSONObject();
    try {
      transJson.put("id", trans.getTransitionId());
      transJson.put("application_date", DateTimeSupport.toUTC(trans.getApplicationDate().getTime()));
      if (trans.getWorkflow().isSome())
        transJson.put("workflow_id", trans.getWorkflow().get().getWorkflowId());
      transJson.put("done", trans.isDone());
    } catch (JSONException e) {
      // This should never happen, because the key is never null
      logger.error("An unexpected error occured: {}", e);
      throw new IllegalStateException(e);
    }

    return transJson;
  }

  /**
   * Serialize a {@link SeriesACLTransition} as {@link JSONObject}. The JSON structure will look like this
   *
   * <pre>
   * {
   *   "id": 37494,
   *   "application_date": "2014-05-15T11:12:13Z",
   *   "workflow_id": "custom_workflow",
   *   "done": true,
   *   "acl_id": 1894,
   *   "override_episodes": true
   * }
   * </pre>
   *
   * @param trans
   *          the transition to serialize
   * @return the transition as JSON object
   * @throws IllegalArgumentException
   *           if the <code>trans</code> parameter is null
   */
  public static JSONObject serializeSeriesACLTransition(SeriesACLTransition trans) {
    JSONObject transJson = serializeACLTransition((ACLTransition) trans);
    try {
      if (trans.getAccessControlList() != null)
        transJson.put("acl_id", trans.getAccessControlList().getId());
      transJson.put("override_episodes", trans.isOverride());
    } catch (JSONException e) {
      // This should never happen, because the key is never null
      logger.error("An unexpected error occured: {}", e);
      throw new IllegalStateException(e);
    }

    return transJson;
  }

  /**
   * Serialize a {@link EpisodeACLTransition} as {@link JSONObject}. The JSON structure will look like this
   *
   * <pre>
   * {
   *   "id": 37494,
   *   "application_date": "2014-05-15T11:12:13Z",
   *   "workflow_id": "custom_workflow",
   *   "done": true,
   *   "acl_id": 1894,
   *   "is_deleted": true
   * }
   * </pre>
   *
   * @param trans
   *          the transition to serialize
   * @return the transition as JSON object
   * @throws IllegalArgumentException
   *           if the <code>trans</code> parameter is null
   */
  public static JSONObject serializeEpisodeACLTransition(EpisodeACLTransition trans) {
    JSONObject transJson = serializeACLTransition((ACLTransition) trans);
    try {
      if (trans.getAccessControlList().isSome())
        transJson.put("acl_id", trans.getAccessControlList().get().getId());
      transJson.put("is_deleted", trans.isDelete());
    } catch (JSONException e) {
      // This should never happen, because the key is never null
      logger.error("An unexpected error occured: {}", e);
      throw new IllegalStateException(e);
    }

    return transJson;
  }

  /**
   * Serialize a {@link AccessControlList} as {@link JSONObject}. The JSON structure will look like this
   *
   * <pre>
   * {
   *   "ROLE_STUDENT": {
   *     "read": true,
   *     "write": false
   *   },
   *   "ROLE_TEACHER": {
   *     "read": true,
   *     "write": true
   *   }
   * }
   * </pre>
   *
   * @param acl
   *          the access control list to serialize
   * @return the acl as JSON object
   * @throws IllegalArgumentException
   *           if the <code>acl</code> parameter is null
   */
  public static JSONObject serializePrivilegesByRole(AccessControlList acl) {
    if (acl == null)
      throw new IllegalArgumentException("The parameter trans must not be null");

    Map<String, JSONObject> privilegesByRole = new HashMap<String, JSONObject>();
    for (AccessControlEntry entry : acl.getEntries()) {
      JSONObject rolePrivileges;
      if (privilegesByRole.containsKey(entry.getRole())) {
        rolePrivileges = privilegesByRole.get(entry.getRole());
      } else {
        rolePrivileges = new JSONObject();
        privilegesByRole.put(entry.getRole(), rolePrivileges);
      }
      try {
        rolePrivileges.put(entry.getAction(), entry.isAllow());
      } catch (JSONException e) {
        // This should never happen, because the key is never null
        logger.error("An unexpected error occured: {}", e);
      }
    }

    JSONObject privilegesJson = new JSONObject();
    for (Entry<String, JSONObject> privilege : privilegesByRole.entrySet()) {
      try {
        privilegesJson.put(privilege.getKey(), privilege.getValue());
      } catch (JSONException e) {
        // This should never happen, because the key is never null
        logger.error("An unexpected error occured: {}", e);
      }
    }
    return privilegesJson;
  }

  /**
   * Matches the given ACL against the given list of managed ACLs returning the first match.
   *
   * @param acls
   *          the list of managed ACLs
   * @param acl
   *          the ACL to search
   * @return an {@link Option} wrapping the matching ACL or none if not found
   */
  public static Option<ManagedAcl> matchAcls(List<ManagedAcl> acls, final AccessControlList acl) {
    return mlist(acls).find(new Predicate<ManagedAcl>() {
      @Override
      public Boolean apply(ManagedAcl macl) {
        return AccessControlUtil.equals(acl, macl.getAcl());
      }
    });
  }

}
