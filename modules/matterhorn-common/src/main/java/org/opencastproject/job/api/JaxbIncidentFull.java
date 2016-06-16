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

package org.opencastproject.job.api;

import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.serviceregistry.api.IncidentL10n;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.util.data.Function;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentFull", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentFull", namespace = "http://job.opencastproject.org")
public final class JaxbIncidentFull {
  @XmlElement(name = "id")
  private long id;

  @XmlElement(name = "jobid")
  private long jobId;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "serviceType")
  private String serviceType;

  @XmlElement(name = "processingHost")
  private String processingHost;

  @XmlElement(name = "date")
  private Date date;

  @XmlElement(name = "severity")
  private String severity;

  @XmlElement(name = "code")
  private String code;

  @XmlElement(name = "detail")
  @XmlElementWrapper(name = "details")
  private List<JaxbIncidentDetail> details;

  public JaxbIncidentFull() {
  }

  public JaxbIncidentFull(Incident incident, IncidentL10n l10n) {
    this.id = incident.getId();
    this.jobId = incident.getJobId();
    this.serviceType = incident.getServiceType();
    this.title = l10n.getTitle();
    this.processingHost = incident.getProcessingHost();
    this.date = incident.getTimestamp();
    this.severity = incident.getSeverity().name();
    this.code = incident.getCode();
    this.details = mlist(incident.getDetails()).map(JaxbIncidentDetail.mkFn).value();
    this.description = l10n.getDescription();
  }

  public static Function<Incident, JaxbIncidentFull> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<Incident, JaxbIncidentFull>() {
      @Override
      public JaxbIncidentFull xapply(Incident incident) throws Exception {
        return new JaxbIncidentFull(incident, svc.getLocalization(incident.getId(), locale));
      }
    };
  }
}
