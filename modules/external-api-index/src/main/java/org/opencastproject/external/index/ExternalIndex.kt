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

package org.opencastproject.external.index

import org.opencastproject.index.service.impl.index.AbstractSearchIndex
import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.index.service.impl.index.event.EventIndexSchema
import org.opencastproject.index.service.impl.index.group.Group
import org.opencastproject.index.service.impl.index.series.Series
import org.opencastproject.util.data.Option

import org.osgi.service.component.ComponentContext
import org.osgi.service.component.ComponentException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException

/**
 * A search index implementation based on ElasticSearch.
 */
class ExternalIndex : AbstractSearchIndex() {

    /**
     * Returns all the known event locations.
     *
     * @return a list of event locations
     */
    val eventLocations: List<String>
        get() = getTermsForField(EventIndexSchema.LOCATION, Option.some(arrayOf(Event.DOCUMENT_TYPE)))

    /**
     * Returns all the known event subjects.
     *
     * @return a list of event subjects
     */
    val eventSubjects: List<String>
        get() = getTermsForField(EventIndexSchema.SUBJECT, Option.some(arrayOf(Event.DOCUMENT_TYPE)))

    /**
     * Returns all the known event contributors.
     *
     * @return a list of contributors
     */
    val eventContributors: List<String>
        get() = getTermsForField(EventIndexSchema.CONTRIBUTOR, Option.some(arrayOf(Event.DOCUMENT_TYPE)))

    /**
     * Returns all the known event presenters
     *
     * @return a list of presenters
     */
    val eventPresenters: List<String>
        get() = getTermsForField(EventIndexSchema.PRESENTER, Option.some(arrayOf(Event.DOCUMENT_TYPE)))

    /**
     * OSGi callback to activate this component instance.
     *
     * @param ctx
     * the component context
     * @throws ComponentException
     * if the search index cannot be initialized
     */
    @Throws(ComponentException::class)
    override fun activate(ctx: ComponentContext) {
        super.activate(ctx)
        try {
            init(INDEX_NAME, INDEX_VERSION)
        } catch (t: Throwable) {
            throw ComponentException("Error initializing elastic search index", t)
        }

    }

    /**
     * OSGi callback to deactivate this component.
     *
     * @param ctx
     * the component context
     * @throws IOException
     */
    @Throws(IOException::class)
    fun deactivate(ctx: ComponentContext) {
        close()
    }

    override fun getIndexName(): String {
        return INDEX_NAME
    }

    /**
     * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex.getDocumentTypes
     */
    override fun getDocumentTypes(): Array<String> {
        return DOCUMENT_TYPES
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(ExternalIndex::class.java)

        /** The name of this index  */
        private val INDEX_NAME = "externalapi"

        /** The required index version  */
        private val INDEX_VERSION = 101

        /** The document types  */
        private val DOCUMENT_TYPES = arrayOf(Event.DOCUMENT_TYPE, Group.DOCUMENT_TYPE, Series.DOCUMENT_TYPE, "version")
    }

}
