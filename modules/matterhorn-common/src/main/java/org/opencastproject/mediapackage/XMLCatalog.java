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

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Definition for a plain xml catalog.
 */
public interface XMLCatalog {

  /**
   * Serializes the catalog to a DOM.
   *
   * todo think about hiding technical exceptions
   *
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog file handling occurs
   */
  Document toXml() throws ParserConfigurationException, TransformerException, IOException;

  /**
   * Serializes the catalog to a JSON string.
   *
   * @return the json string
   * @throws IOException
   *           if an error with the catalog file handling occurs
   */
  String toJson() throws IOException;

  /**
   * Writes an xml representation of this Catalog to a string.
   *
   * @return the Catalog serialized to a string
   */
  String toXmlString() throws IOException;

  /**
   * Writes an xml representation of this Catalog to a stream.
   *
   * @param out
   *          The output stream
   * @param format
   *          Whether to format the output for readability, or not (false gives better performance)
   */
  void toXml(OutputStream out, boolean format) throws IOException;

}
