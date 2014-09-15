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

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.util.doc.DocData;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds data to be displayed in an email message. The following data will be available: mediaPackage, workflow,
 * workflowConfig: workflow configuration as key-value pairs, catalogs: hash of catalogs whose key is the catalog flavor
 * sub-type e.g. "series", "episode", failedOperation: the last operation marked as "failOnError" that failed.
 * 
 * @author Rute Santos
 * 
 */
public class EmailData extends DocData {

  private WorkflowInstance workflow;

  private MediaPackage mediaPackage;

  // "episode" --> { "creator": "Rute Santos", "type": "L05", "isPartOf": "20140224038"... }
  // "series" --> { "creator": "Harvard", "identifier": 20140224038"... }
  private HashMap<String, HashMap<String, String>> catalogs = new HashMap<String, HashMap<String, String>>();

  private static final String DEFAULT_DELIMITER_FOR_MULTIPLE = ",";

  /**
   * Create a new EmailData object, which will make workflow and media package data accessible for being displayed in an
   * email.
   * 
   * @param name
   *          the name of the document (must be alphanumeric (includes _) and no spaces or special chars)
   * @param wf
   *          the workflow instance
   * @param ws
   *          the workspace
   */
  public EmailData(String name, WorkflowInstance wf, Workspace ws) {
    super(name, null, null);

    workflow = wf;
    mediaPackage = wf.getMediaPackage();
    initCatalogs(ws);
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
    m.put("workflowConfig", initWorkflowConfiguration());
    m.put("catalogs", catalogs);
    m.put("failedOperation", findFailedOperation()); // Null if no errors
    return m;
  }

  /**
   * Initializes the map with all fields from the dublin core catalogs.
   */
  private void initCatalogs(Workspace workspace) {

    Catalog[] dcs = mediaPackage.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE);

    for (int i = 0; dcs != null && i < dcs.length; i++) {
      DublinCoreCatalog dc = null;
      InputStream in = null;
      try {
        File f = workspace.get(dcs[i].getURI());
        in = new FileInputStream(f);
        dc = new DublinCoreCatalogImpl(in);
      } catch (Exception e) {
        // What to do here???????
        continue;
      } finally {
        IOUtils.closeQuietly(in);
      }

      if (dc != null) {
        String catalogFlavor = dcs[i].getFlavor().getSubtype();
        HashMap<String, String> catalogHash = new HashMap<String, String>();
        for (EName ename : dc.getProperties()) {
          String name = ename.getLocalName();
          catalogHash.put(name, dc.getAsText(ename, DublinCore.LANGUAGE_ANY, DEFAULT_DELIMITER_FOR_MULTIPLE));
        }
        catalogs.put(catalogFlavor, catalogHash);
      }
    }
  }

  private Map<String, String> initWorkflowConfiguration() {
    Map<String, String> wfConfiguration = new HashMap<String, String>();
    for (String key : workflow.getConfigurationKeys()) {
      wfConfiguration.put(key, workflow.getConfiguration(key));
    }
    return wfConfiguration;
  }

  private WorkflowOperationInstance findFailedOperation() {
    ArrayList<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>(workflow.getOperations());
    // Current operation is the email operation
    WorkflowOperationInstance emailOp = workflow.getCurrentOperation();
    // Look for the last operation that is in failed state and has failOnError true
    int i = operations.indexOf(emailOp) - 1;
    WorkflowOperationInstance op = null;
    for (; i >= 0; i--) {
      op = operations.get(i);
      if (OperationState.FAILED.equals(op.getState()) && op.isFailWorkflowOnException()) {
        break; // Found!
      }
    }
    if (i >= 0) {
      return op;
    }
    return null;
  }

}
