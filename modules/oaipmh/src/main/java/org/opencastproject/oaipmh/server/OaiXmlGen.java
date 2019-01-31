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
package org.opencastproject.oaipmh.server;

import static org.opencastproject.oaipmh.OaiPmhConstants.OAI_2_0_SCHEMA_LOCATION;
import static org.opencastproject.oaipmh.OaiPmhConstants.OAI_2_0_XML_NS;
import static org.opencastproject.oaipmh.OaiPmhUtil.toUtcSecond;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;

/**
 * OAI-PMH specific XML generator.
 */
public abstract class OaiXmlGen extends XmlGen {

  protected OaiPmhRepository repository;

  /**
   * Create a new OaiXmlGen for a certain repository.
   */
  public OaiXmlGen(OaiPmhRepository repository) {
    super(some(OaiPmhConstants.OAI_2_0_XML_NS));
    this.repository = repository;
  }

  /**
   * Create the OAI-PMH tag.
   */
  Element oai(Node... nodes) {
    final List<Node> combined = new ArrayList<>();
    combined.add(schemaLocation(OAI_2_0_SCHEMA_LOCATION));
    combined.add($eTxt("responseDate", OAI_2_0_XML_NS, toUtcSecond(new Date())));
    Collections.addAll(combined, nodes);
    return $e("OAI-PMH",
              Collections.singletonList(ns("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)),
              combined);
  }

  /**
   * Create the dublin core tag from single nodes.
   */
  Element dc(Node... nodes) {
    List<Node> combined = new ArrayList<Node>(Arrays.asList(nodes));
    combined.add(schemaLocation(OaiPmhConstants.OAI_DC_SCHEMA_LOCATION));
    return $e("oai_dc:dc", OaiPmhConstants.OAI_DC_XML_NS,
              Arrays.asList(ns("dc", "http://purl.org/dc/elements/1.1/"),
                   ns("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)),
              combined);
  }

  /**
   * Create the dublin core tag from a search result item. Note: Sets are currently not supported.
   */
  @SuppressWarnings("unchecked") Element dc(final SearchResultItem item, Option<String> set) {
    try {
      return getDublincoreElement(item.getEpisodeDublinCore());
    } catch (OaiPmhDatabaseException ex) {
      return dc($e("dc:identifier", $txtBlank(item.getId())));
    }
  }

  // <dcterms:description xml:lang="en">
  // Introduction lecture from the Institute for Atmospheric and Climate Science.
  // </dcterms:description>
  private Element getDublincoreElement(DublinCoreCatalog dc) {
    List<Node> nodes = new ArrayList<Node>();
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_TITLE));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_CREATOR));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_SUBJECT));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_DESCRIPTION));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_PUBLISHER));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_CONTRIBUTOR));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_TYPE));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_FORMAT));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_IDENTIFIER));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_SOURCE));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_LANGUAGE));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_RELATION));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_COVERAGE));
    nodes.addAll(getDublinCoreNodes(dc, DublinCore.PROPERTY_LICENSE));
    return dc(nodes.toArray(new Node[nodes.size()]));
  }

  private List<Node> getDublinCoreNodes(DublinCoreCatalog dc, EName eName) {
    List<Node> nodes = new ArrayList<Node>();

    List<DublinCoreValue> values = dc.get(eName);
    for (DublinCoreValue dcValue : values) {
      Element element = $e("dc:" + eName.getLocalName(), $langNode(dcValue.getLanguage()), $txt(dcValue.getValue()));
      nodes.add(element);
    }
    return nodes;
  }

  /**
   * Create the resumption token and store the query.
   */
  Node resumptionToken(final Option<String> resumptionToken, final String metadataPrefix, final SearchResult result,
                       Date until, Option<String> set) {
    // compute the token value...
    final Option<Option<String>> token;
    if (result.size() == result.getLimit()) {
      SearchResultItem lastResult = result.getItems().get((int) (result.size() - 1));
      // more to come...
      token = some(some(repository.saveQuery(new ResumableQuery(metadataPrefix, lastResult.getModificationDate(),
                                                                until, set))));
    } else if (resumptionToken.isSome()) {
      // last page reached
      token = some(Option.<String>none());
    } else {
      token = none();
    }
    // ... then transform it into a node
    return token.map(new Function<Option<String>, Node>() {
      @Override
      public Node apply(Option<String> token) {
        return $e("resumptionToken",
                  // $a("completeListSize", Long.toString(result.getTotalSize())),
                  // $a("cursor", Integer.toString(offset)),
                  token.map(mkText).getOrElse(nodeZero));
      }
    }).getOrElse(nodeZero);
  }

  /**
   * Create a record element.
   */
  Element record(final SearchResultItem item, final Node metadata) {
    if (item.isDeleted()) {
      return $e("record", header(item));
    } else {
      return $e("record", header(item), $e("metadata", metadata));
    }
  }

  /**
   * Create a metadata format element.
   */
  Element metadataFormat(MetadataFormat f) {
    return $e("metadataFormat", $eTxt("metadataPrefix", f.getPrefix()), $eTxt("schema", f.getSchema().toString()),
              $eTxt("metadataNamespace", f.getNamespace().toString()));
  }

  /**
   * Create a metadata prefix attribute if one is requested in the params.
   */
  Node metadataPrefixAttr(Params p) {
    return $aSome("metadataPrefix", p.getMetadataPrefix());
  }

  /**
   * Create the header element for a result item.
   */
  Element header(final SearchResultItem item) {
    // todo output setSpec
    // How to determine the media type?
    // There is a field oc_mediatype in the index but this one distinguishes
    // only audioVisual and series.
    if (item.isDeleted()) {
      return $e("header", $a("status", "deleted"), $eTxt("identifier", item.getId()),
                $eTxt("datestamp", repository.toSupportedGranularity.apply(item.getModificationDate())));
    } else {
      return $e("header", $eTxt("identifier", item.getId()),
                $eTxt("datestamp", repository.toSupportedGranularity(item.getModificationDate())));
    }
  }

  /**
   * Merge two node arrays into a list.
   */
  protected List<Node> merge(Node[] a, Node... b) {
    List<Node> merge = new ArrayList<Node>(a.length + b.length);
    java.util.Collections.addAll(merge, a);
    java.util.Collections.addAll(merge, b);
    return merge;
  }
}
