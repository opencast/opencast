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
package org.opencastproject.episode.impl.jmx;

import org.opencastproject.episode.impl.elementstore.ElementStore;
import org.opencastproject.util.data.Option.Match;

public class ElementStoreBean implements ElementStoreMXBean {

  private final ElementStore elementStore;

  public ElementStoreBean(ElementStore elementStore) {
    this.elementStore = elementStore;
  }

  /**
   * @see org.opencastproject.episode.impl.jmx.ElementStoreMXBean#getFreeSpace()
   */
  @Override
  public long getFreeSpace() {
    return elementStore.getUsableSpace().fold(new Match<Long, Long>() {
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
   * @see org.opencastproject.episode.impl.jmx.ElementStoreMXBean#getUsedSpace()
   */
  @Override
  public long getUsedSpace() {
    return elementStore.getUsedSpace().fold(new Match<Long, Long>() {
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
   * @see org.opencastproject.episode.impl.jmx.ElementStoreMXBean#getTotalSpace()
   */
  @Override
  public long getTotalSpace() {
    return elementStore.getTotalSpace().fold(new Match<Long, Long>() {
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
