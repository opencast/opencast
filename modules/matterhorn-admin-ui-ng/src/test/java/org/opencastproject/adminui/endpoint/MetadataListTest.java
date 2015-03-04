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
package org.opencastproject.adminui.endpoint;

import static org.junit.Assert.assertThat;

import com.entwinemedia.fn.data.json.SimpleSerializer;
import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataList.Locked;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.WebApplicationException;

public class MetadataListTest {
  private CommonEventCatalogUIAdapter episodeDublinCoreCatalogUIAdapter;

  @Before
  public void setUp() {
    episodeDublinCoreCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    episodeDublinCoreCatalogUIAdapter.activate();
  }

  @Test
  public void testFromJson() throws WebApplicationException, Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/metadata-list-input.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray inputJson = (JSONArray) new JSONParser().parse(reader);

    AbstractMetadataCollection abstractMetadataCollection = episodeDublinCoreCatalogUIAdapter.getRawFields();

    MetadataList metadataList = new MetadataList();
    metadataList.add(episodeDublinCoreCatalogUIAdapter, abstractMetadataCollection);
    metadataList.fromJSON(inputJson.toJSONString());
  }

  @Test
  public void testLocked() throws WebApplicationException, Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/metadata-list-input-locked.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray inputJson = (JSONArray) new JSONParser().parse(reader);

    MetadataList metadataList = new MetadataList();
    metadataList.add(episodeDublinCoreCatalogUIAdapter, episodeDublinCoreCatalogUIAdapter.getRawFields());
    metadataList.setLocked(Locked.WORKFLOW_RUNNING);

    assertThat(inputJson.toJSONString(), SameJSONAs.sameJSONAs(new SimpleSerializer().toJson(metadataList.toJSON()))
            .allowingAnyArrayOrdering());
  }

}
