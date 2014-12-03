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
import javax.xml.bind.annotation.XmlID;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.smil.entity.api.SmilObject;

/**
 * Implementation of {@link SmilObject}.
 */
public abstract class SmilObjectImpl implements SmilObject {

  /**
   * Id builder
   */
  private static final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();
  /**
   * Smil object Id
   */
  private String id;

  /**
   * Constructor. Generate a new Id.
   */
  public SmilObjectImpl() {
    id = String.format("%s-%s", getIdPrefix(), idBuilder.createNew().compact());
  }

  /**
   * Constructor.
   *
   * @param id Id to set
   */
  public SmilObjectImpl(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc }
   */
  @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
  @XmlID
  @Override
  public String getId() {
    return id;
  }

  /**
   * Set Id.
   *
   * @param id Id to set
   */
  private void setId(String id) {
    this.id = id;
  }

  /**
   * Returns {@link SmilObject} Id prefix (must begin with alphanumeric
   * charackter).
   *
   * @return Id prefix
   */
  protected abstract String getIdPrefix();

  /**
   * Returns element with given elementId or null.
   *
   * @param elementId element Id
   * @return element with given elementId or null
   */
  public abstract SmilObject getElementOrNull(String elementId);

  /**
   * Put all containing elements into {@link List} given as parameter.
   *
   * @param a {@link List} where to pul child elements to
   */
  public abstract void putAllChilds(List<SmilObject> elements);

  /**
   * Remove element with given Id and returns it. Returns null if there is no
   * element with given Id.
   *
   * @param elementId element Id
   * @return removed element or null
   */
  public abstract SmilObject removeElement(String elementId);
}
