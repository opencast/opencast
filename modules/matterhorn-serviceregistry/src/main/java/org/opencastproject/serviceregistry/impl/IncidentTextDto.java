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

import org.opencastproject.util.data.Function;
import org.opencastproject.util.persistence.Queries;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity(name = "IncidentText")
@Access(AccessType.FIELD)
@Table(name = "mh_incident_text")
@NamedQueries({@NamedQuery(name = "IncidentText.findAll", query = "select a from IncidentText a")})
public class IncidentTextDto {
  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "text")
  private String text;

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public static IncidentTextDto mk(String id, String text) {
    final IncidentTextDto dto = new IncidentTextDto();
    dto.id = id;
    dto.text = text;
    return dto;
  }

  public static final Function<EntityManager, List<IncidentTextDto>> findAll =
          Queries.named.findAll("IncidentText.findAll");
}
