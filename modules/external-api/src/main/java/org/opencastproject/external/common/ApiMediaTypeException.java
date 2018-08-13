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
package org.opencastproject.external.common;

public final class ApiMediaTypeException extends RuntimeException {

  private static final long serialVersionUID = -5875765624123555637L;

  public static ApiMediaTypeException invalidVersion(String mediaType) {
    return new ApiMediaTypeException("'" + mediaType + "' does not contain a valid version");
  }

  public static ApiMediaTypeException invalidFormat(String mediaType) {
    return new ApiMediaTypeException("'" + mediaType + "' does not contain a valid format");
  }

  private ApiMediaTypeException(String message) {
    super(message);
  }

}
