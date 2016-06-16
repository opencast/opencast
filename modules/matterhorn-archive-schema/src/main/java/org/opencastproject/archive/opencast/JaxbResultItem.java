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

package org.opencastproject.archive.opencast;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.schema.JaxbOcDublinCore;
import org.opencastproject.schema.OcDublinCoreUtil;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Function;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class models an item in the search result. It represents a 'video' or 'series' object. It does not, however,
 * represent the complete solr document. Authorization information, for instance, is not serialized.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "result", namespace = "http://archive.opencastproject.org")
@XmlRootElement(name = "result", namespace = "http://archive.opencastproject.org")
public class JaxbResultItem {
  /** Media identificator. * */
  @XmlID
  @XmlAttribute(name = "id")
  protected String id = "";

  @XmlAttribute(name = "org")
  protected String organization;

  @XmlElement
  protected String seriesId;

  /** The media package */
  @XmlElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
  protected MediaPackage mediaPackage;

  @XmlElement(name = "metadata", namespace = DublinCores.OC_DC_CATALOG_NS_URI)
  protected JaxbOcDublinCore dublinCore;

  @XmlElement(name = "metadata", namespace = DublinCores.OC_DC_CATALOG_NS_URI)
  protected JaxbOcDublinCore seriesDublinCore;

  /** Field oc_acl */
  @XmlElement(name = "acl", namespace = "http://org.opencastproject.security")
  protected AccessControlList acl;

  /** The version in the archive */
  @XmlElement
  protected long version = -1;

  /** Is latest version archive field */
  @XmlElement
  protected boolean latestVersion = false;

  /** No-arg constructor needed by JAXB. */
  public JaxbResultItem() {
  }

  public static final Function<OpencastResultItem, JaxbResultItem> create = new Function<OpencastResultItem, JaxbResultItem>() {
    @Override
    public JaxbResultItem apply(OpencastResultItem source) {
      return create(source);
    }
  };

  public static JaxbResultItem create(final OpencastResultItem source) {
    final JaxbResultItem target = new JaxbResultItem();
    target.id = source.getMediaPackageId();
    target.mediaPackage = source.getMediaPackage();
    target.acl = source.getAcl();
    target.version = source.getVersion().value();
    target.latestVersion = source.isLatestVersion();
    target.organization = source.getOrganizationId();
    target.dublinCore = OcDublinCoreUtil.toJaxb(source.getDublinCore());
    if (source.getSeriesDublinCore().isSome()) {
      target.seriesDublinCore = OcDublinCoreUtil.toJaxb(source.getSeriesDublinCore().get());
    } else {
      target.seriesDublinCore = null;
    }
    target.seriesId = source.getSeriesId().getOrElseNull();
    return target;
  }
}
