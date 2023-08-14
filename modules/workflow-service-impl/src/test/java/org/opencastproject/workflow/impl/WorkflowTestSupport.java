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

package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowStateListener;

public final class WorkflowTestSupport {

  public static final int WAITTIME = 5; //ms

  private WorkflowTestSupport() {
    //pass
  }

  /*
   * Wait up to 1000ms for the number of changes to be right
   */
  public static void poll(WorkflowStateListener listener, int changes) throws Exception {
    for (int counter = 5000 / WAITTIME; listener.countStateChanges() != changes && counter > 0; counter--) {
      Thread.sleep(WAITTIME);
    }
    if (listener.countStateChanges() != changes) {
      throw new RuntimeException(String.format("Listener did not have the correct number (%s) of events inside of %s ms, got %s instead", changes, WAITTIME, listener.countStateChanges()));
    }
  }
}
