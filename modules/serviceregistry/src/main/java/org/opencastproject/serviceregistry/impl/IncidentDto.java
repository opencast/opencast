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

package org.opencastproject.serviceregistry.impl;

import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.util.JsonVal.asString;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.JsonVal;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Prop;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "Incident")
@Access(AccessType.FIELD)
@Table(name = "oc_incident", indexes = {
    @Index(name = "IX_oc_incident_jobid", columnList = ("jobid")),
    @Index(name = "IX_oc_incident_severity", columnList = ("severity")) })
@NamedQueries({@NamedQuery(name = "Incident.findByJobId",
                           query = "select a from Incident a where a.jobId = :jobId")})
public class IncidentDto {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "jobid")
  private Long jobId;

  @Column(name = "timestamp")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  @Column(name = "code")
  private String code;

  @Column(name = "severity")
  private Integer severity;

  @Lob
  @Column(name = "parameters", length = 65535)
  private String parameters;

  @Lob
  @Column(name = "details", length = 65535)
  private String details;

  /** Constructor method. */
  public static IncidentDto mk(
          Long jobId,
          Date date,
          String code,
          Severity severity,
          Map<String, String> parameters,
          List<Tuple<String, String>> details) {
    IncidentDto dto = new IncidentDto();
    dto.jobId = jobId;
    dto.timestamp = date;
    dto.code = code;
    dto.severity = severity.ordinal();

    List<Prop> props = new ArrayList<Jsons.Prop>();
    for (Entry<String, String> entry : parameters.entrySet()) {
      props.add(p(entry.getKey(), entry.getValue()));
    }
    dto.parameters = obj(props.toArray(new Prop[props.size()])).toJson();

    props = new ArrayList<Jsons.Prop>();
    for (Tuple<String, String> t : details) {
      props.add(p(t.getA(), t.getB()));
    }
    dto.details = obj(props.toArray(new Prop[props.size()])).toJson();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public long getJobId() {
    return jobId;
  }

  /** @see org.opencastproject.job.api.Incident#getTimestamp() */
  public Date getTimestamp() {
    return timestamp;
  }

  /** @see org.opencastproject.job.api.Incident#getSeverity() */
  public Severity getSeverity() {
    return Severity.values()[severity];
  }

  /** @see org.opencastproject.job.api.Incident#getCode() */
  public String getCode() {
    return code;
  }

  /** @see org.opencastproject.job.api.Incident#getDetails() */
  public List<Tuple<String, String>> getTechnicalInformation() {
    final List<Tuple<String, String>> list = new ArrayList<Tuple<String, String>>();
    JsonObj messageJson = JsonObj.jsonObj(details);
    for (Object k : messageJson.keySet()) {
      String title = JsonVal.asJsonVal.apply(k).as(asString);
      String content = messageJson.val(title).as(asString);
      list.add(tuple(title, content));
    }
    return list;
  }

  /** @see org.opencastproject.job.api.Incident#getDescriptionParameters() */
  public Map<String, String> getParameters() {
    Map<String, String> param = new HashMap<String, String>();
    JsonObj paramJson = JsonObj.jsonObj(parameters);
    for (Object k : paramJson.keySet()) {
      String key = JsonVal.asJsonVal.apply(k).as(asString);
      String value = paramJson.val(key).as(asString);
      param.put(key, value);
    }
    return param;
  }

  public static Function<EntityManager, List<IncidentDto>> findByJobIdQuery(long jobId) {
    return namedQuery.findAll("Incident.findByJobId", IncidentDto.class, Pair.of("jobId", jobId));
  }
}
