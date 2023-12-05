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

package org.opencastproject.publication.api;

/**
 * A PublicationException indicates that an error occurred while interacting with a publication channel.
 */
public class PublicationException extends Exception {

  private static final long serialVersionUID = -5953133029145197376L;

  public PublicationException(String message) {
    super(message);
  }

  public PublicationException(Throwable cause) {
    super(cause);
  }

  public PublicationException(String message, Throwable cause) {
    super(message, cause);
  }

}
