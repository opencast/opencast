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

package org.opencastproject.composer.api;

import org.opencastproject.util.XmlSafeParser;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * Provides a mechanism to transform {@link EncodingProfile}s to and from xml.
 */
public final class EncodingProfileBuilder {

  /** The singleton instance for this factory */
  private static EncodingProfileBuilder instance = null;

  protected JAXBContext jaxbContext = null;

  private EncodingProfileBuilder() throws JAXBException {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.composer.api");
    jaxbContext = JAXBContext.newInstance(sb.toString(), EncodingProfileBuilder.class.getClassLoader());
  }

  /**
   * Returns an instance of the {@link EncodingProfileBuilder}.
   *
   * @return a factory
   */
  public static EncodingProfileBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new EncodingProfileBuilder();
      } catch (JAXBException e) {
        throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  /**
   * Loads an encoding profile from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the encoding profile
   * @throws Exception
   *           if creating the profile fails
   */
  public EncodingProfile parseProfile(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    try {
      return unmarshaller.unmarshal(XmlSafeParser.parse(in), EncodingProfileImpl.class).getValue();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Loads an encoding profile list from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the encoding profile list
   * @throws Exception
   *           if creating the profile list fails
   */
  public EncodingProfileList parseProfileList(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    try {
      return unmarshaller.unmarshal(XmlSafeParser.parse(in), EncodingProfileList.class).getValue();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
