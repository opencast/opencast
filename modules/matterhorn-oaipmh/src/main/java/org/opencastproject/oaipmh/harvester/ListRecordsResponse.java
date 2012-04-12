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
import static org.opencastproject.util.data.Option.option;

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
   * Get all records in the current response.
   * <pre>
   *  &lt;record&gt;
   *    &lt;header&gt;...
   *    &lt;/header&gt;
   *    &lt;metadata&gt;...
   *    &lt;/metadata&gt;
   *  &lt;/record&gt;
   * </pre>
   */
  public NodeList getRecords() {
    return xpathNodeList("/oai20:OAI-PMH/oai20:ListRecords/oai20:record");
  }

  /**
   * Extract the content, i.e. the first child node, of the metadata node of a record.
   * <pre>
   *  &lt;record&gt;
   *    &lt;header&gt;...
   *    &lt;/header&gt;
   *    &lt;metadata&gt;
   *      &lt;myMd&gt;
   *      &lt;/myMd&gt;
   *    &lt;/metadata&gt;
   *  &lt;/record&gt;
   *
   *  =&gt;
   *
   *  &lt;myMd&gt;
   *  &lt;/myMd&gt;
   * </pre>
   */
  public static Node metadataOfRecord(Node recordNode) {
    return xpathNode(createXPath(), recordNode, "oai20:metadata/*[1]");
  }

  /**
   * Get all records performing a complete request resuming any partial responses.
   */
  public static Iterable<Node> getAllRecords(final ListRecordsResponse first, final OaiPmhRepositoryClient client) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new ResponseIterator(first) {
          @Override
          protected OaiPmhRepositoryClient getClient() {
            return client;
          }

          @Override
          protected NodeList extractNodes(ListRecordsResponse response) {
            return response.getRecords();
          }
        };
      }
    };
  }

  /**
   * Get all metadata performing a complete request resuming any partial responses.
   */
  public static Iterable<Node> getAllMetadataElems(final ListRecordsResponse first, final OaiPmhRepositoryClient client) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new ResponseIterator(first) {
          @Override
          protected OaiPmhRepositoryClient getClient() {
            return client;
          }

          @Override
          protected NodeList extractNodes(ListRecordsResponse response) {
            return response.getMetadataElems();
          }
        };
      }
    };
  }

  public String getMetadataPrefix() {
    return xpathString("/oai20:OAI-PMH/oai20:request/@metadataPrefix");
  }

  public Option<String> getResumptionToken() {
    return option(trimToNull(xpathString("/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken/text()")));
  }

  //

  private abstract static class ResponseIterator implements Iterator<Node> {

    private NodeList elems;
      private Option<String> token;
      private String metadataPrefix;
      private int i;

    ResponseIterator(ListRecordsResponse response) {
      initIteration(response);
      }

    private void initIteration(ListRecordsResponse response) {
      elems = extractNodes(response);
        token = response.getResumptionToken();
        metadataPrefix = response.getMetadataPrefix();
        i = 0;
      }

    protected abstract OaiPmhRepositoryClient getClient();

    /**
     * Extract the relevant part of the response.
     */
    protected abstract NodeList extractNodes(ListRecordsResponse response);

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
        initIteration(getClient().resumeListRecords(metadataPrefix, token.get()));
        }
      return elems.item(i++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private boolean hasNextInCurrent() {
      return i < elems.getLength();
  }
  }
}