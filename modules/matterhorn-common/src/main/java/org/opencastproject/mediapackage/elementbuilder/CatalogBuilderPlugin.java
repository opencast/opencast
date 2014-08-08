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

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.UnsupportedElementException;
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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes metadata catalogs and provides the
 * functionality of reading it on behalf of the media package.
 */
public class CatalogBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /** The xpath facility */
  protected XPath xpath = XPathFactory.newInstance().newXPath();

  /**
   * the logging facility provided by log4j
   */
  private static final Logger logger = LoggerFactory.getLogger(CatalogBuilderPlugin.class);

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Catalog);
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    String name = elementNode.getNodeName();
    if (name.contains(":")) {
      name = name.substring(name.indexOf(":") + 1);
    }
    return name.equalsIgnoreCase(MediaPackageElement.Type.Catalog.toString());
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return MediaPackageElement.Type.Catalog.equals(type);
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromURI(URI)
   */
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    logger.trace("Creating video track from " + uri);
    Catalog track = CatalogImpl.fromURI(uri);
    return track;
  }

  @Override
  public String toString() {
    return "Indefinite Catalog Builder Plugin";
  }

  protected Catalog catalogFromManifest(String id, URI uri) {
    Catalog cat = CatalogImpl.fromURI(uri);
    cat.setIdentifier(id);
    return cat;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws UnsupportedElementException {
    String id = null;
    String flavor = null;
    URI url = null;
    long size = -1;
    Checksum checksum = null;
    MimeType mimeType = null;
    String reference = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolvePath(xpath.evaluate("url/text()", elementNode).trim());

      // flavor
      flavor = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // size
      String documentSize = xpath.evaluate("size/text()", elementNode).trim();
      if (!"".equals(documentSize))
        size = Long.parseLong(documentSize);

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(mimeTypeValue))
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      // create the catalog
      Catalog dc = CatalogImpl.fromURI(url);
      if (StringUtils.isNotEmpty(id))
        dc.setIdentifier(id);

      // Add url
      dc.setURI(url);

      // Add flavor
      if (flavor != null)
        dc.setFlavor(MediaPackageElementFlavor.parseFlavor(flavor));

      // Add reference
      if (StringUtils.isNotEmpty(reference))
        dc.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Set size
      if (size > 0)
        dc.setSize(size);

      // Set checksum
      if (checksum != null)
        dc.setChecksum(checksum);

      // Set Mimetype
      if (mimeType != null)
        dc.setMimeType(mimeType);

      // Tags
      NodeList tagNodes = (NodeList) xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET);
      for (int i = 0; i < tagNodes.getLength(); i++) {
        dc.addTag(tagNodes.item(i).getTextContent());
      }

      return dc;
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading catalog information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedElementException("Unsupported digest algorithm: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new UnsupportedElementException("Error while reading dublin core catalog " + url + ": " + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#newElement(org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public MediaPackageElement newElement(Type type, MediaPackageElementFlavor flavor) {
    Catalog cat = CatalogImpl.newInstance();
    cat.setFlavor(flavor);
    return cat;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#init()
   */
  @Override
  public void init() throws Exception {
  }

}
