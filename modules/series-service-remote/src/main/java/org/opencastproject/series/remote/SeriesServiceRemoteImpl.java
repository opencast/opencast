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

package org.opencastproject.series.remote;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A proxy to a remote series service.
 */
@Path("/series")
@RestService(
    name = "seriesservice",
    title = "Series Service Remote",
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
    property = {
        "service.description=Series Remote Service Proxy",
        "opencast.service.type=org.opencastproject.series",
        "opencast.service.path=/series",
        "opencast.service.publish=false"
    },
    immediate = true,
    service = { SeriesService.class, SeriesServiceRemoteImpl.class }
)
@JaxrsResource
public class SeriesServiceRemoteImpl extends RemoteBase implements SeriesService {

  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceRemoteImpl.class);


  private static final Gson gson = new Gson();
  private static final Type seriesListType = new TypeToken<ArrayList<Series>>() { }.getType();

  public SeriesServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   */
  @Override
  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    super.setTrustedHttpClient(client);
  }

  /**
   * Sets the remote service manager.
   *
   * @param remoteServiceManager
   */
  @Override
  @Reference
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    super.setRemoteServiceManager(remoteServiceManager);
  }

  @Override
  public DublinCoreCatalog updateSeries(DublinCoreCatalog dc) throws SeriesException, UnauthorizedException {
    String seriesId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);

    HttpPost post = new HttpPost("/");
    try {
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("series", dc.toXmlString()));
      post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new SeriesException("Unable to assemble a remote series request for updating series " + seriesId, e);
    }

    HttpResponse response = getResponse(post, SC_NO_CONTENT, SC_CREATED, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SC_NO_CONTENT == statusCode) {
          logger.info("Successfully updated series {} in the series service", seriesId);
          return null;
        } else if (SC_UNAUTHORIZED == statusCode) {
          throw new UnauthorizedException("Not authorized to update series " + seriesId);
        } else if (SC_CREATED == statusCode) {
          DublinCoreCatalog catalogImpl = DublinCores.read(response.getEntity().getContent());
          logger.info("Successfully created series {} in the series service", seriesId);
          return catalogImpl;
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to update series " + seriesId + " using the remote series services: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to update series " + seriesId + " using the remote series services");
  }

  @Override
  public boolean updateAccessControl(String seriesID, AccessControlList accessControl)
          throws NotFoundException, SeriesException, UnauthorizedException {
    return updateAccessControl(seriesID, accessControl, false);
  }

  @Override
  public boolean updateAccessControl(String seriesID, AccessControlList accessControl, boolean overrideEpisodeAcl)
          throws NotFoundException, SeriesException, UnauthorizedException {
    HttpPost post = new HttpPost(seriesID + "/accesscontrol");
    try {
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("seriesID", seriesID));
      params.add(new BasicNameValuePair("acl", AccessControlParser.toXml(accessControl)));
      params.add(new BasicNameValuePair("overrideEpisodeAcl", Boolean.toString(overrideEpisodeAcl)));
      post.setEntity(new UrlEncodedFormEntity(params,  StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new SeriesException("Unable to assemble a remote series request for updating an ACL " + accessControl, e);
    }

    HttpResponse response = getResponse(post, SC_NO_CONTENT, SC_CREATED, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int status = response.getStatusLine().getStatusCode();
        if (SC_NOT_FOUND == status) {
          throw new NotFoundException("Series not found: " + seriesID);
        } else if (SC_NO_CONTENT == status) {
          logger.info("Successfully updated ACL of {} to the series service", seriesID);
          return true;
        } else if (SC_UNAUTHORIZED == status) {
          throw new UnauthorizedException("Not authorized to update series ACL of " + seriesID);
        } else if (SC_CREATED == status) {
          logger.info("Successfully created ACL of {} to the series service", seriesID);
          return false;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to update series ACL " + accessControl + " using the remote series services");
  }

  @Override
  public void deleteSeries(String seriesID) throws SeriesException, NotFoundException, UnauthorizedException {
    HttpDelete del = new HttpDelete(seriesID);
    HttpResponse response = getResponse(del, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SC_NOT_FOUND == statusCode) {
          throw new NotFoundException("Series not found: " + seriesID);
        } else if (SC_UNAUTHORIZED == statusCode) {
          throw new UnauthorizedException("Not authorized to delete series " + seriesID);
        } else if (SC_OK == statusCode) {
          logger.info("Successfully deleted {} from the remote series index", seriesID);
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to remove " + seriesID + " from a remote series index");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesID:.+}.json")
  public Response getSeriesJSON(@PathParam("seriesID") String seriesID) throws UnauthorizedException {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = getSeries(seriesID);
      return Response.ok(dc.toJson()).build();
    } catch (NotFoundException e) {
      return Response.status(NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve series: {}", e.getMessage());
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{seriesID:.+}/acl.json")
  public Response getSeriesAccessControlListJson(@PathParam("seriesID") String seriesID) {
    logger.debug("Series ACL lookup: {}", seriesID);
    try {
      AccessControlList acl = getSeriesAccessControl(seriesID);
      return Response.ok(acl).build();
    } catch (NotFoundException e) {
      return Response.status(NOT_FOUND).build();
    } catch (SeriesException e) {
      logger.error("Could not retrieve series ACL: {}", e.getMessage());
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException, UnauthorizedException {
    HttpGet get = new HttpGet(seriesID + ".xml");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Series " + seriesID + " not found in remote series index!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          throw new UnauthorizedException("Not authorized to get series " + seriesID);
        } else {
          DublinCoreCatalog dublinCoreCatalog = DublinCores.read(response.getEntity().getContent());
          logger.debug("Successfully received series {} from the remote series index", seriesID);
          return dublinCoreCatalog;
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to parse series from remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to get series from remote series index");
  }

  @Override
  public List<Series> getAllForAdministrativeRead(Date from, Optional<Date> to, int limit)
          throws SeriesException, UnauthorizedException {
    // Assemble URL
    StringBuilder url = new StringBuilder();
    url.append("/allInRangeAdministrative.json?");

    List<NameValuePair> queryParams = new ArrayList<>();
    queryParams.add(new BasicNameValuePair("from", Long.toString(from.getTime())));
    queryParams.add(new BasicNameValuePair("limit", Integer.toString(limit)));
    if (to.isPresent()) {
      queryParams.add(new BasicNameValuePair("to", Long.toString(to.get().getTime())));
    }
    url.append(URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8));
    HttpGet get = new HttpGet(url.toString());

    // Send HTTP request
    HttpResponse response = getResponse(get, SC_OK, SC_BAD_REQUEST, SC_UNAUTHORIZED);
    try {
      if (response == null) {
        throw new SeriesException("Unable to get series from remote series index");
      }

      if (response.getStatusLine().getStatusCode() == SC_BAD_REQUEST) {
        throw new SeriesException("internal server error when fetching /allInRangeAdministrative.json");
      } else if (response.getStatusLine().getStatusCode() == SC_UNAUTHORIZED) {
        throw new UnauthorizedException("got UNAUTHORIZED when fetching /allInRangeAdministrative.json");
      } else {
        // Retrieve and deserialize data
        Reader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
        return gson.fromJson(reader, seriesListType);
      }
    } catch (IOException e) {
      throw new SeriesException("failed to reader response body of /allInRangeAdministrative.json", e);
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException {
    HttpGet get = new HttpGet(seriesID + "/acl.xml");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Series ACL " + seriesID + " not found on remote series index!");
        } else {
          AccessControlList acl = AccessControlParser.parseAcl(response.getEntity().getContent());
          logger.info("Successfully get series ACL {} from the remote series index", seriesID);
          return acl;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to parse series ACL form remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to get series ACL from remote series index");
  }

  @Override
  public int getSeriesCount() throws SeriesException {
    HttpGet get = new HttpGet("/count");
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        int count = Integer.parseInt(IOUtils.toString(response.getEntity().getContent()));
        logger.info("Successfully get series dublin core catalog list from the remote series index");
        return count;
      }
    } catch (Exception e) {
      throw new SeriesException("Unable to count series from remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to count series from remote series index");
  }

  @Override
  public Map<String, String> getSeriesProperties(String seriesID)
          throws SeriesException, NotFoundException, UnauthorizedException {
    HttpGet get = new HttpGet(seriesID + "/properties.json");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    JSONParser parser = new JSONParser();
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Series " + seriesID + " not found in remote series index!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          throw new UnauthorizedException("Not authorized to get series " + seriesID);
        } else {
          logger.debug("Successfully received series {} properties from the remote series index", seriesID);
          StringWriter writer = new StringWriter();
          IOUtils.copy(response.getEntity().getContent(), writer, StandardCharsets.UTF_8);
          JSONArray jsonProperties = (JSONArray) parser.parse(writer.toString());
          Map<String, String> properties = new TreeMap<>();
          for (int i = 0; i < jsonProperties.length(); i++) {
            JSONObject property = (JSONObject) jsonProperties.get(i);
            JSONArray names = property.names();
            for (int j = 0; j < names.length(); j++) {
              properties.put(names.get(j).toString(), property.get(names.get(j).toString()).toString());
            }
          }
          return properties;
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to parse series properties from remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to get series from remote series index");
  }

  @Override
  public String getSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    HttpGet get = new HttpGet(seriesID + "/property/" + propertyName + ".json");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Series " + seriesID + " not found in remote series index!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          throw new UnauthorizedException("Not authorized to get series " + seriesID);
        } else {
          logger.debug("Successfully received series {} property {} from the remote series index", seriesID,
                  propertyName);
          StringWriter writer = new StringWriter();
          IOUtils.copy(response.getEntity().getContent(), writer, StandardCharsets.UTF_8);
          return writer.toString();
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to parse series from remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to get series from remote series index");
  }

  @Override
  public void updateSeriesProperty(String seriesID, String propertyName, String propertyValue)
          throws SeriesException, NotFoundException, UnauthorizedException {
    HttpPost post = new HttpPost("/" + seriesID + "/property");
    try {
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("name", propertyName));
      params.add(new BasicNameValuePair("value", propertyValue));
      post.setEntity(new UrlEncodedFormEntity(params,  StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new SeriesException("Unable to assemble a remote series request for updating series " + seriesID
              + " series property " + propertyName + ":" + propertyValue, e);
    }

    HttpResponse response = getResponse(post, SC_NO_CONTENT, SC_CREATED, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SC_NO_CONTENT == statusCode) {
          logger.info("Successfully updated series {} with property name {} and value {} in the series service",
                  seriesID, propertyName, propertyValue);
          return;
        } else if (SC_UNAUTHORIZED == statusCode) {
          throw new UnauthorizedException("Not authorized to update series " + seriesID);
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to update series " + seriesID + " with property " + propertyName + ":"
              + propertyValue + " using the remote series services: ", e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to update series " + seriesID + " using the remote series services");
  }

  @Override
  public void deleteSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    HttpDelete del = new HttpDelete("/" + seriesID + "/property/" + propertyName);
    HttpResponse response = getResponse(del, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SC_NOT_FOUND == statusCode) {
          throw new NotFoundException("Series not found: " + seriesID);
        } else if (SC_UNAUTHORIZED == statusCode) {
          throw new UnauthorizedException("Not authorized to delete series " + seriesID);
        } else if (SC_OK == statusCode) {
          logger.info("Successfully deleted {} from the remote series index", seriesID);
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to remove " + seriesID + " from a remote series index");
  }

  @Override
  public boolean updateExtendedMetadata(String seriesId, String type, DublinCoreCatalog dc) throws SeriesException {
    HttpPut put = new HttpPut("/" + seriesId + "/extendedMetadata/" + type);
    try {
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("dc", dc.toXmlString()));
      put.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new SeriesException("Unable to assemble a remote series request for updating extended metadata of series "
              + seriesId, e);
    }

    HttpResponse response = getResponse(put, SC_NO_CONTENT, SC_CREATED, SC_INTERNAL_SERVER_ERROR);
    try {
      if (response == null) {
        throw new SeriesException(format("Error while updating extended metadata catalog of type '%s' for series '%s'",
                type, seriesId));
      } else {
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case SC_NO_CONTENT:
          case SC_CREATED:
            return true;
          case SC_INTERNAL_SERVER_ERROR:
            throw new SeriesException(
                    format("Error while updating extended metadata catalog of type '%s' for series '%s'", type,
                            seriesId));
          default:
            throw new SeriesException(format("Unexpected status code", statusCode));
        }
      }
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public Opt<Map<String, byte[]>> getSeriesElements(String seriesID) throws SeriesException {
    HttpGet get = new HttpGet("/" + seriesID + "/elements.json");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_INTERNAL_SERVER_ERROR);
    JSONParser parser = new JSONParser();

    try {
      if (response == null) {
        throw new SeriesException(format("Error while retrieving elements from series '%s'", seriesID));
      } else {
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case SC_OK:
            JSONArray elementArray = (JSONArray) parser.parse(IOUtils.toString(response.getEntity().getContent()));
            Map<String, byte[]> elements = new HashMap<>();
            for (int i = 0; i < elementArray.length(); i++) {
              final String type = elementArray.getString(i);
              Opt<byte[]> optData = getSeriesElementData(seriesID, type);
              if (optData.isSome()) {
                elements.put(type, optData.get());
              } else {
                throw new SeriesException(format("Tried to load non-existing element of type '%s'", type));
              }
            }
            return Opt.some(elements);
          case SC_NOT_FOUND:
            return Opt.none();
          case SC_INTERNAL_SERVER_ERROR:
            throw new SeriesException(format("Error while retrieving elements from series '%s'", seriesID));
          default:
            throw new SeriesException(format("Unexpected status code", statusCode));
        }
      }
    } catch (Exception e) {
      logger.warn("Error while retrieving elements from remote service:", e);
      throw new SeriesException(e);
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public Opt<byte[]> getSeriesElementData(String seriesID, String type) throws SeriesException {
    HttpGet get = new HttpGet("/" + seriesID + "/elements/" + type);
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_INTERNAL_SERVER_ERROR);

    try {
      if (response == null) {
        throw new SeriesException(
                format("Error while retrieving element of type '%s' from series '%s'", type, seriesID));
      } else {
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case SC_OK:
            return Opt.some(IOUtils.toByteArray(response.getEntity().getContent()));
          case SC_NOT_FOUND:
            return Opt.none();
          case SC_INTERNAL_SERVER_ERROR:
            throw new SeriesException(
                    format("Error while retrieving element of type '%s' from series '%s'", type, seriesID));
          default:
            throw new SeriesException(format("Unexpected status code", statusCode));
        }
      }
    } catch (Exception e) {
      logger.warn("Error while retrieving element from remote service:", e);
      throw new SeriesException(e);
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public boolean updateSeriesElement(String seriesID, String type, byte[] data) throws SeriesException {
    HttpPut put = new HttpPut("/" + seriesID + "/elements/" + type);
    put.setEntity(new ByteArrayEntity(data, ContentType.DEFAULT_BINARY));

    HttpResponse response = getResponse(put, SC_CREATED, SC_NO_CONTENT, SC_INTERNAL_SERVER_ERROR);
    try {
      if (response == null) {
        throw new SeriesException(format("Error while updating element of type '%s' in series '%s'", type, seriesID));
      } else {
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case SC_NO_CONTENT:
          case SC_CREATED:
            return true;
          case SC_INTERNAL_SERVER_ERROR:
            throw new SeriesException(
                    format("Error while updating element of type '%s' in series '%s'", type, seriesID));
          default:
            throw new SeriesException(format("Unexpected status code", statusCode));
        }
      }
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public boolean deleteSeriesElement(String seriesID, String type) throws SeriesException {
    if (isBlank(seriesID)) {
      throw new IllegalArgumentException("Series ID must not be blank");
    }
    if (isBlank(type)) {
      throw new IllegalArgumentException("Element type must not be blank");
    }

    HttpDelete del = new HttpDelete("/" + seriesID + "/elements/" + type);
    HttpResponse response = getResponse(del, SC_NO_CONTENT, SC_NOT_FOUND, SC_INTERNAL_SERVER_ERROR);
    try {
      if (response == null) {
        throw new SeriesException("Unable to remove " + seriesID + " from a remote series index");
      } else {
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case SC_NO_CONTENT:
            return true;
          case SC_NOT_FOUND:
            return false;
          case SC_INTERNAL_SERVER_ERROR:
            throw new SeriesException(
                    format("Error while deleting element of type '%s' from series '%s'", type, seriesID));
          default:
            throw new SeriesException(format("Unexpected status code", statusCode));
        }
      }
    } finally {
      closeConnection(response);
    }
  }
}
