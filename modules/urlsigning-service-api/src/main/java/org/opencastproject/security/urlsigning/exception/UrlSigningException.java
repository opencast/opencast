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
package org.opencastproject.security.urlsigning.exception;

import static com.google.common.base.MoreObjects.toStringHelper;
/**
 * An {@link Exception} thrown if there is a problem signing a URL.
 */
public class UrlSigningException extends Exception {
  private static final long serialVersionUID = 594382838580879079L;

  private enum Reason {
    URL_NOT_SUPPORTED, INTERNAL_PROVIDER_ERROR;
  };

  /** The reason for the exception. */
  private Reason reason;

  public UrlSigningException(Throwable t) {
    super(t);
  }

  private UrlSigningException(Reason reason) {
    this.reason = reason;
  }

  public static UrlSigningException urlNotSupported() {
    return new UrlSigningException(Reason.URL_NOT_SUPPORTED);
  }

  public static UrlSigningException internalProviderError() {
    return new UrlSigningException(Reason.INTERNAL_PROVIDER_ERROR);
  }

  public Reason getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("Reason", reason).toString();
  }
}
