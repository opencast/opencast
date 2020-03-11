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

package org.opencastproject.list.api;

/**
 * An exception which indicates an error from the list provider
 */
public class ListProviderException extends Exception {

  private static final long serialVersionUID = 7020240266255081300L;

  /**
   * Constructor without cause.
   * 
   * @param message
   */
  public ListProviderException(String message) {
    super(message);
  }

  /**
   * Full fledged constructor.
   * 
   * @param message
   * @param cause
   */
  public ListProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
