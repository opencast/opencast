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

package org.opencastproject.search.impl.solr;

import static org.opencastproject.util.RequireUtil.notNull;

/**
 * A dynamic field in the solr index.
 */
public class DField<A> {

  private final A value;
  private final String suffix;

  public DField(A value, String suffix) {
    this.value = notNull(value, "value");
    this.suffix = notNull(suffix, "suffix");
  }

  public A getValue() {
    return value;
  }

  public String getSuffix() {
    return suffix;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("DField");
    sb.append("{value=").append(value);
    sb.append(", suffix=").append(suffix);
    sb.append('}');
    return sb.toString();
  }

}
