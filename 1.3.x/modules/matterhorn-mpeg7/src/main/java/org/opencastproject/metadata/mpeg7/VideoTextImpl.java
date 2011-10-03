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

import java.awt.Rectangle;

/**
 * Default implementation of the video text element.
 */
public class VideoTextImpl implements VideoText {

  /** The video text type */
  protected Type type = Type.superimposed;

  /** The segment identifier */
  protected String id = null;

  /** The text */
  protected Textual text = null;

  /** The text's bounding box */
  protected Rectangle boundary = null;

  /** The text' time and duration */
  protected SpatioTemporalLocator locator = null;

  /** The font's name */
  protected String fontType = null;

  /** The font size */
  protected int fontSize = -1;

  /**
   * Creates a new <code>VideoText</code> element with the given id, text and text boundary.
   * 
   * @param id
   *          the text identifier
   */
  public VideoTextImpl(String id) {
    this(id, null, null, null);
  }

  /**
   * Creates a new <code>VideoText</code> element with the given id, text and text boundary.
   * 
   * @param id
   *          the text identifier
   * @param text
   *          the text
   * @param boundary
   *          the text's bounding box
   * @param time
   *          the media time
   */
  public VideoTextImpl(String id, Textual text, Rectangle boundary, MediaTime time) {
    this.id = id;
    this.text = text;
    this.boundary = boundary;
    if (time != null)
      this.locator = new SpatioTemporalLocatorImpl(time);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setIdentifier(java.lang.String)
   */
  @Override
  public void setIdentifier(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.Segment#getIdentifier()
   */
  @Override
  public String getIdentifier() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setType(org.opencastproject.metadata.mpeg7.VideoText.Type)
   */
  @Override
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#getType()
   */
  @Override
  public Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setText(org.opencastproject.metadata.mpeg7.Textual)
   */
  @Override
  public void setText(Textual text) {
    this.text = text;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#getText()
   */
  @Override
  public Textual getText() {
    return text;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setBoundary(java.awt.Rectangle)
   */
  @Override
  public void setBoundary(Rectangle rectangle) {
    this.boundary = rectangle;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#getBoundary()
   */
  @Override
  public Rectangle getBoundary() {
    return boundary;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setFontSize(int)
   */
  @Override
  public void setFontSize(int size) {
    this.fontSize = size;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#getFontSize()
   */
  @Override
  public int getFontSize() {
    return fontSize;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#setFontType(java.lang.String)
   */
  @Override
  public void setFontType(String fontType) {
    this.fontType = fontType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.VideoText#getFontType()
   */
  @Override
  public String getFontType() {
    return fontType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.MovingRegion#setSpatioTemporalLocator(org.opencastproject.metadata.mpeg7.SpatioTemporalLocator)
   */
  @Override
  public void setSpatioTemporalLocator(SpatioTemporalLocator locator) {
    this.locator = locator;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.mpeg7.MovingRegion#getSpatioTemporalLocator()
   */
  @Override
  public SpatioTemporalLocator getSpatioTemporalLocator() {
    return locator;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  @Override
  public Node toXml(Document document) {
    Element videoText = document.createElement("VideoText");
    videoText.setAttribute("id", id);
    videoText.setAttribute("textType", type.toString());
    if (fontSize > 0)
      videoText.setAttribute("fontSize", Integer.toString(fontSize));
    if (fontType != null)
      videoText.setAttribute("fontType", fontType);

    // Media locator
    if (locator != null)
      videoText.appendChild(locator.toXml(document));

    // Temporal Mask (Boundary)
    if (boundary != null) {
      Element temporalMask = document.createElement("SpatioTemporalMask");
      videoText.appendChild(temporalMask);
  
      Element subRegion = document.createElement("SubRegion");
      temporalMask.appendChild(subRegion);
      Element parameterTrajectory = document.createElement("ParameterTrajectory");
      subRegion.appendChild(parameterTrajectory);
  
      parameterTrajectory.appendChild(new MediaTimeImpl(new MediaTimePointImpl(), null).toXml(document));
      Element initialRegion = document.createElement("InitialRegion");
      parameterTrajectory.appendChild(initialRegion);
  
      StringBuffer coordinates = new StringBuffer();
      coordinates.append(boundary.getX()).append(" ");
      coordinates.append(boundary.getY()).append(" ");
      coordinates.append(boundary.getX() + boundary.getWidth()).append(" ");
      coordinates.append(boundary.getY() + boundary.getHeight());
  
      Element box = document.createElement("Box");
      box.setAttribute("dim", "2 2");
      box.appendChild(document.createTextNode(coordinates.toString()));
      initialRegion.appendChild(box);
    }

    // Text
    videoText.appendChild(text.toXml(document));
    return videoText;
  }

}
