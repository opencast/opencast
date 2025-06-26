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
package org.opencastproject.adopter.registration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "oc_adopter_registration_extra")
@NamedQueries({
    @NamedQuery(name = "AdopterRegistrationExtra.findAll", query = "SELECT e FROM AdopterRegistrationExtra e")
})
public class AdopterRegistrationExtra {

  @Id
  @Column(name = "type", length = 32)
  private String type;

  @Lob
  @Column(name = "data", nullable = false, length = 65535)
  private String data;

  public AdopterRegistrationExtra() {
  }

  public AdopterRegistrationExtra(String type, String data) {
    this.type = type;
    this.data = data;
  }

  public String getType() {
    return this.type;
  }

  public String getData() {
    return this.data;
  }

  public void setData(String data) {
    this.data = data;
  }
}
