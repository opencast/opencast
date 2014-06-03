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
 * Textual is a generic text type:
 *
 * <pre>
 * &lt;complexType name="TextualBaseType" abstract="true"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="string"&gt;
 *       &lt;attribute ref="xml:lang" use="optional"/&gt;
 *       &lt;attribute name="phoneticTranscription" use="optional"&gt;
 *         &lt;simpleType&gt;
 *           &lt;list itemType="mpeg7:PhoneType"/&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="phoneticAlphabet" type="mpeg7:phoneticAlphabetType" use="optional" default="sampa"/&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface Textual extends XmlElement {

  /**
   * Sets the text.
   *
   * @param text
   *          the text
   */
  void setText(String text);

  /**
   * Returns the actual text.
   *
   * @return the text
   */
  String getText();

  /**
   * Sets the language, which must correspond to the defininition of <code>xml:lang</code>.
   *
   * @param language
   *          the language
   */
  void setLanguage(String language);

  /**
   * Returns the text's language.
   *
   * @return the language
   */
  String getLanguage();

  /**
   * Sets the phonetic transcription and specifies which alphabet was used to produce it.
   *
   * @param transcription
   *          the transcription
   * @param alphabet
   *          the alphabet
   */
  void setPhoneticTranscription(String transcription, String alphabet);

  /**
   * Returns the optional phonetic transcription.
   *
   * @return the phonetic transcription
   */
  String getPhoneticTranscription();

  /**
   * Returns the optional phonetic alphabet or the default value <code>sampa</code> if no alphabet has been specified.
   *
   * @return the phonetic alphabet
   */
  String getPhoneticAlphabet();

}
