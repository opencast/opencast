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

package org.opencastproject.message.broker.api.workflow;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.workflow.api.WorkflowInstance;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a WorkflowService queue.
 */
public class WorkflowItem implements MessageItem, Serializable {

  private static final long serialVersionUID = -202811055899495045L;

  public static final String WORKFLOW_QUEUE_PREFIX = "WORKFLOW.";

  public static final String WORKFLOW_QUEUE = WORKFLOW_QUEUE_PREFIX + "QUEUE";

  private final String id;
  private final String workflowDefinitionId;
  private final long workflowInstanceId;
  private final String episodeDublincoreCatalog;
  private final String mediaPackage;
  private final String state;
  private final String accessControlListJSON;

  private final Type type;

  public enum Type {
    DeleteInstance, UpdateInstance
  };

  /**
   * @param workflowInstance
   *          The workflow instance to update.
   * @param dublincoreXml
   *          The episode dublincore catalog used for metadata updates
   * @return Builds {@link WorkflowItem} for updating a workflow instance.
   */
  public static WorkflowItem updateInstance(WorkflowInstance workflowInstance, String dublincoreXml,
          AccessControlList accessControlList) {
    return new WorkflowItem(workflowInstance, dublincoreXml, accessControlList);
  }

  /**
   * @param workflowInstanceId
   *          The unique id of the workflow instance to delete.
   * @param workflowInstance
   *          The workflow instance to delete.
   * @return Builds {@link WorkflowItem} for deleting a workflow instance.
   */
  public static WorkflowItem deleteInstance(long workflowInstanceId, WorkflowInstance workflowInstance) {
    return new WorkflowItem(workflowInstanceId, workflowInstance);
  }

  /**
   * Constructor to build an update workflow instance {@link WorkflowItem}.
   *
   * @param workflowInstance
   *          The workflow instance to update.
   */
  public WorkflowItem(WorkflowInstance workflowInstance, String dublincoreXml, AccessControlList accessControlList) {
    this.id = workflowInstance.getMediaPackage().getIdentifier().compact();
    this.workflowDefinitionId = workflowInstance.getTemplate();
    this.workflowInstanceId = workflowInstance.getId();
    this.episodeDublincoreCatalog = dublincoreXml;
    this.mediaPackage = MediaPackageParser.getAsXml(workflowInstance.getMediaPackage());
    this.state = workflowInstance.getState().toString();
    this.accessControlListJSON = AccessControlParser.toJsonSilent(accessControlList);
    this.type = Type.UpdateInstance;
  }

  /**
   * Constructor to build a delete workflow {@link WorkflowItem}.
   *
   * @param workflowInstanceId
   *          The id of the workflow instance to delete.
   * @param workflowInstance
   *          The workflow instance to update.
   */
  public WorkflowItem(long workflowInstanceId, WorkflowInstance workflowInstance) {
    // We just need the media package id and workflow id
    this.id = workflowInstance.getMediaPackage().getIdentifier().compact();
    this.workflowInstanceId = workflowInstanceId;
    this.workflowDefinitionId = null;
    this.episodeDublincoreCatalog = null;
    this.mediaPackage = null;
    this.state = null;
    this.accessControlListJSON = null;
    this.type = Type.DeleteInstance;
  }

  @Override
  public String getId() {
    return id;
  }

  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  public long getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public Type getType() {
    return type;
  }

  public DublinCoreCatalog getEpisodeDublincoreCatalog() {
    if (episodeDublincoreCatalog == null) {
      return null;
    }
    try {
      return DublinCoreXmlFormat.read(episodeDublincoreCatalog);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new IllegalStateException("Unable to parse dublincore catalog", e);
    }
  }

  public MediaPackage getMediaPackage() {
    if (mediaPackage == null) {
      return null;
    }
    try {
      return MediaPackageParser.getFromXml(mediaPackage);
    } catch (MediaPackageException e) {
      throw new IllegalStateException("Could not parse media package XML", e);
    }
  }

  public WorkflowInstance.WorkflowState getState() {
    return WorkflowInstance.WorkflowState.valueOf(state);
  }

  public String getAccessControlListJSON() {
    return accessControlListJSON;
  }
}
