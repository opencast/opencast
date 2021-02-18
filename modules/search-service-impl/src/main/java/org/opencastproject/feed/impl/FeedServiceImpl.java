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


package org.opencastproject.feed.impl;

import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.WireFeedOutput;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

@Path("/")
@RestService(name = "feedservice", title = "Feed Service",
    abstractText = "This class is responsible of creating RSS and Atom feeds.", notes = {})
/**
 * This class is responsible of creating RSS and Atom feeds.
 * <p>
 * The implementation relies on the request uri containing information about the requested feed type and the query used
 * to construct the feed contents.
 * </p>
 * <p>
 * Therefore, assuming that this servlet has been mounted to <code>/feeds/*</code>, a correct uri for this servlet looks
 * like this: <code>/feeds/&lt;feed type&gt;/&lt;version&gt;/&lt;query&gt;</code>, e. g.
 *
 * <pre>
 *     http://localhost/feeds/Atom/1.0/favorites
 * </pre>
 *
 * which would indicate a requeste to an atom 1.0 feed with <tt>favourites</tt> being the query.
 *
 * The servlet returns a HTTP status 200 with the feed data.
 * If the feed could not be found because the query is unknown a HTTP error 404 is returned
 * If the feed could not be build (wrong RSS or Atom version, corrupt data, etc) an HTTP error 500 is returned.
 */
public class FeedServiceImpl {

  /** The serial version uid */
  private static final long serialVersionUID = -4623160106007127801L;

  /** Name of the size parameter */
  private static final String PARAM_SIZE = "size";

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(FeedServiceImpl.class);

  /** List of feed generators */
  private List<FeedGenerator> feeds = new ArrayList<FeedGenerator>();

  /** The security service */
  private SecurityService securityService = null;

  /** For Feedlinks */
  private Gson gson = new Gson();

  /*
   *
   * Feedlinks for Admin UI
   * /feeds/feeds
   *
   */

  @GET
  @Path("/feeds")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "feeds",
      description = "List available series based feeds",
      returnDescription = "Return list of feeds",
      responses = {
          @RestResponse(
              responseCode = SC_OK,
              description = "List of available feeds returned.")
      })
  public String listFeedServices() {

    List<Map<String, String>> feedServices = new ArrayList<>();

    for (FeedGenerator generator : feeds) {
      if (generator.getName().equals("Series")
              || generator.getName().equals("Series episodes containing audio files")) {
        Map<String, String> details = new HashMap<>();
        details.put("identifier", generator.getIdentifier());
        details.put("name", generator.getName());
        details.put("description", generator.getDescription());
        details.put("copyright", generator.getCopyright());
        details.put("pattern", generator.getPattern());
        details.put("type", generator.getClass().getSimpleName());
        feedServices.add(details);
      }
    }

    return gson.toJson(feedServices);
  }

  /**
   * Note: We're using Regex matching for the path here, instead of normal JAX-RS paths.  Previously this class was a servlet,
   * which was fine except that it had auth issues.  Removing the servlet fixed the auth issues, but then the paths (as written
   * in the RestQuery docs) don't work because  JAX-RS does not support having "/" characters as part of the variable's value.
   *
   * So, what we've done instead is match everything that comes in under the /feeds/ namespace, and then substring it out the way
   * the old servlet code did.  But without the servlet, or auth issues :)
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{type}/{version}/{query:.*}")
  @RestQuery(name = "getFeed", description = "Gets an Atom or RSS feed", pathParameters = {
          @RestParameter(description = "Feed type (atom or rss)", name = "type", type = Type.STRING, isRequired = true),
          @RestParameter(description = "Feed version", name = "version", type = Type.STRING, isRequired = true),
          @RestParameter(description = "Feed query", name = "query", type = Type.STRING, isRequired = true)
      }, restParameters = {
          @RestParameter(description = "Requested result size", name = "size", type = Type.INTEGER, isRequired = false)
      }, responses = {
          @RestResponse(description = "Return the feed of the appropriate type", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response getFeed(@Context HttpServletRequest request) {
    String contentType = null;

    logger.debug("Requesting RSS or Atom feed.");
    FeedInfo feedInfo = null;
    Organization organization = securityService.getOrganization();

    // Try to extract requested feed type and content
    try {
      feedInfo = extractFeedInfo(request);
    } catch (Exception e) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // Set the content type
    if (feedInfo.getType().equals(Feed.Type.Atom))
      contentType = "application/atom+xml";
    else if (feedInfo.getType().equals(Feed.Type.RSS))
      contentType = "application/rss+xml";

    // Have a feed generator create the requested feed
    Feed feed = null;
    for (FeedGenerator generator : feeds) {
      if (generator.accept(feedInfo.getQuery())) {
        feed = generator.createFeed(feedInfo.getType(), feedInfo.getQuery(), feedInfo.getSize(), organization);
        if (feed == null) {
          return Response.serverError().build();
        }
        break;
      }
    }

    // Have we found a feed generator?
    if (feed == null) {
      logger.debug("RSS/Atom feed could not be generated");
      return Response.status(Status.NOT_FOUND).build();
    }

    // Set character encoding
    Variant v = new Variant(MediaType.valueOf(contentType), (String) null, feed.getEncoding());
    String outputString = null;
    try {
      if (feedInfo.getType().equals(Feed.Type.RSS)) {
        logger.debug("Creating RSS feed output.");
        SyndFeedOutput output = new SyndFeedOutput();
        outputString = output.outputString(new RomeRssFeed(feed, feedInfo));
      } else {
        logger.debug("Creating Atom feed output.");
        WireFeedOutput output = new WireFeedOutput();
        outputString = output.outputString(new RomeAtomFeed(feed, feedInfo));
      }
    } catch (FeedException e) {
      return Response.serverError().build();
    }

    return Response.ok(outputString, v).build();
  }

  /**
   * Returns information about the requested feed by extracting all relevant pieces from the servlet request's uri.
   * <p>
   * This method throws an {@link IllegalStateException} if the information cannot be extracted from the uri.
   * </p>
   *
   * @param request
   *          the servlet request
   * @return the requested feed
   * @throws IllegalStateException
   *           if the uri does not contain sufficient information about the request
   */
  private FeedInfo extractFeedInfo(HttpServletRequest request) throws IllegalStateException {
    String path = request.getPathInfo();
    if (path.startsWith("/"))
      path = path.substring(1);
    String[] pathElements = path.split("/");

    if (pathElements.length < 3)
      throw new IllegalStateException("Cannot extract requested feed parameters.");
    Feed.Type type = null;
    try {
      type = Feed.Type.parseString(pathElements[0]);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot extract requested feed type.");
    }
    float version = 0;
    try {
      version = Float.parseFloat(pathElements[1]);
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Cannot extract requested feed version.");
    }
    int queryLength = pathElements.length - 2;
    String[] query = new String[queryLength];
    for (int i = 0; i < queryLength; i++)
      query[i] = pathElements[i + 2];

    String sizeParam = request.getParameter(PARAM_SIZE);
    if (StringUtils.isNotBlank(sizeParam)) {
      try {
        return new FeedInfo(type, version, query, Integer.parseInt(sizeParam));
      } catch (Exception e) {
        logger.warn("Value of feed parameter 'size' is not an integer: '{}'", sizeParam);
        return new FeedInfo(type, version, query);
      }
    } else {
      return new FeedInfo(type, version, query);
    }
  }

  /**
   * Adds the feed generator to the list of generators.
   *
   * @param generator
   *          the generator
   */
  public void addFeedGenerator(FeedGenerator generator) {
    logger.info("Registering '{}' feed", generator.getIdentifier());
    feeds.add(generator);
  }

  /**
   * Removes the generator from the list of feed generators.
   *
   * @param generator
   *          the feed generator
   */
  public void removeFeedGenerator(FeedGenerator generator) {
    logger.info("Removing '{}' feed", generator.getIdentifier());
    feeds.remove(generator);
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
