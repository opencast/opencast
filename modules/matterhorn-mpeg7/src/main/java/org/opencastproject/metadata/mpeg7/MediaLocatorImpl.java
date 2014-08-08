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

import java.net.URI;

/**
 * TODO: Comment me!
 */
public class MediaLocatorImpl implements MediaLocator {

  /** The locator's media uri */
  private URI mediaUri = null;

  /**
   * Creates a new and empty (read: invalid) media locator.
   */
  public MediaLocatorImpl() {
  }

  /**
   * Creates a new media locator with the specified uri.
   *
   * @param mediaURI
   *          the media uri
   */
  public MediaLocatorImpl(URI mediaURI) {
    if (mediaURI == null)
      throw new IllegalArgumentException("Argument mediaURI must not be null");
    this.mediaUri = mediaURI;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaLocator#getMediaURI()
   */
  public URI getMediaURI() {
    return mediaUri;
  }

  /**
   * Sets the media uri.
   *
   * @param mediaURI
   *          the uri
   */
  public void setMediaURI(URI mediaURI) {
    if (mediaURI == null)
      throw new IllegalArgumentException("Argument mediaURI must not be null");
    this.mediaUri = mediaURI;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("MediaLocator");
    Element uriNode = document.createElement("MediaUri");
    uriNode.setTextContent(mediaUri.toString());
    node.appendChild(uriNode);
    return node;
  }

}
