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

package org.opencastproject.index.service.impl.index.group;

import org.opencastproject.index.service.impl.index.IndexObject;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

/**
 * Object wrapper for a group.
 */
@XmlType(name = "group", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "name",
        "description", "organization", "roles", "members" })
@XmlRootElement(name = "group", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Group implements IndexObject {

  /** The name of the surrounding XML tag to wrap a result of multiple groups */
  public static final String XML_SURROUNDING_TAG = "groups";

  /** The document type */
  public static final String DOCUMENT_TYPE = "group";

  /** The identifier */
  @XmlElement(name = "identifier")
  private String identifier = null;

  /** The name for the group */
  @XmlElement(name = "name")
  private String name = null;

  /** The description */
  @XmlElement(name = "description")
  private String description = null;

  /** The organization for the group */
  @XmlElement(name = "organization")
  private String organization = null;

  @XmlElementWrapper(name = "roles")
  @XmlElement(name = "role")
  private Set<String> roles = null;

  @XmlElementWrapper(name = "members")
  @XmlElement(name = "member")
  private Set<String> members = null;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Group() {

  }

  public Group(String identifier, String organization) {
    this.identifier = identifier;
    this.organization = organization;
  }

  /**
   * Create a new group with all of the properties populated.
   *
   * @param identifier
   *          The identifier for this group
   * @param name
   *          The easy to read name of this group
   * @param description
   *          The description of the group
   * @param organization
   *          The organization applying to this group
   * @param roles
   *          The roles that are given with being in this group
   * @param members
   *          The user names of the current members of the group.
   */
  public Group(String identifier, String name, String description, String organization, Set<String> roles,
          Set<String> members) {
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.organization = organization;
    this.roles = roles;
    this.members = members;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Add role to this group
   *
   * @param role
   *          The role.
   */
  public void addRole(String role) {
    if (roles == null) {
      roles = new HashSet<String>();
    }
    roles.add(role);
  }

  /**
   * Sets the list of roles.
   *
   * @param roles
   *          the roles for this group
   */
  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  /**
   * Returns the group's roles.
   *
   * @return the roles
   */
  public Set<String> getRoles() {
    return roles;
  }

  /**
   * Add a member
   *
   * @param member
   *          The member's name.
   */
  public void addMember(String member) {
    if (members == null) {
      members = new HashSet<String>();
    }
    members.add(member);
  }

  /**
   * Sets the list of members.
   *
   * @param members
   *          the members for this group
   */
  public void setMembers(Set<String> members) {
    this.members = members;
  }

  /**
   * Returns the group members.
   *
   * @return the members
   */
  public Set<String> getMembers() {
    return members;
  }

  /**
   * Reads the group from the input stream.
   *
   * @param xml
   *          the input stream
   * @return the deserialized group
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Group valueOf(InputStream xml) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(xml), Group.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Reads the group from the input stream.
   *
   * @param json
   *          the input stream
   * @return the deserialized group
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Group valueOfJson(InputStream json) throws IOException, JSONException, XMLStreamException,
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
    xmlToJsonNamespaces.put(INDEX_XML_NAMESPACE, "");
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
    MappedNamespaceConvention con = new MappedNamespaceConvention(config);
    XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Group event = (Group) unmarshaller.unmarshal(xmlStreamReader);
    return event;
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Group.class);
  }

  @Override
  public String toJSON() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Marshaller marshaller = Group.context.createMarshaller();

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
   * Serializes the group to an XML format.
   *
   * @return A String with this group' content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Group.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
