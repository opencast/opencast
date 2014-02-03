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
package org.opencastproject.kernel.mail;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.doc.DocData;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds data to be displayed in an email message.
 * 
 * @author Rute Santos
 * 
 */
public class EmailDocData extends DocData {

  private WorkflowInstance workflow;

  private MediaPackage mediaPackage;

  /**
   * Create the base data object for creating REST documentation.
   * 
   * @param name
   */
  public EmailDocData(String name, WorkflowInstance wf) {
    super(name, null, null);

    workflow = wf;
    mediaPackage = wf.getMediaPackage();
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
    return m;
  }

}
