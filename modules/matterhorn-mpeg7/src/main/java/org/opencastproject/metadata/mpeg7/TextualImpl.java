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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Default implementation of a <code>TextualBase</code> mpeg-7 element.
 */
public class TextualImpl implements Textual {

  /** The default phonetic alphabet */
  public static final String DEFAULT_PHONETIC_ALPHABET = "sampa";

  /** The text */
  protected String text = null;

  /** The text */
  protected String language = null;

  /** The phonetic transcription */
  protected String transcription = null;

  /** The phonetic alphabet */
  protected String alphabet = DEFAULT_PHONETIC_ALPHABET;

  /**
   * Creates a new textual base element.
   */
  public TextualImpl() {
  }

  /**
   * Creates a new textual base element.
   *
   * @param text
   *          the text
   * @throws IllegalArgumentException
   *           if the text is <code>null</code> or empty
   */
  public TextualImpl(String text) {
    this(text, null);
  }

  /**
   * Creates a new Textual element from a number of words.
   *
   * @param words
   *          the words
   */
  public TextualImpl(String[] words) {
    this(words, null);
  }

  /**
   * Creates a new textual base element.
   *
   * @param text
   *          the text
   * @throws IllegalArgumentException
   *           if the text is <code>null</code> or empty
   */
  public TextualImpl(String text, String language) {
    if (StringUtils.trimToNull(text) == null)
      throw new IllegalArgumentException("The text cannot be empty");
    this.text = text;
    this.language = language;
  }

  /**
   * Creates a new textual base element.
   *
   * @param words
   *          the words
   * @throws IllegalArgumentException
   *           if the words is <code>null</code> or empty
   */
  public TextualImpl(String[] words, String language) {
    this(StringUtils.join(words, ' '), null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#getLanguage()
   */
  @Override
  public String getLanguage() {
    return language;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#getPhoneticAlphabet()
   */
  @Override
  public String getPhoneticAlphabet() {
    return alphabet;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#getPhoneticTranscription()
   */
  @Override
  public String getPhoneticTranscription() {
    return transcription;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#setText(java.lang.String)
   */
  @Override
  public void setText(String text) {
    if (StringUtils.trimToNull(text) == null) {
      throw new IllegalArgumentException("The text cannot be empty");
    } else {
      this.text = text;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#getText()
   */
  @Override
  public String getText() {
    return text;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#setLanguage(java.lang.String)
   */
  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.Textual#setPhoneticTranscription(java.lang.String, java.lang.String)
   */
  @Override
  public void setPhoneticTranscription(String transcription, String alphabet) {
    if (transcription != null && alphabet == null)
      alphabet = DEFAULT_PHONETIC_ALPHABET;
    this.transcription = transcription;
    this.alphabet = alphabet;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  @Override
  public Node toXml(Document document) {
    Element node = document.createElement("Text");
    if (language != null)
      node.setAttribute("xml:lang", language);
    if (transcription != null) {
      node.setAttribute("phoneticTranscription", transcription);
      node.setAttribute("phoneticAlphabet", alphabet);
    }
    node.appendChild(document.createTextNode(text));
    return node;
  }

}
