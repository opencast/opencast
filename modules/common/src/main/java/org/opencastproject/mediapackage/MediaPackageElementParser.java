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

import static org.apache.commons.io.IOUtils.toInputStream;

import org.opencastproject.util.XmlSafeParser;
import org.opencastproject.util.data.Function;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Convenience implementation that supports serializing and deserializing media package elements.
 */
public final class MediaPackageElementParser {

  /**
   * Private constructor to prohibit instances of this static utility class.
   */
  private MediaPackageElementParser() {
    // Nothing to do
  }

  /**
   * Serializes the media package element to a string.
   *
   * @param element
   *         the element
   * @return the serialized media package element
   * @throws MediaPackageException
   *         if serialization failed
   */
  public static String getAsXml(MediaPackageElement element) throws MediaPackageException {
    if (element == null)
      throw new IllegalArgumentException("Mediapackage element must not be null");
    StringWriter writer = new StringWriter();
    Marshaller m = null;
    try {
      m = MediaPackageImpl.context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
      m.marshal(element, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new MediaPackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /** {@link #getAsXml(MediaPackageElement)} as function. */
  public static <A extends MediaPackageElement> Function<A, String> getAsXml() {
    return new Function.X<A, String>() {
      @Override protected String xapply(MediaPackageElement elem) throws Exception {
        return getAsXml(elem);
      }
    };
  }

  /**
   * Parses the serialized media package element and returns its object representation.
   *
   * @param xml
   *         the serialized element
   * @return the media package element instance
   * @throws MediaPackageException
   *         if de-serializing the element fails
   */
  public static MediaPackageElement getFromXml(String xml) throws MediaPackageException {
    Unmarshaller m = null;
    try {
      m = MediaPackageImpl.context.createUnmarshaller();
      return (MediaPackageElement) m.unmarshal(XmlSafeParser.parse(toInputStream(xml)));
    } catch (JAXBException e) {
      throw new MediaPackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } catch (IOException | SAXException e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * Serializes media package element list to a string.
   *
   * @param elements
   *         element list to be serialized
   * @return serialized media package element list
   * @throws MediaPackageException
   *         if serialization fails
   */
  public static String getArrayAsXml(Collection<? extends MediaPackageElement> elements) throws MediaPackageException {
    // TODO write real serialization function
    if (elements == null || elements.isEmpty()) return "";
    try {
      StringBuilder builder = new StringBuilder();
      Iterator<? extends MediaPackageElement> it = elements.iterator();
      builder.append(getAsXml(it.next()));
      while (it.hasNext()) {
        builder.append("###");
        builder.append(getAsXml(it.next()));
      }
      return builder.toString();
    } catch (Exception e) {
      if (e instanceof MediaPackageException) {
        throw (MediaPackageException) e;
      } else {
        throw new MediaPackageException(e);
      }
    }
  }

  /**
   * Parses the serialized media package element list.
   *
   * @param xml
   *         String to be parsed
   * @return parsed media package element list
   * @throws MediaPackageException
   *         if de-serialization fails
   */
  public static List<? extends MediaPackageElement> getArrayFromXml(String xml) throws MediaPackageException {
    // TODO write real deserialization function
    try {
      List<MediaPackageElement> elements = new LinkedList<MediaPackageElement>();
      String[] xmlArray = xml.split("###");
      for (String xmlElement : xmlArray) {
        if ("".equals(xmlElement.trim())) continue;
        elements.add(getFromXml(xmlElement.trim()));
      }
      return elements;
    } catch (Exception e) {
      if (e instanceof MediaPackageException) {
        throw (MediaPackageException) e;
      } else {
        throw new MediaPackageException(e);
      }
    }
  }

  /**
   * Same as getArrayFromXml(), but throwing a RuntimeException instead of a checked exception. Useful in streams.
   *
   * @param xml
   *         String to be parsed
   * @return parsed media package element list
   *
   * @throws MediaPackageRuntimeException
   *         if de-serialization fails
   */
  public static List<? extends MediaPackageElement> getArrayFromXmlUnchecked(String xml) {
    try {
      return getArrayFromXml(xml);
    } catch (MediaPackageException e) {
      throw new MediaPackageRuntimeException(e);
    }
  }

}
