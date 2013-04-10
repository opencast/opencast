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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PublicationBuilderPlugin extends AbstractElementBuilderPlugin {
	
	  /**
	   * the logging facility provided by log4j
	   */
	  private static final Logger logger = LoggerFactory.getLogger(PublicationBuilderPlugin.class);

	@Override
	public boolean accept(Type type, MediaPackageElementFlavor flavor) {
		return type.equals(MediaPackageElement.Type.Publication);
	}

	@Override
	public boolean accept(URI uri, Type type, MediaPackageElementFlavor flavor) {
		return MediaPackageElement.Type.Publication.equals(type);
	}

	@Override
	public boolean accept(Node elementNode) {
	    String name = elementNode.getNodeName();
	    if (name.contains(":")) {
	      name = name.substring(name.indexOf(":") + 1);
	    }
	    return name.equalsIgnoreCase(MediaPackageElement.Type.Publication.toString());
	}

	@Override
	public MediaPackageElement elementFromURI(URI uri)
			throws UnsupportedElementException {
		// TODO Auto-generated method stub
		logger.trace("Creating publication element from " + uri);
	    Publication publication = new PublicationImpl();
	    publication.setURI(uri);
		return publication;
	}

	@Override
	public MediaPackageElement elementFromManifest(Node elementNode,
			MediaPackageSerializer serializer)
			throws UnsupportedElementException {
		
	 	String id = null;
	    MimeType mimeType = null;
	    MediaPackageElementFlavor flavor = null;
	    String reference = null;
	    String channel = null;
	    URI url = null;
	    long size = -1;
	    Checksum checksum = null;

	    try {
	      // id
	      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);
	      if (StringUtils.isEmpty(id)) {
	    	  throw new UnsupportedElementException("Unvalid or missing id argument!");
	      }
	      
	      // url
	      url = serializer.resolvePath(xpath.evaluate("url/text()", elementNode).trim());

	      // channel
	      channel = (String) xpath.evaluate("@channel", elementNode).trim();
	      if (StringUtils.isEmpty(channel)) {
	    	  throw new UnsupportedElementException("Unvalid or missing channel argument!");
	      }
	      
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

	      // checksum
	      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
	      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
	      if (StringUtils.isNotEmpty(checksumValue) && checksumType != null)
	        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

	      // mimetype
	      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
	      if (StringUtils.isNotEmpty(mimeTypeValue)) {
	        mimeType = MimeTypes.parseMimeType(mimeTypeValue);
	      } else {
	    	  throw new UnsupportedElementException("Unvalid or missing mimetype argument!");
	      }

	      // Build the publication element
	      PublicationImpl publication = new PublicationImpl(id, channel, url, mimeType);

	      if (StringUtils.isNotBlank(id))
	        publication.setIdentifier(id);

	      // Add url
	      publication.setURI(url);

	      // Add reference
	      if (StringUtils.isNotEmpty(reference))
	        publication.referTo(MediaPackageReferenceImpl.fromString(reference));

	      // Set size
	      if (size > 0)
	        publication.setSize(size);

	      // Set checksum
	      if (checksum != null)
	        publication.setChecksum(checksum);

	      // Set mimetpye
	      if (mimeType != null)
	        publication.setMimeType(mimeType);

	      if (flavor != null)
	        publication.setFlavor(flavor);

	      // description
	      String description = (String) xpath.evaluate("description/text()", elementNode, XPathConstants.STRING);
	      if (StringUtils.isNotBlank(description))
	        publication.setElementDescription(description.trim());

	      // tags
	      NodeList tagNodes = (NodeList) xpath.evaluate("tags/tag", elementNode, XPathConstants.NODESET);
	      for (int i = 0; i < tagNodes.getLength(); i++) {
	        publication.addTag(tagNodes.item(i).getTextContent());
	      }

	      return publication;
	    } catch (XPathExpressionException e) {
	      throw new UnsupportedElementException("Error while reading track information from manifest: " + e.getMessage());
	    } catch (NoSuchAlgorithmException e) {
	      throw new UnsupportedElementException("Unsupported digest algorithm: " + e.getMessage());
	    } catch (URISyntaxException e) {
	      throw new UnsupportedElementException("Error while reading presenter track " + url + ": " + e.getMessage());
	    }
	}

	@Override
	public MediaPackageElement newElement(Type type,
			MediaPackageElementFlavor flavor) {
		Publication element = new PublicationImpl();
		element.setFlavor(flavor);
		return element;
	}

}
