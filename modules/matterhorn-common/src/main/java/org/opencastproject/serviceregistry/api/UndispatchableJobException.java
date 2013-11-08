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
package org.opencastproject.serviceregistry.api;

/**
 * Exception that is thrown if a job is not dispatchable by any service that would normally accept this type of work.
 * <p>
 * The exception indicates that there may be something wrong with the job or that the job cannot be dispatched because
 * of related circumstances.
 */
public class UndispatchableJobException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = 5006552593095889618L;

  /**
   * Creates a new undispatchable job exception
   * 
   * @param message
   *          the error message
   * @param t
   *          the exception causing the error
   */
  public UndispatchableJobException(String message, Throwable t) {
    super(message, t);
  }

  /**
   * Creates a new undispatchable job exception
   * 
   * @param message
   *          the error message
   */
  public UndispatchableJobException(String message) {
    super(message);
  }

  /**
   * Creates a new undispatchable job exception
   * 
   * @param t
   *          the exception causing the error
   */
  public UndispatchableJobException(Throwable t) {
    super(t);
  }

}
