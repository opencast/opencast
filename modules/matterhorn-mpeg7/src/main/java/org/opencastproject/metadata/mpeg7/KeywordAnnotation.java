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

import org.opencastproject.mediapackage.XmlElement;

/**
 * Models a keyword annotation with relevance, confidence and the keyword itself.
 *
 * <pre>
 * &lt;complexType name=&quot;KeywordAnnotationType&quot;&gt;
 *   &lt;sequence&gt;
 *       &lt;element name=&quot;Keyword&quot; minOccurs=&quot;1&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *           &lt;complexType&gt;
 *               &lt;simpleContent&gt;
 *                   &lt;extension base=&quot;mpeg7:TextualType&quot;&gt;
 *                       &lt;attribute name=&quot;type&quot; use=&quot;optional&quot; default=&quot;main&quot;&gt;
 *                           &lt;simpleType&gt;
 *                               &lt;restriction base=&quot;NMTOKEN&quot;&gt;
 *                                   &lt;enumeration value=&quot;main&quot;/&gt;
 *                                   &lt;enumeration value=&quot;secondary&quot;/&gt;
 *                                   &lt;enumeration value=&quot;other&quot;/&gt;
 *                               &lt;/restriction&gt;
 *                           &lt;/simpleType&gt;
 *                       &lt;/attribute&gt;
 *                   &lt;/extension&gt;
 *               &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *       &lt;/element&gt;
 *   &lt;/sequence&gt;
 *   &lt;attribute ref=&quot;xml:lang&quot; use=&quot;optional&quot;/&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface KeywordAnnotation extends XmlElement {

  /**
   * Enumeration defining possible types for a keyword annotation.
   */
  enum Type {
    main, secondary, other
  };

  /**
   * Returns the keyword.
   *
   * @return the keyword
   */
  String getKeyword();

  /**
   * Returns the type of this keyword annotation. The default value is <code>main</code>.
   *
   * @return the keyword type
   */
  Type getType();

}
