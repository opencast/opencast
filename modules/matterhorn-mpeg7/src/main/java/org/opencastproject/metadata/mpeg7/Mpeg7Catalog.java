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

package org.opencastproject.metadata.mpeg7;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.api.MetadataCatalog;

import org.w3c.dom.Document;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * The <code>MPEG7</code> catalog encapsulates MPEG-7 metadata.
 */
public interface Mpeg7Catalog extends Mpeg7, MetadataCatalog, Cloneable {
  /** A flavor that matches any mpeg7 element */
  MediaPackageElementFlavor ANY_MPEG7 = MediaPackageElementFlavor.parseFlavor("mpeg-7/*");

  /**
   * Saves the catalog to disk.
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

  Mpeg7Catalog clone();
}
