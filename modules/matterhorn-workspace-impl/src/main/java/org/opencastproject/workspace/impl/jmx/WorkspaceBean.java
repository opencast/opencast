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
package org.opencastproject.workspace.impl.jmx;

import org.opencastproject.workspace.api.Workspace;

public class WorkspaceBean implements WorkspaceMXBean {

  private final Workspace workspace;

  public WorkspaceBean(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean#getFreeSpace()
   */
  @Override
  public long getFreeSpace() {
    return workspace.getUsableSpace();
  }

  /**
   * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean#getUsedSpace()
   */
  @Override
  public long getUsedSpace() {
    return workspace.getTotalSpace() - workspace.getUsableSpace();
  }

  /**
   * @see org.opencastproject.workspace.impl.jmx.WorkspaceMXBean#getTotalSpace()
   */
  @Override
  public long getTotalSpace() {
    return workspace.getTotalSpace();
  }

}
