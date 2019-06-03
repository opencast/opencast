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

package org.opencastproject.workflow.api;

import org.opencastproject.mediapackage.MediaPackage;

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An single instance of a running, paused, or stopped workflow. WorkflowInstance objects are snapshots in time for a
 * particular workflow. They are not thread-safe, and will not be updated by other threads.
 */
@XmlJavaTypeAdapter(WorkflowInstanceImpl.Adapter.class)
public interface WorkflowInstance extends Configurable {

  enum WorkflowState {
    INSTANTIATED, RUNNING, STOPPED, PAUSED, SUCCEEDED, FAILED, FAILING;

    public boolean isTerminated() {
      switch (this) {
        case STOPPED:
        case SUCCEEDED:
        case FAILED:
          return true;
        default:
          return false;
      }
    }
    public static class Adapter extends XmlAdapter<String, WorkflowState> {

      @Override
      public String marshal(WorkflowState workflowState) {
        return workflowState == null ? null : workflowState.toString().toLowerCase();
      }

      @Override
      public WorkflowState unmarshal(String val) {
        return val == null ? null : WorkflowState.valueOf(val.toUpperCase());
      }

    }
  }

  /**
   * The unique ID of this {@link WorkflowInstance}.
   */
  long getId();

  /**
   * Sets the workflow identifier.
   *
   * @param id
   *          the identifier
   */
  void setId(long id);

  /**
   * The short title of the workflow definition used to create this workflow instance.
   */
  String getTitle();

  /**
   * The identifier of the workflow definition used to create this workflow instance.
   */
  String getTemplate();

  /**
   * The longer description of the workflow definition used to create this workflow instance.
   */
  String getDescription();

  /**
   * The parent workflow instance ID, if any.
   */
  Long getParentId();

  /**
   * Returns the username of the user that created this workflow.
   *
   * @return username of the workflow's creator
   */
  String getCreatorName();

  /**
   * Returns the organization that this workflow belongs to.
   *
   * @return the organization
   */
  String getOrganizationId();

  /**
   * Returns a copy of the {@link WorkflowOperationInstance}s that make up this workflow. In order to modify the
   * operations, call setOperations.
   *
   * @return the workflow operations
   */
  List<WorkflowOperationInstance> getOperations();

  /**
   * Sets the list of workflow operations.
   *
   * @param operations
   *          the new list of operations
   */
  void setOperations(List<WorkflowOperationInstance> operations);

  /**
   * Returns the {@link WorkflowOperationInstance} that is currently either in {@link WorkflowState#RUNNING} or
   * {@link WorkflowState#PAUSED}.
   *
   * @return the current operation
   * @throws IllegalStateException
   *           if the workflow instance has no operations
   */
  WorkflowOperationInstance getCurrentOperation() throws IllegalStateException;

  /**
   * The current {@link WorkflowState} of this {@link WorkflowInstance}.
   */
  WorkflowState getState();

  /**
   * Set the state of the workflow.
   *
   * @param state
   *          the new workflow state
   */
  void setState(WorkflowState state);

  /**
   * @return True if the workflow has not finished, been stopped or failed.
   */
  boolean isActive();

  /**
   * The {@link MediaPackage} being worked on by this workflow instance.
   */
  MediaPackage getMediaPackage();

  /**
   * Returns the next operation, and marks it as current. If there is no next operation, this method will return null.
   */
  WorkflowOperationInstance next();

  /**
   * Return whether there is another operation after the current operation. If there is no next operation, this will
   * return null.
   */
  boolean hasNext();

  /**
   * Set the media package this workflow instance is processing.
   */
  void setMediaPackage(MediaPackage mp);

  /**
   * Appends the operations found in the workflow definition to the end of this workflow instance.
   *
   * @param workflowDefinition
   *          the workflow definition
   */
  void extend(WorkflowDefinition workflowDefinition);

  /**
   * Insert the operations found in the workflow definition after the operation <code>after</code>.
   * This allows to include a different workflow at any point. This method is a generalization
   * of {@link #extend(org.opencastproject.workflow.api.WorkflowDefinition)}.
   *
   * @param workflowDefinition
   *          the workflow to insert
   * @param after
   *          insert the given workflow after this operation
   */
  void insert(WorkflowDefinition workflowDefinition, WorkflowOperationInstance after);
}
