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

import org.opencastproject.mediapackage.XmlElement;

import java.util.Iterator;

/**
 * A video segment represents a temporal decomposition of the video stream that may have properties like text
 * annotations attached to it.
 *
 * <pre>
 * &lt;complexType name="SegmentType" abstract="true"&gt;
 *   &lt;complexContent&gt;
 *       &lt;extension base="mpeg7:DSType"&gt;
 *           &lt;sequence&gt;
 *               &lt;choice minOccurs="0"&gt;
 *                   &lt;element name="MediaInformation" type="mpeg7:MediaInformationType"/&gt;
 *                   &lt;element name="MediaInformationRef" type="mpeg7:ReferenceType"/&gt;
 *                   &lt;element name="MediaLocator" type="mpeg7:MediaLocatorType"/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name="StructuralUnit" type="mpeg7:ControlledTermUseType" minOccurs="0"/&gt;
 *               &lt;choice minOccurs="0"&gt;
 *                   &lt;element name="CreationInformation" type="mpeg7:CreationInformationType"/&gt;
 *                   &lt;element name="CreationInformationRef" type="mpeg7:ReferenceType"/&gt;
 *               &lt;/choice&gt;
 *               &lt;choice minOccurs="0"&gt;
 *                   &lt;element name="UsageInformation" type="mpeg7:UsageInformationType"/&gt;
 *                   &lt;element name="UsageInformationRef" type="mpeg7:ReferenceType"/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name="TextAnnotation" minOccurs="0" maxOccurs="unbounded"&gt;
 *                   &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                           &lt;extension base="mpeg7:TextAnnotationType"&gt;
 *                               &lt;attribute name="type" use="optional"&gt;
 *                                   &lt;simpleType&gt;
 *                                       &lt;union memberTypes="mpeg7:termReferenceType string"/&gt;
 *                                   &lt;/simpleType&gt;
 *                               &lt;/attribute&gt;
 *                           &lt;/extension&gt;
 *                       &lt;/complexContent&gt;
 *                   &lt;/complexType&gt;
 *               &lt;/element&gt;
 *               &lt;choice minOccurs="0" maxOccurs="unbounded"&gt;
 *                   &lt;element name="Semantic" type="mpeg7:SemanticType"/&gt;
 *                   &lt;element name="SemanticRef" type="mpeg7:ReferenceType"/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name="MatchingHint" type="mpeg7:MatchingHintType" minOccurs="0" maxOccurs="unbounded"/&gt;
 *               &lt;element name="PointOfView" type="mpeg7:PointOfViewType" minOccurs="0" maxOccurs="unbounded"/&gt;
 *               &lt;element name="Relation" type="mpeg7:RelationType" minOccurs="0" maxOccurs="unbounded"/&gt;
 *           &lt;/sequence&gt;
 *       &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface Segment extends XmlElement {

  /** The segment type */
  enum Type {
    AudioSegment, VideoSegment, AudioVisualSegment
  };

  /**
   * Returns the segment identifier.
   *
   * @return the identifier
   */
  String getIdentifier();

  /**
   * Sets the segment's media time constraints.
   *
   * @param mediaTime
   *          the media time
   */
  void setMediaTime(MediaTime mediaTime);

  /**
   * Returns the segment's time constraints.
   *
   * @return the media time
   */
  MediaTime getMediaTime();

  /**
   * Returns <code>true</code> if the segment contains any text annotations.
   *
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations();

  /**
   * Returns the number of text annotations. Note that text annotations are containers themselves, containing a number
   * of keywords and free text entries.
   *
   * @return the number of text annotations
   */
  int getTextAnnotationCount();

  /**
   * Returns <code>true</code> if the segment contains text annotations in the specified language.
   *
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(String language);

  /**
   * Returns <code>true</code> if the segment contains text annotations that satisfy the given relevance and confidence
   * values.
   *
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(float relevance, float confidence);

  /**
   * Returns <code>true</code> if the segment contains text annotations that satisfy the given relevance, confidence and
   * language constraints.
   *
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(float relevance, float confidence, String language);

  /**
   * Creates a new text annotation that will hold keywords and free text comments.
   *
   * @param relevance
   *          the relevance value
   * @param confidence
   *          the confidence
   * @param language
   *          the language identifier
   * @return the new text annotation
   */
  TextAnnotation createTextAnnotation(float relevance, float confidence, String language);

  /**
   * Returns this segment's text annotations.
   *
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotations();

  /**
   * Returns this segment's text annotations, sorted by relevance.
   *
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotationsByRelevance();

  /**
   * Returns this segment's text annotations, sorted by relevance.
   *
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotationsByConfidence();

}
