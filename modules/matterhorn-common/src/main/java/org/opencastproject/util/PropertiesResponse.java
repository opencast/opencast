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

package org.opencastproject.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Properties;

/**
 * A handy wrapper to wrap Properties objects for automated JAXB serialization.
 */
@XmlType(name = "properties-response", namespace = "http://common.opencastproject.org")
@XmlRootElement(name = "properties-response", namespace = "http://common.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertiesResponse {

  @XmlJavaTypeAdapter(HashtableAdapter.class)
  private Properties properties;

  public PropertiesResponse() {
  }

  public PropertiesResponse(Properties properties) {
    this.properties = properties;
  }

  public Properties getProperties() {
    return this.properties;
  }
}
