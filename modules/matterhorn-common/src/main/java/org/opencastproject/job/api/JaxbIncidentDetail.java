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

import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * JAXB DTO for a technical detail of a job incident. See {@link Incident#getDetails()}.
 * <p/>
 * To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "detail", namespace = "http://job.opencastproject.org")
public final class JaxbIncidentDetail {
  @XmlAttribute(name = "title")
  private String title;

  @XmlValue
  private String content;

  /** Constructor for JAXB */
  public JaxbIncidentDetail() {
  }

  public JaxbIncidentDetail(Tuple<String, String> detail) {
    this.title = detail.getA();
    this.content = detail.getB();
  }

  public static final Function<Tuple<String, String>, JaxbIncidentDetail> mkFn = new Function<Tuple<String, String>, JaxbIncidentDetail>() {
    @Override public JaxbIncidentDetail apply(Tuple<String, String> detail) {
      return new JaxbIncidentDetail(detail);
    }
  };

  public Tuple<String, String> toDetail() {
    return tuple(title, content);
  }

  public static final Function<JaxbIncidentDetail, Tuple<String, String>> toDetailFn = new Function<JaxbIncidentDetail, Tuple<String, String>>() {
    @Override public Tuple<String, String> apply(JaxbIncidentDetail dto) {
      return dto.toDetail();
    }
  };
}
