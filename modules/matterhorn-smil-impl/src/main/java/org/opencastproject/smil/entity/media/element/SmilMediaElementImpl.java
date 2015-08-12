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

package org.opencastproject.smil.entity.media.element;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.SmilMediaObjectImpl;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.smil.entity.media.param.SmilMediaParamImpl;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;

/**
 * {@code SmilMediaElemnt} implementation.
 */
@XmlSeeAlso({SmilMediaAudioImpl.class, SmilMediaVideoImpl.class})
public abstract class SmilMediaElementImpl extends SmilMediaObjectImpl implements SmilMediaElement {

  /**
   * SMIL src URI
   */
  private URI src;
  /**
   * SMIL clipBegin attribute
   */
  private String clipBegin;
  /**
   * SMIL clipEnd attribute
   */
  private String clipEnd;
  /**
   * SMIL paramGroup Id attribute
   */
  private String paramGroupId;
  /**
   * SMIL param elements
   */
  private List<SmilMediaParam> params = new LinkedList<SmilMediaParam>();

  /**
   * Empty constructor, needed for JAXB.
   */
  private SmilMediaElementImpl() {
    this(null, null, null);
  }

  /**
   * Constructor.
   *
   * @param src media source URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   */
  public SmilMediaElementImpl(URI src, String clipBegin, String clipEnd) {
    this(src, clipBegin, clipEnd, null);
  }

  /**
   * Constructor.
   *
   * @param src media source URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaElementImpl(URI src, String clipBegin, String clipEnd, String paramGroupId) {
    this.src = src;
    this.clipBegin = clipBegin;
    this.clipEnd = clipEnd;
    this.paramGroupId = paramGroupId;
  }

  /**
   * Constructor.
   *
   * @param src media source URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   */
  public SmilMediaElementImpl(URI src, long clipBeginMS, long clipEndMS) {
    this(src, clipBeginMS + "ms", clipEndMS + "ms", null);
  }

  /**
   * Constructor.
   *
   * @param src media source URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaElementImpl(URI src, long clipBeginMS, long clipEndMS, String paramGroupId) {
    this(src, clipBeginMS + "ms", clipEndMS + "ms", paramGroupId);
  }

  /**
   * {@inheritDoc}
   */
  @XmlAttribute(name = "src")
  @Override
  public URI getSrc() {
    return src;
  }

  /**
   * Set src attribute.
   *
   * @param src the src URI to set
   */
  public void setSrc(URI src) {
    this.src = src;
  }

  /**
   * {@inheritDoc}
   */
  @XmlAttribute(name = "clipBegin")
  @Override
  public String getClipBegin() {
    return clipBegin;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getClipBeginMS() throws SmilException {
    return convertTimeToMS(clipBegin);
  }

  /**
   * Set clipBegin attribute.
   *
   * @param clipBegin the clipBegin to set
   */
  public void setClipBegin(String clipBegin) {
    this.clipBegin = clipBegin;
  }

  /**
   * {@inheritDoc}
   */
  @XmlAttribute(name = "clipEnd")
  @Override
  public String getClipEnd() {
    return clipEnd;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getClipEndMS() throws SmilException {
    return convertTimeToMS(clipEnd);
  }

  /**
   * Set clipEnd attribute.
   *
   * @param clipEnd the clipEnd to set
   */
  public void setClipEnd(String clipEnd) {
    this.clipEnd = clipEnd;
  }

  /**
   * {@inheritDoc}
   */
  @XmlAttribute(name = "paramGroup")
  @Override
  public String getParamGroup() {
    return paramGroupId;
  }

  /**
   * Set paramGroup Id attribute.
   *
   * @param paramGroup the paramGroup Id to set
   */
  public void setParamGroup(String paramGroupId) {
    this.paramGroupId = paramGroupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SmilMediaParam> getParams() {
    return Collections.unmodifiableList(params);
  }

  /**
   * Returns {@link List} of {@link SmilMediaParam}s.
   *
   * @return the params list
   */
  @XmlElementRef(type = SmilMediaParamImpl.class)
  protected List<SmilMediaParam> getParamsList() {
    return params;
  }

  /**
   * Set {@link List} of {@link SmilMediaParam}s.
   *
   * @param params the params list to set
   */
  protected void setParamsList(List<SmilMediaParam> params) {
    this.params = params;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isContainer() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract MediaType getMediaType();

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    for (SmilMediaParam media : getParams()) {
      SmilObject element = ((SmilMediaParamImpl) media).getElementOrNull(elementId);
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
  public SmilObject removeElement(String elementId) {
    SmilObject child = null;
    for (SmilObject element : params) {
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
      params.remove(child);
    }
    return child;
  }

  /**
   * Convert time unit to milliseconds.
   *
   * @param timeUnit to convert
   * @return time unit in milliseconds
   * @throws SmilException if time unit format can't parsed
   */
  protected long convertTimeToMS(String timeUnit) throws SmilException {
    TimeUnit unit;
    long time = -1;
    if (timeUnit.endsWith("ms")) {
      unit = TimeUnit.MILLISECONDS;
      time = Long.parseLong(timeUnit.replace("ms", "").trim());
    } else if (timeUnit.endsWith("s")) {
      unit = TimeUnit.SECONDS;
      time = (long) Double.parseDouble(timeUnit.replace("s", "").trim());
    } else {
      // TODO: parse other formats
      throw new SmilException("failed parsing time unit");
    }

    return unit.toMillis(time);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAllChilds(List<SmilObject> elements) {
    for (SmilObject child : getParams()) {
      elements.add(child);
      ((SmilObjectImpl) child).putAllChilds(elements);
    }
  }
}
