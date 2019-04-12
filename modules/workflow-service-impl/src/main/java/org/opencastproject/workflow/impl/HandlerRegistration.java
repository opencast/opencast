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

import org.opencastproject.workflow.api.WorkflowOperationHandler;

/**
 * A tuple of a workflow operation handler and the name of the operation it handles
 */
public class HandlerRegistration {

  protected WorkflowOperationHandler handler;
  protected String operationName;

  public HandlerRegistration(String operationName, WorkflowOperationHandler handler) {
    if (operationName == null)
      throw new IllegalArgumentException("Operation name cannot be null");
    if (handler == null)
      throw new IllegalArgumentException("Handler cannot be null");
    this.operationName = operationName;
    this.handler = handler;
  }

  public WorkflowOperationHandler getHandler() {
    return handler;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + handler.hashCode();
    result = prime * result + operationName.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    HandlerRegistration other = (HandlerRegistration) obj;
    if (!handler.equals(other.handler))
      return false;
    return operationName.equals(other.operationName);
  }
}
