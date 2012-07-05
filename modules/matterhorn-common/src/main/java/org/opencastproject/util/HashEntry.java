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
package org.opencastproject.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map.Entry;

/**
 * JaxB implementation of the entry of a Hashtable, so that the element can be serialized in the intendet way The Entry
 * now looks <item key="key"><value>value</value></item>
 * 
 */
@XmlType(name = "hash-entry", namespace = "http://util.opencastproject.org")
@XmlRootElement(name = "hash-entry", namespace = "http://util.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class HashEntry implements Entry<String, String> {

  @XmlAttribute(name = "key")
  protected String key;

  @XmlElement
  protected String value;

  public HashEntry() {
  }

  public HashEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map.Entry#getKey()
   */
  @Override
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map.Entry#getValue()
   */
  @Override
  public String getValue() {
    return value;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map.Entry#setValue(java.lang.Object)
   */
  @Override
  public String setValue(String value) {
    this.value = value;
    return this.value;
  }

}
