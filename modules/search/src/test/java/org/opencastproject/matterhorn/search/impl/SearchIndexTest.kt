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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.matterhorn.search.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.opencastproject.matterhorn.search.impl.SearchIndexImplStub.CONTENT_TYPE

import org.opencastproject.matterhorn.search.SearchMetadata

import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.IOException
import java.util.ArrayList

/**
 * Test case for [AbstractElasticsearchIndex].
 */
class SearchIndexTest {

    /**
     * Does the cleanup after each test.
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        idx!!.clear()
    }

    /**
     * Test method for [org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex.getIndexVersion] .
     */
    @Test
    @Throws(Exception::class)
    fun testGetIndexVersion() {
        populateIndex()
        assertEquals(indexVersion.toLong(), idx!!.indexVersion.toLong())
    }

    /**
     * Adds sample pages to the search index and returns the number of documents added.
     *
     * @return the number of pages added
     */
    @Throws(Exception::class)
    protected fun populateIndex(): Int {
        var count = 0

        // Add content to the index
        for (i in 0..9) {
            val metadata = ArrayList<SearchMetadata<*>>()

            val title = SearchMetadataImpl<String>("title")
            title.addValue("Test entry " + (count + 1))
            metadata.add(title)

            val doc = ElasticsearchDocument(Integer.toString(i), CONTENT_TYPE, metadata)
            idx!!.update(doc)
            count++
        }

        return count
    }

    companion object {

        /** The search index  */
        protected var idx: SearchIndexImplStub? = null

        /** The index root directory  */
        protected var idxRoot: File? = null

        /** The name of the index  */
        protected val indexName = "test"

        /** The index version  */
        protected val indexVersion = 12345

        /** Flag to indicate read only index  */
        protected var isReadOnly = false

        @ClassRule
        var testFolder = TemporaryFolder()

        /**
         * Sets up the solr search index. Since solr sometimes has a hard time shutting down cleanly, it's done only once for
         * all the tests.
         *
         * @throws Exception
         */
        @BeforeClass
        @Throws(Exception::class)
        fun setupClass() {
            // Index
            idxRoot = testFolder.newFolder()
            System.setProperty("opencast.home", idxRoot!!.path)
            ElasticsearchUtils.createIndexConfigurationAt(idxRoot)
            idx = SearchIndexImplStub(indexName, indexVersion, idxRoot!!.path)
        }

        /**
         * Does the cleanup after the test suite.
         */
        @AfterClass
        fun tearDownClass() {
            try {
                if (idx != null)
                    idx!!.close()
            } catch (e: IOException) {
                fail("Error closing search index: " + e.message)
            }

        }
    }
}
