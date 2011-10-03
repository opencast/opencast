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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Default implementation of the <code>SegmentType</code>.
 */
public class SegmentImpl implements Segment, AudioSegment, VideoSegment, AudioVisualSegment {

  /** The content type */
  protected Segment.Type type = null;

  /** The content element identifier */
  protected String id = null;

  /** The content time contraints */
  protected MediaTime mediaTime = null;

  /** The text annotations */
  protected List<TextAnnotation> annotations = null;

  /** An optional spatio-temporal decomposition */
  protected SpatioTemporalDecomposition spatioTemporalDecomposition = null;

  /**
   * Creates a new content segment.
   * 
   * @param type
   *          the segment type
   * @param id
   *          the segment identifier
   */
  public SegmentImpl(Segment.Type type, String id) {
    this.type = type;
    this.id = id;
    annotations = new ArrayList<TextAnnotation>();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#getIdentifier()
   */
  public String getIdentifier() {
    return id;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#setMediaTime(org.opencastproject.mediapackage.mpeg7.MediaTime)
   */
  public void setMediaTime(MediaTime mediaTime) {
    this.mediaTime = mediaTime;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#getMediaTime()
   */
  public MediaTime getMediaTime() {
    return mediaTime;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#hasTextAnnotations()
   */
  public boolean hasTextAnnotations() {
    return annotations.size() > 0;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#hasTextAnnotations(java.lang.String)
   */
  public boolean hasTextAnnotations(String language) {
    return hasTextAnnotations(0.0f, 0.0f, language);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#hasTextAnnotations(float, float)
   */
  public boolean hasTextAnnotations(float relevance, float confidence) {
    return hasTextAnnotations(relevance, confidence, null);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#hasTextAnnotations(float, float, java.lang.String)
   */
  public boolean hasTextAnnotations(float relevance, float confidence, String language) {
    for (TextAnnotation annotation : annotations) {
      if (annotation.getRelevance() >= relevance && annotation.getConfidence() >= confidence) {
        if (language != null) {
          if (language.equals(annotation.getLanguage()))
            return true;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#getTextAnnotationCount()
   */
  public int getTextAnnotationCount() {
    return annotations.size();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#textAnnotationsByConfidence()
   */
  public Iterator<TextAnnotation> textAnnotationsByConfidence() {
    SortedSet<TextAnnotation> set = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
      public int compare(TextAnnotation a1, TextAnnotation a2) {
        if (a1.getConfidence() > a2.getConfidence())
          return 1;
        else if (a1.getConfidence() > a2.getConfidence())
          return -1;
        return 0;
      }
    });
    set.addAll(annotations);
    return set.iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#textAnnotationsByRelevance()
   */
  public Iterator<TextAnnotation> textAnnotationsByRelevance() {
    SortedSet<TextAnnotation> set = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
      public int compare(TextAnnotation a1, TextAnnotation a2) {
        if (a1.getRelevance() > a2.getRelevance())
          return 1;
        else if (a1.getRelevance() > a2.getRelevance())
          return -1;
        return 0;
      }
    });
    set.addAll(annotations);
    return set.iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#createTextAnnotation(float, float, String)
   */
  public TextAnnotation createTextAnnotation(float relevance, float confidence, String language) {
    TextAnnotationImpl annotation = new TextAnnotationImpl(relevance, confidence, language);
    annotations.add(annotation);
    return annotation;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Segment#textAnnotations()
   */
  public Iterator<TextAnnotation> textAnnotations() {
    return annotations.iterator();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoSegment#createSpatioTemporalDecomposition(boolean, boolean)
   */
  @Override
  public SpatioTemporalDecomposition createSpatioTemporalDecomposition(boolean gap, boolean overlap)
          throws IllegalStateException {
    if (spatioTemporalDecomposition != null)
      throw new IllegalStateException("A spatio temporal decomposition has already been created");
    spatioTemporalDecomposition = new SpatioTemporalDecompositionImpl(true, false);
    return spatioTemporalDecomposition;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.AudioVisualSegment#getSpatioTemporalDecomposition()
   */
  @Override
  public SpatioTemporalDecomposition getSpatioTemporalDecomposition() {
    return spatioTemporalDecomposition;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.AudioVisualSegment#hasSpatioTemporalDecomposition()
   */
  @Override
  public boolean hasSpatioTemporalDecomposition() {
    return spatioTemporalDecomposition != null;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Segment) {
      return id.equals(((Segment) obj).getIdentifier());
    }
    return super.equals(obj);
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement(type.toString());
    node.setAttribute("id", id);
    node.appendChild(mediaTime.toXml(document));
    if (spatioTemporalDecomposition != null)
      node.appendChild(spatioTemporalDecomposition.toXml(document));
    for (TextAnnotation annotation : annotations) {
      node.appendChild(annotation.toXml(document));
    }
    return node;
  }

}
