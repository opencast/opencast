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

package org.opencastproject.mediapackage.track;

import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.SubtitleStream;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.mediapackage.SubtitleStream}.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "subtitle", namespace = "http://mediapackage.opencastproject.org")
public class SubtitleStreamImpl extends AbstractStreamImpl implements SubtitleStream {
  public SubtitleStreamImpl() {
    this(UUID.randomUUID().toString());
  }

  public SubtitleStreamImpl(String identifier) {
    super(identifier);
  }

  /**
   * Create a subtitle stream from the XML manifest.
   *
   * @param streamIdHint
   *          stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
   *          manifest.
   */
  public static SubtitleStreamImpl fromManifest(String streamIdHint, Node node, XPath xpath)
      throws IllegalStateException, XPathException {
    // Create stream
    String sid = (String) xpath.evaluate("@id", node, XPathConstants.STRING);
    if (StringUtils.isEmpty(sid)) {
      sid = streamIdHint;
    }
    SubtitleStreamImpl ss = new SubtitleStreamImpl(sid);
    partialFromManifest(ss, node, xpath);

    return ss;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement("subtitle");
    addCommonManifestElements(node, document, serializer);
    return node;
  }
}
