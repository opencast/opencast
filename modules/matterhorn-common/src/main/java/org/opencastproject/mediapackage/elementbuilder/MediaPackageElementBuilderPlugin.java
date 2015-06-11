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

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.UnsupportedElementException;

import org.w3c.dom.Node;

import java.net.URI;

/**
 * An element builder plugin is an object that is able to recognize one ore more filetypes slated for ingest into
 * matterhorn.
 * <p/>
 * <strong>Implementation note:</strong> Builder plugins may be stateful. They are intended to be used as throw-away
 * objects.
 */
public interface MediaPackageElementBuilderPlugin {

  /**
   * This method is called once in a plugin's life cycle. When this method is called, the plugin can make sure that
   * everything is in place for it to work properly. If this isn't the case, it should throw an exception so it will no
   * longer be bothered by the element builder.
   *
   * @throws Exception
   *           if some unrecoverable state is reached
   */
  void init() throws Exception;

  /**
   * This method is called before the plugin is abandoned by the element builder.
   */
  void destroy();

  /**
   * This method is called if the media package builder tries to create a new media package element of type
   * <code>elementType</code>.
   * <p>
   * Every registered builder plugin will then be asked whether it is able to create a media package element from the
   * given element type. If this is the case for a plugin, it will then be asked to create such an element by a call to
   * {@link #newElement(org.opencastproject.mediapackage.MediaPackageElement.Type ,MediaPackageElementFlavor)}.
   * </p>
   *
   * @param type
   *          the type
   * @param flavor
   *          the element flavor
   * @return <code>true</code> if the plugin is able to create such an element
   */
  boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

  /**
   * This method is called on every registered media package builder plugin until one of these plugins returns
   * <code>true</code>. If no plugin recognises the file, it is rejected.
   * <p>
   * The parameters <code>type</code> and <code>flavor</code> may be taken as strong hints and may both be
   * <code>null</code>.
   * </p>
   * <p>
   * Implementers schould return the correct mime type for the given file if they are absolutely sure about the file.
   * Otherwise, <code>null</code> should be returned.
   * </p>
   *
   * @param uri
   *          the element location
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @return <code>true</code> if the plugin can handle the element
   */
  boolean accept(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

  /**
   * This method is called while the media package builder parses a media package manifest.
   * <p>
   * Every registered builder plugin will then be asked, whether it is able to create a media package element from the
   * given element definition.
   * </p>
   * <p>
   * The element must then be constructed and returned in the call to
   * {@link #elementFromManifest(Node, MediaPackageSerializer)}.
   * </p>
   *
   * @param elementNode
   *          the node
   * @return <code>true</code> if the plugin is able to create such an element
   */
  boolean accept(Node elementNode);

  /**
   * Creates a media package element from the given url that was previously accepted.
   *
   * @param uri
   *          the element location
   * @return the new media package element
   * @throws UnsupportedElementException
   *           if creating the media package element fails
   */
  MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException;

  /**
   * Creates a media package element from the DOM element.
   *
   * @param elementNode
   *          the DOM node
   * @param serializer
   *          the media package serializer
   * @return the media package element
   * @throws UnsupportedElementException
   */
  MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws UnsupportedElementException;

  /**
   * Creates a new media package element of the specified type.
   *
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @return the new media package element
   */
  MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

}
