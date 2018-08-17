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
package org.opencastproject.external.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;

public class XMLListWrapper<T> {

  private List<T> items;

  public XMLListWrapper() {
    items = new ArrayList<T>();
  }

  public XMLListWrapper(List<T> items) {
    this.items = items;
  }

  @XmlAnyElement(lax = true)
  public List<T> getItems() {
    return items;
  }

  /**
   * Wrap List in Wrapper, then leverage JAXBElement to supply root element information.
   */
  public static String marshal(Marshaller marshaller, List<?> list, String name) throws JAXBException {
    StringWriter stringWriter = new StringWriter();
    QName qName = new QName(name);
    XMLListWrapper<?> wrapper = new XMLListWrapper(list);
    JAXBElement<XMLListWrapper> jaxbElement = new JAXBElement<XMLListWrapper>(qName, XMLListWrapper.class, wrapper);
    marshaller.marshal(jaxbElement, stringWriter);
    return stringWriter.toString();
  }

}
