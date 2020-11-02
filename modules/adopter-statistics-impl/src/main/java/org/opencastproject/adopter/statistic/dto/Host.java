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

package org.opencastproject.adopter.statistic.dto;

import org.opencastproject.serviceregistry.api.HostRegistration;

/**
 * DTO that contains information about a host machine of an adopter. It's a simplified version
 * of the HostRegistration class {@link org.opencastproject.serviceregistry.api.HostRegistration}.
 */
public class Host {

  /** Amount of cores. */
  private int cores;

  /** The maximum load this host can run. */
  private float maxLoad;

  /** The allocated memory of this host. */
  private long memory;


  public Host(HostRegistration host) {
    this.cores = host.getCores();
    this.maxLoad = host.getMaxLoad();
    this.memory = host.getMemory();
  }


  //================================================================================
  // Getter and Setter
  //================================================================================

  public int getCores() {
    return cores;
  }

  public void setCores(int cores) {
    this.cores = cores;
  }

  public float getMaxLoad() {
    return maxLoad;
  }

  public void setMaxLoad(float maxLoad) {
    this.maxLoad = maxLoad;
  }

  public long getMemory() {
    return memory;
  }

  public void setMemory(long memory) {
    this.memory = memory;
  }

}
