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

package org.opencastproject.util.jaxb;

import org.opencastproject.util.data.Option;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB Adapter for the {@link org.opencastproject.util.data.Option Option} type
 *
 * @param <T>
 */
public class OptionAdapter<T> extends XmlAdapter<T, Option<T>> {

  @Override
  public T marshal(Option<T> option) throws Exception {
    return option.getOrElse((T) null);
  }

  @Override
  public Option<T> unmarshal(T o) throws Exception {
    return Option.option(o);
  }

}
