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

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
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
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes attachments and provides utility
 * methods for creating media package element representations for them.
 */
public abstract class AbstractAttachmentBuilderPlugin extends AbstractElementBuilderPlugin {

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(AbstractAttachmentBuilderPlugin.class);

  /** The candidate type */
  protected MediaPackageElement.Type type = MediaPackageElement.Type.Attachment;

  /** The flavor to look for */
  protected MediaPackageElementFlavor flavor = null;

  /**
   * Creates a new attachment plugin builder that will accept attachments with any flavor.
   */
  public AbstractAttachmentBuilderPlugin() {
    this(null);
  }

  /**
   * Creates a new attachment plugin builder that will accept attachments with the given flavor.
   *
   * @param flavor
   *          the attachment flavor
   */
  public AbstractAttachmentBuilderPlugin(MediaPackageElementFlavor flavor) {
    this.flavor = flavor;
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return accept(type, flavor);
  }

  /**
   * This implementation of <code>accept</code> tests for the element type (attachment).
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.opencastproject.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    if (this.flavor != null && !this.flavor.equals(flavor))
      return false;
    return type == null || MediaPackageElement.Type.Attachment.toString().equalsIgnoreCase(type.toString());
  }

  /**
   * This implementation of <code>accept</code> tests for the correct node type (attachment).
   *
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  @Override
  public boolean accept(Node elementNode) {
    try {
      // Test for attachment
      String nodeName = elementNode.getNodeName();
      if (nodeName.contains(":")) {
        nodeName = nodeName.substring(nodeName.indexOf(":") + 1);
      }
      if (!MediaPackageElement.Type.Attachment.toString().equalsIgnoreCase(nodeName))
        return false;
      // Check flavor
      if (this.flavor != null) {
        String nodeFlavor = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);
        if (!flavor.eq(nodeFlavor))
          return false;
      }
      // Check mime type
      if (mimeTypes != null && mimeTypes.size() > 0) {
        String nodeMimeType = (String) xpath.evaluate("mimetype", elementNode, XPathConstants.STRING);
        MimeType mimeType = MimeTypes.parseMimeType(nodeMimeType);
        if (!mimeTypes.contains(mimeType))
          return false;
      }

      return true;
    } catch (XPathExpressionException e) {
      logger.warn("Error while reading attachment flavor from manifest: " + e.getMessage());
      return false;
    }
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    Attachment attachment = new AttachmentImpl();
    attachment.setFlavor(flavor);
    return attachment;
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws UnsupportedElementException {

    String id = null;
    String attachmentFlavor = null;
    String reference = null;
    URI uri = null;
    long size = -1;
    Checksum checksum = null;
    MimeType mimeType = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // flavor
      attachmentFlavor = (String) xpath.evaluate("@type", elementNode, XPathConstants.STRING);

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // url
      uri = serializer.resolvePath(xpath.evaluate("url/text()", elementNode).trim());

      // size
      String attachmentSize = xpath.evaluate("size/text()", elementNode).trim();
      if (!"".equals(attachmentSize))
        size = Long.parseLong(attachmentSize);

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (StringUtils.isNotEmpty(mimeTypeValue))
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      // create the attachment
      AttachmentImpl attachment = (AttachmentImpl) AttachmentImpl.fromURI(uri);

      if (StringUtils.isNotEmpty(id))
        attachment.setIdentifier(id);

      // Add url
      attachment.setURI(uri);

      // Add reference
      if (StringUtils.isNotEmpty(reference))
        attachment.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Add type/flavor information
      if (StringUtils.isNotEmpty(attachmentFlavor)) {
        try {
          MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(attachmentFlavor);
          attachment.setFlavor(flavor);
        } catch (IllegalArgumentException e) {
          logger.warn("Unable to read attachment flavor: " + e.getMessage());
        }
      }

      // Set the size
      if (size > 0)
        attachment.setSize(size);

      // Set checksum
      if (checksum != null)
        attachment.setChecksum(checksum);

      // Set mimetype
      if (mimeType != null)
        attachment.setMimeType(mimeType);

      // Set the description
      String description = xpath.evaluate("description/text()", elementNode);
      if (StringUtils.isNotEmpty(description))
        attachment.setElementDescription(description.trim());

      // Set tags
      NodeList tagNodes = (NodeList) xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET);
      for (int i = 0; i < tagNodes.getLength(); i++) {
        attachment.addTag(tagNodes.item(i).getTextContent());
      }

      return specializeAttachment(attachment);
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading attachment from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedElementException("Unsupported digest algorithm: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new UnsupportedElementException("Error while reading attachment file " + uri + ": " + e.getMessage());
    }
  }

  /**
   * Utility method that returns an attachment object from the given url.
   *
   * @param uri
   *          the element location
   * @return an attachment object
   * @throws UnsupportedElementException
   *           if the attachment cannto be read
   */
  @Override
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    logger.trace("Creating attachment from " + uri);
    return specializeAttachment(AttachmentImpl.fromURI(uri));
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#elementFromURI(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement elementFromURI(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException {
    return elementFromURI(uri);
  }

  /**
   * Overwrite this method in order to return a specialization of the attachment. This implementation just returns the
   * attachment that is was given.
   *
   * @param attachment
   *          the general attachment representation
   * @return a specialized attachment
   * @throws UnsupportedElementException
   *           if the attachment fails to be specialized
   */
  protected Attachment specializeAttachment(Attachment attachment) throws UnsupportedElementException {
    return attachment;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Attachment Builder Plugin";
  }

}
