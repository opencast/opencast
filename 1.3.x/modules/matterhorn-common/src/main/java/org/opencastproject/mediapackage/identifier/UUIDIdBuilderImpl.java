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

import java.util.UUID;

/**
 * Default implementation of an id builder. This implementation yields for a distributed id generator that will create
 * unique ids for the system.
 */
public class UUIDIdBuilderImpl implements IdBuilder {

  /**
   * Creates a new id builder.
   */
  public UUIDIdBuilderImpl() {
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#createNew()
   */
  public Id createNew() {
    return new IdImpl(UUID.randomUUID().toString());
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#fromString(String)
   */
  public Id fromString(String id) throws IllegalArgumentException {
    if (id == null)
      throw new IllegalArgumentException("Argument 'id' is null");
    try {
      UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw e;
    }
    return new IdImpl(id);
  }

}
