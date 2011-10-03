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
package org.opencastproject.mediapackage;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;

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
   *          the element
   * @return the serialized media package element
   * @throws MediaPackageException
   *           if serialization failed
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

  /**
   * Parses the serialized media package element and returns its object representation.
   * 
   * @param xml
   *          the serialized element
   * @return the media package element instance
   * @throws MediaPackageException
   *           if de-serializing the element fails
   */
  public static MediaPackageElement getFromXml(String xml) throws MediaPackageException {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(IOUtils.toInputStream(xml, "UTF-8"));
      MediaPackageElement element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
      return (AbstractMediaPackageElement) element;
    } catch (Exception e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * Serializes media package element list to a string.
   * 
   * @param elements
   *          element list to be serialized
   * @return serialized media package element list
   * @throws MediaPackageException
   *           if serialization fails
   */
  public static String getArrayAsXml(List<? extends MediaPackageElement> elements) throws MediaPackageException {
    // TODO write real serialization function
    try {
      StringBuilder builder = new StringBuilder();
      builder.append(getAsXml(elements.get(0)));
      for (int i = 1; i < elements.size(); i++) {
        builder.append("###");
        builder.append(getAsXml(elements.get(i)));
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
   *          String to be parsed
   * @return parsed media package element list
   * @throws MediaPackageException
   *           if de-serialization fails
   */
  public static List<? extends MediaPackageElement> getArrayFromXml(String xml) throws MediaPackageException {
    // TODO write real deserialization function
    try {
      List<MediaPackageElement> elements = new LinkedList<MediaPackageElement>();
      String[] xmlArray = xml.split("###");
      for (String xmlElement : xmlArray) {
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
}
