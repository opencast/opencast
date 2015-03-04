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

package org.opencastproject.matterhorn.search.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opencastproject.matterhorn.search.impl.SearchIndexImplStub.CONTENT_TYPE;

import org.opencastproject.matterhorn.search.SearchMetadata;
import org.opencastproject.util.PathSupport;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test case for {@link AbstractElasticsearchIndex}.
 */
public class SearchIndexTest {

  /** The search index */
  protected static SearchIndexImplStub idx = null;

  /** The index root directory */
  protected static File idxRoot = null;

  /** The name of the index */
  protected static final String indexName = "test";

  /** The index version */
  protected static final int indexVersion = 12345;

  /** Flag to indicate read only index */
  protected static boolean isReadOnly = false;

  /**
   * Sets up the solr search index. Since solr sometimes has a hard time shutting down cleanly, it's done only once for
   * all the tests.
   * 
   * @throws Exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    // Index
    String rootPath = PathSupport.concat(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
    System.setProperty("matterhorn.home", rootPath);
    idxRoot = new File(rootPath);
    ElasticsearchUtils.createIndexConfigurationAt(idxRoot, indexName);
    idx = new SearchIndexImplStub(indexName, indexVersion, rootPath);
  }

  /**
   * Does the cleanup after the test suite.
   */
  @AfterClass
  public static void tearDownClass() {
    try {
      if (idx != null)
        idx.close();
      FileUtils.deleteQuietly(idxRoot);
    } catch (IOException e) {
      fail("Error closing search index: " + e.getMessage());
    }
  }

  /**
   * Does the cleanup after each test.
   */
  @After
  public void tearDown() throws Exception {
    idx.clear();
  }

  /**
   * Test method for {@link org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex#getIndexVersion()} .
   */
  @Test
  public void testGetIndexVersion() throws Exception {
    populateIndex();
    assertEquals(indexVersion, idx.getIndexVersion());
  }

  /**
   * Adds sample pages to the search index and returns the number of documents added.
   * 
   * @return the number of pages added
   */
  protected int populateIndex() throws Exception {
    int count = 0;

    // Add content to the index
    for (int i = 0; i < 10; i++) {
      List<SearchMetadata<?>> metadata = new ArrayList<SearchMetadata<?>>();

      SearchMetadata<String> title = new SearchMetadataImpl<String>("title");
      title.addValue("Test entry " + (count + 1));
      metadata.add(title);

      ElasticsearchDocument doc = new ElasticsearchDocument(Integer.toString(i), CONTENT_TYPE, metadata);
      idx.update(doc);
      count++;
    }

    return count;
  }
}
