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
package org.opencastproject.job.api;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * JAXB annotated implementation of the job context.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "jobcontext", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "jobcontext", namespace = "http://job.opencastproject.org")
public class JaxbJobContext implements JobContext {

  /** The context identifier, which is equal to the root job identifier */
  protected Long id = null;

  /** The properties */
  protected HashMap<String, String> properties = null;

  // protected Long parentJobId = null;
  //
  // protected String userId = null;
  //

  /** The default no arg constructor needed by JAXB */
  public JaxbJobContext() {
    this.properties = new HashMap<String, String>();
  }

  /**
   * Constructs a jaxb job context from another context.
   * 
   * @param jobContext
   *          the template
   */
  public JaxbJobContext(JobContext jobContext) {
    this.id = jobContext.getId();
    this.properties = new HashMap<String, String>();
    // TODO NULL CHECK
    if (jobContext.getProperties() != null) {
      this.properties.putAll(jobContext.getProperties());
    }
  }

  //
  // /**
  // * {@inheritDoc}
  // *
  // * @see org.opencastproject.job.api.JobContext#getParentJobId()
  // */
  // @XmlElement(name = "parent")
  // @Override
  // public Long getParentJobId() {
  // return parentJobId;
  // }
  //
  // /**
  // * Sets the parent job id.
  // *
  // * @param id
  // * the parent job id
  // */
  // public void setParentJobId(Long id) {
  // this.parentJobId = id;
  // }
  //
  // /**
  // * {@inheritDoc}
  // *
  // * @see org.opencastproject.job.api.JobContext#getUserId()
  // */
  // @XmlElement(name = "user")
  // @Override
  // public String getUserId() {
  // return userId;
  // }
  //
  // /**
  // * Sets the user id.
  // *
  // * @param userId
  // * the user id
  // */
  // public void setUserId(String userId) {
  // this.userId = userId;
  // }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobContext#getProperties()
   */
  @XmlElement(name = "properties")
  @Override
  public HashMap<String, String> getProperties() {
    return properties;
  }

  /**
   * Sets the context properties.
   * 
   * @param properties
   *          the properties
   */
  public void setProperties(Map<String, String> properties) {
    this.properties.clear();
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  /**
   * Gets the identifier for this context.
   * 
   * @return the context identifier
   */
  @XmlAttribute(name = "id")
  public Long getId() {
    return id;
  }

  /**
   * Sets the context identifier.
   * 
   * @param id
   *          the context id to set the id
   */
  public void setId(Long id) {
    this.id = id;
  }

}
