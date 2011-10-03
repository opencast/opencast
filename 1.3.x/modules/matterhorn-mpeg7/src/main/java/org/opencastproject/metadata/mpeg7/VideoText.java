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

import java.awt.Rectangle;

/**
 * The <code>VideoText</code> element represents parts of the video with superimposed text. It is a subtype of a moving
 * region (<code>mpeg7:MovingRegionType</code>).
 * 
 * <pre>
 * &lt;complexType name="VideoTextType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="mpeg7:MovingRegionType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Text" type="mpeg7:TextualType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="textType" use="optional"&gt;
 *         &lt;simpleType&gt;
 *           &lt;union&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="NMTOKEN"&gt;
 *                 &lt;enumeration value="superimposed"/&gt;
 *                 &lt;enumeration value="scene"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="mpeg7:termAliasReferenceType"/&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="mpeg7:termURIReferenceType"/&gt;
 *             &lt;/simpleType&gt;
 *           &lt;/union&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="fontSize" type="positiveInteger" use="optional"/&gt;
 *       &lt;attribute name="fontType" type="string" use="optional"/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface VideoText extends MovingRegion {

  /** Video text type */
  enum Type {
    superimposed, scene
  };

  /**
   * Sets the segment identifier.
   * 
   * @param id
   *          the identifier
   */
  void setIdentifier(String id);

  /**
   * Returns the segment identifier.
   * 
   * @return the identifier
   */
  String getIdentifier();

  /**
   * Sets the text.
   * 
   * @param text
   *          the text
   */
  void setText(Textual text);

  /**
   * Returns the text.
   * 
   * @return the text
   */
  Textual getText();

  /**
   * Sets the videotext type.
   * 
   * @param type
   *          the type
   */
  void setType(Type type);

  /**
   * Rerturns the videotext type.
   * 
   * @return the type
   */
  Type getType();

  /**
   * Sets the optional font type.
   * 
   * @param fontType
   *          the font type
   */
  void setFontType(String fontType);

  /**
   * Returns the font type.
   * 
   * @return the font type
   */
  String getFontType();

  /**
   * Sets the optional font size.
   * 
   * @param size
   *          the font size
   */
  void setFontSize(int size);

  /**
   * Returns the font size.
   * 
   * @return the font size
   */
  int getFontSize();

  /**
   * Sets the text's bounding box.
   * 
   * @param rectangle
   *          the bounding box
   */
  void setBoundary(Rectangle rectangle);

  /**
   * Returns the text's bounding box.
   * 
   * @return the bounding box
   */
  Rectangle getBoundary();

}
