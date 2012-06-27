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

import org.opencastproject.deliver.store.FileSystemStore;
import org.opencastproject.deliver.store.InvalidKeyException;
import org.opencastproject.deliver.store.MemoryStore;
import org.opencastproject.deliver.store.Store;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A Schedule is a collection of Task instances that carry out periodic
 * actions. To use the scheduler:</p>
 * <ul>
 * <li>Subclass the Action class. Override the execute method to carry out
 * the work. Call the resumeAfter to schedule a new cal to the execute method
 * after a specified delay.</li>
 * <li>Create an Action instance, then call scheduler.start(action) to begin
 * execution.</li>
 * </ul>
 * <p>Schedule instances save copies of Tasks in persistent storage between
 * execution episodes so that they can be rescheduled after the application has
 * been restarted. To do this the scheduler uses two Store objects: active for
 * saving information about active tasks, and complete for information about
 * successful and failed tasks.</p>
 *
 * @author Jonatan A. Smith
 */

public class Schedule {

    /** Thread pool size. */
    public final static int POOL_SIZE = 10;

    /** Store used for active task storage. */
    private Store<Task> active_store;

    /** Store used for completed task storage. */
    private Store<Task> completed_store;

    /** Map from Task name to Task for active tasks only. */
    private final ConcurrentHashMap<String, Task> task_cache;

    /** Executer used to execute actions. */
    private final ScheduledThreadPoolExecutor executor;

    /** Logger. */
    private final Logger logger =
            Logger.getLogger("org.opencastproject.deliver.schedule");

    /**
     * Constructs a Schedule with persistant storage in a specified file
     * system directory.
     *
     * @param active_store Store used to save active tasks
     * @param completed_store Store used to save completed and failed tasks
     * @throws InvalidKeyException 
     */

    public Schedule(Store<Task> active_store, Store<Task> completed_store) throws InvalidKeyException {
        this.active_store = active_store;
        this.completed_store = completed_store;
        executor = new ScheduledThreadPoolExecutor(POOL_SIZE);
        task_cache = new ConcurrentHashMap<String, Task>();
        resumeAll();
    }
    
    /**
     * Constructs a Schedule using FileSystemStores to store task information.
     * 
     * @param directory Directory where task information is to be stored.
     * @throws InvalidKeyException 
     */

    public Schedule(File directory) throws InvalidKeyException {
        this(new FileSystemStore<Task>(new File(directory, "active"),
                new TaskSerializer(), true),
             new FileSystemStore<Task>(new File(directory, "complete"),
                new TaskSerializer(), true));
    }

    /**
     * Constructs a Schedule using MemoryStore to keep task information in
     * memory. Note that this version will not survive a restart and is used
     * mainly for testing.
     * @throws InvalidKeyException 
     */

    public Schedule() throws InvalidKeyException {
        this(new MemoryStore<Task>(new TaskSerializer()),
             new MemoryStore<Task>(new TaskSerializer()));
    }

    /**
     * Called when a Schedule is created. Scans the active store for all
     * Tasks and re-inserts them into the schedule (so they will be resumed).
     * @throws InvalidKeyException 
     */

    private void resumeAll() throws InvalidKeyException {
        long now = System.currentTimeMillis();
        for (String name : active_store.keySet()) {
            Task task = active_store.get(name);
            task.setSchedule(this);
            task_cache.put(task.getName(), task);
            long resume_milliseconds = task.getResumeTime().getTime();
            long seconds = Math.max(0L, (resume_milliseconds - now) / 1000L);
            executor.schedule(task, seconds, TimeUnit.SECONDS);
        }
    }

    // **** Tasks

    /**
     * Returns a Task (if any) with the specified name. If the Task is
     * inactive it is retrieved from the completed store. Note
     * that any client should syncronize on the Task to insure it
     * will not be updated.
     *
     * @param name Task Name
     * @return Task with the specified name or null if not found
     * @throws InvalidKeyException 
     */

    public Task getTask(String name) throws InvalidKeyException {
        Task task = task_cache.get(name);
        if (task != null)
            return task;
        return completed_store.get(name);
    }

    /**
     * Returns a copy of a Task (if any) with the specified name. Note that
     * the returned Task is the last saved in the active or completed store
     * and so may not be executed. It is, however, safe to use this for
     * reproting without synchronization.
     *
     * @param name Task name
     * @return Copy of Task with specified name, or null if not found
     * @throws InvalidKeyException 
     */

    public Task getSavedTask(String name) throws InvalidKeyException {
        Task task = active_store.get(name);
        if (task != null)
            return task;
        return completed_store.get(name);
    }

    /**
     * Start execution of an action.
     *
     * @param action The action to execute
     * @return started Task
     * @throws InvalidKeyException 
     */

    public Task start(Action action) throws InvalidKeyException {
        Task task = action.makeTask();
        task.setSchedule(this);
        task.setResumeTime(new Date());
        saveTask(task);
        task_cache.put(task.getName(), task);
        executor.execute(task);
        return task;
    }
    
    /**
     * Resumes the task after a specified number of seconds. This method has
     * package scope and should be called only by the Task when it finishes
     * Task resumption.
     *
     * @param task Task to be resumed
     * @param seconds seconds delay before re-executing
     */

    void resumeTaskAfter(Task task, long seconds) {
        executor.schedule(task, seconds, TimeUnit.SECONDS);
    }

    /**
     * SaveTask is called to save the state of a task after it has finished
     * executing. Note that if the task is active it is retained in the task.
     * cache. A copy is written to the store so as to allow for recovery on a
     * restart.
     *
     * @param task Task to be saved.
     * @throws InvalidKeyException 
     */

    void saveTask(Task task) throws InvalidKeyException {
        String task_name = task.getName();
        switch (task.getState()) {
            case INITIAL:
            case ACTIVE:
                active_store.put(task_name, task);
                break;
            default:
                task_cache.remove(task_name);
                active_store.remove(task_name);
                completed_store.put(task_name, task);
        }
    }


    // **** Thread Management

    /**
     * Waits for all threads in the pool to finish execution
     * then shuts down execution.
     */

    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Stops all threads and shuts down the executor.
     */

    public void shutdownNow() {
        executor.shutdownNow();
    }

    // **** Logging

    /**
     * Logs a message originating in a task.
     *
     * @param message message to be logged
     */

    public void log(String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Logs an exception originating in a task.
     *
     * @param message description of exception
     * @param cause cause of exception
     */

    public void log(String message, Throwable cause) {
       logger.log(Level.SEVERE, message, cause);
    }

    // **** String Representation

    /**
     * Returns a string representation of the Schedule.
     *
     * @return String representaiton.
     */

    @Override
    public String toString() {
        return "";
    }

}
