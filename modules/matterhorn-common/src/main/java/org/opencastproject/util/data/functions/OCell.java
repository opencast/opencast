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

package org.opencastproject.util.data.functions;

import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Option;

import static org.opencastproject.util.data.Option.some;

/** Cell containing optional values. Just to shorten type annotations a bit. */
public class OCell<A> extends Cell<Option<A>> {
  public OCell(Option<A> as) {
    super(as);
  }

  /** Create a cell containing some a. */
  public static <A> OCell<A> ocell(A a) {
    return new OCell<A>(some(a));
  }

  /** Create a cell containing none. */
  public static <A> OCell<A> ocell() {
    return new OCell<A>(Option.<A>none());
  }
}
