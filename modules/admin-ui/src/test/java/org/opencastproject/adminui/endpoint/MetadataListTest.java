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

package org.opencastproject.adminui.endpoint;

import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.metadata.dublincore.MetadataList.Locked;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.PropertiesUtil;

import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class MetadataListTest {
  private CommonEventCatalogUIAdapter episodeDublinCoreCatalogUIAdapter;

  @Before
  public void setUp() throws Exception {
    episodeDublinCoreCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    Properties episodeCatalogProperties = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/episode-catalog.properties");
      episodeCatalogProperties.load(in);
    } catch (IOException e) {
      throw new ComponentException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }

    episodeDublinCoreCatalogUIAdapter.updated(PropertiesUtil.toDictionary(episodeCatalogProperties));
  }

  @Test
  public void testFromJson() throws WebApplicationException, Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/metadata-list-input.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray inputJson = (JSONArray) new JSONParser().parse(reader);

    DublinCoreMetadataCollection abstractMetadataCollection = episodeDublinCoreCatalogUIAdapter.getRawFields();

    MetadataList metadataList = new MetadataList();
    metadataList.add(episodeDublinCoreCatalogUIAdapter, abstractMetadataCollection);
    MetadataJson.fillListFromJson(metadataList, inputJson);
  }

  @Test
  public void testLocked() throws WebApplicationException, Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/metadata-list-input-locked.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray inputJson = (JSONArray) new JSONParser().parse(reader);

    MetadataList metadataList = new MetadataList();
    metadataList.add(episodeDublinCoreCatalogUIAdapter, episodeDublinCoreCatalogUIAdapter.getRawFields());
    metadataList.setLocked(Locked.WORKFLOW_RUNNING);

    assertThat(inputJson.toJSONString(),
      SameJSONAs.sameJSONAs(new SimpleSerializer().toJson(MetadataJson.listToJson(metadataList, true)))
        .allowingAnyArrayOrdering());
  }

}
