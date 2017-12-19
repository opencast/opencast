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


package org.opencastproject.mediapackage;

/**
 * Exception thrown by {@link CatalogImpl} in case of any namespace binding errors.
 */
public class NamespaceBindingException extends RuntimeException {

  /** Serial version uid */
  private static final long serialVersionUID = 3520050243419468968L;

  public NamespaceBindingException() {
  }

  public NamespaceBindingException(String message) {
    super(message);
  }

  public NamespaceBindingException(String message, Throwable cause) {
    super(message, cause);
  }

  public NamespaceBindingException(Throwable cause) {
    super(cause);
  }
}
