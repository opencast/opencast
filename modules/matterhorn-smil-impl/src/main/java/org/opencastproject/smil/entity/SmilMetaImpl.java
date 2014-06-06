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
package org.opencastproject.smil.entity;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.opencastproject.smil.entity.api.SmilMeta;
import org.opencastproject.smil.entity.api.SmilObject;

/**
 * {@link SmilMeta} implemetation.
 */
@XmlRootElement(name = "meta")
public class SmilMetaImpl extends SmilObjectImpl implements SmilMeta {

  /**
   * SMIL meta name
   */
  private String name;
  /**
   * SMIL meta content
   */
  private String content;

  /**
   * Empty constructor.
   */
  private SmilMetaImpl() {
    this("", "");
  }

  /**
   * Constructor.
   *
   * @param name meta name
   * @param content meta content
   */
  public SmilMetaImpl(String name, String content) {
    this.name = name;
    this.content = content;
  }

  /**
   * {@inheritDoc}
   */
  @XmlAttribute(name = "name", required = true)
  @Override
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  protected void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc }
   */
  @XmlAttribute(name = "content", required = true)
  @Override
  public String getContent() {
    return content;
  }

  /**
   * @param content the content to set
   */
  protected void setContent(String content) {
    this.content = content;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  protected String getIdPrefix() {
    return "meta";
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
    } else {
      return null;
    }
  }

  @Override
  public void putAllChilds(List<SmilObject> elements) {
    // Meta elements hasn't childs
  }
}

