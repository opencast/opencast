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

package org.opencastproject.workingfilerepository.jmx;

import org.opencastproject.util.data.Option.Match;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

public class WorkingFileRepositoryBean implements WorkingFileRepositoryMXBean {

  private final WorkingFileRepository workingFileRepository;

  public WorkingFileRepositoryBean(WorkingFileRepository workingFileRepository) {
    this.workingFileRepository = workingFileRepository;
  }

  /**
   * @see org.opencastproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getFreeSpace()
   */
  @Override
  public long getFreeSpace() {
    return workingFileRepository.getUsableSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

  /**
   * @see org.opencastproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getUsedSpace()
   */
  @Override
  public long getUsedSpace() {
    return workingFileRepository.getUsedSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

  /**
   * @see org.opencastproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getTotalSpace()
   */
  @Override
  public long getTotalSpace() {
    return workingFileRepository.getTotalSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

}
