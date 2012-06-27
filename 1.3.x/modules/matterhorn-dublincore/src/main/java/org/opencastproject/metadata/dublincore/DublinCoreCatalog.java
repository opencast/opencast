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

package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.XMLCatalog;
import org.opencastproject.mediapackage.XMLCatalogImpl;
import org.opencastproject.metadata.api.MetadataCatalog;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The Dublin Core catalog encapsulates dublin core metadata. For a reference to this standard, see
 * <code>http://dublincore.org/</code>.
 */
@XmlJavaTypeAdapter(XMLCatalogImpl.Adapter.class)
public interface DublinCoreCatalog extends XMLCatalog, DublinCore, MetadataCatalog, Cloneable {
  /** A flavor that matches any dublin core element */
  MediaPackageElementFlavor ANY_DUBLINCORE = MediaPackageElementFlavor.parseFlavor("dublincore/*");
}
