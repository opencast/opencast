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

package org.opencastproject.mediapackage.identifier;

import java.net.URL;

/**
 * This interface identifies a CNRI handle. Handles are a special type of <code>digital object identifier</code> that
 * allow for permanent localization of digital resources.
 */
public interface Handle extends Id {

  /** The handle protocol identifier */
  String PROTOCOL = "hdl";

  /** The handle prefix */
  String PREFIX = "10.";

  /**
   * Returns the naming authority (or prefix) for the handle.
   *
   * @return the naming authority
   */
  String getNamingAuthority();

  /**
   * Returs the handle local name.
   *
   * @return the handle local name
   */
  String getLocalName();

  /**
   * Returns the full name consisting of authority and local name, e.g.
   * <code>10.3930/ETHZ/AV-0bf9cb23-5535-4c5c-940e-c1a7d91e3191</code>
   */
  String getFullName();

  /**
   * Resolves this handle to the target url by using the configured handle server. If the server cannot be reached, or
   * resolving fails, a {@link HandleException} is thrown.
   *
   * @return the handle target
   */
  URL resolve() throws HandleException;

  /**
   * Updates the value of this handle to the new target url by using the configured handle server. If the server cannot
   * be reached, or updating fails, a {@link HandleException} is thrown.
   */
  void update(URL value) throws HandleException;

}
