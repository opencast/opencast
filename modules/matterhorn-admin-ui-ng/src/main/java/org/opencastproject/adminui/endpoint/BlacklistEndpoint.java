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
package org.opencastproject.adminui.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.Tuple3;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
@RestService(name = "blacklistservice", title = "Blacklist Service", notes = "", abstractText = "Provides Blacklist operations for users and capture agents")
public class BlacklistEndpoint {
  /** The key used for returning the number of courses effected by a blacklist. */
  public static final String COURSES_COUNT_KEY = "coursesCount";
  /** The key used for returning the number of events effected by a blacklist. */
  public static final String EVENTS_COUNT_KEY = "eventsCount";

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(BlacklistEndpoint.class);

  /** The participation persistence */
  private ParticipationManagementDatabase participationPersistence;

  /** Default server URL */
  private String serverUrl = "http://localhost:8080";

  /** Service url */
  private String serviceUrl = null;

  /** Used to parse JSON input */
  private JSONParser parser = new JSONParser();

  /** OSGi callback for participation persistence. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationPersistence) {
    this.participationPersistence = participationPersistence;
  }

  protected void activate(ComponentContext cc) {
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY);
      logger.debug("Configured server url is {}", ccServerUrl);
      if (ccServerUrl != null)
        this.serverUrl = ccServerUrl;
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
    logger.info("Activated blacklist service endpoint");
  }

  @GET
  @Path("{periodId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getperiod", description = "Returns the blacklist period by the given id as JSON", returnDescription = "The blacklist period as JSON", pathParameters = {
          @RestParameter(name = "periodId", description = "The period id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Returns the blacklist period as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No blacklist period with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getBlacklist(@PathParam("periodId") long periodId)
          throws NotFoundException {
    try {
      Blacklist blacklist = participationPersistence.getBlacklistByPeriodId(periodId);
      return Response.ok(blacklist.toJson(Option.option(periodId)).get(0).toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve blacklist with id {}: {}", periodId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("blacklists.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getblacklists", description = "Returns a list of blacklists by the given type filter as JSON", returnDescription = "A list of blacklists as JSON", restParameters = {
          @RestParameter(name = "type", description = "The type to filter", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any of the following: NAME, PERIOD, PURPOSE.  Add '_DESC' to reverse the sort order (e.g. NAME_DESC).", type = STRING),
          @RestParameter(name = "offset", isRequired = false, description = "The page offset", type = Type.INTEGER),
          @RestParameter(name = "limit", isRequired = false, description = "Results per page", type = Type.INTEGER),
          @RestParameter(name = "name", isRequired = false, description = "Filter by name", type = STRING),
          @RestParameter(name = "purpose", isRequired = false, description = "Filter by purpose", type = STRING) }, reponses = {
          @RestResponse(description = "Returns a list of blacklists as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unknown type, person and room are valid types only!", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response getBlacklist(@QueryParam("type") String type, @QueryParam("sort") String sort,
          @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit, @QueryParam("name") String name,
          @QueryParam("purpose") String purpose) throws NotFoundException, UnauthorizedException {
    if (type == null || (!Person.TYPE.equals(type.toLowerCase()) && !Room.TYPE.equals(type.toLowerCase())))
      return Response.status(Status.BAD_REQUEST).build();

    try {
      List<Jsons.Val> pArr = new ArrayList<Jsons.Val>();
      Option<String> order = Option.option(StringUtils.trimToNull(sort));
      List<Blacklist> blacklists = participationPersistence.findBlacklists(type, Option.option(limit),
              Option.option(offset), Option.option(StringUtils.trimToNull(name)),
              Option.option(StringUtils.trimToNull(purpose)), order);

      // Convert to Tuple3 entries list
      List<Tuple3<Long, Blacklistable, Period>> entries = new ArrayList<Tuple3<Long, Blacklistable, Period>>();
      for (Blacklist b : blacklists) {
        for (Period p : b.getPeriods()) {
          entries.add(Tuple3.tuple3(b.getId(), b.getBlacklisted(), p));
        }
      }

      // Sort if ordered by period or purpose
      if (order.isSome() && order.get().toLowerCase().startsWith("period")) {
        Collections.sort(entries, getPeriodComparator(isOrderAsc(order)));
      } else if (order.isSome() && order.get().toLowerCase().startsWith("purpose")) {
        Collections.sort(entries, getPurposeComparator(isOrderAsc(order)));
      }

      for (Tuple3<Long, Blacklistable, Period> entry : entries) {
        Blacklist b = new Blacklist(entry.getA(), entry.getB());
        b.addPeriod(entry.getC());
        pArr.addAll(b.toJson(entry.getC().getId()));
      }

      return Response.ok(Jsons.arr(pArr).toJson()).build();
    } catch (Exception e) {
      logger.error("Could not retrieve blacklist list of type {}: {}", type, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isOrderAsc(Option<String> order) {
    String[] sortArray = StringUtils.split(order.get(), "_");
    return sortArray.length > 1 && "desc".equals(sortArray[1].toLowerCase()) ? false : true;
  }

  private Comparator<Tuple3<Long, Blacklistable, Period>> getPeriodComparator(final boolean isAsc) {
    return new Comparator<Tuple3<Long, Blacklistable, Period>>() {
      @Override
      public int compare(Tuple3<Long, Blacklistable, Period> o1, Tuple3<Long, Blacklistable, Period> o2) {
        int sComp = o1.getC().getStart().compareTo(o2.getC().getStart());

        if (!isAsc)
          sComp *= -1;

        if (sComp == 0)
          sComp = o1.getB().getName().compareTo(o2.getB().getName());

        if (sComp == 0)
          sComp = o1.getC().getPurpose().getOrElse("").compareTo(o2.getC().getPurpose().getOrElse(""));

        return sComp;
      }
    };
  }

  private Comparator<Tuple3<Long, Blacklistable, Period>> getPurposeComparator(final boolean isAsc) {
    return new Comparator<Tuple3<Long, Blacklistable, Period>>() {
      @Override
      public int compare(Tuple3<Long, Blacklistable, Period> o1, Tuple3<Long, Blacklistable, Period> o2) {
        int sComp = o1.getC().getPurpose().getOrElse("").compareTo(o2.getC().getPurpose().getOrElse(""));

        if (!isAsc)
          sComp *= -1;

        if (sComp == 0)
          sComp = o1.getB().getName().compareTo(o2.getB().getName());

        if (sComp == 0)
          sComp = o1.getC().getStart().compareTo(o2.getC().getStart());

        return sComp;
      }
    };
  }

  @PUT
  @Path("{periodId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updatePeriod", description = "Updates the blacklist period by the given id", returnDescription = "The blacklist period as JSON", pathParameters = {
          @RestParameter(name = "periodId", description = "The period id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "start", description = "The start date", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "end", description = "The end date", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "purpose", description = "The purpose", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "comment", description = "The comment", isRequired = false, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the updated blacklist period as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to parse date", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No period with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updatePeriod(@PathParam("periodId") long periodId, @FormParam("start") String start,
          @FormParam("end") String end, @FormParam("purpose") String purpose, @FormParam("comment") String comment)
          throws NotFoundException {
    try {
      Period period = participationPersistence.getPeriod(periodId);

      Date startTime;
      Date endTime;
      try {
        if (StringUtils.isBlank(start)) {
          startTime = period.getStart();
        } else {
          startTime = new Date(DateTimeSupport.fromUTC(start));
        }
      } catch (Exception e) {
        logger.warn("Unable to parse start date {}", start);
        return Response.status(Status.BAD_REQUEST).build();
      }
      try {
        if (StringUtils.isBlank(end)) {
          endTime = period.getEnd();
        } else {
          endTime = new Date(DateTimeSupport.fromUTC(end));
        }
      } catch (Exception e) {
        logger.warn("Unable to parse end date {}", end);
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (startTime.after(endTime)) {
        logger.warn("Start time {} was after end time {}", startTime, endTime);
        return Response.status(Status.BAD_REQUEST).build();
      }

      if (StringUtils.isBlank(purpose)) {
        purpose = period.getPurpose().get();
      }

      if (StringUtils.isBlank(comment)) {
        comment = period.getComment().get();
      }

      period = Period.period(periodId, startTime, endTime, purpose, comment);
      Period updatedPeriod = participationPersistence.updatePeriod(period);
      return Response.ok(updatedPeriod.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update blacklist with id {}: {}", periodId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createblacklist", description = "Creates a blacklist", returnDescription = "The blacklist as JSON", restParameters = {
          @RestParameter(name = "type", description = "The blacklistable type", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "blacklistedId", description = "The id of the blacklistable element", isRequired = true, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "start", description = "The start date", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "end", description = "The end date", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "purpose", description = "The purpose", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "comment", description = "The comment", isRequired = false, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the created blacklist as JSON", responseCode = HttpServletResponse.SC_CREATED),
          @RestResponse(description = "Unable to parse date, unknown type of blacklistable room or person, and if start time is after endpoint", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "Blacklisted element not found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response createBlacklist(@FormParam("type") String type, @FormParam("blacklistedId") int blacklistedId,
          @FormParam("start") String start, @FormParam("end") String end, @FormParam("purpose") String purpose,
          @FormParam("comment") String comment) throws NotFoundException {
    long startTime;
    long endTime;
    try {
      startTime = DateTimeSupport.fromUTC(start);
    } catch (Exception e) {
      logger.warn("Unable to parse start date {}", start);
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      endTime = DateTimeSupport.fromUTC(end);
    } catch (Exception e) {
      logger.warn("Unable to parse end date {}", end);
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (startTime > endTime) {
      logger.warn("Start time {} was after end time {}", startTime, endTime);
      return Response.status(Status.BAD_REQUEST).build();
    }

    Blacklist blacklist = null;
    try {
      Blacklistable blacklistable;
      if (Room.TYPE.equals(type)) {
        blacklistable = participationPersistence.getRoom(blacklistedId);
      } else if (Person.TYPE.equals(type)) {
        blacklistable = participationPersistence.getPerson(blacklistedId);
      } else {
        logger.warn("Unknown type {}", type);
        return Response.status(Status.BAD_REQUEST).build();
      }

      List<Blacklist> blacklists = participationPersistence.findBlacklists(blacklistable);

      Period period = Period.period(new Date(startTime), new Date(endTime), purpose, comment);
      if (blacklists.isEmpty()) {
        List<Period> periods = new ArrayList<Period>();
        periods.add(period);
        blacklist = new Blacklist(blacklistable, periods);
      } else if (blacklists.size() == 1) {
        blacklist = blacklists.get(0);
        blacklist.addPeriod(period);
      } else {
        logger.warn("There was more than one blacklist returned using a single id and we can't handle that.");
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      }

      Blacklist createdBlacklist = participationPersistence.updateBlacklist(blacklist);
      Long createdPeriodId = null;
      for (Period p : createdBlacklist.getPeriods()) {
        if (p.getStart().equals(period.getStart()) && p.getEnd().equals(period.getEnd())) {
          createdPeriodId = p.getId().get();
          break;
        }
      }
      return Response.created(new URI(getBlacklistUrl(createdBlacklist.getId())))
              .entity(createdBlacklist.toJson(Option.some(createdPeriodId)).get(0).toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not create blacklist {}: {}", blacklist, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("blacklists")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createblacklists", description = "Creates multiple blacklists", returnDescription = "A JSON array listing which rooms or people were blacklisted", restParameters = {
          @RestParameter(name = "type", description = "The blacklistable type either room or person", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "blacklistedIds", description = "The ids of the blacklistable elements to blacklist", defaultValue = "[]", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "start", description = "The start date in UTC format", defaultValue = "2011-07-16T20:39:05Z", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "end", description = "The end date in UTC format", defaultValue = "2030-07-16T20:39:05Z", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "purpose", description = "The reason to blacklist these elements.", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "comment", description = "A comment about creating the blacklist", isRequired = false, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns a JSON object with the results for the different blacklistable elements such as ok, notFound or error.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to parse date, identify type, or parse JSON array of blacklistable elements", responseCode = HttpServletResponse.SC_BAD_REQUEST)})
  public Response createBlacklists(@FormParam("type") String type, @FormParam("blacklistedIds") String blacklistedIds,
          @FormParam("start") String start, @FormParam("end") String end, @FormParam("purpose") String purpose,
          @FormParam("comment") String comment) throws NotFoundException {
    long startTime;
    long endTime;
    try {
      startTime = DateTimeSupport.fromUTC(start);
    } catch (Exception e) {
      logger.warn("Unable to parse start date {}", start);
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      endTime = DateTimeSupport.fromUTC(end);
    } catch (Exception e) {
      logger.warn("Unable to parse end date {}", end);
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (startTime > endTime) {
      logger.warn("Start time {} was after end time {}", startTime, endTime);
      return Response.status(Status.BAD_REQUEST).build();
    }

    JSONArray blacklistedIdsArray;
    try {
      blacklistedIdsArray = (JSONArray) parser.parse(blacklistedIds);
    } catch (ParseException e) {
      logger.warn("Unable to parse blacklisted ids {} : {}", blacklistedIds, ExceptionUtils.getStackTrace(e));
      return Response.status(Status.BAD_REQUEST).build();
    } catch (NullPointerException e) {
      logger.warn("Unable to parse blacklisted ids because it was null {}", blacklistedIds);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.warn("Unable to parse blacklisted ids because it was the wrong class {} : {}", blacklistedIds,
              ExceptionUtils.getMessage(e));
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (!Room.TYPE.equals(type) && !Person.TYPE.equals(type)) {
      logger.warn("Unable to identify type of blacklisted items '{}'", type);
      return Response.status(Status.BAD_REQUEST).build();
    }

    BulkOperationResult result = new BulkOperationResult();

    for (Object idObject : blacklistedIdsArray) {
      Blacklist blacklist = null;
      try {
        long blacklistedId = Long.parseLong(idObject.toString());
        Blacklistable blacklistable;
        if (Room.TYPE.equals(type)) {
          blacklistable = participationPersistence.getRoom(blacklistedId);
        } else {
          blacklistable = participationPersistence.getPerson(blacklistedId);
        }

        List<Blacklist> blacklists = participationPersistence.findBlacklists(blacklistable);

        Period period = Period.period(new Date(startTime), new Date(endTime), purpose, comment);
        if (blacklists.isEmpty()) {
          List<Period> periods = new ArrayList<Period>();
          periods.add(period);
          blacklist = new Blacklist(blacklistable, periods);
        } else if (blacklists.size() == 1) {
          blacklist = blacklists.get(0);
          blacklist.addPeriod(period);
        } else {
          logger.warn("There was more than one blacklist returned using a single id and we can't handle that.");
          throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        participationPersistence.updateBlacklist(blacklist);
        result.addOk(blacklistedId);
      } catch (NotFoundException e) {
        result.addNotFound(idObject.toString());
      } catch (Exception e) {
        logger.error("Could not create blacklist {}: {}", blacklist, ExceptionUtils.getStackTrace(e));
        result.addServerError(idObject.toString());
      }

    }
    return Response.ok(result.toJson()).build();
  }


  /**
   * Gets the number of events and courses effected by a blacklist.
   *
   * @param type
   *          The type of the blacklist such as person or location.
   * @param blacklistedId
   *          The id of the blacklisted person or location.
   * @param startTime
   *          The start time of the blacklist.
   * @param endTime
   *          The end time of the blacklist.
   * @return A Tuple where the first value is the number of events effected, the second value is the number of courses
   *         effected.
   * @throws ParticipationManagementDatabaseException
   *           Thrown if there is a problem with counting the events or courses
   * @throws NotFoundException
   *           Thrown if the person or location cannot be found.
   */
  private Tuple<Long, Long> getBlacklistCount(String type, int blacklistedId, long startTime, long endTime) throws ParticipationManagementDatabaseException, NotFoundException {
      long coursesCount = -1;
      long eventsCount = -1;
      if (Room.TYPE.equalsIgnoreCase(type)) {
        eventsCount = participationPersistence.countRecordingsByDateRangeAndRoom(blacklistedId, startTime, endTime);
        coursesCount = participationPersistence.countAffectedCoursesByDateRangeAndRoom(blacklistedId, startTime, endTime);
      } else if (Person.TYPE.equalsIgnoreCase(type)) {
        eventsCount = participationPersistence.countRecordingsByDateRangeAndPerson(blacklistedId, startTime, endTime);
        coursesCount = participationPersistence.countAffectedCoursesByDateRangeAndPerson(blacklistedId, startTime, endTime);
      } else {
        throw new IllegalArgumentException("Invalid type of blacklisted item '" + type + "'");
      }
      return new Tuple<Long, Long>(eventsCount, coursesCount);
  }

  /**
   * Gets the Recordings effected by a blacklist.
   *
   * @param type
   *          The type of the blacklist such as person or location.
   * @param blacklistedId
   *          The id of the blacklisted person or location.
   * @param startTime
   *          The start time of the blacklist.
   * @param endTime
   *          The end time of the blacklist.
   * @return The list of recordings effected by this blacklist.
   *
   */
  private List<Recording> getRecordingsImpactedBytBlacklist(String type, long blacklistedId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    if (Room.TYPE.equalsIgnoreCase(type)) {
      return participationPersistence.findRecordingsByDateRangeAndRoom(blacklistedId, startTime, endTime);
    } else if (Person.TYPE.equalsIgnoreCase(type)) {
      return participationPersistence.findRecordingsByDateRangeAndPerson(blacklistedId, startTime, endTime);
    } else {
      throw new IllegalArgumentException("Invalid type of blacklisted item '" + type + "'");
    }
  }

  /**
   * Gets the Courses effected by a blacklist.
   *
   * @param type
   *          The type of the blacklist such as person or location.
   * @param blacklistedId
   *          The id of the blacklisted person or location.
   * @param startTime
   *          The start time of the blacklist.
   * @param endTime
   *          The end time of the blacklist.
   * @return A list of courses effected by this blacklist.
   */
  private List<Course> getCoursesImpactedByBlacklist(String type, long blacklistedId, long startTime, long endTime)
          throws ParticipationManagementDatabaseException, NotFoundException {
    if (Room.TYPE.equalsIgnoreCase(type)) {
      return participationPersistence.findAffectedCoursesByDateRangeAndRoom(blacklistedId, startTime, endTime);
    } else if (Person.TYPE.equalsIgnoreCase(type)) {
      return participationPersistence.findAffectedCoursesByDateRangeAndPerson(blacklistedId, startTime, endTime);
    } else {
      throw new IllegalArgumentException("Invalid type of blacklisted item '" + type + "'");
    }
  }

  /**
   * Creates a json object representing the events and courses that are effected by a blacklist.
   * @param blacklistedId
   *        The id of the blacklisted item used as a key for these results.
   * @param eventsJson
   *        The list of events that are effected by this blacklist.
   * @param coursesJson
   *        The list of courses that are effected by this blacklist.
   * @return  A JSON object with all of these results added.
   */
  private Obj createImpactedEventsAndCoursesJsonObj(String blacklistedId, List<Val> eventsJson, List<Val> coursesJson) {
    return Jsons.obj(Jsons.p(blacklistedId, Jsons.obj(Jsons.p("events", Jsons.arr(eventsJson)), Jsons.p("courses", Jsons.arr(coursesJson)),
            Jsons.p(EVENTS_COUNT_KEY, eventsJson.size()), Jsons.p(COURSES_COUNT_KEY, coursesJson.size()))));
  }

  @GET
  @Path("/blacklistCount")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getBlacklistCount", description = "Gets the number of events and courses effected by a blacklist", returnDescription = "The count of events and courses effected in JSON", restParameters = {
          @RestParameter(name = "type", description = "The blacklistable type, either person or room", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "blacklistedId", description = "The id of the blacklistable element, either the person or room.", isRequired = true, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "start", description = "The start date of the blacklist", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "end", description = "The end date of the blacklist", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
          @RestResponse(description = "The count of events and courses effected in JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to parse date or unknown type", responseCode = HttpServletResponse.SC_BAD_REQUEST)})
  public Response getBlacklistCount(@QueryParam("type") String type, @QueryParam("blacklistedId") int blacklistedId,
          @QueryParam("start") String start, @QueryParam("end") String end) throws NotFoundException {
    long startTime;
    long endTime;

    try {
      startTime = DateTimeSupport.fromUTC(start);
    } catch (Exception e) {
      logger.warn("Unable to parse start date {}", start);
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      endTime = DateTimeSupport.fromUTC(end);
    } catch (Exception e) {
      logger.warn("Unable to parse end date {}", end);
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (!type.equals(Room.TYPE) && !type.equals(Person.TYPE)) {
      logger.warn("Unknown type {}", type);
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      List<Recording> recordings = getRecordingsImpactedBytBlacklist(type, blacklistedId, startTime, endTime);
      List<Course> courses = getCoursesImpactedByBlacklist(type, blacklistedId, startTime, endTime);
      List<Val> eventsJson = new ArrayList<Val>();
      List<Val> coursesJson = new ArrayList<Val>();
      for (Recording recording : recordings) {
        eventsJson.add(recording.toJson());
      }
      for (Course course : courses) {
        coursesJson.add(course.toJson());
      }

      return Response.ok(
              Jsons.obj(Jsons.p("events", Jsons.arr(eventsJson)), Jsons.p("courses", Jsons.arr(coursesJson)),
                      Jsons.p(EVENTS_COUNT_KEY, recordings.size()), Jsons.p(COURSES_COUNT_KEY, courses.size()))
                      .toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not count number of blacklisted events by {} with start {} and end {} and id {}: {}",
              new Object[] { type, start, end, blacklistedId, ExceptionUtils.getStackTrace(e) });
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/blacklistCounts")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getBlacklistCount", description = "Gets the number of events and courses effected by a blacklist", returnDescription = "The count of events and courses effected in JSON", restParameters = {
          @RestParameter(name = "type", description = "The blacklistable type, either 'person' or 'room'", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "blacklistedIds", description = "A JSON array of ids of blacklistable elements, either the people or rooms such as [1, 2, 3, 4].", isRequired = true, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "start", description = "The start date of the blacklist in UTC e.g. 2011-07-16T20:39:05Z", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "end", description = "The end date of the blacklist in UTC e.g. 2030-07-16T20:39:05Z", isRequired = true, type = RestParameter.Type.STRING)}, reponses = {
          @RestResponse(description = "The count of events and courses effected in JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to parse date, unknown type or bad id", responseCode = HttpServletResponse.SC_BAD_REQUEST)})
  public Response getBlacklistCounts(@QueryParam("type") String type, @QueryParam("blacklistedIds") String blacklistedIds,
          @QueryParam("start") String start, @QueryParam("end") String end) throws NotFoundException {
    long startTime;
    long endTime;

    JSONArray blacklistedIdsArray;
    try {
      blacklistedIdsArray = (JSONArray) parser.parse(blacklistedIds);
    } catch (ParseException e) {
      logger.warn("Unable to parse blacklisted ids {}", blacklistedIds);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (NullPointerException e) {
      logger.warn("Unable to parse blacklisted ids because it was null {}", blacklistedIds);
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      startTime = DateTimeSupport.fromUTC(start);
    } catch (Exception e) {
      logger.warn("Unable to parse start date {}", start);
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      endTime = DateTimeSupport.fromUTC(end);
    } catch (Exception e) {
      logger.warn("Unable to parse end date {}", end);
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (!type.equals(Room.TYPE) && !type.equals(Person.TYPE)) {
      logger.warn("Unknown type {}", type);
      return Response.status(Status.BAD_REQUEST).build();
    }

    HashMap<Long, Recording> uniqueRecordings = new HashMap<Long, Recording>();
    HashMap<Long, Course> uniqueCourses = new HashMap<Long, Course>();

    Obj results = Jsons.obj();
    for (Object idObject : blacklistedIdsArray) {
      try {
        long blacklistedId = Long.parseLong(idObject.toString());
        List<Val> eventsJson = new ArrayList<Val>();
        List<Val> coursesJson = new ArrayList<Val>();

        List<Recording> currentRecordings = getRecordingsImpactedBytBlacklist(type, blacklistedId, startTime, endTime);
        for (Recording recording : currentRecordings) {
          if (recording.getId().isSome()) {
            uniqueRecordings.put(recording.getId().get(), recording);
            eventsJson.add(recording.toJson());
          }
        }

        List<Course> currentCourses = getCoursesImpactedByBlacklist(type, blacklistedId, startTime, endTime);
        for (Course course : currentCourses) {
          uniqueCourses.put(course.getId(), course);
          coursesJson.add(course.toJson());
        }
        results = results.append(createImpactedEventsAndCoursesJsonObj(idObject.toString(), eventsJson, coursesJson));
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Could not count number of blacklisted events by {} with start {} and end {} and id {}: {}",
                new Object[] { type, start, end, idObject, ExceptionUtils.getStackTrace(e) });
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
      } catch (NumberFormatException e) {
        logger.error("Unable to parse {}: {}", idObject, e);
        return Response.status(Status.BAD_REQUEST).build();
      } catch (Exception e) {
        logger.warn("Couldn't find {}:{}", idObject.toString(), ExceptionUtils.getStackTrace(e));
        results = results.append(createImpactedEventsAndCoursesJsonObj(idObject.toString(), new ArrayList<Val>(), new ArrayList<Val>()));
      }
    }
    Obj object = Jsons.obj(Jsons.p("coursesTotal", uniqueCourses.values().size()),
            Jsons.p("eventsTotal", uniqueRecordings.values().size()), Jsons.p("results", results));
    return Response.ok(object.toJson()).build();
  }

  @DELETE
  @Path("{periodId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteperiod", description = "Deletes the blacklist period by the given id", returnDescription = "No content", pathParameters = {
          @RestParameter(name = "periodId", description = "The period id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "The blacklist period has been deleted", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "No blacklist period with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteBlacklist(@PathParam("periodId") long periodId)
          throws NotFoundException {
    try {
      participationPersistence.deletePeriod(periodId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete blacklist period with id {}: {}", periodId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  public String getBlacklistUrl(long blacklistId) {
    return UrlSupport.concat(serverUrl, serviceUrl, blacklistId + ".json");
  }

}
