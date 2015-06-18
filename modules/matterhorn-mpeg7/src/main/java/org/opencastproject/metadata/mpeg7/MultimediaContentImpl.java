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


package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TODO: Comment me!
 */
public class MultimediaContentImpl<T extends MultimediaContentType> implements MultimediaContent<T> {

  /** List of content elements */
  protected Map<String, T> content = null;

  /** The content type */
  protected MultimediaContent.Type type = null;

  /**
   * Creates a new multimedia content container.
   *
   * @param type
   *          the content type
   */
  public MultimediaContentImpl(MultimediaContent.Type type) {
    this.content = new HashMap<String, T>();
    this.type = type;
  }

  /**
   * Adds a content element to the collection.
   *
   * @param c
   *          the content to add
   */
  public void add(T c) {
    if (c == null)
      throw new IllegalArgumentException("Multimedia content must not be null");
    if (content.containsKey(c.getId()))
      throw new IllegalStateException("Duplicate content id detected: " + c.getId());
    content.put(c.getId(), c);
  }

  /**
   * Removes the content element from the collection.
   *
   * @param c
   *          the content to remove
   */
  public T remove(T c) {
    return content.remove(c.getId());
  }

  /**
   * Removes the content element with the given identifier from the collection.
   *
   * @param id
   *          the content identifier
   */
  public T remove(String id) {
    return content.remove(id);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MultimediaContent#elements()
   */
  public Iterator<T> elements() {
    return content.values().iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MultimediaContent#getElementById(java.lang.String)
   */
  public T getElementById(String id) {
    return content.get(id);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MultimediaContent#getType()
   */
  public MultimediaContent.Type getType() {
    return type;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MultimediaContent#size()
   */
  public int size() {
    return content.size();
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("MultimediaContent");
    node.setAttribute("xsi:type", type.toString());
    for (T contentElement : content.values()) {
      node.appendChild(contentElement.toXml(document));
    }
    return node;
  }

}
