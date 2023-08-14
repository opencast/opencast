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

package org.opencastproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * List of possible retry strategies in case of operation hold state
 */
public enum RetryStrategy {

  /** Failed without retry strategy */
  NONE,

  /** Restart the operation */
  RETRY,

  /** Keep the operation in hold state */
  HOLD;

  public static class Adapter extends XmlAdapter<String, RetryStrategy> {

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public String marshal(RetryStrategy retryStrategy) {
      return retryStrategy == null ? null : retryStrategy.toString().toLowerCase();
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public RetryStrategy unmarshal(String val) {
      return val == null ? null : RetryStrategy.valueOf(val.toUpperCase());
    }

  }

}
