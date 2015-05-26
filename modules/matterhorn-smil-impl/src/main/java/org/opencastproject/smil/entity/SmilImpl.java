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

package org.opencastproject.smil.entity;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilBody;
import org.opencastproject.smil.entity.api.SmilHead;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.xml.sax.SAXException;

/**
 * {@link Smil} implementation.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"head", "body"})
@XmlRootElement(name = "smil")
public class SmilImpl extends SmilObjectImpl implements Smil {

  /**
   * SMIL version
   */
  @XmlAttribute
  private static final String version = "3.0";
  /**
   * SMIL profile
   */
  @XmlAttribute
  private static final String baseProfile = "Language";
  /**
   * SMIL head
   */
  private SmilHead head = new SmilHeadImpl();
  /**
   * SMIL body
   */
  private SmilBody body = new SmilBodyImpl();

  /**
   * Empty constructor.
   */
  public SmilImpl() {
  }

  /**
   * {@inheritDoc}
   */
  @XmlElement(type = SmilHeadImpl.class)
  @Override
  public SmilHead getHead() {
    return head;
  }

  /**
   * Set the head of the SMIL.
   *
   * @param head the head to set
   */
  public void setHead(SmilHead head) {
    this.head = head;
  }

  /**
   * {@inheritDoc}
   */
  @XmlElement(type = SmilBodyImpl.class)
  @Override
  public SmilBody getBody() {
    return body;
  }

  /**
   * Set the body of the SMIL.
   *
   * @param body the body to set
   */
  public void setBody(SmilBody body) {
    this.body = body;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getIdPrefix() {
    return "s";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject removeElement(String elementId) {
    SmilObject child = null;
    if (head.getId().equals(elementId)) {
      child = head;
      head = null;
      return child;
    }
    if (body.getId().equals(elementId)) {
      child = body;
      body = null;
      return child;
    }

    child = ((SmilObjectImpl) head).removeElement(elementId);
    if (child != null) {
      return child;
    }
    child = ((SmilObjectImpl) body).removeElement(elementId);
    if (child != null && child instanceof SmilMediaElement) {
      // media elements can reference paramGroup element
      // remove linked paramGroup, if it is not referenced by another element
      String paramGroupId = ((SmilMediaElement) child).getParamGroup();
      if (paramGroupId != null && !paramGroupId.isEmpty()) {
        List<SmilObject> childs = new LinkedList<SmilObject>();
        putAllChilds(childs);
        for (SmilObject c : childs) {
          if (c instanceof SmilMediaElement
                  && paramGroupId.equals(((SmilMediaElement) c).getParamGroup())) {
            // set paramGroup to null, to prevent delete operation
            paramGroupId = null;
            break;
          }
        }
        if (paramGroupId != null) {
          removeElement(paramGroupId);
        }
      }
    }
    return child;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toXML() throws JAXBException, SAXException, MalformedURLException {
    StringWriter sw = new StringWriter();
    JAXBContext jctx = JAXBContext.newInstance(SmilImpl.class);
    Marshaller smilMarshaller = jctx.createMarshaller();
//        SmilMediaParamGroupsmilMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//        smilMarshaller.setSchema(schema);

    // TODO: add doctype
    // <!DOCTYPE smil PUBLIC "-//W3C//DTD SMIL 3.0 Language//EN" "http://www.w3.org/2008/SMIL30/SMIL30Language.dtd">

    smilMarshaller.marshal(this, sw);
    return sw.toString();
  }

  /**
   * JAXB helper method, references to {
   *
   * @see SmilImpl#fromXml(String)}.
   *
   * @param smil {@link Smil} document as xml
   * @return parsed {@link SmilImpl}
   * @throws JAXBException if unmarshalling fail
   */
  public static SmilImpl fromString(String smil) throws JAXBException {
    return (SmilImpl) fromXML(smil);
  }

  /**
   * Unmarshall a SMIL document from string.
   *
   * @param xml {@link Smil} document as xml
   * @return parsed {@link Smil}
   * @throws JAXBException if unmarshalling fail
   */
  public static Smil fromXML(String xml) throws JAXBException {
    JAXBContext jctx = JAXBContext.newInstance(SmilImpl.class);
    Unmarshaller unmarshaller = jctx.createUnmarshaller();
    return (Smil) unmarshaller.unmarshal(new StringReader(xml));
  }

  /**
   * Unmarshall a SMIL document from file.
   *
   * @param xml {@link Smil} document as file
   * @return parsed {@link Smil}
   * @throws JAXBException if unmarshalling fail
   */
  public static Smil fromXML(File xmlFile) throws JAXBException {
    JAXBContext jctx = JAXBContext.newInstance(SmilImpl.class);
    Unmarshaller unmarshaller = jctx.createUnmarshaller();
    return (Smil) unmarshaller.unmarshal(xmlFile);
  }

  /**
   * Clear all content in head and body.
   */
  public void clear() {
    ((SmilHeadImpl) head).clear();
    ((SmilBodyImpl) body).clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject getElementOrNull(String elementId) {
    if (getId().equals(elementId)) {
      return this;
    }

    SmilObject element = ((SmilHeadImpl) getHead()).getElementOrNull(elementId);
    if (element != null) {
      return element;
    }

    return ((SmilBodyImpl) getBody()).getElementOrNull(elementId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAllChilds(List<SmilObject> elements) {
    elements.add(getHead());
    ((SmilObjectImpl) getHead()).putAllChilds(elements);
    elements.add(getBody());
    ((SmilObjectImpl) getBody()).putAllChilds(elements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilObject get(String elementId) throws SmilException {
    SmilObject element = getElementOrNull(elementId);
    if (element == null) {
      throw new SmilException("There is no element with Id " + elementId);
    }

    return element;
  }
}
