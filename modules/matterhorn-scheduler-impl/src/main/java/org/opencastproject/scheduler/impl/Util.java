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
package org.opencastproject.scheduler.impl;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;

public final class Util {
  private Util() {
  }

  public static long getEventIdentifier(DublinCoreCatalog dc) {
    try {
      return Long.parseLong(dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    } catch (Exception e) {
      throw new IllegalArgumentException("DublinCore does not have an identifier of type long");
    }
  }

  /** Sets the dcterms:identifier property of a copy of dc. */
  public static DublinCoreCatalog setEventIdentifierImmutable(long eventId, DublinCoreCatalog dc) {
    if (!(dc instanceof DublinCoreCatalogImpl)) {
      throw new IllegalArgumentException("Dublin core catalog must be of type DublinCoreCatalogImpl");
    }
    final DublinCoreCatalog copy = (DublinCoreCatalog) ((DublinCoreCatalogImpl) dc).clone();
    copy.set(DublinCore.PROPERTY_IDENTIFIER, Long.toString(eventId));
    return copy;
  }

  /** Mutates the dcterms:identifier property of dc. */
  public static void setEventIdentifierMutable(long eventId, DublinCoreCatalog dc) {
    dc.set(DublinCore.PROPERTY_IDENTIFIER, Long.toString(eventId));
  }
}
