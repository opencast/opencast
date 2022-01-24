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


package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.util.XmlSafeParser;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * This class provides factory methods for the creation of media packages from manifest files, directories or from
 * scratch. This class is not thread safe, so create a new builder in each method invocation.
 */
public class MediaPackageBuilderImpl implements MediaPackageBuilder {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageBuilderImpl.class);

  /** The media package serializer */
  protected MediaPackageSerializer serializer = null;

  /**
   * Creates a new media package builder.
   *
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl() {
  }

  /**
   * Creates a new media package builder that uses the given serializer to resolve urls while reading manifests and
   * adding new elements.
   *
   * @param serializer
   *          the media package serializer
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl(MediaPackageSerializer serializer) {
    if (serializer == null)
      throw new IllegalArgumentException("Serializer may not be null");
    this.serializer = serializer;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#createNew()
   */
  public MediaPackage createNew() {
    return new MediaPackageImpl();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#createNew(org.opencastproject.mediapackage.identifier.Id)
   */
  public MediaPackage createNew(Id identifier) {
    return new MediaPackageImpl(identifier);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#loadFromXml(java.io.InputStream)
   */
  public MediaPackage loadFromXml(InputStream is) throws MediaPackageException {
    try {
      Document xml = XmlSafeParser.parse(is);
      if (serializer != null) {
        //Convert InputStream to XML document to rewrite the URLs
        rewriteUrls(xml, serializer);
      }
      return loadFromXml(xml);
    } catch (Exception e) {
      throw new MediaPackageException("Error deserializing paths in media package", e);
    }
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#getSerializer()
   */
  public MediaPackageSerializer getSerializer() {
    return serializer;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#setSerializer(org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  public void setSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageBuilder#loadFromXml(java.lang.String)
   */
  @Override
  public MediaPackage loadFromXml(String xml) throws MediaPackageException {
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(xml, "UTF-8");
      return loadFromXml(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Override
  public MediaPackage loadFromXml(Node xml) throws MediaPackageException {
    if (serializer != null) {
      // FIXME This code runs if *any* serializer is present, regardless of the serializer implementation
      try {
        rewriteUrls(xml, serializer);
        return MediaPackageImpl.valueOf(xml);
      } catch (Exception e) {
        throw new MediaPackageException("Error deserializing paths in media package", e);
      }
    } else {
      return MediaPackageImpl.valueOf(xml);
    }
  }

  /**
   * Rewrite the url elements using the serializer. Attention: This method modifies the given DOM!
   */
  private static void rewriteUrls(Node xml, MediaPackageSerializer serializer) throws XPathExpressionException,
  URISyntaxException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodes = (NodeList) xPath.evaluate("//*[local-name() = 'url']", xml, XPathConstants.NODESET);
    for (int i = 0; i < nodes.getLength(); i++) {
      Node uri = nodes.item(i).getFirstChild();
      if (uri != null) {
        String uriStr = uri.getNodeValue();
        String trimmedUriStr = uriStr.trim();
        /*
         * Warn the user if trimming is necessary as this means that the URI was technically invalid.
         */
        if (!trimmedUriStr.equals(uriStr)) {
          logger.warn("Detected invalid URI. Trying to fix it by " + "removing spaces from beginning/end.");
        }
        uri.setNodeValue(serializer.decodeURI(new URI(trimmedUriStr)).toString());
      }
    }
  }

}
