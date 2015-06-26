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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;

/**
 * {@link SmilMediaParamGroup} implementation.
 */
@XmlRootElement(name = "paramGroup")
public class SmilMediaParamGroupImpl extends SmilObjectImpl implements SmilMediaParamGroup {

  /**
   * SMIL param list
   */
  private List<SmilMediaParam> params = new LinkedList<SmilMediaParam>();

  /**
   * {@inheritDoc }
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
   * {@inheritDoc }
   */
  @Override
  protected String getIdPrefix() {
    return "pg";
  }

  /**
   * Returns {@link SmilMediaParam} with given name.
   *
   * @param name param name
   * @return {@link SmilMediaParam} with given name or null
   */
  public SmilMediaParam getParamByName(String name) {
    for (SmilMediaParam p : params) {
      if (p.getName().equals(name)) {
        return p;
      }
    }
    return null;
  }

  /**
   * Add new {@link SmilMediaParam} with given name and vaalue.
   *
   * @param name param name
   * @param value param value
   * @return new {@link SmilMediaParam}
   */
  public SmilMediaParam addParam(String name, String value) {
    SmilMediaParam param = getParamByName(name);
    if (param == null) {
      param = new SmilMediaParamImpl(name, value);
    }
    params.add(param);
    return param;
  }

  /**
   * Add given {@link SmilMediaParam}.
   *
   * @param param to add
   */
  public void addParam(SmilMediaParam param) {
    if (param == null) {
      return;
    }
    SmilObject p = getElementOrNull(param.getId());
    if (p != null && p instanceof SmilMediaParam) {
      ((SmilMediaParamImpl) p).setName(param.getName());
      ((SmilMediaParamImpl) p).setValue(param.getValue());
    } else {
      params.add(param);
    }
  }

  /**
   * Remove {@link SmilMediaParam} with given name.
   *
   * @param name param name
   */
  public void removeParamByName(String name) {
    removeParam(getParamByName(name));
  }

  /**
   * Remove given {@link SmilMediaParam}.
   *
   * @param param to remove
   */
  public void removeParam(SmilMediaParam param) {
    if (param == null) {
      return;
    }
    if (params.contains(param)) {
      params.remove(param);
    }
  }

  /**
   * {@inheritDoc }
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
   * {
   *
   * @inheritDocs}
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    for (SmilMediaParam param : getParams()) {
      SmilObject element = ((SmilMediaParamImpl) param).getElementOrNull(elementId);
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
    for (SmilObject child : getParams()) {
      elements.add(child);
      ((SmilObjectImpl) child).putAllChilds(elements);
    }
  }
}
