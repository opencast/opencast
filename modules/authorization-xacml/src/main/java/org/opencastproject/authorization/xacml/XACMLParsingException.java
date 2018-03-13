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
package org.opencastproject.authorization.xacml;

/**
 * This exception may be thrown during parsing of a XACML.
 */
public class XACMLParsingException extends Exception {

  /**
   * Creates a new xacml parsing exception with the given error message.
   *
   * @param message
   *          the error message
   */
  public XACMLParsingException(String message) {
    super(message);
  }

  /**
   * Creates a new xacml parsing exception with the given error message, caused by the given exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the error cause
   */
  public XACMLParsingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new xacml parsing exception, caused by the given exception.
   *
   * @param cause
   *          the error cause
   */
  public XACMLParsingException(Throwable cause) {
    super(cause);
  }
}
