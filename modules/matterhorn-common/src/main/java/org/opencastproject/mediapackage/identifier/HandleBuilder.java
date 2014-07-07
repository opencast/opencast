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
 * Interface for a facility that is able to create CNRI handles as described in more detail on the <a
 * href="http://www.handle.net/">handle system page</a>.
 */
public interface HandleBuilder extends IdBuilder {

  /**
   * Creates a new handle by connecting to the handle server and asking it to hand over a new instance for the default
   * url. Do not forget to update the handle once the final url is known.
   *
   * @return the new handle
   */
  Handle createNew();

  /**
   * Creates a new handle by connecting to the handle server and asking it to hand over a new instance for the specified
   * url.
   *
   * @param url
   *          the handle target
   * @return the new handle
   * @throws HandleException
   *           if the handle cannot be created
   */
  Handle createNew(URL url) throws HandleException;

  /**
   * Creates a handle object from the specified value. The value must be a valid handle in string form, e. g.
   * <code>10.254/test</code>, optionally preceeded by the handle protocol identifier {@link Handle#PROTOCOL};
   *
   * @param value
   *          the handle value
   * @return the handle
   * @throws HandleException
   *           if the value is malformatted
   */
  Handle fromString(String value) throws IllegalArgumentException;

  /**
   * Updates the handle to point to the new url.
   *
   * @param handle
   *          the handle
   * @param url
   *          the new handle url
   * @throws HandleException
   *           if updating the handle failed
   */
  boolean update(Handle handle, URL url) throws HandleException;

  /**
   * Resolves the handle and returns its target url.
   *
   * @param handle
   *          the handle
   * @throws HandleException
   *           if resolving the handle failed
   */
  URL resolve(Handle handle) throws HandleException;

  /**
   * Removes the handle from the handle server.
   * <p>
   * Note that removing a handle should not occur for urls that have been in handed out. Rather update the handle to
   * some meaningful <code>404</code> page instead.
   * </p>
   *
   * @param handle
   *          the handle
   * @throws HandleException
   *           if removing the handle failed
   */
  boolean delete(Handle handle) throws HandleException;

}
