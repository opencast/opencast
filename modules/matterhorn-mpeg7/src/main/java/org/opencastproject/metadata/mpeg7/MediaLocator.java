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

import java.net.URI;

/**
 * The media locator tells where the audio/video track is located.
 * 
 * <pre>
 * &lt;complexType name=&quot;MediaLocatorType&quot;&gt;
 *   &lt;sequence&gt;
 *       &lt;choice minOccurs=&quot;0&quot;&gt;
 *           &lt;element name=&quot;MediaUri&quot; type=&quot;anyURI&quot;/&gt;
 *           &lt;element name=&quot;InlineMedia&quot; type=&quot;mpeg7:InlineMediaType&quot;/&gt;
 *       &lt;/choice&gt;
 *       &lt;element name=&quot;StreamID&quot; type=&quot;nonNegativeInteger&quot; minOccurs=&quot;0&quot;/&gt;
 *   &lt;/sequence&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface MediaLocator extends XmlElement {

  /**
   * Returns the media uri of the track.
   * 
   * @return the media uri
   */
  URI getMediaURI();

}
