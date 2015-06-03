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
package org.opencastproject.serviceregistry.api;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

/**
 * Marshals and unmarshals {@link SystemLoad}s.
 */
public final class SystemLoadParser {

  /** The jaxb context to use when creating marshallers and unmarshallers */
  private static final JAXBContext jaxbContext;

  /** Static initializer to setup the jaxb context */
  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.serviceregistry.api",
              SystemLoadParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow construction of this utility class */
  private SystemLoadParser() {
  }

  /**
   * Parses an xml string representing a {@link SystemLoad}
   *
   * @param xml
   *          The serialized data
   * @return The SystemLoad
   */
  public static SystemLoad parseXml(String xml) throws IOException {
    return parse(IOUtils.toInputStream(xml, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link SystemLoad}
   *
   * @param in
   *          The serialized data
   * @param format
   *          the serialization format
   * @return The SystemLoad
   */
  public static SystemLoad parse(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), SystemLoad.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Gets a serialized representation of a {@link SystemLoad}
   *
   * @param systemLoad
   *          The SystemLoad to marshal
   * @return the serialized SystemLoad
   */
  public static InputStream toXmlStream(SystemLoad systemLoad) throws IOException {
    return IOUtils.toInputStream(toXml(systemLoad), "UTF-8");
  }

  /**
   * Gets an xml representation of a {@link SystemLoad}
   *
   * @param systemLoad
   *          The SystemLoad to marshal
   * @return the serialized registration
   */
  public static String toXml(SystemLoad systemLoad) throws IOException {
    Marshaller marshaller;
    try {
      marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(systemLoad, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }
}
