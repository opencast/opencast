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
package org.opencastproject.security.api;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

/**
 * Marshals and unmarshalls {@link Organization}s to/from XML.
 */
public final class OrganizationParser {

  private static final JAXBContext jaxbContext;

  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.security.api",
              OrganizationParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Disallow construction of this utility class.
   */
  private OrganizationParser() {
  }

  public static Organization fromXml(String xml) {
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(xml);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbOrganization.class).getValue();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static String toXml(Organization organization) {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(organization, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
