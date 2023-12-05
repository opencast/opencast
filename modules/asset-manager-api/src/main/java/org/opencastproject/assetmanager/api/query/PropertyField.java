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
package org.opencastproject.assetmanager.api.query;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyName;

/**
 * Description of a property of type <code>A</code>.
 * <p>
 * Implementations are supposed to be immutable.
 */
public interface PropertyField<A> extends Field<A> {
  Target target();

  PropertyName name();

  Property mk(String mpId, A value);
}
