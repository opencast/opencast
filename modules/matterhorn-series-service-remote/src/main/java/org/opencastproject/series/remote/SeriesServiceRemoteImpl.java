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
package org.opencastproject.series.remote;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A proxy to a remote series service.
 */
@Path("/")
@RestService(name = "seriesservice", title = "Series Service Remote", abstractText = "This service creates, edits and retrieves and helps managing series.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class SeriesServiceRemoteImpl extends RemoteBase implements SeriesService {

  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceRemoteImpl.class);

  public SeriesServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public DublinCoreCatalog updateSeries(DublinCoreCatalog dc) throws SeriesException, UnauthorizedException {
    String seriesId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);

    HttpPost post = new HttpPost("/");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("series", dc.toXmlString()));
      post.setEntity(new UrlEncodedFormEntity(params));
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
          DublinCoreCatalogImpl catalogImpl = new DublinCoreCatalogImpl(response.getEntity().getContent());
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
  public boolean updateAccessControl(String seriesID, AccessControlList accessControl) throws NotFoundException,
          SeriesException, UnauthorizedException {
    HttpPost post = new HttpPost(seriesID + "/accesscontrol");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("seriesID", seriesID));
      params.add(new BasicNameValuePair("acl", AccessControlParser.toXml(accessControl)));
      post.setEntity(new UrlEncodedFormEntity(params));
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
          DublinCoreCatalog dublinCoreCatalog = new DublinCoreCatalogImpl(response.getEntity().getContent());
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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("series.json")
  public Response getSeriesAsJson(@QueryParam("startPage") long startPage, @QueryParam("count") long count)
          throws UnauthorizedException {
    try {
      DublinCoreCatalogList result = getSeries(new SeriesQuery().setStartPage(startPage).setCount(count));
      return Response.ok(result.getResultsAsJson()).build();
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not perform search query: {}", e.getMessage());
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @Override
  public DublinCoreCatalogList getSeries(SeriesQuery query) throws SeriesException, UnauthorizedException {
    HttpGet get = new HttpGet(getSeriesUrl(query));
    HttpResponse response = getResponse(get, SC_OK, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SC_OK == statusCode) {
          DublinCoreCatalogList list = DublinCoreCatalogList.parse(EntityUtils.toString(response.getEntity(), "UTF-8"));
          logger.info("Successfully get series dublin core catalog list from the remote series index");
          return list;
        } else if (SC_UNAUTHORIZED == statusCode) {
          throw new UnauthorizedException("Not authorized to get series from query");
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SeriesException("Unable to get series from query from remote series index: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SeriesException("Unable to get series from query from remote series index: " + getSeriesUrl(query));
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

  /**
   * Builds the a series URL.
   *
   * @param q
   *          the series query
   * @param admin
   *          whether this is for an administrative read
   * @return the series URL
   */
  private String getSeriesUrl(SeriesQuery q) {
    StringBuilder url = new StringBuilder();
    url.append("/series.xml?");

    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if (q.getText() != null)
      queryStringParams.add(new BasicNameValuePair("q", q.getText()));
    if (q.getSeriesId() != null)
      queryStringParams.add(new BasicNameValuePair("seriesId", q.getSeriesId()));
    queryStringParams.add(new BasicNameValuePair("edit", Boolean.toString(q.isEdit())));
    if (q.getSeriesTitle() != null)
      queryStringParams.add(new BasicNameValuePair("seriesTitle", q.getSeriesTitle()));
    if (q.getCreator() != null)
      queryStringParams.add(new BasicNameValuePair("creator", q.getCreator()));
    if (q.getContributor() != null)
      queryStringParams.add(new BasicNameValuePair("contributor", q.getContributor()));
    if (q.getPublisher() != null)
      queryStringParams.add(new BasicNameValuePair("publisher", q.getPublisher()));
    if (q.getRightsHolder() != null)
      queryStringParams.add(new BasicNameValuePair("rightsholder", q.getRightsHolder()));
    if (q.getCreatedFrom() != null)
      queryStringParams.add(new BasicNameValuePair("createdfrom", SolrUtils.serializeDate(q.getCreatedFrom())));
    if (q.getCreatedTo() != null)
      queryStringParams.add(new BasicNameValuePair("createdto", SolrUtils.serializeDate(q.getCreatedTo())));
    if (q.getLanguage() != null)
      queryStringParams.add(new BasicNameValuePair("language", q.getLanguage()));
    if (q.getLicense() != null)
      queryStringParams.add(new BasicNameValuePair("license", q.getLicense()));
    if (q.getSubject() != null)
      queryStringParams.add(new BasicNameValuePair("subject", q.getSubject()));
    if (q.getAbstract() != null)
      queryStringParams.add(new BasicNameValuePair("abstract", q.getAbstract()));
    if (q.getDescription() != null)
      queryStringParams.add(new BasicNameValuePair("description", q.getDescription()));
    if (q.getSort() != null) {
      String sortString = q.getSort().toString();
      if (!q.isSortAscending())
        sortString = sortString.concat("_DESC");
      queryStringParams.add(new BasicNameValuePair("sort", sortString));
    }
    queryStringParams.add(new BasicNameValuePair("startPage", Long.toString(q.getStartPage())));
    queryStringParams.add(new BasicNameValuePair("count", Long.toString(q.getCount())));

    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
    return url.toString();
  }
}
