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

/**
 * Exception thrown when Action should be retried.
 *
 * @author Jonathan A. Smith
 */

public class RetryException extends ActionException {

    /** Default retry delay in seconds. */
    public static final int DEFAULT_DELAY = 30 * 60;

    /** Number of seconds to delay before retry. */
    private final int retry_delay;

    /**
     * Constructs a RetryException thrown because of an earlier exception.
     * The software will wait at least retry_delay seconds before trying
     * again.
     *
     * @param message description of exception
     * @param cause Throwable that caused the exception or null if none
     * @param retry_delay delay in seconds before action is retried
     */

    public RetryException(String message, Throwable cause, int retry_delay) {
        super(message, cause);
        this.retry_delay = retry_delay;
    }

    /**
     * Constructs a RetryException thrown because of an earlier exception.
     * The software will wait at least DEFAULT_DELAY seconds before trying
     * again.
     *
     * @param message description of exception
     * @param cause Throwable that caused the exception or null if none
     */

    public RetryException(String message, Throwable cause) {
        this(message, cause, DEFAULT_DELAY);
    }

    /**
     * Constructs a RetryException thrown because of an earlier exception.
     * The software will wait at least DEFAULT_DELAY seconds before trying
     * again.
     *
     * @param cause Throwable that caused the exception or null if none
     * @param retry_delay delay in seconds before action is retried
     */

    public RetryException(Throwable cause, int retry_delay) {
        this(null, cause, retry_delay);
    }

    /**
     * Constructs a RetryException thrown because of an earlier exception.
     * The software will wait at least DEFAULT_DELAY seconds before trying
     * again.
     *
     * @param cause Throwable that caused the exception or null if none
     */

    public RetryException(Throwable cause) {
        this(null, cause, DEFAULT_DELAY);
    }

     /**
      * Constructs a RetryException that will cause the Task to be retried at a
      * later time. The software will wait at least retry_delay seconds before
      * trying again.
      *
      * @param message description of exception
      * @param retry_delay delay in seconds before action is retried
      */

    public RetryException(String message, int retry_delay) {
        this(message, null, retry_delay);
    }

     /**
      * Constructs a RetryException that will cause the Task to be retried at a
      * later time. The software will wait at least DEFAULT_DELAY seconds before
      * trying again.
      *
      * @param message description of exception
      */

    public RetryException(String message) {
        this(message, null, DEFAULT_DELAY);
    }
 

    /**
     * Returns the number of seconds before the Task will be retried.
     *
     * @return retry delay in seconds
     */

    public int getRetryDelay() {
        return retry_delay; 
    }
}
