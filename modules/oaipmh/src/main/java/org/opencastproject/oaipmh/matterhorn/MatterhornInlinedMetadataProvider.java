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
package org.opencastproject.oaipmh.matterhorn;

import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.UrlSupport.url;

import org.opencastproject.oaipmh.persistence.SearchResultElementItem;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.oaipmh.server.MetadataFormat;
import org.opencastproject.oaipmh.server.MetadataProvider;
import org.opencastproject.oaipmh.server.OaiPmhRepository;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * The matterhorn-inlined metadata provider provides whole media packages, series and episode DublinCores and series ACLs.
 */
public class MatterhornInlinedMetadataProvider implements MetadataProvider {
  private static final URL SCHEMA_URL = url("http://www.opencastproject.org/oai/matterhorn-inlined.xsd");
  private static final URI NAMESPACE_URI = uri("http://www.opencastproject.org/oai/matterhorn-inlined");

  private static final MetadataFormat METADATA_FORMAT = new MetadataFormat() {
    @Override
    public String getPrefix() {
      return "matterhorn-inlined";
    }

    @Override
    public URL getSchema() {
      return SCHEMA_URL;
    }

    @Override
    public URI getNamespace() {
      return NAMESPACE_URI;
    }
  };

  private static final Option<String> NS_URI = Option.some(NAMESPACE_URI.toString());

  @Override
  public MetadataFormat getMetadataFormat() {
    return METADATA_FORMAT;
  }

  @Override
  public Element createMetadata(OaiPmhRepository repository, final SearchResultItem item, Option<String> set) {
    XmlGen xml = new XmlGen(Option.<String>none()) {
      @Override
      public Element create() {
        List<Node> inlinedNodes = Collections.list(parse(Option.option(item.getMediaPackageXml())));
        for (SearchResultElementItem elementItem : item.getElements()) {
          inlinedNodes.add($e(elementItem.getType(), NS_URI,
                  $a("type", elementItem.getFlavor()),
                  parse(Option.option(elementItem.getXml()))));
        }
        return $e("inlined", NS_URI, inlinedNodes);
      }

      private Node parse(Option<String> xml) {
        for (final String a : xml) {
          for (final Document d : XmlUtil.parseNs(a).right()) {
            return d.getDocumentElement();
          }
        }
        return nodeZero();
      }
    };
    return xml.create();
  }

}
