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

package org.opencastproject.serviceregistry.api;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Marshals and unmarshals {@link ServiceRegistration}s.
 */
public final class ServiceRegistrationParser {

  /** The jaxb context to use when creating marshallers and unmarshallers */
  private static final JAXBContext jaxbContext;

  /** Static initializer to setup the jaxb context */
  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.serviceregistry.api:org.opencastproject.job.api",
              ServiceRegistrationParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow construction of this utility class */
  private ServiceRegistrationParser() {
  }

}
