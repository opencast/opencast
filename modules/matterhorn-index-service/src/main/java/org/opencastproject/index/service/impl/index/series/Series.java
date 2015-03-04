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
package org.opencastproject.index.service.impl.index.series;

import org.opencastproject.index.service.impl.index.IndexObject;
import org.opencastproject.util.DateTimeSupport.UtcTimestampAdapter;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.IoSupport;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

/**
 * Object wrapper for a series.
 */
@XmlType(name = "series", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "title",
        "description", "subject", "organization", "language", "creator", "license", "accessPolicy", "managedAcl",
        "createdDateTime", "organizers", "contributors", "publishers", "optOut", "rightsHolder", "theme" })
@XmlRootElement(name = "series", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Series implements IndexObject {

  /** The document type */
  public static final String DOCUMENT_TYPE = "series";

  /** The name of the surrounding XML tag to wrap a result of multiple series */
  public static final String XML_SURROUNDING_TAG = "series-list";

  /** The identifier */
  @XmlElement(name = "identifier")
  private String identifier = null;

  /** The title */
  @XmlElement(name = "title")
  private String title = null;

  /** The description */
  @XmlElement(name = "description")
  private String description = null;

  /** The subject */
  @XmlElement(name = "subject")
  private String subject = null;

  /** The organization for the series */
  @XmlElement(name = "organization")
  private String organization = null;

  /** The language for the series */
  @XmlElement(name = "language")
  private String language = null;

  /** The creator of the series */
  @XmlElement(name = "creator")
  private String creator = null;

  /** The license of the series */
  @XmlElement(name = "license")
  private String license = null;

  /** The access policy of the series */
  @XmlElement(name = "access_policy")
  private String accessPolicy = null;

  /** The name of the managed ACL used by the series (if set) */
  @XmlElement(name = "managed_acl")
  private String managedAcl = null;

  /** The date and time the series was created in UTC format e.g. 2011-07-16T20:39:05Z */
  @XmlElement(name = "createdDateTime")
  @XmlJavaTypeAdapter(UtcTimestampAdapter.class)
  private Date createdDateTime;

  @XmlElementWrapper(name = "organizers")
  @XmlElement(name = "organizer")
  private List<String> organizers = null;

  @XmlElementWrapper(name = "contributors")
  @XmlElement(name = "contributor")
  private List<String> contributors = null;

  @XmlElementWrapper(name = "publishers")
  @XmlElement(name = "publisher")
  private List<String> publishers = null;

  @XmlElement(name = "rights_holder")
  private String rightsHolder = null;

  /** The series opted out status from the participation management, whether the series is opted out or not opted out */
  @XmlElement(name = "opt_out")
  private Boolean optOut = false;

  @XmlElement(name = "theme")
  private Long theme = null;

  private boolean seriesTitleUpdated = false;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Series() {

  }

  /**
   * The series identifier.
   *
   * @param identifier
   *          the object identifier
   * @param organization
   *          the organization
   */
  public Series(String identifier, String organization) {
    this.identifier = identifier;
    this.organization = organization;
  }

  /**
   * Returns the series identifier.
   *
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Sets the series title.
   *
   * @param title
   *          the title
   */
  public void setTitle(String title) {
    if (EqualsUtil.eq(this.title, title))
      return;

    this.title = title;
    seriesTitleUpdated = true;
  }

  /**
   * Returns the series title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the series description.
   *
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the series description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the series subject.
   *
   * @param subject
   *          the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Returns the series subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Returns the series organization.
   *
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Sets the series language.
   *
   * @param language
   *          the language
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * Returns the series language.
   *
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Sets the series creator.
   *
   * @param creator
   *          the creator
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * Returns the series creator.
   *
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Sets the series license.
   *
   * @param license
   *          the license
   */
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * Returns the series license.
   *
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Sets the series access policy.
   *
   * @param accessPolicy
   *          the access policy
   */
  public void setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  /**
   * Returns the series access policy.
   *
   * @return the access policy
   */
  public String getAccessPolicy() {
    return accessPolicy;
  }

  /**
   * Sets the name of the managed ACL used by the series.
   *
   * @param managedAcl
   *          the managed ACL name
   */
  public void setManagedAcl(String managedAcl) {
    this.managedAcl = managedAcl;
  }

  /**
   * Returns the name of the managed ACL, if the series does not have a custom ACL.
   *
   * @return the managed ACL name
   */
  public String getManagedAcl() {
    return managedAcl;
  }

  /**
   * Sets the series created date and time.
   *
   * @param date
   *          the date and time the series was created.
   */
  public void setCreatedDateTime(Date createdDateTime) {
    this.createdDateTime = createdDateTime;
  }

  /**
   * Returns the series date and time created.
   *
   * @return the created date and time
   */
  public Date getCreatedDateTime() {
    return createdDateTime;
  }

  /**
   * Add an organizer
   *
   * @param organizer
   *          The organizer's name.
   */
  public void addOrganizer(String organizer) {
    if (organizers == null) {
      organizers = new ArrayList<String>();
    }
    organizers.add(organizer);
  }

  /**
   * Sets the list of presenters.
   *
   * @param presenters
   *          the presenters for this event
   */
  public void setOrganizers(List<String> organizers) {
    this.organizers = organizers;
  }

  /**
   * Returns the series presenters.
   *
   * @return the presenters
   */
  public List<String> getOrganizers() {
    return organizers;
  }

  /**
   * Add a contributor
   *
   * @param contributor
   *          The contributor's name.
   */
  public void addContributor(String contributor) {
    if (contributors == null) {
      contributors = new ArrayList<String>();
    }
    contributors.add(contributor);
  }

  /**
   * Sets the list of contributors.
   *
   * @param contributors
   *          the contributors for this event
   */
  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  /**
   * Returns the series contributors.
   *
   * @return the contributors
   */
  public List<String> getContributors() {
    return contributors;
  }

  /**
   * Add a publisher
   *
   * @param publisher
   *          The publisher's name.
   */
  public void addPublisher(String publisher) {
    if (publishers == null) {
      publishers = new ArrayList<String>();
    }
    publishers.add(publisher);
  }

  /**
   * Sets the list of publishers.
   *
   * @param publishers
   *          the publishers for this event
   */
  public void setPublishers(List<String> publishers) {
    this.publishers = publishers;
  }

  /**
   * Returns the series publishers.
   *
   * @return the publishers
   */
  public List<String> getPublishers() {
    return publishers;
  }

  /**
   * Sets the series rights holder.
   *
   * @param rights
   *          holder the rights holder
   */
  public void setRightsHolder(String rightsHolder) {
    this.rightsHolder = rightsHolder;
  }

  /**
   * Returns the series rights holder.
   *
   * @return the rights holder
   */
  public String getRightsHolder() {
    return rightsHolder;
  }

  /**
   * Sets the opt out status for this series
   *
   * @param optedOut
   *          the opt out status, whether the series is opted-out or not opted-out
   */
  public void setOptOut(boolean optOut) {
    this.optOut = optOut;
  }

  /**
   * Returns the opt out status from this series
   *
   * @return the opt out status from this series, whether the series is opted-out or not opted-out
   */
  public boolean isOptedOut() {
    return optOut;
  }

  /**
   * Sets the theme for this series
   *
   * @param theme
   *          the theme
   */
  public void setTheme(Long theme) {
    this.theme = theme;
  }

  /**
   * Returns the theme of this series
   *
   * @return the theme of this series
   */
  public Long getTheme() {
    return theme;
  }

  public boolean isSeriesTitleUpdated() {
    return seriesTitleUpdated;
  }

  /**
   * Reads the series from the input stream.
   *
   * @param xml
   *          the input stream
   * @return the deserialized recording event
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Series valueOf(InputStream xml) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(xml), Series.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Reads the series from the input stream.
   *
   * @param json
   *          the input stream
   * @return the deserialized recording event
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Series valueOfJson(InputStream json) throws IOException, JSONException, XMLStreamException,
          JAXBException {
    // TODO Get this to work, it is currently returning null properties for all properties.
    if (context == null) {
      createJAXBContext();
    }

    BufferedReader streamReader = new BufferedReader(new InputStreamReader(json, "UTF-8"));
    StringBuilder jsonStringBuilder = new StringBuilder();
    String inputStr;
    while ((inputStr = streamReader.readLine()) != null)
      jsonStringBuilder.append(inputStr);

    JSONObject obj = new JSONObject(jsonStringBuilder.toString());
    Configuration config = new Configuration();
    config.setSupressAtAttributes(true);
    Map<String, String> xmlToJsonNamespaces = new HashMap<String, String>(1);
    xmlToJsonNamespaces.put(IndexObject.INDEX_XML_NAMESPACE, "");
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
    MappedNamespaceConvention con = new MappedNamespaceConvention(config);
    XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Series event = (Series) unmarshaller.unmarshal(xmlStreamReader);
    return event;
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Series.class);
  }

  /**
   * Serializes the series.
   *
   * @return the serialized series
   */
  @Override
  public String toJSON() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Marshaller marshaller = Series.context.createMarshaller();

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
   * Serializes the series to an XML format.
   *
   * @return A String with this series' content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Series.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
