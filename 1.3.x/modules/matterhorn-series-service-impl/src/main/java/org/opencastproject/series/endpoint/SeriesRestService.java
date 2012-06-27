/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.series.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Series Service.
 * 
 */
@Path("/")
@RestService(name = "seriesservice", title = "Series Service", abstractText = "This service creates, edits and retrieves and helps managing series.", notes = { "$Rev$" })
public class SeriesRestService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesRestService.class);

  /** Series Service */
  private SeriesService seriesService;

  /** Dublin Core Catalog service */
  private DublinCoreCatalogService dcService;

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";

  /** Service url */
  protected String serviceUrl = null;

  /** Default number of items on page */
  private static final int DEFAULT_LIMIT = 20;

  /** Maximum number of items on page */
  private static final int MAX_LIMIT = 100;

  /** Suffix to mark descending ordering of results */
  public static final String DESCENDING_SUFFIX = "_DESC";

  /**
   * OSGi callback for setting series service.
   * 
   * @param seriesService
   */
  public void setService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi callback for setting Dublin Core Catalog service.
   * 
   * @param dcService
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Activates REST service.
   * 
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc == null) {
      this.serverUrl = "http://localhost:8080";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.debug("Configured server url is {}", ccServerUrl);
      if (ccServerUrl == null)
        this.serverUrl = "http://localhost:8080";
      else {
        this.serverUrl = ccServerUrl;
      }
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{seriesID:.+}.xml")
  @RestQuery(name = "getAsXml", description = "Returns the series with the given identifier", returnDescription = "Returns the series dublin core XML document", pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series dublin core."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.") })
  public Response getSeriesXml(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = this.seriesService.getSeries(seriesID);
      String dcXML = serializeDublinCore(dc);
      return Response.ok(dcXML).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Could not retrieve series: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesID:.+}.json")
  @RestQuery(name = "getAsJson", description = "Returns the series with the given identifier", returnDescription = "Returns the series dublin core JSON document", pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series dublin core."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.") })
  public Response getSeriesJSON(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = this.seriesService.getSeries(seriesID);
      return Response.ok(dc.toJson()).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Could not retrieve series: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{seriesID:.+}/acl.xml")
  @RestQuery(name = "getAclAsXml", description = "Returns the access control list for the series with the given identifier", returnDescription = "Returns the series ACL as XML", pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.") })
  public Response getSeriesAccessControlListXml(@PathParam("seriesID") String seriesID) {
    return getSeriesAccessControlList(seriesID);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{seriesID:.+}/acl.json")
  @RestQuery(name = "getAclAsJson", description = "Returns the access control list for the series with the given identifier", returnDescription = "Returns the series ACL as JSON", pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found.") })
  public Response getSeriesAccessControlListJson(@PathParam("seriesID") String seriesID) {
    return getSeriesAccessControlList(seriesID);
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

  /**
   * Serializes Dublin Core and returns is as string.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized.
   * @return String representation of Dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = this.dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  @POST
  @Path("/")
  @RestQuery(name = "updateSeries", description = "Updates a series", returnDescription = "No content.", restParameters = {
          @RestParameter(name = "series", isRequired = true, defaultValue = "${this.sampleDublinCore}", description = "The series document", type = TEXT),
          @RestParameter(name = "acl", isRequired = false, defaultValue = "${this.sampleAccessControlList}", description = "The access control list for the series", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing in the request."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The access control list has been updated."),
          @RestResponse(responseCode = SC_CREATED, description = "The access control list has been created.") })
  public Response addOrUpdateSeries(@FormParam("series") String series, @FormParam("acl") String accessControl) {
    if (series == null) {
      logger.warn("series that should be added is null");
      return Response.status(BAD_REQUEST).build();
    }
    AccessControlList acl;
    try {
      acl = AccessControlParser.parseAcl(accessControl);
    } catch (Exception e) {
      logger.warn("Could not parse ACL: {}", e.getMessage());
      return Response.status(BAD_REQUEST).build();
    }
    DublinCoreCatalog dc;
    try {
      dc = this.dcService.load(new ByteArrayInputStream(series.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException e1) {
      logger.error("Could not deserialize dublin core catalog: {}", e1);
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    } catch (IOException e1) {
      logger.warn("Could not deserialize dublin core catalog: {}", e1);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      DublinCoreCatalog newSeries = seriesService.updateSeries(dc);
      if (accessControl != null) {
        seriesService.updateAccessControl(dc.getFirst(PROPERTY_IDENTIFIER), acl);
      }
      if (newSeries == null) {
        logger.debug("Updated series {} ", dc.getFirst(PROPERTY_IDENTIFIER));
        return Response.status(NO_CONTENT).build();
      }
      String id = newSeries.getFirst(PROPERTY_IDENTIFIER);
      String serializedSeries = serializeDublinCore(newSeries);
      logger.debug("Created series {} ", id);
      return Response.status(CREATED)
              .header("Location", UrlSupport.concat(new String[] { this.serverUrl, this.serviceUrl, id + ".xml" }))
              .header("Location", UrlSupport.concat(new String[] { this.serverUrl, this.serviceUrl, id + ".json" }))
              .entity(serializedSeries).build();
    } catch (Exception e) {
      logger.warn("Could not add/update series: {}", e.getMessage());
    }
    throw new WebApplicationException(INTERNAL_SERVER_ERROR);
  }

  @POST
  @Path("/{seriesID:.+}/accesscontrol")
  @RestQuery(name = "updateAcl", description = "Updates the access control list for a series", returnDescription = "No content.", restParameters = { @RestParameter(name = "acl", isRequired = true, defaultValue = "${this.sampleAccessControlList}", description = "The access control list for the series", type = TEXT) }, pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The access control list has been updated."),
          @RestResponse(responseCode = SC_CREATED, description = "The access control list has been created."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing in the request.") })
  public Response updateAccessControl(@PathParam("seriesID") String seriesID, @FormParam("acl") String accessControl)
          throws UnauthorizedException {
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
      boolean updated = seriesService.updateAccessControl(seriesID, acl);
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
  @RestQuery(name = "delete", description = "Delete a series", returnDescription = "No content.", pathParameters = { @RestParameter(name = "seriesID", isRequired = true, description = "The series identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No series with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The series was deleted.") })
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
  @Produces(MediaType.APPLICATION_JSON)
  @Path("series.json")
  @RestQuery(name = "listSeriesAsJson", description = "Returns the series matching the query parameters", returnDescription = "Returns the series search results as JSON", restParameters = {
          @RestParameter(name = "q", isRequired = false, description = "Free text search", type = STRING),
          @RestParameter(name = "edit", isRequired = false, description = "Whether this query should return only series that are editable", type = BOOLEAN),
          @RestParameter(name = "seriesId", isRequired = false, description = "The series identifier", type = STRING),
          @RestParameter(name = "seriesTitle", isRequired = false, description = "The series title", type = STRING),
          @RestParameter(name = "creator", isRequired = false, description = "The series creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "The series contributor", type = STRING),
          @RestParameter(name = "publisher", isRequired = false, description = "The series publisher", type = STRING),
          @RestParameter(name = "rightsholder", isRequired = false, description = "The series rights holder", type = STRING),
          @RestParameter(name = "createdfrom", isRequired = false, description = "Filter results by created from (yyyy-MM-dd'T'HH:mm:ss'Z')", type = STRING),
          @RestParameter(name = "createdto", isRequired = false, description = "Filter results by created to (yyyy-MM-dd'T'HH:mm:ss'Z')", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "The series language", type = STRING),
          @RestParameter(name = "license", isRequired = false, description = "The series license", type = STRING),
          @RestParameter(name = "subject", isRequired = false, description = "The series subject", type = STRING),
          @RestParameter(name = "abstract", isRequired = false, description = "The series abstract", type = STRING),
          @RestParameter(name = "description", isRequired = false, description = "The series description", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any of the following: TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, TEMPORAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = STRING),
          @RestParameter(name = "startPage", isRequired = false, description = "The page offset", type = STRING),
          @RestParameter(name = "count", isRequired = false, description = "Results per page (max 100)", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The access control list.") })
  // CHECKSTYLE:OFF
  public Response getSeriesAsJson(@QueryParam("q") String text, @QueryParam("seriesId") String seriesId,
          @QueryParam("edit") Boolean edit, @QueryParam("seriesTitle") String seriesTitle,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("publisher") String publisher, @QueryParam("rightsholder") String rightsHolder,
          @QueryParam("createdfrom") String createdFrom, @QueryParam("createdto") String createdTo,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("subject") String subject, @QueryParam("abstract") String seriesAbstract,
          @QueryParam("description") String description, @QueryParam("sort") String sort,
          @QueryParam("startPage") String startPage, @QueryParam("count") String count) {
    // CHECKSTYLE:ON
    try {
      DublinCoreCatalogList result = getSeries(text, seriesId, edit, seriesTitle, creator, contributor, publisher,
              rightsHolder, createdFrom, createdTo, language, license, subject, seriesAbstract, description, sort,
              startPage, count);
      return Response.ok(result.getResultsAsJson()).build();
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", e.getMessage());
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series.xml")
  @RestQuery(name = "listSeriesAsXml", description = "Returns the series matching the query parameters", returnDescription = "Returns the series search results as XML", restParameters = {
          @RestParameter(name = "q", isRequired = false, description = "Free text search", type = STRING),
          @RestParameter(name = "edit", isRequired = false, description = "Whether this query should return only series that are editable", type = BOOLEAN),
          @RestParameter(name = "seriesId", isRequired = false, description = "The series identifier", type = STRING),
          @RestParameter(name = "seriesTitle", isRequired = false, description = "The series title", type = STRING),
          @RestParameter(name = "creator", isRequired = false, description = "The series creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "The series contributor", type = STRING),
          @RestParameter(name = "publisher", isRequired = false, description = "The series publisher", type = STRING),
          @RestParameter(name = "rightsholder", isRequired = false, description = "The series rights holder", type = STRING),
          @RestParameter(name = "createdfrom", isRequired = false, description = "Filter results by created from (yyyy-MM-dd'T'HH:mm:ss'Z')", type = STRING),
          @RestParameter(name = "createdto", isRequired = false, description = "Filter results by created to (yyyy-MM-dd'T'HH:mm:ss'Z')", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "The series language", type = STRING),
          @RestParameter(name = "license", isRequired = false, description = "The series license", type = STRING),
          @RestParameter(name = "subject", isRequired = false, description = "The series subject", type = STRING),
          @RestParameter(name = "abstract", isRequired = false, description = "The series abstract", type = STRING),
          @RestParameter(name = "description", isRequired = false, description = "The series description", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any of the following: TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, TEMPORAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = STRING),
          @RestParameter(name = "startPage", isRequired = false, description = "The page offset", type = STRING),
          @RestParameter(name = "count", isRequired = false, description = "Results per page (max 100)", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The access control list.") })
  // CHECKSTYLE:OFF
  public Response getSeriesAsXml(@QueryParam("q") String text, @QueryParam("seriesId") String seriesId,
          @QueryParam("edit") Boolean edit, @QueryParam("seriesTitle") String seriesTitle,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("publisher") String publisher, @QueryParam("rightsholder") String rightsHolder,
          @QueryParam("createdfrom") String createdFrom, @QueryParam("createdto") String createdTo,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("subject") String subject, @QueryParam("abstract") String seriesAbstract,
          @QueryParam("description") String description, @QueryParam("sort") String sort,
          @QueryParam("startPage") String startPage, @QueryParam("count") String count) {
    // CHECKSTYLE:ON
    try {
      DublinCoreCatalogList result = getSeries(text, seriesId, edit, seriesTitle, creator, contributor, publisher,
              rightsHolder, createdFrom, createdTo, language, license, subject, seriesAbstract, description, sort,
              startPage, count);
      return Response.ok(result.getResultsAsXML()).build();
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", e.getMessage());
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  // CHECKSTYLE:OFF
  private DublinCoreCatalogList getSeries(String text, String seriesId, Boolean edit, String seriesTitle,
          String creator, String contributor, String publisher, String rightsHolder, String createdFrom,
          String createdTo, String language, String license, String subject, String seriesAbstract, String description,
          String sort, String startPageString, String countString) throws SeriesException, UnauthorizedException {
    // CHECKSTYLE:ON
    int startPage = 0;
    if (StringUtils.isNotEmpty(startPageString)) {
      try {
        startPage = Integer.parseInt(startPageString);
      } catch (NumberFormatException e) {
        logger.warn("Bad start page parameter");
      }
      if (startPage < 0) {
        startPage = 0;
      }
    }

    int count = DEFAULT_LIMIT;
    if (StringUtils.isNotEmpty(countString)) {
      try {
        count = Integer.parseInt(countString);
      } catch (NumberFormatException e) {
        logger.warn("Bad count parameter");
      }
      if ((count < 1) || (count > MAX_LIMIT)) {
        count = DEFAULT_LIMIT;
      }
    }

    SeriesQuery q = new SeriesQuery();
    q.setCount(count);
    q.setStartPage(startPage);
    if (edit != null) {
      q.setEdit(edit);
    }
    if (StringUtils.isNotEmpty(text)) {
      q.setText(text.toLowerCase());
    }
    if (StringUtils.isNotEmpty(seriesId)) {
      q.setSeriesId(seriesId.toLowerCase());
    }
    if (StringUtils.isNotEmpty(seriesTitle)) {
      q.setSeriesTitle(seriesTitle.toLowerCase());
    }
    if (StringUtils.isNotEmpty(creator)) {
      q.setCreator(creator.toLowerCase());
    }
    if (StringUtils.isNotEmpty(contributor)) {
      q.setContributor(contributor.toLowerCase());
    }
    if (StringUtils.isNotEmpty(language)) {
      q.setLanguage(language.toLowerCase());
    }
    if (StringUtils.isNotEmpty(license)) {
      q.setLicense(license.toLowerCase());
    }
    if (StringUtils.isNotEmpty(subject)) {
      q.setSubject(subject.toLowerCase());
    }
    if (StringUtils.isNotEmpty(publisher)) {
      q.setPublisher(publisher.toLowerCase());
    }
    if (StringUtils.isNotEmpty(seriesAbstract)) {
      q.setSeriesAbstract(seriesAbstract.toLowerCase());
    }
    if (StringUtils.isNotEmpty(description)) {
      q.setDescription(description.toLowerCase());
    }
    if (StringUtils.isNotEmpty(rightsHolder)) {
      q.setRightsHolder(rightsHolder.toLowerCase());
    }
    try {
      if (StringUtils.isNotEmpty(createdFrom)) {
        q.setCreatedFrom(SolrUtils.parseDate(createdFrom));
      }
      if (StringUtils.isNotEmpty(createdTo)) {
        q.setCreatedTo(SolrUtils.parseDate(createdTo));
      }
    } catch (ParseException e1) {
      logger.warn("Could not parse date parameter: {}", e1);
    }

    if (StringUtils.isNotBlank(sort)) {
      SeriesQuery.Sort sortField = null;
      if (sort.endsWith("_DESC")) {
        String enumKey = sort.substring(0, sort.length() - "_DESC".length()).toUpperCase();
        try {
          sortField = SeriesQuery.Sort.valueOf(enumKey);
          q.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SeriesQuery.Sort.valueOf(sort);
          q.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }

    return seriesService.getSeries(q);
  }

  /**
   * Generates sample Dublin core.
   * 
   * @return sample Dublin core
   */
  public String getSampleDublinCore() {
    return "<?xml version=\"1.0\"?>\n<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance/\"\n  xsi:schemaLocation=\"http://www.opencastproject.org http://www.opencastproject.org/schema.xsd\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n  xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:oc=\"http://www.opencastproject.org/matterhorn\">\n\n  <dcterms:title xml:lang=\"en\">\n    Land and Vegetation: Key players on the Climate Scene\n    </dcterms:title>\n  <dcterms:subject>\n    climate, land, vegetation\n    </dcterms:subject>\n  <dcterms:description xml:lang=\"en\">\n    Introduction lecture from the Institute for\n    Atmospheric and Climate Science.\n    </dcterms:description>\n  <dcterms:publisher>\n    ETH Zurich, Switzerland\n    </dcterms:publisher>\n  <dcterms:identifier>\n    10.0000/5819\n    </dcterms:identifier>\n  <dcterms:modified xsi:type=\"dcterms:W3CDTF\">\n    2007-12-05\n    </dcterms:modified>\n  <dcterms:format xsi:type=\"dcterms:IMT\">\n    video/x-dv\n    </dcterms:format>\n  <oc:promoted>\n    true\n  </oc:promoted>\n</dublincore>";
  }

  /**
   * Generates sample access control list.
   * 
   * @return sample ACL
   */
  public String getSampleAccessControlList() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:acl xmlns:ns2=\"org.opencastproject.security\"><ace><role>admin</role><action>delete</action><allow>true</allow></ace></ns2:acl>";
  }

}
