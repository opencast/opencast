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

package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * TODO: Comment me!
 */
public class KeywordAnnotationImpl implements KeywordAnnotation {

  /** The keyword */
  protected String keyword = null;

  /** Keyword type */
  protected KeywordAnnotation.Type type = null;

  /**
   * Creates a new keyword annotation.
   * 
   * @param keyword
   *          the keyword
   */
  public KeywordAnnotationImpl(String keyword) {
    this(keyword, null);
  }

  /**
   * Creates a new keyword annotation.
   * 
   * @param keyword
   *          the keyword
   * @param type
   *          the type
   */
  public KeywordAnnotationImpl(String keyword, KeywordAnnotation.Type type) {
    this.keyword = keyword;
    this.type = type;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.KeywordAnnotation#getKeyword()
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.KeywordAnnotation#getType()
   */
  public Type getType() {
    return type;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("Keyword");
    node.setTextContent(keyword);
    if (type != null)
      node.setAttribute("type", type.toString());
    return node;
  }

}
