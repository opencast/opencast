/*
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

package org.opencastproject.job.api;

import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.jaxb.UtcTimestampAdapter;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/** 1:1 serialization of a {@link Incident}. */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "incident", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incident", namespace = "http://job.opencastproject.org")
public final class JaxbIncident {
  @XmlElement(name = "id")
  private long id;

  @XmlElement(name = "jobId")
  private long jobId;

  @XmlElement(name = "serviceType")
  private String serviceType;

  @XmlElement(name = "processingHost")
  private String processingHost;

  @XmlElement(name = "timestamp")
  @XmlJavaTypeAdapter(UtcTimestampAdapter.class)
  private Date timestamp;

  @XmlElement(name = "severity")
  private Severity severity;

  @XmlElement(name = "code")
  private String code;

  @XmlElementWrapper(name = "descriptionParameters")
  @XmlElement(name = "param")
  private List<Param> descriptionParameters;

  @XmlElementWrapper(name = "details")
  @XmlElement(name = "detail")
  private List<JaxbIncidentDetail> details;

  /** Constructor for JAXB */
  public JaxbIncident() {
  }

  public JaxbIncident(Incident incident) {
    this.id = incident.getId();
    this.jobId = incident.getJobId();
    this.serviceType = incident.getServiceType();
    this.processingHost = incident.getProcessingHost();
    this.timestamp = new Date(incident.getTimestamp().getTime());
    this.severity = incident.getSeverity();
    this.code = incident.getCode();
    this.descriptionParameters = incident.getDescriptionParameters().entrySet().stream()
        .map(Param::mk)
        .collect(Collectors.toList());
    this.details = incident.getDetails().stream()
        .map(JaxbIncidentDetail::new)
        .collect(Collectors.toList());
  }

  public Incident toIncident() {
    List<Tuple<String, String>> mappedDetails = (details != null ? details : List.<JaxbIncidentDetail>of()).stream()
        .map(JaxbIncidentDetail::toDetail)
        .collect(Collectors.toList());

    Map<String, String> paramMap = (descriptionParameters != null ? descriptionParameters : List.<Param>of()).stream()
        .collect(Collectors.toMap(
            Param::getName,
            Param::getValue
        ));

    return new IncidentImpl(
        id,
        jobId,
        serviceType,
        processingHost,
        timestamp,
        severity,
        code,
        mappedDetails,
        paramMap
    );
  }

  /**
   * An description parameter. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "param", namespace = "http://job.opencastproject.org")
  public static final class Param {
    @XmlAttribute(name = "name")
    private String name;

    @XmlValue
    private String value;

    public static Param mk(Entry<String, String> entry) {
      final Param dto = new Param();
      dto.name = entry.getKey();
      dto.value = entry.getValue();
      return dto;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }
}
