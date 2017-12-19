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
package org.opencastproject.email.template.impl;

import org.opencastproject.job.api.Incident;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.doc.DocData;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds data to be displayed in an email message. The following data will be available: mediaPackage, workflow,
 * workflowConfig: workflow configuration as key-value pairs, catalogs: hash of catalogs whose key is the catalog flavor
 * sub-type e.g. "series", "episode", failedOperation: the last operation marked as "failOnError" that failed.
 *
 */
public class EmailData extends DocData {

  private final WorkflowInstance workflow;
  private final Map<String, String> workflowConfig;
  private final MediaPackage mediaPackage;
  // The hash map below looks like this:
  // "episode" --> { "creator": "John Harvard", "type": "L05", "isPartOf": "20140224038"... }
  // "series" --> { "creator": "Harvard", "identifier": "20140224038"... }
  private final HashMap<String, HashMap<String, String>> catalogs;
  private final WorkflowOperationInstance failed;
  private final List<Incident> incidents;

  /**
   * Create the base data object for populating email fields.
   *
   * @param name
   *          a name for this object
   * @param workflow
   *          workflow instance
   * @param catalogs
   *          hash map of media package catalogs
   * @param failed
   *          workflow operation that caused the workflow to fail
   * @param incidents
   *          incidents
   */
  public EmailData(String name, WorkflowInstance workflow, HashMap<String, HashMap<String, String>> catalogs,
          WorkflowOperationInstance failed, List<Incident> incidents) {
    super(name, null, null);

    this.workflow = workflow;
    this.workflowConfig = new HashMap<String, String>();
    for (String key : workflow.getConfigurationKeys()) {
      workflowConfig.put(key, workflow.getConfiguration(key));
    }
    this.mediaPackage = workflow.getMediaPackage();
    this.catalogs = catalogs;
    this.failed = failed;
    this.incidents = incidents;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "EmailDOC:name=" + meta.get("name") + ", notes=" + notes + ", workflow id=" + workflow.getId()
            + ", media package id=" + mediaPackage.getIdentifier();
  }

  @Override
  public Map<String, Object> toMap() throws IllegalStateException {
    LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
    m.put("meta", meta);
    m.put("notes", notes);
    m.put("mediaPackage", mediaPackage);
    m.put("workflow", workflow);
    m.put("workflowConfig", workflowConfig);
    m.put("catalogs", catalogs);
    m.put("failedOperation", failed); // Will be null if no errors
    m.put("incident", incidents); // Will be null if no incidents
    return m;
  }

}
