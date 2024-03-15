/*
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

package org.opencastproject.series.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.SimpleSerializer;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Series Service.
 *
 */
@Path("/series")
@RestService(
    name = "seriesservice",
    title = "Series Service",
    abstractText = "This service creates, edits and retrieves and helps managing series.",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the "
            + "underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
@Component(
    immediate = true,
    service = SeriesRestService.class,
    property = {
        "service.description=Series REST Endpoint",
        "opencast.service.type=org.opencastproject.series",
        "opencast.service.path=/series"
    }
)
@JaxrsResource
public class SeriesRestService {

  private static final String SERIES_ELEMENT_CONTENT_TYPE_PREFIX = "series/";

  private static final Gson gson = new Gson();

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesRestService.class);

  /** Series Service */
  private SeriesService seriesService;

  /** Dublin Core Catalog service */
  private DublinCoreCatalogService dcService;

  /** The security service */
  private SecurityService securityService;

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  /** Default number of items on page */
  private static final int DEFAULT_LIMIT = 20;

  /** Suffix to mark descending ordering of results */
  public static final String DESCENDING_SUFFIX = "_DESC";

  private static final String SAMPLE_DUBLIN_CORE = "<?xml version=\"1.0\"?>\n"
      + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" "
      + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
      + "    xsi:schemaLocation=\"http://www.opencastproject.org http://www.opencastproject.org/schema.xsd\" "
      + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
      + "    xmlns:dcterms=\"http://purl.org/dc/terms/\" "
      + "    xmlns:oc=\"http://www.opencastproject.org/matterhorn/\">\n\n"
      + "  <dcterms:title xml:lang=\"en\">\n"
      + "    Land and Vegetation: Key players on the Climate Scene\n"
      + "  </dcterms:title>\n"
      + "  <dcterms:subject>"
      + "    climate, land, vegetation\n"
      + "  </dcterms:subject>\n"
      + "  <dcterms:description xml:lang=\"en\">\n"
      + "    Introduction lecture from the Institute for\n"
      + "    Atmospheric and Climate Science.\n"
      + "  </dcterms:description>\n"
      + "  <dcterms:publisher>\n"
      + "    ETH Zurich, Switzerland\n"
      + "  </dcterms:publisher>\n"
      + "  <dcterms:identifier>\n"
      + "    10.0000/5819\n"
      + "  </dcterms:identifier>\n"
      + "  <dcterms:modified xsi:type=\"dcterms:W3CDTF\">\n"
      + "    2007-12-05\n"
      + "  </dcterms:modified>\n"
      + "  <dcterms:format xsi:type=\"dcterms:IMT\">\n"
      + "    video/x-dv\n"
      + "  </dcterms:format>\n"
      + "</dublincore>";

  private static final String SAMPLE_ACCESS_CONTROL_LIST =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
          + "<acl xmlns=\"http://org.opencastproject.security\">\n"
          + "  <ace>\n"
          + "    <role>admin</role>\n"
          + "    <action>delete</action>\n"
          + "    <allow>true</allow>\n"
          + "  </ace>\n"
          + "</acl>";

  /**
   * OSGi callback for setting series service.
   *
   * @param seriesService
   */
  @Reference
  public void setService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi callback for setting Dublin Core Catalog service.
   *
   * @param dcService
   */
  @Reference
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /** OSGi callback for the security service */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }


  /**
   * Activates REST service.
   *
   * @param cc
   *          ComponentContext
   */
  @Activate
  public void activate(ComponentContext cc) {
    if (cc == null) {
      this.serverUrl = "http://localhost:8080";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      logger.debug("Configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        this.serverUrl = "http://localhost:8080";
      } else {
        this.serverUrl = ccServerUrl;
      }
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  public String getSeriesXmlUrl(String seriesId) {
    return UrlSupport.concat(serverUrl, serviceUrl, seriesId + ".xml");
  }

  public String getSeriesJsonUrl(String seriesId) {
    return UrlSupport.concat(serverUrl, serviceUrl, seriesId + ".json");
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{seriesID:.+}.xml")
  @RestQuery(
      name = "getAsXml",
      description = "Returns the series with the given identifier",
      returnDescription = "Returns the series dublin core XML document",
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The series dublin core."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "You do not have permission to view this series."),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "You do not have permission to view this series. Maybe you need to authenticate."
          )
      }
  )
  public Response getSeriesXml(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = this.seriesService.getSeries(seriesID);
      return Response.ok(dc.toXmlString()).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.warn("permission exception retrieving series");
      // TODO this should be an 403 (Forbidden) if the user is logged in
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    } catch (Exception e) {
      logger.error("Could not retrieve series: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesID:.+}.json")
  @RestQuery(
      name = "getAsJson",
      description = "Returns the series with the given identifier",
      returnDescription = "Returns the series dublin core JSON document",
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The series dublin core."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "You do not have permission to view this series. Maybe you need to authenticate."
          )
      }
  )
  public Response getSeriesJSON(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = this.seriesService.getSeries(seriesID);
      return Response.ok(dc.toJson()).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.warn("permission exception retrieving series");
      // TODO this should be an 403 (Forbidden) if the user is logged in
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    } catch (Exception e) {
      logger.error("Could not retrieve series: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{seriesID:.+}/acl.xml")
  @RestQuery(
      name = "getAclAsXml",
      description = "Returns the access control list for the series with the given identifier",
      returnDescription = "Returns the series ACL as XML",
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.")
      }
  )
  public Response getSeriesAccessControlListXml(@PathParam("seriesID") String seriesID) {
    return getSeriesAccessControlList(seriesID);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{seriesID:.+}/acl.json")
  @RestQuery(
      name = "getAclAsJson",
      description = "Returns the access control list for the series with the given identifier",
      returnDescription = "Returns the series ACL as JSON",
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.")
      }
  )
  public Response getSeriesAccessControlListJson(@PathParam("seriesID") String seriesID) {
    return getSeriesAccessControlList(seriesID);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/allInRangeAdministrative.json")
  @RestQuery(
      name = "allInRangeAdministrative",
      description = "Internal API! Returns all series (included deleted ones!) in the given "
          + "range 'from' (inclusive) .. 'to' (exclusive). Returns at most 'limit' many series. "
          + "Can only be used as administrator!",
      returnDescription = "Series in the range",
      restParameters = {
          @RestParameter(
              name = "from",
              isRequired = true,
              description = "Start of date range (inclusive) in milliseconds "
                  + "since 1970-01-01T00:00:00Z. Has to be >=0.",
              type = Type.INTEGER
          ),
          @RestParameter(
              name = "to",
              isRequired = false,
              // TODO: this shows the default value as 0 despite us not setting this value!
              description = "End of date range (exclusive) in milliseconds "
                  + "since 1970-01-01T00:00:00Z. Has to be > 'from'.",
              type = Type.INTEGER
          ),
          @RestParameter(
              name = "limit",
              isRequired = true,
              description = "Maximum number of series to be returned. Has to be >0.",
              type = Type.INTEGER
          ),
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "All series in the range"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "if the given parameters are invalid"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the user is not an administrator"),
      }
  )
  public Response getAllInRangeAdministrative(
      @FormParam("from") Long from,
      @FormParam("to") Long to,
      @FormParam("limit") Integer limit
  ) throws UnauthorizedException {
    // Parameter error handling
    if (from == null) {
      return badRequestAllInRange("Required parameter 'from' not specified");
    }
    if (limit == null) {
      return badRequestAllInRange("Required parameter 'limit' not specified");
    }
    if (from < 0) {
      return badRequestAllInRange("Parameter 'from' < 0, but it has to be >= 0");
    }
    if (to != null && to <= from) {
      return badRequestAllInRange("Parameter 'to' <= 'from', but that is not allowed");
    }
    if (limit <= 0) {
      return badRequestAllInRange("Parameter 'limit' <= 0, but it has to be > 0");
    }

    try {
      final List<Series> series = seriesService.getAllForAdministrativeRead(
          new Date(from),
          Optional.ofNullable(to).map(millis -> new Date(millis)),
          limit);

      return Response.ok(gson.toJson(series)).build();
    } catch (SeriesException e) {
      logger.error("Unexpected exception in getAllInRangeAdministrative", e);
      return Response.status(INTERNAL_SERVER_ERROR)
          .entity("internal server error")
          .build();
    }
  }

  /**
   * Returns a {@code Response} object representing a BAD_REQUEST to `allInRangeAdministrative`
   * with the given message as body. Also logs the message.
   */
  private static Response badRequestAllInRange(String msg) {
    logger.debug("Bad request to /series/allInRangeAdministrative: {}", msg);
    return Response.status(BAD_REQUEST).entity(msg).build();
  }

  /**
   * Retrieves ACL associated with series.
   *
   * @param seriesID
   *          series of which ACL should be retrieved
   * @return
   */
  private Response getSeriesAccessControlList(String seriesID) {
    logger.debug("Series ACL lookup: {}", seriesID);
    try {
      AccessControlList acl = seriesService.getSeriesAccessControl(seriesID);
      return Response.ok(acl).build();
    } catch (NotFoundException e) {
      return Response.status(NOT_FOUND).build();
    } catch (SeriesException e) {
      logger.error("Could not retrieve series ACL: {}", e.getMessage());
    }
    throw new WebApplicationException(INTERNAL_SERVER_ERROR);
  }

  private void addDcData(final DublinCoreCatalog dc, final String field, final String value) {
    if (StringUtils.isNotBlank(value)) {
      EName en = new EName(DublinCore.TERMS_NS_URI, field);
      dc.add(en, value);
    }
  }

  @POST
  @Path("/")
  @RestQuery(
      name = "updateSeries",
      description = "Updates a series",
      returnDescription = "No content.",
      restParameters = {
          @RestParameter(
              name = "series",
              isRequired = false,
              defaultValue = SAMPLE_DUBLIN_CORE,
              description = "The series document. Will take precedence over metadata fields",
              type = TEXT
          ),
          @RestParameter(
              name = "acl",
              isRequired = false,
              defaultValue = SAMPLE_ACCESS_CONTROL_LIST,
              description = "The access control list for the series",
              type = TEXT
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "accessRights",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "available",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "contributor",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "coverage",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "created",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "creator",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "date",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "description",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "extent",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "format",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "identifier",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "isPartOf",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "isReferencedBy",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "isReplacedBy",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "language",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "license",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "publisher",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "relation",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "replaces",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "rights",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "rightsHolder",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "source",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "spatial",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "subject",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "temporal",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "title",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              description = "Series metadata value",
              isRequired = false,
              name = "type",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "override",
              isRequired = false,
              defaultValue = "false",
              description = "If true the series ACL will take precedence over any existing episode ACL",
              type = STRING
          )
      },
      responses = {
          @RestResponse(
              responseCode = SC_BAD_REQUEST,
              description = "The required form params were missing in the request."
          ),
          @RestResponse(
              responseCode = SC_NO_CONTENT,
              description = "The access control list has been updated."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          ),
          @RestResponse(
              responseCode = SC_CREATED,
              description = "The access control list has been created."
          )
      }
  )
  public Response addOrUpdateSeries(
      @FormParam("series") String series,
      @FormParam("acl") String accessControl,
      @FormParam("accessRights") String dcAccessRights,
      @FormParam("available") String dcAvailable,
      @FormParam("contributor") String dcContributor,
      @FormParam("coverage") String dcCoverage,
      @FormParam("created") String dcCreated,
      @FormParam("creator") String dcCreator,
      @FormParam("date") String dcDate,
      @FormParam("description") String dcDescription,
      @FormParam("extent") String dcExtent,
      @FormParam("format") String dcFormat,
      @FormParam("identifier") String dcIdentifier,
      @FormParam("isPartOf") String dcIsPartOf,
      @FormParam("isReferencedBy") String dcIsReferencedBy,
      @FormParam("isReplacedBy") String dcIsReplacedBy,
      @FormParam("language") String dcLanguage,
      @FormParam("license") String dcLicense,
      @FormParam("publisher") String dcPublisher,
      @FormParam("relation") String dcRelation,
      @FormParam("replaces") String dcReplaces,
      @FormParam("rights") String dcRights,
      @FormParam("rightsHolder") String dcRightsHolder,
      @FormParam("source") String dcSource,
      @FormParam("spatial") String dcSpatial,
      @FormParam("subject") String dcSubject,
      @FormParam("temporal") String dcTemporal,
      @FormParam("title") String dcTitle,
      @FormParam("type") String dcType,
      @DefaultValue("false") @FormParam("override") boolean override
  ) throws UnauthorizedException {
    DublinCoreCatalog dc;
    if (StringUtils.isNotBlank(series)) {
      try {
        dc = this.dcService.load(new ByteArrayInputStream(series.getBytes(StandardCharsets.UTF_8)));
      } catch (UnsupportedEncodingException e1) {
        logger.error("Could not deserialize dublin core catalog", e1);
        throw new WebApplicationException(INTERNAL_SERVER_ERROR);
      } catch (IOException e1) {
        logger.warn("Could not deserialize dublin core catalog", e1);
        return Response.status(BAD_REQUEST).build();
      }
    } else if (StringUtils.isNotBlank(dcTitle)) {
      dc = DublinCores.mkOpencastSeries().getCatalog();
      addDcData(dc, "accessRights", dcAccessRights);
      addDcData(dc, "available", dcAvailable);
      addDcData(dc, "contributor", dcContributor);
      addDcData(dc, "coverage", dcCoverage);
      addDcData(dc, "created", dcCreated);
      addDcData(dc, "creator", dcCreator);
      addDcData(dc, "date", dcDate);
      addDcData(dc, "description", dcDescription);
      addDcData(dc, "extent", dcExtent);
      addDcData(dc, "format", dcFormat);
      addDcData(dc, "identifier", dcIdentifier);
      addDcData(dc, "isPartOf", dcIsPartOf);
      addDcData(dc, "isReferencedBy", dcIsReferencedBy);
      addDcData(dc, "isReplacedBy", dcIsReplacedBy);
      addDcData(dc, "language", dcLanguage);
      addDcData(dc, "license", dcLicense);
      addDcData(dc, "publisher", dcPublisher);
      addDcData(dc, "relation", dcRelation);
      addDcData(dc, "replaces", dcReplaces);
      addDcData(dc, "rights", dcRights);
      addDcData(dc, "rightsHolder", dcRightsHolder);
      addDcData(dc, "source", dcSource);
      addDcData(dc, "spatial", dcSpatial);
      addDcData(dc, "subject", dcSubject);
      addDcData(dc, "temporal", dcTemporal);
      addDcData(dc, "title", dcTitle);
      addDcData(dc, "type", dcType);
    } else {
      return Response.status(BAD_REQUEST).entity("Required series metadata not provided").build();
    }
    AccessControlList acl = null;
    if (StringUtils.isNotBlank(accessControl)) {
      try {
        acl = AccessControlParser.parseAcl(accessControl);
      } catch (Exception e) {
        logger.debug("Could not parse ACL", e);
        return Response.status(BAD_REQUEST).entity("Could not parse ACL").build();
      }
    }

    try {
      DublinCoreCatalog newSeries = seriesService.updateSeries(dc);
      if (acl != null) {
        seriesService.updateAccessControl(dc.getFirst(PROPERTY_IDENTIFIER), acl, override);
      }
      if (newSeries == null) {
        logger.debug("Updated series {} ", dc.getFirst(PROPERTY_IDENTIFIER));
        return Response.status(NO_CONTENT).build();
      }
      String id = newSeries.getFirst(PROPERTY_IDENTIFIER);
      logger.debug("Created series {} ", id);
      return Response.status(CREATED)
          .header("Location", getSeriesXmlUrl(id))
          .header("Location", getSeriesJsonUrl(id))
          .entity(newSeries.toXmlString())
          .build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not add/update series", e);
    }
    return Response.serverError().build();
  }

  @POST
  @Path("/{seriesID:.+}/accesscontrol")
  @RestQuery(
      name = "updateAcl",
      description = "Updates the access control list for a series",
      returnDescription = "No content.",
      restParameters = {
          @RestParameter(
              name = "acl",
              isRequired = true,
              defaultValue = SAMPLE_ACCESS_CONTROL_LIST,
              description = "The access control list for the series",
              type = TEXT
          ),
          @RestParameter(
              name = "override",
              isRequired = false,
              defaultValue = "false",
              description = "If true the series ACL will take precedence over any existing episode ACL",
              type = STRING
          )
      },
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(
              responseCode = SC_NOT_FOUND,
              description = "No series with this identifier was found."
          ),
          @RestResponse(
              responseCode = SC_NO_CONTENT,
              description = "The access control list has been updated."
          ),
          @RestResponse(
              responseCode = SC_CREATED,
              description = "The access control list has been created."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          ),
          @RestResponse(
              responseCode = SC_BAD_REQUEST,
              description = "The required path or form params were missing in the request."
          )
      }
  )
  public Response updateAccessControl(
      @PathParam("seriesID") String seriesID,
      @FormParam("acl") String accessControl,
      @DefaultValue("false") @FormParam("override") boolean override
  ) throws UnauthorizedException {
    if (accessControl == null) {
      logger.warn("Access control parameter is null.");
      return Response.status(BAD_REQUEST).build();
    }
    AccessControlList acl;
    try {
      acl = AccessControlParser.parseAcl(accessControl);
    } catch (Exception e) {
      logger.warn("Could not parse ACL: {}", e.getMessage());
      return Response.status(BAD_REQUEST).build();
    }
    try {
      boolean updated = seriesService.updateAccessControl(seriesID, acl, override);
      if (updated) {
        return Response.status(NO_CONTENT).build();
      }
      return Response.status(CREATED).build();
    } catch (NotFoundException e) {
      return Response.status(NOT_FOUND).build();
    } catch (SeriesException e) {
      logger.warn("Could not update ACL for {}: {}", seriesID, e.getMessage());
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @DELETE
  @Path("/{seriesID:.+}")
  @RestQuery(
      name = "delete",
      description = "Delete a series",
      returnDescription = "No content.",
      pathParameters = {
          @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(
              responseCode = SC_NOT_FOUND,
              description = "No series with this identifier was found."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          ),
          @RestResponse(
              responseCode = SC_NO_CONTENT,
              description = "The series was deleted."
          )
      }
  )
  public Response deleteSeries(@PathParam("seriesID") String seriesID) throws UnauthorizedException {
    try {
      this.seriesService.deleteSeries(seriesID);
      return Response.ok().build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (SeriesException se) {
      logger.warn("Could not delete series {}: {}", seriesID, se.getMessage());
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/count")
  @RestQuery(
      name = "count",
      description = "Returns the number of series",
      returnDescription = "Returns the number of series",
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The number of series")
      }
  )
  public Response getCount() throws UnauthorizedException {
    try {
      int count = seriesService.getSeriesCount();
      return Response.ok(count).build();
    } catch (SeriesException se) {
      logger.warn("Could not count series: {}", se.getMessage());
      throw new WebApplicationException(se);
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}/properties.json")
  @RestQuery(
      name = "getSeriesProperties",
      description = "Returns the series properties",
      returnDescription = "Returns the series properties as JSON",
      pathParameters = {
          @RestParameter(name = "id", description = "ID of series", isRequired = true, type = Type.STRING)
      },
      responses = {
          @RestResponse(
              responseCode = SC_OK,
              description = "The access control list."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          )
      }
  )
  public Response getSeriesPropertiesAsJson(@PathParam("id") String seriesId)
          throws UnauthorizedException, NotFoundException {
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
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesId}/property/{propertyName}.json")
  @RestQuery(
      name = "getSeriesProperty",
      description = "Returns a series property value",
      returnDescription = "Returns the series property value",
      pathParameters = {
          @RestParameter(
              name = "seriesId",
              description = "ID of series",
              isRequired = true,
              type = Type.STRING
          ),
          @RestParameter(
              name = "propertyName",
              description = "Name of series property",
              isRequired = true,
              type = Type.STRING
          )
      },
      responses = {
          @RestResponse(
              responseCode = SC_OK,
              description = "The access control list."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          )
      }
  )
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
      logger.warn("Could not perform search query", e);
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @POST
  @Path("/{seriesId}/property")
  @RestQuery(
      name = "updateSeriesProperty",
      description = "Updates a series property",
      returnDescription = "No content.",
      restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The property's name", type = TEXT),
          @RestParameter(name = "value", isRequired = true, description = "The property's value", type = TEXT)
      },
      pathParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)
      },
      responses = {
          @RestResponse(
              responseCode = SC_NOT_FOUND,
              description = "No series with this identifier was found."
          ),
          @RestResponse(
              responseCode = SC_NO_CONTENT,
              description = "The access control list has been updated."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          ),
          @RestResponse(
              responseCode = SC_BAD_REQUEST,
              description = "The required path or form params were missing in the request."
          )
      }
  )
  public Response updateSeriesProperty(
      @PathParam("seriesId") String seriesId,
      @FormParam("name") String name,
      @FormParam("value") String value
  ) throws UnauthorizedException {
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
      logger.warn("Could not update series property for series {} property {}:{} :", seriesId, name, value, e);
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @DELETE
  @Path("{seriesId}/property/{propertyName}")
  @RestQuery(
      name = "deleteSeriesProperty",
      description = "Deletes a series property",
      returnDescription = "No Content",
      pathParameters = {
          @RestParameter(
              name = "seriesId",
              description = "ID of series",
              isRequired = true,
              type = Type.STRING
          ),
          @RestParameter(
              name = "propertyName",
              description = "Name of series property",
              isRequired = true,
              type = Type.STRING
          )
      },
      responses = {
          @RestResponse(
              responseCode = SC_NO_CONTENT,
              description = "The series property has been deleted."
          ),
          @RestResponse(
              responseCode = SC_NOT_FOUND,
              description = "The series or property has not been found."
          ),
          @RestResponse(
              responseCode = SC_UNAUTHORIZED,
              description = "If the current user is not authorized to perform this action"
          )
      }
  )
  public Response deleteSeriesProperty(
      @PathParam("seriesId") String seriesId,
      @PathParam("propertyName") String propertyName
  ) throws UnauthorizedException, NotFoundException {
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
      logger.warn("Could not delete series '{}' property '{}' query:", seriesId, propertyName, e);
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Path("{seriesId}/elements.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getSeriesElements",
      description = "Returns all the element types of a series",
      returnDescription = "Returns a JSON array with all the types of elements of the given series.",
      pathParameters = {
          @RestParameter(name = "seriesId", description = "The series identifier", type = STRING, isRequired = true)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Series found"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Series not found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error while processing the request")
      }
  )
  public Response getSeriesElements(@PathParam("seriesId") String seriesId) {
    try {
      Opt<Map<String, byte[]>> optSeriesElements = seriesService.getSeriesElements(seriesId);
      if (optSeriesElements.isSome()) {
        Map<String, byte[]> seriesElements = optSeriesElements.get();
        JValue jsonArray = Jsons.arr(Stream.$(seriesElements.keySet()).map(Jsons.Functions.stringToJValue));
        return Response.ok(new SimpleSerializer().toJson(jsonArray)).build();
      } else {
        return R.notFound();
      }
    } catch (SeriesException e) {
      logger.warn("Error while retrieving elements for sieres '{}'", seriesId, e);
      return R.serverError();
    }
  }

  @GET
  @Path("{seriesId}/elements/{elementType}")
  @RestQuery(
      name = "getSeriesElement",
      description = "Returns the series element",
      returnDescription = "The data of the series element",
      pathParameters = {
          @RestParameter(
              name = "seriesId",
              description = "The series identifier",
              type = STRING,
              isRequired = true
          ),
          @RestParameter(
              name = "elementType",
              description = "The element type. This is equal to the subtype of the media type of "
                  + "this element: series/<elementtype>",
              type = STRING,
              isRequired = true
          )
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Series element found"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Series element not found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error while processing the request")
      }
  )
  public Response getSeriesElement(
      @PathParam("seriesId") String seriesId,
      @PathParam("elementType") String elementType
  ) {
    try {
      Opt<byte[]> data = seriesService.getSeriesElementData(seriesId, elementType);
      if (data.isSome()) {
        return Response.ok().entity(new ByteArrayInputStream(data.get()))
                .type(SERIES_ELEMENT_CONTENT_TYPE_PREFIX + elementType).build();
      } else {
        return R.notFound();
      }
    } catch (SeriesException e) {
      logger.warn("Error while returning element '{}' of series '{}':", elementType, seriesId, e);
      return R.serverError();
    }
  }

  @PUT
  @Path("{seriesId}/extendedMetadata/{type}")
  @RestQuery(
          name = "updateExtendedMetadata",
          description = "Updates extended metadata of a series",
          returnDescription = "An empty response",
          pathParameters = {
                  @RestParameter(name = "seriesId", description = "The series identifier", type = STRING,
                          isRequired = true),
                  @RestParameter(name = "type", description = "The type of the catalog flavor", type = STRING,
                          isRequired = true)
          },
          restParameters = {
                  @RestParameter(name = "dc", description = "The catalog with extended metadata.", type = TEXT,
                          isRequired = true, defaultValue = SAMPLE_DUBLIN_CORE
                  )
          },
          responses = {
                  @RestResponse(responseCode = SC_NO_CONTENT, description = "Extended metadata updated"),
                  @RestResponse(responseCode = SC_CREATED, description = "Extended metadata created"),
                  @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR,
                          description = "Error while processing the request")
          }
  )
  public Response putSeriesExtendedMetadata(
          @PathParam("seriesId") String seriesId,
          @PathParam("type") String type,
          @FormParam("dc") String dcString
  ) {
    try {
      DublinCoreCatalog dc = dcService.load(new ByteArrayInputStream(dcString.getBytes(StandardCharsets.UTF_8)));
      boolean elementExists = seriesService.getSeriesElementData(seriesId, type).isSome();
      if (seriesService.updateExtendedMetadata(seriesId, type, dc)) {
        if (elementExists) {
          return R.noContent();
        } else {
          return R.created(URI.create(UrlSupport.concat(serverUrl, serviceUrl, seriesId, "elements", type)));
        }
      } else {
        return R.serverError();
      }
    } catch (IOException e) {
      logger.warn("Could not deserialize dublin core catalog", e);
      return Response.status(BAD_REQUEST).build();
    } catch (SeriesException e) {
      logger.warn("Error while updating extended metadata of series '{}'", seriesId, e);
      return R.serverError();
    }
  }


  @PUT
  @Path("{seriesId}/elements/{elementType}")
  @RestQuery(
      name = "updateSeriesElement",
      description = "Updates an existing series element",
      returnDescription = "An empty response",
      pathParameters = {
          @RestParameter(name = "seriesId", description = "The series identifier", type = STRING, isRequired = true),
          @RestParameter(name = "elementType", description = "The element type", type = STRING, isRequired = true)
      },
      responses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Series element updated"),
          @RestResponse(responseCode = SC_CREATED, description = "Series element created"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error while processing the request")
      }
  )
  public Response putSeriesElement(
      @Context HttpServletRequest request,
      @PathParam("seriesId") String seriesId,
      @PathParam("elementType") String elementType
  ) {
    InputStream is = null;
    try {
      is = request.getInputStream();
      final byte[] data = IOUtils.toByteArray(is);
      boolean elementExists = seriesService.getSeriesElementData(seriesId, elementType).isSome();
      if (seriesService.updateSeriesElement(seriesId, elementType, data)) {
        if (elementExists) {
          return R.noContent();
        } else {
          return R.created(URI.create(UrlSupport.concat(serverUrl, serviceUrl, seriesId, "elements", elementType)));
        }
      } else {
        return R.serverError();
      }
    } catch (IOException e) {
      logger.error("Error while trying to read from request", e);
      return R.serverError();
    } catch (SeriesException e) {
      logger.warn("Error while adding element to series '{}'", seriesId, e);
      return R.serverError();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @DELETE
  @Path("{seriesId}/elements/{elementType}")
  @RestQuery(
      name = "deleteSeriesElement",
      description = "Deletes a series element",
      returnDescription = "An empty response",
      pathParameters = {
          @RestParameter(name = "seriesId", description = "The series identifier", type = STRING, isRequired = true),
          @RestParameter(name = "elementType", description = "The element type", type = STRING, isRequired = true)
      },
      responses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Series element deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Series element not found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error while processing the request")
      }
  )
  public Response deleteSeriesElement(
      @PathParam("seriesId") String seriesId,
      @PathParam("elementType") String elementType
  ) {
    try {
      if (seriesService.deleteSeriesElement(seriesId, elementType)) {
        return R.noContent();
      } else {
        return R.notFound();
      }
    } catch (SeriesException e) {
      return R.serverError();
    }
  }

}
