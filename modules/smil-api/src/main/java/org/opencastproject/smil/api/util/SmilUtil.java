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
package org.opencastproject.smil.api.util;

import static org.opencastproject.util.IoSupport.withResource;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.CatalogSelector;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.workspace.api.Workspace;

import com.android.mms.dom.smil.parser.SmilXmlParser;
import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.FnX;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.smil.SMILDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

/**
 * General purpose utility functions for dealing with SMIL.
 */
public final class SmilUtil {

  private static final Logger logger = LoggerFactory.getLogger(SmilUtil.class);

  public static final String SMIL_NODE_NAME = "smil";
  public static final String SMIL_NS_URI = "http://www.w3.org/ns/SMIL";

  public enum TrackType {
    PRESENTER, PRESENTATION
  }

  private SmilUtil() {
  }

  /** Parse a SMIL document from an input stream. */
  public static final Fn<InputStream, SMILDocument> parseSmilFn = new FnX<InputStream, SMILDocument>() {
    @Override
    public SMILDocument applyX(InputStream in) throws SAXException, IOException {
      return new SmilXmlParser().parse(in);
    }
  };

  /**
   * Read a SMIL document from a string.
   *
   * @throws java.io.IOException
   *           in case of any IO error
   * @throws org.xml.sax.SAXException
   *           in case of a SAX related error
   */
  public static SMILDocument readSmil(String smil) throws IOException, SAXException {
    return withResource(IOUtils.toInputStream(smil, "UTF-8"), parseSmilFn);
  }

  /**
   * Load the SMIL document identified by <code>mpe</code>. Throws an exception if it does not exist or cannot be loaded
   * by any reason.
   *
   * @return the document
   */
  public static Document loadSmilDocument(InputStream in, MediaPackageElement mpe) {
    try {
      Either<Exception, org.w3c.dom.Document> eitherDocument = XmlUtil.parseNs(new InputSource(in));
      if (eitherDocument.isRight())
        return eitherDocument.right().value();

      throw eitherDocument.left().value();
    } catch (Exception e) {
      logger.warn("Unable to load smil document from catalog '{}': {}", mpe, ExceptionUtils.getStackTrace(e));
      return Misc.chuck(e);
    }
  }

  /**
   * Creates a skeleton SMIL document
   *
   * @return the SMIL document
   */
  public static Document createSmil() {
    Document smilDocument = XmlUtil.newDocument();
    smilDocument.setXmlVersion("1.1");
    Element smil = smilDocument.createElementNS(SMIL_NS_URI, SMIL_NODE_NAME);
    smil.setAttribute("version", "3.0");
    smilDocument.appendChild(smil);
    Node head = smilDocument.createElement("head");
    smil.appendChild(head);
    Node body = smilDocument.createElement("body");
    smil.appendChild(body);
    Element parallel = smilDocument.createElement("par");
    parallel.setAttribute("dur", "0ms");
    body.appendChild(parallel);
    return smilDocument;
  }

  /**
   * Adds a track to the SMIL document.
   *
   * @param smilDocument
   *          the SMIL document
   * @param trackType
   *          the track type
   * @param hasVideo
   *          whether the track has a video stream
   * @param startTime
   *          the start time
   * @param duration
   *          the duration
   * @param uri
   *          the track URI
   * @return the augmented SMIL document
   */
  public static Document addTrack(Document smilDocument, TrackType trackType, boolean hasVideo, long startTime,
          long duration, URI uri) {
    return addTrack(smilDocument, trackType, hasVideo, startTime,duration, uri, null);
  }

  /**
   * Adds a track to the SMIL document.
   *
   * @param smilDocument
   *          the SMIL document
   * @param trackType
   *          the track type
   * @param hasVideo
   *          whether the track has a video stream
   * @param startTime
   *          the start time
   * @param duration
   *          the duration
   * @param uri
   *          the track URI
   * @param trackId
   *          the Id of the track
   * @return the augmented SMIL document
   */
  public static Document addTrack(Document smilDocument, TrackType trackType, boolean hasVideo, long startTime,
          long duration, URI uri, String trackId) {
    Element parallel = (Element) smilDocument.getElementsByTagName("par").item(0);
    if (parallel.getChildNodes().getLength() == 0) {
      Node presenterSeq = smilDocument.createElement("seq");
      parallel.appendChild(presenterSeq);
      Node presentationSeq = smilDocument.createElement("seq");
      parallel.appendChild(presentationSeq);
    }

    String trackDurationString = parallel.getAttribute("dur");
    Long oldTrackDuration = Long.parseLong(trackDurationString.substring(0, trackDurationString.indexOf("ms")));
    Long newTrackDuration = startTime + duration;
    if (newTrackDuration > oldTrackDuration) {
      parallel.setAttribute("dur", newTrackDuration + "ms");
    }

    Node sequence;
    switch (trackType) {
      case PRESENTER:
        sequence = parallel.getChildNodes().item(0);
        break;
      case PRESENTATION:
        sequence = parallel.getChildNodes().item(1);
        break;
      default:
        throw new IllegalStateException("Unknown track type " + trackType.toString());
    }

    Element element = smilDocument.createElement(hasVideo ? "video" : "audio");
    element.setAttribute("begin", Long.toString(startTime) + "ms");
    element.setAttribute("dur", Long.toString(duration) + "ms");
    element.setAttribute("src", URIUtil.getPath(uri.toString()));
    if (trackId != null) {
      element.setAttribute("xml:id", trackId);
    }
    sequence.appendChild(element);
    return smilDocument;
  }


  public static SMILDocument getSmilDocumentFromMediaPackage(MediaPackage mp, MediaPackageElementFlavor smilFlavor,
      Workspace workspace)
      throws IOException, SAXException, NotFoundException {
    final AbstractMediaPackageElementSelector<Catalog> smilSelector = new CatalogSelector();
    smilSelector.addFlavor(smilFlavor);
    final Collection<Catalog> smilCatalog = smilSelector.select(mp, false);
    if (smilCatalog.size() == 1) {
      return getSmilDocument(smilCatalog.iterator().next(), workspace);
    } else {
      logger.error("More or less than one smil catalog found: {}", smilCatalog);
      throw new IllegalStateException("More or less than one smil catalog found!");
    }
  }

  /** Get the SMIL document from a catalog. */
  private static SMILDocument getSmilDocument(final Catalog smilCatalog, Workspace workspace) throws NotFoundException,
      IOException, SAXException {
    FileInputStream in = null;
    try {
      File smilXmlFile = workspace.get(smilCatalog.getURI());
      SmilXmlParser smilParser = new SmilXmlParser();
      in = new FileInputStream(smilXmlFile);
      return smilParser.parse(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
