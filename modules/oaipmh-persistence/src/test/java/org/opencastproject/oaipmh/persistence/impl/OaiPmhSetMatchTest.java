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
package org.opencastproject.oaipmh.persistence.impl;

import org.opencastproject.oaipmh.persistence.OaiPmhElementEntity;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinition;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinitionFilter;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinitionImpl;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class OaiPmhSetMatchTest {

  private static String episodeDublincore;
  private static String seriesDublincore;

  @BeforeClass
  public static void setup() throws IOException {
    episodeDublincore = IOUtils.resourceToString("/episode-dublincore.xml", Charset.forName("UTF-8"));
    seriesDublincore = IOUtils.resourceToString("/series-dublincore.xml", Charset.forName("UTF-8"));
  }

  @Test
  public void testMatchSetDef() {
    List<SearchResultElementItem> elements = new ArrayList<>();
    elements.add(new SearchResultElementItemImpl(new OaiPmhElementEntity(null,
        "dublincore/episode", episodeDublincore)));
    elements.add(new SearchResultElementItemImpl(new OaiPmhElementEntity(null,
        "dublincore/series", seriesDublincore)));

    OaiPmhSetDefinition setDef = OaiPmhSetDefinitionImpl.build("open_videos", "Open Videos", null);
    ((OaiPmhSetDefinitionImpl)setDef).addFilter("filter1", "dublincore/episode",
        OaiPmhSetDefinitionFilter.CRITERION_CONTAINS, "license>CC-BY<");

    AbstractOaiPmhDatabase oaiPmhDatabase = new OaiPmhDatabaseImpl();
    // Test episode license is CC-BY (should match)
    Assert.assertTrue(oaiPmhDatabase.matchSetDef(setDef, elements));

    ((OaiPmhSetDefinitionImpl)setDef).addFilter("filter2", "dublincore/series",
        OaiPmhSetDefinitionFilter.CRITERION_CONTAINS, "license>CC-BY-SA<");
    // Test episode license is CC-BY and series license is CC-BY-SA (should not match)
    Assert.assertFalse(oaiPmhDatabase.matchSetDef(setDef, elements));

    ((OaiPmhSetDefinitionImpl)setDef).addFilter("filter2", "dublincore/series",
        OaiPmhSetDefinitionFilter.CRITERION_CONTAINS, "license>CC-BY<");
    // Test episode license is CC-BY and series license is CC-BY-SA or CC-BY (should match)
    Assert.assertTrue(oaiPmhDatabase.matchSetDef(setDef, elements));
  }
}
