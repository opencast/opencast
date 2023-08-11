/*
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
package org.opencastproject.metadata.dublincore;

import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Byte serialization of Dublin Core catalogs.
 */
public final class DublinCoreByteFormat {
  private DublinCoreByteFormat() {
  }

  /**
   * Parse a Dublin Core catalog represented by a byte array. Will recognize if it's in JSON or XML format and use
   * the correct method.
   *
   * @param bytes
   *         the catalog represented by a byte array
   * @return the catalog
   * @throws IOException
   *         if the input can't be read
   * @throws ParseException
   *         if setting up the JSON parser failed
   * @throws ParserConfigurationException
   *         if setting up the XML parser failed
   * @throws SAXException
   *         if an error occurred while parsing the XML catalog
   */
  public static DublinCoreCatalog read(byte[] bytes)
          throws IOException, ParseException, ParserConfigurationException, SAXException {
    final String catalogString = new String(bytes, StandardCharsets.UTF_8);
    if (DublinCoreJsonFormat.isJson(catalogString)) {
      return DublinCoreJsonFormat.read(catalogString);
    } else {
      return DublinCoreXmlFormat.read(catalogString);
    }
  }
}
