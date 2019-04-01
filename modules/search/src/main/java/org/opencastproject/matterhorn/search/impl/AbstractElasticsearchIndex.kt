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

import org.opencastproject.matterhorn.search.impl.IndexSchema.VERSION

import org.opencastproject.matterhorn.search.SearchIndex
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SearchQuery.Order

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.GetRequestBuilder
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.loader.JsonSettingsLoader
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeValidationException
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.osgi.service.component.ComponentContext
import org.osgi.service.component.ComponentException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import kotlin.collections.Map.Entry

/**
 * A search index implementation based on ElasticSearch.
 */
abstract class AbstractElasticsearchIndex : SearchIndex {

    /** The index identifier  */
    /**
     * Returns the name of this index.
     *
     * @return the index name
     */
    var indexName: String? = null
        private set

    /** Client for talking to elastic search  */
    /**
     * Returns the client used to query the index.
     *
     * @return the Elasticsearch node client
     */
    protected var searchClient: Client? = null
        private set

    /** List of sites with prepared index  */
    private val preparedIndices = ArrayList<String>()

    /** The version number  */
    private var indexVersion = -1

    /** The path to the index settings  */
    protected var indexSettingsPath: String? = null

    /**
     * Address of an external Elasticsearch server to connect to.
     * Opencast will not try to launch an internal server if this is defined.
     */
    private var externalServerAddress: String? = null

    /** Port of an external Elasticsearch server to connect to  */
    private var externalServerPort = 9300

    /**
     * Returns an array of document types for the index. For every one of these, the corresponding document type
     * definition will be loaded.
     *
     * @return the document types
     */
    abstract val documentTypes: Array<String>

    /**
     * OSGi callback to activate this component instance.
     *
     * @param ctx
     * the component context
     * @throws ComponentException
     * if the search index cannot be initialized
     */
    @Throws(ComponentException::class)
    open fun activate(ctx: ComponentContext) {
        indexSettingsPath = StringUtils.trimToNull(ctx.bundleContext.getProperty("karaf.etc"))
        if (indexSettingsPath == null) {
            throw ComponentException("Could not determine Karaf configuration path")
        }

        // Address of an external Elasticsearch node.
        // It's fine if this is not set. Opencast will then launch its own node.
        externalServerAddress = StringUtils.trimToNull(ctx.bundleContext.getProperty(ELASTICSEARCH_SERVER_ADDRESS_KEY))

        // Silently fall back to port 9300
        externalServerPort = Integer.parseInt(StringUtils.defaultIfBlank(
                ctx.bundleContext.getProperty(ELASTICSEARCH_SERVER_PORT_KEY), "9300"))
    }

    /**
     * {@inheritDoc}
     */
    override fun getIndexVersion(): Int {
        return indexVersion
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun clear() {
        try {
            val indicesExistsResponse = searchClient!!.admin().indices()
                    .exists(IndicesExistsRequest(indexName)).actionGet()
            if (indicesExistsResponse.isExists) {
                val delete = searchClient!!.admin().indices().delete(DeleteIndexRequest(indexName))
                        .actionGet()
                if (!delete.isAcknowledged)
                    logger.error("Index '{}' could not be deleted", indexName)
            } else {
                logger.error("Cannot clear non-existing index '{}'", indexName)
            }
        } catch (t: Throwable) {
            throw IOException("Cannot clear index", t)
        }

        preparedIndices.remove(indexName)
        // Create the index
        try {
            createIndex(indexName)
        } catch (e: SearchIndexException) {
            logger.error("Unable to re-create the index after a clear", e)
        }

    }

    /**
     * Removes the given document from the specified index.
     *
     * @param type
     * the document type
     * @param uid
     * the identifier
     * @return `true` if the element was found and deleted
     * @throws SearchIndexException
     * if deletion fails
     */
    @Throws(SearchIndexException::class)
    protected open fun delete(type: String, uid: String): Boolean {

        if (!preparedIndices.contains(indexName)) {
            try {
                createIndex(indexName)
            } catch (e: IOException) {
                throw SearchIndexException(e)
            }

        }

        logger.debug("Removing element with id '{}' from searching index", uid)

        val deleteRequest = searchClient!!.prepareDelete(indexName, type, uid)
        deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
        val delete = deleteRequest.execute().actionGet()
        if (delete.result == DocWriteResponse.Result.NOT_FOUND) {
            logger.trace("Document {} to delete was not found", uid)
            return false
        }

        return true
    }

    /**
     * Posts the input document to the search index.
     *
     * @param documents
     * the input documents
     * @return the query response
     * @throws SearchIndexException
     * if posting to the index fails
     */
    @Throws(SearchIndexException::class)
    fun update(vararg documents: ElasticsearchDocument): BulkResponse {

        val bulkRequest = searchClient!!.prepareBulk()
        for (doc in documents) {
            val type = doc.type
            val uid = doc.uid
            bulkRequest.add(searchClient!!.prepareIndex(indexName, type, uid).setSource(doc))
        }

        // Make sure the operations are searchable immediately
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)

        try {
            val bulkResponse = bulkRequest.execute().actionGet()

            // Check for errors
            if (bulkResponse.hasFailures()) {
                for (item in bulkResponse.items) {
                    if (item.isFailed) {
                        logger.warn("Error updating {}: {}", item, item.failureMessage)
                        throw SearchIndexException(item.failureMessage)
                    }
                }
            }

            return bulkResponse
        } catch (t: Throwable) {
            throw SearchIndexException("Cannot update documents in index " + indexName!!, t)
        }

    }

    /**
     * Initializes an Elasticsearch node for the given index.
     *
     * @param index
     * the index identifier
     * @param version
     * the index version
     * @throws SearchIndexException
     * if the index configuration cannot be loaded
     * @throws IOException
     * if loading of settings fails
     * @throws IllegalArgumentException
     * if the index identifier is blank.
     */
    @Throws(IOException::class, IllegalArgumentException::class, SearchIndexException::class)
    protected fun init(index: String, version: Int) {
        if (StringUtils.isBlank(index)) {
            throw IllegalArgumentException("Search index identifier must be set")
        }

        this.indexName = index
        this.indexVersion = version

        // Configure and start Elasticsearch
        synchronized(AbstractElasticsearchIndex::class.java) {

            // Prepare the configuration of the elastic search node
            val settings = loadNodeSettings()
            if (elasticSearch == null && externalServerAddress == null) {
                logger.info("Starting local Elasticsearch node")

                // Configure and start the elastic search node. In a testing scenario,
                // the node is being created locally.
                elasticSearch = OpencastNode(settings)
                try {
                    elasticSearch!!.start()
                } catch (e: NodeValidationException) {
                    throw SearchIndexException(e)
                }

                logger.info("Elasticsearch node is up and running")
            }

            // Create the client
            if (searchClient == null) {
                if (elasticSearch == null) {
                    // configure external Elasticsearch
                    val inetAddress = InetAddress.getByName(externalServerAddress)
                    searchClient = PreBuiltTransportClient(settings)
                            .addTransportAddress(InetSocketTransportAddress(inetAddress, externalServerPort))
                } else {
                    // configure internal Elasticsearch
                    searchClient = elasticSearch!!.client()
                }
                elasticSearchClients.add(searchClient)
            }
        }

        // Create the index
        createIndex(index)
    }

    /**
     * Closes the client and stops and closes the Elasticsearch node.
     *
     * @throws IOException
     * if stopping the Elasticsearch node fails
     */
    @Throws(IOException::class)
    protected open fun close() {
        try {
            if (searchClient != null) {
                searchClient!!.close()
                synchronized(AbstractElasticsearchIndex::class.java) {
                    elasticSearchClients.remove(searchClient)
                    if (elasticSearchClients.isEmpty() && elasticSearch != null) {
                        logger.info("Stopping local Elasticsearch node")
                        elasticSearch!!.close()
                        elasticSearch = null
                    }
                }
            }
        } catch (t: Throwable) {
            throw IOException("Error stopping the Elasticsearch node", t)
        }

    }

    /**
     * Prepares Elasticsearch index to store data for the types (or mappings) as returned by [.getDocumentTypes].
     *
     * @param idx
     * the index name
     *
     * @throws SearchIndexException
     * if index and type creation fails
     * @throws IOException
     * if loading of the type definitions fails
     */
    @Throws(SearchIndexException::class, IOException::class)
    private fun createIndex(idx: String?) {

        // Make sure the site index exists
        try {
            val indicesExistsResponse = searchClient!!.admin().indices()
                    .exists(IndicesExistsRequest(idx)).actionGet()
            if (!indicesExistsResponse.isExists) {
                logger.debug("Trying to create index for '{}'", idx)
                val indexCreateRequest = CreateIndexRequest(idx)
                        .settings(JsonSettingsLoader(false).load(loadResources("indexSettings.json")))
                val siteIdxResponse = searchClient!!.admin().indices().create(indexCreateRequest).actionGet()
                if (!siteIdxResponse.isAcknowledged) {
                    throw SearchIndexException("Unable to create index for '$idx'")
                }
            }
        } catch (e: ResourceAlreadyExistsException) {
            logger.info("Detected existing index '{}'", idx)
        }

        // Store the correct mapping
        for (type in documentTypes) {
            val siteMappingRequest = PutMappingRequest(idx)
            siteMappingRequest.source(loadResources("$type-mapping.json")!!)
            siteMappingRequest.type(type)
            val siteMappingResponse = searchClient!!.admin().indices().putMapping(siteMappingRequest).actionGet()
            if (!siteMappingResponse.isAcknowledged) {
                throw SearchIndexException("Unable to install '$type' mapping for index '$idx'")
            }
        }

        // See if the index version exists and check if it matches. The request will
        // fail if there is no version index
        var versionIndexExists = false
        val getRequestBuilder = searchClient!!.prepareGet(idx, VERSION_TYPE, ROOT_ID)
        try {
            val response = getRequestBuilder.execute().actionGet()
            if (response.isExists && response.getField(VERSION) != null) {
                val actualIndexVersion = Integer.parseInt(response.getField(VERSION).value.toString())
                if (indexVersion != actualIndexVersion)
                    throw SearchIndexException("Search index is at version " + actualIndexVersion + ", but codebase expects "
                            + indexVersion)
                versionIndexExists = true
                logger.debug("Search index version is {}", indexVersion)
            }
        } catch (e: ElasticsearchException) {
            logger.debug("Version index has not been created")
        }

        // The index does not exist, let's create it
        if (!versionIndexExists) {
            logger.debug("Creating version index for site '{}'", idx)
            var requestBuilder = searchClient!!.prepareIndex(idx, VERSION_TYPE, ROOT_ID)
            logger.debug("Index version of site '{}' is {}", idx, indexVersion)
            requestBuilder = requestBuilder.setSource(VERSION, Integer.toString(indexVersion))
            requestBuilder.execute().actionGet()
        }

        preparedIndices.add(idx)
    }

    /**
     * Load resources from active index class resources if they exist or fall back to this classes resources as default.
     *
     * @return the string containing the resource
     * @throws IOException
     * if reading the resources fails
     */
    @Throws(IOException::class)
    private fun loadResources(filename: String): String? {
        val resourcePath = "/elasticsearch/$filename"
        // Try loading from the index implementation first.
        // This allows index implementations to override the defaults
        for (cls in Arrays.asList<Class<out AbstractElasticsearchIndex>>(this.javaClass, AbstractElasticsearchIndex::class.java)) {
            cls.getResourceAsStream(resourcePath).use { `is` ->
                if (`is` != null) {
                    val settings = IOUtils.toString(`is`, StandardCharsets.UTF_8)
                    logger.debug("Reading elasticsearch configuration resources from {}:\n{}", cls, settings)
                    return settings
                }
            }
        }
        return null
    }

    /**
     * Loads the settings for the elastic search configuration at `etc/elasticsearch.yml`.
     *
     * @return the elastic search settings
     * @throws IOException
     * if the index cannot be created in case it is not there already
     * @throws SearchIndexException
     * if the index configuration cannot be found
     */
    @Throws(IOException::class, SearchIndexException::class)
    private fun loadNodeSettings(): Settings {
        val configFile = File(indexSettingsPath, "elasticsearch.yml")
        if (!configFile.isFile) {
            throw SearchIndexException("Settings for Elasticsearch not found at $configFile")
        }

        // Finally, try and load the index settings
        val settings = Settings.builder().loadFromPath(configFile.toPath()).build()
        val preparedSettings = Settings.builder()
        for (entry in settings.asMap.entries) {
            var value = entry.value
            for ((key, value1) in System.getProperties()) {
                value = value.replace("\${$key}", value1.toString())
            }
            preparedSettings.put(entry.key, value)
        }

        if (externalServerAddress == null) {
            preparedSettings.put("transport.type", "local")
            preparedSettings.put("http.enabled", "false")
        }

        Configurator.initialize(ConfigurationBuilderFactory.newConfigurationBuilder().build())

        return preparedSettings.build()
    }

    /**
     * Creates a request builder for a search query based on the properties known by the search query.
     *
     *
     * Once this query builder has been created, support for ordering needs to be configured as needed.
     *
     * @param query
     * the search query
     * @return the request builder
     */
    protected fun getSearchRequestBuilder(query: SearchQuery, queryBuilder: QueryBuilder): SearchRequestBuilder {

        val requestBuilder = searchClient!!.prepareSearch(indexName)
        requestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH)
        requestBuilder.setPreference("_local")

        // Create the actual search query
        requestBuilder.setQuery(queryBuilder)
        logger.debug("Searching for {}", requestBuilder.toString())

        // Make sure all fields are being returned
        if (query.fields.size > 0) {
            requestBuilder.storedFields(*query.fields)
        } else {
            requestBuilder.storedFields("*")
        }

        // Types
        requestBuilder.setTypes(*query.types)

        // Pagination
        if (query.offset >= 0)
            requestBuilder.setFrom(query.offset)

        var limit = ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW
        if (query.limit > 0) {
            // limit + offset may not exceed some limit
            // this limit seems to be Integer.MAX_VALUE in elasticsearch v1.3 (as we currently use)
            // elasticsearch version 2.1 onwards documented this behaviour by index.max_result_window
            // see https://www.elastic.co/guide/en/elasticsearch/reference/2.1/index-modules.html
            if (query.offset > 0 && query.offset.toLong() + query.limit.toLong() > ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW)
                limit = ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW - query.offset
            else
                limit = query.limit
        }
        requestBuilder.setSize(limit)

        // Sort orders
        val sortCriteria = query.sortOrders
        for ((key, value) in sortCriteria) {
            when (value) {
                SearchQuery.Order.Ascending -> requestBuilder.addSort(key, SortOrder.ASC)
                SearchQuery.Order.Descending -> requestBuilder.addSort(key, SortOrder.DESC)
                else -> {
                }
            }
        }

        return requestBuilder
    }

    private inner class OpencastNode internal constructor(preparedSettings: Settings) : Node(preparedSettings)

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(AbstractElasticsearchIndex::class.java)

        /** The Elasticsearch maximum results window size  */
        private val ELASTICSEARCH_INDEX_MAX_RESULT_WINDOW = Integer.MAX_VALUE

        /** Configuration key defining the address of an external Elasticsearch server  */
        val ELASTICSEARCH_SERVER_ADDRESS_KEY = "org.opencastproject.elasticsearch.server.address"

        /** Configuration key defining the port of an external Elasticsearch server  */
        val ELASTICSEARCH_SERVER_PORT_KEY = "org.opencastproject.elasticsearch.server.port"

        /** Identifier of the root entry  */
        private val ROOT_ID = "root"

        /** Type of the document containing the index version information  */
        private val VERSION_TYPE = "version"

        /** The local elastic search node  */
        private var elasticSearch: Node? = null

        /** List of clients to the local node  */
        private val elasticSearchClients = ArrayList<Client>()
    }

}
