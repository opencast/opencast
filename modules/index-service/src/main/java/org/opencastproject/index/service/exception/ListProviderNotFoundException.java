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

package org.opencastproject.index.service.exception;

/**
 * This exception indicates that the queried list provider resource could not be found.
 */
public class ListProviderNotFoundException extends ListProviderException {

  /**
   * Constructs an exception with a simple message.
   *
   * @param message the simple message
   */
  public ListProviderNotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with its cause and a simple message.
   *
   * @param message the simple message
   * @param cause the cause
   */
  public ListProviderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
