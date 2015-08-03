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

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.notFound;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.impl.index.theme.ThemeIndexSchema;
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery;
import org.opencastproject.index.service.resources.list.query.ThemesListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.staticfiles.endpoint.StaticFileRestService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "themes", title = "Themes facade service", notes = "This service offers the default themes CRUD Operations for the admin UI.", abstractText = "Provides operations for the themes")
public class ThemesEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ThemesEndpoint.class);

  /** The themes service database */
  private ThemesServiceDatabase themesServiceDatabase;

  /** The security service */
  private SecurityService securityService;

  /** The admin UI search index */
  private AdminUISearchIndex searchIndex;

  /** The series service */
  private SeriesService seriesService;

  /** The static file service */
  private StaticFileService staticFileService;

  /** The static file REST service */
  private StaticFileRestService staticFileRestService;

  /** OSGi callback for the themes service database. */
  public void setThemesServiceDatabase(ThemesServiceDatabase themesServiceDatabase) {
    this.themesServiceDatabase = themesServiceDatabase;
  }

  /** OSGi callback for the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI. */
  public void setIndex(AdminUISearchIndex index) {
    this.searchIndex = index;
  }

  /** OSGi DI. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI. */
  public void setStaticFileService(StaticFileService staticFileService) {
    this.staticFileService = staticFileService;
  }

  /** OSGi DI. */
  public void setStaticFileRestService(StaticFileRestService staticFileRestService) {
    this.staticFileRestService = staticFileRestService;
  }

  public void activate(BundleContext bundleContext) {
    logger.info("Activate themes endpoint");
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("themes.json")
  @RestQuery(name = "getThemes", description = "Return all of the known themes on the system", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.INTEGER),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.INTEGER),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: NAME, CREATOR.  Add '_DESC' to reverse the sort order (e.g. CREATOR_DESC).", type = STRING) }, reponses = { @RestResponse(description = "A JSON representation of the themes", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getThemes(@QueryParam("filter") String filter, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset, @QueryParam("sort") String sort) {
    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));

    ThemeSearchQuery query = new ThemeSearchQuery(securityService.getOrganization().getId(), securityService.getUser());

    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    if (optLimit.isSome())
      query.withLimit(optLimit.get());
    if (optOffset.isSome())
      query.withOffset(offset);

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (ThemesListQuery.FILTER_CREATOR_NAME.equals(name))
        query.withCreator(filters.get(name));
      if (ThemesListQuery.FILTER_TEXT_NAME.equals(name))
        query.withText("*" + filters.get(name) + "*");
    }

    if (optSort.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case ThemeIndexSchema.NAME:
            query.sortByName(criterion.getOrder());
            break;
          case ThemeIndexSchema.DESCRIPTION:
            query.sortByDescription(criterion.getOrder());
            break;
          case ThemeIndexSchema.CREATOR:
            query.sortByCreator(criterion.getOrder());
            break;
          case ThemeIndexSchema.DEFAULT:
            query.sortByDefault(criterion.getOrder());
            break;
          case ThemeIndexSchema.CREATION_DATE:
            query.sortByCreatedDateTime(criterion.getOrder());
            break;
          default:
            logger.info("Unknown sort criteria {}", criterion.getFieldName());
            return Response.status(SC_BAD_REQUEST).build();
        }
      }
    }

    logger.trace("Using Query: " + query.toString());

    SearchResult<org.opencastproject.index.service.impl.index.theme.Theme> results = null;
    try {
      results = searchIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the themes list: {}", e);
      return RestUtil.R.serverError();
    }

    List<JValue> themesJSON = new ArrayList<JValue>();

    // If the results list if empty, we return already a response.
    if (results.getPageSize() == 0) {
      logger.debug("No themes match the given filters.");
      return okJsonList(themesJSON, Opt.nul(offset).or(0), Opt.nul(limit).or(0), 0);
    }

    for (SearchResultItem<org.opencastproject.index.service.impl.index.theme.Theme> item : results.getItems()) {
      org.opencastproject.index.service.impl.index.theme.Theme theme = item.getSource();
      themesJSON.add(themeToJSON(theme, false));
    }

    return okJsonList(themesJSON, Opt.nul(offset).or(0), Opt.nul(limit).or(0), results.getHitCount());
  }

  @GET
  @Path("{themeId}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getTheme", description = "Returns the theme by the given id as JSON", returnDescription = "The theme as JSON", pathParameters = { @RestParameter(name = "themeId", description = "The theme id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Returns the theme as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No theme with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getThemeResponse(@PathParam("themeId") long id) throws Exception {
    Opt<org.opencastproject.index.service.impl.index.theme.Theme> theme = getTheme(id);
    if (theme.isNone())
      return notFound("Cannot find a theme with id '%s'", id);

    return okJson(themeToJSON(theme.get(), true));
  }

  @GET
  @Path("{themeId}/usage.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getThemeUsage", description = "Returns the theme usage by the given id as JSON", returnDescription = "The theme usage as JSON", pathParameters = { @RestParameter(name = "themeId", description = "The theme id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Returns the theme usage as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Theme with the given id does not exist", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getThemeUsage(@PathParam("themeId") long themeId) throws Exception {
    Opt<org.opencastproject.index.service.impl.index.theme.Theme> theme = getTheme(themeId);
    if (theme.isNone())
      return notFound("Cannot find a theme with id {}", themeId);

    SeriesSearchQuery query = new SeriesSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withTheme(themeId);
    SearchResult<Series> results = null;
    try {
      results = searchIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the series with theme '{}': {}", themeId,
              ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }
    List<JValue> seriesValues = new ArrayList<JValue>();
    for (SearchResultItem<Series> item : results.getItems()) {
      Series series = item.getSource();
      seriesValues.add(j(f("id", v(series.getIdentifier())), f("title", v(series.getTitle()))));
    }
    return okJson(j(f("series", a(seriesValues))));
  }

  @POST
  @Path("")
  @RestQuery(name = "createTheme", description = "Add a theme", returnDescription = "Return the created theme", restParameters = {
          @RestParameter(name = "default", description = "Whether the theme is default", isRequired = true, type = Type.BOOLEAN),
          @RestParameter(name = "name", description = "The theme name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The theme description", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "bumperActive", description = "Whether the theme bumper is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "trailerActive", description = "Whether the theme trailer is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "titleSlideActive", description = "Whether the theme title slide is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "licenseSlideActive", description = "Whether the theme license slide is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "watermarkActive", description = "Whether the theme watermark is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "bumperFile", description = "The theme bumper file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "trailerFile", description = "The theme trailer file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "watermarkFile", description = "The theme watermark file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "titleSlideBackground", description = "The theme title slide background file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideBackground", description = "The theme license slide background file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "titleSlideMetadata", description = "The theme title slide metadata", isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideDescription", description = "The theme license slide description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "watermarkPosition", description = "The theme watermark position", isRequired = false, type = Type.STRING), }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Theme created"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The theme references a non-existing file") })
  public Response createTheme(@FormParam("default") boolean isDefault, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("bumperActive") Boolean bumperActive,
          @FormParam("trailerActive") Boolean trailerActive, @FormParam("titleSlideActive") Boolean titleSlideActive,
          @FormParam("licenseSlideActive") Boolean licenseSlideActive,
          @FormParam("watermarkActive") Boolean watermarkActive, @FormParam("bumperFile") String bumperFile,
          @FormParam("trailerFile") String trailerFile, @FormParam("watermarkFile") String watermarkFile,
          @FormParam("titleSlideBackground") String titleSlideBackground,
          @FormParam("licenseSlideBackground") String licenseSlideBackground,
          @FormParam("titleSlideMetadata") String titleSlideMetadata,
          @FormParam("licenseSlideDescription") String licenseSlideDescription,
          @FormParam("watermarkPosition") String watermarkPosition) {
    User creator = securityService.getUser();

    Theme theme = new Theme(Option.<Long> none(), new Date(), isDefault, creator, name,
            StringUtils.trimToNull(description), BooleanUtils.toBoolean(bumperActive),
            StringUtils.trimToNull(bumperFile), BooleanUtils.toBoolean(trailerActive),
            StringUtils.trimToNull(trailerFile), BooleanUtils.toBoolean(titleSlideActive),
            StringUtils.trimToNull(titleSlideMetadata), StringUtils.trimToNull(titleSlideBackground),
            BooleanUtils.toBoolean(licenseSlideActive), StringUtils.trimToNull(licenseSlideBackground),
            StringUtils.trimToNull(licenseSlideDescription), BooleanUtils.toBoolean(watermarkActive),
            StringUtils.trimToNull(watermarkFile), StringUtils.trimToNull(watermarkPosition));

    try {
      persistReferencedFiles(theme);
    } catch (NotFoundException e) {
      logger.warn("A file that is referenced in theme '{}' was not found: {}", theme, e.getMessage());
      return R.badRequest("Referenced non-existing file");
    } catch (IOException e) {
      logger.warn("Error while persisting file: {}", e.getMessage());
      return R.serverError();
    }

    try {
      Theme createdTheme = themesServiceDatabase.updateTheme(theme);
      return RestUtils.okJson(themeToJSON(createdTheme));
    } catch (ThemesServiceDatabaseException e) {
      logger.error("Unable to create a theme");
      return RestUtil.R.serverError();
    }
  }

  @PUT
  @Path("{themeId}")
  @RestQuery(name = "updateTheme", description = "Updates a theme", returnDescription = "Return the updated theme", pathParameters = { @RestParameter(name = "themeId", description = "The theme identifier", isRequired = true, type = Type.INTEGER) }, restParameters = {
          @RestParameter(name = "default", description = "Whether the theme is default", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "name", description = "The theme name", isRequired = false, type = Type.STRING),
          @RestParameter(name = "description", description = "The theme description", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "bumperActive", description = "Whether the theme bumper is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "trailerActive", description = "Whether the theme trailer is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "titleSlideActive", description = "Whether the theme title slide is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "licenseSlideActive", description = "Whether the theme license slide is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "watermarkActive", description = "Whether the theme watermark is active", isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "bumperFile", description = "The theme bumper file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "trailerFile", description = "The theme trailer file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "watermarkFile", description = "The theme watermark file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "titleSlideBackground", description = "The theme title slide background file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideBackground", description = "The theme license slide background file", isRequired = false, type = Type.STRING),
          @RestParameter(name = "titleSlideMetadata", description = "The theme title slide metadata", isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideDescription", description = "The theme license slide description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "watermarkPosition", description = "The theme watermark position", isRequired = false, type = Type.STRING), }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Theme updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the theme has not been found."), })
  public Response updateTheme(@PathParam("themeId") long themeId, @FormParam("default") Boolean isDefault,
          @FormParam("name") String name, @FormParam("description") String description,
          @FormParam("bumperActive") Boolean bumperActive, @FormParam("trailerActive") Boolean trailerActive,
          @FormParam("titleSlideActive") Boolean titleSlideActive,
          @FormParam("licenseSlideActive") Boolean licenseSlideActive,
          @FormParam("watermarkActive") Boolean watermarkActive, @FormParam("bumperFile") String bumperFile,
          @FormParam("trailerFile") String trailerFile, @FormParam("watermarkFile") String watermarkFile,
          @FormParam("titleSlideBackground") String titleSlideBackground,
          @FormParam("licenseSlideBackground") String licenseSlideBackground,
          @FormParam("titleSlideMetadata") String titleSlideMetadata,
          @FormParam("licenseSlideDescription") String licenseSlideDescription,
          @FormParam("watermarkPosition") String watermarkPosition) throws NotFoundException {
    try {
      Theme origTheme = themesServiceDatabase.getTheme(themeId);

      if (isDefault == null)
        isDefault = origTheme.isDefault();
      if (StringUtils.isBlank(name))
        name = origTheme.getName();
      if (StringUtils.isEmpty(description))
        description = origTheme.getDescription();
      if (bumperActive == null)
        bumperActive = origTheme.isBumperActive();
      if (StringUtils.isEmpty(bumperFile))
        bumperFile = origTheme.getBumperFile();
      if (trailerActive == null)
        trailerActive = origTheme.isTrailerActive();
      if (StringUtils.isEmpty(trailerFile))
        trailerFile = origTheme.getTrailerFile();
      if (titleSlideActive == null)
        titleSlideActive = origTheme.isTitleSlideActive();
      if (StringUtils.isEmpty(titleSlideMetadata))
        titleSlideMetadata = origTheme.getTitleSlideMetadata();
      if (StringUtils.isEmpty(titleSlideBackground))
        titleSlideBackground = origTheme.getTitleSlideBackground();
      if (licenseSlideActive == null)
        licenseSlideActive = origTheme.isLicenseSlideActive();
      if (StringUtils.isEmpty(licenseSlideBackground))
        licenseSlideBackground = origTheme.getLicenseSlideBackground();
      if (StringUtils.isEmpty(licenseSlideDescription))
        licenseSlideDescription = origTheme.getLicenseSlideDescription();
      if (watermarkActive == null)
        watermarkActive = origTheme.isWatermarkActive();
      if (StringUtils.isEmpty(watermarkFile))
        watermarkFile = origTheme.getWatermarkFile();
      if (StringUtils.isEmpty(watermarkPosition))
        watermarkPosition = origTheme.getWatermarkPosition();

      Theme theme = new Theme(origTheme.getId(), origTheme.getCreationDate(), isDefault, origTheme.getCreator(), name,
              StringUtils.trimToNull(description), BooleanUtils.toBoolean(bumperActive),
              StringUtils.trimToNull(bumperFile), BooleanUtils.toBoolean(trailerActive),
              StringUtils.trimToNull(trailerFile), BooleanUtils.toBoolean(titleSlideActive),
              StringUtils.trimToNull(titleSlideMetadata), StringUtils.trimToNull(titleSlideBackground),
              BooleanUtils.toBoolean(licenseSlideActive), StringUtils.trimToNull(licenseSlideBackground),
              StringUtils.trimToNull(licenseSlideDescription), BooleanUtils.toBoolean(watermarkActive),
              StringUtils.trimToNull(watermarkFile), StringUtils.trimToNull(watermarkPosition));

      try {
        updateReferencedFiles(origTheme, theme);
      } catch (IOException e) {
        logger.warn("Error while persisting file: {}", e.getMessage());
        return R.serverError();
      } catch (NotFoundException e) {
        logger.warn("A file that is referenced in theme '{}' was not found: {}", theme, e.getMessage());
        return R.badRequest("Referenced non-existing file");
      }

      Theme updatedTheme = themesServiceDatabase.updateTheme(theme);
      return RestUtils.okJson(themeToJSON(updatedTheme));
    } catch (ThemesServiceDatabaseException e) {
      logger.error("Unable to update theme {}: {}", themeId, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }
  }

  @DELETE
  @Path("{themeId}")
  @RestQuery(name = "deleteTheme", description = "Deletes a theme", returnDescription = "The method doesn't return any content", pathParameters = { @RestParameter(name = "themeId", isRequired = true, description = "The theme identifier", type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the theme has not been found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The method does not return any content"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response deleteTheme(@PathParam("themeId") long themeId) throws NotFoundException, UnauthorizedException {
    try {
      Theme theme = themesServiceDatabase.getTheme(themeId);
      try {
        deleteReferencedFiles(theme);
      } catch (IOException e) {
        logger.warn("Error while deleting referenced file: {}", e.getMessage());
        return R.serverError();
      }

      themesServiceDatabase.deleteTheme(themeId);
      deleteThemeOnSeries(themeId);

      return RestUtil.R.noContent();
    } catch (NotFoundException e) {
      logger.warn("Unable to find a theme with id " + themeId);
      throw e;
    } catch (ThemesServiceDatabaseException e) {
      logger.error("Error getting theme {} during delete operation because: {}", themeId,
              ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }
  }

  /**
   * Deletes all related series theme entries
   *
   * @param themeId
   *          the theme id
   */
  private void deleteThemeOnSeries(long themeId) throws UnauthorizedException {
    SeriesSearchQuery query = new SeriesSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withTheme(themeId);
    SearchResult<Series> results = null;
    try {
      results = searchIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the series with theme '{}': {}", themeId,
              ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
    for (SearchResultItem<Series> item : results.getItems()) {
      String seriesId = item.getSource().getIdentifier();
      try {
        seriesService.deleteSeriesProperty(seriesId, SeriesEndpoint.THEME_KEY);
      } catch (NotFoundException e) {
        logger.warn("Theme {} already deleted on series {}", themeId, seriesId);
      } catch (SeriesException e) {
        logger.error("Unable to remove theme from series {}: {}", seriesId, ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
      }
    }
  }

  /**
   * Get a single theme
   *
   * @param id
   *          the theme id
   * @return a theme or none if not found, wrapped in an option
   * @throws SearchIndexException
   */
  private Opt<org.opencastproject.index.service.impl.index.theme.Theme> getTheme(long id) throws SearchIndexException {
    SearchResult<org.opencastproject.index.service.impl.index.theme.Theme> result = searchIndex
            .getByQuery(new ThemeSearchQuery(securityService.getOrganization().getId(), securityService.getUser())
                    .withIdentifier(id));
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find theme with id {}", id);
      return Opt.<org.opencastproject.index.service.impl.index.theme.Theme> none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  /**
   * Returns the JSON representation of this theme.
   *
   * @param theme
   *          the theme
   * @param editResponse
   *          whether the returning representation should contain edit information
   * @return the JSON representation of this theme.
   */
  private JValue themeToJSON(org.opencastproject.index.service.impl.index.theme.Theme theme, boolean editResponse) {
    List<JField> fields = new ArrayList<JField>();
    fields.add(f("id", v(theme.getIdentifier())));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(theme.getCreationDate().getTime()))));
    fields.add(f("default", v(theme.isDefault())));
    fields.add(f("name", v(theme.getName())));
    fields.add(f("creator", v(theme.getCreator())));
    fields.add(f("description", vN(theme.getDescription())));
    fields.add(f("bumperActive", v(theme.isBumperActive())));
    fields.add(f("bumperFile", vN(theme.getBumperFile())));
    fields.add(f("trailerActive", v(theme.isTrailerActive())));
    fields.add(f("trailerFile", vN(theme.getTrailerFile())));
    fields.add(f("titleSlideActive", v(theme.isTitleSlideActive())));
    fields.add(f("titleSlideMetadata", vN(theme.getTitleSlideMetadata())));
    fields.add(f("titleSlideBackground", vN(theme.getTitleSlideBackground())));
    fields.add(f("licenseSlideActive", v(theme.isLicenseSlideActive())));
    fields.add(f("licenseSlideDescription", vN(theme.getLicenseSlideDescription())));
    fields.add(f("licenseSlideBackground", vN(theme.getLicenseSlideBackground())));
    fields.add(f("watermarkActive", v(theme.isWatermarkActive())));
    fields.add(f("watermarkFile", vN(theme.getWatermarkFile())));
    fields.add(f("watermarkPosition", vN(theme.getWatermarkPosition())));
    if (editResponse) {
      extendStaticFileInfo("bumperFile", theme.getBumperFile(), fields);
      extendStaticFileInfo("trailerFile", theme.getTrailerFile(), fields);
      extendStaticFileInfo("titleSlideBackground", theme.getTitleSlideBackground(), fields);
      extendStaticFileInfo("licenseSlideBackground", theme.getLicenseSlideBackground(), fields);
      extendStaticFileInfo("watermarkFile", theme.getWatermarkFile(), fields);
    }
    return j(fields);
  }

  private void extendStaticFileInfo(String fieldName, String staticFileId, List<JField> fields) {
    if (StringUtils.isNotBlank(staticFileId)) {
      try {
        fields.add(f(fieldName.concat("Name"), v(staticFileService.getFileName(staticFileId))));
        fields.add(f(fieldName.concat("Url"), vN(staticFileRestService.getStaticFileURL(staticFileId).toString())));
      } catch (IllegalStateException | NotFoundException e) {
        logger.error("Error retreiving static file '{}' : {}", staticFileId, ExceptionUtils.getStackTrace(e));
      }
    }
  }

  /**
   * @return The JSON representation of this theme.
   */
  private JValue themeToJSON(Theme theme) {
    String creator = StringUtils.isNotBlank(theme.getCreator().getName()) ? theme.getCreator().getName() : theme
            .getCreator().getUsername();

    List<JField> fields = new ArrayList<JField>();
    fields.add(f("id", v(theme.getId().getOrElse(-1L))));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(theme.getCreationDate().getTime()))));
    fields.add(f("default", v(theme.isDefault())));
    fields.add(f("name", v(theme.getName())));
    fields.add(f("creator", v(creator)));
    fields.add(f("description", vN(theme.getDescription())));
    fields.add(f("bumperActive", v(theme.isBumperActive())));
    fields.add(f("bumperFile", vN(theme.getBumperFile())));
    fields.add(f("trailerActive", v(theme.isTrailerActive())));
    fields.add(f("trailerFile", vN(theme.getTrailerFile())));
    fields.add(f("titleSlideActive", v(theme.isTitleSlideActive())));
    fields.add(f("titleSlideMetadata", vN(theme.getTitleSlideMetadata())));
    fields.add(f("titleSlideBackground", vN(theme.getTitleSlideBackground())));
    fields.add(f("licenseSlideActive", v(theme.isLicenseSlideActive())));
    fields.add(f("licenseSlideDescription", vN(theme.getLicenseSlideDescription())));
    fields.add(f("licenseSlideBackground", vN(theme.getLicenseSlideBackground())));
    fields.add(f("watermarkActive", v(theme.isWatermarkActive())));
    fields.add(f("watermarkFile", vN(theme.getWatermarkFile())));
    fields.add(f("watermarkPosition", vN(theme.getWatermarkPosition())));
    return j(fields);
  }

  /**
   * Persist all files that are referenced in the theme.
   *
   * @param theme
   *          The theme
   * @throws NotFoundException
   *           If a referenced file is not found.
   * @throws IOException
   *           If there was an error while persisting the file.
   */
  private void persistReferencedFiles(Theme theme) throws NotFoundException, IOException {
    if (isNotBlank(theme.getBumperFile()))
      staticFileService.persistFile(theme.getBumperFile());
    if (isNotBlank(theme.getLicenseSlideBackground()))
      staticFileService.persistFile(theme.getLicenseSlideBackground());
    if (isNotBlank(theme.getTitleSlideBackground()))
      staticFileService.persistFile(theme.getTitleSlideBackground());
    if (isNotBlank(theme.getTrailerFile()))
      staticFileService.persistFile(theme.getTrailerFile());
    if (isNotBlank(theme.getWatermarkFile()))
      staticFileService.persistFile(theme.getWatermarkFile());
  }

  /**
   * Delete all files that are referenced in the theme.
   *
   * @param theme
   *          The theme
   * @throws NotFoundException
   *           If a referenced file is not found.
   * @throws IOException
   *           If there was an error while persisting the file.
   */
  private void deleteReferencedFiles(Theme theme) throws NotFoundException, IOException {
    if (isNotBlank(theme.getBumperFile()))
      staticFileService.deleteFile(theme.getBumperFile());
    if (isNotBlank(theme.getLicenseSlideBackground()))
      staticFileService.deleteFile(theme.getLicenseSlideBackground());
    if (isNotBlank(theme.getTitleSlideBackground()))
      staticFileService.deleteFile(theme.getTitleSlideBackground());
    if (isNotBlank(theme.getTrailerFile()))
      staticFileService.deleteFile(theme.getTrailerFile());
    if (isNotBlank(theme.getWatermarkFile()))
      staticFileService.deleteFile(theme.getWatermarkFile());
  }

  /**
   * Update all files that have changed between {@code original} and {@code updated}.
   *
   * @param original
   *          The original theme
   * @param updated
   *          The updated theme
   * @throws NotFoundException
   *           If one of the referenced files could not be found.
   * @throws IOException
   *           If there was an error while updating the referenced files.
   */
  private void updateReferencedFiles(Theme original, Theme updated) throws NotFoundException, IOException {
    updateReferencedFile(original.getBumperFile(), updated.getBumperFile());
    updateReferencedFile(original.getLicenseSlideBackground(), updated.getLicenseSlideBackground());
    updateReferencedFile(original.getTitleSlideBackground(), updated.getTitleSlideBackground());
    updateReferencedFile(original.getTrailerFile(), updated.getTrailerFile());
    updateReferencedFile(original.getWatermarkFile(), updated.getWatermarkFile());
  }

  /**
   * If the file resource has changed between {@code original} and {@code updated}, the original file is deleted and the
   * updated one persisted.
   *
   * @param original
   *          The UUID of the original file
   * @param updated
   *          The UUID of the updated file
   * @throws NotFoundException
   *           If the file could not be found
   * @throws IOException
   *           If there was an error while persisting or deleting one of the files.
   */
  private void updateReferencedFile(String original, String updated) throws NotFoundException, IOException {
    if (EqualsUtil.ne(original, updated)) {
      if (isNotBlank(original))
        staticFileService.deleteFile(original);
      if (isNotBlank(updated))
        staticFileService.persistFile(updated);
    }
  }

}
