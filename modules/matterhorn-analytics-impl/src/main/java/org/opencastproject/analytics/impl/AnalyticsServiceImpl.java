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
package org.opencastproject.analytics.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.usertracking.api.UserAction;
import org.opencastproject.usertracking.api.UserActionList;
import org.opencastproject.usertracking.api.UserSummaryList;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.impl.UserActionListImpl;
import org.opencastproject.usertracking.impl.UserSummaryListImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/**
 * This is a class that handles rest calls to the visualization endpoints mostly
 * by collecting data from other endpoints and stitching it together in a more
 * useful manner for the visualizations.
 */
public class AnalyticsServiceImpl {
	// The maximum episodes to return. 
  private static final int MAXIMUM_EPISODES = 100000;
  // The key to retrieve the url of the engage node so that we can use rest calls on it.
	private static final String ENGAGE_URL_KEY = "org.opencastproject.engage.ui.url";
	//The key to retrieve the url of the current server node so that we can use rest calls on it.
	private static final String SERVER_URL_KEY = "org.opencastproject.server.url";
	// The XML tag for the amount of time a video has been played.
	private static final String PLAYED_XML_TAG = "played";
	// The XML tag for the amount of views a video has been watched
	private static final String VIEWS_XML_TAG = "views";
	// The XML tag for the id of the episode that this xml represents.
	private static final String EPISODE_ID_TAG_NAME = "episode-id";
	// The default limit of episodes to retrieve from a usertracking report. 
	private static final int DEFAULT_LIMIT = 1000000;
	// The default offset to use
	private static final int DEFAULT_OFFSET = 0;
	// The number of milliseconds in a second. 
	private static final long secondsToMilliseconds = 1000;
	// The trusted http client to make rest calls on the admin or engage node. 
	private TrustedHttpClient client;
	// The logger
	private Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
  // The URL representing the engage node retrieved from the context.
  private String engageURL = null;
  // Series Service
  private SeriesService seriesService = null;
  
  private SecurityService securityService = null;

	private SearchService searchService = null;
	
	private UserTrackingService userTrackingService = null;

  /**
   * Sets the http client which this service uses to communicate with the engage server.
   * 
   * @param c
   *          The client object.
   */
  public void setTrustedClient(TrustedHttpClient c) {
		client = c;
	}

	/**
	 * Activate this module and get the admin and engage urls from the bundle
	 * context.
	 **/
  public void activate(ComponentContext ctx) {
    logger.debug("Activating " + AnalyticsServiceImpl.class.getName());
    // Get the engage node's location
    String engageURLProperty = StringUtils.trimToNull((String) ctx.getBundleContext().getProperty(ENGAGE_URL_KEY));
    // Get the server node's location in case the engage setting is not enabled.
    String serverURLProperty = StringUtils.trimToNull((String) ctx.getBundleContext().getProperty(SERVER_URL_KEY));
    if (engageURLProperty == null && serverURLProperty != null) {
      engageURLProperty = serverURLProperty;
      logger.info("Using " + serverURLProperty
              + " as the engage location. If you have a seperate engage node please set the " + ENGAGE_URL_KEY
              + " in config.properties.");
    }
    try {
      engageURL = new URL(engageURLProperty).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ServiceException(ENGAGE_URL_KEY + " is malformed: " + engageURLProperty);
    }

  }

  public void setService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void unsetService(SeriesService seriesService) {
    this.seriesService = null;
  }
  
  public void setService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void unsetService(SecurityService securityService) {
    this.securityService = null;
  }
	
  public void setService(SearchService searchService) {
    this.searchService = searchService;
  }
  
  public void unsetService(SearchService searchService) {
    this.searchService = null;
  }
  
  public void setService(UserTrackingService userTrackingService) {
    this.userTrackingService = userTrackingService;
  }
  
  public void unsetService(UserTrackingService userTrackingService) {
    this.userTrackingService = null;
  }
  
  /**
   * Check to see if the necessary services are up. 
   * 
   * @return A Response of null if everything is up and running. SERVICE_UNAVAILABLE if there mandatory services
   *         missing.
   **/
  public Response servicesAreUp() {
    if (securityService == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Visualization service is unavailable due to security service being unavailable, please wait...").build();
    }

    if (seriesService == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Visualization service is unavailable due to series service being unavailable, please wait...").build();
    }
    
    if (searchService == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Visualization service is unavailable due to search service being unavailable, please wait...").build();
    }
    
    if (userTrackingService == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Visualization service is unavailable due to user tracking service being unavailable, please wait...").build();
    }
    
    return null;
  }
  
	/**
	 * Gets the details for a particular episode in XML format. 
	 * 
	 * @param episodeID
	 *            The unique id for the episode, also known as the mediapackage
	 *            id.
	 * @return Returns a string representation of the XML that represents an
	 *         episode.
	 * @throws TrustedHttpClientException
	 *             Thrown if the trusted http client cannot get the data from
	 *             the endpoint.
	 */
	public Response getEpisodeAsXml(String episodeID)
			throws TrustedHttpClientException {
	  if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    return Response.ok(getEpisodesByID(episodeID)).type(MediaType.APPLICATION_XML).build();
	}

	/**
	 * Gets the details for a particular episode in JSON format. 
	 * 
	 * @param episodeID
	 *            The unique id for the episode, also known as the mediapackage
	 *            id.
	 * @return Returns a string representation of the JSON that represents an
	 *         episode.
	 * @throws TrustedHttpClientException
	 *             Thrown if the trusted http client cannot get the data from
	 *             the endpoint.
	 */
	public Response getEpisodeAsJson(String episodeID)
			throws TrustedHttpClientException {
	  if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    return Response.ok(getEpisodesByID(episodeID)).type(MediaType.APPLICATION_JSON).build();
	}
	
  /**
   * Returns a SearchResult with the details of the episode that matches the unique id provided.
   * 
   * @param episodeID
   *          the unique id of the episode (also known as the mediapackage id)
   * @return The details of the episode if the user is allowed to see it and it exists.
   * **/
  public SearchResult getEpisodesByID(String episodeID) {
    boolean permitted = false;
    SearchQuery search = new SearchQuery();
    search.withLimit(MAXIMUM_EPISODES).withOffset(0);
    search.episodeId(episodeID);
    SearchResult searchResult = searchService.getByQuery(search);
    if (searchResult.getItems().length > 0) {
      String episodeSeries = searchResult.getItems()[0].getDcIsPartOf();
      List<String> availableSeriesIDs = getAnalyzeSeriesIDs();
      for (String availableSeriesID : availableSeriesIDs) {
        if (availableSeriesID.equalsIgnoreCase(episodeSeries)) {
          permitted = true;
        }
      }
    }
    if (permitted) {
      return searchResult;
    } else {
      return new SearchResultImpl();
    }
  }
	
  /**
   * Check to see if a user can see the analytics for a particular episode.
   * 
   * @param episodeID
   *          The unique id of the episode to check if the user has access to analyze it.
   * @return Returns true if the user can analyze the episode, false if s/he can't.
   * **/
  public boolean canAnalyzeEpisode(String episodeID) {
    boolean permitted = false;
    SearchQuery search = new SearchQuery();
    search.withLimit(MAXIMUM_EPISODES).withOffset(0);
    search.episodeId(episodeID);
    SearchResult searchResult = searchService.getByQuery(search);
    if (searchResult.getItems().length > 0) {
      String episodeSeries = searchResult.getItems()[0].getDcIsPartOf();
      List<String> availableSeriesIDs = getAnalyzeSeriesIDs();
      for (String availableSeriesID : availableSeriesIDs) {
        if (availableSeriesID.equalsIgnoreCase(episodeSeries)) {
          permitted = true;
        }
      }
    }
    return permitted;
  }
  
  /**
   * Get the episodes of a particular series if the user can see the analytics for that series.
   * 
   * @param seriesID
   *          The series to get all of the episodes for.
   * @return All of the episodes that the user is able to see the analytics for.
   */
	public SearchResult getEpisodesBySeries(String seriesID) {
	  boolean permitted = canAnalyzeSeries(seriesID);
    
    if (permitted) {
      SearchQuery search = new SearchQuery();
      search.withLimit(MAXIMUM_EPISODES).withOffset(0);
      search.partOf(seriesID);
      return searchService.getByQuery(search);
    } else {
      return new SearchResultImpl();
    }
	}

  /**
   * Check to see if a user can view the analytics of a series. 
   * 
   * @param seriesID
   *          The series to check whether the user can view the analytics.
   * @return true if the user has the correct permissions to analyze the series.
   */
  private boolean canAnalyzeSeries(String seriesID) {
    boolean permitted = false;
    List<String> availableSeriesIDs = getAnalyzeSeriesIDs();
    for (String availableSeriesID : availableSeriesIDs) {
      if (availableSeriesID.equalsIgnoreCase(seriesID)) {
        permitted = true;
      }
    }
    return permitted;
  }
	
	/**
	 * Get all of the episodes for a particular series id in XML format. 
	 * 
	 * @param seriesID
	 *            The id of the series you are interested in.
	 * @return An xml representation of all of the episodes for a series.
	 * @throws TrustedHttpClientException
	 *             Thrown if it cannot query a rest endpoint.
	 */
	public Response getEpisodesBySeriesAsXml(String seriesID)
			throws TrustedHttpClientException {
		if (servicesAreUp() != null) {
		  return servicesAreUp();
		}
		return Response.ok(getEpisodesBySeries(seriesID)).type(MediaType.APPLICATION_XML).build();
	}
	
	/**
	 * Get all of the episodes for a particular series id in JSON format. 
	 * 
	 * @param seriesID
	 *            The id of the series you are interested in.
	 * @return An JSON representation of all of the episodes for a series.
	 * @throws TrustedHttpClientException
	 *             Thrown if it cannot query a rest endpoint.
	 */
	public Response getEpisodesBySeriesAsJson(String seriesID)
			throws TrustedHttpClientException {
	  if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    return Response.ok(getEpisodesBySeries(seriesID)).type(MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * Get an xml representation of all of the available series in the system that the user can view their analytics.
	 * 
	 * @return An xml representation of all of the available series in the
	 *         system.
	 * @throws TrustedHttpClientException
	 *             Thrown if it cannot query a rest endpoint.
	 */
  public Response getSeriesAsXml() throws TrustedHttpClientException {
    if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    try {
      return Response.ok(getAnalyzeSeriesDublinCoreCatalogList(getAnalyzeSeriesIDs()).getResultsAsXML()).build();
    } catch (IOException e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Could not create XML for endpoint due to " + e.getMessage()).build();
    }
	}
	
	/**
	 * Get an json representation of all of the available series in the system that the user can view their analytics.
	 * 
	 * @return An json representation of all of the available series in the
	 *         system.
	 * @throws TrustedHttpClientException
	 *             Thrown if it cannot query a rest endpoint.
	 */
	public Response getSeriesAsJson() throws TrustedHttpClientException {
	  if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    return Response.ok(getAnalyzeSeriesDublinCoreCatalogList(getAnalyzeSeriesIDs()).getResultsAsJson()).build();
	}
		
  /**
   * A helper function to find all of the possible series in a list of episodes. 
   * @param allEpisodes
   *          All of the episodes to search for series to return.
   * @return Returns a HashMap with the key as the series id and the value of false for each entry.
   */
  private HashMap<String, Boolean> getAvailableSeries(SearchResult allEpisodes) {
    HashMap<String, Boolean> series = new HashMap<String, Boolean>();
    for (SearchResultItem episode : allEpisodes.getItems()) {
      if (StringUtils.trimToNull(episode.getDcIsPartOf()) != null) {
        series.put(episode.getDcIsPartOf(), false);
      }
    }
    return series;
  }
  
  /**
   * A helper function that returns the dublin core catalogs of a list of series.
   * 
   * @param series
   *          A list of series ids as Strings to return the dublin core catalogs of.
   * @return Returns a list of dublin core catalogs of the series in the parameter
   */
  private DublinCoreCatalogList getAnalyzeSeriesDublinCoreCatalogList(List<String> series) {
    List<DublinCoreCatalog> catalogs = new LinkedList<DublinCoreCatalog>();

    for (String seriesID : series) {
      try {
        catalogs.add(seriesService.getSeries(seriesID));
      } catch (SeriesException e) {
        logger.error("While trying to get the dublin core catalogs of series there was a series exception.", e);
        e.printStackTrace();
      } catch (NotFoundException e) {
        logger.error("While trying to get the dublin core catalogs of series there was a not found exception.", e);
      } catch (UnauthorizedException e) {
        logger.error("While trying to get the dublin core catalogs of series there was an Unauthorized exception.", e);
      }
    }

    return new DublinCoreCatalogList(catalogs, catalogs.size());
  }
  
  /**
   * Get all of the series that a user is able to view the analytics for.
   * 
   * @return Returns the list of series ids that a user is able to view the analytics for.
   */
  private List<String> getAnalyzeSeriesIDs() {
    LinkedList<String> analyzeSeries = new LinkedList<String>();
    // Get all of the episodes
    SearchResult allEpisodes = searchForAllEpisodes();
    // Get all of the series for those episodes
    HashMap<String, Boolean> allSeries = getAvailableSeries(allEpisodes);
    // Filter the series to the ones we have analyze ability on
    User user = securityService.getUser();
    String[] roles = user.getRoles();
    
    for (String seriesID : allSeries.keySet()) {
      AccessControlList accessControlList;
      try {
        accessControlList = seriesService.getSeriesAccessControl(seriesID);
        for (AccessControlEntry accessControlEntry : accessControlList.getEntries()) {
          if (accessControlEntry.getAction().equalsIgnoreCase("analyze")) {
            for (String role : roles) {
              if (accessControlEntry.getRole().equalsIgnoreCase(role)) {
                if (!allSeries.get(seriesID)) {
                  analyzeSeries.add(seriesID);
                  allSeries.put(seriesID, true);  
                }
              }
            }
          }
        }
      } catch (NotFoundException e) {
        logger.warn("Wasn't able to find the access control list for a series with id " + seriesID);
      } catch (SeriesException e) {
        e.printStackTrace();
      }
    }
    return analyzeSeries;
  }
  
  /**
   * Search for all of the episodes a user can watch. 
   * @return Returns a list of all of the episodes available to this user.
   */
  private SearchResult searchForAllEpisodes() {
    SearchQuery search = new SearchQuery();
    search.withLimit(MAXIMUM_EPISODES).withOffset(0);
    return searchService.getByQuery(search);
  }
  
	/**
	 * Gets the number of times an episode was watched and for how long in
	 * intervals over a time range.
	 * 
	 * @param id
	 *            The unique id of the episode to get the statistics for.
	 * @param start
	 *            The start of the period to investigate in the form
	 *            YYYYMMDDHHMM e.g. 201212312359.
	 * @param end
	 *            The end of the period to investigate in the form YYYYMMDDHHMM
	 *            e.g. 201212312359.
	 * @param intervalString
	 *            The number of seconds to break up the views and durations into
	 *            from start time to end time.
	 * @return An xml representation of all of these intervals between start and
	 *         end.
	 * @throws TrustedHttpClientException
	 *             Thrown if rest calls cannot be made. Thrown if it cannot
	 *             query a rest endpoint.
	 */
  public ViewCollection getViews(String id, String start, String end, String intervalString)
          throws TrustedHttpClientException {
    if (canAnalyzeEpisode(id)) {

      long limit = DEFAULT_LIMIT;
      long interval = Long.parseLong(intervalString);

      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
      Date startDate = new Date();
      Date endDate = new Date();
      try {
        startDate = dateFormat.parse(start);
        endDate = dateFormat.parse(end);
      } catch (ParseException e) {
        e.printStackTrace();
      }

      ViewCollection viewCollection = new ViewCollection();
      viewCollection.setLimit(limit);
      viewCollection.setFrom(startDate);
      viewCollection.setTo(endDate);
      viewCollection.setInterval(interval);

      long intervalCount = 0;
      Date intervalStart;
      Date intervalEnd;
      HttpGet getInterval;
      HttpResponse response;
      Boolean foundViews = false;
      
      do {
        foundViews = false;
        // Get the start and end of the interval
        intervalStart = new Date(startDate.getTime() + interval * secondsToMilliseconds * intervalCount);
        intervalEnd = new Date(startDate.getTime() + interval * secondsToMilliseconds * (intervalCount + 1));
        String uri = UrlSupport.concat(engageURL, "/usertracking/report.xml");
        uri += "?from=" + dateFormat.format(intervalStart);
        uri += "&to=" + dateFormat.format(intervalEnd);
        uri += "&limit=" + limit;
        getInterval = new HttpGet(uri);
        response = client.execute(getInterval);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
          dBuilder = dbFactory.newDocumentBuilder();
          Document document = dBuilder.parse(response.getEntity().getContent());
          document.getDocumentElement().normalize();
          NodeList reports = document.getChildNodes();
          ViewItem viewItem = new ViewItem();
          for (int i = 0; i < reports.getLength(); i++) {
            Node report = reports.item(i);
            NodeList reportItems = report.getChildNodes();
            for (int j = 0; j < reportItems.getLength(); j++) {
              Node reportItem = reportItems.item(j);
              if (reportItem.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) reportItem;
                String tagID = getTagValue(EPISODE_ID_TAG_NAME, eElement);
                if (id.equals(tagID)) {
                  viewItem.setId(getTagValue(EPISODE_ID_TAG_NAME, eElement));
                  viewItem.setViews(getTagValue(VIEWS_XML_TAG, eElement));
                  viewItem.setPlayed(getTagValue(PLAYED_XML_TAG, eElement));
                  viewItem.setStart(dateFormat.format(intervalStart));
                  viewItem.setEnd(dateFormat.format(intervalEnd));
                  viewCollection.add(viewItem);
                  viewCollection.setViews(viewCollection.getViews()
                          + Integer.parseInt(getTagValue(VIEWS_XML_TAG, eElement)));
                  viewCollection.setPlayed(viewCollection.getPlayed()
                          + Integer.parseInt(getTagValue(PLAYED_XML_TAG, eElement)));
                  viewCollection.setTotal(viewCollection.getTotal() + 1);
                  foundViews = true;
                }

              }
            }
          }
          // Handle the case where there is no data for this episode during this interval. 
          if (!foundViews) {
            viewItem.setId(id);
            viewItem.setViews("0");
            viewItem.setPlayed("0");
            viewItem.setStart(dateFormat.format(intervalStart));
            viewItem.setEnd(dateFormat.format(intervalEnd));
            viewCollection.add(viewItem);
          }
        } catch (IllegalStateException e) {
          e.printStackTrace();
        } catch (SAXException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (ParserConfigurationException e1) {
          e1.printStackTrace();
        }
        intervalCount++;
      } while (intervalStart.before(endDate) && intervalEnd.before(endDate));
      return viewCollection;
    } else {
      return new ViewCollection();
    }
  }

  public Response getUserActionsAsXml(String type, String day, String limit, String offset) {
    return getUserActions(type, day, limit, offset, MediaType.APPLICATION_XML);
  }
  
  public Response getUserActionsAsJson(String type, String day, String limit, String offset) {
    return getUserActions(type, day, limit, offset, MediaType.APPLICATION_JSON);
  }
  
  /**
   * Get all of the user actions for a particular day of a particular type.  
   * 
   * @param type
   *          The type of user actions to be queried e.g. HEARTBEAT
   * @param day
   *          The date that the actions are from.
   * @param limitInput
   *          The limit of actions to return.
   * @param offsetInput
   *          The offset for the return set.
   * @param mediaType
   *          Whether to use XML or JSON as the final return type.
   * @return A list of user actions of type on day.
   */
  private Response getUserActions(String type, String day, String limitInput, String offsetInput, String mediaType) {
    if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    
    int offset = DEFAULT_OFFSET;
    if (StringUtils.trimToNull(offsetInput) != null) {
      try {
        offset = Integer.parseInt(offsetInput);
      } catch (NumberFormatException e) {
        return Response.serverError()
                .status(Response.Status.BAD_REQUEST)
                .entity("Your offset "
                        + offsetInput
                        + " was not parseable to an int. Please check that argument and try again with a different value.")
                .build();
      }
    }
    
    int limit = DEFAULT_LIMIT;
    if (StringUtils.trimToNull(limitInput) != null) {
      try {
        limit = Integer.parseInt(limitInput);
      } catch (NumberFormatException e) {
        return Response.serverError()
                .status(Response.Status.BAD_REQUEST)
                .entity("Your limit "
                        + limitInput
                        + " was not parseable to an int. Please check that argument and try again with a different value.")
                .build();
      }
    }
    
    List<String> seriesIDs = getAnalyzeSeriesIDs();
    UserActionListImpl result = new UserActionListImpl();
    int numberLeft = limit;
    for (String seriesId : seriesIDs) {
      SearchResult episodes = getEpisodesBySeries(seriesId);
      for (SearchResultItem episode : episodes.getItems()) {
        try {
          if (numberLeft > 0) {
            UserActionList current = userTrackingService.getUserActionsByTypeAndMediapackageId("HEARTBEAT",
                    episode.getId(), offset, numberLeft);
            result.add(current.getUserActions());
            numberLeft = numberLeft - current.getUserActions().size();
          }
        } catch (UserTrackingException e) {
          logger.error("Could not process episode " + episode.getId() + " from series " + seriesId + "because ", e);
        }
      }
    }
    
    result.setLimit(limit);
    result.setOffset(offset);
    result.setTotal(result.getUserActions().size());
    
    return Response.ok(result).type(mediaType).build();
  }
  
  public Response getFirstUserActionsAsXml(String seriesID) {
    return getFirstUserActionForSeries(seriesID, MediaType.APPLICATION_XML);
  }

  public Response getFirstUserActionsAsJson(String seriesID) {
    return getFirstUserActionForSeries(seriesID, MediaType.APPLICATION_JSON);
  }
  
  /**
   * Get the oldest record of a user action for a series. 
   * 
   * @param seriesID
   *          The series to find the first user action from.
   * @param mediaType
   *          The type of response to give either XML or JSON. 
   * @return The first user action for a series of this user if it exists.
   */
  public Response getFirstUserActionForSeries(String seriesID, String mediaType) {
    if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    int offset = 0;
    int limit = 1;
    String type = "PLAY";
    SearchResult searchResult = getEpisodesBySeries(seriesID);
    UserAction oldest = null;
    UserAction current = null;
    for (SearchResultItem episode : searchResult.getItems()) {
      try {
        UserActionList userActionList = userTrackingService.getUserActionsByTypeAndMediapackageIdByDate(type,
                episode.getId(), offset, limit);
        if (userActionList.getUserActions().size() > 0) {
          current = userActionList.getUserActions().get(0);

          if (current != null && oldest == null) {
            oldest = current;
          } else if (current != null && current.getCreated() != null && oldest != null && oldest.getCreated() != null
                  && current.getCreated().before(oldest.getCreated())) {
            oldest = current;
          }
        }
      } catch (UserTrackingException e) {
        logger.warn("Couldn't get first UserAction for episode " + episode.getId(), e);
      }
    }
    UserActionListImpl result = new UserActionListImpl();
    if (oldest != null) {
      result.add(oldest);
    }
    result.setLimit(limit);
    result.setOffset(offset);
    result.setTotal(result.getUserActions().size());
    return Response.ok(result).type(mediaType).build();
  }

  public Response getLastUserActionForSeriesAsXml(String seriesID) {
    return getLastUserActionForSeries(seriesID, MediaType.APPLICATION_XML);
  }

  public Response getLastUserActionForSeriesAsJson(String seriesID) {
    return getLastUserActionForSeries(seriesID, MediaType.APPLICATION_JSON);
  }
  
  
  /**
   * Get the most recent user action on a series.
   * 
   * @param seriesID
   *          The series to get the most recent event form.
   * @param mediaType
   *          The media type to return e.g. XML or JSON.
   * @return Returns a response containing the most recent event in a series if it exists.
   */
  public Response getLastUserActionForSeries(String seriesID, String mediaType) {
    if (servicesAreUp() != null) {
      return servicesAreUp();
    }
    int offset = 0;
    int limit = 1;
    String type = "PLAY";
    SearchResult searchResult = getEpisodesBySeries(seriesID);
    UserAction newest = null;
    UserAction current = null;
    for (SearchResultItem episode : searchResult.getItems()) {
      try {
        UserActionList userActionList = userTrackingService.getUserActionsByTypeAndMediapackageIdByDescendingDate(type,
                episode.getId(), offset, limit);
        if (userActionList.getUserActions().size() > 0) {
          current = userActionList.getUserActions().get(0);

          if (current != null && newest == null) {
            newest = current;
          } else if (current != null && current.getCreated() != null && newest != null && newest.getCreated() != null
                  && current.getCreated().after(newest.getCreated())) {
            newest = current;
          }
        }
      } catch (UserTrackingException e) {
        logger.warn("Couldn't get first UserAction for episode " + episode.getId(), e);
      }
    }
    UserActionListImpl result = new UserActionListImpl();
    if (newest != null) {
      result.add(newest);
    }
    result.setLimit(limit);
    result.setOffset(offset);
    result.setTotal(result.getUserActions().size());
    return Response.ok(result).type(mediaType).build();
  }
  
  /**
   * Reduce a list of UserActions to only the ones that a user is able to see the analytics for.
   * 
   * @param userActionList
   *          The userActionList of entries to filter out invalid ones.
   * @return Returns a UserActionList without the entries for episodes that a user is not allowed to view the analytics
   *         for.
   */
  public UserActionList filterUserActions(UserActionList userActionList) {
    // Go through each of the user actions determining whether we are allowed to view their episode.
    UserActionListImpl result = new UserActionListImpl();
    // Build a set that allows us to quickly check whether we have determined if they have access to a particular
    // episode.
    HashMap<String, Boolean> analyzeEpisode = new HashMap<String, Boolean>();
    List<UserAction> userActions = userActionList.getUserActions();
    for (UserAction userAction : userActions) {
      String mediapackageId = userAction.getMediapackageId();
      if (analyzeEpisode.get(mediapackageId) != null && analyzeEpisode.get(mediapackageId)) {
        result.add(userAction);
      } else if (analyzeEpisode.get(mediapackageId) == null) {
        if (canAnalyzeEpisode(mediapackageId)) {
          analyzeEpisode.put(mediapackageId, true);
          result.add(userAction);
        } else {
          analyzeEpisode.put(mediapackageId, false);
        }
      }
    }
    result.setTotal(result.getUserActions().size());
    return result;
  }
  
  public Response getUserSummaryForSeriesAsXml(String type, String seriesID) {
    return getUserSummaryForSeries(type, seriesID, MediaType.APPLICATION_XML);
  }

  public Response getUserSummaryForSeriesAsJson(String type, String seriesID) {
    return getUserSummaryForSeries(type, seriesID, MediaType.APPLICATION_JSON);
  }
  
  /**
   * Get the user summaries of activity for a particular series based on a particular type of user activity. 
   * 
   * @param type
   *          The type of UserActions to look for e.g. HEARTBEATS to create the summary.
   * @param seriesID
   *          The series to find the user summary for.
   * @param mediaType
   *          Either XML or JSON.
   * @return The summary of user activity for a given series based on the type of UserAction passed to it.
   */
  public Response getUserSummaryForSeries(String type, String seriesID, String mediaType) {
    UserSummaryListImpl result = new UserSummaryListImpl();
    SearchResult episodes = getEpisodesBySeries(seriesID);
    for (SearchResultItem episode : episodes.getItems()) {
      UserSummaryList current = userTrackingService.getUserSummaryByTypeAndMediaPackage(type, episode.getId());
      result.add(current.getUserSummaries());
    }
    return Response.ok(result).type(mediaType).build();
  }
  
  /**
   * Get the value of an xml tag. 
   * 
   * @param sTag
   *          The xml tag to look for and get the first result.
   * @param eElement
   *          The element to check for the tag.
   * @return The value associated with the xml tag.
   */
  private static String getTagValue(String sTag, Element eElement) {
    Node nodeValue = eElement.getElementsByTagName(sTag).item(0);
    return nodeValue.getFirstChild().getNodeValue();
	}
}
