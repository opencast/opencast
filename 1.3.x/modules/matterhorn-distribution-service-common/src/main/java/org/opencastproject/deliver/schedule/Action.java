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
package org.opencastproject.deliver.schedule;

import org.opencastproject.deliver.store.InvalidKeyException;

import flexjson.JSONSerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Abstract class of periodic actions. The 'execute' method is called
 * to execute the action on a thread pool thread. An action may call
 * 'resumeAfter' to schedule a new call execute after a specified number
 * of seconds, or may call 'succeed' or 'fail' to report on the final
 * resolution of the action.</p>
 *
 * <p>Action objects need not be thread safe as only one thread will call
 * 'execute' at a time. Each action has a Task object as a wrapper that
 * is thread safe and carries out all of the work of scheduling.</p>
 *
 * <p>Actions may throw any exception (terminating any further execution),
 * or call InvalidException (if the Actions's initial state is invalid),
 * FiledException (to signal that the Action failed and cannot be
 * accomplished), or RetryException to cause a retry after a specified
 * period if the Task has not exceeded its retry count or outlived its
 * deadline.</p>
 *
 * <p>The Action class also includes convenience methods for logging and
 * setting the Task's status message and specifying the execution deadline.
 * Note that all Actions must be serializable using the selected storage
 * system.</p>
 *
 * @author Jonathan A. Smith
 */

public abstract class Action implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2600233233804117511L;

    /** Default number of seconds allowed for action. */
    public static final long DEFAULT_TASK_SECONDS = 60L * 60L * 5L;

    /** Default number of times to retry on a RetryException. */
    public static final int DEFAULT_RETRIES = 3;

    /** Task name. */
    private String name;

    /** Task that owns this Action while executing. */
    private Task task;

    /**
     * Constructs an Action.
     */

    public Action() {
    }
    
    /**
     * Constructs an Action with a specified name.
     * 
     * @param name Action name
     */

    public Action(String name) {
        this.name = name;
    }

    /**
     * Creates a new task to execute this Action.
     *
     * @return Task task used to execute this Action
     */

    public Task makeTask() {
        Task new_task = new Task();
        new_task.setAction(this);
        new_task.setDeadline(
            new Date(System.currentTimeMillis() + deadlineSeconds() * 1000L));

        return new_task;
    }

    /**
     * Returns the number of seconds allowed for this Action to complete.
     *
     * @return Maximum seconds until task deadline
     */

    protected long deadlineSeconds() {
        return DEFAULT_TASK_SECONDS;
    }

    /**
     * Returns the number of times the Action will be re-tried in case a
     * RetryException is thrown.
     *
     * @return number of times to retry
     */

    protected int retryLimit() {
        return DEFAULT_RETRIES;
    }

    // **** Accessors

    /**
     * Sets the associated task when the action is running.
     *
     * @param task Task associated with this Action.
     */

    public void setTask(Task task) {
        this.task = task;
    }

    /**
     * Sets the task's name (or id).
     *
     * @param name Task name
     */

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the Task's name or id.
     *
     * @return Task name
     */

    public String getName() {
        return name;
    }

    // **** Execution

    /**
     * Execute this action.
     */

    protected abstract void execute() throws Exception;

    /**
     * Method called first time Action is executed. Override if needed.
     */

    protected void start() {
    }

    /**
     * Method called after last execution. Override if needed.
     */
    
    protected void finish() {
    }

    // **** Callbacks

    /**
     * Sets the delay before the Task is resumed. Note that this sets a value,
     * if called more than once it will just reset the value used in
     * rescheduling. There is no way to schedule this Task more than once.
     *
     * @param seconds time delay in seconds
     */

    protected void resumeAfter(long seconds) {
        task.resumeAfter(seconds);
    }

    /**
     * Sets the human-readable Task status message.
     *
     * @param status Human readable Task status message
     * @throws InvalidKeyException 
     */

    protected void status(String status) throws InvalidKeyException {
        task.setStatus(status);
    }

    /**
     * Signals that the Task has succeeded.
     * @throws InvalidKeyException 
     */
    
    protected void succeed() throws InvalidKeyException {
        task.succeed();
    }

    /**
     * Signals that the Task has succeeded.
     *
     * @param message message indicating success
     * @throws InvalidKeyException 
     */

    protected void succeed(String message) throws InvalidKeyException {
        task.succeed(message);
    }

    /**
     * Logs a message relating to this task. Note that this should not be used
     * when an error occurs. On an error, throw a RetryException if the task
     * should be retired, or another Exception if the error should cause the
     * Task to fail.
     *
     * @param message message to be logged
     */

    protected void log(String message) {
        task.log(message);
    }

    /**
     * Finds and returns a task by name.
     *
     * @param name name of task to find.
     * @return Task object or nil if not found.
     * @throws InvalidKeyException 
     */

    protected Task getTaskNamed(String name) throws InvalidKeyException {
        return task.getTaskNamed(name);
    }

    // **** String Representation

    /**
     * Returns a String representation of an Action.
     * @return String
     */

    @Override
    public String toString() {
        return new JSONSerializer().serialize(this);
    }


}
