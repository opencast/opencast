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

import java.net.URI;

/**
 * A media package element builder provides factory methods for the creation of media package elements.
 * <p>
 * To read media package elements from XML, use {@link MediaPackageElementParser} instead.
 * </p>
 */
public interface MediaPackageElementBuilder {

  /**
   * Creates an attachment for the given location.
   *
   * @param uri
   *          the element location
   * @return the new media package element
   * @throws UnsupportedElementException
   *           if creating the media package element fails
   */
  MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException;

  /**
   * Creates a media package element of the given type and flavor for the given location.
   * <p>
   * If <code>type</code> is <code>null</code>, an attachment is created. In that case, <code>flavor</code> must be
   * either <code>null</code> or {@link Attachment#FLAVOR}.
   * </p>
   *
   * @param uri
   *          the element location
   * @param type
   *          the element type, may be <code>null</code>
   * @param flavor
   *          the element flavor, may be <code>null</code>
   * @return the new media package element
   * @throws UnsupportedElementException
   *           if no element can be created for this type and flavor
   */
  MediaPackageElement elementFromURI(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException;

  /**
   * Creates a new media package element of the specified type.
   *
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @return the new media package element or <code>null</code> if elements of this type cannot be created
   */
  MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

}
