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
 * Exception thrown when a Task fails.
 * 
 */

public class FailedException extends Exception {

  private static final long serialVersionUID = -5469568161350489305L;

  public FailedException(Throwable cause) {
    super(cause);
  }

  public FailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public FailedException(String message) {
    super(message);
  }
}
