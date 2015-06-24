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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of text annotations.
 */
public class TextAnnotationImpl implements TextAnnotation {

  /** Number formatter, used to deal with relevance values in a locale independent way */
  private static DecimalFormatSymbols standardSymbols = new DecimalFormatSymbols(Locale.US);

  /** Confidence value */
  protected float confidence = -1.0f;

  /** Relevance value */
  protected float relevance = -1.0f;

  /** Language identifier */
  protected String language = null;

  /** Keyword annotations */
  protected List<KeywordAnnotation> keywordAnnotations = null;

  /** Free text annotations */
  protected List<FreeTextAnnotation> freeTextAnnotations = null;

  static {
    standardSymbols.setDecimalSeparator('.');
  }

  /**
   * Creates a new text annotation.
   *
   * @param confidence
   *          the confidence value <code>[0.0..1.0]</code>
   * @param relevance
   *          the relevance value <code>[0.0..1.0]</code>
   * @param language
   *          the language identifier
   */
  public TextAnnotationImpl(float confidence, float relevance, String language) {
    this.confidence = confidence;
    this.relevance = relevance;
    this.language = language;
    keywordAnnotations = new ArrayList<KeywordAnnotation>();
    freeTextAnnotations = new ArrayList<FreeTextAnnotation>();
  }

  /**
   * Adds the keyword to this annotation.
   *
   * @param keyword
   *          the keyword
   */
  public void addKeyword(String keyword) {
    addKeywordAnnotation(new KeywordAnnotationImpl(keyword));
  }

  /**
   * Adds the keyword to this annotation.
   *
   * @param keyword
   *          the keyword
   * @param type
   *          the keyword type
   */
  public void addKeyword(String keyword, KeywordAnnotation.Type type) {
    addKeywordAnnotation(new KeywordAnnotationImpl(keyword, type));
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#addKeywordAnnotation(org.opencastproject.mediapackage.mpeg7.KeywordAnnotation)
   */
  public void addKeywordAnnotation(KeywordAnnotation keywordAnnotation) {
    keywordAnnotations.add(keywordAnnotation);
  }

  /**
   * Adds free text to this annotation.
   *
   * @param text
   *          the free text
   */
  public void addFreeText(String text) {
    addFreeTextAnnotation(new FreeTextAnnotationImpl(text));
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#addFreeTextAnnotation(org.opencastproject.mediapackage.mpeg7.FreeTextAnnotation)
   */
  public void addFreeTextAnnotation(FreeTextAnnotation freeTextAnnotation) {
    freeTextAnnotations.add(freeTextAnnotation);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#getConfidence()
   */
  public float getConfidence() {
    return confidence;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#getLanguage()
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#getRelevance()
   */
  public float getRelevance() {
    return relevance;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#keywordAnnotations()
   */
  public Iterator<KeywordAnnotation> keywordAnnotations() {
    return keywordAnnotations.iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.TextAnnotation#freeTextAnnotations()
   */
  public Iterator<FreeTextAnnotation> freeTextAnnotations() {
    return freeTextAnnotations.iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    DecimalFormat format = new DecimalFormat("0.0");
    format.setDecimalFormatSymbols(standardSymbols);
    Element node = document.createElement("TextAnnotation");
    if (confidence >= 0.0)
      node.setAttribute("confidence", format.format(confidence));
    if (relevance >= 0.0)
      node.setAttribute("relevance", format.format(relevance));
    if (language != null)
      node.setAttribute("xml:lang", language);

    // Keyword anntiations
    if (keywordAnnotations.size() > 0) {
      Element kwAnnotationNode = document.createElement("KeywordAnnotation");
      for (KeywordAnnotation annotation : keywordAnnotations) {
        kwAnnotationNode.appendChild(annotation.toXml(document));
      }
      node.appendChild(kwAnnotationNode);
    }

    // Free text anntiations
    for (FreeTextAnnotation annotation : freeTextAnnotations) {
      node.appendChild(annotation.toXml(document));
    }

    return node;
  }

}
