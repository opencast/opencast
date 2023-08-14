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
package org.opencastproject.transcription.persistence;

public class TranscriptionProviderControl {

  /**
   * The provider id
   */
  private long id;

  /**
   * The provider
   */
  private String provider;

  public TranscriptionProviderControl(long id, String provider) {
    this.provider = provider;
    this.id = id;
  }

  /**
   * Sets the id
   *
   * @param id the provider id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the provider id
   *
   * @return the id
   */
  public long getId() {
    return this.id;
  }

  /**
   * Sets the provider
   *
   * @param provider the provider
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   * Returns the provider
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
  }

}
