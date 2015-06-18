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

package org.opencastproject.smil.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.entity.api.SmilBody;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.SmilMediaObjectImpl;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.SmilMediaContainerImpl;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;

/**
 * {@link SmilBody} implementation.
 */
@XmlRootElement(name = "body")
public class SmilBodyImpl extends SmilObjectImpl implements SmilBody {

  /**
   * Media Elements
   */
  private List<SmilMediaObject> mediaElements = new LinkedList<SmilMediaObject>();

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SmilMediaObject> getMediaElements() {
    return Collections.unmodifiableList(mediaElements);
  }

  /**
   * Returns SMIL media elements.
   *
   * @return the media elements
   */
  @XmlElementRef(type = SmilMediaObjectImpl.class)
  protected List<SmilMediaObject> getMediaObjects() {
    return mediaElements;
  }

  /**
   * Set SMIL media elements.
   *
   * @param mediaElements the mediae lements to set
   */
  protected void setMediaObjects(List<SmilMediaObject> mediaElements) {
    this.mediaElements = mediaElements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getIdPrefix() {
    return "b";
  }

  /**
   * Add given {@link SmilMediaObject} to the element with given Id.
   *
   * @param mediaObject to add
   * @param parentId where to add new media
   * @throws SmilException if there is no element inside with given Id
   */
  public void addMediaElement(SmilMediaObject mediaObject, String parentId) throws SmilException {
    if (getId().equals(parentId)) {
      mediaElements.add(mediaObject);
    } else {
      SmilMediaElement parent = null;
      for (SmilMediaObject element : mediaElements) {
        if (element.isContainer() && (element.getId().equals(parentId)
                || ((SmilMediaContainer) element).isParentOf(parentId))) {
          ((SmilMediaContainerImpl) element).addMediaObject(mediaObject, parentId);
          return;
        }
      }
      throw new SmilException("There is no element with id " + parentId);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject removeElement(String elementId) {
    SmilMediaObject child = null;
    for (SmilMediaObject element : mediaElements) {
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
      mediaElements.remove(child);
    }
    return child;
  }

  /**
   * Remove all media Elements inside.
   */
  public void clear() {
    mediaElements.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    for (SmilMediaObject media : getMediaElements()) {
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
    for (SmilObject child : getMediaElements()) {
      elements.add(child);
      ((SmilObjectImpl) child).putAllChilds(elements);
    }
  }
}
