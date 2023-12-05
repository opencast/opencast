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

package org.opencastproject.composer.api;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB annotated collection wrapper for {@link EncodingProfileImpl}s.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "profiles", namespace = "http://composer.opencastproject.org")
@XmlRootElement(name = "profiles", namespace = "http://composer.opencastproject.org")
public class EncodingProfileList {

  public EncodingProfileList() {
  }

  public EncodingProfileList(List<EncodingProfileImpl> list) {
    this.profiles = list;
  }

  @XmlElement(name = "profile")
  protected List<EncodingProfileImpl> profiles;

  public List<EncodingProfileImpl> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<EncodingProfileImpl> profiles) {
    this.profiles = profiles;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((profiles == null) ? 0 : profiles.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EncodingProfileList other = (EncodingProfileList) obj;
    if (profiles == null) {
      if (other.profiles != null)
        return false;
    } else if (!profiles.equals(other.profiles))
      return false;
    return true;
  }

}
