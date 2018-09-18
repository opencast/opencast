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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.external.util.AclUtils;
import org.opencastproject.external.util.ExternalMetadataUtils;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesIndexSchema;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.util.RequestUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons.Functions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0 })
@RestService(name = "externalapiseries", title = "External API Series Service", notes = "", abstractText = "Provides resources and operations related to the series")
public class SeriesEndpoint {

  private static final int CREATED_BY_UI_ORDER = 9;
  private static final int DEFAULT_LIMIT = 100;

  private static final Logger logger = LoggerFactory.getLogger(SeriesEndpoint.class);

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /* OSGi service references */
  private ExternalIndex externalIndex;
  private IndexService indexService;
  private SecurityService securityService;
  private SeriesService seriesService;

  /** OSGi DI */
  void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  /** OSGi DI */
  void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI */
  void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi activation method */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Series Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  @GET
  @Path("")
  @RestQuery(name = "getseries", description = "Returns a list of series.", returnDescription = "", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.", type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(description = "A (potentially empty) list of series is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getSeriesList(@HeaderParam("Accept") String acceptHeader, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("order") String order, @QueryParam("offset") int offset,
          @QueryParam("limit") int limit) throws UnauthorizedException {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    try {
      SeriesSearchQuery query = new SeriesSearchQuery(securityService.getOrganization().getId(),
              securityService.getUser());
      Option<String> optSort = Option.option(trimToNull(sort));

      if (offset > 0) {
        query.withOffset(offset);
      }

      // If limit is 0, we set the default limit
      query.withLimit(limit < 1 ? DEFAULT_LIMIT : limit);

      // Parse the filters
      if (StringUtils.isNotBlank(filter)) {
        for (String f : filter.split(",")) {
          String[] filterTuple = f.split(":");
          if (filterTuple.length != 2) {
            logger.info("No value for filter {} in filters list: {}", filterTuple[0], filter);
            continue;
          }
          String name = filterTuple[0];

          String value;
          if (!requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
            // MH-13038 - 1.1.0 and higher support semi-colons in values
            value = f.substring(name.length() + 1);
          } else {
            value = filterTuple[1];
          }

          if ("managedAcl".equals(name)) {
            query.withAccessPolicy(value);
          } else if ("contributors".equals(name)) {
            query.withContributor(value);
          } else if ("CreationDate".equals(name)) {
            if (name.split("/").length == 2) {
              try {
                Tuple<Date, Date> fromAndToCreationRange = getFromAndToCreationRange(name.split("/")[0],
                        name.split("/")[1]);
                query.withCreatedFrom(fromAndToCreationRange.getA());
                query.withCreatedTo(fromAndToCreationRange.getB());
              } catch (IllegalArgumentException e) {
                return RestUtil.R.badRequest(e.getMessage());
              }

            }
            query.withCreator(value);
          } else if ("Creator".equals(name)) {
            query.withCreator(value);
          } else if ("textFilter".equals(name)) {
            query.withText("*" + value + "*");
          } else if ("language".equals(name)) {
            query.withLanguage(value);
          } else if ("license".equals(name)) {
            query.withLicense(value);
          } else if ("organizers".equals(name)) {
            query.withOrganizer(value);
          } else if ("subject".equals(name)) {
            query.withSubject(value);
          } else if ("title".equals(name)) {
            query.withTitle(value);
          } else if (!requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
            // additional filters only available with Version 1.1.0 or higher
            if ("identifier".equals(name)) {
              query.withIdentifier(value);
            } else if ("description".equals(name)) {
              query.withDescription(value);
            } else if ("creator".equals(name)) {
              query.withCreator(value);
            } else if ("publishers".equals(name)) {
              query.withPublisher(value);
            } else if ("rightsholder".equals(name)) {
              query.withRightsHolder(value);
            } else {
              logger.warn("Unknown filter criteria {}", name);
              return Response.status(SC_BAD_REQUEST).build();
            }
          }
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
            case EventIndexSchema.CREATED:
              query.sortByCreatedDateTime(criterion.getOrder());
              break;
            default:
              logger.info("Unknown sort criteria {}", criterion.getFieldName());
              return Response.status(SC_BAD_REQUEST).build();
          }
        }
      }

      logger.trace("Using Query: " + query.toString());

      SearchResult<Series> result = externalIndex.getByQuery(query);

      return ApiResponses.Json.ok(requestedVersion, arr($(result.getItems()).map(new Fn<SearchResultItem<Series>, JValue>() {
        @Override
        public JValue apply(SearchResultItem<Series> a) {
          final Series s = a.getSource();
          JValue subjects;
          if (s.getSubject() == null) {
            subjects = arr();
          } else {
            subjects = arr(splitSubjectIntoArray(s.getSubject()));
          }
          Date createdDate = s.getCreatedDateTime();
          JObject result;
          if (requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
            result = obj(
                    f("identifier", v(s.getIdentifier())),
                    f("title", v(s.getTitle())),
                    f("creator", v(s.getCreator(), BLANK)),
                    f("created", v(createdDate != null ? toUTC(createdDate.getTime()) : null, BLANK)),
                    f("subjects", subjects),
                    f("contributors", arr($(s.getContributors()).map(Functions.stringToJValue))),
                    f("organizers", arr($(s.getOrganizers()).map(Functions.stringToJValue))),
                    f("publishers", arr($(s.getPublishers()).map(Functions.stringToJValue))));
          }
          else {
            result = obj(
                    f("identifier", v(s.getIdentifier())),
                    f("title", v(s.getTitle())),
                    f("description", v(s.getDescription(), BLANK)),
                    f("creator", v(s.getCreator(), BLANK)),
                    f("created", v(createdDate != null ? toUTC(createdDate.getTime()) : null, BLANK)),
                    f("subjects", subjects),
                    f("contributors", arr($(s.getContributors()).map(Functions.stringToJValue))),
                    f("organizers", arr($(s.getOrganizers()).map(Functions.stringToJValue))),
                    f("language", v(s.getLanguage(), BLANK)),
                    f("license", v(s.getLicense(), BLANK)),
                    f("rightsholder", v(s.getRightsHolder(), BLANK)),
                    f("publishers", arr($(s.getPublishers()).map(Functions.stringToJValue))));
          }
          return result;

        }
      }).toList()));
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("{seriesId}")
  @RestQuery(name = "getseries", description = "Returns a single series.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The series is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getSeries(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id)
          throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    for (final Series s : indexService.getSeries(id, externalIndex)) {
      JValue subjects;
      if (s.getSubject() == null) {
        subjects = arr();
      } else {
        subjects = arr(splitSubjectIntoArray(s.getSubject()));
      }
      Date createdDate = s.getCreatedDateTime();
      JValue responseContent;
      if (requestedVersion.isSmallerThan(ApiVersion.VERSION_1_1_0)) {
        responseContent = obj(
                f("identifier", v(s.getIdentifier())),
                f("title", v(s.getTitle())),
                f("description", v(s.getDescription(), BLANK)),
                f("creator", v(s.getCreator(), BLANK)),
                f("subjects", subjects),
                f("organization", v(s.getOrganization())),
                f("created", v(createdDate != null ? toUTC(createdDate.getTime()) : null, BLANK)),
                f("contributors", arr($(s.getContributors()).map(Functions.stringToJValue))),
                f("organizers", arr($(s.getOrganizers()).map(Functions.stringToJValue))),
                f("publishers", arr($(s.getPublishers()).map(Functions.stringToJValue))),
                f("opt_out", v(s.isOptedOut())));
      }
      else {
        responseContent = obj(
                f("identifier", v(s.getIdentifier())),
                f("title", v(s.getTitle())),
                f("description", v(s.getDescription(), BLANK)),
                f("creator", v(s.getCreator(), BLANK)),
                f("subjects", subjects),
                f("organization", v(s.getOrganization())),
                f("created", v(createdDate != null ? toUTC(createdDate.getTime()) : null, BLANK)),
                f("contributors", arr($(s.getContributors()).map(Functions.stringToJValue))),
                f("organizers", arr($(s.getOrganizers()).map(Functions.stringToJValue))),
                f("publishers", arr($(s.getPublishers()).map(Functions.stringToJValue))),
                f("language", v(s.getLanguage(), BLANK)),
                f("license", v(s.getLicense(), BLANK)),
                f("rightsholder", v(s.getRightsHolder(), BLANK)),
                f("opt_out", v(s.isOptedOut())));
      }
      return ApiResponses.Json.ok(requestedVersion, responseContent);
    }
    return ApiResponses.notFound("Cannot find an series with id '%s'.", id);
  }

  private List<JValue> splitSubjectIntoArray(final String subject) {
    return com.entwinemedia.fn.Stream.$(subject.split(",")).map(new Fn<String, JValue>() {
      @Override
      public JValue apply(String a) {
        return v(a.trim());
      }
    }).toList();
  }

  @GET
  @Path("{seriesId}/metadata")
  @RestQuery(name = "getseriesmetadata", description = "Returns a series' metadata of all types or returns a series' metadata collection of the given type when the query string parameter type is specified. For each metadata catalog there is a unique property called the flavor such as dublincore/series so the type in this example would be 'dublincore/series'", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = false, description = "The type of metadata to return", type = STRING) }, reponses = {
                          @RestResponse(description = "The series' metadata are returned.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getSeriesMetadata(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id,
          @QueryParam("type") String type) throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    if (StringUtils.trimToNull(type) == null) {
      return getAllMetadata(id, requestedVersion);
    } else {
      return getMetadataByType(id, type, requestedVersion);
    }
  }

  private Response getAllMetadata(String id, ApiVersion requestedVersion) throws SearchIndexException {
    Opt<Series> optSeries = indexService.getSeries(id, externalIndex);
    if (optSeries.isNone())
      return ApiResponses.notFound("Cannot find a series with id '%s'.", id);

    MetadataList metadataList = new MetadataList();
    List<SeriesCatalogUIAdapter> catalogUIAdapters = indexService.getSeriesCatalogUIAdapters();
    catalogUIAdapters.remove(indexService.getCommonSeriesCatalogUIAdapter());
    for (SeriesCatalogUIAdapter adapter : catalogUIAdapters) {
      final Opt<MetadataCollection> optSeriesMetadata = adapter.getFields(id);
      if (optSeriesMetadata.isSome()) {
        metadataList.add(adapter.getFlavor(), adapter.getUITitle(), optSeriesMetadata.get());
      }
    }
    MetadataCollection collection = getSeriesMetadata(optSeries.get());
    ExternalMetadataUtils.changeSubjectToSubjects(collection);
    ExternalMetadataUtils.changeTypeOrderedTextToText(collection);
    metadataList.add(indexService.getCommonSeriesCatalogUIAdapter(), collection);
    return ApiResponses.Json.ok(requestedVersion, metadataList.toJSON());
  }

  private Response getMetadataByType(String id, String type, ApiVersion requestedVersion) throws SearchIndexException {
    Opt<Series> optSeries = indexService.getSeries(id, externalIndex);
    if (optSeries.isNone())
      return ApiResponses.notFound("Cannot find a series with id '%s'.", id);

    // Try the main catalog first as we load it from the index.
    if (typeMatchesSeriesCatalogUIAdapter(type, indexService.getCommonSeriesCatalogUIAdapter())) {
      MetadataCollection collection = getSeriesMetadata(optSeries.get());
      ExternalMetadataUtils.changeSubjectToSubjects(collection);
      ExternalMetadataUtils.changeTypeOrderedTextToText(collection);
      return ApiResponses.Json.ok(requestedVersion, collection.toJSON());
    }

    // Try the other catalogs
    List<SeriesCatalogUIAdapter> catalogUIAdapters = indexService.getSeriesCatalogUIAdapters();
    catalogUIAdapters.remove(indexService.getCommonSeriesCatalogUIAdapter());

    for (SeriesCatalogUIAdapter adapter : catalogUIAdapters) {
      if (typeMatchesSeriesCatalogUIAdapter(type, adapter)) {
        final Opt<MetadataCollection> optSeriesMetadata = adapter.getFields(id);
        if (optSeriesMetadata.isSome()) {
          return ApiResponses.Json.ok(requestedVersion, optSeriesMetadata.get().toJSON());
        }
      }
    }
    return ApiResponses.notFound("Cannot find a catalog with type '%s' for series with id '%s'.", type, id);
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
    MetadataField<String> newOrganizers = MetadataUtils.copyMetadataField(organizers);
    newOrganizers.setValue(StringUtils.join(series.getOrganizers(), ", "));
    metadata.addField(newOrganizers);

    MetadataField<?> contributors = metadata.getOutputFields().get(DublinCore.PROPERTY_CONTRIBUTOR.getLocalName());
    metadata.removeField(contributors);
    MetadataField<String> newContributors = MetadataUtils.copyMetadataField(contributors);
    newContributors.setValue(StringUtils.join(series.getContributors(), ", "));
    metadata.addField(newContributors);

    MetadataField<?> publishers = metadata.getOutputFields().get(DublinCore.PROPERTY_PUBLISHER.getLocalName());
    metadata.removeField(publishers);
    MetadataField<String> newPublishers = MetadataUtils.copyMetadataField(publishers);
    newPublishers.setValue(StringUtils.join(series.getPublishers(), ", "));
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

    ExternalMetadataUtils.removeCollectionList(metadata);

    return metadata;
  }

  /**
   * Checks if a flavor type matches a series catalog's flavor type.
   *
   * @param type
   *          The flavor type to compare against the catalog's flavor
   * @param catalog
   *          The catalog to check if it matches the flavor.
   * @return True if it matches.
   */
  private boolean typeMatchesSeriesCatalogUIAdapter(String type, SeriesCatalogUIAdapter catalog) {
    if (StringUtils.trimToNull(type) == null) {
      return false;
    }
    MediaPackageElementFlavor catalogFlavor = MediaPackageElementFlavor.parseFlavor(catalog.getFlavor());
    try {
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(type);
      return flavor.equals(catalogFlavor);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Opt<MediaPackageElementFlavor> getFlavor(String flavorString) {
    try {
      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
      return Opt.some(flavor);
    } catch (IllegalArgumentException e) {
      return Opt.none();
    }
  }

  @PUT
  @Path("{seriesId}/metadata")
  @RestQuery(name = "updateseriesmetadata", description = "Update a series' metadata of the given type. For a metadata catalog there is the flavor such as 'dublincore/series' and this is the unique type.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to update", type = STRING),
                  @RestParameter(name = "metadata", description = "Series metadata as Form param", isRequired = true, type = STRING) }, reponses = {
                          @RestResponse(description = "The series' metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateSeriesMetadata(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id,
          @QueryParam("type") String type, @FormParam("metadata") String metadataJSON) throws Exception {
    if (StringUtils.trimToNull(metadataJSON) == null) {
      return RestUtil.R.badRequest("Unable to update metadata for series as the metadata provided is empty.");
    }
    Map<String, String> updatedFields;
    try {
      updatedFields = RequestUtils.getKeyValueMap(metadataJSON);
    } catch (ParseException e) {
      logger.debug("Unable to update series '{}' with metadata type '{}' and content '{}' because: {}",
              id, type, metadataJSON, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.badRequest(String.format("Unable to parse metadata fields as json from '%s' because '%s'",
              metadataJSON, ExceptionUtils.getStackTrace(e)));
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    }

    if (updatedFields == null || updatedFields.size() == 0) {
      return RestUtil.R.badRequest(
              String.format("Unable to parse metadata fields as json from '%s' because there were no fields to update.",
                      metadataJSON));
    }

    Opt<MetadataCollection> optCollection = Opt.none();
    SeriesCatalogUIAdapter adapter = null;

    Opt<Series> optSeries = indexService.getSeries(id, externalIndex);
    if (optSeries.isNone())
      return ApiResponses.notFound("Cannot find a series with id '%s'.", id);

    MetadataList metadataList = new MetadataList();

    // Try the main catalog first as we load it from the index.
    if (typeMatchesSeriesCatalogUIAdapter(type, indexService.getCommonSeriesCatalogUIAdapter())) {
      optCollection = Opt.some(getSeriesMetadata(optSeries.get()));
      adapter = indexService.getCommonSeriesCatalogUIAdapter();
    } else {
      metadataList.add(indexService.getCommonSeriesCatalogUIAdapter(), getSeriesMetadata(optSeries.get()));
    }

    // Try the other catalogs
    List<SeriesCatalogUIAdapter> catalogUIAdapters = indexService.getSeriesCatalogUIAdapters();
    catalogUIAdapters.remove(indexService.getCommonSeriesCatalogUIAdapter());
    if (catalogUIAdapters.size() > 0) {
      for (SeriesCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
        if (typeMatchesSeriesCatalogUIAdapter(type, catalogUIAdapter)) {
          optCollection = catalogUIAdapter.getFields(id);
          adapter = catalogUIAdapter;
        } else {
          Opt<MetadataCollection> current = catalogUIAdapter.getFields(id);
          if (current.isSome()) {
            metadataList.add(catalogUIAdapter, current.get());
          }
        }
      }
    }

    if (optCollection.isNone()) {
      return ApiResponses.notFound("Cannot find a catalog with type '%s' for series with id '%s'.", type, id);
    }

    MetadataCollection collection = optCollection.get();

    for (String key : updatedFields.keySet()) {
      MetadataField<?> field = collection.getOutputFields().get(key);
      if (field == null) {
        return ApiResponses.notFound(
                "Cannot find a metadata field with id '%s' from event with id '%s' and the metadata type '%s'.", key,
                id, type);
      } else if (field.isRequired() && StringUtils.isBlank(updatedFields.get(key))) {
        return R.badRequest(String.format(
                "The series metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
                key, type));
      }
      collection.removeField(field);
      collection.addField(MetadataField.copyMetadataFieldWithValue(field, updatedFields.get(key)));
    }

    metadataList.add(adapter, collection);
    indexService.updateAllSeriesMetadata(id, metadataList, externalIndex);
    return ApiResponses.Json.ok(acceptHeader, "");
  }

  @DELETE
  @Path("{seriesId}/metadata")
  @RestQuery(name = "deleteseriesmetadata", description = "Deletes a series' metadata catalog of the given type. All fields and values of that catalog will be deleted.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "type", isRequired = true, description = "The type of metadata to delete", type = STRING) }, reponses = {
                          @RestResponse(description = "The metadata have been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The main metadata catalog dublincore/series cannot be deleted as it has mandatory fields.", responseCode = HttpServletResponse.SC_FORBIDDEN),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteSeriesMetadataByType(@HeaderParam("Accept") String acceptHeader,
          @PathParam("seriesId") String id, @QueryParam("type") String type) throws Exception {
    if (StringUtils.trimToNull(type) == null) {
      return RestUtil.R
              .badRequest(String.format("A type of catalog needs to be specified for series '%s' to delete it.", id));
    }

    Opt<MediaPackageElementFlavor> flavor = getFlavor(type);

    if (flavor.isNone()) {
      return RestUtil.R.badRequest(
              String.format("Unable to parse flavor '%s' it should look something like dublincore/series.", type));
    }

    if (typeMatchesSeriesCatalogUIAdapter(type, indexService.getCommonSeriesCatalogUIAdapter())) {
      return Response
              .status(Status.FORBIDDEN).entity(String
                      .format("Unable to delete mandatory metadata catalog with type '%s' for series '%s'", type, id))
              .build();
    }

    Opt<Series> optSeries = indexService.getSeries(id, externalIndex);
    if (optSeries.isNone())
      return ApiResponses.notFound("Cannot find a series with id '%s'.", id);

    try {
      indexService.removeCatalogByFlavor(optSeries.get(), MediaPackageElementFlavor.parseFlavor(type));
    } catch (NotFoundException e) {
      return ApiResponses.notFound(e.getMessage());
    }
    return Response.noContent().build();
  }

  @GET
  @Path("{seriesId}/acl")
  @RestQuery(name = "getseriesacl", description = "Returns a series' access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The series' access policy is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getSeriesAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id) throws Exception {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    JSONParser parser = new JSONParser();
    for (final Series series : indexService.getSeries(id, externalIndex)) {
      // The ACL is stored as JSON string in the index. Parse it and extract the part we want to have in the API.
      JSONObject acl = (JSONObject) parser.parse(series.getAccessPolicy());
      return ApiResponses.Json.ok(requestedVersion, ((JSONArray) ((JSONObject) acl.get("acl")).get("ace")).toJSONString());
    }

    return ApiResponses.notFound("Cannot find an series with id '%s'.", id);
  }

  @GET
  @Path("{seriesId}/properties")
  @RestQuery(name = "getseriesproperties", description = "Returns a series' properties", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The series' properties are returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getSeriesProperties(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id) throws Exception {
    if (indexService.getSeries(id, externalIndex).isSome()) {
      final Map<String, String> properties = seriesService.getSeriesProperties(id);

      return ApiResponses.Json.ok(acceptHeader, obj($(properties.entrySet()).map(new Fn<Entry<String, String>, Field>() {
                @Override
                public Field apply(Entry<String, String> a) {
                  return f(a.getKey(), v(a.getValue(), BLANK));
                }
              }).toList()));
    } else {
      return ApiResponses.notFound("Cannot find an series with id '%s'.", id);
    }
  }

  @DELETE
  @Path("{seriesId}")
  @RestQuery(name = "deleteseries", description = "Deletes a series.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The series has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteSeries(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String id)
          throws NotFoundException {
    try {
      indexService.removeSeries(id);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find a series with id '%s'.", id);
    } catch (Exception e) {
      logger.error("Unable to delete the series '{}' due to: {}", id, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
  }

  @PUT
  @Path("{seriesId}")
  @RestQuery(name = "updateallseriesmetadata", description = "Update all series metadata.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "metadata", description = "Series metadata as Form param", isRequired = true, type = STRING) }, reponses = {
                          @RestResponse(description = "The series' metadata have been updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateSeriesMetadata(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String seriesID,
          @FormParam("metadata") String metadataJSON)
          throws UnauthorizedException, NotFoundException, SearchIndexException {
    try {
      MetadataList metadataList = indexService.updateAllSeriesMetadata(seriesID, metadataJSON, externalIndex);
      return ApiResponses.Json.ok(acceptHeader, metadataList.toJSON());
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to update series '{}' with metadata '{}' because: {}",
              seriesID, metadataJSON, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.badRequest(e.getMessage());
    } catch (IndexServiceException e) {
      logger.error("Unable to update series '{}' with metadata '{}' because: {}",
              seriesID, metadataJSON, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }
  }

  @POST
  @Path("")
  @RestQuery(name = "createseries", description = "Creates a series.", returnDescription = "", restParameters = {
          @RestParameter(name = "metadata", isRequired = true, description = "Series metadata", type = STRING),
          @RestParameter(name = "acl", description = "A collection of roles with their possible action", isRequired = true, type = STRING),
          @RestParameter(name = "theme", description = "The theme ID to be applied to the series", isRequired = false, type = STRING) }, reponses = {
                  @RestResponse(description = "A new series is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The request is invalid or inconsistent..", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "The user doesn't have the rights to create the series.", responseCode = HttpServletResponse.SC_UNAUTHORIZED) })
  public Response createNewSeries(@HeaderParam("Accept") String acceptHeader,
          @FormParam("metadata") String metadataParam, @FormParam("acl") String aclParam,
          @FormParam("theme") String themeIdParam) throws UnauthorizedException, NotFoundException {
    if (isBlank(metadataParam))
      return R.badRequest("Required parameter 'metadata' is missing or invalid");

    if (isBlank(aclParam))
      return R.badRequest("Required parameter 'acl' is missing or invalid");

    MetadataList metadataList;
    try {
      metadataList = deserializeMetadataList(metadataParam);
    } catch (ParseException e) {
      logger.debug("Unable to parse series metadata '{}' because: {}", metadataParam, ExceptionUtils.getStackTrace(e));
      return R.badRequest(String.format("Unable to parse metadata because '%s'", e.toString()));
    } catch (NotFoundException e) {
      // One of the metadata fields could not be found in the catalogs or one of the catalogs cannot be found.
      return R.badRequest(e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to create series with metadata '{}' because: {}", metadataParam,
              ExceptionUtils.getStackTrace(e));
      return R.badRequest(e.getMessage());
    }
    Map<String, String> options = new TreeMap<>();
    Opt<Long> optThemeId = Opt.none();
    if (StringUtils.trimToNull(themeIdParam) != null) {
      try {
        Long themeId = Long.parseLong(themeIdParam);
        optThemeId = Opt.some(themeId);
      } catch (NumberFormatException e) {
        return R.badRequest(String.format("Unable to parse the theme id '%s' into a number", themeIdParam));
      }
    }
    AccessControlList acl;
    try {
      acl = AclUtils.deserializeJsonToAcl(aclParam, false);
    } catch (ParseException e) {
      logger.debug("Unable to parse acl '{}' because: '{}'", aclParam, ExceptionUtils.getStackTrace(e));
      return R.badRequest(String.format("Unable to parse acl '%s' because '%s'", aclParam, e.getMessage()));
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to create new series with acl '{}' because: '{}'", aclParam,
              ExceptionUtils.getStackTrace(e));
      return R.badRequest(e.getMessage());
    }

    try {
      String seriesId = indexService.createSeries(metadataList, options, Opt.some(acl), optThemeId);
      return ApiResponses.Json.created(acceptHeader, URI.create(getSeriesUrl(seriesId)),
                                       obj(f("identifier", v(seriesId, BLANK))));
    } catch (IndexServiceException e) {
      logger.error("Unable to create series with metadata '{}', acl '{}', theme '{}' because: ",
              metadataParam, aclParam, themeIdParam, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Change the simplified fields of key values provided to the external api into a {@link MetadataList}.
   *
   * @param json
   *          The json string that contains an array of metadata field lists for the different catalogs.
   * @return A {@link MetadataList} with the fields populated with the values provided.
   * @throws ParseException
   *           Thrown if unable to parse the json string.
   * @throws NotFoundException
   *           Thrown if unable to find the catalog or field that the json refers to.
   */
  protected MetadataList deserializeMetadataList(String json) throws ParseException, NotFoundException {
    MetadataList metadataList = new MetadataList();
    JSONParser parser = new JSONParser();
    JSONArray jsonCatalogs = (JSONArray) parser.parse(json);
    for (int i = 0; i < jsonCatalogs.size(); i++) {
      JSONObject catalog = (JSONObject) jsonCatalogs.get(i);
      if (catalog.get("flavor") == null || StringUtils.isBlank(catalog.get("flavor").toString())) {
        throw new IllegalArgumentException(
                "Unable to create new series as no flavor was given for one of the metadata collections");
      }
      String flavorString = catalog.get("flavor").toString();

      MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(flavorString);

      MetadataCollection collection = null;
      SeriesCatalogUIAdapter adapter = null;
      for (SeriesCatalogUIAdapter seriesCatalogUIAdapter : indexService.getSeriesCatalogUIAdapters()) {
        MediaPackageElementFlavor catalogFlavor = MediaPackageElementFlavor
                .parseFlavor(seriesCatalogUIAdapter.getFlavor());
        if (catalogFlavor.equals(flavor)) {
          adapter = seriesCatalogUIAdapter;
          collection = seriesCatalogUIAdapter.getRawFields();
        }
      }

      if (collection == null) {
        throw new IllegalArgumentException(
                String.format("Unable to find an SeriesCatalogUIAdapter with Flavor '%s'", flavorString));
      }

      String fieldsJson = catalog.get("fields").toString();
      if (StringUtils.trimToNull(fieldsJson) != null) {
        Map<String, String> fields = RequestUtils.getKeyValueMap(fieldsJson);
        for (String key : fields.keySet()) {
          if ("subjects".equals(key)) {
            MetadataField<?> field = collection.getOutputFields().get("subject");
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            collection.removeField(field);
            try {
              JSONArray subjects = (JSONArray) parser.parse(fields.get(key));
              collection.addField(
                      MetadataField.copyMetadataFieldWithValue(field, StringUtils.join(subjects.iterator(), ",")));
            } catch (ParseException e) {
              throw new IllegalArgumentException(
                      String.format("Unable to parse the 'subjects' metadata array field because: %s", e.toString()));
            }
          } else {
            MetadataField<?> field = collection.getOutputFields().get(key);
            if (field == null) {
              throw new NotFoundException(String.format(
                      "Cannot find a metadata field with id '%s' from Catalog with Flavor '%s'.", key, flavorString));
            }
            collection.removeField(field);
            collection.addField(MetadataField.copyMetadataFieldWithValue(field, fields.get(key)));
          }
        }
      }
      metadataList.add(adapter, collection);
    }
    return metadataList;
  }

  @PUT
  @Path("{seriesId}/acl")
  @RestQuery(name = "updateseriesacl", description = "Updates a series' access policy.", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "acl", isRequired = true, description = "Access policy", type = STRING) }, reponses = {
                          @RestResponse(description = "The access control list for the specified series is updated.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateSeriesAcl(@HeaderParam("Accept") String acceptHeader, @PathParam("seriesId") String seriesID,
          @FormParam("acl") String aclJson) throws NotFoundException, SeriesException, UnauthorizedException {
    if (isBlank(aclJson))
      return R.badRequest("Missing form parameter 'acl'");

    JSONParser parser = new JSONParser();
    JSONArray acl;
    try {
      acl = (JSONArray) parser.parse(aclJson);
    } catch (ParseException e) {
      logger.debug("Could not parse ACL ({}): {}", aclJson, getStackTrace(e));
      return R.badRequest("Could not parse ACL");
    }

    List<AccessControlEntry> accessControlEntries = $(acl.toArray()).map(new Fn<Object, AccessControlEntry>() {
      @Override
      public AccessControlEntry apply(Object a) {
        JSONObject ace = (JSONObject) a;
        return new AccessControlEntry((String) ace.get("role"), (String) ace.get("action"), (boolean) ace.get("allow"));
      }
    }).toList();

    seriesService.updateAccessControl(seriesID, new AccessControlList(accessControlEntries));
    return ApiResponses.Json.ok(acceptHeader, aclJson);
  }

  @SuppressWarnings("unchecked")
  @PUT
  @Path("{seriesId}/properties")
  @RestQuery(name = "updateseriesproperties", description = "Updates a series' properties", returnDescription = "", pathParameters = {
          @RestParameter(name = "seriesId", description = "The series id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "properties", isRequired = true, description = "Series properties", type = STRING) }, reponses = {
                          @RestResponse(description = "Successfully updated the series' properties.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The specified series does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateSeriesProperties(@HeaderParam("Accept") String acceptHeader,
          @PathParam("seriesId") String seriesID, @FormParam("properties") String propertiesJson)
          throws NotFoundException, SeriesException, UnauthorizedException {
    if (StringUtils.isBlank(propertiesJson))
      return R.badRequest("Missing form parameter 'acl'");

    JSONParser parser = new JSONParser();
    JSONObject props;
    try {
      props = (JSONObject) parser.parse(propertiesJson);
    } catch (ParseException e) {
      logger.debug("Could not parse properties ({}): {}", propertiesJson, getStackTrace(e));
      return R.badRequest("Could not parse series properties");
    }

    for (Object prop : props.entrySet()) {
      Entry<String, Object> field = (Entry<String, Object>) prop;
      seriesService.updateSeriesProperty(seriesID, field.getKey(), field.getValue().toString());
    }

    return ApiResponses.Json.ok(acceptHeader, propertiesJson);
  }

  /**
   * Parse two strings in UTC format into Date objects to represent a range of dates.
   *
   * @param createdFrom
   *          The string that represents the start date of the range.
   * @param createdTo
   *          The string that represents the end date of the range.
   * @return A Tuple with the two Dates
   * @throws IllegalArgumentException
   *           Thrown if the input strings are not valid UTC strings
   */
  private Tuple<Date, Date> getFromAndToCreationRange(String createdFrom, String createdTo) {
    Date createdFromDate = null;
    Date createdToDate = null;
    if ((StringUtils.isNotBlank(createdFrom) && StringUtils.isBlank(createdTo))
            || (StringUtils.isBlank(createdFrom) && StringUtils.isNotBlank(createdTo))) {
      logger.error("Both createdTo '{}' and createdFrom '{}' have to be specified or neither of them", createdTo,
              createdFrom);
      throw new IllegalArgumentException("Both createdTo '" + createdTo + "' and createdFrom '" + createdFrom
              + "' have to be specified or neither of them");
    } else {

      if (StringUtils.isNotBlank(createdFrom)) {
        try {
          createdFromDate = new Date(DateTimeSupport.fromUTC(createdFrom));
        } catch (IllegalStateException e) {
          logger.error("Unable to parse createdFrom parameter '{}':{}", createdFrom, ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse createdFrom parameter.");
        } catch (java.text.ParseException e) {
          logger.error("Unable to parse createdFrom parameter '{}':{}", createdFrom, ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse createdFrom parameter.");
        }
      }

      if (StringUtils.isNotBlank(createdTo)) {
        try {
          createdToDate = new Date(DateTimeSupport.fromUTC(createdTo));
        } catch (IllegalStateException e) {
          logger.error("Unable to parse createdTo parameter '{}':{}", createdTo, ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse createdTo parameter.");
        } catch (java.text.ParseException e) {
          logger.error("Unable to parse createdTo parameter '{}':{}", createdTo, ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse createdTo parameter.");
        }
      }
    }
    return new Tuple<>(createdFromDate, createdToDate);
  }

  private String getSeriesUrl(String seriesId) {
    return UrlSupport.concat(endpointBaseUrl, seriesId);
  }
}
