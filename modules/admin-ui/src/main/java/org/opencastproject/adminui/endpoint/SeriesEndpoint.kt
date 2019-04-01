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

package org.opencastproject.adminui.endpoint

import com.entwinemedia.fn.Stream.`$`
import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
import javax.servlet.http.HttpServletResponse.SC_OK
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.NOT_FOUND
import javax.ws.rs.core.Response.Status.NO_CONTENT
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.index.service.util.RestUtils.notFound
import org.opencastproject.index.service.util.RestUtils.okJson
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.util.DateTimeSupport.toUTC
import org.opencastproject.util.RestUtil.R.badRequest
import org.opencastproject.util.RestUtil.R.conflict
import org.opencastproject.util.RestUtil.R.notFound
import org.opencastproject.util.RestUtil.R.ok
import org.opencastproject.util.RestUtil.R.serverError
import org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN
import org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING
import org.opencastproject.util.doc.rest.RestParameter.Type.TEXT

import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.adminui.util.QueryPreprocessor
import org.opencastproject.authorization.xacml.manager.api.AclService
import org.opencastproject.authorization.xacml.manager.api.AclServiceException
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.index.service.catalog.adapter.MetadataList
import org.opencastproject.index.service.exception.IndexServiceException
import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.index.service.impl.index.event.EventSearchQuery
import org.opencastproject.index.service.impl.index.series.Series
import org.opencastproject.index.service.impl.index.series.SeriesIndexSchema
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery
import org.opencastproject.index.service.impl.index.theme.Theme
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery
import org.opencastproject.index.service.resources.list.query.SeriesListQuery
import org.opencastproject.index.service.util.AccessInformationUtil
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SearchResult
import org.opencastproject.matterhorn.search.SearchResultItem
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.MetadataCollection
import org.opencastproject.metadata.dublincore.MetadataField
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter
import org.opencastproject.rest.BulkOperationResult
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.security.api.Permissions
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workflow.api.WorkflowInstance

import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons
import com.entwinemedia.fn.data.json.Jsons.Functions

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.ArrayList
import java.util.Date
import java.util.Dictionary
import java.util.HashMap

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Path("/")
@RestService(name = "SeriesProxyService", title = "UI Series", abstractText = "This service provides the series data for the UI.", notes = ["This service offers the series CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class SeriesEndpoint : ManagedService {

    private var deleteSeriesWithEventsAllowed: Boolean? = true
    var onlySeriesWithWriteAccessSeriesTab: Boolean? = false
        private set
    var onlySeriesWithWriteAccessEventsFilter: Boolean? = false
        private set

    private var seriesService: SeriesService? = null
    private var securityService: SecurityService? = null
    private var aclServiceFactory: AclServiceFactory? = null
    private var indexService: IndexService? = null
    private var searchIndex: AdminUISearchIndex? = null

    /** Default server URL  */
    private var serverUrl = "http://localhost:8080"

    private val aclService: AclService
        get() = aclServiceFactory!!.serviceFor(securityService!!.organization)

    val newMetadata: Response
        @GET
        @Path("new/metadata")
        @RestQuery(name = "getNewMetadata", description = "Returns all the data related to the metadata tab in the new series modal as JSON", returnDescription = "All the data related to the series metadata tab as JSON", reponses = [RestResponse(responseCode = SC_OK, description = "Returns all the data related to the series metadata tab as JSON")])
        get() {
            val metadataList = indexService!!.metadataListWithAllSeriesCatalogUIAdapters
            val metadataByAdapter = metadataList
                    .getMetadataByAdapter(indexService!!.commonSeriesCatalogUIAdapter)
            if (metadataByAdapter.isSome) {
                val collection = metadataByAdapter.get()
                safelyRemoveField(collection, "identifier")
                metadataList.add(indexService!!.commonSeriesCatalogUIAdapter, collection)
            }
            return okJson(metadataList.toJSON())
        }

    // need to set limit because elasticsearch limit results by 10 per default
    val newThemes: Response
        @GET
        @Path("new/themes")
        @RestQuery(name = "getNewThemes", description = "Returns all the data related to the themes tab in the new series modal as JSON", returnDescription = "All the data related to the series themes tab as JSON", reponses = [RestResponse(responseCode = SC_OK, description = "Returns all the data related to the series themes tab as JSON")])
        get() {
            val query = ThemeSearchQuery(securityService!!.organization.id, securityService!!.user)
            query.withLimit(Integer.MAX_VALUE)
            query.withOffset(0)
            query.sortByName(SearchQuery.Order.Ascending)
            var results: SearchResult<Theme>? = null
            try {
                results = searchIndex!!.getByQuery(query)
            } catch (e: SearchIndexException) {
                logger.error("The admin UI Search Index was not able to get the themes", e)
                return RestUtil.R.serverError()
            }

            val themesJson = JSONObject()
            for (item in results!!.items) {
                val themeInfoJson = JSONObject()
                val theme = item.source
                themeInfoJson["name"] = theme.name
                themeInfoJson["description"] = theme.description
                themesJson[theme.identifier] = themeInfoJson
            }
            return Response.ok(themesJson.toJSONString()).build()
        }

    val seriesOptions: Response
        @GET
        @Path("configuration.json")
        @Produces(MediaType.APPLICATION_JSON)
        @RestQuery(name = "getseriesconfiguration", description = "Get the series configuration", returnDescription = "List of configuration keys", reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."), RestResponse(responseCode = SC_OK, description = "The access information ")])
        get() {
            val jsonReturnObj = JSONObject()
            jsonReturnObj["deleteSeriesWithEventsAllowed"] = deleteSeriesWithEventsAllowed
            return Response.ok(jsonReturnObj.toString()).build()
        }

    /** OSGi callback for the series service.  */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /** OSGi callback for the search index.  */
    fun setIndex(index: AdminUISearchIndex) {
        this.searchIndex = index
    }

    /** OSGi DI.  */
    fun setIndexService(indexService: IndexService) {
        this.indexService = indexService
    }

    /** OSGi callback for the security service  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback for the acl service factory  */
    fun setAclServiceFactory(aclServiceFactory: AclServiceFactory) {
        this.aclServiceFactory = aclServiceFactory
    }

    protected fun activate(cc: ComponentContext?) {
        if (cc != null) {
            val ccServerUrl = cc.bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)
            logger.debug("Configured server url is {}", ccServerUrl)
            if (ccServerUrl != null)
                this.serverUrl = ccServerUrl
        }
        logger.info("Activate series endpoint")
    }

    /** OSGi callback if properties file is present  */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        var dictionaryValue: Any? = properties.get(SERIES_HASEVENTS_DELETE_ALLOW_KEY)
        if (dictionaryValue != null) {
            deleteSeriesWithEventsAllowed = BooleanUtils.toBoolean(dictionaryValue.toString())
        }

        dictionaryValue = properties.get(SERIESTAB_ONLYSERIESWITHWRITEACCESS_KEY)
        if (dictionaryValue != null) {
            onlySeriesWithWriteAccessSeriesTab = BooleanUtils.toBoolean(dictionaryValue.toString())
        }

        dictionaryValue = properties.get(EVENTSFILTER_ONLYSERIESWITHWRITEACCESS_KEY)
        if (dictionaryValue != null) {
            onlySeriesWithWriteAccessEventsFilter = BooleanUtils.toBoolean(dictionaryValue.toString())
        }
    }

    @GET
    @Path("{seriesId}/access.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getseriesaccessinformation", description = "Get the access information of a series", returnDescription = "The access information", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."), RestResponse(responseCode = SC_OK, description = "The access information ")])
    @Throws(NotFoundException::class)
    fun getSeriesAccessInformation(@PathParam("seriesId") seriesId: String): Response {
        if (StringUtils.isBlank(seriesId))
            return RestUtil.R.badRequest("Path parameter series ID is missing")

        val hasProcessingEvents = hasProcessingEvents(seriesId)

        // Add all available ACLs to the response
        val systemAclsJson = JSONArray()
        val acls = aclService.acls
        for (acl in acls) {
            systemAclsJson.add(AccessInformationUtil.serializeManagedAcl(acl))
        }

        val seriesAccessJson = JSONObject()
        try {
            val seriesAccessControl = seriesService!!.getSeriesAccessControl(seriesId)
            val currentAcl = AccessInformationUtil.matchAcls(acls, seriesAccessControl)
            seriesAccessJson["current_acl"] = if (currentAcl.isSome) currentAcl.get().id else 0
            seriesAccessJson["privileges"] = AccessInformationUtil.serializePrivilegesByRole(seriesAccessControl)
            seriesAccessJson["acl"] = AccessControlParser.toJsonSilent(seriesAccessControl)
            seriesAccessJson["locked"] = hasProcessingEvents
        } catch (e: SeriesException) {
            logger.error("Unable to get ACL from series {}", seriesId, e)
            return RestUtil.R.serverError()
        }

        val jsonReturnObj = JSONObject()
        jsonReturnObj["system_acls"] = systemAclsJson
        jsonReturnObj["series_access"] = seriesAccessJson

        return Response.ok(jsonReturnObj.toString()).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{seriesId}/metadata.json")
    @RestQuery(name = "getseriesmetadata", description = "Returns the series metadata as JSON", returnDescription = "Returns the series metadata as JSON", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The series metadata as JSON."), RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class, SearchIndexException::class)
    fun getSeriesMetadata(@PathParam("seriesId") series: String): Response {
        val optSeries = indexService!!.getSeries(series, searchIndex)
        if (optSeries.isNone)
            return notFound("Cannot find a series with id '%s'.", series)

        val metadataList = MetadataList()
        val catalogUIAdapters = indexService!!.seriesCatalogUIAdapters
        catalogUIAdapters.remove(indexService!!.commonSeriesCatalogUIAdapter)
        for (adapter in catalogUIAdapters) {
            val optSeriesMetadata = adapter.getFields(series)
            if (optSeriesMetadata.isSome) {
                metadataList.add(adapter.flavor, adapter.uiTitle, optSeriesMetadata.get())
            }
        }
        metadataList.add(indexService!!.commonSeriesCatalogUIAdapter, getSeriesMetadata(optSeries.get()))
        return okJson(metadataList.toJSON())
    }

    /**
     * Loads the metadata for the given series
     *
     * @param series
     * the source [Series]
     * @return a [MetadataCollection] instance with all the series metadata
     */
    private fun getSeriesMetadata(series: Series): MetadataCollection {
        val metadata = indexService!!.commonSeriesCatalogUIAdapter.rawFields

        val title = metadata.outputFields[DublinCore.PROPERTY_TITLE.localName]
        metadata.removeField(title)
        val newTitle = MetadataField(title)
        newTitle.setValue(series.title)
        metadata.addField(newTitle)

        val subject = metadata.outputFields[DublinCore.PROPERTY_SUBJECT.localName]
        metadata.removeField(subject)
        val newSubject = MetadataField(subject)
        newSubject.setValue(series.subject)
        metadata.addField(newSubject)

        val description = metadata.outputFields[DublinCore.PROPERTY_DESCRIPTION.localName]
        metadata.removeField(description)
        val newDescription = MetadataField(description)
        newDescription.setValue(series.description)
        metadata.addField(newDescription)

        val language = metadata.outputFields[DublinCore.PROPERTY_LANGUAGE.localName]
        metadata.removeField(language)
        val newLanguage = MetadataField(language)
        newLanguage.setValue(series.language)
        metadata.addField(newLanguage)

        val rightsHolder = metadata.outputFields[DublinCore.PROPERTY_RIGHTS_HOLDER.localName]
        metadata.removeField(rightsHolder)
        val newRightsHolder = MetadataField(rightsHolder)
        newRightsHolder.setValue(series.rightsHolder)
        metadata.addField(newRightsHolder)

        val license = metadata.outputFields[DublinCore.PROPERTY_LICENSE.localName]
        metadata.removeField(license)
        val newLicense = MetadataField(license)
        newLicense.setValue(series.license)
        metadata.addField(newLicense)

        val organizers = metadata.outputFields[DublinCore.PROPERTY_CREATOR.localName]
        metadata.removeField(organizers)
        val newOrganizers = MetadataField(organizers)
        newOrganizers.setValue(series.organizers)
        metadata.addField(newOrganizers)

        val contributors = metadata.outputFields[DublinCore.PROPERTY_CONTRIBUTOR.localName]
        metadata.removeField(contributors)
        val newContributors = MetadataField(contributors)
        newContributors.setValue(series.contributors)
        metadata.addField(newContributors)

        val publishers = metadata.outputFields[DublinCore.PROPERTY_PUBLISHER.localName]
        metadata.removeField(publishers)
        val newPublishers = MetadataField(publishers)
        newPublishers.setValue(series.publishers)
        metadata.addField(newPublishers)

        // Admin UI only field
        val createdBy = MetadataField.createTextMetadataField("createdBy", Opt.none(),
                "EVENTS.SERIES.DETAILS.METADATA.CREATED_BY", true, false, Opt.none(),
                Opt.none(), Opt.none(), Opt.some(CREATED_BY_UI_ORDER), Opt.none())
        createdBy.setValue(series.creator)
        metadata.addField(createdBy)

        val uid = metadata.outputFields[DublinCore.PROPERTY_IDENTIFIER.localName]
        metadata.removeField(uid)
        val newUID = MetadataField(uid)
        newUID.setValue(series.identifier)
        metadata.addField(newUID)

        return metadata
    }

    @PUT
    @Path("{seriesId}/metadata")
    @RestQuery(name = "updateseriesmetadata", description = "Update the series metadata with the one given JSON", returnDescription = "Returns OK if the metadata have been saved.", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], restParameters = [RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update")], reponses = [RestResponse(responseCode = SC_OK, description = "The series metadata as JSON."), RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class, SearchIndexException::class)
    fun updateSeriesMetadata(@PathParam("seriesId") seriesID: String,
                             @FormParam("metadata") metadataJSON: String): Response {
        try {
            val metadataList = indexService!!.updateAllSeriesMetadata(seriesID, metadataJSON, searchIndex)
            return okJson(metadataList.toJSON())
        } catch (e: IllegalArgumentException) {
            return RestUtil.R.badRequest(e.message)
        } catch (e: IndexServiceException) {
            return RestUtil.R.serverError()
        }

    }

    private fun safelyRemoveField(collection: MetadataCollection, fieldName: String) {
        val metadataField = collection.outputFields[fieldName]
        if (metadataField != null) {
            collection.removeField(metadataField)
        }
    }

    @POST
    @Path("new")
    @RestQuery(name = "createNewSeries", description = "Creates a new series by the given metadata as JSON", returnDescription = "The created series id", restParameters = [RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT)], reponses = [RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Returns the created series id"), RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "he request could not be fulfilled due to the incorrect syntax of the request"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If user doesn't have rights to create the series")])
    @Throws(UnauthorizedException::class)
    fun createNewSeries(@FormParam("metadata") metadata: String): Response {
        val seriesId: String
        try {
            seriesId = indexService!!.createSeries(metadata)
            return Response.created(URI.create(UrlSupport.concat(serverUrl, "admin-ng/series/", seriesId, "metadata.json")))
                    .entity(seriesId).build()
        } catch (e: IllegalArgumentException) {
            return RestUtil.R.badRequest(e.message)
        } catch (e: IndexServiceException) {
            return RestUtil.R.serverError()
        }

    }

    @DELETE
    @Path("{seriesId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "deleteseries", description = "Delete a series.", returnDescription = "Ok if the series has been deleted.", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The id of the series to delete.", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The series has been deleted."), RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The series could not be found.")])
    @Throws(NotFoundException::class)
    fun deleteSeries(@PathParam("seriesId") id: String): Response {
        try {
            indexService!!.removeSeries(id)
            return Response.ok().build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unable to delete the series '{}' due to", id, e)
            return Response.serverError().build()
        }

    }

    @POST
    @Path("deleteSeries")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "deletemultipleseries", description = "Deletes a json list of series by their given ids e.g. [\"Series-1\", \"Series-2\"]", returnDescription = "A JSON object with arrays that show whether a series was deleted, was not found or there was an error deleting it.", reponses = [RestResponse(description = "Series have been deleted", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST)])
    @Throws(NotFoundException::class)
    fun deleteMultipleSeries(seriesIdsContent: String): Response {
        if (StringUtils.isBlank(seriesIdsContent)) {
            return Response.status(Status.BAD_REQUEST).build()
        }

        val parser = JSONParser()
        val seriesIdsArray: JSONArray
        try {
            seriesIdsArray = parser.parse(seriesIdsContent) as JSONArray
        } catch (e: org.json.simple.parser.ParseException) {
            logger.error("Unable to parse '{}'", seriesIdsContent, e)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: ClassCastException) {
            logger.error("Unable to cast '{}' to a JSON array", seriesIdsContent, e)
            return Response.status(Status.BAD_REQUEST).build()
        }

        val result = BulkOperationResult()
        for (seriesId in seriesIdsArray) {
            try {
                indexService!!.removeSeries(seriesId.toString())
                result.addOk(seriesId.toString())
            } catch (e: NotFoundException) {
                result.addNotFound(seriesId.toString())
            } catch (e: Exception) {
                logger.error("Unable to remove the series '{}'", seriesId.toString(), e)
                result.addServerError(seriesId.toString())
            }

        }
        return Response.ok(result.toJson()).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("series.json")
    @RestQuery(name = "listSeriesAsJson", description = "Returns the series matching the query parameters", returnDescription = "Returns the series search results as JSON", restParameters = [RestParameter(name = "sortorganizer", isRequired = false, description = "The sort type to apply to the series organizer or organizers either Ascending or Descending.", type = STRING), RestParameter(name = "sort", description = "The order instructions used to sort the query result. Must be in the form '<field name>:(ASC|DESC)'", isRequired = false, type = STRING), RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2,value2'", type = STRING), RestParameter(name = "offset", isRequired = false, description = "The page offset", type = INTEGER, defaultValue = "0"), RestParameter(name = "limit", isRequired = false, description = "The limit to define the number of returned results (-1 for all)", type = INTEGER, defaultValue = "100")], reponses = [RestResponse(responseCode = SC_OK, description = "The access control list."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class)
    fun getSeries(@QueryParam("filter") filter: String, @QueryParam("sort") sort: String,
                  @QueryParam("offset") offset: Int, @QueryParam("limit") limit: Int): Response {
        try {
            logger.debug("Requested series list")
            val query = SeriesSearchQuery(securityService!!.organization.id,
                    securityService!!.user)
            val optSort = Option.option(trimToNull(sort))

            if (offset != 0) {
                query.withOffset(offset)
            }

            // If limit is 0, we set the default limit
            query.withLimit(if (limit == 0) DEFAULT_LIMIT else limit)

            val filters = RestUtils.parseFilter(filter)
            for (name in filters.keys) {
                if (SeriesListQuery.FILTER_ACL_NAME == name) {
                    query.withManagedAcl(filters[name])
                } else if (SeriesListQuery.FILTER_CONTRIBUTORS_NAME == name) {
                    query.withContributor(filters[name])
                } else if (SeriesListQuery.FILTER_CREATIONDATE_NAME == name) {
                    try {
                        val fromAndToCreationRange = RestUtils.getFromAndToDateRange(filters[name])
                        query.withCreatedFrom(fromAndToCreationRange.a)
                        query.withCreatedTo(fromAndToCreationRange.b)
                    } catch (e: IllegalArgumentException) {
                        return RestUtil.R.badRequest(e.message)
                    }

                } else if (SeriesListQuery.FILTER_CREATOR_NAME == name) {
                    query.withCreator(filters[name])
                } else if (SeriesListQuery.FILTER_TEXT_NAME == name) {
                    query.withText(QueryPreprocessor.sanitize(filters[name]))
                } else if (SeriesListQuery.FILTER_LANGUAGE_NAME == name) {
                    query.withLanguage(filters[name])
                } else if (SeriesListQuery.FILTER_LICENSE_NAME == name) {
                    query.withLicense(filters[name])
                } else if (SeriesListQuery.FILTER_ORGANIZERS_NAME == name) {
                    query.withOrganizer(filters[name])
                } else if (SeriesListQuery.FILTER_SUBJECT_NAME == name) {
                    query.withSubject(filters[name])
                } else if (SeriesListQuery.FILTER_TITLE_NAME == name) {
                    query.withTitle(filters[name])
                }
            }

            if (optSort.isSome) {
                val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
                for (criterion in sortCriteria) {

                    when (criterion.fieldName) {
                        SeriesIndexSchema.TITLE -> query.sortByTitle(criterion.order)
                        SeriesIndexSchema.CONTRIBUTORS -> query.sortByContributors(criterion.order)
                        SeriesIndexSchema.CREATOR -> query.sortByOrganizers(criterion.order)
                        SeriesIndexSchema.CREATED_DATE_TIME -> query.sortByCreatedDateTime(criterion.order)
                        SeriesIndexSchema.MANAGED_ACL -> query.sortByManagedAcl(criterion.order)
                        else -> {
                            logger.info("Unknown filter criteria {}", criterion.fieldName)
                            return Response.status(SC_BAD_REQUEST).build()
                        }
                    }
                }
            }

            // We search for write actions
            if (onlySeriesWithWriteAccessSeriesTab!!) {
                query.withoutActions()
                query.withAction(Permissions.Action.WRITE)
                query.withAction(Permissions.Action.READ)
            }

            logger.trace("Using Query: $query")

            val result = searchIndex!!.getByQuery(query)
            if (logger.isDebugEnabled) {
                logger.debug("Found {} results in {} ms", result.documentCount, result.searchTime)
            }

            val series = ArrayList<JValue>()
            for (item in result.items) {
                val fields = ArrayList<Field>()
                val s = item.source
                val sId = s.identifier
                fields.add(f("id", v(sId)))
                fields.add(f("title", v(s.title, Jsons.BLANK)))
                fields.add(f("organizers", arr(`$`(s.organizers).map(Functions.stringToJValue))))
                fields.add(f("contributors", arr(`$`(s.contributors).map(Functions.stringToJValue))))
                if (s.creator != null) {
                    fields.add(f("createdBy", v(s.creator)))
                }
                if (s.createdDateTime != null) {
                    fields.add(f("creation_date", v(toUTC(s.createdDateTime.time), Jsons.BLANK)))
                }
                if (s.language != null) {
                    fields.add(f("language", v(s.language)))
                }
                if (s.license != null) {
                    fields.add(f("license", v(s.license)))
                }
                if (s.rightsHolder != null) {
                    fields.add(f("rightsHolder", v(s.rightsHolder)))
                }
                if (StringUtils.isNotBlank(s.managedAcl)) {
                    fields.add(f("managedAcl", v(s.managedAcl)))
                }
                series.add(obj(fields))
            }
            logger.debug("Request done")

            return okJsonList(series, offset, limit, result.hitCount)
        } catch (e: Exception) {
            logger.warn("Could not perform search query", e)
            throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
        }

    }

    /**
     * Search all user series with write or read-only permissions.
     *
     * @param writeAccess
     * true: write access
     * false: read-only access
     * @return user series with write or read-only access,
     * depending on the parameter
     */
    fun getUserSeriesByAccess(writeAccess: Boolean): HashMap<String, String> {
        try {
            val query = SeriesSearchQuery(
                    securityService!!.organization.id, securityService!!.user)

            if (writeAccess) {
                query.withoutActions()
                query.withAction(Permissions.Action.WRITE)
                query.withAction(Permissions.Action.READ)
            }

            val result = searchIndex!!.getByQuery(query)
            val seriesMap = HashMap<String, String>()
            for (item in result.items) {
                val series = item.source
                seriesMap[series.title] = series.identifier
            }

            return seriesMap
        } catch (e: SearchIndexException) {
            logger.warn("Could not perform search query: {}", e)
            throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
        }

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/properties")
    @RestQuery(name = "getSeriesProperties", description = "Returns the series properties", returnDescription = "Returns the series properties as JSON", pathParameters = [RestParameter(name = "id", description = "ID of series", isRequired = true, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The access control list."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun getSeriesPropertiesAsJson(@PathParam("id") seriesId: String): Response {
        if (StringUtils.isBlank(seriesId)) {
            logger.warn("Series id parameter is blank '{}'.", seriesId)
            return Response.status(BAD_REQUEST).build()
        }
        try {
            val properties = seriesService!!.getSeriesProperties(seriesId)
            val jsonProperties = JSONArray()
            for (name in properties.keys) {
                val property = JSONObject()
                property[name] = properties[name]
                jsonProperties.add(property)
            }
            return Response.ok(jsonProperties.toString()).build()
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not perform search query: {}", e.message)
        }

        throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{seriesId}/property/{propertyName}.json")
    @RestQuery(name = "getSeriesProperty", description = "Returns a series property value", returnDescription = "Returns the series property value", pathParameters = [RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING), RestParameter(name = "propertyName", description = "Name of series property", isRequired = true, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The access control list."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun getSeriesProperty(@PathParam("seriesId") seriesId: String,
                          @PathParam("propertyName") propertyName: String): Response {
        if (StringUtils.isBlank(seriesId)) {
            logger.warn("Series id parameter is blank '{}'.", seriesId)
            return Response.status(BAD_REQUEST).build()
        }
        if (StringUtils.isBlank(propertyName)) {
            logger.warn("Series property name parameter is blank '{}'.", propertyName)
            return Response.status(BAD_REQUEST).build()
        }
        try {
            val propertyValue = seriesService!!.getSeriesProperty(seriesId, propertyName)
            return Response.ok(propertyValue).build()
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not perform search query", e)
        }

        throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }

    @POST
    @Path("/{seriesId}/property")
    @RestQuery(name = "updateSeriesProperty", description = "Updates a series property", returnDescription = "No content.", restParameters = [RestParameter(name = "name", isRequired = true, description = "The property's name", type = TEXT), RestParameter(name = "value", isRequired = true, description = "The property's value", type = TEXT)], pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."), RestResponse(responseCode = SC_NO_CONTENT, description = "The access control list has been updated."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action"), RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing in the request.")])
    @Throws(UnauthorizedException::class)
    fun updateSeriesProperty(@PathParam("seriesId") seriesId: String, @FormParam("name") name: String,
                             @FormParam("value") value: String): Response {
        if (StringUtils.isBlank(seriesId)) {
            logger.warn("Series id parameter is blank '{}'.", seriesId)
            return Response.status(BAD_REQUEST).build()
        }
        if (StringUtils.isBlank(name)) {
            logger.warn("Name parameter is blank '{}'.", name)
            return Response.status(BAD_REQUEST).build()
        }
        if (StringUtils.isBlank(value)) {
            logger.warn("Series id parameter is blank '{}'.", value)
            return Response.status(BAD_REQUEST).build()
        }
        try {
            seriesService!!.updateSeriesProperty(seriesId, name, value)
            return Response.status(NO_CONTENT).build()
        } catch (e: NotFoundException) {
            return Response.status(NOT_FOUND).build()
        } catch (e: SeriesException) {
            logger.warn("Could not update series property for series {} property {}:{}", seriesId, name, value, e)
        }

        throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }

    @DELETE
    @Path("{seriesId}/property/{propertyName}")
    @RestQuery(name = "deleteSeriesProperty", description = "Deletes a series property", returnDescription = "No Content", pathParameters = [RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING), RestParameter(name = "propertyName", description = "Name of series property", isRequired = true, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_NO_CONTENT, description = "The series property has been deleted."), RestResponse(responseCode = SC_NOT_FOUND, description = "The series or property has not been found."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun deleteSeriesProperty(@PathParam("seriesId") seriesId: String,
                             @PathParam("propertyName") propertyName: String): Response {
        if (StringUtils.isBlank(seriesId)) {
            logger.warn("Series id parameter is blank '{}'.", seriesId)
            return Response.status(BAD_REQUEST).build()
        }
        if (StringUtils.isBlank(propertyName)) {
            logger.warn("Series property name parameter is blank '{}'.", propertyName)
            return Response.status(BAD_REQUEST).build()
        }
        try {
            seriesService!!.deleteSeriesProperty(seriesId, propertyName)
            return Response.status(NO_CONTENT).build()
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not delete series '{}' property '{}' query", seriesId, propertyName, e)
        }

        throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }

    /**
     * Creates an ok response with the entity being the theme id and name.
     *
     * @param theme
     * The theme to get the id and name from.
     * @return A [Response] with the theme id and name as json contents
     */
    private fun getSimpleThemeJsonResponse(theme: Theme): Response {
        return okJson(obj(f(java.lang.Long.toString(theme.identifier), v(theme.name))))
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{seriesId}/theme.json")
    @RestQuery(name = "getSeriesTheme", description = "Returns the series theme id and name as JSON", returnDescription = "Returns the series theme name and id as JSON", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The series theme id and name as JSON."), RestResponse(responseCode = SC_NOT_FOUND, description = "The series or theme has not been found")])
    fun getSeriesTheme(@PathParam("seriesId") seriesId: String): Response {
        val themeId: Long?
        try {
            val series = indexService!!.getSeries(seriesId, searchIndex)
            if (series.isNone)
                return notFound("Cannot find a series with id {}", seriesId)

            themeId = series.get().theme
        } catch (e: SearchIndexException) {
            logger.error("Unable to get series {}", seriesId, e)
            throw WebApplicationException(e)
        }

        // If no theme is set return empty JSON
        if (themeId == null)
            return okJson(obj())

        try {
            val themeOpt = getTheme(themeId)
            return if (themeOpt.isNone) notFound("Cannot find a theme with id {}", themeId) else getSimpleThemeJsonResponse(themeOpt.get())

        } catch (e: SearchIndexException) {
            logger.error("Unable to get theme {}", themeId, e)
            throw WebApplicationException(e)
        }

    }

    @PUT
    @Path("{seriesId}/theme")
    @RestQuery(name = "updateSeriesTheme", description = "Update the series theme id", returnDescription = "Returns the id and name of the theme.", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], restParameters = [RestParameter(name = "themeId", isRequired = true, type = RestParameter.Type.INTEGER, description = "The id of the theme for this series")], reponses = [RestResponse(responseCode = SC_OK, description = "The series theme has been updated and the theme id and name are returned as JSON."), RestResponse(responseCode = SC_NOT_FOUND, description = "The series or theme has not been found"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun updateSeriesTheme(@PathParam("seriesId") seriesID: String, @FormParam("themeId") themeId: Long): Response {
        try {
            val themeOpt = getTheme(themeId)
            if (themeOpt.isNone)
                return notFound("Cannot find a theme with id {}", themeId)

            seriesService!!.updateSeriesProperty(seriesID, THEME_KEY, java.lang.Long.toString(themeId))
            return getSimpleThemeJsonResponse(themeOpt.get())
        } catch (e: SeriesException) {
            logger.error("Unable to update series theme {}", themeId, e)
            throw WebApplicationException(e)
        } catch (e: SearchIndexException) {
            logger.error("Unable to get theme {}", themeId, e)
            throw WebApplicationException(e)
        }

    }

    @DELETE
    @Path("{seriesId}/theme")
    @RestQuery(name = "deleteSeriesTheme", description = "Removes the theme from the series", returnDescription = "Returns no content", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)], reponses = [RestResponse(responseCode = SC_NO_CONTENT, description = "The series theme has been removed"), RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun deleteSeriesTheme(@PathParam("seriesId") seriesID: String): Response {
        try {
            seriesService!!.deleteSeriesProperty(seriesID, THEME_KEY)
            return Response.noContent().build()
        } catch (e: SeriesException) {
            logger.error("Unable to remove theme from series {}", seriesID, e)
            throw WebApplicationException(e)
        }

    }

    @POST
    @Path("/{seriesId}/access")
    @RestQuery(name = "applyAclToSeries", description = "Immediate application of an ACL to a series", returnDescription = "Status code", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series ID", type = STRING)], restParameters = [RestParameter(name = "acl", isRequired = true, description = "The ACL to apply", type = STRING), RestParameter(name = "override", isRequired = false, defaultValue = "false", description = "If true the series ACL will take precedence over any existing episode ACL", type = BOOLEAN)], reponses = [RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"), RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the given ACL"), RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"), RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error")])
    @Throws(SearchIndexException::class)
    fun applyAclToSeries(@PathParam("seriesId") seriesId: String, @FormParam("acl") acl: String,
                         @DefaultValue("false") @FormParam("override") override: Boolean): Response {

        val accessControlList: AccessControlList
        try {
            accessControlList = AccessControlParser.parseAcl(acl)
        } catch (e: Exception) {
            logger.warn("Unable to parse ACL '{}'", acl)
            return badRequest()
        }

        val series = indexService!!.getSeries(seriesId, searchIndex)
        if (series.isNone)
            return notFound("Cannot find a series with id {}", seriesId)

        if (hasProcessingEvents(seriesId)) {
            logger.warn("Can not update the ACL from series {}. Events being part of the series are currently processed.",
                    seriesId)
            return conflict()
        }

        try {
            if (aclService.applyAclToSeries(seriesId, accessControlList, override))
                return ok()
            else {
                logger.warn("Unable to find series '{}' to apply the ACL.", seriesId)
                return notFound()
            }
        } catch (e: AclServiceException) {
            logger.error("Error applying acl to series {}", seriesId)
            return serverError()
        }

    }

    /**
     * Check if the series with the given Id has events being currently processed
     *
     * @param seriesId
     * the series Id
     * @return true if events being part of the series are currently processed
     */
    private fun hasProcessingEvents(seriesId: String): Boolean {
        val query = EventSearchQuery(securityService!!.organization.id, securityService!!.user)
        var elementsCount: Long = 0
        query.withSeriesId(seriesId)

        try {
            query.withWorkflowState(WorkflowInstance.WorkflowState.RUNNING.toString())
            var events = searchIndex!!.getByQuery(query)
            elementsCount = events.hitCount
            query.withWorkflowState(WorkflowInstance.WorkflowState.INSTANTIATED.toString())
            events = searchIndex!!.getByQuery(query)
            elementsCount += events.hitCount
        } catch (e: SearchIndexException) {
            logger.warn("Could not perform search query", e)
            throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
        }

        return elementsCount > 0
    }

    @GET
    @Path("{seriesId}/hasEvents.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "hasEvents", description = "Check if given series has events", returnDescription = "true if series has events, otherwise false", pathParameters = [RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."), RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."), RestResponse(responseCode = SC_OK, description = "The access information ")])
    @Throws(Exception::class)
    fun getSeriesEvents(@PathParam("seriesId") seriesId: String): Response {
        if (StringUtils.isBlank(seriesId))
            return RestUtil.R.badRequest("Path parameter series ID is missing")

        var elementsCount: Long = 0

        try {
            val query = EventSearchQuery(securityService!!.organization.id, securityService!!.user)
            query.withSeriesId(seriesId)
            val result = searchIndex!!.getByQuery(query)
            elementsCount = result.hitCount
        } catch (e: SearchIndexException) {
            logger.warn("Could not perform search query", e)
            throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
        }

        val jsonReturnObj = JSONObject()
        jsonReturnObj["hasEvents"] = elementsCount > 0
        return Response.ok(jsonReturnObj.toString()).build()
    }

    /**
     * Get a single theme
     *
     * @param id
     * the theme id
     * @return a theme or none if not found, wrapped in an option
     * @throws SearchIndexException
     */
    @Throws(SearchIndexException::class)
    private fun getTheme(id: Long): Opt<Theme> {
        val result = searchIndex!!.getByQuery(ThemeSearchQuery(securityService!!.organization.id,
                securityService!!.user).withIdentifier(id))
        if (result.pageSize == 0L) {
            logger.debug("Didn't find theme with id {}", id)
            return Opt.none()
        }
        return Opt.some(result.items[0].source)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SeriesEndpoint::class.java)

        private val CREATED_BY_UI_ORDER = 9

        /** Default number of items on page  */
        private val DEFAULT_LIMIT = 100

        val THEME_KEY = "theme"

        val SERIES_HASEVENTS_DELETE_ALLOW_KEY = "series.hasEvents.delete.allow"
        val SERIESTAB_ONLYSERIESWITHWRITEACCESS_KEY = "seriesTab.onlySeriesWithWriteAccess"
        val EVENTSFILTER_ONLYSERIESWITHWRITEACCESS_KEY = "eventsFilter.onlySeriesWithWriteAccess"
    }
}
