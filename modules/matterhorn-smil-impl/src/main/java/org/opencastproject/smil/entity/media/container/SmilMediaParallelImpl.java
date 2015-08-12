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

package org.opencastproject.smil.entity.media.container;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represent SMIL par element and implement {@link SmilMediaContainer}.
 */
@XmlRootElement(name = "par")
public class SmilMediaParallelImpl extends SmilMediaContainerImpl {

  /**
   * Returns {@link ContainerType}.PAR
   *
   * @return container type PAR
   */
  @Override
  public ContainerType getContainerType() {
    return ContainerType.PAR;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  protected String getIdPrefix() {
    return "par";
  }
}
