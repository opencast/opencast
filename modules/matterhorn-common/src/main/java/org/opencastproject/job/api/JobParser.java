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

package org.opencastproject.job.api;

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
 * Marshals and unmarshals {@link Job}s.
 */
public final class JobParser {
  private static final JAXBContext jaxbContext;

  /** Disallow constructing this utility class */
  private JobParser() {
  }

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.job.api:org.opencastproject.serviceregistry.api");
    try {
      jaxbContext = JAXBContext.newInstance(sb.toString(), JobParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Parses an xml string representing a {@link Job}
   *
   * @param serializedForm
   *          The serialized data
   * @return The job
   */
  public static Job parseJob(String serializedForm) throws IOException {
    return parseJob(IOUtils.toInputStream(serializedForm, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link Job}
   *
   * @param in
   *          The serialized data
   * @param format
   *          the serialization format
   * @return The job
   */
  public static Job parseJob(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbJob.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Serializes the job into a string representation.
   *
   * @param job
   *          the job
   * @return the job's serialized form
   * @throws IOException
   *           if parsing fails
   */
  public static String toXml(Job job) throws IOException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(job, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  /**
   * Parses an xml string representing a {@link JaxbJobList}
   *
   * @param serializedForm
   *          The serialized data
   * @return The job list
   */
  public static JaxbJobList parseJobList(String serializedForm) throws IOException {
    return parseJobList(IOUtils.toInputStream(serializedForm, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link JaxbJobList}
   *
   * @param content
   *          the serialized data
   * @return the job list
   */
  public static JaxbJobList parseJobList(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbJobList.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
