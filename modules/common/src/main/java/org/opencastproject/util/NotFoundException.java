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

package org.opencastproject.util;

/**
 * An exception that indicates that a resource that was expected to exist does not exist.
 */
public class NotFoundException extends Exception {

  private static final long serialVersionUID = -6781286820876007809L;

  /**
   * Constructs a NotFoundException without a detail message.
   */
  public NotFoundException() {
    super();
  }

  /**
   * Constructs a NotFoundException with a detail message.
   */
  public NotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a NotFoundException with a cause.
   */
  public NotFoundException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a NotFoundException with a detail message and a cause.
   */
  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
