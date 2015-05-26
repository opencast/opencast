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

import java.util.Map;

/**
 * The result of a workflow operation.
 */
public interface WorkflowOperationResult {
  public enum Action {
    CONTINUE, PAUSE, SKIP
  }

  /**
   * @return The media package that results from the execution of a workflow operation.
   */
  MediaPackage getMediaPackage();

  /**
   * Operations may optionally set properties on a workflow operation.
   *
   * @return The properties to set
   */
  Map<String, String> getProperties();

  /**
   * Sets the action to take.
   *
   * @param action
   *          the action
   */
  void setAction(Action action);

  /**
   * Operations may optionally request that the workflow be placed in a certain state.
   *
   * @return The action that the workflow service should take on this workflow instance.
   */
  Action getAction();

  /**
   * Specifies whether the operation should be continuable by the user.
   *
   * @param isContinuable
   */
  void setAllowsContinue(boolean isContinuable);

  /**
   * Returns <code>true</code> if this operation can be continued by the user from an optional hold state. This value is
   * only considered if the action returned by this result equals {@link Action#PAUSE}.
   *
   * @return <code>true</code> if a paused operation should be continuable
   */
  boolean allowsContinue();

  /**
   * Specifies whether the operation should be abortable by the user.
   *
   * @param isAbortable
   */
  void setAllowsAbort(boolean isAbortable);

  /**
   * Returns <code>true</code> if this operation can be canceled by the user from an optional hold state. This value is
   * only considered if the action returned by this result equals {@link Action#PAUSE}.
   *
   * @return <code>true</code> if a paused operation should be abortable
   */
  boolean allowsAbort();

  /**
   * The number of milliseconds this operation sat in a queue before finishing.
   *
   * @return The time spent in a queue
   */
  long getTimeInQueue();

}
