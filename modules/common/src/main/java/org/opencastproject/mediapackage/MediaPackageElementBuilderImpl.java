/*
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


package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;

import java.net.URI;

/**
 * Default implementation for a media package element builder.
 * <p>
 * Supports tracks, catalogs and attachments. Publications cannot have a flavor and are not supported. Use
 * {@link PublicationImpl#publication} to create them. The builder is stateless and can be shared.
 * </p>
 */
public class MediaPackageElementBuilderImpl implements MediaPackageElementBuilder {

  @Override
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    return elementFromURI(uri, null, null);
  }

  @Override
  public MediaPackageElement elementFromURI(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException {
    // Without a type, the element is an attachment, unless the flavor says otherwise
    if (type == null) {
      if (flavor != null && !flavor.equals(Attachment.FLAVOR)) {
        throw new UnsupportedElementException("Cannot create an element without type and with flavor " + flavor
                + " from " + uri);
      }
      type = MediaPackageElement.Type.Attachment;
    }

    MediaPackageElement element = createElement(type, uri);
    if (element == null) {
      throw new UnsupportedElementException("Cannot create an element of type " + type + " from " + uri);
    }
    element.setFlavor(flavor);
    return element;
  }

  @Override
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    MediaPackageElement element = createElement(type);
    if (element != null) {
      element.setFlavor(flavor);
    }
    return element;
  }

  /** Create an element for the given location. The factory methods also derive the mime type from the location. */
  private static MediaPackageElement createElement(MediaPackageElement.Type type, URI uri) {
    switch (type) {
      case Attachment:
        return AttachmentImpl.fromURI(uri);
      case Catalog:
        return CatalogImpl.fromURI(uri);
      case Track:
        return TrackImpl.fromURI(uri);
      default:
        return null;
    }
  }

  private static MediaPackageElement createElement(MediaPackageElement.Type type) {
    switch (type) {
      case Attachment:
        return new AttachmentImpl();
      case Catalog:
        return CatalogImpl.newInstance();
      case Track:
        return new TrackImpl();
      default:
        return null;
    }
  }

}
