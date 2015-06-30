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

package org.opencastproject.smil.entity.media.param;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;

/**
 * {@link SmilMediaParam} implementation.
 */
@XmlRootElement(name = "param")
public class SmilMediaParamImpl extends SmilObjectImpl implements SmilMediaParam {

  // allowed valuetypes: data, ref, object
  // http://www.w3.org/TR/smil/smil-extended-media-object.html#smilMediaNS-MediaParam
  /**
   * SMIL valueType attribute (constant: data)
   */
  @XmlAttribute(name = "valuetype")
  private final String valueType = "data";
  /**
   * SMIL param name attribute
   */
  private String name;
  /**
   * SMIL param value attribute
   */
  private String value;

  /**
   * Empty constructor (needed for JAXB).
   */
  private SmilMediaParamImpl() {
    this(null, null);
  }

  /**
   * Constructor.
   *
   * @param name param name
   * @param value param value
   */
  public SmilMediaParamImpl(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * {@inheritDoc }
   */
  @XmlAttribute(required = true)
  @Override
  public String getName() {
    return name;
  }

  /**
   * Set param name.
   *
   * @param name the name to set
   */
  protected void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc }
   */
  @XmlAttribute(required = true)
  @Override
  public String getValue() {
    return value;
  }

  /**
   * Set param value.
   *
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  protected String getIdPrefix() {
    return "param";
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject removeElement(String elementId) {
    return null;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAllChilds(List<SmilObject> elements) {
    // param does not have any elements inside
  }
}
