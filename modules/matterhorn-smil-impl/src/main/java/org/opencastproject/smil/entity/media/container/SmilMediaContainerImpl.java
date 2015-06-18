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

package org.opencastproject.smil.entity.media.container;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.SmilMediaObjectImpl;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;

/**
 * {@link SmilMediaContainer} abstract class.
 */
@XmlSeeAlso({SmilMediaParallelImpl.class, SmilMediaSequenceImpl.class})
public abstract class SmilMediaContainerImpl extends SmilMediaObjectImpl implements SmilMediaContainer {

  /**
   * SMIL media elements inside
   */
  private List<SmilMediaObject> elements = new LinkedList<SmilMediaObject>();

  /**
   * {@inheritDoc }
   */
  @Override
  public boolean isContainer() {
    return true;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public List<SmilMediaObject> getElements() {
    return Collections.unmodifiableList(elements);
  }

  /**
   * Returns {@link List} of {@link SmilMediaObject}s.
   *
   * @return the SMIL media objects
   */
  @XmlElementRef(type = SmilMediaObjectImpl.class)
  protected List<SmilMediaObject> getElementsList() {
    return elements;
  }

  /**
   * Set {@link List} of {@link SmilMediaObject}s.
   *
   * @param elements SMIL media objects to set
   */
  protected void setElementsList(List<SmilMediaObject> elements) {
    this.elements = elements;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public abstract ContainerType getContainerType();

  /**
   * {@inheritDoc }
   */
  @Override
  public boolean isParentOf(String childId) {
    for (SmilMediaObject child : elements) {
      if (child.getId().equals(childId)) {
        return true;
      } else if (child.isContainer() && ((SmilMediaContainer) child).isParentOf(childId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add new {@link SmilMediaObject} inside an child element with given parent
   * Id.
   *
   * @param mediaObject to add
   * @param parentId where to add
   * @throws SmilException if there is no element with given Id
   */
  public void addMediaObject(SmilMediaObject mediaObject, String parentId) throws SmilException {
    if (getId().equals(parentId)) {
      elements.add(mediaObject);
    } else {
      // iterate over all child elements
      for (SmilMediaObject child : elements) {
        // if child is a media container (only media container can have other media elements inside)
        // and if childs Id is equals parentId or contain an element with ParentId
        if (child.isContainer() && ((SmilMediaContainer) child).isParentOf(parentId)) {
          // add mediaObject there
          ((SmilMediaContainerImpl) child).addMediaObject(mediaObject, parentId);
        }
      }
      throw new SmilException("There is no element with id " + parentId);
    }
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    for (SmilMediaObject media : getElements()) {
      SmilObject element = ((SmilMediaObjectImpl) media).getElementOrNull(elementId);
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAllChilds(List<SmilObject> elements) {
    for (SmilObject child : getElements()) {
      elements.add(child);
      ((SmilObjectImpl) child).putAllChilds(elements);
    }
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject removeElement(String elementId) {
    SmilObject child = null;
    for (SmilObject element : elements) {
      if (element.getId().equals(elementId)) {
        child = element;
        break;
      } else {
        SmilObject removed = ((SmilObjectImpl) element).removeElement(elementId);
        if (removed != null) {
          return removed;
        }
      }
    }
    if (child != null) {
      elements.remove(child);
    }
    return child;
  }
}
