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

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.notFound;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.conflict;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.adminui.util.QueryPreprocessor;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesIndexSchema;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeSearchQuery;
import org.opencastproject.index.service.resources.list.query.SeriesListQuery;
import org.opencastproject.index.service.util.AccessInformationUtil;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowInstance;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.Jsons.Functions;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
@RestService(name = "SeriesProxyService", title = "UI Series",
  abstractText = "This service provides the series data for the UI.",
  notes = { "This service offers the series CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class SeriesEndpoint implements ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(SeriesEndpoint.class);

  private static final int CREATED_BY_UI_ORDER = 9;

  /** Default number of items on page */
  private static final int DEFAULT_LIMIT = 100;

  public static final String THEME_KEY = "theme";

  private Boolean deleteSeriesWithEventsAllowed = true;

  public static final String SERIES_HASEVENTS_DELETE_ALLOW_KEY = "series.hasEvents.delete.allow";

  private SeriesService seriesService;
  private SecurityService securityService;
  private AclServiceFactory aclServiceFactory;
  private IndexService indexService;
  private AdminUISearchIndex searchIndex;

  /** Default server URL */
  private String serverUrl = "http://localhost:8080";

  /** OSGi callback for the series service. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi callback for the search index. */
  public void setIndex(AdminUISearchIndex index) {
    this.searchIndex = index;
  }

  /** OSGi DI. */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi callback for the security service */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for the acl service factory */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  private AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  protected void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      logger.debug("Configured server url is {}", ccServerUrl);
      if (ccServerUrl != null)
        this.serverUrl = ccServerUrl;
    }
    logger.info("Activate series endpoint");
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    Object dictionaryValue = properties.get(SERIES_HASEVENTS_DELETE_ALLOW_KEY);
    if (dictionaryValue != null) {
      deleteSeriesWithEventsAllowed = BooleanUtils.toBoolean(dictionaryValue.toString());
    }
  }

  @GET
  @Path("{seriesId}/access.json")
  @SuppressWarnings("unchecked")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getseriesaccessinformation", description = "Get the access information of a series", returnDescription = "The access information", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."),
          @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getSeriesAccessInformation(@PathParam("seriesId") String seriesId) throws NotFoundException {
    if (StringUtils.isBlank(seriesId))
      return RestUtil.R.badRequest("Path parameter series ID is missing");

    boolean hasProcessingEvents = hasProcessingEvents(seriesId);

    // Add all available ACLs to the response
    JSONArray systemAclsJson = new JSONArray();
    List<ManagedAcl> acls = getAclService().getAcls();
    for (ManagedAcl acl : acls) {
      systemAclsJson.add(AccessInformationUtil.serializeManagedAcl(acl));
    }

    final TransitionQuery q = TransitionQuery.query().withId(seriesId).withScope(AclScope.Series);
    List<SeriesACLTransition> seriesTransistions;
    JSONArray transitionsJson = new JSONArray();
    try {
      seriesTransistions = getAclService().getTransitions(q).getSeriesTransistions();
      for (SeriesACLTransition trans : seriesTransistions) {
        transitionsJson.add(AccessInformationUtil.serializeSeriesACLTransition(trans));
      }
    } catch (AclServiceException e) {
      logger.error(
              "There was an error while trying to get the ACL transitions for serie '{}' from the ACL service: {}",
              seriesId, e);
      return RestUtil.R.serverError();
    }

    JSONObject seriesAccessJson = new JSONObject();
    try {
      AccessControlList seriesAccessControl = seriesService.getSeriesAccessControl(seriesId);
      Option<ManagedAcl> currentAcl = AccessInformationUtil.matchAcls(acls, seriesAccessControl);
      seriesAccessJson.put("current_acl", currentAcl.isSome() ? currentAcl.get().getId() : 0);
      seriesAccessJson.put("privileges", AccessInformationUtil.serializePrivilegesByRole(seriesAccessControl));
      seriesAccessJson.put("acl", AccessControlParser.toJsonSilent(seriesAccessControl));
      seriesAccessJson.put("transitions", transitionsJson);
      seriesAccessJson.put("locked", hasProcessingEvents);
    } catch (SeriesException e) {
      logger.error("Unable to get ACL from series {}: {}", seriesId, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    JSONObject jsonReturnObj = new JSONObject();
    jsonReturnObj.put("system_acls", systemAclsJson);
    jsonReturnObj.put("series_access", seriesAccessJson);

    return Response.ok(jsonReturnObj.toString()).build();
  }

  @GET
  @Path("{seriesId}/participation.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getseriesparticipationinformation", description = "Get the particition information of a series", returnDescription = "The participation information", pathParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."),
                  @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getSeriesParticipationInformation(@PathParam("seriesId") String seriesId) throws Exception {
    if (StringUtils.isBlank(seriesId))
      return RestUtil.R.badRequest("Path parameter series ID is missing");

    Opt<Series> optSeries = indexService.getSeries(seriesId, searchIndex);

    if (optSeries.isNone()) {
      logger.warn("Unable to find the series '{}'", seriesId);
      return notFound();
    }

    Series series = optSeries.get();

    return okJson(obj(f("opt_out", v(series.isOptedOut()))));
  }

  @PUT
  @Path("{seriesId}/optout/{optout}")
  @RestQuery(name = "updateSeriesOptoutStatus", description = "Updates a series opt out status.", returnDescription = "The method doesn't return any content", pathParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = RestParameter.Type.STRING),
          @RestParameter(name = "optout", isRequired = true, description = "True or false, true to opt out of this series.", type = RestParameter.Type.BOOLEAN) }, restParameters = {}, reponses = {
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"),
                  @RestResponse(responseCode = SC_NO_CONTENT, description = "The method doesn't return any content") })
  public Response updateSeriesOptOut(@PathParam("seriesId") String seriesId, @PathParam("optout") boolean optout)
          throws NotFoundException {
    try {
      seriesService.updateOptOutStatus(seriesId, optout);
      return Response.noContent().build();
    } catch (SeriesException e) {
      logger.error("Unable to updated opt out status for series with id {}", seriesId);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("optouts")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "changeOptOuts", description = "Change the opt out status of many series", returnDescription = "A JSON array listing which series were or were not opted out.", restParameters = {
          @RestParameter(name = "seriesIds", description = "A JSON array of ids of the series to opt out or in", defaultValue = "[]", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "optout", description = "Whether to opt out or not either true or false.", defaultValue = "false", isRequired = true, type = RestParameter.Type.BOOLEAN), }, reponses = {
                  @RestResponse(description = "Returns a JSON object with the results for the different opted out or in elements such as ok, notFound or error.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Unable to parse boolean value to opt out, or parse JSON array of opt out series", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response changeOptOuts(@FormParam("optout") boolean optout, @FormParam("seriesIds") String seriesIds) {
    JSONParser parser = new JSONParser();
    JSONArray seriesIdsArray;
    try {
      seriesIdsArray = (JSONArray) parser.parse(seriesIds);
    } catch (org.json.simple.parser.ParseException e) {
      logger.warn("Unable to parse series ids {} : {}", seriesIds, ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    } catch (NullPointerException e) {
      logger.warn("Unable to parse series ids because it was null {}", seriesIds);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.warn("Unable to parse series ids because it was the wrong class {} : {}", seriesIds,
              ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    }

    BulkOperationResult result = new BulkOperationResult();
    for (Object seriesId : seriesIdsArray) {
      try {
        seriesService.updateOptOutStatus(seriesId.toString(), optout);
        result.addOk(seriesId.toString());
      } catch (NotFoundException e) {
        result.addNotFound(seriesId.toString());
      } catch (Exception e) {
        logger.error("Could not update opt out status of series {}: {}", seriesId.toString(),
                ExceptionUtils.getStackTrace(e));
        result.addServerError(seriesId.toString());
      }
    }
    return Response.ok(result.toJson()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesId}/metadata.json")
  @RestQuery(name = "getseriesmetadata", description = "Returns the series metadata as JSON", returnDescription = "Returns the series metadata as JSON", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series metadata as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getSeriesMetadata(@PathParam("seriesId") String series) throws UnauthorizedException,
          NotFoundException, SearchIndexException {
    Opt<Series> optSeries = indexService.getSeries(series, searchIndex);
    if (optSeries.isNone())
      return notFound("Cannot find a series with id '%s'.", series);

    MetadataList metadataList = new MetadataList();
    List<SeriesCatalogUIAdapter> catalogUIAdapters = indexService.getSeriesCatalogUIAdapters();
    catalogUIAdapters.remove(indexService.getCommonSeriesCatalogUIAdapter());
    for (SeriesCatalogUIAdapter adapter : catalogUIAdapters) {
      final Opt<MetadataCollection> optSeriesMetadata = adapter.getFields(series);
      if (optSeriesMetadata.isSome()) {
        metadataList.add(adapter.getFlavor(), adapter.getUITitle(), optSeriesMetadata.get());
      }
    }
    metadataList.add(indexService.getCommonSeriesCatalogUIAdapter(), getSeriesMetadata(optSeries.get()));
    return okJson(metadataList.toJSON());
  }

  /**
   * Loads the metadata for the given series
   *
   * @param series
   *          the source {@link Series}
   * @return a {@link MetadataCollection} instance with all the series metadata
   */
  @SuppressWarnings("unchecked")
  private MetadataCollection getSeriesMetadata(Series series) {
    MetadataCollection metadata = indexService.getCommonSeriesCatalogUIAdapter().getRawFields();

    MetadataField<?> title = metadata.getOutputFields().get(DublinCore.PROPERTY_TITLE.getLocalName());
    metadata.removeField(title);
    MetadataField<String> newTitle = MetadataUtils.copyMetadataField(title);
    newTitle.setValue(series.getTitle());
    metadata.addField(newTitle);

    MetadataField<?> subject = metadata.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
    metadata.removeField(subject);
    MetadataField<String> newSubject = MetadataUtils.copyMetadataField(subject);
    newSubject.setValue(series.getSubject());
    metadata.addField(newSubject);

    MetadataField<?> description = metadata.getOutputFields().get(DublinCore.PROPERTY_DESCRIPTION.getLocalName());
    metadata.removeField(description);
    MetadataField<String> newDescription = MetadataUtils.copyMetadataField(description);
    newDescription.setValue(series.getDescription());
    metadata.addField(newDescription);

    MetadataField<?> language = metadata.getOutputFields().get(DublinCore.PROPERTY_LANGUAGE.getLocalName());
    metadata.removeField(language);
    MetadataField<String> newLanguage = MetadataUtils.copyMetadataField(language);
    newLanguage.setValue(series.getLanguage());
    metadata.addField(newLanguage);

    MetadataField<?> rightsHolder = metadata.getOutputFields().get(DublinCore.PROPERTY_RIGHTS_HOLDER.getLocalName());
    metadata.removeField(rightsHolder);
    MetadataField<String> newRightsHolder = MetadataUtils.copyMetadataField(rightsHolder);
    newRightsHolder.setValue(series.getRightsHolder());
    metadata.addField(newRightsHolder);

    MetadataField<?> license = metadata.getOutputFields().get(DublinCore.PROPERTY_LICENSE.getLocalName());
    metadata.removeField(license);
    MetadataField<String> newLicense = MetadataUtils.copyMetadataField(license);
    newLicense.setValue(series.getLicense());
    metadata.addField(newLicense);

    MetadataField<?> organizers = metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName());
    metadata.removeField(organizers);
    MetadataField<List<String>> newOrganizers = MetadataUtils.copyMetadataField(organizers);
    newOrganizers.setValue(series.getOrganizers());
    metadata.addField(newOrganizers);

    MetadataField<?> contributors = metadata.getOutputFields().get(DublinCore.PROPERTY_CONTRIBUTOR.getLocalName());
    metadata.removeField(contributors);
    MetadataField<List<String>> newContributors = MetadataUtils.copyMetadataField(contributors);
    newContributors.setValue(series.getContributors());
    metadata.addField(newContributors);

    MetadataField<?> publishers = metadata.getOutputFields().get(DublinCore.PROPERTY_PUBLISHER.getLocalName());
    metadata.removeField(publishers);
    MetadataField<List<String>> newPublishers = MetadataUtils.copyMetadataField(publishers);
    newPublishers.setValue(series.getPublishers());
    metadata.addField(newPublishers);

    // Admin UI only field
    MetadataField<String> createdBy = MetadataField.createTextMetadataField("createdBy", Opt.<String> none(),
            "EVENTS.SERIES.DETAILS.METADATA.CREATED_BY", true, false, Opt.<Boolean> none(),
            Opt.<Map<String, String>> none(), Opt.<String> none(), Opt.some(CREATED_BY_UI_ORDER), Opt.<String> none());
    createdBy.setValue(series.getCreator());
    metadata.addField(createdBy);

    MetadataField<?> uid = metadata.getOutputFields().get(DublinCore.PROPERTY_IDENTIFIER.getLocalName());
    metadata.removeField(uid);
    MetadataField<String> newUID = MetadataUtils.copyMetadataField(uid);
    newUID.setValue(series.getIdentifier());
    metadata.addField(newUID);

    return metadata;
  }

  @PUT
  @Path("{seriesId}/metadata")
  @RestQuery(name = "updateseriesmetadata", description = "Update the series metadata with the one given JSON", returnDescription = "Returns OK if the metadata have been saved.", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, restParameters = { @RestParameter(name = "metadata", isRequired = true, type = RestParameter.Type.TEXT, description = "The list of metadata to update") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series metadata as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response updateSeriesMetadata(@PathParam("seriesId") String seriesID,
          @FormParam("metadata") String metadataJSON) throws UnauthorizedException, NotFoundException,
          SearchIndexException {
    try {
      MetadataList metadataList = indexService.updateAllSeriesMetadata(seriesID, metadataJSON, searchIndex);
      return okJson(metadataList.toJSON());
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      return RestUtil.R.serverError();
    }
  }

  @GET
  @Path("new/metadata")
  @RestQuery(name = "getNewMetadata", description = "Returns all the data related to the metadata tab in the new series modal as JSON", returnDescription = "All the data related to the series metadata tab as JSON", reponses = { @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the series metadata tab as JSON") })
  public Response getNewMetadata() {
    MetadataList metadataList = indexService.getMetadataListWithAllSeriesCatalogUIAdapters();
    Opt<MetadataCollection> metadataByAdapter = metadataList
            .getMetadataByAdapter(indexService.getCommonSeriesCatalogUIAdapter());
    if (metadataByAdapter.isSome()) {
      MetadataCollection collection = metadataByAdapter.get();
      safelyRemoveField(collection, "identifier");
      metadataList.add(indexService.getCommonSeriesCatalogUIAdapter(), collection);
    }
    return okJson(metadataList.toJSON());
  }

  private void safelyRemoveField(MetadataCollection collection, String fieldName) {
    MetadataField<?> metadataField = collection.getOutputFields().get(fieldName);
    if (metadataField != null) {
      collection.removeField(metadataField);
    }
  }

  @GET
  @Path("new/themes")
  @SuppressWarnings("unchecked")
  @RestQuery(name = "getNewThemes", description = "Returns all the data related to the themes tab in the new series modal as JSON", returnDescription = "All the data related to the series themes tab as JSON", reponses = { @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the series themes tab as JSON") })
  public Response getNewThemes() {
    ThemeSearchQuery query = new ThemeSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
    // need to set limit because elasticsearch limit results by 10 per default
    query.withLimit(Integer.MAX_VALUE);
    query.withOffset(0);
    query.sortByName(SearchQuery.Order.Ascending);
    SearchResult<Theme> results = null;
    try {
      results = searchIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the themes: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    JSONObject themesJson = new JSONObject();
    for (SearchResultItem<Theme> item : results.getItems()) {
      JSONObject themeInfoJson = new JSONObject();
      Theme theme = item.getSource();
      themeInfoJson.put("name", theme.getName());
      themeInfoJson.put("description", theme.getDescription());
      themesJson.put(theme.getIdentifier(), themeInfoJson);
    }
    return Response.ok(themesJson.toJSONString()).build();
  }

  @POST
  @Path("new")
  @RestQuery(name = "createNewSeries", description = "Creates a new series by the given metadata as JSON", returnDescription = "The created series id", restParameters = { @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Returns the created series id"),
          @RestResponse(responseCode = HttpServletResponse.SC_BAD_REQUEST, description = "he request could not be fulfilled due to the incorrect syntax of the request"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If user doesn't have rights to create the series") })
  public Response createNewSeries(@FormParam("metadata") String metadata) throws UnauthorizedException {
    String seriesId;
    try {
      seriesId = indexService.createSeries(metadata);
      return Response.created(URI.create(UrlSupport.concat(serverUrl, "admin-ng/series/", seriesId, "metadata.json")))
              .entity(seriesId).build();
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      return RestUtil.R.serverError();
    }
  }

  @DELETE
  @Path("{seriesId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteseries", description = "Delete a series.", returnDescription = "Ok if the series has been deleted.", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The id of the series to delete.", type = STRING), }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series has been deleted."),
          @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "The series could not be found.") })
  public Response deleteSeries(@PathParam("seriesId") String id) throws NotFoundException {
    try {
      indexService.removeSeries(id);
      return Response.ok().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to delete the series '{}' due to: {}", id, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @POST
  @Path("deleteSeries")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deletemultipleseries", description = "Deletes a json list of series by their given ids e.g. [\"Series-1\", \"Series-2\"]", returnDescription = "A JSON object with arrays that show whether a series was deleted, was not found or there was an error deleting it.", reponses = {
          @RestResponse(description = "Series have been deleted", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The list of ids could not be parsed into a json list.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response deleteMultipleSeries(String seriesIdsContent) throws NotFoundException {
    if (StringUtils.isBlank(seriesIdsContent)) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    JSONParser parser = new JSONParser();
    JSONArray seriesIdsArray;
    try {
      seriesIdsArray = (JSONArray) parser.parse(seriesIdsContent);
    } catch (org.json.simple.parser.ParseException e) {
      logger.error("Unable to parse '{}' because: {}", seriesIdsContent, ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.error("Unable to cast '{}' to a JSON array because: {}", seriesIdsContent, ExceptionUtils.getMessage(e));
      return Response.status(Status.BAD_REQUEST).build();
    }

    BulkOperationResult result = new BulkOperationResult();
    for (Object seriesId : seriesIdsArray) {
      try {
        indexService.removeSeries(seriesId.toString());
        result.addOk(seriesId.toString());
      } catch (NotFoundException e) {
        result.addNotFound(seriesId.toString());
      } catch (Exception e) {
        logger.error("Unable to remove the series '{}': {}", seriesId.toString(), ExceptionUtils.getStackTrace(e));
        result.addServerError(seriesId.toString());
      }
    }
    return Response.ok(result.toJson()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("series.json")
  @RestQuery(name = "listSeriesAsJson", description = "Returns the series matching the query parameters", returnDescription = "Returns the series search results as JSON", restParameters = {
          @RestParameter(name = "sortorganizer", isRequired = false, description = "The sort type to apply to the series organizer or organizers either Ascending or Descending.", type = STRING),
          @RestParameter(name = "sort", description = "The order instructions used to sort the query result. Must be in the form '<field name>:(ASC|DESC)'", isRequired = false, type = STRING),
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2,value2'", type = STRING),
          @RestParameter(name = "offset", isRequired = false, description = "The page offset", type = INTEGER, defaultValue = "0"),
          @RestParameter(name = "optedOut", isRequired = false, description = "Whether this series is opted out", type = BOOLEAN),
          @RestParameter(name = "limit", isRequired = false, description = "Results per page (max 100)", type = INTEGER, defaultValue = "100") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getSeries(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit, @QueryParam("optedOut") Boolean optedOut)
          throws UnauthorizedException {
    try {
      logger.debug("Requested series list");
      SeriesSearchQuery query = new SeriesSearchQuery(securityService.getOrganization().getId(),
              securityService.getUser());
      Option<String> optSort = Option.option(trimToNull(sort));

      if (offset != 0) {
        query.withOffset(offset);
      }

      // If limit is 0, we set the default limit
      query.withLimit(limit == 0 ? DEFAULT_LIMIT : limit);

      if (optedOut != null)
        query.withOptedOut(optedOut);

      Map<String, String> filters = RestUtils.parseFilter(filter);
      for (String name : filters.keySet()) {
        if (SeriesListQuery.FILTER_ACL_NAME.equals(name)) {
          query.withManagedAcl(filters.get(name));
        } else if (SeriesListQuery.FILTER_CONTRIBUTORS_NAME.equals(name)) {
          query.withContributor(filters.get(name));
        } else if (SeriesListQuery.FILTER_CREATIONDATE_NAME.equals(name)) {
          try {
            Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(filters.get(name));
            query.withCreatedFrom(fromAndToCreationRange.getA());
            query.withCreatedTo(fromAndToCreationRange.getB());
          } catch (IllegalArgumentException e) {
            return RestUtil.R.badRequest(e.getMessage());
          }
        } else if (SeriesListQuery.FILTER_CREATOR_NAME.equals(name)) {
          query.withCreator(filters.get(name));
        } else if (SeriesListQuery.FILTER_TEXT_NAME.equals(name)) {
          query.withText(QueryPreprocessor.sanitize(filters.get(name)));
        } else if (SeriesListQuery.FILTER_LANGUAGE_NAME.equals(name)) {
          query.withLanguage(filters.get(name));
        } else if (SeriesListQuery.FILTER_LICENSE_NAME.equals(name)) {
          query.withLicense(filters.get(name));
        } else if (SeriesListQuery.FILTER_ORGANIZERS_NAME.equals(name)) {
          query.withOrganizer(filters.get(name));
        } else if (SeriesListQuery.FILTER_SUBJECT_NAME.equals(name)) {
          query.withSubject(filters.get(name));
        } else if (SeriesListQuery.FILTER_TITLE_NAME.equals(name)) {
          query.withTitle(filters.get(name));
        }
      }

      if (optSort.isSome()) {
        Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
        for (SortCriterion criterion : sortCriteria) {

          switch (criterion.getFieldName()) {
            case SeriesIndexSchema.TITLE:
              query.sortByTitle(criterion.getOrder());
              break;
            case SeriesIndexSchema.CONTRIBUTORS:
              query.sortByContributors(criterion.getOrder());
              break;
            case SeriesIndexSchema.CREATOR:
              query.sortByOrganizers(criterion.getOrder());
              break;
            case SeriesIndexSchema.CREATED_DATE_TIME:
              query.sortByCreatedDateTime(criterion.getOrder());
              break;
            case SeriesIndexSchema.MANAGED_ACL:
              query.sortByManagedAcl(criterion.getOrder());
              break;
            default:
              logger.info("Unknown filter criteria {}", criterion.getFieldName());
              return Response.status(SC_BAD_REQUEST).build();
          }
        }
      }

      logger.trace("Using Query: " + query.toString());

      SearchResult<Series> result = searchIndex.getByQuery(query);
      if (logger.isDebugEnabled()) {
        logger.debug("Found {} results in {} ms", result.getDocumentCount(), result.getSearchTime());
      }

      List<JValue> series = new ArrayList<>();
      for (SearchResultItem<Series> item : result.getItems()) {
        List<Field> fields = new ArrayList<>();
        Series s = item.getSource();
        String sId = s.getIdentifier();
        fields.add(f("id", v(sId)));
        fields.add(f("optedOut", v(s.isOptedOut())));
        fields.add(f("title", v(s.getTitle(), Jsons.BLANK)));
        fields.add(f("organizers", arr($(s.getOrganizers()).map(Functions.stringToJValue))));
        fields.add(f("contributors", arr($(s.getContributors()).map(Functions.stringToJValue))));
        if (s.getCreator() != null) {
          fields.add(f("createdBy", v(s.getCreator())));
        }
        if (s.getCreatedDateTime() != null) {
          fields.add(f("creation_date", v(toUTC(s.getCreatedDateTime().getTime()), Jsons.BLANK)));
        }
        if (s.getLanguage() != null) {
          fields.add(f("language", v(s.getLanguage())));
        }
        if (s.getLicense() != null) {
          fields.add(f("license", v(s.getLicense())));
        }
        if (s.getRightsHolder() != null) {
          fields.add(f("rightsHolder", v(s.getRightsHolder())));
        }
        if (StringUtils.isNotBlank(s.getManagedAcl())) {
          fields.add(f("managedAcl", v(s.getManagedAcl())));
        }
        series.add(obj(fields));
      }
      logger.debug("Request done");

      return okJsonList(series, offset, limit, result.getHitCount());
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/properties")
  @RestQuery(name = "getSeriesProperties", description = "Returns the series properties", returnDescription = "Returns the series properties as JSON", pathParameters = { @RestParameter(name = "id", description = "ID of series", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getSeriesPropertiesAsJson(@PathParam("id") String seriesId) throws UnauthorizedException,
          NotFoundException {
    if (StringUtils.isBlank(seriesId)) {
      logger.warn("Series id parameter is blank '{}'.", seriesId);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      Map<String, String> properties = seriesService.getSeriesProperties(seriesId);
      JSONArray jsonProperties = new JSONArray();
      for (String name : properties.keySet()) {
        JSONObject property = new JSONObject();
        property.put(name, properties.get(name));
        jsonProperties.add(property);
      }
      return Response.ok(jsonProperties.toString()).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", e.getMessage());
    }
    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesId}/property/{propertyName}.json")
  @RestQuery(name = "getSeriesProperty", description = "Returns a series property value", returnDescription = "Returns the series property value", pathParameters = {
          @RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING),
          @RestParameter(name = "propertyName", description = "Name of series property", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getSeriesProperty(@PathParam("seriesId") String seriesId,
          @PathParam("propertyName") String propertyName) throws UnauthorizedException, NotFoundException {
    if (StringUtils.isBlank(seriesId)) {
      logger.warn("Series id parameter is blank '{}'.", seriesId);
      return Response.status(BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(propertyName)) {
      logger.warn("Series property name parameter is blank '{}'.", propertyName);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      String propertyValue = seriesService.getSeriesProperty(seriesId, propertyName);
      return Response.ok(propertyValue).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", ExceptionUtils.getStackTrace(e));
    }
    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
  }

  @POST
  @Path("/{seriesId}/property")
  @RestQuery(name = "updateSeriesProperty", description = "Updates a series property", returnDescription = "No content.", restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The property's name", type = TEXT),
          @RestParameter(name = "value", isRequired = true, description = "The property's value", type = TEXT) }, pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The access control list has been updated."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing in the request.") })
  public Response updateSeriesProperty(@PathParam("seriesId") String seriesId, @FormParam("name") String name,
          @FormParam("value") String value) throws UnauthorizedException {
    if (StringUtils.isBlank(seriesId)) {
      logger.warn("Series id parameter is blank '{}'.", seriesId);
      return Response.status(BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(name)) {
      logger.warn("Name parameter is blank '{}'.", name);
      return Response.status(BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(value)) {
      logger.warn("Series id parameter is blank '{}'.", value);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      seriesService.updateSeriesProperty(seriesId, name, value);
      return Response.status(NO_CONTENT).build();
    } catch (NotFoundException e) {
      return Response.status(NOT_FOUND).build();
    } catch (SeriesException e) {
      logger.warn("Could not update series property for series {} property {}:{} : {}", seriesId, name,
              value, ExceptionUtils.getStackTrace(e));
    }
    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
  }

  @DELETE
  @Path("{seriesId}/property/{propertyName}")
  @RestQuery(name = "deleteSeriesProperty", description = "Deletes a series property", returnDescription = "No Content", pathParameters = {
          @RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING),
          @RestParameter(name = "propertyName", description = "Name of series property", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The series property has been deleted."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series or property has not been found."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response deleteSeriesProperty(@PathParam("seriesId") String seriesId,
          @PathParam("propertyName") String propertyName) throws UnauthorizedException, NotFoundException {
    if (StringUtils.isBlank(seriesId)) {
      logger.warn("Series id parameter is blank '{}'.", seriesId);
      return Response.status(BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(propertyName)) {
      logger.warn("Series property name parameter is blank '{}'.", propertyName);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      seriesService.deleteSeriesProperty(seriesId, propertyName);
      return Response.status(NO_CONTENT).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not delete series '{}' property '{}' query: {}", seriesId, propertyName,
              ExceptionUtils.getStackTrace(e));
    }
    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
  }

  /**
   * Creates an ok response with the entity being the theme id and name.
   *
   * @param theme
   *          The theme to get the id and name from.
   * @return A {@link Response} with the theme id and name as json contents
   */
  private Response getSimpleThemeJsonResponse(Theme theme) {
    return okJson(obj(f(Long.toString(theme.getIdentifier()), v(theme.getName()))));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesId}/theme.json")
  @RestQuery(name = "getSeriesTheme", description = "Returns the series theme id and name as JSON", returnDescription = "Returns the series theme name and id as JSON", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series theme id and name as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series or theme has not been found") })
  public Response getSeriesTheme(@PathParam("seriesId") String seriesId) {
    Long themeId;
    try {
      Opt<Series> series = indexService.getSeries(seriesId, searchIndex);
      if (series.isNone())
        return notFound("Cannot find a series with id {}", seriesId);

      themeId = series.get().getTheme();
    } catch (SearchIndexException e) {
      logger.error("Unable to get series {}: {}", seriesId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }

    // If no theme is set return empty JSON
    if (themeId == null)
      return okJson(obj());

    try {
      Opt<Theme> themeOpt = getTheme(themeId);
      if (themeOpt.isNone())
        return notFound("Cannot find a theme with id {}", themeId);

      return getSimpleThemeJsonResponse(themeOpt.get());
    } catch (SearchIndexException e) {
      logger.error("Unable to get theme {}: {}", themeId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("{seriesId}/theme")
  @RestQuery(name = "updateSeriesTheme", description = "Update the series theme id", returnDescription = "Returns the id and name of the theme.", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, restParameters = { @RestParameter(name = "themeId", isRequired = true, type = RestParameter.Type.INTEGER, description = "The id of the theme for this series") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series theme has been updated and the theme id and name are returned as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series or theme has not been found"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response updateSeriesTheme(@PathParam("seriesId") String seriesID, @FormParam("themeId") long themeId)
          throws UnauthorizedException, NotFoundException {
    try {
      Opt<Theme> themeOpt = getTheme(themeId);
      if (themeOpt.isNone())
        return notFound("Cannot find a theme with id {}", themeId);

      seriesService.updateSeriesProperty(seriesID, THEME_KEY, Long.toString(themeId));
      return getSimpleThemeJsonResponse(themeOpt.get());
    } catch (SeriesException e) {
      logger.error("Unable to update series theme {}: {}", themeId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    } catch (SearchIndexException e) {
      logger.error("Unable to get theme {}: {}", themeId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("{seriesId}/theme")
  @RestQuery(name = "deleteSeriesTheme", description = "Removes the theme from the series", returnDescription = "Returns no content", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The series theme has been removed"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response deleteSeriesTheme(@PathParam("seriesId") String seriesID) throws UnauthorizedException,
          NotFoundException {
    try {
      seriesService.deleteSeriesProperty(seriesID, THEME_KEY);
      return Response.noContent().build();
    } catch (SeriesException e) {
      logger.error("Unable to remove theme from series {}: {}", seriesID, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/{seriesId}/access")
  @RestQuery(name = "applyAclToSeries", description = "Immediate application of an ACL to a series", returnDescription = "Status code", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series ID", type = STRING) }, restParameters = {
          @RestParameter(name = "acl", isRequired = true, description = "The ACL to apply", type = STRING),
          @RestParameter(name = "override", isRequired = false, defaultValue = "false", description = "If true the series ACL will take precedence over any existing episode ACL", type = BOOLEAN) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the given ACL"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error") })
  public Response applyAclToSeries(@PathParam("seriesId") String seriesId, @FormParam("acl") String acl,
          @DefaultValue("false") @FormParam("override") boolean override) throws SearchIndexException {

    AccessControlList accessControlList;
    try {
      accessControlList = AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      logger.warn("Unable to parse ACL '{}'", acl);
      return badRequest();
    }

    Opt<Series> series = indexService.getSeries(seriesId, searchIndex);
    if (series.isNone())
      return notFound("Cannot find a series with id {}", seriesId);

    if (hasProcessingEvents(seriesId)) {
      logger.warn("Can not update the ACL from series {}. Events being part of the series are currently processed.",
              seriesId);
      return conflict();
    }

    try {
      if (getAclService().applyAclToSeries(seriesId, accessControlList, override, Option.none()))
        return ok();
      else {
        logger.warn("Unable to find series '{}' to apply the ACL.", seriesId);
        return notFound();
      }
    } catch (AclServiceException e) {
      logger.error("Error applying acl to series {}", seriesId);
      return serverError();
    }
  }

  /**
   * Check if the series with the given Id has events being currently processed
   *
   * @param seriesId
   *          the series Id
   * @return true if events being part of the series are currently processed
   */
  private boolean hasProcessingEvents(String seriesId) {
    EventSearchQuery query = new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
    long elementsCount = 0;
    query.withSeriesId(seriesId);

    try {
      query.withWorkflowState(WorkflowInstance.WorkflowState.RUNNING.toString());
      SearchResult<Event> events = searchIndex.getByQuery(query);
      elementsCount = events.getHitCount();
      query.withWorkflowState(WorkflowInstance.WorkflowState.INSTANTIATED.toString());
      events = searchIndex.getByQuery(query);
      elementsCount += events.getHitCount();
    } catch (SearchIndexException e) {
      logger.warn("Could not perform search query: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return elementsCount > 0;
  }

  @GET
  @Path("{seriesId}/hasEvents.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "hasEvents", description = "Check if given series has events", returnDescription = "true if series has events, otherwise false", pathParameters = {
    @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = Type.STRING) }, reponses = {
    @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
    @RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."),
    @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getSeriesEvents(@PathParam("seriesId") String seriesId) throws Exception {
    if (StringUtils.isBlank(seriesId))
      return RestUtil.R.badRequest("Path parameter series ID is missing");

    long elementsCount = 0;

    try {
      EventSearchQuery query = new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
      query.withSeriesId(seriesId);
      SearchResult<Event> result = searchIndex.getByQuery(query);
      elementsCount = result.getHitCount();
    } catch (SearchIndexException e) {
      logger.warn("Could not perform search query", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    JSONObject jsonReturnObj = new JSONObject();
    jsonReturnObj.put("hasEvents", elementsCount > 0);
    return Response.ok(jsonReturnObj.toString()).build();
  }

  /**
   * Get a single theme
   *
   * @param id
   *          the theme id
   * @return a theme or none if not found, wrapped in an option
   * @throws SearchIndexException
   */
  private Opt<Theme> getTheme(long id) throws SearchIndexException {
    SearchResult<Theme> result = searchIndex.getByQuery(new ThemeSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withIdentifier(id));
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find theme with id {}", id);
      return Opt.<Theme> none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @GET
  @Path("configuration.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getseriesconfiguration", description = "Get the series configuration", returnDescription = "List of configuration keys", reponses = {
    @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
    @RestResponse(responseCode = SC_NOT_FOUND, description = "If the series has not been found."),
    @RestResponse(responseCode = SC_OK, description = "The access information ") })
  public Response getSeriesOptions() {
    JSONObject jsonReturnObj = new JSONObject();
    jsonReturnObj.put("deleteSeriesWithEventsAllowed", deleteSeriesWithEventsAllowed);
    return Response.ok(jsonReturnObj.toString()).build();
  }
}
