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

package org.opencastproject.oaipmh.harvester;

import org.opencastproject.util.data.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * The "ListRecords" response.
 * See <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ListRecords">4.5 ListRecords</a> for further
 * information.
 * <p/>
 * todo implement missing element accessors
 */
public class ListRecordsResponse extends OaiPmhResponse {

  public ListRecordsResponse(Document doc) {
    super(doc);
  }

  /**
   * Get the content of all metadata elements in the current response.
   */
  public NodeList getMetadataElems() {
    return xpathNodeList("/oai20:OAI-PMH/oai20:ListRecords/oai20:record/oai20:metadata/*[1]");
  }

  /**
   * Perform complete request resuming any partial responses.
   */
  public static Iterable<Node> getAllMetadataElems(final ListRecordsResponse response, final OaiPmhRepositoryClient client) {
    class MetadataIterator implements Iterator<Node> {

      private NodeList metadataElems;
      private Option<String> token;
      private String metadataPrefix;
      private int i;

      MetadataIterator(ListRecordsResponse response) {
        init(response);
      }

      void init(ListRecordsResponse response) {
        metadataElems = response.getMetadataElems();
        token = response.getResumptionToken();
        metadataPrefix = response.getMetadataPrefix();
        i = 0;
      }

      @Override
      public boolean hasNext() {
        return hasNextInCurrent() || token.isSome();
      }

      @Override
      public Node next() {
        if (!hasNext())
          throw new NoSuchElementException();
        if (!hasNextInCurrent()) {
          // get next document
          init(client.resumeListRecords(metadataPrefix, token.get()));
        }
        return metadataElems.item(i++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private boolean hasNextInCurrent() {
        return i < metadataElems.getLength();
      }
    }

    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new MetadataIterator(response);
      }
    };
  }

  public String getMetadataPrefix() {
    return xpathString("/oai20:OAI-PMH/oai20:request/@metadataPrefix");
  }

  public Option<String> getResumptionToken() {
    return Option.wrap(trimToNull(xpathString("/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken/text()")));
  }
}