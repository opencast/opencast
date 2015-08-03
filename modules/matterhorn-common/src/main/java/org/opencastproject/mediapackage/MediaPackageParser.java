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

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.DateTimeSupport;

import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Convenience implementation that supports serializing and deserializing media packages.
 */
public final class MediaPackageParser {

  /**
   * Private constructor to prohibit instances of this static utility class.
   */
  private MediaPackageParser() {
    // Nothing to do
  }

  /**
   * Serializes the media package to a string.
   *
   * @param mediaPackage
   *          the media package
   * @return the serialized media package
   */
  public static String getAsXml(MediaPackage mediaPackage) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must not be null");
    try {
      Marshaller marshaller = MediaPackageImpl.context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
      StringWriter writer = new StringWriter();
      marshaller.marshal(mediaPackage, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the media package to a JSON string.
   *
   * @param mediaPackage
   *          the media package
   * @return the serialized media package
   */
  public static String getAsJSON(MediaPackage mediaPackage) {
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Mediapackage must not be null");
    }
    try {
      Marshaller marshaller = MediaPackageImpl.context.createMarshaller();

      Configuration config = new Configuration();
      config.setSupressAtAttributes(true);
      MappedNamespaceConvention con = new MappedNamespaceConvention(config);
      StringWriter writer = new StringWriter();
      XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
        }
      };

      marshaller.marshal(mediaPackage, xmlStreamWriter);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /** Serializes a media package to a {@link Document} without any further processing. */
  public static Document getAsXmlDocument(MediaPackage mp) {
    try {
      final Marshaller marshaller = MediaPackageImpl.context.createMarshaller();
      final Document doc = newDocument();
      marshaller.marshal(mp, doc);
      return doc;
    } catch (JAXBException e) {
      return chuck(e);
    }
  }

  /** Create a new DOM document. */
  private static Document newDocument() {
    final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    try {
      return docBuilderFactory.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      return chuck(e);
    }
  }

  /**
   * Serializes the media package to a {@link org.w3c.dom.Document}.
   * <p/>
   * todo Implementation is currently defective since it misses various properties. See
   * http://opencast.jira.com/browse/MH-9489 Use {@link #getAsXmlDocument(MediaPackage)} instead if you do not need a
   * serializer.
   *
   * @param mediaPackage
   *          the mediapackage
   * @param serializer
   *          the serializer
   * @return the serialized media package
   * @throws MediaPackageException
   *           if serializing fails
   */
  public static Document getAsXml(MediaPackage mediaPackage, MediaPackageSerializer serializer)
          throws MediaPackageException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);

    DocumentBuilder docBuilder = null;
    try {
      docBuilder = docBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e1) {
      throw new MediaPackageException(e1);
    }

    Document doc = docBuilder.newDocument();

    // Root element "mediapackage"
    Element mpXml = doc.createElement("mediapackage");
    doc.appendChild(mpXml);

    // Handle
    if (mediaPackage.getIdentifier() != null)
      mpXml.setAttribute("id", mediaPackage.getIdentifier().toString());

    // Start time
    if (mediaPackage.getDate() != null && mediaPackage.getDate().getTime() > 0)
      mpXml.setAttribute("start", DateTimeSupport.toUTC(mediaPackage.getDate().getTime()));

    // Duration
    if (mediaPackage.getDuration() != null)
      mpXml.setAttribute("duration", Long.toString(mediaPackage.getDuration()));

    // Separate the media package members
    List<Track> tracks = new ArrayList<Track>();
    List<Attachment> attachments = new ArrayList<Attachment>();
    List<Catalog> metadata = new ArrayList<Catalog>();
    List<MediaPackageElement> others = new ArrayList<MediaPackageElement>();

    // Sort media package elements
    for (MediaPackageElement e : mediaPackage.elements()) {
      if (e instanceof Track)
        tracks.add((Track) e);
      else if (e instanceof Attachment)
        attachments.add((Attachment) e);
      else if (e instanceof Catalog)
        metadata.add((Catalog) e);
      else
        others.add(e);
    }

    // Tracks
    if (tracks.size() > 0) {
      Element tracksNode = doc.createElement("media");
      Collections.sort(tracks);
      for (Track t : tracks) {
        tracksNode.appendChild(t.toManifest(doc, serializer));
      }
      mpXml.appendChild(tracksNode);
    }

    // Metadata
    if (metadata.size() > 0) {
      Element metadataNode = doc.createElement("metadata");
      Collections.sort(metadata);
      for (Catalog m : metadata) {
        metadataNode.appendChild(m.toManifest(doc, serializer));
      }
      mpXml.appendChild(metadataNode);
    }

    // Attachments
    if (attachments.size() > 0) {
      Element attachmentsNode = doc.createElement("attachments");
      Collections.sort(attachments);
      for (Attachment a : attachments) {
        attachmentsNode.appendChild(a.toManifest(doc, serializer));
      }
      mpXml.appendChild(attachmentsNode);
    }

    // Unclassified
    if (others.size() > 0) {
      Element othersNode = doc.createElement("unclassified");
      Collections.sort(others);
      for (MediaPackageElement e : others) {
        othersNode.appendChild(e.toManifest(doc, serializer));
      }
      mpXml.appendChild(othersNode);
    }

    return mpXml.getOwnerDocument();
  }

  /**
   * Parses the media package and returns its object representation.
   *
   * @param xml
   *          the serialized media package
   * @return the media package instance
   * @throws MediaPackageException
   *           if de-serializing the media package fails
   */
  public static MediaPackage getFromXml(String xml) throws MediaPackageException {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    return builder.loadFromXml(xml);
  }

  /**
   * Writes an xml representation of this MediaPackage to a stream.
   *
   * @param mediaPackage
   *          the mediaPackage
   * @param out
   *          The output stream
   * @param format
   *          Whether to format the output for readability, or not (false gives better performance)
   * @throws MediaPackageException
   *           if serializing or reading from a serialized media package fails
   */
  public static void getAsXml(MediaPackage mediaPackage, OutputStream out, boolean format) throws MediaPackageException {
    try {
      Marshaller marshaller = MediaPackageImpl.context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
      marshaller.marshal(mediaPackage, out);
    } catch (JAXBException e) {
      throw new MediaPackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
