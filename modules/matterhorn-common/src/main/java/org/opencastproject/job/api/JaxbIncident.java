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

import static org.opencastproject.util.data.Collections.nullToNil;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.fn.juc.Mutables;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

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
    this.descriptionParameters = mlist(incident.getDescriptionParameters().entrySet()).map(Param.mkFn).value();
    this.details = mlist(incident.getDetails()).map(JaxbIncidentDetail.mkFn).value();
  }

  public static final Function<Incident, JaxbIncident> mkFn = new Function<Incident, JaxbIncident>() {
    @Override public JaxbIncident apply(Incident incident) {
      return new JaxbIncident(incident);
    }
  };

  public Incident toIncident() {
    return new IncidentImpl(
            id,
            jobId,
            serviceType,
            processingHost,
            timestamp,
            severity,
            code,
            mlist(nullToNil(details)).map(JaxbIncidentDetail.toDetailFn).value(),
            mlist(nullToNil(descriptionParameters)).foldl(Mutables.<String, String>hashMap(), new Function2<Map<String, String>, Param, Map<String, String>>() {
              @Override public Map<String, String> apply(Map<String, String> sum, Param param) {
                sum.put(param.getName(), param.getValue());
                return sum;
              }
            }));
  }

  public static final Function<JaxbIncident, Incident> toIncidentFn = new Function<JaxbIncident, Incident>() {
    @Override public Incident apply(JaxbIncident dto) {
      return dto.toIncident();
    }
  };

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

    public static final Function<Entry<String, String>, Param> mkFn = new Function<Entry<String, String>, Param>() {
      @Override public Param apply(Entry<String, String> entry) {
        return mk(entry);
      }
    };
  }
}
