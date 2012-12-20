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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItemImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.data.NonEmptyList;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class OaiPmhRepositoryTest {

  private static final long RESULT_LIMIT = 3;

  @Test
  public void testVerbListSets() {
    OaiPmhRepository repo = newRepo(null);
    Document doc = repo.selectVerb(newParams("ListSets", null, null, null, null, null)).generate();
    assertXpathExists(doc, "//ListSets/set/setSpec");
    assertXpathExists(doc, "//ListSets/set/setName");
    assertXpathExists(doc, "//ListSets/set/setDescription/*/*/text()");
    assertXpathExists(doc, "//ListSets/set[setSpec=\"series\"]");
    assertXpathExists(doc, "//ListSets/set[setSpec=\"episode\"]");
    assertXpathExists(doc, "//ListSets/set[setSpec=\"episode:audio\"]");
    assertXpathExists(doc, "//ListSets/set[setSpec=\"episode:video\"]");
  }

  @Test
  public void testVerbListIdentifiersBadArgument() {
    OaiPmhRepository repo = newRepo(null);
    Document doc = repo.selectVerb(newParams("ListIdentifiers", null, null, null, null, null)).generate();
    assertXpathExists(doc, "//error[@code=\"badArgument\"]");
  }

  @Test
  public void testVerbListIdentifiersAll() {
    OaiPmhRepository repo = newRepo(newSearchServiceMock(newSearchResultItem("id-1", new Date(), new Date()),
            newSearchResultItem("id-2", newDate(2011, 5, 30), newDate(2011, 6, 1))));
    Document doc = repo.selectVerb(newParams("ListIdentifiers", null, "oai_dc", null, null, null)).generate();
    assertXpathExists(doc, "//ListIdentifiers/header[identifier=\"id-1\"]");
    assertXpathExists(doc, "//ListIdentifiers/header[identifier=\"id-2\"]");
    assertXpathExists(doc, "//ListIdentifiers/header[datestamp=\"2011-06-01\"]");
    assertEquals(2.0, xpath(doc, "count(//ListIdentifiers/header)", XPathConstants.NUMBER));
    EasyMock.verify(repo.getSearchService());
  }

  /**
   * Date range queries are just checked for the error case, since it doesn't make much sense to test with a mocked
   * search service.
   */
  @Test
  public void testVerbListIdentifiersDateRangeError() {
    OaiPmhRepository repo = newRepo(null);
    Document doc1 = repo.selectVerb(newParams("ListIdentifiers", null, "oai_dc", "2011-01-02", "2011-01-01", null))
            .generate();
    assertXpathExists(doc1, "//error[@code=\"badArgument\"]");
    Document doc2 = repo.selectVerb(
            newParams("ListIdentifiers", null, "oai_dc", "2011-01-01T10:20:10Z", "2011-01-01T10:20:00Z", null))
            .generate();
    assertXpathExists(doc2, "//error[@code=\"badArgument\"]");
  }

  @Test
  public void testVerbListRecordsAll() {
    OaiPmhRepository repo = newRepo(newSearchServiceMock(newSearchResultItem("id-1", new Date(), new Date()),
            newSearchResultItem("id-2", newDate(2011, 5, 30), newDate(2011, 6, 1))));
    Document doc = repo.selectVerb(newParams("ListRecords", null, "oai_dc", null, null, null)).generate();
    assertXpathExists(doc, "//ListRecords/record/header[identifier=\"id-1\"]");
    assertXpathExists(doc, "//ListRecords/record/header[identifier=\"id-2\"]");
    assertXpathExists(doc, "//ListRecords/record/header[datestamp=\"2011-06-01\"]");
    assertEquals(2.0, xpath(doc, "count(//ListRecords/record)", XPathConstants.NUMBER));
    assertEquals(2.0, xpath(doc, "count(//ListRecords/record/metadata)", XPathConstants.NUMBER));
    EasyMock.verify(repo.getSearchService());
  }

  @Test
  public void testResumption() {
    SearchResultItem[] items1 = new SearchResultItem[] {
            newSearchResultItem("id-1", newDate(2011, 5, 10), newDate(2011, 5, 10)),
            newSearchResultItem("id-2", newDate(2011, 5, 11), newDate(2011, 5, 11)),
            newSearchResultItem("id-3", newDate(2011, 5, 12), newDate(2011, 5, 12)) };
    SearchResultItem[] items2 = new SearchResultItem[] {
            newSearchResultItem("id-4", newDate(2011, 5, 13), newDate(2011, 5, 13)),
            newSearchResultItem("id-5", newDate(2011, 5, 14), newDate(2011, 5, 14)) };
    // setup search service mock
    // this setup is really ugly since it needs knowledge about implementation details
    SearchService search = EasyMock.createMock(SearchService.class);
    SearchResult result = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(search.getByQuery(EasyMock.<SearchQuery> anyObject())).andReturn(result).anyTimes();
    EasyMock.expect(search.getByQuery(EasyMock.<String> anyObject(), EasyMock.anyInt(), EasyMock.anyInt()))
            .andReturn(result).anyTimes();
    EasyMock.expect(result.getItems()).andReturn(items1).andReturn(items2);
    EasyMock.expect(result.getQuery()).andReturn("").anyTimes();
    EasyMock.expect(result.getLimit()).andReturn(RESULT_LIMIT).anyTimes();
    EasyMock.expect(result.getOffset()).andReturn(0L).times(2).andReturn(RESULT_LIMIT).anyTimes();
    EasyMock.expect(result.size()).andReturn((long) items1.length).times(2).andReturn((long) items2.length).times(2);
    EasyMock.expect(result.getTotalSize()).andReturn((long) items1.length + items2.length).anyTimes();
    EasyMock.replay(search);
    EasyMock.replay(result);
    // do testing
    OaiPmhRepository repo = newRepo(search);
    Document doc1 = repo.selectVerb(newParams("ListIdentifiers", null, "oai_dc", null, null, null)).generate();
    assertEquals(3.0, xpath(doc1, "count(//ListIdentifiers/header)", XPathConstants.NUMBER));
    assertXpathEquals(doc1, "r-token", "//ListIdentifiers/resumptionToken/text()");
    assertXpathExists(doc1, "//ListIdentifiers/resumptionToken[@cursor=0]");
    assertXpathExists(doc1, "//ListIdentifiers/resumptionToken[@completeListSize=" + (items1.length + items2.length)
            + "]");
    assertXpathEquals(doc1, "id-1", "//ListIdentifiers/header[1]/identifier/text()");
    assertXpathEquals(doc1, "id-2", "//ListIdentifiers/header[2]/identifier/text()");
    assertXpathEquals(doc1, "id-3", "//ListIdentifiers/header[3]/identifier/text()");
    // resume query
    Document doc2 = repo.selectVerb(newParams("ListIdentifiers", null, null, null, null, "r-token")).generate();
    assertEquals(2.0, xpath(doc2, "count(//ListIdentifiers/header)", XPathConstants.NUMBER));
    assertXpathEquals(doc2, "id-4", "//ListIdentifiers/header[1]/identifier/text()");
    assertXpathEquals(doc2, "id-5", "//ListIdentifiers/header[2]/identifier/text()");
    // token must be empty now since there are no more pages
    assertXpathEquals(doc2, "", "//ListIdentifiers/resumptionToken/text()");
    assertXpathExists(doc2, "//ListIdentifiers/resumptionToken[@cursor=" + RESULT_LIMIT + "]");
    EasyMock.verify(repo.getSearchService());
  }

  // --

  private static Object xpath(Document document, String path, QName returnType) {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
      return xPath.compile(path).evaluate(document, returnType);
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertXpathExists(Document doc, String path) {
    try {
      NodeList nodes = (NodeList) xpath(doc, path, XPathConstants.NODESET);
      assertTrue("No nodes match", nodes.getLength() > 0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertXpathEquals(Document doc, String expected, String path) {
    try {
      assertEquals(expected, ((String) xpath(doc, path, XPathConstants.STRING)).trim());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Params newParams(final String verb,
          final String identifier,
          final String metadataPrefix,
          final String from,
          final String until,
          final String resumptionToken) {
    return new Params() {
      @Override
      String getParameter(String key) {
        if ("verb".equals(key))
          return verb;
        if ("identifier".equals(key))
          return identifier;
        if ("metadataPrefix".equals(key))
          return metadataPrefix;
        if ("from".equals(key))
          return from;
        if ("until".equals(key))
          return until;
        if ("resumptionToken".equals(key))
          return resumptionToken;
        return null;
      }
    };
  }

  private static Date newDate(int year, int month, int day) {
    Calendar c = Calendar.getInstance();
    c.setTimeZone(TimeZone.getTimeZone("UTC"));
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static SearchService newSearchServiceMock(SearchResultItem... items) {
    SearchService search = EasyMock.createNiceMock(SearchService.class);
    SearchResult result = EasyMock.createNiceMock(SearchResult.class);
    EasyMock.expect(search.getByQuery(EasyMock.<SearchQuery> anyObject())).andReturn(result).anyTimes();
    EasyMock.expect(search.getByQuery(EasyMock.<String> anyObject(), EasyMock.anyInt(), EasyMock.anyInt()))
            .andReturn(result).anyTimes();
    EasyMock.expect(result.getItems()).andReturn(items);
    EasyMock.expect(result.size()).andReturn((long) items.length);
    EasyMock.replay(search);
    EasyMock.replay(result);
    return search;
  }

  private static SearchResultItemImpl newSearchResultItem(String id, Date created, Date modified) {
    SearchResultItemImpl item = new SearchResultItemImpl();
    item.setDcCreated(created);
    item.setModified(modified);
    item.setId(id);
    return item;
  }

  private static OaiPmhRepository newRepo(final SearchService searchService) {
    return new OaiPmhRepository() {
      @Override
      public Granularity getRepositoryTimeGranularity() {
        return Granularity.DAY;
      }

      @Override
      public String getBaseUrl() {
        return "http://localhost.org/oaipmh";
      }

      @Override
      public String getRepositoryName() {
        return "Test OAI Repository";
      }

      @Override
      public SearchService getSearchService() {
        return searchService;
      }

      @Override
      public String getAdminEmail() {
        return "admin@localhost.org";
      }

      @Override
      public String saveQuery(ResumableQuery query) {
        return "r-token";
      }

      @Override
      public Option<ResumableQuery> getSavedQuery(String resumptionToken) {
        return Option.some(new ResumableQuery("", "oai_dc", 0, (int) RESULT_LIMIT));
      }

      @Override
      public int getResultLimit() {
        return (int) RESULT_LIMIT;
      }

      @Override
      public List<MetadataProvider> getMetadataProviders() {
        return new NonEmptyList<MetadataProvider>(new OaiDcMetadataProvider());
      }
    };
  }
}
