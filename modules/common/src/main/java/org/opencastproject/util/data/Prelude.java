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

package org.opencastproject.util.data;

/**
 * The prelude contains general purpose functions.
 */
public final class Prelude {
  private Prelude() {
  }

  /**
   * Java is not able to determine the exhaustiveness of a match. Use this function to throw a defined error and to
   * improve readability.
   */
  public static <A> A unexhaustiveMatch() {
    throw new Error("Unexhaustive match");
  }

  /**
   * Java is not able to determine the exhaustiveness of a match. Use this function to throw a defined error and to
   * improve readability.
   */
  public static Error unexhaustiveMatchError() {
    return new Error("Unexhaustive match");
  }

  public static <A> A notYetImplemented() {
    throw new Error("not yet implemented");
  }

  /** Sleep for a while. Returns false if interrupted. */
  public static boolean sleep(long ms) {
    try {
      Thread.sleep(ms);
      return true;
    } catch (InterruptedException ignore) {
      return false;
    }
  }
}
