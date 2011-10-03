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
package org.opencastproject.scheduler.impl.solr;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Tests Solr index.
 * 
 */
public class SchedulerServiceSolrIndexTest {

  private SchedulerServiceSolrIndex index;
  private DublinCoreCatalogService dcService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    index = new SchedulerServiceSolrIndex(PathSupport.concat("target", Long.toString(System.currentTimeMillis())));
    dcService = new DublinCoreCatalogService();
    index.setDublinCoreService(dcService);
    index.activate(null);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    index.deactivate();
    FileUtils.deleteQuietly(new File(index.solrRoot));
    index = null;
  }

  @Test
  public void testInsertingAndDelition() throws Exception {
    DublinCoreCatalog catalog = dcService.newInstance();
    catalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    catalog.add(DublinCore.PROPERTY_TITLE, "Testing");
    catalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");
    Date start = new Date(System.currentTimeMillis() + 600000);
    Date end = new Date(System.currentTimeMillis() + 3600000);
    catalog.add(DublinCore.PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(start, end), Precision.Second));

    index.index(catalog);
    index.getDublinCore("1");

    // SchedulerQuery q = new SchedulerQuery().setSpatial("Device one").setStartsTo(new Date());
    SchedulerQuery q = new SchedulerQuery().setSpatial("Device one").setStartsFrom(new Date());
    List<DublinCoreCatalog> result = index.search(q);
    Assert.assertEquals(1, result.size());

    index.delete("1");
  }

  @Test
  public void testUpdating() throws Exception {

    DublinCoreCatalog dc = dcService.newInstance();
    dc.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    dc.add(DublinCore.PROPERTY_TITLE, "Testing");
    dc.add(DublinCore.PROPERTY_SPATIAL, "Device one");
    Date start = new Date(System.currentTimeMillis() - 60000);
    Date end = new Date(System.currentTimeMillis() + 3600000);
    dc.add(DublinCore.PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(start, end), Precision.Second));

    Date beforeIndexing = new Date();
    index.index(dc);
    Date afterIndexing = new Date();

    SchedulerQuery filter = new SchedulerQuery().setSpatial("Device one");

    Date lastModified = index.getLastModifiedDate(filter);
    Assert.assertTrue("Wrong last modified returned",
            !beforeIndexing.after(lastModified) && !afterIndexing.before(lastModified));

    SchedulerQuery q = new SchedulerQuery().setSpatial("Device one").setStartsFrom(new Date());
    Assert.assertTrue("There should be no results", index.search(q).isEmpty());

    start = new Date(System.currentTimeMillis() + 60000);
    dc.set(DublinCore.PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(start, end), Precision.Second));

    beforeIndexing = new Date();
    index.index(dc);
    afterIndexing = new Date();

    lastModified = index.getLastModifiedDate(filter);
    Assert.assertTrue("Wrong last modified returned",
            !beforeIndexing.after(lastModified) && !afterIndexing.before(lastModified));

    q = new SchedulerQuery().setSpatial("Device one").setStartsFrom(new Date());
    Assert.assertTrue(!index.search(q).isEmpty());
  }

  @Test
  public void testLastModified() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "First");
    firstCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Second");
    secondCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");

    DublinCoreCatalog thirdCatalog = dcService.newInstance();
    thirdCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    thirdCatalog.add(DublinCore.PROPERTY_TITLE, "Third");
    thirdCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device two");

    index.index(firstCatalog);
    Date beforeSecondIndexing = new Date();
    index.index(secondCatalog);
    Date afterSecondIndexing = new Date();
    index.index(thirdCatalog);

    SchedulerQuery filter = new SchedulerQuery().setSpatial("Device one");

    Date lastModified = index.getLastModifiedDate(filter);
    Assert.assertTrue("Wrong last modified returned",
            !beforeSecondIndexing.after(lastModified) && !afterSecondIndexing.before(lastModified));
  }

  @Test
  public void testAddingOfCaptureMetadata() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "First");

    Properties caProperties = new Properties();
    caProperties.put("test.properties", "test");

    index.index(firstCatalog);
    index.index("1", caProperties);
    Properties properties = index.getCaptureAgentProperties("1");
    Assert.assertTrue("Incorrect CA properties", properties.containsKey("test.properties"));
    try {
      index.index("2", caProperties);
      Assert.fail("Should not be able to add CA metadata to nonexistent event");
    } catch (NotFoundException e) {
    }
  }

  @Test
  public void testConflicitngEvents() throws Exception {
    DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "First");
    firstCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");
    Date firstStart = df.parse("03/01/2011 10:00:00");
    Date firstEnd = df.parse("03/01/2011 12:00:00");
    firstCatalog.add(DublinCore.PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(firstStart, firstEnd), Precision.Second));

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Second");
    secondCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");
    Date secondStart = df.parse("03/01/2011 16:00:00");
    Date secondEnd = df.parse("03/01/2011 18:00:00");
    secondCatalog.add(DublinCore.PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(secondStart, secondEnd), Precision.Second));

    DublinCoreCatalog thirdCatalog = dcService.newInstance();
    thirdCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "3");
    thirdCatalog.add(DublinCore.PROPERTY_TITLE, "Third");
    thirdCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device one");
    Date thirdStart = df.parse("04/01/2011 13:00:00");
    Date thirdEnd = df.parse("04/01/2011 15:00:00");
    thirdCatalog.add(DublinCore.PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(thirdStart, thirdEnd), Precision.Second));

    DublinCoreCatalog forthCatalog = dcService.newInstance();
    forthCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "4");
    forthCatalog.add(DublinCore.PROPERTY_TITLE, "Forth");
    forthCatalog.add(DublinCore.PROPERTY_SPATIAL, "Device two");
    Date forthStart = df.parse("03/01/2011 13:00:00");
    Date forthEnd = df.parse("03/01/2011 15:00:00");
    forthCatalog.add(DublinCore.PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(forthStart, forthEnd), Precision.Second));

    index.index(firstCatalog);
    index.index(secondCatalog);
    index.index(thirdCatalog);
    index.index(forthCatalog);

    Date conflictStart = df.parse("03/01/2011 11:00:00");
    Date conflictEnd = df.parse("03/01/2011 17:00:00");
    SchedulerQuery q = new SchedulerQuery().setSpatial("Device one").setEndsFrom(conflictStart)
            .setStartsTo(conflictEnd);
    List<DublinCoreCatalog> result = index.search(q);
    Assert.assertTrue("Incorrect number of conflicting events: " + result.size(), result.size() == 2);
  }

}
