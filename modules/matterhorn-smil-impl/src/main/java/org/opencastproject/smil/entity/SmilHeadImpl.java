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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import org.opencastproject.smil.entity.api.SmilHead;
import org.opencastproject.smil.entity.api.SmilMeta;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.param.SmilMediaParamGroupImpl;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;

/**
 * {@link SmilHead} implementation.
 */
@XmlRootElement(name = "head")
public class SmilHeadImpl extends SmilObjectImpl implements SmilHead {

  /**
   * Meta data elements.
   */
  private List<SmilMeta> metas = new LinkedList<SmilMeta>();
  /**
   * Param group elements.
   */
  private List<SmilMediaParamGroup> paramGroups = new LinkedList<SmilMediaParamGroup>();

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SmilMeta> getMetas() {
    return Collections.unmodifiableList(metas);
  }

  /**
   * Returns {@link List} of {@link SmilMeta} objects.
   *
   * @return the meta data elements
   */
  @XmlElementRef(type = SmilMetaImpl.class)
  protected List<SmilMeta> getMetasList() {
    return metas;
  }

  /**
   * Set {@link List} with {@link SmilMeta} objects.
   *
   * @param metas the meta data elements to set
   */
  protected void setMetasList(List<SmilMeta> metas) {
    this.metas = metas;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SmilMediaParamGroup> getParamGroups() {
    return Collections.unmodifiableList(paramGroups);
  }

  /**
   * Returns {@link List} of {@link SmilMediaParamGroup} objects.
   *
   * @return the param groups
   */
  @XmlElementRef(type = SmilMediaParamGroupImpl.class)
  protected List<SmilMediaParamGroup> getParamGroupsList() {
    return paramGroups;
  }

  /**
   * Set {@link List} of {@link SmilMediaParamGroup} objects.
   *
   * @param paramGroups the param groups to set
   */
  protected void setParamGroupsList(List<SmilMediaParamGroup> paramGroups) {
    this.paramGroups = paramGroups;
  }

  /**
   * {@inheritedDoc}
   */
  @Override
  protected String getIdPrefix() {
    return "h";
  }

  /**
   * {@inheritedDoc}
   */
  @Override
  public SmilObject removeElement(String elementId) {
    SmilObject child = null;
    for (SmilObject meta : metas) {
      if (meta.getId().equals(elementId)) {
        child = meta;
        break;
      }
    }
    if (child != null) {
      metas.remove(child);
      return child;
    }

    for (SmilObject paramGroup : paramGroups) {
      if (paramGroup.getId().equals(elementId)) {
        child = paramGroup;
        break;
      } else {
        SmilObject removedElement = ((SmilMediaParamGroupImpl) paramGroup).removeElement(elementId);
        if (removedElement != null) {
          return removedElement;
        }
      }
    }
    if (child != null) {
      paramGroups.remove(child);
      return child;
    }
    return null;
  }

  /**
   * Remove all elements inside.
   */
  public void clear() {
    metas.clear();
    paramGroups.clear();
  }

  /**
   * Returns {@link SmilMeta} element with given name.
   *
   * @param name {@link SmilMeta} element name
   * @return{@link SmilMeta} element or null
   */
  public SmilMeta getMetaByName(String name) {
    for (SmilMeta m : metas) {
      if (m.getName().equals(name)) {
        return m;
      }
    }
    return null;
  }

  /**
   * Add new {@link SmilMeta} with given values.
   *
   * @param name {@link SmilMeta} name
   * @param content {@link SmilMeta} content
   * @return the new {@link SmilMeta}
   */
  public SmilMeta addMeta(String name, String content) {
    SmilMeta meta = getMetaByName(name);
    if (meta != null) {
      ((SmilMetaImpl) meta).setName(name);
      ((SmilMetaImpl) meta).setContent(content);
    } else {
      meta = new SmilMetaImpl(name, content);
      metas.add(meta);
    }
    return meta;
  }

  /**
   * Add given {@link SmilMeta} element.
   *
   * @param meta {@link SmilMeta} to add
   */
  public void addMeta(SmilMeta meta) {
    SmilObject m = getElementOrNull(meta.getId());
    if (m != null && m instanceof SmilMeta) {
      ((SmilMetaImpl) m).setName(meta.getName());
      ((SmilMetaImpl) m).setContent(meta.getContent());
    } else {
      metas.add(meta);
    }
  }

  /**
   * Remove {@link SmilMeta} element with given name.
   *
   * @param name {@link SmilMeta} name
   */
  public void removeMetaByName(String name) {
    SmilMeta metaFound = getMetaByName(name);
    if (metaFound != null) {
      metas.remove(metaFound);
    }
  }

  /**
   * Add param group and its content if not exists.
   *
   * @param group param group
   */
  public void addParamGroup(SmilMediaParamGroup group) {
    if (group == null) {
      return;
    }
    SmilObject g = getElementOrNull(group.getId());
    if (g != null && g instanceof SmilMediaParamGroup) {
      for (SmilMediaParam p : group.getParams()) {
        ((SmilMediaParamGroupImpl) g).addParam(p);
      }
    } else {
      paramGroups.add(group);
    }
  }

  /**
   * Remove given param group.
   *
   * @param group param group to remove
   */
  public void removeParamGroup(SmilMediaParamGroup group) {
    SmilObject g = getElementOrNull(group.getId());
    if (g != null && g instanceof SmilMediaParamGroup) {
      paramGroups.remove(g);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }
    SmilObject element = null;
    for (SmilMeta meta : metas) {
      element = ((SmilMetaImpl) meta).getElementOrNull(elementId);
      if (element != null) {
        return element;
      }
    }

    for (SmilMediaParamGroup paramGroup : paramGroups) {
      element = ((SmilMediaParamGroupImpl) paramGroup).getElementOrNull(elementId);
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
    for (SmilObject meta : getMetas()) {
      elements.add(meta);
      ((SmilObjectImpl) meta).putAllChilds(elements);
    }
    for (SmilObject paramGroup : getParamGroups()) {
      elements.add(paramGroup);
      ((SmilObjectImpl) paramGroup).putAllChilds(elements);
    }
  }
}
