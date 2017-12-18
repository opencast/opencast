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

// use the class extensions to mock concrete classes (DublinCoreCatalogService)

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodeDate;
import static org.opencastproject.security.util.SecurityUtil.createSystemUser;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.xmlmatchers.XmlMatchers.hasXPath;
import static org.xmlmatchers.xpath.XpathReturnType.returningANumber;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.harvester.OaiPmhNamespaceContext;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.impl.AbstractOaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.impl.OaiPmhDatabaseImpl;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** Second test suite for OAI-PMH including a fully functional persistence backend. */
public class OaiPmhRepositoryPersistenceTest {
  private static final NamespaceContext NS_CTX = OaiPmhNamespaceContext.getContext();
  private static final String FORMAT_PREFIX = OaiPmhConstants.OAI_DC_METADATA_FORMAT.getPrefix();
  private static final String REPOSITORY_ID = "repo";

  /** Turn an XmlGen into a Source. */
  private static Source s(XmlGen g) {
    final Source s = new DOMSource(g.generate());
    print(s);
    return s;
  }

  private static String enc(Date a) {
    return encodeDate(a, Precision.Second).getValue();
  }

  @Test
  public void testInsertRepoSecond() throws Exception {
    final Date now = new Date();
    final MediaPackage mp1 = MediaPackageSupport.loadFromClassPath("/mp1.xml");
    final MediaPackage mp2 = MediaPackageSupport.loadFromClassPath("/mp2.xml");
    final MediaPackage mp3 = MediaPackageSupport.loadFromClassPath("/mp3.xml");
    final OaiPmhRepository repo = repo(oaiPmhDatabase(mp1, mp2, mp3), Granularity.SECOND);
    assertThat(
            "List all records yields 3 records",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, null, null, null))),
            allOf(hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(),
                    equalTo(3.0)),
                    hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='10.0000/11']", NS_CTX),
                    hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='10.0000/12']", NS_CTX),
                    hasXPath("//oai20:ListRecords/oai20:record/oai20:header[oai20:identifier='10.0000/13']", NS_CTX)));
    assertThat("List records from time in the past yields 3 records",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(now), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(3.0)));
    final Date future = new Date(System.currentTimeMillis() + 1000);
    assertThat("List records from time in the future yields no records",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(future), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(0.0)));
  }

  @Test
  public void testInsertRepoDay() throws Exception {
    final Date ref = new Date();
    final MediaPackage mp1 = MediaPackageSupport.loadFromClassPath("/mp1.xml");
    final MediaPackage mp2 = MediaPackageSupport.loadFromClassPath("/mp2.xml");
    final MediaPackage mp3 = MediaPackageSupport.loadFromClassPath("/mp3.xml");
    final OaiPmhRepository repo = repo(oaiPmhDatabase(mp1, mp2, mp3), Granularity.DAY);
    final Date ref2 = new Date();
    final long diff = (ref2.getTime() - ref.getTime()) / 1000;
    assertThat("List records from time in the past",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(ref), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(3.0)));
    assertThat("List records from time in the future. Attention: Test will fail during the last " + diff
            + " seconds of a day!",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(ref2), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(3.0)));
    assertThat(
            "List records from time in the future (5 days ahead)",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(new Date(ref.getTime() + 5 * 60 * 60 * 24
                    * 1000)), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(0.0)));
  }

  @Test
  public void testIdentify() throws Exception {
    assertThat("Identify reports the right date granularity",
            s(repo(oaiPmhDatabase(), Granularity.DAY).selectVerb(params("Identify", null, null, null, null, null))),
            hasXPath("//oai20:Identify[oai20:granularity='YYYY-MM-DD']", NS_CTX));
    assertThat("Identify reports the right date granularity",
            s(repo(oaiPmhDatabase(), Granularity.SECOND).selectVerb(params("Identify", null, null, null, null, null))),
            hasXPath("//oai20:Identify[oai20:granularity='YYYY-MM-DDThh:mm:ssZ']", NS_CTX));
  }

  @Test
  public void testSelectDateRange() throws Exception {
    final Date ref1 = new Date();
    final MediaPackage mp1 = MediaPackageSupport.loadFromClassPath("/mp1.xml");
    final MediaPackage mp2 = MediaPackageSupport.loadFromClassPath("/mp2.xml");
    final MediaPackage mp3 = MediaPackageSupport.loadFromClassPath("/mp3.xml");
    final OaiPmhRepository repo = repo(oaiPmhDatabase(mp1), Granularity.SECOND);
    // wait 1 second since the repo has a time granularity of seconds
    Thread.sleep(1000);
    final Date ref2 = new Date();
    repo.getPersistence().store(mp2, REPOSITORY_ID);
    // wait 1 second since the repo has a time granularity of seconds
    Thread.sleep(1000);
    final Date ref3 = new Date();
    repo.getPersistence().store(mp3, REPOSITORY_ID);
    assertThat("List records from time 1 yields all 3 records",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(ref1), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(3.0)));
    assertThat("List records from time 3 yields only the record record inserted last",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(ref3), null, null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(1.0)));
    assertThat("List records until time 3 yields the first two records",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, null, enc(ref3), null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(2.0)));
    assertThat("List records from time 2 until time 3 yields the record inserted second",
            s(repo.selectVerb(params("ListRecords", null, FORMAT_PREFIX, enc(ref2), enc(ref3), null))),
            hasXPath("count(//oai20:ListRecords/oai20:record/oai20:header)", NS_CTX, returningANumber(), equalTo(1.0)));
  }

  @Test
  public void testListMetadataFormats() throws Exception {
    final MediaPackage mp1 = MediaPackageSupport.loadFromClassPath("/mp1.xml");
    final OaiPmhRepository repo = repo(oaiPmhDatabase(), Granularity.SECOND);
    // add the media package to two different repositories
    repo.getPersistence().store(mp1, repo.getRepositoryId());
    repo.getPersistence().store(mp1, "ANOTHER_REPO");
    assertThat(
            "ListMetadataFormat yields error response",
            s(repo.selectVerb(params("ListMetadataFormats", "UNKNOWN_ID", null, null, null, null))),
            allOf(hasXPath("//oai20:request[@verb='ListMetadataFormats']", NS_CTX),
                    hasXPath("//oai20:error[@code='idDoesNotExist']", NS_CTX)));
    assertThat("ListMetadataFormat yields a response",
            s(repo.selectVerb(params("ListMetadataFormats", mp1.getIdentifier().toString(), null, null, null, null))),
            hasXPath("//oai20:ListMetadataFormats/oai20:metadataFormat", NS_CTX));
  }

  @Test
  public void testBadVerb() throws Exception {
    final OaiPmhRepository repo = repo(oaiPmhDatabase(), Granularity.SECOND);
    assertThat("BadVerb error does not have verb attribute",
            s(repo.selectVerb(params("TheBadVerb", null, null, null, null, null))),
            hasXPath("count(//oai20:request[@verb])", NS_CTX, returningANumber(), equalTo(0.0)));
  }

  @Test
  public void testListSets() throws Exception {
    final OaiPmhRepository repo = repo(oaiPmhDatabase(), Granularity.SECOND);
    assertThat(
            "ListSets return noSetHierarchy response",
            s(repo.selectVerb(params("ListSets", null, null, null, null, null))),
            allOf(hasXPath("//oai20:error[@code='noSetHierarchy']", NS_CTX),
                    hasXPath("count(//oai20:ListSets/oai20:set)", NS_CTX, returningANumber(), equalTo(0.0))));
  }

  // --

  /** Each param may be null. */
  private static Params params(final String verb, final String identifier, final String metadataPrefix,
          final String from, final String until, final String resumptionToken) {
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

      @Override
      String getRepositoryUrl() {
        return "http://localhost:8080/oaipmh";
      }
    };
  }

  private static AbstractOaiPmhDatabase oaiPmhDatabase(MediaPackage... mps) {
    try {
      final Organization org = new DefaultOrganization();
      final SecurityService secSvc = EasyMock.createNiceMock(SecurityService.class);
      // security service
      final User user = createSystemUser("admin", org);
      expect(secSvc.getOrganization()).andReturn(org).anyTimes();
      expect(secSvc.getUser()).andReturn(user).anyTimes();
      EasyMock.replay(secSvc);
      // series service
      final SeriesService seriesService = EasyMock.createNiceMock(SeriesService.class);
      final String xacml = IOUtils.toString(OaiPmhRepositoryPersistenceTest.class.getResource("/xacml.xml").toURI());
      final AccessControlList securityACL = AccessControlParser.parseAcl(xacml);
      EasyMock.expect(seriesService.getSeriesAccessControl("10.0000/1")).andReturn(securityACL).anyTimes();
      EasyMock.replay(seriesService);
      // workspace
      final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
      final File episodeDublinCore = new File(OaiPmhRepositoryPersistenceTest.class.getResource(
              "/episode-dublincore.xml").toURI());
      final File seriesDublinCore = new File(OaiPmhRepositoryPersistenceTest.class
              .getResource("/series-dublincore.xml").toURI());
      expect(workspace.read(EasyMock.anyObject())).andAnswer(() -> {
        final String uri = getCurrentArguments()[0].toString();
        if ("dublincore.xml".equals(uri))
          return new FileInputStream(episodeDublinCore);
        if ("series-dublincore.xml".equals(uri))
          return new FileInputStream(seriesDublinCore);
        throw new Error("Workspace mock does not know about file " + uri);
      }).anyTimes();
      EasyMock.replay(workspace);
      // oai-pmh database
      final EntityManagerFactory emf = PersistenceUtil
              .newTestEntityManagerFactory(OaiPmhDatabaseImpl.PERSISTENCE_UNIT_NAME);
      final AbstractOaiPmhDatabase db = new AbstractOaiPmhDatabase() {
        @Override
        public EntityManagerFactory getEmf() {
          return emf;
        }

        @Override
        public SecurityService getSecurityService() {
          return secSvc;
        }

        @Override
        public SeriesService getSeriesService() {
          return seriesService;
        }

        @Override
        public Workspace getWorkspace() {
          return workspace;
        }

        @Override
        public Date currentDate() {
          return new Date();
        }
      };
      for (MediaPackage mp : mps)
        db.store(mp, REPOSITORY_ID);
      return db;
    } catch (Exception e) {
      return chuck(e);
    }
  }

  private static OaiPmhRepository repo(final AbstractOaiPmhDatabase persistence, final Granularity granularity) {
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
        return REPOSITORY_ID;
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
        return Option.some(new ResumableQuery(FORMAT_PREFIX, new Date(), new Date(), Option.<String> none()));
      }

      @Override
      public int getResultLimit() {
        return 3;
      }

      @Override
      public List<MetadataProvider> getRepositoryMetadataProviders() {
        return nil();
      }

      @Override
      public Date currentDate() {
        return new Date();
      }
    };
  }

  public static void print(Source source) {
    try {
      final Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.transform(source, new StreamResult(System.out));
    } catch (TransformerException e) {
      chuck(e);
    }
  }
}
