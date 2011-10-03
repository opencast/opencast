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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface for an identifier.
 */
@XmlJavaTypeAdapter(Id.Adapter.class)
public interface Id {

  /**
   * Returns the local identifier of this {@link Id}. The local identifier is defined to be free of separator characters
   * that could potentially get into the way when creating file or directory names from the identifier.
   * 
   * For example, given that the interface is implemented by a class representing CNRI handles, the identifier would
   * then look something like <code>10.3930/ETHZ/abcd</code>, whith <code>10.3930</code> being the handle prefix,
   * <code>ETH</code> the authority and <code>abcd</code> the local part. <code>toURI()</code> would then return
   * <code>10.3930-ETH-abcd</code> or any other suitable form.
   * 
   * @return a path separator-free representation of the identifier
   */
  String compact();

  static class Adapter extends XmlAdapter<IdImpl, Id> {
    public IdImpl marshal(Id id) throws Exception {
      if (id instanceof IdImpl) {
        return (IdImpl) id;
      } else if (id instanceof HandleImpl) {
        return (HandleImpl) id;
      } else {
        throw new IllegalStateException("an unknown ID is un use: " + id);
      }
    }

    public Id unmarshal(IdImpl id) throws Exception {
      return id;
    }
  }

  /**
   * Return a string representation of the identifier from which an object of type Id should
   * be reconstructable.
   */
  String toString();
}
