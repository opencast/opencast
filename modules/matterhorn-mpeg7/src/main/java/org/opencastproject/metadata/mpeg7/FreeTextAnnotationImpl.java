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
public class FreeTextAnnotationImpl implements FreeTextAnnotation {

  /** The text annotation */
  protected String text = null;

  /**
   * Creates a new free text annotation.
   *
   * @param text
   *          the annotation
   */
  public FreeTextAnnotationImpl(String text) {
    this.text = text;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.FreeTextAnnotation#getText()
   */
  public String getText() {
    return text;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("FreeTextAnnotation");
    node.setTextContent(text);
    return node;
  }

}
