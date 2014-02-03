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

package org.opencastproject.oaipmh.server;

import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.opencastproject.oaipmh.OaiPmhUtil.toUtcSecond;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

/**
 * OAI-PMH specific XML generator.
 */
public abstract class OaiXmlGen extends XmlGen {

  protected OaiPmhRepository repository;

  /**
   * Create a new OaiXmlGen for a certain repository.
   */
  public OaiXmlGen(OaiPmhRepository repository) {
    this.repository = repository;
  }

  /**
   * Create the OAI-PMH tag.
   */
  Element oai(Node... nodes) {
    List<Node> combined = Collections.concat(
            _(
                    schemaLocation(OaiPmhConstants.OAI_2_0_SCHEMA_LOCATION),
                    $eTxt("responseDate", toUtcSecond(new Date()))
            ),
            Arrays.asList(nodes));
    return $e("OAI-PMH",
        OaiPmhConstants.OAI_2_0_XML_NS,
        _(
            ns("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
        ),
        combined);
  }

  /**
   * Create the dublin core tag from single nodes.
   */
  Element dc(Node... nodes) {
    List<Node> combined = Collections.concat(
            _(schemaLocation(OaiPmhConstants.OAI_DC_SCHEMA_LOCATION)),
            _(nodes));
    return $e("oai_dc:dc",
        OaiPmhConstants.OAI_DC_XML_NS,
        _(
            ns("dc", "http://purl.org/dc/elements/1.1/"),
            ns("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
        ),
        combined);
  }

  /**
   * Create the dublin core tag from a search result item.
   */
  Element dc(SearchResultItem item) {
    return dc(
        $e("dc:title", $txtBlank(item.getDcTitle())),
        $e("dc:creator", $txtBlank(item.getDcCreator())),
        $e("dc:subject", $txtBlank(item.getDcSubject())),
        $e("dc:description", $txtBlank(item.getDcDescription())),
        $e("dc:publisher", $txtBlank(item.getDcPublisher())),
        $e("dc:contributor", $txtBlank(item.getDcContributor())),
        $e("dc:date", $txtBlank(repository.toSupportedGranularity(item.getDcCreated()))),
        $e("dc:type", $txtBlank(item.getDcType())),
        $e("dc:identifier", $txtBlank(item.getId())),
        $e("dc:language", $txtBlank(item.getDcLanguage())),
        $e("dc:rights", $txtBlank(item.getDcLicense()))
    );
  }

  /**
   * Create the resumption token and store the query.
   */
  Node resumptionToken(final Option<String> resumptionToken, final String metadataPrefix, final SearchResult result) {
    final int offset;
    if (result.getOffset() <= Integer.MAX_VALUE) {
      offset = (int) result.getOffset();
    } else {
      throw new RuntimeException("offset too big");
    }
    // compute the token value...
    final Option<Option<String>> token;
    if (offset + result.size() < result.getTotalSize()) {
      // more to come...
      token = some(some(repository.saveQuery(new ResumableQuery(result.getQuery(),
          metadataPrefix,
          offset,
          repository.getResultLimit()))));
    } else if (resumptionToken.isSome()) {
      // last page reached
      token = some(Option.<String>none());
    } else {
      token = none();
    }
    // ... then transform it into a node
    return token
        .map(new Function<Option<String>, Node>() {
          @Override
          public Node apply(Option<String> token) {
            return $e("resumptionToken",
                $a("completeListSize", Long.toString(result.getTotalSize())),
                $a("cursor", Integer.toString(offset)),
                token.map(mkText).getOrElse(nodeZero));
          }
        })
        .getOrElse(nodeZero);
  }

  /**
   * Create a record element.
   */
  Element record(SearchResultItem item, Node metadata) {
    return $e("record",
        header(item),
        $e("metadata",
            metadata));
  }

  /**
   * Create a metadata format element.
   */
  Element metadataFormat(MetadataFormat f) {
    return $e("metadataFormat",
        $eTxt("metadataPrefix", f.getPrefix()),
        $eTxt("schema", f.getSchema().toString()),
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
  Element header(SearchResultItem item) {
    return $e("header",
        $eTxt("identifier", item.getId()),
        $eTxt("datestamp",
            option(item.getModified()).map(repository.toSupportedGranularity).getOrElse(Functions.defaultValue("", "created"))
            // todo output setSpec and deleted status
            // How to determine the media type?
            // There is a field oc_mediatype in the index but this one distinguishes
            // only audioVisual and series.
        ));
  }

  /**
   * Merge two node arrays into a list.
   */
  protected List<Node> merge(Node[] a, Node... b) {
    return Collections.concat(_(a), _(b));
  }
}
