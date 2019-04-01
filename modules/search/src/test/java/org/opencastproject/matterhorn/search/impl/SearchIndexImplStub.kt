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

import org.opencastproject.matterhorn.search.SearchIndexException

import java.io.IOException

/**
 * Stub implementation for the search index.
 */
class SearchIndexImplStub
/**
 * Creates a new search index.
 *
 * @param indexName
 * the name of the index
 * @param indexVersion
 * the version
 * @throws SearchIndexException
 * if the index cannot be created
 * @throws IOException
 * if reading and writing from and to the index fails
 */
@Throws(SearchIndexException::class, IOException::class)
constructor(indexName: String, indexVersion: Int, settingsPath: String) : AbstractElasticsearchIndex() {

    /**
     * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex.getDocumentTypes
     */
    override val documentTypes: Array<String>
        get() = arrayOf("content", "version")

    init {
        indexSettingsPath = settingsPath
        super.init(indexName, indexVersion)
    }

    /**
     * Calls the underlying deactivate-method.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    public override fun close() {
        super.close()
    }

    companion object {

        /** Name of the content document type  */
        val CONTENT_TYPE = "content"

        /** Name of the version document type  */
        val VERSION_TYPE = "version"
    }

}
