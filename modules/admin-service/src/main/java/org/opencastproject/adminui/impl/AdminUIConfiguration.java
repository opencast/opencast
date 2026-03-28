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
package org.opencastproject.adminui.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component(
  immediate = true,
  service = AdminUIConfiguration.class,
  property = {
    "service.description=Admin UI - Configuration",
    "service.pid=org.opencastproject.adminui"
  }
)
public class AdminUIConfiguration {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AdminUIConfiguration.class);

  public static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  private static final String OPT_RETRACT_WORKFLOW_ID = "retract.workflow.id";
  private static final String OPT_MATCH_MANAGED_ACL_ROLE_PREFIXES = "match.managed.acl.role.prefixes";

  private static final String DEFAULT_PREVIEW_SUBTYPE = "preview";
  private static final String DEFAULT_RETRACT_WORKFLOW_ID = "delete";
  private static final String DEFAULT_MATCH_MANAGED_ACL_ROLE_PREFIXES = "";

  private String previewSubtype = DEFAULT_PREVIEW_SUBTYPE;
  private String retractWorkflowId = DEFAULT_RETRACT_WORKFLOW_ID;
  private List<String> matchManagedAclRolePrefixes = new ArrayList<>();

  public String getPreviewSubtype() {
    return previewSubtype;
  }

  public String getRetractWorkflowId() {
    return retractWorkflowId;
  }

  public List<String> getMatchManagedAclRolePrefixes() { return matchManagedAclRolePrefixes; }

  @Activate
  @Modified
  public void modified(Map<String, Object> properties) {
    if (properties == null) {
      properties = Map.of();
    }

    // Preview subtype
    previewSubtype = Objects.toString(properties.get(OPT_PREVIEW_SUBTYPE), DEFAULT_PREVIEW_SUBTYPE);
    logger.debug("Preview subtype configuration set to '{}'", previewSubtype);

    // Retract workflow ID
    retractWorkflowId = Objects.toString(properties.get(OPT_RETRACT_WORKFLOW_ID), DEFAULT_RETRACT_WORKFLOW_ID);
    logger.debug("Retract workflow ID set to {}", retractWorkflowId);

    // Managed ACL role prefix
    String tmp = Objects.toString(properties.get(OPT_MATCH_MANAGED_ACL_ROLE_PREFIXES),
            DEFAULT_MATCH_MANAGED_ACL_ROLE_PREFIXES);
    matchManagedAclRolePrefixes = Arrays.asList(tmp.split(","));
    logger.debug("Match managed acl role prefixes set to {}", matchManagedAclRolePrefixes);
  }
}
