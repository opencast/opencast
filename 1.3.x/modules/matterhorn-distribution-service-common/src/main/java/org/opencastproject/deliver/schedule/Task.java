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

import flexjson.JSON;
import flexjson.JSONSerializer;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * Tasks represent things to be done over a period of time. Note "Task" is used in the sense of something to do, not an
 * operating system task. Tasks subclass Runnable and are run one or more times by thread pool threads. Tasks are
 * designed to be executed more than once with a delay between execution episodes.
 * </p>
 * 
 * <p>
 * Task instances are generally created by a Schedule and need not be overridden in an application. Instead, create a
 * subclass of Action that performs the desired action. Task is thread safe, Action, by design is not.
 * </p>
 * 
 * <p>
 * Task instances are serialized and stored in persistent storage between execution episodes. If the application
 * restarts all active Tasks are reloaded and execution resumes. For this reason both Task and Action objects must be
 * serializable by the storage system.
 * </p>
 * 
 * @author Jonathan A. Smith
 */

public class Task implements Runnable, Serializable {

  private static final long serialVersionUID = 6074999198466279317L;

  /** Task execution state. */
  public enum State {
    INITIAL, ACTIVE, COMPLETE, FAILED
  }

  /** The Schedule that owns this task. */
  private Schedule schedule;

  /** Current task state */
  private State state = State.INITIAL;

  /** Seconds delay before task is resumed. */
  private long resume_seconds;

  /** Time after which task should be resumed. */
  private Date resume_time;

  /** Action to carry out when resumed. */
  private Action action;

  /** Time when completion is required. */
  private Date deadline;

  /** Status message (for human review.) */
  private String status;

  /** Number of times Task has been resumed. */
  private int resume_count = 0;

  /** Number of times this task has been resumed after an exception. */
  private int retry_count = 0;

  /** Time to when task was started. */
  private Date start_time;

  /** Time when task was last active. */
  private Date active_time;

  // **** Object Creation

  /**
   * Constructs a task.
   */

  public Task() {
  }

  // *** Accessors

  public Action getAction() {
    return action;
  }

  public synchronized void setAction(Action action) {
    this.action = action;
  }

  public Date getDeadline() {
    return deadline;
  }

  public synchronized void setDeadline(Date deadline) {
    this.deadline = deadline;
  }

  @JSON(include = false)
  public Schedule getSchedule() {
    return schedule;
  }

  public synchronized void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public Date getStartTime() {
    return start_time;
  }

  public synchronized void setStartTime(Date start_time) {
    this.start_time = start_time;
  }

  public State getState() {
    return state;
  }

  public synchronized void setState(State state) {
    this.state = state;
  }

  public String getStatus() {
    return status;
  }

  public synchronized void setStatus(String status) throws InvalidKeyException {
    this.status = status;
    save();
  }

  public int getResumeCount() {
    return resume_count;
  }

  public synchronized void setResumeCount(int resume_count) {
    this.resume_count = resume_count;
  }

  public Date getResumeTime() {
    return resume_time;
  }

  public synchronized void setResumeTime(Date resume_time) {
    this.resume_time = resume_time;
  }

  public int getRetryCount() {
    return retry_count;
  }

  public synchronized void setRetryCount(int retry_count) {
    this.retry_count = retry_count;
  }

  public Date getActiveTime() {
    return active_time;
  }

  public synchronized void setActiveTime(Date active_time) {
    this.active_time = active_time;
  }

  @JSON(include = false)
  public String getName() {
    return action.getName();
  }

  // **** Execution

  /**
   * Executes this task's peridic action. Nore that this is called in a worker thread and should not rely on having
   * private access to any data other than the Task and its Action.
   */

  public synchronized void run() {
    try {
      checkTime();
    } catch (InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
    resume_seconds = 0;
    action.setTask(this);
    try {
      execute();
    } catch (InvalidKeyException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    if (state == State.ACTIVE && resume_seconds > 0)
      scheduleResume();
    try {
      schedule.saveTask(this);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Schedule resumption of the task after resume_seconds seconds.
   */

  private void scheduleResume() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, (int) resume_seconds);
    resume_time = calendar.getTime();
    schedule.resumeTaskAfter(this, resume_seconds);
  }

  /**
   * Records the current time (and start time if not set). Checks the current time against the deadline. If the current
   * time is after the deadline, singles Task failure.
   * @throws InvalidKeyException 
   */

  private void checkTime() throws InvalidKeyException {
    active_time = new Date();
    resume_count += 1;
    if (start_time == null)
      start_time = active_time;
    if (deadline != null && active_time.after(deadline))
      fail("Deadline reached");
  }

  /**
   * Execute the action.
   * @throws InvalidKeyException 
   */

  private void execute() throws InvalidKeyException {
    try {
      switch (state) {
      case INITIAL:
        state = State.ACTIVE;
        action.start();
      case ACTIVE:
        action.execute();
        break;
      }
    } catch (RetryException except) {
      retry(except);
    } catch (Throwable except) {
      log("Task failed", except);
      fail();
    } finally {
      if (state == State.COMPLETE || state == State.FAILED)
        finish();
    }
  }

  private void retry(RetryException except) throws InvalidKeyException {
    final int limit = action.retryLimit();
    log("Retry exception caught: count = " + retry_count + ", limit = " + limit, except);
    retry_count += 1;
    if (retry_count >= limit)
      fail("Retry count exceeded.");
    else
      resume_seconds = Math.max(1, except.getRetryDelay());
  }

  /**
   * Calls the finish method on the action. Catches exceptions to handle task failure or retries.
   * @throws InvalidKeyException 
   */

  private void finish() throws InvalidKeyException {
    try {
      action.finish();
    } catch (Throwable except) {
      fail();
    }
  }

  /**
   * Sets the number of seconds before Task is resumed. This should be called by the Action. Note that the task is not
   * rescheduled until execution is finished. This just sets the delay time that will be used when execution is done.
   * 
   * @param seconds
   *          delay before Task is resumed in seconds
   */

  void resumeAfter(long seconds) {
    resume_seconds = seconds;
  }

  // **** Logging and Status

  /**
   * Indicates that the Task is complete and succeeded.
   * 
   * @param message
   *          Information about success or null
   * @throws InvalidKeyException 
   */

  void succeed(String message) throws InvalidKeyException {
    state = State.COMPLETE;
    save();
    if (message != null)
      log("Action succeeded: " + message);
    else
      log("Action succeeded");
  }

  /**
   * Indicates that the Task is complete and succeeded.
   * @throws InvalidKeyException 
   */

  void succeed() throws InvalidKeyException {
    succeed(null);
  }

  /**
   * Indicates that the Task is complete and failed.
   * 
   * @param message
   *          indicates reason for failure or null
   * @throws InvalidKeyException 
   */

  void fail(String message) throws InvalidKeyException {
    state = State.FAILED;
    save();
    if (message != null)
      log("Action failed: " + message);
    else
      log("Action failed");
  }

  /**
   * Indicates that the Task is complete and failed.
   * @throws InvalidKeyException 
   */

  void fail() throws InvalidKeyException {
    fail(null);
  }

  /**
   * Saves the state of the task (including the Action)
   * @throws InvalidKeyException 
   */

  synchronized void save() throws InvalidKeyException {
    if (schedule != null)
      schedule.saveTask(this);
  }

  // **** Logging

  /**
   * Logs a message related to this task.
   * 
   * @param message
   */

  void log(String message) {
    String log_message = action.getName() + ": " + message;
    schedule.log(log_message);
  }

  /**
   * Logs an exception related to this task.
   * 
   * @param message
   *          description of error
   * @param cause
   *          Throwable to be logged
   */

  void log(String message, Throwable cause) {
    String log_message = action.getName() + ": " + message;
    schedule.log(log_message, cause);
  }

  /**
   * Logs an exception related to this task.
   * 
   * @param cause
   *          Throwable to be logged
   */

  void log(Throwable cause) {
    String log_message = action.getName() + ": Exception";
    schedule.log(log_message, cause);
  }

  // **** Callbacks

  /**
   * Finds and returns a task by name.
   * 
   * @param name
   *          name of task to find.
   * @return Task object or nil if not found.
   * @throws InvalidKeyException 
   */

  protected Task getTaskNamed(String name) throws InvalidKeyException {
    if (schedule == null)
      return null;
    return schedule.getTask(name);
  }

  // **** String Representation

  /**
   * Returns a JSON String representation of this Task.
   * 
   * @return JSON String
   */

  public synchronized String toJSON() {
    return new JSONSerializer().serialize(this);
  }

  /**
   * Returns a JSON String representation of this Task.
   * 
   * @return JSON String
   */

  @Override
  public String toString() {
    return toJSON();
  }

}
