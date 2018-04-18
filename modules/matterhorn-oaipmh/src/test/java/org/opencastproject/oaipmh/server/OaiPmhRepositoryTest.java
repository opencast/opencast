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

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.oaipmh.server.OaiPmhRepositoryTest.OaiPmhResponseStatus.IsError;
import static org.opencastproject.oaipmh.server.OaiPmhRepositoryTest.OaiPmhResponseStatus.IsValid;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.xmlmatchers.transform.XmlConverters.the;
import static org.xmlmatchers.xpath.HasXPath.hasXPath;
import static org.xmlmatchers.xpath.XpathReturnType.returningANumber;
import static org.xmlmatchers.xpath.XpathReturnType.returningAString;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.harvester.OaiPmhNamespaceContext;
import org.opencastproject.oaipmh.matterhorn.MatterhornInlinedMetadataProvider;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.util.HttpUtil;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.JsonVal;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.easymock.EasyMock;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;

public class OaiPmhRepositoryTest {
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhRepositoryTest.class);
  private static final NamespaceContext NS_CTX = OaiPmhNamespaceContext.getContext();
  private static final long RESULT_LIMIT = 3;

  private static final boolean DISABLE_VALIDATION = true;
  private static boolean runValidation = false;

  // CHECKSTYLE:OFF
  @Rule
  public Timeout globalTimeout = new Timeout(5000);
  // CHECKSTYLE:ON

  @BeforeClass
  public static void checkHttpConnection() {
    final CloseableHttpClient client = HttpClients.createDefault();
    try {
      runValidation = !DISABLE_VALIDATION && HttpUtil.isOk(client.execute(HttpUtil.get(VALIDATOR_SERVICE)));
      logger.info("Using external OAI-PMH validator service (" + VALIDATOR_SERVICE + "): " + runValidation);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IoSupport.closeQuietly(client);
    }
  }

  @Test
  public void testVerbIdentify() throws Exception {
    final OaiPmhRepository repo = repo(null, Granularity.DAY);
    runChecks(OaiPmhConstants.VERB_IDENTIFY,
              repo.selectVerb(params("Identify", null, null, null, null, null)),
              some(IsValid),
              list(hasXPath("//oai20:Identify[oai20:deletedRecord='transient']", NS_CTX)));
  }

  @Test
  public void testVerbListIdentifiersBadArgument() throws Exception {
    final OaiPmhRepository repo = repo(null, Granularity.DAY);
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo.selectVerb(params("ListIdentifiers", null, null, null, null, null)),
              some(IsError),
              list(hasXPath("//oai20:error[@code='badArgument']", NS_CTX)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVerbListIdentifiersAll() throws Exception {
    final OaiPmhRepository repo = repo(
            oaiPmhPersistenceMock(searchResultItem("id-1", new Date(), false),
                                  searchResultItem("id-2", utcDate(2011, 6, 1), false)), Granularity.DAY);
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo.selectVerb(params("ListIdentifiers", null, "oai_dc", null, null, null)),
              some(IsValid),
              list(hasXPath("//oai20:ListIdentifiers/oai20:header[oai20:identifier='id-1']", NS_CTX),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[oai20:identifier='id-2']", NS_CTX),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[oai20:datestamp='2011-06-01']", NS_CTX),
                   hasXPath("count(//oai20:ListIdentifiers/oai20:header)", NS_CTX, returningANumber(), equalTo(2.0))));
  }

  /**
   * Date range queries are just checked for the error case, since it doesn't make much sense to test with a mocked
   * episode service.
   */
  @Test
  public void testVerbListIdentifiersDateRangeError() throws Exception {
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo(null, Granularity.DAY)
                      .selectVerb(params("ListIdentifiers", null, "oai_dc", "2011-01-02", "2011-01-01", null)),
              some(IsError),
              list(hasXPath("//oai20:error[@code='badArgument']", NS_CTX)));
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo(null, Granularity.SECOND)
                      .selectVerb(params("ListIdentifiers", null, "oai_dc", "2011-01-01T10:20:10Z", "2011-01-01T10:20:00Z", null)),
              some(IsError),
              list(hasXPath("//oai20:error[@code='badArgument']", NS_CTX)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVerbListRecordsAll() throws Exception {
    runChecks(OaiPmhConstants.VERB_LIST_RECORDS,
              repo(oaiPmhPersistenceMock(searchResultItem("id-1", utcDate(2011, 5, 1), false),
                                         searchResultItem("id-2", utcDate(2011, 6, 1), true)), Granularity.DAY)
                      .selectVerb(params("ListRecords", null, "oai_dc", null, null, null)),
              some(IsValid),
              list(hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='id-1']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:datestamp='2011-05-01']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[@status='deleted']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='id-2']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:datestamp='2011-06-01']", NS_CTX),
                   hasXPath("count(//oai20:ListRecords/oai20:record)", NS_CTX, returningANumber(), equalTo(2.0)),
                   hasXPath("count(//oai20:ListRecords/oai20:record/oai20:metadata)", NS_CTX, returningANumber(), equalTo(1.0))));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVerbGetRecord() throws Exception {
    runChecks(OaiPmhConstants.VERB_GET_RECORD,
              repo(oaiPmhPersistenceMock(searchResultItem("id-1", utcDate(2011, 6, 1), false)),
                   Granularity.DAY)
                      .selectVerb(params("GetRecord", "id-1", "oai_dc", null, null, null)),
              Option.<OaiPmhResponseStatus>none(),
              list(hasXPath("//oai20:GetRecord/oai20:record/oai20:header[oai20:identifier='id-1']", NS_CTX),
                   hasXPath("//oai20:GetRecord/oai20:record/oai20:header[oai20:datestamp='2011-06-01']", NS_CTX),
                   hasXPath("//oai20:GetRecord/oai20:record/oai20:header[not(@status='deleted')]", NS_CTX),
                   hasXPath("count(//oai20:GetRecord/oai20:record)", NS_CTX, returningANumber(), equalTo(1.0)),
                   hasXPath("count(//oai20:GetRecord/oai20:record/oai20:metadata)", NS_CTX, returningANumber(), equalTo(1.0))));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVerbGetRecordDeleted() throws Exception {
    runChecks(OaiPmhConstants.VERB_GET_RECORD,
              repo(oaiPmhPersistenceMock(searchResultItem("id-1", utcDate(2011, 5, 1), true)),
                   Granularity.DAY)
                      .selectVerb(params("GetRecord", "id-1", "oai_dc", null, null, null)),
              Option.<OaiPmhResponseStatus>none(),
              list(hasXPath("//oai20:GetRecord/oai20:record/oai20:header[oai20:identifier='id-1']", NS_CTX),
                   hasXPath("//oai20:GetRecord/oai20:record/oai20:header[oai20:datestamp='2011-05-01']", NS_CTX),
                   hasXPath("//oai20:GetRecord/oai20:record/oai20:header[@status='deleted']", NS_CTX),
                   hasXPath("count(//oai20:GetRecord/oai20:record)", NS_CTX, returningANumber(), equalTo(1.0)),
                   hasXPath("count(//oai20:GetRecord/oai20:record/oai20:metadata)", NS_CTX, returningANumber(), equalTo(0.0))));
  }

  @Test
  public void testMatterhornInlinedMetadataProvider() throws Exception {
    runChecks(OaiPmhConstants.VERB_LIST_RECORDS,
              repo(oaiPmhPersistenceMock(searchResultItem("id-1", utcDate(2011, 5, 1), false),
                                         searchResultItem("id-2", utcDate(2011, 6, 1), true)), Granularity.DAY)
                      .selectVerb(params("ListRecords", null, "matterhorn-inlined", null, null, null)),
              some(IsValid),
              list(hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='id-1']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:datestamp='2011-05-01']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[@status='deleted']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='id-2']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:datestamp='2011-06-01']", NS_CTX),
                   hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:datestamp='2011-06-01']", NS_CTX),
                   hasXPath("count(//oai20:ListRecords/oai20:record)", NS_CTX, returningANumber(), equalTo(2.0)),
                   hasXPath("count(//oai20:ListRecords/oai20:record)", NS_CTX, returningANumber(), equalTo(2.0)),
                   hasXPath("count(//oai20:ListRecords/oai20:record/oai20:metadata)", NS_CTX, returningANumber(), equalTo(1.0))));
  }

  @Ignore
  @Test
  @SuppressWarnings("unchecked")
  public void testResumption() throws Exception {
    List<SearchResultItem> items1 = new ArrayList<SearchResultItem>();
    items1.add(searchResultItem("id-1", utcDate(2011, 5, 10), false));
    items1.add(searchResultItem("id-2", utcDate(2011, 5, 11), false));
    items1.add(searchResultItem("id-3", utcDate(2011, 5, 12), false));

    List<SearchResultItem> items2 = new ArrayList<SearchResultItem>();
    items2.add(searchResultItem("id-4", utcDate(2011, 5, 13), false));
    items2.add(searchResultItem("id-5", utcDate(2011, 5, 14), false));

    // setup episode service mock
    // this setup is really ugly since it needs knowledge about implementation details
    OaiPmhDatabase persistence = EasyMock.createMock(OaiPmhDatabase.class);
    SearchResult result = EasyMock.createMock(SearchResult.class);
    EasyMock.expect(result.getItems()).andReturn(items1).times(3).andReturn(items2).times(3);
    EasyMock.expect(result.getLimit()).andReturn(RESULT_LIMIT).anyTimes();
    EasyMock.expect(result.getOffset()).andReturn(0L).times(3).andReturn(RESULT_LIMIT).anyTimes();
    EasyMock.expect(result.size()).andReturn((long) items1.size()).times(4).andReturn((long) items2.size()).times(4);
    EasyMock.expect(persistence.search(EasyMock.<Query>anyObject())).andReturn(result).anyTimes();
    EasyMock.replay(persistence);
    EasyMock.replay(result);
    // do testing
    final OaiPmhRepository repo = repo(persistence, Granularity.DAY);
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo.selectVerb(params("ListIdentifiers", null, "oai_dc", null, null, null)),
              some(IsValid),
              list(hasXPath("count(//oai20:ListIdentifiers/oai20:header)", NS_CTX, returningANumber(), equalTo(3.0)),
                   hasXPath("//oai20:ListIdentifiers/oai20:resumptionToken/text()", NS_CTX, returningAString(), equalTo("r-token")),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[1]/oai20:identifier/text()", NS_CTX, returningAString(), equalTo("id-1")),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[2]/oai20:identifier/text()", NS_CTX, returningAString(), equalTo("id-2")),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[3]/oai20:identifier/text()", NS_CTX, returningAString(), equalTo("id-3"))));
    // resume query
    runChecks(OaiPmhConstants.VERB_LIST_IDENTIFIERS,
              repo.selectVerb(params("ListIdentifiers", null, null, null, null, "r-token")),
              some(IsValid),
              list(hasXPath("count(//oai20:ListIdentifiers/oai20:header)", NS_CTX, returningANumber(), equalTo(2.0)),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[1]/oai20:identifier/text()", NS_CTX, returningAString(), equalTo("id-4")),
                   hasXPath("//oai20:ListIdentifiers/oai20:header[2]/oai20:identifier/text()", NS_CTX, returningAString(), equalTo("id-5")),
                   // token must be empty now since there are no more pages
                   hasXPath("//oai20:ListIdentifiers/oai20:resumptionToken/text()", NS_CTX, returningAString(), equalTo(""))));
    EasyMock.verify(repo.getPersistence());
  }

  @Test
  public void testDateAdaption() {
    final Date d = utcDate(2012, 5, 24, 13, 24, 0);
    final Date expect = utcDate(2012, 5, 24, 0, 0, 0);
    assertEquals(expect, OaiPmhRepository.granulate(Granularity.DAY, d));
  }

  // --

  private void runChecks(String verb, XmlGen xmlGen, Option<OaiPmhResponseStatus> status, List<Matcher<Source>> matchers) throws Exception {
    if (runValidation) {
      for (OaiPmhResponseStatus s : status) {
        assertTrue("http://validator.oaipmh.com/ reports errors", validate(verb, xmlGen, s));
      }
    }
    final Document doc = xmlGen.generate();
    final Source xml = the(doc);
    logger.info(XmlUtil.toXmlString(doc));
    for (Matcher<Source> m : matchers) {
      assertThat(xml, m);
    }
  }

//  private static Source s(XmlGen g) {
//    return new DOMSource(g.generate());
//  }

  private static final String VALIDATOR_SERVICE = "http://validator.oaipmh.com/analysers/validateDirectInput.json";

  enum OaiPmhResponseStatus {
    IsError, IsValid
  }

  private static boolean validate(String verb, XmlGen xmlgen, OaiPmhResponseStatus status) {
    logger.info("--- TALKING TO EXTERNAL OAI-PMH VALIDATION SERVICE ---");
    logger.info("--- " + VALIDATOR_SERVICE);
    final CloseableHttpClient client = HttpClients.createDefault();
    final String xml = xmlgen.generateAsString();
    final HttpPost post = HttpUtil.post(VALIDATOR_SERVICE, HttpUtil.param("xml", xml));
    logger.info("--- REQUEST ---");
    logger.info(xml);
    try {
      final HttpResponse res = client.execute(post);
      final String json = withResource(res.getEntity().getContent(), IoSupport.readToString);
      logger.info("--- RESPONSE ---");
      logger.info(json);
      boolean ok = true;
      for (JsonVal message : JsonObj.jsonObj(json).obj("json").arr("messages")) {
        if (message.isObj()) {
          final JsonObj messageObj = message.as(JsonVal.asJsonObj);
          if (messageObj.has("className")) {
            final String className = messageObj.val("className").as(JsonVal.asString).trim();
            final String text = messageObj.val("text").as(JsonVal.asString).trim();
            logger.info(format("[%s] %s", className, text));
            ok = ok && (eq(className, "correct")
                    // since the validator does not validate everything correctly here are some exclusions
                    || (status == IsError && eq(text, "Could not find a valid OAI-PMH command.")))
                    || (eq(verb, OaiPmhConstants.VERB_IDENTIFY) && eq(text, "Invalid OAI-PMH protocol version ."))
                    || (eq(verb, OaiPmhConstants.VERB_IDENTIFY) && eq(text, "Invalid adminEmail ."));
          }
        }
      }
      return ok;
    } catch (Exception e) {
      return (Boolean) chuck(e);
    } finally {
      IoSupport.closeQuietly(client);
    }
  }

  private static Params params(final String verb, final String identifier, final String metadataPrefix,
                               final String from, final String until, final String resumptionToken) {
    return new Params() {
      @Override String getParameter(String key) {
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

      @Override String getRepositoryUrl() {
        return "http://localhost:8080/oaipmh";
      }
    };
  }

  private static Date utcDate(int year, int month, int day) {
    return utcDate(year, month, day, 0, 0, 0);
  }

  private static Date utcDate(int year, int month, int day, int hour, int minute, int second) {
    Calendar c = Calendar.getInstance();
    c.setTimeZone(TimeZone.getTimeZone("UTC"));
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, second);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static OaiPmhDatabase oaiPmhPersistenceMock(SearchResultItem... items) {
    final SearchResult result = EasyMock.createNiceMock(SearchResult.class);
    final List<SearchResultItem> db = list(items);
    EasyMock.expect(result.getItems()).andReturn(db).anyTimes();
    EasyMock.expect(result.size()).andReturn((long) items.length).anyTimes();
    EasyMock.replay(result);
    return new OaiPmhDatabase() {
      @Override
      public void store(MediaPackage mediaPackage, String repository) throws OaiPmhDatabaseException {
        // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void delete(String mediaPackageId, String repository) throws OaiPmhDatabaseException, NotFoundException {
        // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public SearchResult search(Query q) {
        return result;
      }
    };
  }

  private SearchResultItem searchResultItem(String id, Date modified, boolean deleted) {
    final String seriesDcXml = IoSupport.loadFileFromClassPathAsString("/series-dublincore.xml").get();
    final String episodeDcXml = IoSupport.loadFileFromClassPathAsString("/episode-dublincore.xml").get();
    final DublinCoreCatalog seriesDc = DublinCores.read(IOUtils.toInputStream(seriesDcXml));
    final DublinCoreCatalog episodeDc = DublinCores.read(IOUtils.toInputStream(episodeDcXml));
    final String mpXml = IoSupport.loadFileFromClassPathAsString("/manifest-full.xml").get();
    final String xacml = IoSupport.loadFileFromClassPathAsString("/xacml.xml").get();
    //
    SearchResultItem item = EasyMock.createNiceMock(SearchResultItem.class);
    EasyMock.expect(item.getModificationDate()).andReturn(modified).anyTimes();
    EasyMock.expect(item.getId()).andReturn(id).anyTimes();
    EasyMock.expect(item.isDeleted()).andReturn(deleted).anyTimes();
    EasyMock.expect(item.getMediaPackageXml()).andReturn(mpXml).anyTimes();

    SearchResultElementItem episodeDcElement = EasyMock.createNiceMock(SearchResultElementItem.class);
    EasyMock.expect(episodeDcElement.getType()).andReturn("catalog").anyTimes();
    EasyMock.expect(episodeDcElement.getFlavor()).andReturn("dublincore/episode").anyTimes();
    EasyMock.expect(episodeDcElement.getXml()).andReturn(episodeDcXml).anyTimes();
    EasyMock.expect(episodeDcElement.isEpisodeDublinCore()).andReturn(true).anyTimes();
    EasyMock.expect(episodeDcElement.isSeriesDublinCore()).andReturn(false).anyTimes();
    try {
      EasyMock.expect(episodeDcElement.asDublinCore()).andReturn(episodeDc).anyTimes();
    } catch (OaiPmhDatabaseException ex) { }

    SearchResultElementItem seriesDcElement = EasyMock.createNiceMock(SearchResultElementItem.class);
    EasyMock.expect(seriesDcElement.getType()).andReturn("catalog").anyTimes();
    EasyMock.expect(seriesDcElement.getFlavor()).andReturn("dublincore/series").anyTimes();
    EasyMock.expect(seriesDcElement.getXml()).andReturn(seriesDcXml).anyTimes();
    EasyMock.expect(seriesDcElement.isEpisodeDublinCore()).andReturn(false).anyTimes();
    EasyMock.expect(seriesDcElement.isSeriesDublinCore()).andReturn(true).anyTimes();
    try {
      EasyMock.expect(seriesDcElement.asDublinCore()).andReturn(seriesDc).anyTimes();
    } catch (OaiPmhDatabaseException ex) { }

    SearchResultElementItem securityXacmlElement = EasyMock.createNiceMock(SearchResultElementItem.class);
    EasyMock.expect(securityXacmlElement.getType()).andReturn("catalog").anyTimes();
    EasyMock.expect(securityXacmlElement.getFlavor()).andReturn("security/xacml+series").anyTimes();
    EasyMock.expect(securityXacmlElement.getXml()).andReturn(xacml).anyTimes();
    EasyMock.expect(securityXacmlElement.isEpisodeDublinCore()).andReturn(false).anyTimes();
    EasyMock.expect(securityXacmlElement.isSeriesDublinCore()).andReturn(false).anyTimes();
    try {
      EasyMock.expect(securityXacmlElement.asDublinCore()).andThrow(
              new OaiPmhDatabaseException("this is not a dublincore catalog")).anyTimes();
    } catch (OaiPmhDatabaseException ex) { }

    EasyMock.expect(item.getElements()).andReturn(
            Collections.list(episodeDcElement, seriesDcElement, securityXacmlElement)).anyTimes();
    try {
      EasyMock.expect(item.getEpisodeDublinCore()).andReturn(episodeDc).anyTimes();
      EasyMock.expect(item.getSeriesDublinCore()).andReturn(seriesDc).anyTimes();
    } catch (OaiPmhDatabaseException ex) { }

    EasyMock.replay(item, episodeDcElement, seriesDcElement, securityXacmlElement);
    return item;
  }

  private static OaiPmhRepository repo(final OaiPmhDatabase persistence, final Granularity granularity) {
    return new OaiPmhRepository() {
      @Override
      public Granularity getRepositoryTimeGranularity() {
        return granularity;
      }

      @Override
      public String getRepositoryName() {
        return "Test OAI Repository";
      }

      @Override
      public String getRepositoryId() {
        return "test";
      }

      @Override
      public OaiPmhDatabase getPersistence() {
        return persistence;
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
        return some(new ResumableQuery("oai_dc", new Date(), new Date(), Option.<String>none()));
      }

      @Override
      public int getResultLimit() {
        return (int) RESULT_LIMIT;
      }

      @Override public List<MetadataProvider> getRepositoryMetadataProviders() {
        return Arrays.<MetadataProvider>asList(new MatterhornInlinedMetadataProvider());
      }
    };
  }
}
