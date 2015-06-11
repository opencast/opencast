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


package org.opencastproject.mediapackage.identifier;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * This serial id generator creates id's that are not unique across distributed installations of a node but return
 * padded representations.
 */
public class SerialIdBuilder implements IdBuilder {

  /** The number format */
  private static NumberFormat nf = new DecimalFormat("000");

  /** The current id value */
  private static long idCounter = 0;

  /** The lock */
  private static Object lock = new Object();

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#createNew()
   */
  public Id createNew() {
    synchronized (lock) {
      idCounter++;
      String id = nf.format(idCounter);
      StringBuffer paddedId = new StringBuffer();
      int i = 0;
      for (i = 0; i < id.length(); i++) {
        if (id.charAt(i) != '0')
          break;
        paddedId.append("_");
      }
      for (int j = i; j < id.length(); j++)
        paddedId.append(id.charAt(j));
      return new IdImpl(paddedId.toString());
    }
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#fromString(String)
   */
  public Id fromString(String id) throws IllegalArgumentException {
    if (id == null)
      throw new IllegalArgumentException("Argument 'id' is null");
    try {
      Long.parseLong(id.replace('_', ' '));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return new IdImpl(id);
  }

}
