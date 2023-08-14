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

package org.opencastproject.smil.entity.media;

import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.SmilMediaContainerImpl;
import org.opencastproject.smil.entity.media.element.SmilMediaElementImpl;

import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * {@link SmilMediaObject} implementation.
 */
@XmlSeeAlso({SmilMediaContainerImpl.class, SmilMediaElementImpl.class})
public abstract class SmilMediaObjectImpl extends SmilObjectImpl implements SmilMediaObject {

  /**
   * {@inheritDoc }
   */
  @Override
  public abstract boolean isContainer();
}
