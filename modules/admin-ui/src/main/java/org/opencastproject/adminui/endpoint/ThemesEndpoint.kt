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

import com.entwinemedia.fn.data.Opt.nul
import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
import javax.servlet.http.HttpServletResponse.SC_OK
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.index.service.util.RestUtils.notFound
import org.opencastproject.index.service.util.RestUtils.okJson
import org.opencastproject.index.service.util.RestUtils.okJsonList
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.adminui.util.QueryPreprocessor
import org.opencastproject.index.service.impl.index.series.Series
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery
import org.opencastproject.index.service.impl.index.theme.ThemeIndexSchema
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery
import org.opencastproject.index.service.resources.list.query.ThemesListQuery
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.matterhorn.search.SearchResult
import org.opencastproject.matterhorn.search.SearchResultItem
import org.opencastproject.matterhorn.search.SortCriterion
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.security.api.User
import org.opencastproject.series.api.SeriesException
import org.opencastproject.series.api.SeriesService
import org.opencastproject.staticfiles.api.StaticFileService
import org.opencastproject.staticfiles.endpoint.StaticFileRestService
import org.opencastproject.themes.Theme
import org.opencastproject.themes.ThemesServiceDatabase
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.EqualsUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil
import org.opencastproject.util.RestUtil.R
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.Date

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.DELETE
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
@RestService(name = "themes", title = "Themes facade service", abstractText = "Provides operations for the themes", notes = ["This service offers the default themes CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
open class ThemesEndpoint {

    /** The themes service database  */
    private var themesServiceDatabase: ThemesServiceDatabase? = null

    /** The security service  */
    private var securityService: SecurityService? = null

    /** The admin UI search index  */
    private var searchIndex: AdminUISearchIndex? = null

    /** The series service  */
    private var seriesService: SeriesService? = null

    /** The static file service  */
    private var staticFileService: StaticFileService? = null

    /** The static file REST service  */
    private var staticFileRestService: StaticFileRestService? = null

    /** OSGi callback for the themes service database.  */
    fun setThemesServiceDatabase(themesServiceDatabase: ThemesServiceDatabase) {
        this.themesServiceDatabase = themesServiceDatabase
    }

    /** OSGi callback for the security service.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi DI.  */
    fun setIndex(index: AdminUISearchIndex) {
        this.searchIndex = index
    }

    /** OSGi DI.  */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /** OSGi DI.  */
    fun setStaticFileService(staticFileService: StaticFileService) {
        this.staticFileService = staticFileService
    }

    /** OSGi DI.  */
    fun setStaticFileRestService(staticFileRestService: StaticFileRestService) {
        this.staticFileRestService = staticFileRestService
    }

    fun activate(bundleContext: BundleContext) {
        logger.info("Activate themes endpoint")
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("themes.json")
    @RestQuery(name = "getThemes", description = "Return all of the known themes on the system", restParameters = [RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING), RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.INTEGER), RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.INTEGER), RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: NAME, CREATOR.  Add '_DESC' to reverse the sort order (e.g. CREATOR_DESC).", type = STRING)], reponses = [RestResponse(description = "A JSON representation of the themes", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    fun getThemes(@QueryParam("filter") filter: String, @QueryParam("limit") limit: Int,
                  @QueryParam("offset") offset: Int, @QueryParam("sort") sort: String): Response {
        var optLimit = Option.option(limit)
        val optOffset = Option.option(offset)
        val optSort = Option.option(trimToNull(sort))

        val query = ThemeSearchQuery(securityService!!.organization.id, securityService!!.user)

        // If the limit is set to 0, this is not taken into account
        if (optLimit.isSome && limit == 0) {
            optLimit = Option.none()
        }

        if (optLimit.isSome)
            query.withLimit(optLimit.get())
        if (optOffset.isSome)
            query.withOffset(offset)

        val filters = RestUtils.parseFilter(filter)
        for (name in filters.keys) {
            if (ThemesListQuery.FILTER_CREATOR_NAME == name)
                query.withCreator(filters[name])
            if (ThemesListQuery.FILTER_TEXT_NAME == name)
                query.withText(QueryPreprocessor.sanitize(filters[name]))
        }

        if (optSort.isSome) {
            val sortCriteria = RestUtils.parseSortQueryParameter(optSort.get())
            for (criterion in sortCriteria) {
                when (criterion.fieldName) {
                    ThemeIndexSchema.NAME -> query.sortByName(criterion.order)
                    ThemeIndexSchema.DESCRIPTION -> query.sortByDescription(criterion.order)
                    ThemeIndexSchema.CREATOR -> query.sortByCreator(criterion.order)
                    ThemeIndexSchema.DEFAULT -> query.sortByDefault(criterion.order)
                    ThemeIndexSchema.CREATION_DATE -> query.sortByCreatedDateTime(criterion.order)
                    else -> {
                        logger.info("Unknown sort criteria {}", criterion.fieldName)
                        return Response.status(SC_BAD_REQUEST).build()
                    }
                }
            }
        }

        logger.trace("Using Query: $query")

        var results: SearchResult<org.opencastproject.index.service.impl.index.theme.Theme>? = null
        try {
            results = searchIndex!!.getByQuery(query)
        } catch (e: SearchIndexException) {
            logger.error("The admin UI Search Index was not able to get the themes list:", e)
            return RestUtil.R.serverError()
        }

        val themesJSON = ArrayList<JValue>()

        // If the results list if empty, we return already a response.
        if (results!!.pageSize == 0L) {
            logger.debug("No themes match the given filters.")
            return okJsonList(themesJSON, nul(offset).getOr(0), nul(limit).getOr(0), 0)
        }

        for (item in results.items) {
            val theme = item.source
            themesJSON.add(themeToJSON(theme, false))
        }

        return okJsonList(themesJSON, nul(offset).getOr(0), nul(limit).getOr(0), results.hitCount)
    }

    @GET
    @Path("{themeId}.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getTheme", description = "Returns the theme by the given id as JSON", returnDescription = "The theme as JSON", pathParameters = [RestParameter(name = "themeId", description = "The theme id", isRequired = true, type = RestParameter.Type.INTEGER)], reponses = [RestResponse(description = "Returns the theme as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "No theme with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getThemeResponse(@PathParam("themeId") id: Long): Response {
        val theme = getTheme(id)
        return if (theme.isNone) notFound("Cannot find a theme with id '%s'", id) else okJson(themeToJSON(theme.get(), true))

    }

    @GET
    @Path("{themeId}/usage.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getThemeUsage", description = "Returns the theme usage by the given id as JSON", returnDescription = "The theme usage as JSON", pathParameters = [RestParameter(name = "themeId", description = "The theme id", isRequired = true, type = RestParameter.Type.INTEGER)], reponses = [RestResponse(description = "Returns the theme usage as JSON", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Theme with the given id does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND)])
    @Throws(Exception::class)
    fun getThemeUsage(@PathParam("themeId") themeId: Long): Response {
        val theme = getTheme(themeId)
        if (theme.isNone)
            return notFound("Cannot find a theme with id {}", themeId)

        val query = SeriesSearchQuery(securityService!!.organization.id,
                securityService!!.user).withTheme(themeId)
        var results: SearchResult<Series>? = null
        try {
            results = searchIndex!!.getByQuery(query)
        } catch (e: SearchIndexException) {
            logger.error("The admin UI Search Index was not able to get the series with theme '{}': {}", themeId,
                    ExceptionUtils.getStackTrace(e))
            return RestUtil.R.serverError()
        }

        val seriesValues = ArrayList<JValue>()
        for (item in results!!.items) {
            val series = item.source
            seriesValues.add(obj(f("id", v(series.identifier)), f("title", v(series.title))))
        }
        return okJson(obj(f("series", arr(seriesValues))))
    }

    @POST
    @Path("")
    @RestQuery(name = "createTheme", description = "Add a theme", returnDescription = "Return the created theme", restParameters = [RestParameter(name = "default", description = "Whether the theme is default", isRequired = true, type = Type.BOOLEAN), RestParameter(name = "name", description = "The theme name", isRequired = true, type = Type.STRING), RestParameter(name = "description", description = "The theme description", isRequired = false, type = Type.TEXT), RestParameter(name = "bumperActive", description = "Whether the theme bumper is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "trailerActive", description = "Whether the theme trailer is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "titleSlideActive", description = "Whether the theme title slide is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "licenseSlideActive", description = "Whether the theme license slide is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "watermarkActive", description = "Whether the theme watermark is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "bumperFile", description = "The theme bumper file", isRequired = false, type = Type.STRING), RestParameter(name = "trailerFile", description = "The theme trailer file", isRequired = false, type = Type.STRING), RestParameter(name = "watermarkFile", description = "The theme watermark file", isRequired = false, type = Type.STRING), RestParameter(name = "titleSlideBackground", description = "The theme title slide background file", isRequired = false, type = Type.STRING), RestParameter(name = "licenseSlideBackground", description = "The theme license slide background file", isRequired = false, type = Type.STRING), RestParameter(name = "titleSlideMetadata", description = "The theme title slide metadata", isRequired = false, type = Type.STRING), RestParameter(name = "licenseSlideDescription", description = "The theme license slide description", isRequired = false, type = Type.STRING), RestParameter(name = "watermarkPosition", description = "The theme watermark position", isRequired = false, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Theme created"), RestResponse(responseCode = SC_BAD_REQUEST, description = "The theme references a non-existing file")])
    fun createTheme(@FormParam("default") isDefault: Boolean, @FormParam("name") name: String,
                    @FormParam("description") description: String, @FormParam("bumperActive") bumperActive: Boolean?,
                    @FormParam("trailerActive") trailerActive: Boolean?, @FormParam("titleSlideActive") titleSlideActive: Boolean?,
                    @FormParam("licenseSlideActive") licenseSlideActive: Boolean?,
                    @FormParam("watermarkActive") watermarkActive: Boolean?, @FormParam("bumperFile") bumperFile: String,
                    @FormParam("trailerFile") trailerFile: String, @FormParam("watermarkFile") watermarkFile: String,
                    @FormParam("titleSlideBackground") titleSlideBackground: String,
                    @FormParam("licenseSlideBackground") licenseSlideBackground: String,
                    @FormParam("titleSlideMetadata") titleSlideMetadata: String,
                    @FormParam("licenseSlideDescription") licenseSlideDescription: String,
                    @FormParam("watermarkPosition") watermarkPosition: String): Response {
        val creator = securityService!!.user

        val theme = Theme(Option.none(), Date(), isDefault, creator, name,
                StringUtils.trimToNull(description), BooleanUtils.toBoolean(bumperActive),
                StringUtils.trimToNull(bumperFile), BooleanUtils.toBoolean(trailerActive),
                StringUtils.trimToNull(trailerFile), BooleanUtils.toBoolean(titleSlideActive),
                StringUtils.trimToNull(titleSlideMetadata), StringUtils.trimToNull(titleSlideBackground),
                BooleanUtils.toBoolean(licenseSlideActive), StringUtils.trimToNull(licenseSlideBackground),
                StringUtils.trimToNull(licenseSlideDescription), BooleanUtils.toBoolean(watermarkActive),
                StringUtils.trimToNull(watermarkFile), StringUtils.trimToNull(watermarkPosition))

        try {
            persistReferencedFiles(theme)
        } catch (e: NotFoundException) {
            logger.warn("A file that is referenced in theme '{}' was not found: {}", theme, e.message)
            return R.badRequest("Referenced non-existing file")
        } catch (e: IOException) {
            logger.warn("Error while persisting file: {}", e.message)
            return R.serverError()
        }

        try {
            val createdTheme = themesServiceDatabase!!.updateTheme(theme)
            return RestUtils.okJson(themeToJSON(createdTheme))
        } catch (e: ThemesServiceDatabaseException) {
            logger.error("Unable to create a theme")
            return RestUtil.R.serverError()
        }

    }

    @PUT
    @Path("{themeId}")
    @RestQuery(name = "updateTheme", description = "Updates a theme", returnDescription = "Return the updated theme", pathParameters = [RestParameter(name = "themeId", description = "The theme identifier", isRequired = true, type = Type.INTEGER)], restParameters = [RestParameter(name = "default", description = "Whether the theme is default", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "name", description = "The theme name", isRequired = false, type = Type.STRING), RestParameter(name = "description", description = "The theme description", isRequired = false, type = Type.TEXT), RestParameter(name = "bumperActive", description = "Whether the theme bumper is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "trailerActive", description = "Whether the theme trailer is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "titleSlideActive", description = "Whether the theme title slide is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "licenseSlideActive", description = "Whether the theme license slide is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "watermarkActive", description = "Whether the theme watermark is active", isRequired = false, type = Type.BOOLEAN), RestParameter(name = "bumperFile", description = "The theme bumper file", isRequired = false, type = Type.STRING), RestParameter(name = "trailerFile", description = "The theme trailer file", isRequired = false, type = Type.STRING), RestParameter(name = "watermarkFile", description = "The theme watermark file", isRequired = false, type = Type.STRING), RestParameter(name = "titleSlideBackground", description = "The theme title slide background file", isRequired = false, type = Type.STRING), RestParameter(name = "licenseSlideBackground", description = "The theme license slide background file", isRequired = false, type = Type.STRING), RestParameter(name = "titleSlideMetadata", description = "The theme title slide metadata", isRequired = false, type = Type.STRING), RestParameter(name = "licenseSlideDescription", description = "The theme license slide description", isRequired = false, type = Type.STRING), RestParameter(name = "watermarkPosition", description = "The theme watermark position", isRequired = false, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "Theme updated"), RestResponse(responseCode = SC_NOT_FOUND, description = "If the theme has not been found.")])
    @Throws(NotFoundException::class)
    fun updateTheme(@PathParam("themeId") themeId: Long, @FormParam("default") isDefault: Boolean?,
                    @FormParam("name") name: String, @FormParam("description") description: String,
                    @FormParam("bumperActive") bumperActive: Boolean?, @FormParam("trailerActive") trailerActive: Boolean?,
                    @FormParam("titleSlideActive") titleSlideActive: Boolean?,
                    @FormParam("licenseSlideActive") licenseSlideActive: Boolean?,
                    @FormParam("watermarkActive") watermarkActive: Boolean?, @FormParam("bumperFile") bumperFile: String,
                    @FormParam("trailerFile") trailerFile: String, @FormParam("watermarkFile") watermarkFile: String,
                    @FormParam("titleSlideBackground") titleSlideBackground: String,
                    @FormParam("licenseSlideBackground") licenseSlideBackground: String,
                    @FormParam("titleSlideMetadata") titleSlideMetadata: String,
                    @FormParam("licenseSlideDescription") licenseSlideDescription: String,
                    @FormParam("watermarkPosition") watermarkPosition: String): Response {
        var isDefault = isDefault
        var name = name
        var description = description
        var bumperActive = bumperActive
        var trailerActive = trailerActive
        var titleSlideActive = titleSlideActive
        var licenseSlideActive = licenseSlideActive
        var watermarkActive = watermarkActive
        var bumperFile = bumperFile
        var trailerFile = trailerFile
        var watermarkFile = watermarkFile
        var titleSlideBackground = titleSlideBackground
        var licenseSlideBackground = licenseSlideBackground
        var titleSlideMetadata = titleSlideMetadata
        var licenseSlideDescription = licenseSlideDescription
        var watermarkPosition = watermarkPosition
        try {
            val origTheme = themesServiceDatabase!!.getTheme(themeId)

            if (isDefault == null)
                isDefault = origTheme.isDefault
            if (StringUtils.isBlank(name))
                name = origTheme.name
            if (StringUtils.isEmpty(description))
                description = origTheme.description
            if (bumperActive == null)
                bumperActive = origTheme.isBumperActive
            if (StringUtils.isEmpty(bumperFile))
                bumperFile = origTheme.bumperFile
            if (trailerActive == null)
                trailerActive = origTheme.isTrailerActive
            if (StringUtils.isEmpty(trailerFile))
                trailerFile = origTheme.trailerFile
            if (titleSlideActive == null)
                titleSlideActive = origTheme.isTitleSlideActive
            if (StringUtils.isEmpty(titleSlideMetadata))
                titleSlideMetadata = origTheme.titleSlideMetadata
            if (StringUtils.isEmpty(titleSlideBackground))
                titleSlideBackground = origTheme.titleSlideBackground
            if (licenseSlideActive == null)
                licenseSlideActive = origTheme.isLicenseSlideActive
            if (StringUtils.isEmpty(licenseSlideBackground))
                licenseSlideBackground = origTheme.licenseSlideBackground
            if (StringUtils.isEmpty(licenseSlideDescription))
                licenseSlideDescription = origTheme.licenseSlideDescription
            if (watermarkActive == null)
                watermarkActive = origTheme.isWatermarkActive
            if (StringUtils.isEmpty(watermarkFile))
                watermarkFile = origTheme.watermarkFile
            if (StringUtils.isEmpty(watermarkPosition))
                watermarkPosition = origTheme.watermarkPosition

            val theme = Theme(origTheme.id, origTheme.creationDate, isDefault, origTheme.creator, name,
                    StringUtils.trimToNull(description), BooleanUtils.toBoolean(bumperActive),
                    StringUtils.trimToNull(bumperFile), BooleanUtils.toBoolean(trailerActive),
                    StringUtils.trimToNull(trailerFile), BooleanUtils.toBoolean(titleSlideActive),
                    StringUtils.trimToNull(titleSlideMetadata), StringUtils.trimToNull(titleSlideBackground),
                    BooleanUtils.toBoolean(licenseSlideActive), StringUtils.trimToNull(licenseSlideBackground),
                    StringUtils.trimToNull(licenseSlideDescription), BooleanUtils.toBoolean(watermarkActive),
                    StringUtils.trimToNull(watermarkFile), StringUtils.trimToNull(watermarkPosition))

            try {
                updateReferencedFiles(origTheme, theme)
            } catch (e: IOException) {
                logger.warn("Error while persisting file: {}", e.message)
                return R.serverError()
            } catch (e: NotFoundException) {
                logger.warn("A file that is referenced in theme '{}' was not found: {}", theme, e.message)
                return R.badRequest("Referenced non-existing file")
            }

            val updatedTheme = themesServiceDatabase!!.updateTheme(theme)
            return RestUtils.okJson(themeToJSON(updatedTheme))
        } catch (e: ThemesServiceDatabaseException) {
            logger.error("Unable to update theme {}", themeId, e)
            return RestUtil.R.serverError()
        }

    }

    @DELETE
    @Path("{themeId}")
    @RestQuery(name = "deleteTheme", description = "Deletes a theme", returnDescription = "The method doesn't return any content", pathParameters = [RestParameter(name = "themeId", isRequired = true, description = "The theme identifier", type = RestParameter.Type.INTEGER)], reponses = [RestResponse(responseCode = SC_NOT_FOUND, description = "If the theme has not been found."), RestResponse(responseCode = SC_NO_CONTENT, description = "The method does not return any content"), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(NotFoundException::class, UnauthorizedException::class)
    fun deleteTheme(@PathParam("themeId") themeId: Long): Response {
        try {
            val theme = themesServiceDatabase!!.getTheme(themeId)
            try {
                deleteReferencedFiles(theme)
            } catch (e: IOException) {
                logger.warn("Error while deleting referenced file: {}", e.message)
                return R.serverError()
            }

            themesServiceDatabase!!.deleteTheme(themeId)
            deleteThemeOnSeries(themeId)

            return RestUtil.R.noContent()
        } catch (e: NotFoundException) {
            logger.warn("Unable to find a theme with id $themeId")
            throw e
        } catch (e: ThemesServiceDatabaseException) {
            logger.error("Error getting theme {} during delete operation because: {}", themeId,
                    ExceptionUtils.getStackTrace(e))
            return RestUtil.R.serverError()
        }

    }

    /**
     * Deletes all related series theme entries
     *
     * @param themeId
     * the theme id
     */
    @Throws(UnauthorizedException::class)
    private fun deleteThemeOnSeries(themeId: Long) {
        val query = SeriesSearchQuery(securityService!!.organization.id,
                securityService!!.user).withTheme(themeId)
        var results: SearchResult<Series>? = null
        try {
            results = searchIndex!!.getByQuery(query)
        } catch (e: SearchIndexException) {
            logger.error("The admin UI Search Index was not able to get the series with theme '{}': {}", themeId,
                    ExceptionUtils.getStackTrace(e))
            throw WebApplicationException(e, Status.INTERNAL_SERVER_ERROR)
        }

        for (item in results!!.items) {
            val seriesId = item.source.identifier
            try {
                seriesService!!.deleteSeriesProperty(seriesId, SeriesEndpoint.THEME_KEY)
            } catch (e: NotFoundException) {
                logger.warn("Theme {} already deleted on series {}", themeId, seriesId)
            } catch (e: SeriesException) {
                logger.error("Unable to remove theme from series {}", seriesId, e)
                throw WebApplicationException(e, Status.INTERNAL_SERVER_ERROR)
            }

        }
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
    private fun getTheme(id: Long): Opt<org.opencastproject.index.service.impl.index.theme.Theme> {
        val result = searchIndex!!
                .getByQuery(ThemeSearchQuery(securityService!!.organization.id, securityService!!.user)
                        .withIdentifier(id))
        if (result.pageSize == 0L) {
            logger.debug("Didn't find theme with id {}", id)
            return Opt.none()
        }
        return Opt.some(result.items[0].source)
    }

    /**
     * Returns the JSON representation of this theme.
     *
     * @param theme
     * the theme
     * @param editResponse
     * whether the returning representation should contain edit information
     * @return the JSON representation of this theme.
     */
    private fun themeToJSON(theme: org.opencastproject.index.service.impl.index.theme.Theme, editResponse: Boolean): JValue {
        val fields = ArrayList<Field>()
        fields.add(f("id", v(theme.identifier)))
        fields.add(f("creationDate", v(DateTimeSupport.toUTC(theme.creationDate.time))))
        fields.add(f("default", v(theme.isDefault)))
        fields.add(f("name", v(theme.name)))
        fields.add(f("creator", v(theme.creator)))
        fields.add(f("description", v(theme.description, Jsons.BLANK)))
        fields.add(f("bumperActive", v(theme.isBumperActive)))
        fields.add(f("bumperFile", v(theme.bumperFile, Jsons.BLANK)))
        fields.add(f("trailerActive", v(theme.isTrailerActive)))
        fields.add(f("trailerFile", v(theme.trailerFile, Jsons.BLANK)))
        fields.add(f("titleSlideActive", v(theme.isTitleSlideActive)))
        fields.add(f("titleSlideMetadata", v(theme.titleSlideMetadata, Jsons.BLANK)))
        fields.add(f("titleSlideBackground", v(theme.titleSlideBackground, Jsons.BLANK)))
        fields.add(f("licenseSlideActive", v(theme.isLicenseSlideActive)))
        fields.add(f("licenseSlideDescription", v(theme.licenseSlideDescription, Jsons.BLANK)))
        fields.add(f("licenseSlideBackground", v(theme.licenseSlideBackground, Jsons.BLANK)))
        fields.add(f("watermarkActive", v(theme.isWatermarkActive)))
        fields.add(f("watermarkFile", v(theme.watermarkFile, Jsons.BLANK)))
        fields.add(f("watermarkPosition", v(theme.watermarkPosition, Jsons.BLANK)))
        if (editResponse) {
            extendStaticFileInfo("bumperFile", theme.bumperFile, fields)
            extendStaticFileInfo("trailerFile", theme.trailerFile, fields)
            extendStaticFileInfo("titleSlideBackground", theme.titleSlideBackground, fields)
            extendStaticFileInfo("licenseSlideBackground", theme.licenseSlideBackground, fields)
            extendStaticFileInfo("watermarkFile", theme.watermarkFile, fields)
        }
        return obj(fields)
    }

    private fun extendStaticFileInfo(fieldName: String, staticFileId: String, fields: MutableList<Field>) {
        if (StringUtils.isNotBlank(staticFileId)) {
            try {
                fields.add(f(fieldName + "Name", v(staticFileService!!.getFileName(staticFileId))))
                fields.add(f(fieldName + "Url", v(staticFileRestService!!.getStaticFileURL(staticFileId).toString(), Jsons.BLANK)))
            } catch (e: IllegalStateException) {
                logger.error("Error retreiving static file '{}' ", staticFileId, e)
            } catch (e: NotFoundException) {
                logger.error("Error retreiving static file '{}' ", staticFileId, e)
            }

        }
    }

    /**
     * @return The JSON representation of this theme.
     */
    private fun themeToJSON(theme: Theme): JValue {
        val creator = if (StringUtils.isNotBlank(theme.creator.name))
            theme.creator.name
        else
            theme
                    .creator.username

        val fields = ArrayList<Field>()
        fields.add(f("id", v(theme.id.getOrElse(-1L))))
        fields.add(f("creationDate", v(DateTimeSupport.toUTC(theme.creationDate.time))))
        fields.add(f("default", v(theme.isDefault)))
        fields.add(f("name", v(theme.name)))
        fields.add(f("creator", v(creator)))
        fields.add(f("description", v(theme.description, Jsons.BLANK)))
        fields.add(f("bumperActive", v(theme.isBumperActive)))
        fields.add(f("bumperFile", v(theme.bumperFile, Jsons.BLANK)))
        fields.add(f("trailerActive", v(theme.isTrailerActive)))
        fields.add(f("trailerFile", v(theme.trailerFile, Jsons.BLANK)))
        fields.add(f("titleSlideActive", v(theme.isTitleSlideActive)))
        fields.add(f("titleSlideMetadata", v(theme.titleSlideMetadata, Jsons.BLANK)))
        fields.add(f("titleSlideBackground", v(theme.titleSlideBackground, Jsons.BLANK)))
        fields.add(f("licenseSlideActive", v(theme.isLicenseSlideActive)))
        fields.add(f("licenseSlideDescription", v(theme.licenseSlideDescription, Jsons.BLANK)))
        fields.add(f("licenseSlideBackground", v(theme.licenseSlideBackground, Jsons.BLANK)))
        fields.add(f("watermarkActive", v(theme.isWatermarkActive)))
        fields.add(f("watermarkFile", v(theme.watermarkFile, Jsons.BLANK)))
        fields.add(f("watermarkPosition", v(theme.watermarkPosition, Jsons.BLANK)))
        return obj(fields)
    }

    /**
     * Persist all files that are referenced in the theme.
     *
     * @param theme
     * The theme
     * @throws NotFoundException
     * If a referenced file is not found.
     * @throws IOException
     * If there was an error while persisting the file.
     */
    @Throws(NotFoundException::class, IOException::class)
    private fun persistReferencedFiles(theme: Theme) {
        if (isNotBlank(theme.bumperFile))
            staticFileService!!.persistFile(theme.bumperFile)
        if (isNotBlank(theme.licenseSlideBackground))
            staticFileService!!.persistFile(theme.licenseSlideBackground)
        if (isNotBlank(theme.titleSlideBackground))
            staticFileService!!.persistFile(theme.titleSlideBackground)
        if (isNotBlank(theme.trailerFile))
            staticFileService!!.persistFile(theme.trailerFile)
        if (isNotBlank(theme.watermarkFile))
            staticFileService!!.persistFile(theme.watermarkFile)
    }

    /**
     * Delete all files that are referenced in the theme.
     *
     * @param theme
     * The theme
     * @throws NotFoundException
     * If a referenced file is not found.
     * @throws IOException
     * If there was an error while persisting the file.
     */
    @Throws(NotFoundException::class, IOException::class)
    private fun deleteReferencedFiles(theme: Theme) {
        if (isNotBlank(theme.bumperFile))
            staticFileService!!.deleteFile(theme.bumperFile)
        if (isNotBlank(theme.licenseSlideBackground))
            staticFileService!!.deleteFile(theme.licenseSlideBackground)
        if (isNotBlank(theme.titleSlideBackground))
            staticFileService!!.deleteFile(theme.titleSlideBackground)
        if (isNotBlank(theme.trailerFile))
            staticFileService!!.deleteFile(theme.trailerFile)
        if (isNotBlank(theme.watermarkFile))
            staticFileService!!.deleteFile(theme.watermarkFile)
    }

    /**
     * Update all files that have changed between `original` and `updated`.
     *
     * @param original
     * The original theme
     * @param updated
     * The updated theme
     * @throws NotFoundException
     * If one of the referenced files could not be found.
     * @throws IOException
     * If there was an error while updating the referenced files.
     */
    @Throws(NotFoundException::class, IOException::class)
    private fun updateReferencedFiles(original: Theme, updated: Theme) {
        updateReferencedFile(original.bumperFile, updated.bumperFile)
        updateReferencedFile(original.licenseSlideBackground, updated.licenseSlideBackground)
        updateReferencedFile(original.titleSlideBackground, updated.titleSlideBackground)
        updateReferencedFile(original.trailerFile, updated.trailerFile)
        updateReferencedFile(original.watermarkFile, updated.watermarkFile)
    }

    /**
     * If the file resource has changed between `original` and `updated`, the original file is deleted and the
     * updated one persisted.
     *
     * @param original
     * The UUID of the original file
     * @param updated
     * The UUID of the updated file
     * @throws NotFoundException
     * If the file could not be found
     * @throws IOException
     * If there was an error while persisting or deleting one of the files.
     */
    @Throws(NotFoundException::class, IOException::class)
    private fun updateReferencedFile(original: String, updated: String) {
        if (EqualsUtil.ne(original, updated)) {
            if (isNotBlank(original))
                staticFileService!!.deleteFile(original)
            if (isNotBlank(updated))
                staticFileService!!.persistFile(updated)
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ThemesEndpoint::class.java)
    }

}
