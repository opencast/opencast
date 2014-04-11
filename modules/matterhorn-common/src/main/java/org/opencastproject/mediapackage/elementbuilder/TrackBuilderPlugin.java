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

package org.opencastproject.mediapackage.elementbuilder;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes video tracks and provides the
 * functionality of reading it on behalf of the media package.
 */
public class TrackBuilderPlugin extends AbstractElementBuilderPlugin {

  /**
   * the logging facility provided by log4j
   */
  private static final Logger logger = LoggerFactory.getLogger(TrackBuilderPlugin.class);

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Track);
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    String name = elementNode.getNodeName();
    if (name.contains(":")) {
      name = name.substring(name.indexOf(":") + 1);
    }
    return name.equalsIgnoreCase(MediaPackageElement.Type.Track.toString());
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return MediaPackageElement.Type.Track.equals(type);
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromURI(URI)
   */
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    logger.trace("Creating track from " + uri);
    Track track = TrackImpl.fromURI(uri);
    return track;
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#newElement(org.opencastproject.mediapackage.MediaPackageElement.Type
   *      ,org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    Track track = new TrackImpl();
    track.setFlavor(flavor);
    return track;
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws UnsupportedElementException {

    String id = null;
    MimeType mimeType = null;
    MediaPackageElementFlavor flavor = null;
    TrackImpl.StreamingProtocol transport = null;
    String reference = null;
    URI url = null;
    long size = -1;
    Checksum checksum = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolvePath(xpath.evaluate("url/text()", elementNode).trim());

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // size
      String trackSize = xpath.evaluate("size/text()", elementNode).trim();
      if (!"".equals(trackSize))
        size = Long.parseLong(trackSize);

      // flavor
      String flavorValue = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(flavorValue))
        flavor = MediaPackageElementFlavor.parseFlavor(flavorValue);

      // transport
      String transportValue = (String) xpath.evaluate("@transport", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(transportValue))
        transport = TrackImpl.StreamingProtocol.valueOf(transportValue);

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(mimeTypeValue))
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      //
      // Build the track

      TrackImpl track = TrackImpl.fromURI(url);

      if (StringUtils.isNotBlank(id))
        track.setIdentifier(id);

      // Add url
      track.setURI(url);

      // Add reference
      if (StringUtils.isNotEmpty(reference))
        track.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Set size
      if (size > 0)
        track.setSize(size);

      // Set checksum
      if (checksum != null)
        track.setChecksum(checksum);

      // Set mimetpye
      if (mimeType != null)
        track.setMimeType(mimeType);

      if (flavor != null)
        track.setFlavor(flavor);

      //set transport
      if (transport != null)
        track.setTransport(transport);

      // description
      String description = (String) xpath.evaluate("description/text()", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotBlank(description))
        track.setElementDescription(description.trim());

      // tags
      NodeList tagNodes = (NodeList) xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET);
      for (int i = 0; i < tagNodes.getLength(); i++) {
        track.addTag(tagNodes.item(i).getTextContent());
      }

      // duration
      try {
        String strDuration = (String) xpath.evaluate("duration/text()", elementNode, XPathConstants.STRING);
        if (StringUtils.isNotEmpty(strDuration)) {
          long duration = Long.parseLong(strDuration.trim());
          track.setDuration(duration);
        }
      } catch (NumberFormatException e) {
        throw new UnsupportedElementException("Duration of track " + url + " is malformatted");
      }

      // audio settings
      Node audioSettingsNode = (Node) xpath.evaluate("audio", elementNode, XPathConstants.NODE);
      if (audioSettingsNode != null && audioSettingsNode.hasChildNodes()) {
        try {
          AudioStreamImpl as = AudioStreamImpl.fromManifest(createStreamID(track), audioSettingsNode, xpath);
          track.addStream(as);
        } catch (IllegalStateException e) {
          throw new UnsupportedElementException("Illegal state encountered while reading audio settings from " + url
                  + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new UnsupportedElementException("Error while parsing audio settings from " + url + ": "
                  + e.getMessage());
        }
      }

      // video settings
      Node videoSettingsNode = (Node) xpath.evaluate("video", elementNode, XPathConstants.NODE);
      if (videoSettingsNode != null && videoSettingsNode.hasChildNodes()) {
        try {
          VideoStreamImpl vs = VideoStreamImpl.fromManifest(createStreamID(track), videoSettingsNode, xpath);
          track.addStream(vs);
        } catch (IllegalStateException e) {
          throw new UnsupportedElementException("Illegal state encountered while reading video settings from " + url
                  + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new UnsupportedElementException("Error while parsing video settings from " + url + ": "
                  + e.getMessage());
        }
      }

      return track;
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading track information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedElementException("Unsupported digest algorithm: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new UnsupportedElementException("Error while reading presenter track " + url + ": " + e.getMessage());
    }
  }

  private String createStreamID(Track track) {
    return "stream-" + (track.getStreams().length + 1);
  }

  @Override
  public String toString() {
    return "Track Builder Plugin";
  }

}
