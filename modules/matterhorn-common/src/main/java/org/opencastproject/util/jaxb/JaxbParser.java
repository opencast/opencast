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
package org.opencastproject.util.jaxb;

import static org.opencastproject.util.data.functions.Misc.chuck;

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

/** Base class for JAXB parser classes. */
public abstract class JaxbParser {
  private final JAXBContext ctx;

  /**
   * Create a new parser.
   *
   * @param contextPath see {@link javax.xml.bind.JAXBContext#newInstance(String, ClassLoader)}
   */
  protected JaxbParser(String contextPath) {
    this.ctx = init(contextPath);
  }

  private JAXBContext init(String contextPath) {
    try {
      return JAXBContext.newInstance(contextPath, this.getClass().getClassLoader());
    } catch (JAXBException e) {
      return chuck(e);
    }
  }

  public JAXBContext getCtx() {
    return ctx;
  }

  /** Unmarshal an instance of class <code>dtoClass</code> from <code>source</code> and close it. */
  public <A> A unmarshal(Class<A> dtoClass, InputStream source) throws IOException {
    try {
      final Unmarshaller unmarshaller = ctx.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(source), dtoClass).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(source);
    }
  }

  /**
   * Marshal an object into a string.
   */
  public String marshal(Object o) throws IOException {
    try {
      final Marshaller marshaller = ctx.createMarshaller();
      final Writer writer = new StringWriter();
      marshaller.marshal(o, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }
}
