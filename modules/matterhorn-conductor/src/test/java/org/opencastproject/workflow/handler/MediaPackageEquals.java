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

package org.opencastproject.workflow.handler;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;

import org.easymock.IArgumentMatcher;

public class MediaPackageEquals implements IArgumentMatcher {
  private MediaPackage expected;

  public MediaPackageEquals(MediaPackage expected) {
    this.expected = expected;
  }

  public boolean matches(Object actual) {
    if (!(actual instanceof MediaPackage)) {
      return false;
    }
    MediaPackage actualMP = (MediaPackage) actual;

    // check tracks with the same id exist and flavour is the same
    if (expected.getTracks().length != actualMP.getTracks().length)
      return false;
    for (Track t : expected.getTracks()) {
      if (!t.getFlavor().equals(actualMP.getTrack(t.getIdentifier()).getFlavor()))
        return false;
    }

    // check catalogs
    if (expected.getCatalogs().length != actualMP.getCatalogs().length)
      return false;
    for (Catalog c : expected.getCatalogs()) {
      if (!c.getFlavor().equals(actualMP.getCatalog(c.getIdentifier()).getFlavor()))
        return false;
    }

    // check attachments
    if (expected.getAttachments().length != actualMP.getAttachments().length)
      return false;
    for (Attachment a : expected.getAttachments()) {
      if (!a.getFlavor().equals(actualMP.getAttachment(a.getIdentifier()).getFlavor()))
        return false;
    }

    return true;

    // this could be done like this:
    // return expected.equals(actual);
    // but it doesent work as this checks if the identifier is the same

  }

  public void appendTo(StringBuffer buffer) {
    buffer.append("eqMediaPackage(\"");
    buffer.append(expected.getIdentifier());
    buffer.append("\")");
  }
}
