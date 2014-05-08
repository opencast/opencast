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
package org.opencastproject.smil.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.io.IOUtils;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.entity.SmilImpl;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilObject;

/**
 * {@link SmilResponse} implementation.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "smil-response", namespace = "http://smil.opencastproject.org")
public class SmilResponseImpl implements SmilResponse {

  /**
   * Smil
   */
  private Smil smil;
  /**
   * Entities
   */
  private SmilObject[] entities;

  /**
   * Empty constructor (needed for JAXB).
   */
  private SmilResponseImpl() {
    smil = null;
    entities = null;
  }

  /**
   * Constructor
   *
   * @param smil to set
   */
  public SmilResponseImpl(Smil smil) {
    this(smil, new SmilObject[]{});
  }

  /**
   * Constructor.
   *
   * @param smil to set
   * @param entity to set
   */
  public SmilResponseImpl(Smil smil, SmilObject entity) {
    this(smil, new SmilObject[]{entity});
  }

  /**
   * Constructor.
   *
   * @param smil to set
   * @param entities to set
   */
  public SmilResponseImpl(Smil smil, SmilObject[] entities) {
    this.smil = smil;
    this.entities = entities;
  }

  /**
   * {@inheritDoc }
   */
  @XmlElement(type = SmilImpl.class, required = true)
  @Override
  public Smil getSmil() {
    return smil;
  }

  /**
   * Set {@link Smil}.
   *
   * @param smil to set
   */
  private void setSmil(Smil smil) {
    this.smil = smil;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public int getEntitiesCount() {
    if (entities == null) {
      return 0;
    } else {
      return entities.length;
    }
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject getEntity() throws SmilException {
    if (entities.length == 0) {
      throw new SmilException("There is no entity.");
    }
    if (entities.length > 1) {
      throw new SmilException("There is more than one entity.");
    }
    return entities[0];
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public SmilObject[] getEntities() throws SmilException {
    if (entities.length == 0) {
      throw new SmilException("There are no entities.");
    }
    return entities;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public String toXml() throws JAXBException {
    StringWriter writer = new StringWriter();
    JAXBContext ctx = JAXBContext.newInstance(SmilResponseImpl.class);
    Marshaller marshaller = ctx.createMarshaller();
    // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.marshal(this, writer);
    return writer.toString();
  }

  /**
   * Deserialize {@link SmilResponse} from XML.
   *
   * @param smilResponseXml {@link SmilResponse} as XML
   * @return {@link SmilResponse} object
   * @throws JAXBException if deserialization fail
   */
  public static SmilResponse fromXml(String smilResponseXml) throws JAXBException {
    InputStream smilStream = IOUtils.toInputStream(smilResponseXml);
    try {
      return fromXml(smilStream);
    } finally {
      IOUtils.closeQuietly(smilStream);
    }
  }

  /**
   * Deserialize {@link SmilResponse} from XML.
   *
   * @param smilResponseXmlFile {@link SmilResponse} as XML {@link File}
   * @return {@link SmilResponse} object
   * @throws JAXBException if deserialization fail
   */
  public static SmilResponse fromXml(File smilResponseXmlFile) throws JAXBException, FileNotFoundException {
    FileInputStream smilStream = new FileInputStream(smilResponseXmlFile);
    try {
      return fromXml(smilStream);
    } finally {
      IOUtils.closeQuietly(smilStream);
    }
  }

  /**
   * Deserialize {@link SmilResponse} from XML.
   *
   * @param smilResponseXmlFile {@link SmilResponse} as XML {@link InputStream}
   * @return {@link SmilResponse} object
   * @throws JAXBException if deserialization fail
   */
  protected static SmilResponse fromXml(InputStream smilResponseXml) throws JAXBException {
    StringWriter writer = new StringWriter();
    JAXBContext ctx = JAXBContext.newInstance(SmilResponseImpl.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    return (SmilResponse) unmarshaller.unmarshal(smilResponseXml);

  }

  /**
   * JAXB helper method.
   *
   * @return
   */
  @XmlElementRef
  private SmilResponseEntity<SmilObject>[] getResponseEntities() {
    SmilResponseEntity[] entitiesWrapped = new SmilResponseEntity[entities.length];
    for (int i = 0; i < entities.length; i++) {
      entitiesWrapped[i] = new SmilResponseEntity(entities[i]);
    }
    return entitiesWrapped;
  }

  /**
   * JAXB helper method.
   *
   * @param entities
   */
  private void setResponseEntities(SmilResponseEntity<SmilObject>[] entities) {
    this.entities = new SmilObject[entities.length];
    for (int e = 0; e < entities.length; e++) {
      this.entities[e] = entities[e].getEntity();
    }
  }

  /**
   * {@link SmilObject} wrapper class for serialization.
   *
   * @param <SmilObject>
   */
  @XmlRootElement(name = "entity", namespace = "http://smil.opencastproject.org")
  private static class SmilResponseEntity<SmilObject> {

    private SmilObject entity;

    SmilResponseEntity() {
    }

    SmilResponseEntity(SmilObject entity) {
      this.entity = entity;
    }

    @XmlElementRef(type = SmilObjectImpl.class)
    public SmilObject getEntity() {
      return entity;
    }

    private void setEntity(SmilObject entity) {
      this.entity = entity;
    }
  }
}
