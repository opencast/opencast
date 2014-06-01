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
package org.opencastproject.workflow.api;

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple workflow listener implementation suitable for monitoring a workflow's state changes.
 */
public class WorkflowStateListener implements WorkflowListener {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowStateListener.class);

  /** The workflow instance identifiers to monitor */
  protected final Set<Long> workflowInstanceIds;

  /** The states that this listener respond to with a notify() */
  protected final Map<WorkflowState, AtomicInteger> notifyStates;

  protected AtomicInteger total = new AtomicInteger(0);

  /**
   * Constructs a workflow listener that notifies for any state change to any workflow instance.
   */
  public WorkflowStateListener() {
    workflowInstanceIds = Collections.unmodifiableSet(new HashSet<Long>());
    notifyStates = Collections.unmodifiableMap(new HashMap<WorkflowState, AtomicInteger>());
  }

  /**
   * Constructs a workflow listener that notifies for any state change to a single workflow instance.
   *
   * @param workflowInstanceId
   *          the workflow identifier
   */
  public WorkflowStateListener(Long workflowInstanceId) {
    Set<Long> ids = new HashSet<Long>();
    if (workflowInstanceId != null) {
      ids.add(workflowInstanceId);
    }
    workflowInstanceIds = Collections.unmodifiableSet(ids);
    notifyStates = Collections.unmodifiableMap(new HashMap<WorkflowState, AtomicInteger>());
  }

  /**
   * Constructs a workflow listener for a single workflow instance. The listener may be configured to be notified on
   * state changes.
   *
   * @param workflowInstanceId
   *          the workflow identifier
   * @param statess
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(Long workflowInstanceId, WorkflowState... states) {
    this(new HashSet<Long>(Arrays.asList(new Long[] { workflowInstanceId })), states);
  }

  /**
   * Constructs a workflow listener for all workflow instances. The listener may be configured to be notified on a set
   * of specific state changes.
   *
   * @param states
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(WorkflowState... states) {
    this(new HashSet<Long>(), states);
  }

  /**
   * Constructs a workflow listener for all workflow instances. The listener may be configured to be notified on a of
   * specific state changes.
   *
   * @param staet
   *          the workflow state change that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(WorkflowState state) {
    this(new HashSet<Long>(), new WorkflowState[] { state });
  }

  /**
   * Constructs a workflow listener for a set of workflow instances. The listener may be configured to be notified on a
   * set of specific state changes.
   *
   * @param workflowInstanceIds
   *          the workflow identifiers
   * @param states
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(Set<Long> workflowInstanceIds, WorkflowState... states) {
    this.workflowInstanceIds = Collections.unmodifiableSet(workflowInstanceIds);

    if (states == null) {
      notifyStates = Collections.unmodifiableMap(new HashMap<WorkflowState, AtomicInteger>());
    } else {
      Map<WorkflowState, AtomicInteger> map = new HashMap<WorkflowState, AtomicInteger>();
      for (WorkflowState state : states) {
        map.put(state, new AtomicInteger(0));
      }
      notifyStates = Collections.unmodifiableMap(map);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowListener#operationChanged(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void operationChanged(WorkflowInstance workflow) {
    synchronized (this) {
      logger.debug("No-op");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowListener#stateChanged(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void stateChanged(WorkflowInstance workflow) {
    synchronized (this) {

      if (!workflowInstanceIds.isEmpty() && !workflowInstanceIds.contains(workflow.getId()))
        return;

      WorkflowState currentState = workflow.getState();
      if (!notifyStates.isEmpty() && !notifyStates.containsKey(currentState))
        return;

      if (notifyStates.containsKey(currentState)) {
        notifyStates.get(currentState).incrementAndGet();
      }

      total.incrementAndGet();

      logger.debug("Workflow {} state updated to {}", workflow.getId(), workflow.getState());
      notifyAll();
    }
  }

  /**
   * Returns the number of state changes that this listener has observed without ignoring.
   *
   * @return the counter
   */
  public int countStateChanges() {
    synchronized (this) {
      return total.get();
    }
  }

  /**
   * Returns the number of state changes that this listener has observed without ignoring.
   *
   * @return the counter
   */
  public int countStateChanges(WorkflowState state) {
    synchronized (this) {
      if (!notifyStates.containsKey(state))
        throw new IllegalArgumentException("State '" + state + "' is not being monitored");
      return notifyStates.get(state).get();
    }
  }

}
