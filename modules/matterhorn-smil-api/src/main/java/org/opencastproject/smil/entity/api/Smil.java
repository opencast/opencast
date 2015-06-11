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

package org.opencastproject.smil.entity.api;

import java.net.MalformedURLException;
import javax.xml.bind.JAXBException;
import org.opencastproject.smil.api.SmilException;
import org.xml.sax.SAXException;

/**
 * Represent a SMIL document.
 */
public interface Smil extends SmilObject {

  /**
   * Returns body of the SMIL.
   *
   * @return the body of the SMIL.
   */
  SmilBody getBody();

  /**
   * Returns head of the SMIL.
   *
   * @return the head of the SMIL.
   */
  SmilHead getHead();

  /**
   * Serialize this object.
   *
   * @return the XML representation of the {@link Smil} object
   * @throws JAXBException if serializing fail
   */
  String toXML() throws JAXBException, SAXException, MalformedURLException;

  /**
   * Returns element with given Id.
   *
   * @param elementId element Id
   * @throws SmilException if there is no element with the same Id
   * @returnelement with given Id
   */
  SmilObject get(String elementId) throws SmilException;
}
