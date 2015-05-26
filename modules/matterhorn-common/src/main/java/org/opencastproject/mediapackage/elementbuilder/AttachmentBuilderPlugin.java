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


package org.opencastproject.mediapackage.elementbuilder;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import org.w3c.dom.Node;

import java.net.URI;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognizes arbitrary attachments and creates
 * media package element representations for them.
 * <p>
 * A media package element is considered an attachment by this plugin if it is of type {@link Attachment} and does not
 * have any specializing flavor.
 */
public class AttachmentBuilderPlugin extends AbstractAttachmentBuilderPlugin implements MediaPackageElementBuilder {

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin#accept(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  @Override
  public boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    if (type != null && flavor != null) {
      if (!type.equals(MediaPackageElement.Type.Attachment))
        return false;
    } else if (type != null && !type.equals(MediaPackageElement.Type.Attachment)) {
      return false;
    } else if (flavor != null && !flavor.equals(Attachment.FLAVOR)) {
      return false;
    }
    return super.accept(uri, type, flavor);
  }

  /**
   * @see org.opencastproject.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin#accept(org.w3c.dom.Node)
   */
  @Override
  public boolean accept(Node elementNode) {
    return super.accept(elementNode);
  }

  /**
   * {@inheritDoc}
   *
   * This plugin is an implementation for unknown attachments, therefore it returns <code>-1</code> as its priority.
   *
   * @see org.opencastproject.mediapackage.elementbuilder.AbstractElementBuilderPlugin#getPriority()
   */
  @Override
  public int getPriority() {
    return -1;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Attachment Builder Plugin";
  }

}
