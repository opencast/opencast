/*
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


package org.opencastproject.elasticsearch.api;

/**
 * Exception that is thrown if service level error states occur when working with a search index.
 *
 */
public class SearchIndexException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 3821061675053251045L;

  /** Whether this exception was caused by a malformed request rather than an operational problem */
  private final boolean badRequest;

  /**
   * Creates a new search index exception with the given message and root cause.
   *
   * @param message
   *          the exception message
   * @param cause
   *          the root cause
   */
  public SearchIndexException(String message, Throwable cause) {
    this(message, cause, false);
  }

  /**
   * Creates a new search index exception with the given message and root cause.
   *
   * @param message
   *          the exception message
   * @param cause
   *          the root cause
   * @param badRequest
   *          whether the cause was a malformed request (e.g. an invalid query) rather than an operational problem.
   *          Callers can use this to decide whether the failure is worth surfacing as an error, and whether it is
   *          safe to retry.
   */
  public SearchIndexException(String message, Throwable cause, boolean badRequest) {
    super(message, cause);
    this.badRequest = badRequest;
  }

  /**
   * Creates a new search index exception with the given message.
   *
   * @param message
   *          the exception message
   */
  public SearchIndexException(String message) {
    super(message);
    this.badRequest = false;
  }

  /**
   * Creates a new search index exception with the given root cause.
   *
   * @param cause
   *          the root cause
   */
  public SearchIndexException(Throwable cause) {
    super(cause);
    this.badRequest = false;
  }

  /**
   * @return whether this exception was caused by a malformed request (e.g. an invalid search query) rather than an
   *         operational problem such as a missing index or an unreachable search service.
   */
  public boolean isBadRequest() {
    return badRequest;
  }

}
