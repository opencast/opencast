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

package org.opencastproject.elasticsearch.index.objects.event;

import org.opencastproject.elasticsearch.index.objects.IndexObject;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.XmlSafeParser;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Object wrapper for a recording comment.
 */
@XmlType(
    name = "comment",
    namespace = IndexObject.INDEX_XML_NAMESPACE,
    propOrder = {
        "id", "reason", "text", "resolvedStatus"
    }
)
@XmlRootElement(name = "comment", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Comment implements IndexObject {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Comment.class);

  /** The document id */
  public static final String DOCUMENT_TYPE = "comment";

  /** The name of the surrounding XML tag to wrap a result of multiple comments */
  public static final String XML_SURROUNDING_TAG = "comments";

  /** The identifier */
  @XmlElement(name = "id")
  private String id = null;

  /** The organization identifier */
  @XmlElement(name = "reason")
  private String reason = null;

  /** The title */
  @XmlElement(name = "text")
  private String text = null;

  /** The title */
  @XmlElement(name = "resolvedStatus")
  private Boolean resolvedStatus = null;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Comment() {

  }

  /**
   * The recording identifier.
   *
   * @param id
   *          the object id
   * @param reason
   *          the reason
   */
  public Comment(String id, String reason, String text, boolean resolvedStatus) {
    this.id = id;
    this.reason = reason;
    this.text = text;
    this.resolvedStatus = resolvedStatus;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Boolean isResolvedStatus() {
    return resolvedStatus;
  }

  public void setResolvedStatus(Boolean resolvedStatus) {
    this.resolvedStatus = resolvedStatus;
  }

  /**
   * Reads the recording comment from the input stream.
   *
   * @param xml
   *          the input stream
   * @param unmarshaller the unmarshaller to use
   * @return the deserialized recording comment
   * @throws IOException
   */
  public static Comment valueOf(InputStream xml, Unmarshaller unmarshaller) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      return unmarshaller.unmarshal(XmlSafeParser.parse(xml), Comment.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } catch (SAXException e) {
      throw new IOException(e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Reads the recording comment from the input stream.
   *
   * @param json
   *          the input stream
   * @return the deserialized recording comment
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Comment valueOfJson(InputStream json)
          throws IOException, JSONException, XMLStreamException, JAXBException {
    // TODO Get this to work, it is currently returning null properties for all properties.
    if (context == null) {
      createJAXBContext();
    }

    BufferedReader streamReader = new BufferedReader(new InputStreamReader(json, "UTF-8"));
    StringBuilder jsonStringBuilder = new StringBuilder();
    String inputStr;
    while ((inputStr = streamReader.readLine()) != null) {
      jsonStringBuilder.append(inputStr);
    }

    JSONObject obj = new JSONObject(jsonStringBuilder.toString());
    Configuration config = new Configuration();
    config.setSupressAtAttributes(true);
    Map<String, String> xmlToJsonNamespaces = new HashMap<String, String>(1);
    xmlToJsonNamespaces.put(IndexObject.INDEX_XML_NAMESPACE, "");
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
    MappedNamespaceConvention con = new MappedNamespaceConvention(config);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    // CHECKSTYLE:OFF
    // the xml is parsed from json and should be safe
    XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);
    Comment comment = (Comment) unmarshaller.unmarshal(xmlStreamReader);
    // CHECKSTYLE:ON
    return comment;
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Comment.class);
  }

  /**
   * Serializes the recording comment.
   *
   * @return the serialized recording comment
   */
  public String toJSON() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Marshaller marshaller = Comment.context.createMarshaller();

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

      marshaller.marshal(this, xmlStreamWriter);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the recording comment to an XML format.
   *
   * @return A String with this comment's content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Comment.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Create an unmarshaller for comments
   * @return an unmarshaller for comments
   * @throws IOException
   */
  public static Unmarshaller createUnmarshaller() throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      return context.createUnmarshaller();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
